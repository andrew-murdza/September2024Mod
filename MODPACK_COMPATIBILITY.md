# Modpack compatibility

The exact supplied Forge jars are recorded in `dependency-lock.json`; Angling is required on client and server. Original dependency jars are unchanged. The runtime dependency audit loaded the complete pack and reached all 47 initial GameTests on 2026-10-19. That first run passed 42 tests and exposed five gameplay regressions, which are tracked separately; it was not a release pass.

## Targeted data repairs

These overrides apply to the pinned versions, not arbitrary newer releases. Re-audit them when updating dependencies.

| Source | Repair and preserved behavior |
| --- | --- |
| VintageDelight 0.1.6 | Define the missing `forge:fruits/gearo_berry` and `forge:protein/peanut` leaf tags using the supplied canonical berry/peanut tags. All other mods' contributions to fruit/protein remain merged. |
| OuterEnd 1.0.13 distribution (internal version 1.0.0) | Replace both Quark plural vertical-slab tags, retaining Quark's canonical singular tag. Seven unregistered OuterEnd integration slabs are explicitly optional. The actual Quark slabs remain required. |
| Hearth & Harvest 1.4.1 | Translate the old `minecraft:alternative` loot condition to `minecraft:any_of` in five wild crops. Original chance, fortune, silk-touch and shear behavior is retained. Gate the palm-half-cabinet recipe advancement on the actual optional item existing. |
| Cultural Delights 6029441 | Translate old avocado-leaf loot conditions. Correct the exotic-roll-medley registry identifier and provide the canonical block loot path. Replace three zero-byte vanilla stew recipe files with valid false-conditioned recipes, retaining their disabled status and AoE's existing replacement stews. |
| Naturalist 5.0pre2 | Use Minecraft 1.20.1's `minecraft:scute` identifier for tortoise drops, retaining counts and looting. |
| Collector's Reap 1.5.5 | Restore the missing conditional crab-noodles advancement body, following the neighboring prawn-noodles advancement and its crab-meat ingredient. |
| OuterEnd / Phantasmic | Correct the catacombs icon to registered `chiseled_violite_bricks`; attach Strange Elixir to the existing Nourished Nether root instead of the deleted placeholder parent. |

Six upstream orphan loot resources have no corresponding registered block or item in this pack: OuterEnd `azure_post`, `stripped_azure_post`, `azure_vertical_slab`, `himmel_vertical_slab`, and Cultural Delights `avocado_bundle`, `avocado_leaf_carpet`. Explicit empty tables prevent their unused invalid data from failing resource reload. No registered block loses its drops. These six exceptions should be removed if a future dependency version restores those blocks.

The existing recipe policy retains only Minecraft, AoE, Quark and the two backport namespaces. It now filters excluded namespaces before recipe decoding, avoiding failures from optional serializers for recipes the pack would remove anyway; the final filter remains as protection against other mixins inserting recipes later. This does not broaden the existing recipe restrictions.

## Distinct BetterEnd and AoE content

Registry IDs and saved entities are preserved. AoE's custom snowy End Fish and BetterEnd's desert/sulfur End Fish remain distinct. AoE's lava-adapted Cubozoa is displayed as **Lava Jellyfish**; BetterEnd's aquatic Cubozoa remains aquatic. No migration merges those variants or removes their unique behavior.

## Regression checks

`PackCompatibilityTests` verifies merged food/Quark tags, actual shearing drops for seven repaired crop/leaf blocks and full exotic-roll-medley drops, restored functional advancements, replacement stew availability, and the unchanged recipe namespace policy. The pre-final complete server run on 2026-10-19 passed all 55 required GameTests, including all three pack compatibility tests. The runtime log contained no tag, recipe, loot-table, or advancement decoding errors. The only remaining ERROR-level lines were three upstream mixin configs omitting the optional minVersion property (Fins and Tails, Ferdinand's Flowers, and YACL); loading continued successfully.

Run `python scripts/validate-pack-dependencies.py --report build/reports/pack-dependencies.json` for exact supplied-jar hashes and required dependency IDs. A real Forge startup is still necessary to validate version ranges, mixins and data loading.

## Isolated production validation

`production-dependencies.json` extends the requested-pack lock to all 70 runtime artifacts: 69 original standalone dependency jars and MixinExtras 0.5.0 embedded in AoE's reobfuscated `-all.jar`. The initial inventory found 169 mandatory dependency edges and no missing mod IDs. Artifact hashes and original archive CRCs were checked. Third-party binaries are not included in a release bundle.

Run `python scripts/prepare-production-mods.py --release build/libs/aoemod-1.0.0-1.20.1-all.jar` to verify and stage them in `run/september-production/mods`. The script rejects mismatched hashes, unexpected extra jars and a release missing its embedded MixinExtras or production refmap. It does not touch a live launcher profile.

The local test server uses official Forge 47.4.22 and Java 17. Its isolated launcher is `run/september-production/launch-server.ps1`; enter `stop` on standard input for a clean save/shutdown. The QA configuration binds only to `127.0.0.1:25579` in offline mode, with a test operator account for controlled multiplayer checks. These test settings are not suitable for a public server.

## Data generation only

Environmental 4.1.2 links GalleryItemModelProvider when Forge dispatches GatherDataEvent, including AoE-only datagen. Its [official 1.20.x build](https://github.com/team-abnormals/environmental/blob/1.20.x/build.gradle) uses Gallery 1.20.1-1.0.2. The isolated `datagenRuntime` configuration supplies that original jar only to `runData`; it is absent from normal clients, servers, release metadata, and the production dependency inventory. `datagen-dependencies.json` records the exact source and SHA256. A subsequent isolated `runData` run completed successfully in 46 seconds after supplying both documented integrations. AoE currently has no GatherData providers; it produced no source changes.

Gallery loaded successfully in the following datagen attempt; the next foreign provider exposed FruitsDelight's unconditional CuisineDelight integration. Its [official build](https://github.com/Minecraft-LightLand/FruitsDelight/blob/1.20/build.gradle) supplies `cuisinedelight-1.1.12-slim.jar` directly in the upstream repository. The same exact jar is now isolated in `datagenRuntime`; its required L2Library and Farmer's Delight are already present. Neither datagen-only jar is added to production.

The later full runtime suite passed all 56 required GameTests before the final pale-oak growth adjustment. The packaged production server and client/restart checks are tracked separately from datagen.


## Additional client repairs

Born in Chaos ships five bare `-` tokens in four GeckoLib animation files. A client-side adapter changes only those exact invalid rotation components to neutral zero before baking; valid upstream or resource-pack values are untouched. No complete animation file is copied into AoE. Client QA checks that the four repaired resources actually have baked animations.

Ice and Fire's dragon-eye loader passed a leading slash to `ClassLoader.getResourceAsStream` and requested uppercase `Ground` while its existing archives use lowercase `ground`. The client adapter normalizes those resource paths, restoring the packaged eye geometry without substituting a model.

Eleven missing English sound subtitle keys are supplied. OuterEnd's two music events retain their original sound lists without an empty subtitle key, and Alex's Caves' registered `luxtructosaurus_breath` event points to its bundled `luxtructosaurus_breathe` event. No audio is synthesized or copied.

The original template author names were removed from release metadata; the description now identifies this project. Set the project's real maintainer/credit and support information before public publication.

## Client model compatibility

The first real client resource load exposed missing/renamed model references in the pinned dependencies. The 102 targeted JSON repairs preserve the original meshes, UV coordinates, texture bindings and display transforms:

- Farmer's Delight's seven old template names and Blueprint's vertical-planks template resolve to the installed canonical templates.
- Pineapple Delight's stage-zero crop, 55 Born in Chaos custom exports (including IcySweetness), and Biodiverse's egg retain every element while removing invalid editor-only parent names. No self-referencing parent cycles are introduced.
- Bountiful Critters' five inner-stair references use their actual namespace; salt-dust and didgeridoo aliases select existing geometry. Unreachable inherited roasted-pillbug bite states use its existing final-bite mesh; normal eating removes the block after bite three.
- Collector's Reap's two crop aliases use the real upper-stage models. Its bush multipart definitions retain all reachable state geometry and allow the intentionally absent upper half of young bushes.
- Floral's 23 missing inventory models and Vintage Delight's vinegar jar reuse their existing block geometry.

A static audit followed 181 transitive model chains against the original runtime jars and Minecraft assets with no missing parent or concrete texture references. The initial model warnings and audit details are recorded locally in `build/reports/client-model-compatibility.json`. This check does not substitute for a client render pass. Hybrid Aquatic's message-in-a-bottle and two buoys explicitly use ENTITYBLOCK_ANIMATED and registered Gecko block-entity renderers; no fake vanilla geometry replaces them.

The first reobfuscated production server reached normal dedicated-server startup with all 70 staged jars and generated its dimensions without dependency linkage or data decoding failures. That build predates the final tree and client-asset corrections; final client, route, reload and restart results must be recorded separately.


The final model audit includes 104 source adapters and 184 transitive model chains, with zero reference errors. The follow-up client bake exposed two additional exact texture mistakes: legacy Farmer's Delight `tray_top` now aliases the installed original `tray` sprite through the block atlas, preserving existing model UVs; OuterEnd's two tall mossy wall models use `block/` instead of the invalid `blocks/` texture prefix. Run `python scripts/validate-client-model-compat.py --report build/reports/client-model-reference-validation.json` to repeat the model-chain audit.

## Audio and remaining upstream diagnostics

Verified sound repairs reuse only original recordings. Ambient Additions' Iiwi and Hoatzin call `honeycreeper.*`, while the pack defines the same bird recordings as `iiwi.*`; three event aliases restore ambient, hurt and death playback. Alex's Caves' cave-mood event aliases the original four mood recordings; Luxtructosaurus breath uses the actual `breath_0/1/2` filenames. Naturalist's giraffe ambient event uses the packaged `mob/giraffe_idle.ogg` path. SoundManager merges event keys across resource packs, so these narrow overrides retain unrelated sound definitions.

Some pinned upstream jars do not include their declared recordings. Entirely absent events are Alex's Caves `luxtructosaurus_snort` and Biodiverse `hippo.hurt`, `jaguar.ambient`, and `jaguar.hurt`. Partially absent events are Alex's Caves purple-soda swimming, Biodiverse lion/reindeer ambient calls, and Environmental zebra/zorse/zonkey hurt; existing samples remain playable. These omissions are documented rather than replaced with unrelated audio or silently suppressed. Three Naturalist legacy events and Hybrid Aquatic's sirenian eating event have no playback callsite outside their registries. Two missing vanilla goat/horn definitions are unchanged baseline warnings. Exact sample lists and field-reference evidence are recorded locally in `build/reports/client-audio-compatibility.json`.

BCLib's AIR ItemLike diagnostic is caught after filtering Twilight Forest's potted-fiddlehead item-color registration, whose block has no item form. CorgiLib's HTTP 301 concerns its optional client announcement endpoint; the request logs and returns null. Neither is an unhandled gameplay or server-login failure.


Ice and Fire's packaged fire-dragon `swimming` and `swim5` Tabula files are absent. Its broad pose preload reports them, but the actual fire-dragon animator constructs its swim cycle from WALK1 through WALK4. The ice-dragon animator uses SWIM1 through SWIM5 and has the corresponding assets. No different dragon's model is substituted, and no active fire-animation failure was demonstrated by these preload warnings.


## Dedicated-server recipe-book synchronization

A real installed client exposed a login disconnect that same-process server tests could not detect. Hearth & Harvest lazily initialized its Cask and Keg recipe-book types on the client, leaving a fresh dedicated server with two fewer types. Recipe-book settings serialize two Boolean flags per type without a separate type count; the four-byte disagreement shifted the following recipe-ID list and caused decoder underflow or malformed IDs.

AoE now initializes those existing Hearth & Harvest types during common setup, before either side sends recipe-book packets. It retains the upstream IDs and menus. The regression asserts that both types exist in a fresh server process before a menu is constructed and checks recipe-book synchronization. The native client login, reload and portal matrix remain separate required confirmation after this fix.


## Final reload-data repairs

The installed-server reload exposed a functional upstream tag omission: Hearth & Harvest's wild-peanut modifier referenced nonexistent `forge:is_temperate/overworld`. Its scoped override now intersects `minecraft:is_overworld` with the complements of `forge:is_hot/overworld` and `forge:is_cold/overworld`, using Forge's native composite holder sets. This is the chosen compatibility definition of *temperate*, rather than a recovered verbatim upstream definition. Existing exclusion of lush caves and mushroom fields, the peanut feature, and its decoration step are retained. It does not define a new global biome tag or change the separate Vintage Delight peanut placement policy.

All 45 Floral Flair block loot tables now declare the correct `minecraft:block` parameter context; every pool, condition, entry, count and random-sequence value remains unchanged. Their previous `minecraft:empty` declaration produced validation warnings even though ordinary block-drop evaluation supplied block parameters. Pineapple Delight's wild crop had two identical unconditional alternative entries; the redundant second entry is removed, retaining the first entry's item, fortune bonus and explosion decay. Exact original-resource comparisons are recorded in `build/reports/reload-resource-repairs.json`.


## Final measured release validation

On 2026-10-20, the final reobfuscated artifact (`87af11197d7fced0063f3571bae55d2ac80785c7c463169d7e8f8eb8c7ec1d35`, 2,630,330 bytes) passed the build, all 68 required GameTests, and isolated `runData`. The resource audit checked 1,257 JSON files with 3,603 checks and no errors.

The installed Forge 47.4.22 server and client used the same 70 production JAR hashes; the client additionally carried one local QA helper, excluded from the release. Actual client login, live server reload, llama/zombie/skeleton probes, all 32 allowed directed portal routes, and all 17 forbidden selections passed. Mixed rotated frames, survival charge (85 observed client ticks), arrival cooldown, return travel, and a real client resource reload also passed. The repaired tag, loot-context and unreachable-entry warnings were absent at startup and reload. All seven repaired sound events resolved to existing recordings.

A clean server stop saved every dimension, and the same artifact restarted with the same world. The connected client reused the exact saved Desert/Nether return coordinates in both directions. Both client and final server exited cleanly with code 0; all dimensions were saved at the final shutdown. Machine-readable evidence is in `build/reports/native-server-validation.json` and `run/portal-production-client/final-qa-report.json`, with preserved logs and screenshots.

These checks use one local offline test player and controlled fixtures. External latency, concurrent-player load, long-duration survival, and exhaustive gameplay for every dependency were not exercised. The documented upstream missing recordings and nonfatal bootstrap diagnostics remain; successful validation does not imply every log line is warning-free.
