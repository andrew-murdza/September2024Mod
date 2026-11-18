#!/usr/bin/env python3
"""Compose one curved Savanna compound from the two decorated native Orcz houses.

Uses the pinned source JAR and the existing lossless typed-NBT codec. The old
watchtower, bone pit, pond, and perimeter are never copied. No image or game tools
are needed. Running this script also validates the saved template offline.
"""
import copy
import hashlib
import json
import math
from pathlib import Path
import runpy
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CODEC = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (CODEC[name] for name in ('Tag', 'read_nbt', 'write_nbt'))
WIDTH, HEIGHT, DEPTH = 90, 18, 78
CENTER = (45, 39)
HOUSES = [('large', 45, 20, 3), ('small', 17, 21, 2), ('small', 73, 21, 2),
          ('small', 17, 57, 0), ('small', 30, 57, 0), ('small', 60, 57, 0), ('small', 73, 57, 0)]
PENS = [(14, 26, 28, 50), (64, 76, 28, 50)]
FIRE = (45, 39)



def ellipse(x, z, rx=43, rz=36):
    return ((x - CENTER[0]) / rx) ** 2 + ((z - CENTER[1]) / rz) ** 2


def rotated(state, turns):
    result = copy.deepcopy(state)
    props = result.get('Properties')
    if not props or not turns:
        return result
    props = props.value
    directions = ['north', 'east', 'south', 'west']
    turn = lambda value: directions[(directions.index(value) + turns) % 4]
    if 'facing' in props and props['facing'].value in directions:
        props['facing'].value = turn(props['facing'].value)
    if 'axis' in props and turns % 2 and props['axis'].value in ('x', 'z'):
        props['axis'].value = 'z' if props['axis'].value == 'x' else 'x'
    sides = {key: props.pop(key) for key in directions if key in props}
    for key, value in sides.items():
        props[turn(key)] = value
    if 'rotation' in props:
        props['rotation'].value = str((int(props['rotation'].value) + 4 * turns) % 16)
    return result


def main():
    jar = ROOT / 'libs/pack/orcz-0.87-1.20.1.jar'
    with zipfile.ZipFile(jar) as archive:
        source_bytes = archive.read('data/orcz/structures/orc_camp_normal_loot.nbt')
    native = read_nbt(source_bytes)
    CODEC['revise'](native, 0, 0)
    native_data = native[1].value
    native_palette = native_data['palette'].value[1]
    native_blocks = native_data['blocks'].value[1]
    palette, palette_keys, blocks = [], {}, {}

    def state_id(state):
        props = state.get('Properties')
        key = (state['Name'].value, tuple(sorted((k, v.value) for k, v in props.value.items())) if props else ())
        if key not in palette_keys:
            palette_keys[key] = len(palette)
            palette.append(copy.deepcopy(state))
        return palette_keys[key]

    def put(x, y, z, name=None, properties=None, state=None, nbt=None):
        assert 0 <= x < WIDTH and 0 <= y < HEIGHT and 0 <= z < DEPTH, (x, y, z)
        if state is None:
            state = {'Name': Tag(8, name)}
            if properties:
                state['Properties'] = Tag(10, {k: Tag(8, str(v)) for k, v in properties.items()})
        block = {'pos': Tag(9, (3, [x, y, z])), 'state': Tag(3, state_id(state))}
        if nbt is not None:
            block['nbt'] = copy.deepcopy(nbt)
        blocks[x, y, z] = block

    # A rounded, fully cleared interior prevents old terrain and trees intersecting houses.
    ground = {(x, z) for x in range(WIDTH) for z in range(DEPTH) if ellipse(x, z, 44, 37) <= 1}
    for x, z in sorted(ground):
        for y in range(HEIGHT):
            put(x, y, z, 'minecraft:mud' if y == 0 else 'minecraft:dirt' if y == 1
                else 'minecraft:grass_block' if y == 2 else 'minecraft:air')

    # Dirt paths bend around the common fire and branch to each house.
    for x, z in sorted(ground):
        avenue = 43 <= x <= 47 and 25 <= z <= 76
        cross = 14 <= x <= 76 and (25 <= z <= 27 or 51 <= z <= 53)
        fire_ring = (x - FIRE[0]) ** 2 + (z - FIRE[1]) ** 2 < 36
        if avenue or cross or fire_ring:
            put(x, 2, z, 'minecraft:dirt_path')

    house_audit = []
    for index, (kind, cx, cz, turns) in enumerate(HOUSES):
        sx, sz, bounds, floor = ((24, 18, (20, 28, 10, 26), 3) if kind == 'large'
                                else (15, 32, (12, 18, 29, 35), 4))
        copied, chests = [], []
        # Native positions, roofing, striped wool, bone horns, fences, carpets,
        # slab overhangs, and chest NBT survive; small houses move down one block.
        for original in native_blocks:
            x, y, z = original['pos'].value[1]
            if not (bounds[0] <= x <= bounds[1] and bounds[2] <= z <= bounds[3] and y >= floor):
                continue
            state = native_palette[original['state'].value]
            name = state['Name'].value
            if name == 'minecraft:air':
                continue
            assert 'palisade' not in name and name not in ('minecraft:dark_oak_log', 'minecraft:oak_leaves'), (kind, name)
            dx, dz = x - sx, z - sz
            for _ in range(turns):
                dx, dz = -dz, dx
            target = (cx + dx, y - (floor - 3), cz + dz)
            assert ellipse(target[0], target[2], 40, 33) < 1, target
            nbt = copy.deepcopy(original.get('nbt'))
            if name == 'minecraft:chest':
                nbt = nbt or Tag(10, {})
                nbt.value.pop('Items', None)
                nbt.value['LootTable'] = Tag(8, 'aoemod:chests/orc_camp')
                nbt.value['LootTableSeed'] = Tag(4, 882731 + index * 9389 + len(chests))
                chests.append(target)
            put(*target, state=rotated(state, turns), nbt=nbt)
            copied.append(target)
        assert len(chests) == (2 if kind == 'large' else 1)
        # A small level wood platform is underneath the copied shell, never a replacement box.
        for x in range(min(p[0] for p in copied), max(p[0] for p in copied) + 1):
            for z in range(min(p[2] for p in copied), max(p[2] for p in copied) + 1):
                put(x, 2, z, 'minecraft:acacia_planks')
        if kind == 'large':
            # Restore the missing slab at the south edge by mirroring the intact north edge.
            assert palette[blocks[43, 7, 23]['state'].value]['Name'].value == 'minecraft:air'
            mirror = palette[blocks[43, 7, 17]['state'].value]
            assert mirror['Name'].value == 'minecraft:black_wool_slab'
            put(43, 7, 23, state=mirror)
        house_audit.append(dict(native_design=kind, center=[cx, 3, cz], clockwise_quarter_turns=turns,
                                native_block_count=len(copied), chests=chests,
                                bounds=[min(p[i] for p in copied) for i in range(3)]
                                + [max(p[i] for p in copied) for i in range(3)]))

    # Two and only two stock enclosures; all gates face the shared courtyard.
    for index, (x0, x1, z0, z1) in enumerate(PENS):
        gate_x, facing = (x1, 'east') if index == 0 else (x0, 'west')
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    if (x == gate_x and z == 39) or (x == (x0 + x1) // 2 and z in (z0, z1)):
                        put(x, 3, z, 'minecraft:acacia_fence_gate', {'facing': ('north' if z == z0 else 'south' if z == z1 else facing), 'open': 'false', 'powered': 'false', 'in_wall': 'false'})
                    else:
                        put(x, 3, z, 'minecraft:acacia_fence', dict(waterlogged='false',
                            north=str(z > z0 and (x in (x0, x1) or z - 1 in (z0, z1))).lower(),
                            south=str(z < z1 and (x in (x0, x1) or z + 1 in (z0, z1))).lower(),
                            west=str(x > x0 and (z in (z0, z1) or x - 1 in (x0, x1))).lower(),
                            east=str(x < x1 and (z in (z0, z1) or x + 1 in (x0, x1))).lower()))
        for x, z in ((x0 + 5, z0 + 5), (x0 + 6, z0 + 5), (x0 + 5, z0 + 6)):
            put(x, 3, z, 'minecraft:hay_block', {'axis': 'y'})
        # A shallow three-by-three livestock pond with a mud bed.
        for x in range(x0 + 1, x0 + 4):
            for z in range(z1 - 3, z1):
                put(x, 1, z, 'minecraft:mud')
                put(x, 2, z, 'minecraft:water', {'level': '0'})

    # Two irrigated food plots, without fences (they are not extra animal pens).
    for index, (x0, x1) in enumerate(((30, 38), (52, 60))):
        for x in range(x0, x1 + 1):
            for z in range(28, 51):
                if x == (x0 + x1) // 2:
                    put(x, 2, z, 'minecraft:water', {'level': '0'})
                else:
                    put(x, 2, z, 'minecraft:farmland', {'moisture': '7'})
                    plant = 'minecraft:wheat' if (x < x0 + 4) == (index == 0) else ('minecraft:carrots' if index == 0 else 'farmersdelight:cabbages')
                    put(x, 3, z, plant, {'age': '3' if plant == 'farmersdelight:cabbages' else '7'})

    # Native communal fire with four real cushion marker seats; processor removes
    # the slabs and SavannaOrcCampPiece spawns each cushion once.
    fx, fz = FIRE
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        put(fx + dx, 2, fz + dz, 'minecraft:blackstone')
    put(fx, 2, fz, 'quark:charcoal_block')
    put(fx, 3, fz, 'minecraft:campfire', {'facing': 'north', 'lit': 'true', 'signal_fire': 'true', 'waterlogged': 'false'})
    for dx, dz in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        put(fx + dx, 3, fz + dz, 'minecraft:smooth_stone_slab', {'type': 'bottom', 'waterlogged': 'false'})

    # Walk the ellipse a quarter at a time in cardinal unit steps. At each corner
    # keep the outer connection so there is no second parallel row in the courtyard.
    quarter = [(43, 0)]
    x, z = quarter[0]
    while x > 0 or z < 36:
        options = ([(x - 1, z)] if x > 0 else []) + ([(x, z + 1)] if z < 36 else [])
        x, z = min(options, key=lambda cell: abs((cell[0]/43)**2 + (cell[1]/36)**2 - 1))
        quarter.append((x, z))
    wall = {(45 + sx*x, min(74, 39 + sz*z)) for x, z in quarter for sx in (-1, 1) for sz in (-1, 1)
            if not (abs(x) <= 3 and sz*z > 0)}
    # Bring the two sides directly to the relocated gate posts.
    wall.update((x, 74) for x in (41, 49))
    for x, z in sorted(wall):
        connections = {}
        for direction, (dx, dz) in {'west': (-1, 0), 'east': (1, 0), 'north': (0, -1), 'south': (0, 1)}.items():
            connections[direction] = 'low' if (x + dx, z + dz) in wall else 'none'
        props = dict(connections, waterlogged='false')
        for y in range(3, 6):
            put(x, y, z, 'aoemod:acacia_palisade', dict(props, up=str(y == 5).lower()))

    posts = []
    for index in range(48):
        angle = index * math.tau / 48
        x, z = min(sorted(wall), key=lambda p: (p[0] - (45 + 42 * math.cos(angle))) ** 2 + (p[1] - (39 + 35 * math.sin(angle))) ** 2)
        if (x, z) in posts:
            continue
        posts.append((x, z))
        top = 6 + (index % 3 == 0)
        for y in range(3, top + 1):
            put(x, y, z, 'minecraft:acacia_log', {'axis': 'y'})
        put(x, top + 1, z, 'minecraft:acacia_fence', {'waterlogged': 'false', 'north': 'false', 'east': 'false', 'south': 'false', 'west': 'false'})
        if index % 3 == 0:
            put(x, top + 2, z, 'minecraft:skeleton_skull', {'rotation': str(round(math.atan2(-(x - 45) / 43**2, (z - 39) / 36**2) * 16 / math.tau) % 16)})
        # Keep the irregular outer edging while leaving the courtyard clear.
        for radius in (43.5,):
            bx = round(45 + radius * math.cos(angle + .045))
            bz = round(39 + radius * 36 / 43 * math.sin(angle + .045))
            for dx, dz in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                xx, zz = bx + dx, bz + dz
                if (xx, zz) not in ground or (xx, zz) in wall or (42 <= xx <= 48 and zz >= 69):
                    continue
                put(xx, 3, zz, 'minecraft:acacia_leaves', {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'})
                if dx == dz == 0 and index % 2:
                    put(xx, 4, zz, 'minecraft:acacia_leaves', {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'})

    for x, z in sorted(ground - wall):
        if ellipse(x, z) > 1 and (x + 3*z) % 5 < 3 and not (40 <= x <= 50 and z > 69):
            put(x, 3, z, 'minecraft:acacia_leaves', {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'})

    for x in (41, 49):
        for y in range(8, HEIGHT):
            put(x, y, 74, 'minecraft:air')
        for y in range(2, 8):
            put(x, y, 74, 'minecraft:acacia_log', {'axis': 'y'})
        put(x, 8, 74, 'minecraft:skeleton_skull', {'rotation': '0'})
    for x in range(42, 49):
        put(x, 7, 74, 'minecraft:acacia_log', {'axis': 'x'})

    # Fixed acacias decorate the green areas beyond the two roads.
    for tx, tz, lean in ((33, 14, -1), (59, 14, 1), (10, 39, -1), (80, 39, 1),
                         (37, 63, -1), (54, 63, 1)):
        for y in range(3, 7):
            put(tx, y, tz, 'minecraft:acacia_log', {'axis': 'y'})
        for by, bx in ((6, tx+lean), (7, tx+lean), (7, tx+2*lean), (8, tx+2*lean)):
            put(bx, by, tz, 'minecraft:acacia_log', {'axis': 'y'})
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if abs(dx) + abs(dz) <= 4:
                    put(tx+2*lean+dx, 9, tz+dz, 'minecraft:acacia_leaves',
                        {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'})
                if abs(dx) + abs(dz) <= 2:
                    put(tx+2*lean+dx, 10, tz+dz, 'minecraft:acacia_leaves',
                        {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'})

    # Dress the new walking surface after composing every building. These
    # grasses and flowers stay on the lawn regardless of neighboring chunk order.
    for x, z in sorted(ground):
        if ellipse(x,z,40,33) >= 1 or any(x0 <= x <= x1 and z0 <= z <= z1 for x0,x1,z0,z1 in PENS):
            continue
        name_at = lambda y: palette[blocks[x,y,z]['state'].value]['Name'].value
        if name_at(2) != 'minecraft:grass_block' or name_at(3) != 'minecraft:air':
            continue
        sample = (x * 863 + z * 191 + x * z * 71) % 97
        if sample < 12:
            put(x, 3, z, 'minecraft:grass' if sample < 10 else 'minecraft:orange_tulip')

    data = {'size': Tag(9, (3, [WIDTH, HEIGHT, DEPTH])), 'entities': Tag(9, (10, [])),
            'blocks': Tag(9, (10, [blocks[pos] for pos in sorted(blocks, key=lambda p: (p[1], p[2], p[0]))])),
            'palette': Tag(9, (10, palette)), 'DataVersion': copy.deepcopy(native_data['DataVersion'])}
    root = ('', Tag(10, data))
    encoded = write_nbt(root)
    assert read_nbt(encoded) == root
    assert len(blocks) == len(data['blocks'].value[1])
    actual_names = [palette[b['state'].value]['Name'].value for b in blocks.values()]
    assert actual_names.count('minecraft:chest') == 8
    assert actual_names.count('minecraft:acacia_fence_gate') == 6
    assert actual_names.count('minecraft:smooth_stone_slab') == 4
    assert actual_names.count('minecraft:campfire') == 1
    assert not any(name.startswith('orcz:') for name in actual_names)
    assert not any(name in ('minecraft:bone_block', 'minecraft:iron_ore', 'minecraft:coal_ore') for name in actual_names)
    assert actual_names.count('minecraft:wheat') == 184
    assert actual_names.count('minecraft:carrots') == actual_names.count('farmersdelight:cabbages') == 92
    assert actual_names.count('minecraft:water') == 64
    for x0, _, _, z1 in PENS:
        for x in range(x0 + 1, x0 + 4):
            for z in range(z1 - 3, z1):
                assert palette[blocks[x, 1, z]['state'].value]['Name'].value == 'minecraft:mud'
                assert palette[blocks[x, 2, z]['state'].value]['Name'].value == 'minecraft:water'
    assert all(ellipse(x, z, 41.3, 34.3) >= 1 for (x, y, z), block in blocks.items()
               if y <= 4 and palette[block['state'].value]['Name'].value == 'minecraft:acacia_leaves')
    assert all('tall' not in [value.value for value in palette[block['state'].value].get('Properties', Tag(10, {})).value.values()]
               for block in blocks.values() if palette[block['state'].value]['Name'].value == 'aoemod:acacia_palisade')
    # Seal the intended front gate for this flood audit: nowhere else can a
    # cardinal ground-level route escape through the compound's curved wall.
    closed = wall | {(x, z) for x in range(42, 49) for z in range(71, 78)}
    seen, frontier = set(), [(45, 39)]
    while frontier:
        x, z = frontier.pop()
        if (x, z) in seen or (x, z) in closed:
            continue
        assert 0 < x < WIDTH - 1 and 0 < z < DEPTH - 1, 'Unintended palisade gap'
        seen.add((x, z))
        frontier.extend((x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    assert all((cx, cz) in seen for _, cx, cz, _ in HOUSES)
    assert all((x, z) in seen for x0, x1, z0, z1 in PENS for x in (x0, x1) for z in (z0, z1))
    target = ROOT / 'src/main/resources/data/aoemod/structures/orc_settlement.nbt'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(encoded)
    audit = dict(template='aoemod:orc_settlement', size=[WIDTH, HEIGHT, DEPTH], ground_center_offset=[-45, -2, -39],
                 sha256=hashlib.sha256(encoded).hexdigest(), source_sha256=hashlib.sha256(source_bytes).hexdigest(),
                 houses=house_audit, pens=[dict(bounds=[x0, z0, x1, z1], count=8) for x0, x1, z0, z1 in PENS],
                 block_count=len(blocks), palette_count=len(palette), palisade_columns=len(wall), native_log_posts=len(posts),
                 chests=8, cushion_markers=4, crop_blocks=368, gate_count=6, livestock_ponds=2, pond_size=[3, 3],
                 checks=['typed NBT round-trip', 'all positions in90x18x78', 'seven native decorated houses',
                         'all houses and both pens inside single sealed curved boundary', 'no retired Orcz blocks',
                         'no watchtower copied', 'no old pond/bone pit copied', 'two fenced pens', 'native house chests preserved',
                         'outer bushes and courtyard acacias', 'two 3x3 mud-bottom livestock ponds', 'normal low palisade connections'])
    audit_path = ROOT / 'build/reports/orc-settlement-template-audit.json'
    audit_path.parent.mkdir(parents=True, exist_ok=True)
    audit_path.write_text(json.dumps(audit, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(audit, indent=2))


if __name__ == '__main__':
    main()
