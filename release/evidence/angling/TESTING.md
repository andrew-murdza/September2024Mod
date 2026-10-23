# Testing the Forge 1.20.1 port

Use a Java 17 JDK and the included Gradle wrapper. The build targets Minecraft 1.20.1, Forge 47.4.0, and GeckoLib 4.4.9. Python 3.11 or newer runs the independent resource/package validator.

## Automated checks

Run from the repository root in PowerShell:

```powershell
.\gradlew.bat build compileGametestJava
python tools/validate_release.py --jar build/libs/angling-forge-1.20.1-1.0.1-forge.1.jar --report build/reports/release-validation.json
.\gradlew.bat runGameTestServer
python tools/validate_release.py --gametest-log run/gametest/logs/latest.log --report build/reports/gametest-validation.json
```

`build` produces the remapped jar for players. Do not upload a `-dev`, `-sources`, or `-javadoc` jar. `src/gametest` is a separate development source set; it must not appear in the release jar.

The Python validator checks duplicate/invalid JSON, local item/block/tag references, recipes and loot references, model/texture/sound files, block-atlas registration of entity textures used by models, animation references, all original entities and features, Forge custom-registry tag folder paths, translations, loader metadata, dependency metadata, mixin refmaps, Java class versions, and accidental Fabric/GeckoLib 3/test content in the release jar. Packaged resources must match the source tree and the jar version must match `gradle.properties`. It writes measured counts and the jar SHA-256 when `--report` and `--jar` are supplied. External Minecraft/Forge references are resolved by runtime tests, not guessed by the static validator.

Thirteen Forge GameTests run in a real dedicated-server environment:

1. Construct and save/reload all 13 entity types, including health, names, and representative variants.
2. Serialize/release all 12 bucketable types, including appearance, health, and fish breeding state.
3. Persist breeding timers/flags/mate data for vanilla and mod fish; migrate legacy Cardinal Components NBT; preserve the default ability to grow when old NBT lacks a flag.
4. Release a mature, growth-locked tadpole from a bucket without prematurely turning it into a frog.
5. Transfer roe item NBT on server placement, hatch it, and verify fry count, parent species, and inherited sunfish variant.
6. Transfer sea-slug egg item NBT on server placement, hatch it, and verify viable appearance and bioluminescence.
7. Load all configured/placed features, verify biome-modifier application, resolve all four custom variant registries' tags, and load all mod advancements and recipes.
8. Save/reload the six states using Angling block entities, including starfish appearance/item placement and a named urchin hat.
9. Feed vanilla and mod fish in survival and creative, enforcing love/cooldown states and the breedable-species tag.
10. Insert, cap, and harvest worms from dirt/mud, restoring the base block after the last worm and conserving creative items.
11. Exercise algae feeding with deterministic random samples, consume its faces, preserve water, and reproduce oysters after five meals.
12. Trade fish buckets with pelicans in survival and creative, preserve variants, and release the previous catch.
13. Accelerate fry growth with worms, freeze growth with fermented spider eyes, and transform an unlocked fry into one persistent adult with the inherited sunfish variant.

The GameTest server writes its Minecraft log to `run/gametest/logs/latest.log`. A successful Gradle build alone does not show that runtime GameTests passed. The log validator requires the full expected test count, a success summary, normal shutdown, and no relevant registry/tag/advancement/mixin failures. These tests accelerate hatch scheduling by invoking the block's scheduled tick; they do not wait the full natural hatch interval.

## Required visual and gameplay acceptance

The optional [client test companion](qa/client/README.md) can capture the rendered game, open the Angling creative tab, reload resources, and snapshot synchronized client data in an isolated profile. It is built separately and is never included in the player JAR. The completed graphical and networking results are in [RELEASE_REPORT.md](RELEASE_REPORT.md).

These checks need a real Minecraft client. Record the client version, Forge/GeckoLib versions, seed, and results; do not mark them passed merely because the server tests succeeded.

- Open the creative inventory and inspect all Angling items, spawn eggs, buckets, translated names, tooltips, and models.
- Spawn every entity. Inspect swimming, land/flop behavior, sounds, glow/overlay layers, crab legs, pelican held entities, and resource reload with F3+T.
- Place the six animated/block-entity states under water and inspect colors, orientations, animation, transparency, water rendering, and chunk reload.
- Capture and release each bucketable type. Check non-default sunfish/crab/slug variants, dongfish without its horn, and fry growth target before and after save/rejoin.
- Feed compatible fish worms, observe mating and roe laying, harvest/replant roe, and let it hatch naturally. Repeat sea-slug egg breeding and inheritance; verify fry growth and fermented-spider-eye growth prevention.
- Check pelican spawning, feeding, trading, carried mobs, and despawn behavior; verify advancement triggers and recipes.
- Generate a new world and inspect oyster reefs, clams, duckweed, sargassum, papyrus, wormy dirt/mud, and natural mobs in their tagged biomes. Use several seeds and visit overworld ocean, swamp, mangrove, savanna, shore, and mushroom biomes.
- Join a separate dedicated server with two clients. Check synchronized variants/colors, breeding and buckets, reconnect, chunk unload/reload, and a normal server restart.
- Test the actual remapped release jar in a clean Forge profile with only required GeckoLib, then repeat with any performance mods advertised as compatible.

Fabric save migration only covers the mod's recognized legacy entity NBT. It is not a guarantee that an entire Fabric modpack world can be moved to Forge.

## Measured status

On 2026-10-18, all 13 required Forge GameTests passed with Java 17, Minecraft 1.20.1, Forge 47.4.0, and GeckoLib 4.4.9. The server completed normally and Gradle reported success. These tests caught and verified fixes for advancement decoding, server-side item placement, dead-starfish item state, and entity spawning with stale bounding boxes.

The static resource validator passed across 212 JSON files, 42 registered items, 14 blocks, 13 entities, and 37 animation definitions. A negative regression check also confirmed that removing the atlas registration rejects both anemone and urchin models. The combined final package/runtime report is generated at `build/reports/release-validation.json` and includes the checked jar and test-log hashes.

Client, production-server, and multiplayer acceptance results are recorded separately in the final release report. The automated result above does not imply the manual acceptance list has been completed.
