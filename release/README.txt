September 2024 mod — updated 22 November 2026
Minecraft 1.20.1 | Forge 47.4.22 | Java 17

Files to install
- aoemod-1.0.0-1.20.1-all.jar is the current September 2024 mod. Use the -all.jar build; it includes MixinExtras 0.5.0.
- Install the same version on client and server, together with the dependencies recorded in production-dependencies.json. This JAR is an update, not the entire modpack.
- Angling is no longer a runtime dependency. Remove an older Angling installation when updating this pack; its license and dated reports remain here as historical records.
- production-dependencies.json pins the current workspace's 60 external runtime artifacts and the one embedded MixinExtras artifact. dependency-lock.json pins the 43 local pack JARs used by Gradle. Other required JARs come from the retained libraries and Gradle dependencies.
- Third-party dependency JARs are not bundled into this release folder. The project's libs folders alone are not the complete runtime pack. Gallery and CuisineDelight under libs/datagen are build-only integrations.
- Replace the older AoE mod JAR instead of installing duplicate versions. Preserve existing configuration files, options, and world serverconfig folders.
- In the development workspace, Launch Minecraft.cmd uses the prepared runtime snapshot. Use "Launch Minecraft.cmd -Online" only when dependencies need downloading. Older staged production folders may contain earlier builds.

Current changes and validation
NOVEMBER_22_REPORT.txt describes the latest document and Snowy cultivation updates, the reused validation evidence, and its limits. CHANGELOG.txt begins with this delivery; IMPLEMENTED_CHANGES.md and older dated reports preserve earlier revisions.
The November 22 updates include the imported illager mansion library and facade fixes, mine and gameplay fixes, and shared Snowy cultivation permissions. Giant mushrooms work in Dappled Forest, and wheat, beetroot, and oats grow in Mushroom Caves with normal soil and light requirements. Snowy growth, bonemeal, breeding, and resource scores remain shared across its biomes.
New structure layouts require newly generated or newly placed structures. The Snowy cultivation and score rules also apply in existing worlds.
SHA256SUMS.txt identifies the exact files included in this publication. Historical reports and archived ZIPs retain their original dates and describe their original builds; their old JAR hashes do not identify this update.

Portals
Build a rectangular frame using any blocks in minecraft:planks, including mixed burnable and non-burnable planks. The opening can be 2-21 blocks wide and 3-21 blocks high; corners are optional. Crouch and right-click the frame with a stick, then select a destination.
The Overworld connects to Tropical, Savanna, Plains, Snowy, and Desert. These five AOE dimensions connect to each other and back to the Overworld. Desert alone also connects to AOE Nether; AOE Nether returns only to Desert. The ordinary Minecraft Nether is not a destination in this picker.
Colors: Tropical green, Savanna amber, Plains stone grey, Snowy white, Desert yellow, AOE Nether red, Overworld blue. Survival travel requires four seconds in the portal. Linked return portals are saved with the world.

Historical material
NOVEMBER_16_REPORT.txt, NOVEMBER_15_ROUND2_REPORT.txt, the November 8 reports, texture-review-2026-11-08.zip, worldgen-followup-2026-11-08.zip, and their dated evidence describe preceding deliveries. TEST_REPORT.txt and validation.json retain historical October results and limitations. BLOCK_AVAILABILITY.md and RANDOM_TICK_BLOCKS.md are historical registry snapshots, not current installation manifests.
Angling retains its original CC0-1.0 license and credits: EightSidedSquare, Diansu, and Sillvia. ANGLING-LICENSE.txt is preserved for the historical unofficial port; Angling is not part of the current runtime release.
This publication updates the existing GitHub project. It does not publish the mod to CurseForge.
