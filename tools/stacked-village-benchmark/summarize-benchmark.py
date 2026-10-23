"""Read benchmark evidence and print compact facts; never assign a pass/fail verdict."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys


def number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def delta(before: dict, after: dict, key: str):
    a, b = before.get(key), after.get(key)
    return b - a if number(a) and number(b) else None


def rounded(value, places=4):
    return round(value, places) if isinstance(value, float) else value


def world_counts(snapshot: dict) -> list[dict]:
    return [{key: world.get(key) for key in (
        "dimension", "villagers", "aliveVillagers", "movingVillagers", "villageTicketChunks", "loadedChunks")}
        for world in snapshot.get("worlds", [])]


def conservation(snapshot: dict) -> dict:
    cargo = snapshot.get("cargo", {})
    background = snapshot.get("backgroundProduction", {})
    return {"backgroundReported": background.get("total_input_output_conserved"),
            "backgroundCanePlusPaper": background.get("cane_plus_paper"),
            "backgroundInitialCane": background.get("initial_sugar_cane"),
            "setupAddedCane": background.get("setup_added_sugar_cane"),
            "setupExportedPaper": background.get("setup_exported_paper"),
            "rabbit": cargo.get("rabbit_input_output_conserved"),
            "gold": cargo.get("gold_input_output_conserved"),
            "paper": cargo.get("paper_input_output_conserved"),
            "violations": cargo.get("violations")}


def phase_summary(phase: dict) -> dict:
    before, after = phase["before"], phase["after"]
    work, intervals = phase.get("fullTickMs", {}), phase.get("tickStartIntervalMs", {})
    interval_mean = intervals.get("mean")
    has_intervals = number(interval_mean) and interval_mean > 0 and intervals.get("count", 0) > 0
    tps = 1000 / interval_mean if has_intervals else phase.get("completedTicksPerWallSecond")
    before_routes = {route["name"]: route for route in before.get("cargo", {}).get("routes", [])}
    routes = []
    for route in after.get("cargo", {}).get("routes", []):
        old = before_routes.get(route["name"], {})
        routes.append({"name": route["name"], "uuid": route.get("uuid"),
                       "pickedUpDelta": delta(old, route, "picked_up"),
                       "depositedDelta": delta(old, route, "deposited"),
                       "roundTripDelta": delta(old, route, "round_trip_deliveries"),
                       "uuidMatchesBefore": old.get("uuid") == route.get("uuid") if old.get("uuid") else None,
                       "missingWorker": route.get("missing_worker", False) if route.get("dimension") else route.get("missing_worker")})
    background_before, background_after = before.get("backgroundProduction", {}), after.get("backgroundProduction", {})
    return {"phase": phase.get("phase"), "requestedPopulation": phase.get("requestedPopulation"),
            "seconds": rounded(phase.get("sampleWindowSeconds")), "ticks": phase.get("completedTicks"),
            "tps": rounded(tps, 6), "tpsMethod": "1000 / mean completed-tick start interval" if has_intervals else "completed ticks / sample wall seconds",
            "mspt": {key: rounded(work.get(key)) for key in ("mean", "p95", "p99", "max")},
            "over50Ms": work.get("over50Ms"), "over100Ms": work.get("over100Ms"),
            "backgroundPaperDelta": delta(background_before, background_after, "paper_current"),
            "backgroundCaneDelta": delta(background_before, background_after, "sugar_cane_remaining"),
            "backgroundWorkers": background_after.get("background_workers"),
            "eligibleBackgroundWorkers": background_after.get("eligible_workshop_workers"),
            "routes": routes, "conservation": conservation(after),
            "playersAfter": after.get("players"), "workersAfter": after.get("fixture", {}).get("workers"),
            "worldsAfter": world_counts(after),
            "gcCollectionCount": phase.get("garbageCollection", {}).get("knownCollectionCount"),
            "gcCollectionTimeMs": phase.get("garbageCollection", {}).get("knownCollectionTimeMillis"),
            "diagnosticsMs": {key: {field: rounded(value.get(field)) for field in ("count", "totalMs", "max")}
                              for key, value in phase.get("measuredDiagnosticOverheadMs", {}).items()}}


def summarize(report: dict, inventory: dict | None = None) -> dict:
    inventory = inventory or {}
    machine, staged_machine = report.get("machine", {}), inventory.get("machine", {})
    ram = machine.get("physicalMemoryBytes", staged_machine.get("physical_memory_bytes"))
    heap = machine.get("maxHeapBytes")
    recorded = report.get("phases", [])
    phases = [phase_summary(phase) for phase in recorded if "before" in phase and "after" in phase]
    progress = report.get("progress", {})
    use_final = report.get("status") == "completed" and isinstance(report.get("final"), dict)
    latest = report["final"] if use_final else progress.get("snapshot", {})
    latest_routes = []
    for route in latest.get("cargo", {}).get("routes", []):
        latest_routes.append({"name": route.get("name"), "uuid": route.get("uuid"),
                              "dimension": route.get("dimension"),
                              "position": [rounded(value, 2) for value in route.get("position", [])],
                              "thought": route.get("thought"), "cargo": route.get("cargo"),
                              "pickedUpTotal": route.get("picked_up"), "depositedTotal": route.get("deposited"),
                              "pickedUpThisPhase": route.get("phase_picked_up"), "depositedThisPhase": route.get("phase_deposited"),
                              "navigation": route.get("navigation"), "remoteShopping": route.get("remote_shopping"),
                              "taskFailures": route.get("task_failures"), "missingWorker": route.get("missing_worker")})
    return {"status": report.get("status"), "world": report.get("world"), "updatedAt": report.get("updatedAt"),
            "configuration": report.get("configuration"),
            "hardware": {"cpu": staged_machine.get("cpu_name"),
                         "logicalProcessors": machine.get("logicalProcessors", staged_machine.get("logical_processors")),
                         "ramGiB": rounded(ram / 2**30, 2) if number(ram) else None,
                         "maxHeapGiB": rounded(heap / 2**30, 2) if number(heap) else None,
                         "java": machine.get("javaVersion")},
            "scope": "Playerless dedicated server; three cargo couriers included in total population, mostly local librarian workers; no client FPS measurement.",
            "artifacts": [{"file": entry.get("file"), "sha256": entry.get("sha256")}
                          for entry in inventory.get("mods", []) if entry.get("file", "").startswith(("aoemod-", "villager_realms-", "stacked-village-benchmark-"))],
            "completedPhases": phases, "partialPhaseCount": len(recorded) - len(phases),
            "latest": {"source": "final" if use_final else "progress", "at": latest.get("at", progress.get("at")),
                       "state": "completed" if use_final else progress.get("state", report.get("status")),
                       "phase": phases[-1]["phase"] if use_final and phases else progress.get("phase"),
                       "measuredSeconds": None if use_final else rounded(progress.get("measuredSeconds")),
                       "players": latest.get("players"), "fixture": latest.get("fixture"),
                       "backgroundWork": latest.get("backgroundProduction"), "worlds": world_counts(latest),
                       "routes": latest_routes, "conservation": conservation(latest)},
            "failure": report.get("failure"),
            "interpretation": "Facts only. Completed reports are not automatic gameplay or performance passes; conservation alone does not demonstrate a delivery."}


def cell(value) -> str:
    if value is None:
        return "—"
    if isinstance(value, bool):
        return str(value).lower()
    return str(value).replace("|", "\\|").replace("\n", " ")


def markdown(summary: dict) -> str:
    hardware = summary["hardware"]
    lines = [f"Report status: **{cell(summary['status'])}**. World: `{cell(summary['world'])}`.", "",
             f"Host: {cell(hardware['cpu'])}; {cell(hardware['logicalProcessors'])} logical processors; "
             f"{cell(hardware['ramGiB'])} GiB RAM; {cell(hardware['maxHeapGiB'])} GiB maximum heap; Java {cell(hardware['java'])}.",
             "", summary["scope"], "", "| Phase | Seconds | TPS¹ | Mean ms | p95 ms | p99 ms | Max ms | >50 / >100 ms ticks | Background paper Δ |",
             "|---|---:|---:|---:|---:|---:|---:|---:|---:|"]
    for phase in summary["completedPhases"]:
        tick = phase["mspt"]
        values = [phase["phase"], phase["seconds"], phase["tps"], tick["mean"], tick["p95"], tick["p99"], tick["max"],
                  f"{cell(phase['over50Ms'])} / {cell(phase['over100Ms'])}", phase["backgroundPaperDelta"]]
        lines.append("| " + " | ".join(map(cell, values)) + " |")
    lines += ["", "¹ TPS uses completed-tick start intervals when available. No performance verdict is assigned.", "",
              "| Phase | Route | Picked-up items Δ | Deposited outputs Δ | Delivery trips Δ | UUID unchanged |",
              "|---|---|---:|---:|---:|---|"]
    for phase in summary["completedPhases"]:
        for route in phase["routes"]:
            lines.append("| " + " | ".join(map(cell, [phase["phase"], route["name"], route["pickedUpDelta"],
                         route["depositedDelta"], route["roundTripDelta"], route["uuidMatchesBefore"]])) + " |")
    for phase in summary["completedPhases"]:
        conserved = phase["conservation"]
        chunks = ", ".join(f"{world['dimension']}: {world['villageTicketChunks']} village tickets / {world['loadedChunks']} loaded chunks"
                           for world in phase["worldsAfter"])
        lines += ["", f"**{cell(phase['phase'])}:** {cell(phase['workersAfter'])} workers, {cell(phase['playersAfter'])} players. {chunks}. "
                  f"Reported conservation: background={cell(conserved['backgroundReported'])}, rabbit={cell(conserved['rabbit'])}, "
                  f"gold={cell(conserved['gold'])}, paper={cell(conserved['paper'])}; violations={cell(conserved['violations'])}."]
    latest = summary["latest"]
    lines += ["", f"Latest {cell(latest['source'])}: {cell(latest['at'])}; {cell(latest['phase'])}; {cell(latest['state'])}.", ""]
    for route in latest["routes"]:
        lines.append(f"- {cell(route['name'])}: {cell(route['dimension'])} {cell(route['position'])}; {cell(route['thought'])}; "
                     f"cargo {cell(route['cargo'])}; total picked/deposited {cell(route['pickedUpTotal'])}/{cell(route['depositedTotal'])}.")
    lines += ["", summary["interpretation"]]
    if summary.get("failure"):
        lines += ["", "Recorded failure:", "```text", summary["failure"], "```"]
    return "\n".join(lines) + "\n"


def main() -> None:
    # Windows redirected Python output may otherwise use cp1252, which cannot
    # encode the Markdown delta symbols or arbitrary recorded worker thoughts.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("--inventory", type=Path)
    parser.add_argument("--markdown", action="store_true")
    args = parser.parse_args()
    report = json.loads(args.report.read_text(encoding="utf-8-sig"))
    inventory = json.loads(args.inventory.read_text(encoding="utf-8-sig")) if args.inventory else None
    result = summarize(report, inventory)
    result["reportPath"] = str(args.report.resolve())
    if args.inventory:
        result["inventoryPath"] = str(args.inventory.resolve())
    print(markdown(result) if args.markdown else json.dumps(result, indent=2), end="" if args.markdown else "\n")


if __name__ == "__main__":
    main()
