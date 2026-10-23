September 2024 / Angling tested release
Minecraft 1.20.1 | Forge 47.4.22 | Java 17

Files to install
- aoemod-1.0.0-1.20.1-all.jar is the September 2024 mod.
- angling-forge-1.20.1-1.0.1-forge.1.jar is the unofficial Angling Forge port.
- Install the same versions on both client and server, together with the dependencies recorded in production-dependencies.json. These two jars are not the entire modpack.
- The complete tested set of 70 runtime jars is already staged at C:/Users/andre/Downloads/Games/September2024Mod/run/september-production/mods. It includes both delivered mods and their standalone dependencies, with no local QA helper. Use that folder as the local installation source.
- The staged Angling copy is named angling-1.0.1-forge.1.jar; it is byte-identical to the Angling jar in this release folder. Install only one of those two filenames.
- Third-party jars are not bundled into this release folder. Some originals are in the project's libs folders; others came from the Gradle cache. The libs folders alone are not the complete runtime pack. dependency-lock.json records the 47 supplied jars plus Angling; production-dependencies.json also records retained dependencies and embedded MixinExtras.
- Use the production manifest when selecting dependency jars. Gallery and CuisineDelight under libs/datagen are build-only integrations and are not part of this runtime pack.
- Replace older copies of these two mods rather than installing duplicate versions. Use the -all.jar September build; it includes the required MixinExtras library.
- Keep a backup before opening an existing world with a changed modpack. The isolated test profiles did not modify the user's AnimalTesting installation.

Portals
Build a rectangular frame using any blocks in minecraft:planks, including mixed burnable and non-burnable planks. The opening can be 2-21 blocks wide and 3-21 blocks high; corners are optional. Crouch and right-click the frame with a stick, then select a destination.

The Overworld connects to Tropical, Savanna, Plains, Snowy, and Desert. These five AOE dimensions connect to each other and back to the Overworld. Desert alone also connects to AOE Nether; AOE Nether returns only to Desert. The ordinary Minecraft Nether is not a destination in this picker.

Colors: Tropical green, Savanna amber, Plains stone grey, Snowy white, Desert yellow, AOE Nether red, Overworld blue. Savanna amber is the selected default because no specific color was requested. Survival travel requires four seconds in the portal. Linked return portals are saved with the world.

Changes and validation
CHANGELOG.txt describes the requested integration, document precedence, #CHANGES work, and gameplay repairs. TEST_REPORT.txt and validation.json distinguish measured test results from remaining testing. evidence/ contains the supporting reports and logs. SHA256SUMS.txt identifies the exact delivered files.

Publication
Angling retains its original CC0-1.0 license and credits: EightSidedSquare, Diansu, and Sillvia. This Forge build is an unofficial port. ANGLING-LICENSE.txt contains the full dedication; additional Angling source and publishing files remain in the Angling project's release folder.

Before a public September release, set the project's actual maintainer, credits, and support information. The former template author names were removed rather than replaced with guessed identities. This folder has not been uploaded to CurseForge.
