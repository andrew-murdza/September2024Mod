"""Copy a stopped benchmark world for an isolated, hash-verified recovery check."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import importlib.util
import json
from pathlib import Path
import re
import shutil


HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("benchmark_prepare", HERE / "prepare-benchmark.py")
BASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BASE)


def inventory_tree(instance: Path, directory: Path) -> list[dict]:
    return [{"file": str(path.relative_to(instance)), "sha256": BASE.digest(path),
             "bytes": path.stat().st_size}
            for path in sorted(directory.rglob("*")) if path.is_file()]


def prepare(args) -> dict:
    source_manifest = args.source_manifest.resolve()
    previous = json.loads(source_manifest.read_text(encoding="utf-8-sig"))
    source = Path(previous["instance"]).resolve()
    report_path = Path(previous["report"]).resolve()
    launch = json.loads((source / "launch.json").read_text(encoding="utf-8-sig"))
    report = json.loads(report_path.read_text(encoding="utf-8-sig"))
    if launch.get("status") != "exited" or launch.get("exitCode") != 0:
        raise RuntimeError("Source server must have exited cleanly before copying its world")
    if report.get("status") != "completed":
        raise RuntimeError("Source benchmark must have a completed, retained report")
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,90}", args.run_id):
        raise ValueError("run-id must contain only letters, digits, underscores and hyphens")
    if args.seconds <= 0 or args.stable_seconds <= 0:
        raise ValueError("Recovery and stability durations must be positive")
    runtime = args.runtime_root.resolve()
    instance = runtime / "runs" / args.run_id
    if instance.exists():
        raise FileExistsError("Refusing to reuse a recovery instance: " + str(instance))
    source_world = source / previous["level_name"]
    if not (source_world / "level.dat").is_file():
        raise FileNotFoundError(source_world / "level.dat")
    for path in (args.vr_jar, args.helper_jar):
        if not path.is_file():
            raise FileNotFoundError(path)

    expected = {entry["file"]: entry for entry in previous["mods"]}
    if set(expected) != {path.name for path in (source / "mods").glob("*.jar")}:
        raise RuntimeError("Source mod filenames differ from the measured inventory")
    for name, entry in expected.items():
        if BASE.digest(source / "mods" / name) != entry["sha256"]:
            raise RuntimeError("Measured source mod changed: " + name)

    (instance / "mods").mkdir(parents=True)
    (instance / "logs").mkdir()
    mods = []
    replacements = {"updated_villager_realms": args.vr_jar.resolve(),
                    "qa_only_helper": args.helper_jar.resolve()}
    seen = set()
    for name, entry in expected.items():
        original = replacements.get(entry["role"], source / "mods" / name)
        target = instance / "mods" / original.name
        if target.exists():
            raise RuntimeError("Duplicate replacement mod filename: " + original.name)
        shutil.copy2(original, target)
        mods.append({**entry, "file": target.name, "source": str(original),
                     "sha256": BASE.digest(target), "bytes": target.stat().st_size})
        seen.add(entry["role"])
    if not set(replacements).issubset(seen):
        raise RuntimeError("Source inventory must contain both VillagerRealms and the QA helper")

    configs = []
    for name in ("config", "defaultconfigs"):
        if (source / name).is_dir():
            shutil.copytree(source / name, instance / name)
            configs.extend(inventory_tree(instance, instance / name))
    level_name = "stackbench-" + args.run_id
    world = instance / level_name
    shutil.copytree(source_world, world)
    world_files = inventory_tree(instance, world)
    # Check every copied byte against the stopped source; no hard links are used.
    for entry in world_files:
        relative = (instance / entry["file"]).relative_to(world)
        if BASE.digest(source_world / relative) != entry["sha256"]:
            raise RuntimeError("Source world changed while copying: " + str(relative))
    shutil.copy2(source / "eula.txt", instance / "eula.txt")
    properties = (source / "server.properties").read_text(encoding="utf-8")
    properties = re.sub(r"^level-name=.*$", "level-name=" + level_name, properties, flags=re.MULTILINE)
    (instance / "server.properties").write_text(properties, encoding="utf-8")
    shutil.copy2(source / "forge-absolute.args", instance / "forge-absolute.args")
    report_output = runtime / "reports" / (args.run_id + ".json")
    report_output.parent.mkdir(parents=True, exist_ok=True)
    benchmark_properties = {
        "enabled": "true", "recovery": "true", "sourceReport": str(report_path),
        "output": str(report_output), "recoverySeconds": str(args.seconds),
        "recoveryStableSeconds": str(args.stable_seconds),
    }
    java_args = ["-Xms1G", "-Xmx6G"]
    java_args.extend(BASE.quote_java_argument("-Dstackbench." + key + "=" + value)
                     for key, value in benchmark_properties.items())
    java_args.append("-Xlog:gc*,safepoint:file=logs/gc.log:time,uptime,level,tags")
    java_args.extend((instance / "forge-absolute.args").read_text().splitlines())
    java_args.append("--nogui")
    argfile = instance / "benchmark.args"
    argfile.write_text("\n".join(java_args) + "\n", encoding="utf-8")
    manifest = {
        **previous, "run_id": args.run_id, "prepared_utc": datetime.now(timezone.utc).isoformat(),
        "instance": str(instance), "level_name": level_name, "report": str(report_output),
        "argfile": str(argfile), "stdout": str(instance / "server-console.log"),
        "stderr": str(instance / "server-stderr.log"), "machine": BASE.machine_info(),
        "mods": mods, "configs": configs, "datapack_files": [], "world_files": world_files,
        "benchmark_properties": benchmark_properties,
        "property_overrides": {**previous.get("property_overrides", {}), "level-name": level_name},
        "source_instance": str(source), "source_world": str(source_world),
        "evidence_inputs": [{"path": str(path), "sha256": BASE.digest(path)}
                            for path in (source_manifest, report_path, source / "launch.json")],
        "launch_files": [{"path": str(path), "sha256": BASE.digest(path)} for path in
                         (argfile, instance / "forge-absolute.args", instance / "server.properties", instance / "eula.txt")],
        "recovery_scope": "Copied saved residents and world; no fixture reconstruction or observer teleportation",
    }
    manifest_path = instance / "artifact-inventory.json"
    BASE.write_json(manifest_path, manifest)
    BASE.write_json(runtime / "latest-recovery.json", {"manifest": str(manifest_path), **manifest})
    BASE.verify(manifest_path)
    return {"manifest": str(manifest_path), "instance": str(instance), "report": str(report_output),
            "mods": len(mods), "world_files": len(world_files), "source_report": str(report_path)}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-manifest", type=Path, required=True)
    parser.add_argument("--runtime-root", type=Path, default=BASE.REPO / "run/stacked-village-benchmark")
    parser.add_argument("--vr-jar", type=Path, required=True)
    parser.add_argument("--helper-jar", type=Path, required=True)
    parser.add_argument("--run-id", default="recovery-01")
    parser.add_argument("--seconds", type=int, default=600)
    parser.add_argument("--stable-seconds", type=int, default=60)
    print(json.dumps(prepare(parser.parse_args()), indent=2))


if __name__ == "__main__":
    main()
