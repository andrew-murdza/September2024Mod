# Angling Forge 1.20.1 — release test record

Test date: 2026-10-18. Candidate version: **1.0.1-forge.1**.

The port targets Minecraft **1.20.1**, Forge **47.4.0**, GeckoLib **4.4.9 for Forge**, and Java **17**. The measured environment was Windows 11 with Eclipse Temurin 17.0.9. Use the remapped JAR with GeckoLib installed on both client and server. The GitHub Actions workflow also defines Linux and Windows builds; those hosted jobs have not been run in this session.

## Automated results

- **13/13 Forge GameTests passed** in a real dedicated-server process. Coverage includes all 13 entity types, all 12 fish/creature buckets, native and legacy fish NBT, growth-locked tadpoles, roe and slug eggs, species/variant inheritance, fry feeding and growth, block entities, fish feeding, worm blocks, oyster reproduction, pelican trades, variant tags, biome modifiers, recipes, and advancements.
- Hatching tests call the scheduled tick directly. They verify the resulting animals and water replacement, including the animals' collision boxes; they do not wait the natural hatch interval.
- Independent resource and JAR auditing checks 212 JSON files, 42 items, 14 blocks, 13 entity types, and 37 animation definitions, as well as packaged resources, loader/dependency metadata, mixin refmaps, access transformations, Java versions, remapping, and block-atlas membership. The exact count, final JAR hash, and runtime log hash are in `validation.json` in the release folder.
- Test classes and structures are in a separate source set and excluded from the release JAR. No Fabric API, Cardinal Components, GeckoLib, or Minecraft classes are bundled in Angling.

## Installed server results

A clean Forge installation under `run/production` loaded the remapped Angling JAR with only the required GeckoLib mod. The server was bound to loopback for local testing.

- Created a new world with seed `8120201`; spawned all 13 creatures; enabled AI on the 12 adult creature types after the initial persistence check.
- Saved, stopped normally, restarted, and confirmed all 13 tagged creatures remained. Queried saved sunfish appearance/breeding data, rainbow starfish data, and an urchin's diamond helmet.
- Reloaded data packs successfully, including all five Angling advancements.
- Generated fresh chunks in warm ocean, swamp, mangrove swamp, and savanna. Executed all six configured feature types successfully. Worm generation was also checked against a prepared dirt volume, where the expected wormy dirt block was verified.
- No Angling registry, mixin, data-pack, or entity-tick exceptions occurred in these successful server runs. Bulk biome searches and chunk generation produced temporary server-lag warnings during concurrent tests; this was not a performance benchmark.

## Client and networking

The installed client uses the stock Forge launcher target and the remapped release JAR. A startup and server join succeeded with Angling and GeckoLib alone. A separate, local test companion then captured game-framebuffer screenshots and client state; it is excluded from the release JAR. Native desktop window control stalled, so these graphical tests use the game's own rendering and screenshot APIs.

- Two simultaneous installed clients joined the separate dedicated server successfully.
- Both clients received the same live sea-slug update from jade/coquelicot stripes to ultramarine/gamboge spots, retaining bioluminescence. Both also received the starfish color change from `16733440` to `65535` and rotation from `35` to `90`.
- After a full server and client restart, both clients observed the same slug UUID and the updated slug and starfish values. The six before/after/restart observations are recorded in `two-client-sync-validation.json`.
- Reviewed aquarium and close-up screenshots show textured fish, the slug's pattern layer, the anemone, a stone hat attached to an urchin, and tinted bucket/item models. The custom acquisition advancements fired when the test player received roe and slug eggs.
- Final screenshots confirm that the pelican carries its fish inside its lower jaw and that the Angling creative tab displays its textured items, buckets, plants, and spawn eggs.
- Resource reload completed with the aquarium loaded. The final client runs have no missing Angling texture/model errors. Vanilla shader sampler and offline-authentication warnings are retained in the logs and do not indicate Angling failures.

## Fixes found by testing

Runtime tests led to fixes for Forge registry timing, custom registry tag paths, advancement predicates, server-side egg item data, dead-starfish appearance, bucket and legacy breeding persistence, mature frozen tadpoles, and creative inventory conservation. A positioning regression affected slug/roe hatching, fry growth, and pelican releases: their raw coordinates changed without updating collision boxes. All four paths now use the position setter that updates the collision box, with regression assertions. Installed-client testing also exposed an anemone texture missing from the new block atlas and a detached pelican fish attachment; the atlas declaration, validator, and held-fish offset now cover those issues.

## Publication and remaining coverage

Recommended initial CurseForge release type: **beta**. Longer survival playthroughs, natural breeding/growth timing, multi-seed spawn distribution, large modpack compatibility, shader/performance-mod compatibility, and migration of complete Fabric worlds have not been certified. See `TESTING.md` for the repeatable procedures and remaining checks.

Retain the CC0-1.0 license and credit **EightSidedSquare, Diansu, and Sillvia**. This is an unofficial port of upstream commit `e0c99ab904cc3d0e5ece2d04a39347b5c1e346ea`. The release folder contains the player JAR, a source JAR, the complete buildable source ZIP, checksums, and publication notes. No CurseForge project or file has been published by this work.
