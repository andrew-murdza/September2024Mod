# Later Minecraft gameplay dependencies

AOE uses the complete Forge 1.20.1 implementations from VanillaBackport and Tiny Takeover Backport. The latest published VanillaBackport binary lacks some requested Wilderness Bound and Chaos Cubed content, so the pinned public source is built locally.

With Java 17 and network access, run this once in PowerShell, then build AOE normally:

```powershell
.\scripts\bootstrap-future-backports.ps1
.\gradlew.bat build
```

Use **Forge 47.4.22 or later** for Minecraft 1.20.1. Platform's current config registration uses `ModLoadingContext.getContainer()`, which does not exist in the former Forge 47.3.0 runtime. The project also uses **Quark 4.0-462** (CurseForge file `6427817`) and **Zeta 1.0-31** (`7335229`); the former Quark 4.0-460 loaded a client-only setup signature during dedicated-server discovery and crashed before datapacks loaded.

The script downloads official sources, builds Platform and VanillaBackport, verifies Tiny Takeover's release checksum, and adapts Tiny Takeover's Armadillo API and overlapping dolphin aging hooks. AOE also registers Tiny's baby-armadillo model layer during client setup, without depending on an early config read. The script writes the three runtime dependencies to ignored `libs/`. YetAnotherConfigLib is resolved by Gradle. A distributed installation needs those same dependencies in its `mods/` folder on the appropriate sides; they are not embedded in the AOE JAR.

| Dependency | Pinned version/source |
|---|---|
| [VanillaBackport](https://github.com/ItsBlackGear/VanillaBackport/tree/c69c9f674ba91d3181b8ccdc90ae8dab9133ca39) | 1.2.0.2640.1, commit `c69c9f674ba91d3181b8ccdc90ae8dab9133ca39` |
| [Platform](https://github.com/ItsBlackGear/Platform/tree/73f4cddb2d8d60ec132a0d373a954bccd9bd1768) | 1.4.0.2640.1, commit `73f4cddb2d8d60ec132a0d373a954bccd9bd1768` |
| [Tiny Takeover Backport](https://github.com/evanbones/Tiny-Takeover-Backport) | Forge 1.6.3, official release plus the reproducible MIT-licensed compatibility transformation |
| [YetAnotherConfigLib](https://modrinth.com/mod/yacl) | 3.6.6+1.20.1-forge |

VanillaBackport's current source license permits private builds and integration but restricts redistribution of rebuilt or modified binaries. Do not commit or publish the locally built VanillaBackport JAR. Use the bootstrap script for private development, and arrange an authorized upstream release before distributing a public pack that needs this newer build.

The compatibility policy keeps AOE's limited nautilus and existing pale/creaking content, excludes Happy Ghasts, harnesses, dried ghasts, and the extra nautilus types, preserves rabbit appearance and dye textures, and suppresses dependency-generated terrain, plants, biomes, and camps. Explicit spawn entries and animal variant tags supply the requested mobs in AOE biomes. Existing nonbone spear stacks migrate to complete backported spear items while retaining their NBT; the bone spear remains an AOE weapon.

The three adult temperate-cow and mooshroom textures are taken from the official Minecraft 26.3 client, SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`, obtained through Mojang's version manifest. They retain the 1.20.1 renderer filenames; `UpdatedCowTextureLayout` supplies the matching modern 64×64 UV layout and muzzle. Baby cow/mooshroom rendering comes from Tiny Takeover. No rabbit textures were imported.

Missing copper-building/generic English subtitle keys and the sulfur eruption subtitle use the same official client text. A sound-event alias connects potent sulfur's continuous eruption start to the existing backport eruption recordings; this corrects a registry/resource name mismatch without replacing the audio.

## Gameplay supplied by these dependencies

- **Chaos Cubed / sulfur and cinnabar:** the sulfur and cinnabar block families, potent sulfur, sulfur spikes, sulfur cube absorption and block-dependent physical behavior, shearing, bucketing, splitting, growth, and dispenser interactions. Sulfur cubes receive natural spawn entries in the existing AOE basalt-deltas biome. No sulfur caves, springs, or generated spikes are inserted by the dependency.
- **Wilderness Bound:** poplar wood and colored foliage, saplings, shelf mushrooms and stew use, red shrubs, cushions, straw beds, and wool/concrete stairs and slabs. Poplar saplings and shelf mushrooms are sold by wandering traders. Red shrubs copy crimson-root block properties, sounds, shape and offset, survive only on crimson nylium, and use AOE's crimson-root bone-meal rules; their separate backport adjacent-spread routine is suppressed. Dappled forests and abandoned camps are not generated.
- **Mounts of Mayhem:** full spear jab/charge attacks, charge phases, speed-dependent damage, reach and Lunge; Parched weakness arrows, camel husks and their natural husk/Parched jockey formation, zombie horses, ridden mount floating, and netherite horse armor. Existing nonbone AOE spears migrate to backport spears; bone spears keep their custom behavior. Netherite spear tier and charge parameters sit between copper and iron. The existing limited AOE nautilus remains; the dependency's nautilus, zombie nautilus, eggs and armor are unavailable through ordinary acquisition/spawning.
- **Copper Age:** sorting copper golems, copper chests, weathering/waxing/scraping, golem statues and poses, copper nugget and horse armor, copper torches/lanterns/chains/bars, lightning-rod oxidation, and interactive wood shelves. Copper tools/armor use the existing AOE items as the canonical equipment. Copper chains use the backport and keep Quark's chain connection behavior. Dye textures and dye palettes retain their previous appearance.
- **Tiny Takeover:** new baby animal models/animations/sounds, animal sound variants, golden-dandelion growth locking, craftable name tags, trumpet note-block sounds and the dependency's stonecutting/trade updates. Rabbit model and texture replacement are disabled to preserve the existing texture layout; the updated nonvisual bounding box and eye height remain enabled. AOE dolphin reproduction and age persistence remain authoritative; Tiny supplies the baby rendering and golden-dandelion integration. Locked animals cannot advance through AOE natural or food growth hooks. Unconfigured animals such as armadillos retain normal growth instead of freezing.
- **Armored Paws:** armadillos, rolling/fear behavior, brushing and scutes, breeding and spider avoidance; dyeable wolf armor, owner equipment/removal/repair and damage absorption; all nine wolf variants. Tamed wolves use the newer health pool, twice the food healing, and no old damage halving. AOE wolf variant habitats are plains (pale), cherry grove (woods/chestnut), snowy forest (ashen/black/snowy), rainforest (rusty), savanna (spotted), and mesa (striped).
- **Other explicit requests:** wildflowers sold by wandering traders, warm pig variants named Boar, updated adult temperate cow and red/brown mooshroom textures, modern colored bundles and bundle controls, the leather-or-rabbit-hide plus string recipe, tuff and copper building variants, and the updated bat model/texture/animation. AOE's existing Bogged remains the active Bogged implementation.

The dependency also contains registrations for its older releases. Compatibility code hides duplicate pale/creaking content and excludes Happy Ghasts, harnesses, dried ghasts, Tears/Lava Chicken music discs and their ordinary acquisition routes. Suppression leaves registry IDs available for compatibility; it does not delete existing chunks. Normal generation is owned by AOE, while explicit runtime mob populations and variant tags preserve the requested new mobs.

## Data generation limitation

AOE currently maintains its resource JSON directly and registers no data providers. The `runData` task is already scoped to `--mod aoemod`, but Forge still dispatches its data-gathering event to installed dependencies. Fruits Delight 1.0.22 constructs a provider that references an absent optional Cuisine Delight class (`IngredientConfig`), so this development command fails before generation. This does not affect `build`, client/server loading, or GameTests, and no generated files are required for these changes.

Validation on 2026-10-17: `build` succeeded and all 24 AOE GameTests passed. Client startup completed entity-model creation, texture-atlas loading, and splash/menu resource setup with the modern cow and baby-armadillo models. The targeted backport sound/subtitle errors were resolved. This startup check does not substitute for playing through every interaction supplied by the dependencies; unrelated existing dependency asset warnings remain.
