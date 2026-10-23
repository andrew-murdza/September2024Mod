#!/usr/bin/env python3
"""Check the exact supplied Forge mod set without launching Minecraft (Python 3.11+)."""
import argparse
import hashlib
import io
import json
import pathlib
import sys
import tomllib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]

def metadata(archive):
    if "META-INF/mods.toml" not in archive.namelist():
        return {}
    return tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8-sig"))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    lock = json.loads((ROOT / "dependency-lock.json").read_text())
    errors, provided, dependencies = [], {}, []
    checked = 0
    for entry in lock["jars"]:
        path = ROOT / entry["file"]
        if not path.is_file():
            errors.append(f"Missing jar: {entry['file']}")
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest != entry["sha256"]:
            errors.append(f"Jar hash differs from selected version: {entry['file']}")
        checked += 1
        with zipfile.ZipFile(path) as archive:
            doc = metadata(archive)
            actual_ids = {mod["modId"] for mod in doc.get("mods", [])}
            expected_ids = {mod["modId"] for mod in entry["mods"]}
            if actual_ids != expected_ids:
                errors.append(f"Mod metadata differs from lock: {entry['file']}")
            if not doc and entry["artifact"] != "kotlinforforge":
                errors.append(f"Not a native Forge mod jar: {entry['file']}")
            for modid in actual_ids:
                if modid in provided:
                    errors.append(f"Duplicate mod id {modid}: {provided[modid]} and {entry['file']}")
                provided[modid] = entry["file"]
            for owner, deps in doc.get("dependencies", {}).items():
                dependencies.extend((owner, dep) for dep in deps if dep.get("mandatory"))
            if "META-INF/jarjar/metadata.json" in archive.namelist():
                embedded = json.loads(archive.read("META-INF/jarjar/metadata.json"))
                for library in embedded.get("jars", []):
                    with zipfile.ZipFile(io.BytesIO(archive.read(library["path"]))) as nested:
                        nested_doc = metadata(nested)
                        for mod in nested_doc.get("mods", []):
                            provided.setdefault(mod["modId"], entry["file"] + "!" + library["path"])
                        for owner, deps in nested_doc.get("dependencies", {}).items():
                            dependencies.extend((owner, dep) for dep in deps if dep.get("mandatory"))
    for owner, dep in dependencies:
        if dep["modId"] not in {"forge", "minecraft"} and dep["modId"] not in provided:
            errors.append(f"Missing required dependency {dep['modId']} for {owner}, range {dep.get('versionRange')}")
    aoe = tomllib.loads((ROOT / "src/main/resources/META-INF/mods.toml").read_text())
    angling = [dep for dep in aoe["dependencies"]["${mod_id}"] if dep["modId"] == "angling"]
    if len(angling) != 1 or not angling[0].get("mandatory") or angling[0].get("side") != "BOTH":
        errors.append("AOE must require Angling on both client and server")
    for directory in (ROOT / "run/mods", ROOT / "run-data/mods"):
        for path in directory.glob("*.jar"):
            with zipfile.ZipFile(path) as archive:
                for mod in metadata(archive).get("mods", []):
                    if mod["modId"] in provided:
                        errors.append(f"Raw jar duplicates Gradle dependency {mod['modId']}: {path.relative_to(ROOT)}")
    result = {"passed": not errors, "locked_jars": len(lock["jars"]), "checked_jars": checked,
              "available_mod_ids_including_embedded": len(provided), "required_dependencies_checked": len(dependencies),
              "errors": errors,
              "limitations": "Version range and mixin compatibility are verified by the actual Forge runtime, not this static presence check."}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))
    return bool(errors)

if __name__ == "__main__":
    sys.exit(main())
