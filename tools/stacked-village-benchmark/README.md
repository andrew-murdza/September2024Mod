# Stacked village production benchmark

This optional QA mod measures a playerless, populated VillagerRealms village across AoE's Desert and Nether in the full stock Forge pack. It is excluded from the gameplay release. [BENCHMARK_RESULTS.md](BENCHMARK_RESULTS.md) records the completed 32/64/128-resident run, its nighttime navigation failure, and the separately validated saved-world recovery with the correction.

## Workload

The fixture creates registered rooms, beds, food storage, workplaces and physical stairs in a fresh generated world. Real villagers use ordinary AI, work schedules, storage transactions and configured seam travel. The village keeps its full 256×256 footprint ticking in both dimensions: 289 intersecting chunks per dimension with the current town anchor. No player or fake player keeps it active.

Population steps are 32, 64 and 128 total villagers, divided between Desert surface and lower Nether workplaces. **Three couriers are included in those totals**: a cook fetches rabbit downward, another fetches gold nuggets upward to make golden carrots, and a librarian fetches sugar cane downward to make paper. The remaining workers are mostly librarians doing local work. This is not a test of 128 simultaneous cross-dimension haulers or every profession.

The current seed's validated fixture places the Desert workplace floor at Y74 and the lower Nether workplaces at Y18. Workers must walk the winding stairs to the Y-30/Y125 seam landings: 104 blocks of vertical travel on the Desert side and 107 on the Nether side, plus horizontal approaches. A full supply round trip can take several minutes. A short period with no deposits is not evidence of adequate hauling throughput; inspect route progress and allow the 600-second measurement window.

The QA helper overrides `villager_realms/library_work/default.json` with `paper_stock_target: 1024` and `book_stock_target: 4`. The paper target remains capped at 1,024. Before each populated phase's warmup, `preparePopulationStock()` uses normal logical-storage transactions to top each background granary up to 1,024 sugar cane and export completed paper from the background library chests. It does not touch cargo rooms or inventories carried by workers, and performs no replenishment or export during measurement. This restores local paper demand between the 600-second population steps without silently carrying over full output chests.

Reports retain cumulative setup-added cane and setup-exported paper. Background conservation checks `remaining cane + current paper + exported paper = initial 8,192 cane + setup-added cane`, including goods carried by background workers. Cargo setup additions are accounted for separately; observer code never performs deliveries. These are benchmark fixture settings, not changes to normal gameplay defaults. Supplies within each measured interval are finite, so check actual production and remaining stock rather than assuming every worker stays busy.

Background furnaces retain their original 64 raw-gold inputs each and are not replenished between phases. They can finish before later population steps, so the benchmark does not represent a constant smelting workload across all stages. House food also remains finite.

## Build and run

Run from the AoE repository root after building the current VillagerRealms release:

```powershell
.\gradlew.bat -p tools/stacked-village-benchmark build
```

First run a functional probe. It measures an empty paired-village baseline and one population step; `-SoakSeconds 0` skips the final soak:

```powershell
.\tools\stacked-village-benchmark\run-benchmark.ps1 -Populations 32 -WarmupSeconds 30 -BaselineSeconds 30 -MeasureSeconds 600 -SoakSeconds 0
```

After inspecting successful cargo activity, run the full sequence:

```powershell
.\tools\stacked-village-benchmark\run-benchmark.ps1 -Populations '32,64,128' -WarmupSeconds 60 -BaselineSeconds 60 -MeasureSeconds 600 -SoakSeconds 600
```

Population comparisons hold daytime; the final soak advances the work/sleep clock. Allow additional time for startup, world generation and fixture construction. `-VrJar` and `-HelperJar` select explicit built artifacts. Add `-PrepareOnly` to stage without launching, then launch that exact fresh instance with `-PreparedManifest <printed-manifest-path>`.

Each run gets a new directory under `run/stacked-village-benchmark/runs/<run-id>` and a unique world name. Previous worlds and the production directory are preserved. The script copies the staged production mods and configs, replaces the optional VillagerRealms jar, adds the QA helper, and enables the example AoE stacked-villages datapack. It uses verified absolute paths to the production Forge libraries. No client is launched.

The dedicated server uses Java 17, Forge 47.4.22, `-Xms1G -Xmx6G`, loopback port 25581, view distance 6 and simulation distance 4. The existing 180,000 ms watchdog allowance permits fixture generation; it does not exclude slow measured ticks. The controller stops the server on completion or failure. The launcher treats a missing, failed or incomplete report as an error even if Java exits successfully.

## Measurements and evidence

`MinecraftServer.tickServer` HEAD/RETURN timing includes Forge END handlers and excludes the idle wait between ticks. Warmup and setup are separate. Phase deadlines use completed tick samples so a slow tick crossing the deadline remains included. Reports contain mean, p50, p95, p99 and maximum MSPT; counts over 50/100 ms; start-to-start intervals; completed ticks per wall-clock second; heap usage; and GC collection deltas. Percentiles use the nearest-rank method. GC bean collection time is not an exact pause sum; `logs/gc.log` also records GC and safepoints.

Periodic QA snapshots and report writes remain included in full tick timing. `measuredDiagnosticOverheadMs` records their cost separately; it is **not subtracted**. Reports also record population, activity, entity and chunk counts. This headless server benchmark does not measure client FPS, rendering, player traffic or network latency.

Find the newest manifest through `run/stacked-village-benchmark/latest-run.json`. Each instance retains `artifact-inventory.json` with jar/config hashes and host CPU/RAM, launch arguments, `launch.json`, console logs and its world. The result path is `run/stacked-village-benchmark/reports/<run-id>.json`, also recorded in the manifest and passed as `stackbench.output`.

`summarize-benchmark.py REPORT.json --inventory INVENTORY.json` prints compact JSON with completed-phase timings, actual production/delivery deltas and the latest worker positions. Add `--markdown` for tables suitable for a report. It uses interval-derived TPS when available and never assigns an automatic pass. Both modes only read the saved evidence and can be used while the benchmark is running.

The earlier `probe-04` report is retained as evidence from before the winding-route timeout fix. Its 300-second populated measurement exercised useful local production but recorded no cargo pickups or deposits. It is not a successful hauling benchmark, and results from later source/artifact hashes must be identified separately.

`status: completed` means the measurement sequence ended, **not that gameplay or performance passed**. For each population, check:

- Actual pickups and destination deposits during the measured interval, including both directions; warmup deliveries are tracked separately.
- Input/output conservation, including carried items and furnace contents; no missing or duplicated goods or unexplained inventory changes.
- Courier UUIDs, village/workplace identity and all requested residents remain present across dimensions.
- Workers are doing useful work, both village footprints stay ticking without players, and the result is not dominated by idle jobs.
- MSPT tails, sustained wall-clock TPS, GC pauses and diagnostic costs together. A low mean alone does not establish smooth operation.

## Saved-world recovery checks

Use `prepare-recovery.py` only after the source benchmark server has exited cleanly. It copies the saved world, residents, configuration and mods into a new instance, replaces the explicitly supplied VillagerRealms and QA-helper jars, and inventories the copied world and retained source report. It does not rebuild the village, move residents, alter the original world or launch a process.

```powershell
python tools/stacked-village-benchmark/prepare-recovery.py --source-manifest run/stacked-village-benchmark/runs/full-01/artifact-inventory.json --vr-jar C:/Users/andre/Downloads/VillagerRealmsMod/build/libs/villager_realms-1.0.0.jar --helper-jar tools/stacked-village-benchmark/build/libs/stacked-village-benchmark-1.0.0.jar --run-id recovery-01
.\tools\stacked-village-benchmark\run-benchmark.ps1 -PreparedManifest run/stacked-village-benchmark/runs/recovery-01/artifact-inventory.json
```

The recovery controller receives `stackbench.recovery=true`, the retained `stackbench.sourceReport`, a new output path, a default 600-second deadline and a 60-second stability period. Its saved-world behavior and outcome are a separate check on separately hashed artifacts, not replacement measurements for `full-01`. The original report, world and failure evidence remain intact.

Recovery holds the Overworld clock at 18000 and observes ordinary AI without constructing fixtures, moving residents or supplying goods. It checks the recorded courier's starting location and identity, continued village/house/workplace membership, physical movement, all resident UUIDs, and paired entity-ticking chunks. All residents must sleep in their assigned houses and remain asleep together for 60 continuous seconds. Residents may briefly wake for normal needs; `allSleepingAfterMeasuredSeconds` and the legacy-named `firstAllSleeping` snapshot describe the start of the latest all-sleep interval, not necessarily the courier's first bed arrival. Recovery includes its own observation costs and is not a repeat of the daytime capacity workload.
