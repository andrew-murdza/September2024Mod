"""Stage a fresh, isolated stock Forge benchmark without changing the production pack."""
from __future__ import annotations

import argparse
import ctypes
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import uuid


REPO = Path(__file__).resolve().parents[2]
PRODUCTION = REPO / "run/september-production"
DEFAULT_JAVA = Path("C:/Program Files/Eclipse Adoptium/jdk-17.0.9.9-hotspot/bin/java.exe")


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def machine_info() -> dict:
    result = {"platform": platform.platform(), "logical_processors": os.cpu_count()}
    if os.name == "nt":
        import winreg
        from ctypes import wintypes
        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"HARDWARE\DESCRIPTION\System\CentralProcessor\0") as key:
            result["cpu_name"] = winreg.QueryValueEx(key, "ProcessorNameString")[0].strip()

        class MemoryStatus(ctypes.Structure):
            _fields_ = [("length", wintypes.DWORD), ("load_percent", wintypes.DWORD)] + [
                (name, ctypes.c_ulonglong) for name in (
                    "total", "available", "page_total", "page_available", "virtual_total", "virtual_available", "extended")]

        memory = MemoryStatus()
        memory.length = ctypes.sizeof(memory)
        if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):
            raise OSError("Could not read physical memory status")
        result.update(physical_memory_bytes=memory.total, available_memory_bytes=memory.available,
                      memory_load_percent=memory.load_percent)
    return result


def quote_java_argument(value: str) -> str:
    return '"' + value.replace("\\", "/").replace('"', '\\"') + '"'


def verify(manifest_path: Path) -> dict:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    instance = Path(manifest["instance"])
    expected = {entry["file"] for entry in manifest["mods"]}
    actual = {path.name for path in (instance / "mods").glob("*.jar")}
    if actual != expected:
        raise RuntimeError(f"Staged mod inventory changed: missing={expected - actual}, extra={actual - expected}")
    for entry in manifest["mods"]:
        if digest(instance / "mods" / entry["file"]) != entry["sha256"]:
            raise RuntimeError("Staged mod hash changed: " + entry["file"])
    for entry in manifest["libraries"]:
        if digest(Path(entry["path"])) != entry["sha256"]:
            raise RuntimeError("Referenced Forge library hash changed: " + entry["path"])
    for entry in manifest["launch_files"]:
        if digest(Path(entry["path"])) != entry["sha256"]:
            raise RuntimeError("Benchmark launch file hash changed: " + entry["path"])
    for entry in manifest["configs"] + manifest["datapack_files"] + manifest.get("world_files", []):
        if digest(instance / entry["file"]) != entry["sha256"]:
            raise RuntimeError("Benchmark configuration changed: " + entry["file"])
    for entry in manifest.get("evidence_inputs", []):
        if digest(Path(entry["path"])) != entry["sha256"]:
            raise RuntimeError("Retained benchmark evidence changed: " + entry["path"])
    if digest(Path(manifest["java"]["path"])) != manifest["java"]["sha256"]:
        raise RuntimeError("Java executable hash changed")
    return manifest


def prepare(args) -> dict:
    source = args.production.resolve()
    vr = args.vr_jar.resolve()
    helper = args.helper_jar.resolve()
    java = args.java.resolve()
    runtime = args.runtime_root.resolve()
    pack = args.datapack.resolve()
    forge_args = source / "libraries/net/minecraftforge/forge/1.20.1-47.4.22/win_args.txt"
    required = [vr, helper, java, forge_args, source / "staged-mods.json", source / "server.properties",
                source / "eula.txt", pack / "pack.mcmeta"]
    for path in required:
        if not path.is_file():
            raise FileNotFoundError(path)
    if not re.search(r"^eula\s*=\s*true\s*$", (source / "eula.txt").read_text(), re.MULTILINE | re.IGNORECASE):
        raise RuntimeError("The source installation does not contain an accepted EULA")
    run_id = args.run_id or datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ-") + uuid.uuid4().hex[:8]
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,90}", run_id):
        raise ValueError("run-id must contain only letters, digits, underscores and hyphens")
    instance = runtime / "runs" / run_id
    if instance.exists():
        raise FileExistsError(f"Refusing to reuse an existing benchmark instance: {instance}")

    production_manifest = json.loads((source / "staged-mods.json").read_text())
    expected = {entry["file"]: entry for entry in production_manifest["artifacts"]}
    actual = {path.name for path in (source / "mods").glob("*.jar")}
    if actual != set(expected):
        raise RuntimeError("Production mod filenames differ from its staged manifest")
    for name, entry in expected.items():
        if digest(source / "mods" / name) != entry["sha256"]:
            raise RuntimeError("Production mod hash differs from its staged manifest: " + name)

    selected = []
    for name in sorted(expected):
        original = source / "mods" / name
        if name.startswith("villager_realms-"):
            selected.append((vr, "updated_villager_realms", expected[name]))
        else:
            selected.append((original, "production", expected[name]))
    if not any(role == "updated_villager_realms" for _, role, _ in selected):
        selected.append((vr, "updated_villager_realms", {}))
    selected.append((helper, "qa_only_helper", {}))
    filenames = [path.name for path, _, _ in selected]
    if len(filenames) != len(set(filenames)):
        raise RuntimeError("Two staged jars would share a filename")

    (instance / "mods").mkdir(parents=True)
    (instance / "logs").mkdir()
    inventory = []
    for path, role, original in selected:
        target = instance / "mods" / path.name
        shutil.copy2(path, target)
        inventory.append({"file": path.name, "source": str(path), "role": role,
                          "sha256": digest(target), "bytes": target.stat().st_size,
                          "compatibility_patches": original.get("compatibility_patches", [])})
    config_inventory = []
    for directory in ("config", "defaultconfigs"):
        if (source / directory).is_dir():
            shutil.copytree(source / directory, instance / directory)
            config_inventory.extend({"file": str(path.relative_to(instance)), "sha256": digest(path)}
                                    for path in sorted((instance / directory).rglob("*")) if path.is_file())
    shutil.copy2(source / "eula.txt", instance / "eula.txt")
    level_name = "stackbench-" + run_id
    shutil.copytree(pack, instance / level_name / "datapacks/aoe-stacked-villages")
    datapack_inventory = [{"file": str(path.relative_to(instance)), "sha256": digest(path)}
                          for path in sorted((instance / level_name / "datapacks").rglob("*")) if path.is_file()]

    properties = (source / "server.properties").read_text(encoding="utf-8")
    overrides = {
        "level-name": level_name, "server-ip": "127.0.0.1", "server-port": "25581",
        "view-distance": "6", "simulation-distance": "4", "max-tick-time": "180000",
        "initial-enabled-packs": "vanilla,file/aoe-stacked-villages",
        "initial-disabled-packs": "", "motd": "AoE isolated stacked village benchmark",
    }
    for key, value in overrides.items():
        pattern = r"^" + re.escape(key) + r"=.*$"
        if re.search(pattern, properties, re.MULTILINE):
            properties = re.sub(pattern, lambda _: key + "=" + value, properties, flags=re.MULTILINE)
        else:
            properties += "\n" + key + "=" + value + "\n"
    (instance / "server.properties").write_text(properties, encoding="utf-8")
    # All referenced libraries are immutable inputs. Configs, mods, saves and logs
    # remain inside the fresh instance; the production directory is never a cwd.
    libraries = source / "libraries"
    rewritten = []
    for line in forge_args.read_text().splitlines():
        if line.startswith("-p "):
            rewritten.extend(["-p", quote_java_argument(line[3:].replace("libraries/", libraries.as_posix() + "/"))])
        elif line.startswith("-DlegacyClassPath="):
            rewritten.append(quote_java_argument(line.replace("libraries/", libraries.as_posix() + "/")))
        elif line == "-DlibraryDirectory=libraries":
            rewritten.append(quote_java_argument("-DlibraryDirectory=" + libraries.as_posix()))
        else:
            rewritten.append(line)
    rewritten_args = instance / "forge-absolute.args"
    rewritten_args.write_text("\n".join(rewritten) + "\n", encoding="utf-8")
    report = runtime / "reports" / (run_id + ".json")
    report.parent.mkdir(parents=True, exist_ok=True)
    java_args = ["-Xms1G", "-Xmx6G", "-Dstackbench.enabled=true",
                 quote_java_argument("-Dstackbench.output=" + report.as_posix()),
                 "-Xlog:gc*,safepoint:file=logs/gc.log:time,uptime,level,tags"]
    benchmark_properties = {}
    for key, value in (("warmupSeconds", args.warmup_seconds), ("baselineSeconds", args.baseline_seconds),
                       ("measureSeconds", args.measure_seconds), ("soakSeconds", args.soak_seconds)):
        if value is not None:
            if value < 0:
                raise ValueError(key + " must be nonnegative")
            benchmark_properties[key] = str(value)
    if args.populations is not None:
        if not re.fullmatch(r"[1-9][0-9]*(,[1-9][0-9]*)*", args.populations):
            raise ValueError("populations must be comma-separated positive integers")
        benchmark_properties["populations"] = args.populations
    java_args.extend("-Dstackbench." + key + "=" + value for key, value in benchmark_properties.items())
    # Java does not recursively expand @files inside argument files.
    java_args.extend(rewritten)
    java_args.append("--nogui")
    argfile = instance / "benchmark.args"
    argfile.write_text("\n".join(java_args) + "\n", encoding="utf-8")
    library_inventory = [{"path": str(path), "sha256": digest(path), "bytes": path.stat().st_size}
                         for path in sorted(libraries.rglob("*.jar"))]
    manifest = {
        "run_id": run_id, "prepared_utc": datetime.now(timezone.utc).isoformat(),
        "instance": str(instance), "production_source": str(source), "level_name": level_name,
        "report": str(report), "argfile": str(argfile),
        "stdout": str(instance / "server-console.log"), "stderr": str(instance / "server-stderr.log"),
        "java": {"path": str(java), "sha256": digest(java), "xms": "1G", "xmx": "6G"},
        "machine": machine_info(), "mods": inventory, "libraries": library_inventory,
        "configs": config_inventory, "datapack_files": datapack_inventory,
        "property_overrides": overrides, "benchmark_properties": benchmark_properties,
        "launch_files": [{"path": str(path), "sha256": digest(path)} for path in
                         (argfile, rewritten_args, instance / "server.properties", instance / "eula.txt")],
        "players": "No player or fake-player client is launched by this harness",
    }
    manifest_path = instance / "artifact-inventory.json"
    write_json(manifest_path, manifest)
    write_json(runtime / "latest-run.json", {"manifest": str(manifest_path), **manifest})
    verify(manifest_path)
    return {"manifest": str(manifest_path), "instance": str(instance), "report": str(report),
            "mods": len(inventory), "java": str(java), "machine": manifest["machine"]}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--production", type=Path, default=PRODUCTION)
    parser.add_argument("--runtime-root", type=Path, default=REPO / "run/stacked-village-benchmark")
    parser.add_argument("--vr-jar", type=Path, default=REPO.parents[1] / "VillagerRealmsMod/build/libs/villager_realms-1.0.0.jar")
    parser.add_argument("--helper-jar", type=Path, default=Path(__file__).resolve().parent / "build/libs/stacked-village-benchmark-1.0.0.jar")
    parser.add_argument("--datapack", type=Path, default=REPO.parents[1] / "VillagerRealmsMod/examples/aoe-stacked-villages")
    parser.add_argument("--java", type=Path, default=DEFAULT_JAVA)
    parser.add_argument("--run-id")
    parser.add_argument("--warmup-seconds", type=int)
    parser.add_argument("--baseline-seconds", type=int)
    parser.add_argument("--measure-seconds", type=int)
    parser.add_argument("--soak-seconds", type=int)
    parser.add_argument("--populations")
    parser.add_argument("--verify", type=Path)
    args = parser.parse_args()
    if args.verify:
        result = verify(args.verify.resolve())
        print(json.dumps({"verified": str(args.verify.resolve()), "mods": len(result["mods"])}))
    else:
        print(json.dumps(prepare(args), indent=2))


if __name__ == "__main__":
    main()
