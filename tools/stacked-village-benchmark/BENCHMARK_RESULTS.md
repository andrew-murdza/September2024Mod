# Paired-dimension village benchmark — full-01

**The full run maintained approximately 20 TPS through 128 residents and completed all three courier routes in every daytime phase.** It also exposed a nighttime home-route failure. That failure was corrected and the actual stranded courier subsequently walked home in a copy of the saved village; all 128 residents then slept in their claimed houses for 60 seconds. The original performance measurements and the later functional recovery use separately recorded VillagerRealms artifacts. The original failure evidence remains intact.

## Reproducible setup

The isolated `full-01` run uses the complete production pack, optional VillagerRealms and the QA helper in a fresh generated world. It leaves existing gameplay worlds untouched. The benchmark runs on an Intel Core i7-13700KF (24 logical processors), with 31.84 GiB physical RAM, Java 17.0.9, Forge 47.4.22 and a dedicated-server heap of `-Xms1G -Xmx6G`. View distance is 6 and simulation distance is 4. No client, real player or fake player is connected.

One registered village spans the Desert and Nether. Its 256×256 footprint intersects 289 village-ticket chunks in each dimension. The server also reports 2,025 loaded chunks per dimension; loaded chunks and village ticking tickets are different counts. The fixture creates rooms, beds, food storage, workplaces and physical winding stairs. Desert workplaces are at Y74 and the lower Nether workplaces at Y18. Reaching the seam landings entails 104 vertical blocks on the Desert side and 107 on the Nether side, plus horizontal approaches.

The population steps are 32, 64 and 128 **total residents, including three long-distance couriers**. The remaining workers are mostly local librarians using eight libraries with 40 linked paper workstations (32 lecterns and eight crafting tables). This is one paired village with three concurrent hauling routes, not a test of 128 simultaneous haulers, many villages, all professions or client FPS.

Each population has 60 seconds of warmup followed by 600 seconds of measurement. The empty paired-village baseline has 60 seconds of warmup and 60 seconds of measurement. Population comparisons hold daytime; the final 128-resident soak advances the work/sleep clock for 600 measured seconds after warmup. That soak covers the evening and night transition, not an entire 20-minute Minecraft day cycle.

Before each populated phase's warmup, normal storage transactions restore background granaries toward 1,024 cane each and export completed paper from the background library chests. The report records every setup-added input and exported output. No stock adjustment occurs during measurement, and cargo rooms and carried worker inventories are excluded from these background adjustments. The QA paper target is capped at 1,024 per library; the book target is four. House meals and furnace input are finite. The eight background furnaces start with 64 raw gold each and are not replenished between phases; their 512 smelts had finished during the first populated phase. Later phases therefore do not contain sustained furnace input processing.

Measured SHA-256 hashes from `full-01/artifact-inventory.json`:

| Artifact | SHA-256 |
| --- | --- |
| AoE | `95caa445bcdbe0e003d0d468f25778c2aa8d4868de36b27cd2e605e053c292fc` |
| VillagerRealms | `f6ae7e07e03836ec677fdc177550eb591fa5c871a775df63b176a4636ab42dd6` |
| QA helper | `ca2aa301cb383b1e74928bd2e034322d2e479e39227b7999add9aee8ef13b230` |

The inventory retains the complete mod, configuration, datapack, Java and launch-file hashes. See [README.md](README.md) for invocation and the workload definition.

## Measurement method

Timing wraps `MinecraftServer.tickServer` at HEAD and RETURN, including Forge END handlers and VillagerRealms work. It excludes idle waits between ticks. Setup and warmup are outside measured phases. A slow tick crossing the phase deadline remains included; only the currently unfinished tick is excluded at finalization. MSPT percentiles use nearest rank. Reported TPS below is `1,000 / mean tick-start interval`, avoiding the small finite-window bias in completed-ticks/sample-wall-time.

Periodic QA snapshots and JSON writes remain inside measured tick times. Their separate diagnostic timing is reported without subtraction. Slow-tick records retain up to the 100 longest ticks over 50 ms, with offsets for later correlation. GC figures below are management-bean collection counts and millisecond totals, not exact stop-the-world pause sums. The retained GC/safepoint log can support a separate tail audit after measurement; no cause is assumed merely from a high maximum tick.

Work is assessed by actual source pickups and destination output deposits, not navigation starts or unchanged stock. Background conservation includes storage and carried goods: `current cane + current paper + exported paper = initial 8,192 cane + setup-added cane`. Cargo conservation also accounts for input/output conversion. Courier UUIDs, village/workplace assignments, survival and both dimension ticket counts are checked alongside timings.

## Completed phases

| Phase | Measured seconds | Interval TPS | Mean MSPT | p95 | p99 | Maximum | Ticks >50 / >100 ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Empty paired village | 60.010 | 20.0000 | 9.11 | 12.80 | 14.62 | 21.05 | 0 / 0 |
| 32 residents | 600.011 | 20.0000 | 11.70 | 15.42 | 18.37 | 149.18 | 5 / 2 |
| 64 residents | 600.014 | 20.0000 | 13.32 | 16.81 | 19.36 | 89.13 | 6 / 0 |
| 128 residents | 600.015 | 20.0000 | 15.80 | 19.62 | 23.93 | 91.58 | 5 / 0 |
| 128 residents, evening/night soak | 600.014 | 20.0000 | 12.48 | 17.10 | 21.30 | 87.99 | 72 / 0 |

| Phase | Local paper produced | Rabbit picked → cooked rabbit deposited | Nuggets picked → golden carrots deposited | Cane picked → paper deposited | Residents present | Conservation |
| --- | ---: | --- | --- | --- | ---: | --- |
| Empty paired village | 0 | No couriers | No couriers | No couriers | 0 / 0 | Background input/output conserved |
| 32 residents | 3,294 | 8 → 8 | 32 → 4 | 12 → 12 | 32 / 32 | Background and all three cargo streams conserved; no recorded violations |
| 64 residents | 4,368 | 8 → 8 | 32 → 4 | 12 → 12 | 64 / 64 | Background and all three cargo streams conserved; no recorded violations |
| 128 residents | 3,636 | 8 → 8 | 32 → 4 | 12 → 12 | 128 / 128 | Background and all three cargo streams conserved; no recorded violations |
| 128 residents, evening/night soak | 231 | 0 → 0 | 0 → 0 | 0 → 0 | 128 / 128 | Tracked goods conserved; nighttime home route failed |

The empty baseline contained 1,201 completed tick samples, three GC collections totaling 24 ms by the GC beans, and two diagnostic snapshots totaling 10.63 ms (maximum 6.78 ms). It retained 289 village-ticket chunks per dimension, with zero players.

The 32-resident phase contained 12,001 completed tick samples. All three couriers completed one round trip, retained their UUIDs and returned with empty cargo after depositing output. All 32 residents were alive, with 16 in each dimension at the final snapshot; both dimensions retained 289 village-ticket chunks and zero players. All 29 background librarians remained eligible for their assigned workshops.

The 64-resident phase also contained 12,001 completed ticks and one completed round trip per courier. All 64 residents were alive, split 32 per dimension at the final snapshot. Courier UUIDs and assigned workplaces matched the phase's starting snapshot; all courier workplaces and sources remained valid. All 61 background librarians were eligible. Both dimensions retained 289 village-ticket chunks with zero players.

The 128-resident daytime phase contained 12,001 completed ticks and another completed round trip per courier. All 128 residents were alive, split 64 per dimension, and all three courier UUIDs matched their starting values. Both village footprints still held 289 ticket chunks with zero players. All 125 background librarians were eligible, but output fell to 3,636 paper compared with 4,368 at 64 residents, despite ample cane. The final snapshot recorded 32 residents “Working,” eight “Going to work,” two “Looking for work,” and 86 “Unable to work: no available tasks or supplies.” These are point-in-time states, not whole-phase utilization averages. This is not evidence of 128 workers being continuously productive. The fixed station count and task-reservation contention are plausible explanations for waiting; this run does not isolate their individual effects.

| Completed populated phase | Eligible background workers | Cane at start → finish | Paper at start → finish | Cumulative setup-added cane | Cumulative setup-exported paper |
| --- | ---: | ---: | ---: | ---: | ---: |
| 32 residents | 29 | 8,135 → 4,841 | 57 → 3,351 | 0 | 0 |
| 64 residents | 61 | 12,136 → 7,768 | 144 → 4,512 | 7,424 | 3,336 |
| 128 residents | 125 | 15,672 → 12,036 | 303 → 3,939 | 15,616 | 7,833 |
| 128 residents, evening/night soak | 125 | 19,889 → 19,658 | 348 → 579 | 23,808 | 11,763 |

Stock totals include items carried by background workers, so restoring granary chests does not make every phase's total input identical. The 40 paper workstations remain fixed as population increases. Eligibility establishes valid work assignments, not that every worker is continuously busy; actual paper production is reported separately. The finite background furnaces had no raw gold remaining and 512 gold ingots at the end of every populated phase.

During the first populated phase, the three first deliveries occurred 453.26 seconds after courier spawn for paper, 502.66 seconds for cooked rabbit, and 509.61 seconds for golden carrots. Those are spawn-to-first-deposit latencies, including warmup, route discovery, travel and processing; they are not pure walking times. The three daytime intervals together recorded 24 raw rabbit → 24 cooked rabbit, 96 gold nuggets → 12 golden carrots, and 36 cane → 36 delivered paper. No courier picked up or deposited its tracked goods during the final evening/night measurement.

The soak advanced the Overworld clock from 11,201 to 23,203. Its final snapshot retained all 128 living residents, zero players and 289 village tickets per dimension. It recorded 127 residents sleeping. The remaining gold courier, UUID `7063651e-25c6-46a9-be57-4a9ac7be95a9`, was stationary at the Desert seam `(202056.5, -30, 201032.5)` with the thought “Unable to sleep: cannot find a path home.” Its workplace remained valid and its carried inventory contained one carrot. All tracked cargo and background conservation checks remained true, with no recorded cargo violation. The failure is reaching its claimed home after cross-dimension travel, not lost tracked cargo or resident death.

### Tick tails and measured observer cost

| Completed phase | GC collections / bean milliseconds | Logged GC pause total / maximum | QA snapshots | QA total / maximum milliseconds |
| --- | ---: | ---: | ---: | ---: |
| Empty paired village | 3 / 24 | 24.092 / 10.680 ms | 2 | 10.63 / 6.78 |
| 32 residents | 65 / 154 | 159.170 / 6.054 ms | 20 | 90.69 / 6.35 |
| 64 residents | 90 / 184 | 190.201 / 6.251 ms | 20 | 104.79 / 9.95 |
| 128 residents | 134 / 255 | 265.082 / 8.512 ms | 20 | 114.82 / 10.67 |
| 128 residents, evening/night soak | 81 / 157 | 162.871 / 4.868 ms | 20 | 106.28 / 7.47 |

The 32-resident phase had five ticks above 50 ms (0.042% of samples), including two above 100 ms. Those two ticks took 149.180 and 103.357 ms and began about 105.950 and 405.949 seconds after the first measured tick. Thus average TPS remained 20, with occasional individual stalls rather than uniformly sub-50 ms ticks.

The saved slow-tick offsets were compared with the GC log's wall timestamps and JVM-uptime timestamps. The first measured HEAD is bounded within approximately 98 ms after the phase's recorded `before` snapshot; the correlation used that full uncertainty window. No GC pause overlaps any of the five candidate slow-tick windows. The two ticks above 100 ms have only nearby `Cleanup` safepoints of 0.0555 and 0.0433 ms; the largest safepoint anywhere in the phase is 6.323 ms. Neither these pauses nor the separately measured maximum diagnostic cost explains the long ticks. Their exact cause remains unassigned; a 300-second separation between the two longest ticks alone is not proof of a cause.

The final soak had 72 ticks over 50 ms (0.600% of its samples), more frequent than the daytime phases, despite a lower mean tick time. Two of those slow-tick windows possibly overlap GC pauses of 1.716 and 1.888 ms; neither pause by itself explains the corresponding 64.939 and 65.671 ms tick. The largest logged safepoint in the soak was 5.005 ms. No slow daytime tick has a candidate GC overlap under the recorded bounds. These measurements support absence of sustained tick-budget overload for this workload, while still showing occasional hitches; they do not support a claim of uniformly hitch-free operation.

## Nighttime recovery check — passed

Review identified a path-length limit affecting the stranded courier's long home route, including the 104-block Desert climb and approach to its house. The corrected sleep route uses bounded waypoints. Its release build passed 392 unit tests and 976 runtime tests. Recovery used a separate copy of the completed `full-01` world, preserving the actual 128 saved residents and the courier's recorded stranded position. Only the recorded VillagerRealms and QA-helper artifacts changed; the original world and report remain untouched.

The observer held night at 18,000, waited for the saved residents and both ticking village footprints, and allowed ordinary AI up to 600 seconds to reach their claimed beds and remain asleep for 60 seconds. It did not rebuild fixtures, teleport residents, invoke movement controllers or replenish stock. Its per-tick readiness and resident checks add observer work, so these recovery timings are a separate functional diagnostic, not a replacement capacity benchmark.

The staged `recovery-01` instance contains 115 copied world files totaling 32,941,018 bytes; each was verified against the stopped source world. AoE retains its original hash. The replacement VillagerRealms SHA-256 is `a0810e8c9b013a63334fab3a099ec02980f59677701dce6f1e9c2f7dacddcf17`; the recovery helper SHA-256 is `640d0fa7a61bc1c72fb94a025b2972da78e0cc6456006883d48fde8888b6ac6c`. The recovery inventory also hashes the retained full-run report and source manifest. No performance results from `full-01` are silently reassigned to these new artifacts.

The saved residents and ticking chunks became ready after 5.04 seconds. The observer first found the same gold courier at exactly `(202056.5, -30, 201032.5)`, with its original house and workplace, one carried carrot, and the queued request for 32 nuggets. Ordinary AI took it up the staircase and across the surface to bed `(201951, 74, 200927)`. The recorded cumulative walking distance was 531.85 blocks. The group reached the successful all-sleeping stability period after 215.69 measured seconds and remained asleep for another 60 seconds. All 128 identities were unchanged and alive; all claimed-house checks passed, both dimensions retained 289 ticking tickets, and no players were present. The courier retained its house, workplace, carrot and queued request.

The recovery measured 5,516 complete ticks over 275.735 seconds: interval-derived TPS 20.0018, mean/p95/p99/max MSPT 11.39/15.38/19.68/46.82, and no ticks over 50 ms. GC beans recorded 19 collections totaling 105 ms. Nine progress snapshots cost 49.10 ms in total (maximum 14.33 ms); four claimed-house validations cost 0.594 ms in total. All observer costs remain included. The report explicitly records `passed: true`; the server exited cleanly with code zero at `2026-10-23T05:40:21.324Z`.

## Earlier failures and interpretation limits

The retained `probe-04` report used an earlier VillagerRealms artifact. It exercised 29 background workers and three couriers, producing 1,422 local paper in its 300-second populated interval, but all three couriers reversed empty and recorded zero pickups or deposits. The winding-stair route exceeded the old Euclidean-distance navigation timeout. That timeout and its regression coverage were corrected before `full-01`. Fixture height selection and linked-workstation setup were also corrected before this run. Earlier probe timings are not interchangeable with this run's hashes and workload.

An idle courier after exhausting a finite batch is not ongoing hauling load. Likewise, low mean MSPT alone does not establish smooth operation. Final interpretation must consider completed physical work, conservation, surviving identities, tick tails, GC/diagnostic costs and the final soak together. `status: completed` in the JSON means the sequence ended; it is not an automatic gameplay or performance pass.

## Retained evidence

- Report: `run/stacked-village-benchmark/reports/full-01.json`
- Instance and artifact inventory: `run/stacked-village-benchmark/runs/full-01/`
- GC/safepoint log: `run/stacked-village-benchmark/runs/full-01/logs/gc.log`
- GC correlation audit: `run/stacked-village-benchmark/reports/full-01-gc-audit.json`
- Saved-world recovery report: `run/stacked-village-benchmark/reports/recovery-01.json`
- Recovery inventory and copied save: `run/stacked-village-benchmark/runs/recovery-01/`
- Compact JSON or Markdown summaries: `tools/stacked-village-benchmark/summarize-benchmark.py`

The source measurement finished at `2026-10-23T05:22:06.460Z`; the server exited cleanly with code zero at `05:22:08.253Z`. It contains 49,205 complete tick samples across 2,460.064 measured seconds. Both isolated instances, worlds, logs, artifact inventories and the earlier probes are retained for review.
