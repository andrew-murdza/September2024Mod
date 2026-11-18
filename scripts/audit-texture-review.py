"""Read-only coverage and integrity audit for the completed texture-review ZIP.

Run after export-texture-review.py --package. No Minecraft, Gradle, image editing,
or extraction is performed. --output writes a JSON report outside the archive.
"""
import argparse
from collections import Counter
from hashlib import sha256
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets"
JAVA = ROOT / "src/main/java/net/amurdza/examplemod"
DEFAULT = ROOT / "release/texture-review-2026-11-08.zip"
EXCLUDED = {
    "squid", "glow_squid", "raw_squid", "raw_glow_squid", "cooked_squid", "cooked_glow_squid",
    "endfish", "end_fish", "cooked_endfish", "cooked_end_fish", "sulfur_end_fish", "cooked_sulfur_end_fish",
    "warped_toad", "raw_warped_toad", "cooked_warped_toad",
    "pufferfish", "cooked_pufferfish", "tropical_fish", "cooked_tropical_fish",
}
SOURCE_FILES = [
    "entity/model/ArthropleuraModel.java", "entity/render/ArthropleuraRenderer.java",
    "entity/ArthropleuraEntity.java", "entity/SpiderQueenEntity.java", "entity/render/SpiderQueenRenderer.java",
    "entity/model/IllagerLordModel.java", "entity/render/IllagerLordRenderer.java",
    "client/StalkerGlowLayer.java", "mixins/othermods/orcz/BoneBossSwordUV.java",
    "util/StingrayTextureColors.java", "util/AquaticMobSizes.java",
    "mixins/othermods/hybridaquatic/TallCrabModels.java", "mixins/othermods/hybridaquatic/TallCrabRenderer.java",
    "mixins/othermods/hybridaquatic/ArrowSquidModel.java", "mixins/othermods/hybridaquatic/StarfishModels.java",
    "portal/PortalDestination.java", "client/PortalTexturePack.java", "client/PortalClient.java",
]
RUNTIME = [
    "aoemod/textures/block/tropical_azalea_leaves.png",
    "aoemod/textures/block/tropical_flowering_azalea_leaves.png",
    "aoemod/textures/block/wooden_portal.png",
    "aoemod/textures/block/wooden_portal.png.mcmeta",
    "ambientadditions/textures/entity/blue_spotted_stingray.png",
]


def excluded_path(path):
    """Match whole species names, preserving requested arrow/vampire/umbrella squid."""
    parts = PurePosixPath(path).parts
    if any(part.lower() in EXCLUDED for part in parts[:-1]):
        return True
    name = PurePosixPath(path).name.lower().removesuffix(".mcmeta")
    name = name.rsplit(".", 1)[0].split("__")[-1]
    if name in EXCLUDED:
        return True
    # Captures carry a namespace prefix and camera suffix; item files do not.
    name = re.sub(r"_(front|side|rear|top|view_[12]|view_3_above)$", "", name)
    name = re.sub(r"^(minecraft|aoemod|culturaldelights|finsandtails|hybrid_aquatic|alexsmobs)_", "", name)
    return name in EXCLUDED


def changed_geometry():
    prefix = "src/main/resources/assets/"
    paths = set()
    for command in (["git", "diff", "HEAD", "--name-only", "--diff-filter=AM", "--", prefix],
                    ["git", "ls-files", "--others", "--exclude-standard", "--", prefix]):
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, check=True)
        paths.update(line[len(prefix):] for line in result.stdout.splitlines()
                     if line.startswith(prefix) and line.endswith(".geo.json"))
    # These remain covered if someone commits the artwork before rerunning the audit.
    for directory in ("aoemod/geo/cephalopod", "aoemod/geo/critter", "aoemod/geo/crustacean"):
        paths.update(p.relative_to(ASSETS).as_posix() for p in (ASSETS / directory).rglob("*.geo.json"))
    paths.add("aoemod/geo/illager_king.geo.json")
    return sorted(paths)


def audit(archive_path):
    errors, coverage = [], []
    report = {"archive": str(archive_path.resolve()), "errors": errors, "coverage": coverage}
    if not archive_path.is_file():
        errors.append("Final ZIP does not exist")
        report["status"] = "failed"
        return report
    with ZipFile(archive_path) as archive:
        entries = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
        names = set(entries)
        duplicates = sorted(name for name, count in Counter(entries).items() if count > 1)
        unsafe = sorted(name for name in names if ".." in PurePosixPath(name).parts
                        or name.startswith(("/", "\\")) or "\\" in name or ":" in name)
        if duplicates or unsafe:
            errors.append({"duplicate_entries": duplicates, "unsafe_entries": unsafe})

        def required(name, category, local=None):
            row = {"file": name, "kind": category}
            if name not in names:
                row["status"] = "missing"
                errors.append("Missing " + name)
            elif local is not None and archive.read(name) != local.read_bytes():
                row["status"] = "differs from current workspace"
                errors.append("Stale/different current file " + name)
            else:
                row["status"] = "present and current" if local else "present"
            coverage.append(row)

        def json_entry(name):
            if name not in names:
                errors.append("Missing " + name)
                return None
            try:
                return json.loads(archive.read(name))
            except (ValueError, UnicodeDecodeError) as failure:
                errors.append("Invalid JSON " + name + ": " + str(failure))
                return None

        forbidden = sorted(name for name in names if excluded_path(name))
        report["forbidden_entries"] = forbidden
        if forbidden:
            errors.append({"excluded_species_artwork_present": forbidden})

        inventory = (ROOT / "TEXTURE_INVENTORY.md").read_text(encoding="utf-8")
        authored = inventory.split("## Supplied or upstream artwork")[0]
        edited = set(re.findall(r"\]\(src/main/resources/assets/([^)]*\.png)\)", authored))
        edited.add("hybrid_aquatic/textures/item/mahi.png")
        report["intentionally_excluded_verified_artwork"] = []
        report["historical_inventory_files_absent_from_workspace"] = []
        for relative in sorted(edited):
            if excluded_path(relative):
                report["intentionally_excluded_verified_artwork"].append(relative)
            elif not (ASSETS / relative).is_file():
                report["historical_inventory_files_absent_from_workspace"].append(relative)
            else:
                required("after/assets/" + relative, "verified edited PNG", ASSETS / relative)

        for relative in changed_geometry():
            required("after/assets/" + relative, "modified geometry", ASSETS / relative)
        for path in (ASSETS / "aoemod/animations/entity/crustacean").rglob("*.json"):
            required("after/assets/" + path.relative_to(ASSETS).as_posix(), "modified animation", path)
        for relative in SOURCE_FILES:
            required("after/source/" + relative, "implementation source", JAVA / relative)
        for relative in ["aoemod/textures/entity/illager_lord.png", "aoemod/animations/illager_lord.animation.json",
                         "aoemod/models/block/wooden_portal_x.json", "aoemod/models/block/wooden_portal_z.json"]:
            required("after/assets/" + relative, "companion model/texture/animation", ASSETS / relative)
        for path in (ASSETS / "aoemod/models/block").glob("*blue_berry*.json*"):
            required("after/assets/" + path.relative_to(ASSETS).as_posix(), "berry model and snow mapping", path)

        for relative in RUNTIME:
            staged = ROOT / "build/texture-gallery/derived-textures" / relative
            required("after/runtime-assets/" + relative, "runtime-generated resource",
                     staged if staged.is_file() else None)
        required("after/assets/hybrid_aquatic/animations/entity/cephalopod/arrow_squid/arrow_squid.animation.json",
                 "native swimming animation for revised arrow squid")
        for variant in ("rose", "roseglow"):
            required(f"before/assets/outer_end/textures/entity/stalker_{variant}.png",
                     "native rose stalker reference")
        portal_source = (JAVA / "portal/PortalDestination.java").read_text()
        destinations = dict(re.findall(r'\w+\("([a-z]+)", [^,]+, 0x([0-9A-F]{6})\)', portal_source))
        palette = json_entry("after/portal-colors.json")
        if palette is not None and {p["id"]: p["rgb"].removeprefix("#").upper() for p in palette} != destinations:
            errors.append("Portal color manifest differs from production PortalDestination")
        for destination in destinations:
            relative = f"aoemod/blockstates/{destination}_portal.json"
            required("after/assets/" + relative, "portal blockstate", ASSETS / relative)
            for extension in (".png", ".png.mcmeta"):
                required(f"after/runtime-assets/portal-colors/{destination}_portal{extension}", "destination-tinted animation")
        report["portal_colors"] = destinations

        pairs = json_entry("seafood/pairs.json")
        if pairs is not None:
            report["seafood_pairs"] = len(pairs)
            for pair in pairs:
                if any(pair[key].split(":")[-1] in EXCLUDED for key in ("entity", "raw", "cooked")):
                    errors.append({"excluded_seafood_pair": pair})
                for kind in ("raw", "cooked"):
                    required(pair[kind + "_file"], "seafood pair " + kind)

        report["screenshots"] = {}
        for phase in ("before", "after"):
            views = json_entry(f"provenance/{phase}-view-manifest.json")
            captures = json_entry(f"screenshots/{phase}/capture-report.json")
            if views is None or captures is None:
                continue
            for view in views:
                if view['id'].split(':')[-1] in EXCLUDED:
                    errors.append('Excluded subject in screenshot manifest: ' + view['id'])
            expected = {view["label"] for view in views}
            actual = {capture["label"] for capture in captures.get("captures", [])}
            pngs = {PurePosixPath(name).stem for name in names
                    if name.startswith(f"screenshots/{phase}/") and name.endswith(".png")}
            if captures.get("status") != "passed" or expected != actual or expected != pngs:
                errors.append({"incomplete_screenshots": phase, "missing_captures": sorted(expected - actual),
                               "missing_files": sorted(expected - pngs), "unexpected_files": sorted(pngs - expected)})
            if captures.get("requested") != len(views) or len(captures.get("captures", [])) != len(views):
                errors.append("Screenshot counts differ for " + phase)
            report["screenshots"][phase] = {"views": len(views), "pngs": len(pngs), "source": captures.get("source")}

        checksums = json_entry("SHA256.json")
        if checksums is not None:
            declared = {entry["path"] for entry in checksums}
            if len(declared) != len(checksums):
                errors.append("SHA256.json contains duplicate paths")
            if declared != names - {"SHA256.json"}:
                errors.append({"unhashed_files": sorted(names - {"SHA256.json"} - declared),
                               "hashes_for_missing_files": sorted(declared - names)})
            verified = 0
            for entry in checksums:
                if entry["path"] not in names:
                    continue
                data = archive.read(entry["path"])  # ZIP reader also verifies each entry CRC.
                if len(data) != entry["bytes"] or sha256(data).hexdigest() != entry["sha256"]:
                    errors.append("Checksum/size mismatch " + entry["path"])
                else:
                    verified += 1
            report["checksum_verified_files"] = verified
        report["archive_files"] = len(entries)
    report["status"] = "passed" if not errors else "failed"
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zip", type=Path, default=DEFAULT, dest="archive")
    parser.add_argument("--output", type=Path, help="Optional external JSON report path")
    args = parser.parse_args()
    report = audit(args.archive)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "coverage"}, indent=2))
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
