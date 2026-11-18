"""Read saved Java 1.20.1 region files and report actual decorated vegetation.

No Minecraft instance or Python dependencies are required. Save the QA world
before inspection; unsaved chunks are deliberately outside this audit's scope.
Example: python scripts/audit-tropical-world.py --world run/qa/world --output build/tropical-audit.json
Use --compare-overworld after visiting vanilla cherry groves/dark forests to
include their saved chunks. --sample name:x:z:radius adds focused measurements.
"""
from __future__ import annotations

import argparse
import collections
from datetime import datetime, timezone
import gzip
import hashlib
import io
import json
from pathlib import Path
import struct
import zlib


AIR = {"Name": "minecraft:air"}
SOIL = {"minecraft:" + name for name in (
    "moss_block", "mud", "clay", "dirt", "rooted_dirt", "grass_block", "podzol", "mycelium", "coarse_dirt")}
VINES = {"minecraft:cave_vines", "minecraft:cave_vines_plant"}
FLOWERS = {"minecraft:" + name for name in (
    "torchflower", "rose_bush", "blue_orchid", "pitcher_plant", "allium", "peony", "lilac", "pink_petals",
    "dandelion", "poppy", "cornflower", "oxeye_daisy", "lily_of_the_valley", "sunflower", "azure_bluet",
    "red_tulip", "orange_tulip", "pink_tulip", "white_tulip")}
FLOWERS |= set("ferdinandsflowers:red_geranium ferdinandsflowers:light_pink_geranium biomesoplenty:orange_cosmos "
               ""
               "biomesoplenty:pink_daffodil biomeswevegone:japanese_orchid floral:blue_tulip floral:yellow_tulip".split())
REPLACEABLE = FLOWERS | {"minecraft:" + name for name in (
    "air", "cave_air", "void_air", "grass", "tall_grass", "fern", "large_fern", "red_mushroom", "brown_mushroom",
    "vine", "glow_lichen", "azalea", "flowering_azalea", "seagrass", "tall_seagrass", "water")}
WATER = {"minecraft:water", "minecraft:bubble_column"}
INTERESTING_BLOCKS = SOIL | FLOWERS | VINES


class Nbt:
    def __init__(self, data):
        self.stream = io.BytesIO(data)

    def read(self, size):
        data = self.stream.read(size)
        if len(data) != size:
            raise ValueError("Truncated NBT")
        return data

    def number(self, code):
        return struct.unpack(">" + code, self.read(struct.calcsize(code)))[0]

    def string(self):
        return self.read(self.number("H")).decode("utf-8", errors="replace")

    def payload(self, kind):
        if kind in (1, 2, 3, 4, 5, 6):
            return self.number({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[kind])
        if kind == 8:
            return self.string()
        if kind == 9:
            child, count = self.number("B"), self.number("i")
            if count < 0:
                raise ValueError("Negative NBT list length")
            return [self.payload(child) for _ in range(count)]
        if kind == 10:
            result = {}
            while (child := self.number("B")) != 0:
                name = self.string()
                result[name] = self.payload(child)
            return result
        if kind in (7, 11, 12):
            count = self.number("i")
            if count < 0:
                raise ValueError("Negative NBT array length")
            if kind == 7:
                return self.read(count)
            code = "i" if kind == 11 else "Q"
            return struct.unpack(">" + code * count, self.read(struct.calcsize(code) * count))
        raise ValueError("Unsupported NBT tag " + str(kind))

    def root(self):
        kind = self.number("B")
        self.string()
        return self.payload(kind)


def index_at(container, index, minimum_bits):
    palette = container.get("palette", [])
    if len(palette) <= 1:
        return 0
    bits = max(minimum_bits, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    data = container.get("data", ())
    if not data:
        raise ValueError("Nonuniform palette has no packed data")
    return (data[index // per_long] >> ((index % per_long) * bits)) & ((1 << bits) - 1)


class World:
    def __init__(self, root, dimension):
        self.dimension = dimension
        namespace, path = dimension.split(":", 1)
        region = root / "region" if dimension == "minecraft:overworld" else root / "dimensions" / namespace / path / "region"
        self.chunks = {}
        self.skipped = collections.Counter()
        self.region_files = []
        for filename in sorted(region.glob("r.*.*.mca")):
            before = filename.stat()
            with filename.open("rb") as source:
                locations = source.read(4096)
                for slot in range(1024):
                    location = int.from_bytes(locations[slot * 4:slot * 4 + 4], "big")
                    if location == 0:
                        continue
                    source.seek((location >> 8) * 4096)
                    length = int.from_bytes(source.read(4), "big")
                    compression = int.from_bytes(source.read(1), "big")
                    data = source.read(length - 1)
                    if compression == 2:
                        data = zlib.decompress(data)
                    elif compression == 1:
                        data = gzip.decompress(data)
                    elif compression != 3:
                        raise ValueError(f"Unsupported region compression {compression}: {filename}, slot {slot}")
                    chunk = Nbt(data).root()
                    status = chunk.get("Status", chunk.get("status", "unknown"))
                    if status not in ("full", "minecraft:full"):
                        self.skipped[status] += 1
                        continue
                    sections = {section["Y"]: section for section in chunk.get("sections", [])}
                    self.chunks[(chunk["xPos"], chunk["zPos"])] = sections
            digest = hashlib.sha256(filename.read_bytes()).hexdigest()
            after = filename.stat()
            if (before.st_size, before.st_mtime_ns) != (after.st_size, after.st_mtime_ns):
                raise ValueError("Region changed during audit; save and quiesce the QA world: " + str(filename))
            self.region_files.append({"path": str(filename.resolve()), "bytes": after.st_size, "sha256": digest,
                                      "modified_utc": datetime.fromtimestamp(after.st_mtime, timezone.utc).isoformat()})

    def block(self, x, y, z):
        sections = self.chunks.get((x // 16, z // 16))
        if sections is None:
            return None
        section = sections.get(y // 16)
        if section is None or "block_states" not in section:
            return AIR
        states = section["block_states"]
        return states["palette"][index_at(states, (y % 16) * 256 + (z % 16) * 16 + x % 16, 4)]

    def biome(self, x, y, z):
        sections = self.chunks.get((x // 16, z // 16), {})
        section = sections.get(y // 16)
        if section is None or "biomes" not in section:
            return "unknown"
        biomes = section["biomes"]
        return biomes["palette"][index_at(biomes, ((y % 16) // 4) * 16 + ((z % 16) // 4) * 4 + (x % 16) // 4, 1)]


def describe(values):
    if not values:
        return {"count": 0}
    values = sorted(values)
    return {"count": len(values), "min": values[0], "median": values[len(values) // 2],
            "p90": values[min(len(values) - 1, int(len(values) * .9))], "max": values[-1],
            "mean": round(sum(values) / len(values), 4)}


def family(name):
    path = name.split(":")[-1]
    return path.removesuffix("_log").removesuffix("_wood")


def aquatic(state):
    name = state["Name"]
    return name in WATER or state.get("Properties", {}).get("waterlogged") == "true" or name.endswith(
        (":kelp", ":kelp_plant", "_kelp", "_kelp_plant", ":seagrass", ":tall_seagrass", "_seagrass"))


def components(points, radius):
    remaining = set(points)
    by_column = collections.defaultdict(set)
    for x, y, z in remaining:
        by_column[x, z].add(y)
    groups = []
    while remaining:
        first = remaining.pop()
        queue = [first]
        group = [first]
        while queue:
            x, y, z = queue.pop()
            for dx in range(-radius, radius + 1):
                for dz in range(-radius, radius + 1):
                    for next_y in by_column.get((x + dx, z + dz), ()):
                        point = (x + dx, next_y, z + dz)
                        if abs(next_y - y) <= 3 and point in remaining:
                            remaining.remove(point)
                            queue.append(point)
                            group.append(point)
        groups.append(group)
    return groups


def woody_support(world, wood, inside):
    remaining = set(wood)
    counts = collections.Counter({name: 0 for name in (
        "components", "components_with_soil_or_mangrove_root_contact",
        "unresolved_components_clipped_by_measurement_edge", "unsupported_interior_components")})
    unsupported = []
    while remaining:
        first = remaining.pop()
        queue, group = [first], [first]
        supported, clipped = False, False
        while queue:
            x, y, z = queue.pop()
            below = (world.block(x, y - 1, z) or AIR)["Name"]
            supported |= below in SOIL or below.endswith("mangrove_roots")
            clipped |= any(not inside(x + dx, z + dz)
                           or ((x + dx) // 16, (z + dz) // 16) not in world.chunks
                           for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        neighbor = (x + dx, y + dy, z + dz)
                        if neighbor in remaining:
                            remaining.remove(neighbor)
                            queue.append(neighbor)
                            group.append(neighbor)
        counts["components"] += 1
        if supported:
            counts["components_with_soil_or_mangrove_root_contact"] += 1
        elif clipped:
            counts["unresolved_components_clipped_by_measurement_edge"] += 1
        else:
            counts["unsupported_interior_components"] += 1
            unsupported.append({"logs": len(group), "lowest_log": list(min(group, key=lambda point: point[1]))})
    return {"counts": dict(counts), "unsupported_interior_components": unsupported,
            "method": "26-neighbor connected log/wood components; branches can join neighboring trees, so this detects isolated floating wood rather than proving each planting origin"}


def metrics(world, bounds=None):
    ground, leaves, roofs = {}, {}, {}
    wood, roots, vines = {}, {}, {}
    flowers = collections.defaultdict(set)
    minimum_y = 0 if world.dimension.startswith("aoemod:") else -64
    selected_chunks = 0
    def inside(x, z):
        return bounds is None or bounds[0] <= x <= bounds[2] and bounds[1] <= z <= bounds[3]
    for (chunk_x, chunk_z), sections in world.chunks.items():
        if bounds and (chunk_x * 16 > bounds[2] or chunk_x * 16 + 15 < bounds[0]
                       or chunk_z * 16 > bounds[3] or chunk_z * 16 + 15 < bounds[1]):
            continue
        selected_chunks += 1
        for section_y, section in sections.items():
            if section_y < minimum_y // 16 or "block_states" not in section:
                continue
            states = section["block_states"]
            palette = states["palette"]
            interesting = {i for i, state in enumerate(palette) if state["Name"] in INTERESTING_BLOCKS
                           or state["Name"].endswith(("_leaves", "_log", "_wood", "mangrove_roots", "_mushroom_block"))}
            if not interesting:
                continue
            for index in range(4096):
                state_index = index_at(states, index, 4)
                if state_index not in interesting:
                    continue
                x, y, z = chunk_x * 16 + index % 16, section_y * 16 + index // 256, chunk_z * 16 + (index // 16) % 16
                if not inside(x, z):
                    continue
                state = palette[state_index]
                name = state["Name"]
                column = (x, z)
                if name in SOIL:
                    ground[column] = max(ground.get(column, minimum_y - 1), y)
                if name.endswith("_leaves"):
                    leaves[column] = max(leaves.get(column, minimum_y - 1), y)
                if name.endswith("_mushroom_block"):
                    roofs[column] = max(roofs.get(column, minimum_y - 1), y)
                if name.endswith(("_log", "_wood")):
                    wood[x, y, z] = name
                if name.endswith("mangrove_roots"):
                    roots[x, y, z] = name
                if name in VINES:
                    vines[x, y, z] = state
                if name in FLOWERS and state.get("Properties", {}).get("half") != "upper":
                    flowers[name].add((x, y, z))
    biomes = collections.defaultdict(lambda: collections.Counter())
    grounded_stems = collections.defaultdict(set)
    wrong = []
    roots_outside_swamp = []
    for (x, z), y in ground.items():
        biome = world.biome(x, y + 1, z)
        stats = biomes[biome]
        stats["soil_columns"] += 1
        stats["soil_columns_below_sea_level"] += y < 63
        state = world.block(x, y + 1, z) or AIR
        wet = aquatic(state)
        if not wet and y < 62:
            wet = any(aquatic(world.block(x, above, z) or AIR) for above in range(y + 2, min(64, y + 5)))
        if wet:
            stats["submerged_soil_columns"] += 1
        else:
            stats["dry_soil_columns"] += 1
            stats["dry_columns_under_leaves"] += leaves.get((x, z), minimum_y - 1) > y
            stats["dry_columns_under_leaves_or_mushroom_roof"] += max(leaves.get((x, z), minimum_y - 1), roofs.get((x, z), minimum_y - 1)) > y
        stem = wood.get((x, y + 1, z))
        if stem:
            species = family(stem)
            grounded_stems[biome, species].add((x, y + 1, z))
            stats["grounded_log_columns"] += 1
            allowed = {"aoemod:rainforest": {"jungle", "dark_oak", "oak", "azalea"},
                       "aoemod:cherry_grove": {"cherry"}, "aoemod:mangrove_swamp": {"mangrove"},
                       "aoemod:coral_reef": set()}.get(biome)
            if allowed is not None and species not in allowed:
                wrong.append({"biome": biome, "species": species, "position": [x, y + 1, z]})
        if (x, y + 1, z) in roots:
            stats["mangrove_rooted_soil_columns"] += 1
            if world.dimension.startswith("aoemod:") and biome != "aoemod:mangrove_swamp":
                roots_outside_swamp.append({"biome": biome, "position": [x, y + 1, z]})
    species_metrics = collections.defaultdict(dict)
    for (biome, species), positions in grounded_stems.items():
        groups = components(positions, 1)
        species_metrics[biome][species] = {"grounded_log_columns": len(positions), "connected_bases": len(groups),
                                           "base_size_distribution": describe([len(group) for group in groups])}
    for biome, stats in biomes.items():
        dry = stats["dry_soil_columns"]
        stats["leaf_canopy_percent_of_dry_soil"] = round(100 * stats["dry_columns_under_leaves"] / dry, 3) if dry else None
        stats["roof_percent_of_dry_soil"] = round(100 * stats["dry_columns_under_leaves_or_mushroom_roof"] / dry, 3) if dry else None
        stats["grounded_log_columns_per_256_dry_columns"] = round(256 * stats["grounded_log_columns"] / dry, 3) if dry else None
    flower_metrics = {}
    for species, positions in sorted(flowers.items()):
        grouped = collections.defaultdict(set)
        for point in positions:
            grouped[world.biome(*point)].add(point)
        flower_metrics[species] = {}
        for biome, points in grouped.items():
            adjacency = components(points, 1)
            proximity = components(points, 3)
            clipped = sum(any(not inside(x + dx, z + dz)
                              or ((x + dx) // 16, (z + dz) // 16) not in world.chunks
                              for x, y, z in group for dx, dz in ((-3, 0), (3, 0), (0, -3), (0, 3),
                                                                (-3, -3), (-3, 3), (3, -3), (3, 3)))
                          for group in proximity)
            flower_metrics[species][biome] = {"lower_or_single_blocks": len(points),
                                              "adjacent_components": describe([len(group) for group in adjacency]),
                                              "components_within_three_blocks": describe([len(group) for group in proximity]),
                                              "proximity_singleton_components": sum(len(group) == 1 for group in proximity),
                                              "proximity_components_touching_unmeasured_edges": clipped}
    lengths, clearances, heads, unfinished = [], [], [], []
    unknown_obstacles = collections.Counter()
    for (x, y, z), state in vines.items():
        if (x, y + 1, z) in vines:
            continue
        bottom = y
        while (x, bottom - 1, z) in vines:
            bottom -= 1
        tip = vines[x, bottom, z]
        lengths.append(y - bottom + 1)
        if tip["Name"] != "minecraft:cave_vines":
            unfinished.append([x, bottom, z])
            continue
        age = tip.get("Properties", {}).get("age")
        heads.append({"position": [x, bottom, z], "length": y - bottom + 1, "age": age})
        for obstacle_y in range(bottom - 1, minimum_y - 1, -1):
            obstacle = world.block(x, obstacle_y, z)
            if obstacle is None:
                break
            name = obstacle["Name"]
            if name in VINES | REPLACEABLE:
                continue
            clearance = bottom - obstacle_y
            clearances.append(clearance)
            heads[-1]["nearest_nonreplaceable_estimate"] = {"y": obstacle_y, "block": name, "distance": clearance}
            if name not in SOIL and not name.endswith(("_leaves", "_log", "_wood", "_mushroom_block")):
                unknown_obstacles[name] += 1
            break
    return {"bounds": bounds, "full_chunks_intersecting_measurement": selected_chunks,
            "biomes": {key: dict(value) for key, value in sorted(biomes.items())},
            "grounded_species": dict(species_metrics), "non_native_grounded_log_contacts": wrong,
            "mangrove_root_contacts_outside_swamp": roots_outside_swamp,
            "woody_component_support": woody_support(world, wood, inside),
            "flowers": flower_metrics,
            "cave_vines": {"body_blocks": sum(state["Name"] == "minecraft:cave_vines_plant" for state in vines.values()),
                           "head_blocks": sum(state["Name"] == "minecraft:cave_vines" for state in vines.values()),
                           "column_lengths": describe(lengths), "single_block_columns": sum(length == 1 for length in lengths),
                           "tips_with_age_25": sum(tip["age"] == "25" for tip in heads),
                           "unterminated_columns": unfinished, "tip_obstacle_distance": describe(clearances),
                           "tip_distance_below_four": sum(distance < 4 for distance in clearances),
                           "unknown_obstacle_kinds": dict(unknown_obstacles), "columns": heads}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--dimension", default="aoemod:tropical_dimension")
    parser.add_argument("--compare-overworld", action="store_true")
    parser.add_argument("--sample", action="append", default=[], help="name:x:z:radius")
    parser.add_argument("--samples-only", action="store_true", help="Measure only the named bounds, useful when retained chunks contain older generation")
    args = parser.parse_args()
    if not args.world.is_dir():
        parser.error("World directory does not exist")
    if args.samples_only and (not args.sample or args.compare_overworld):
        parser.error("--samples-only requires --sample and cannot be combined with --compare-overworld")
    dimensions = [args.dimension]
    if args.compare_overworld and "minecraft:overworld" not in dimensions:
        dimensions.append("minecraft:overworld")
    report = {"world": str(args.world.resolve()), "scope": "Named sample bounds only" if args.samples_only else "All saved FULL chunks and named samples", "method": {
        "source": "Saved FULL-status region chunks, decoded directly from block/biome palettes",
        "canopy": "Columns with leaves above actual soil; mushrooms additionally counted in roof coverage",
        "bases": "Logs immediately above soil; adjacent 2x2 logs form a connected base, not four asserted trees",
        "biomes": "Stored quart-cell biome palettes, without the runtime seeded Voronoi biome zoom",
        "flowers": "Single/lower blocks, with both adjacent and <=3-block same-species component distributions",
        "vine_clearance": "Distance to first block outside explicit air/replaceable-plant/water set; unknown obstacle kinds are reported",
        "limitations": ["Only saved FULL chunks are measured; absent chunks are never treated as empty terrain",
                        "Connected bases can merge unusually close trees; mangrove roots are reported separately",
                        "Non-native log contacts are candidates: branches can touch rising soil and quart cells differ from runtime boundaries",
                        "Mangrove root contacts outside swamp can include roots extending across a legitimate boundary",
                        "Flower components approximate patches and can merge nearby independent placements",
                        "Offline NBT has no runtime collision/replaceability tags; vine obstacle distances are estimates"]},
        "dimensions": {}}
    level_dat = args.world / "level.dat"
    if level_dat.exists():
        level = Nbt(gzip.decompress(level_dat.read_bytes())).root().get("Data", {})
        report["seed"] = level.get("WorldGenSettings", {}).get("seed")
    for dimension in dimensions:
        world = World(args.world, dimension)
        entry = {"region_files": world.region_files, "saved_full_chunks": len(world.chunks),
                 "skipped_nonfull_chunks_by_status": dict(world.skipped), "samples": {}}
        if not world.chunks and dimension == args.dimension:
            raise ValueError("No saved FULL chunks in the requested dimension; visit and save the world first")
        if not args.samples_only:
            entry["all_saved_chunks"] = metrics(world)
        if dimension == args.dimension:
            for sample in args.sample:
                name, x, z, radius = sample.split(":")
                x, z, radius = int(x), int(z), int(radius)
                entry["samples"][name] = metrics(world, [x - radius, z - radius, x + radius, z + radius])
        report["dimensions"][dimension] = entry
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(args.output.resolve()), "seed": report.get("seed"),
                      "dimensions": {dimension: {"full_chunks": entry["saved_full_chunks"],
                                                  "biomes": entry.get("all_saved_chunks", {}).get("biomes"),
                                                  "sample_biomes": {name: sample["biomes"] for name, sample in entry["samples"].items()}}
                                     for dimension, entry in report["dimensions"].items()}}, indent=2))


if __name__ == "__main__":
    main()
