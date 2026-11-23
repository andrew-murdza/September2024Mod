package net.amurdza.examplemod.gametest;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyAquaticRosterTests {
    private static final List<String> BIOMES = List.of("snowy_forest", "dappled_forest", "mushroom_caves", "cold_ocean", "mountain_lake");
    private static final Set<String> RETAINED = Set.of("minecraft:salmon", "alexsmobs:flying_fish",
            "hybrid_aquatic:arrow_squid", "hybrid_aquatic:tuna", "alexsmobs:orca",
            "alexscaves:tripodfish", "alexscaves:gossamer_worm", "minecraft:glow_squid",
            "hybrid_aquatic:vampire_squid", "hybrid_aquatic:spider_crab", "alexsmobs:lobster");
    private static final List<String> REMOVED_FROM_SNOWY = List.of("minecraft:squid", "minecraft:dolphin", "hybrid_aquatic:needlefish",
            "alexscaves:lanternfish", "hybrid_aquatic:dragonfish", "hybrid_aquatic:bull_shark",
            "aoemod:end_fish", "aoemod:cubozoa");

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyLoadedAquaticsMatchTheReducedRosterAndKeepManualSpawns(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        helper.assertFalse(ModList.get().isLoaded("angling"), "The retired Angling mod must not be loaded");
        helper.assertFalse(ForgeRegistries.ENTITY_TYPES.getKeys().stream().anyMatch(id -> id.getNamespace().equals("angling")),
                "The removed dependency must not leave hidden Angling entities");
        helper.assertFalse(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation("iceandfire:hippocampus")),
                "The previously removed Ice and Fire dependency must not be restored by aquatic roster changes");
        helper.assertFalse(ModList.get().isLoaded("betterend"), "The actual BetterEnd dependency must be removed");
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.getKeys().stream().noneMatch(id -> id.getNamespace().equals("betterend")),
                "Removed BetterEnd entities must not survive as hidden manual-spawn entries");
        Set<ResourceLocation> charnias = Set.of(new ResourceLocation("betterend:charnia_light_blue"), new ResourceLocation("betterend:charnia_cyan"));
        helper.assertTrue(ForgeRegistries.BLOCKS.getKeys().stream().filter(id -> id.getNamespace().equals("betterend"))
                        .collect(Collectors.toSet()).equals(charnias)
                        && ForgeRegistries.ITEMS.getKeys().stream().filter(id -> id.getNamespace().equals("betterend"))
                        .collect(Collectors.toSet()).equals(charnias),
                "The AoE-owned light blue and cyan Charnia blocks/items may preserve their legacy BetterEnd IDs");
        for (String name : BIOMES) {
            Set<String> expected = new HashSet<>(RETAINED);
            if (name.equals("cold_ocean") || name.equals("mountain_lake")) expected.add("aoemod:nautilus");
            var declared = DocumentedFauna.forBiome(name);
            Set<String> aquatic = declared.stream().filter(s -> s.habitat() != DocumentedFauna.Habitat.LAND)
                    .map(s -> s.id().toString()).collect(Collectors.toSet());
            helper.assertTrue(aquatic.equals(expected), "Incorrect Snowy aquatic roster in " + name + ": " + aquatic);
            var biome = snowy.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
            Set<String> loaded = Arrays.stream(MobCategory.values()).filter(c -> c != MobCategory.MONSTER && c != MobCategory.MISC)
                    .flatMap(c -> biome.value().getMobSettings().getMobs(c).unwrap().stream())
                    .map(s -> ForgeRegistries.ENTITY_TYPES.getKey(s.type).toString()).collect(Collectors.toSet());
            declared.stream().filter(s -> s.habitat() == DocumentedFauna.Habitat.LAND)
                    .forEach(s -> loaded.remove(s.id().toString()));
            helper.assertTrue(loaded.equals(expected), "Loaded biome retained an extra aquatic or lost one in " + name + ": " + loaded);
            var habitat = AquaticPlantGenerationTests.fixture(helper, name, new HashMap<>(), 32, snowy);
            BlockPos pos = new BlockPos(0, 101, 0);
            for (String id : REMOVED_FROM_SNOWY) {
                var key = new ResourceLocation(id);
                helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(key), "Spawn removal must not unregister retained external/manual entity " + id);
                var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
                for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.SPAWN_EGG, MobSpawnType.COMMAND}) {
                    var event = new MobSpawnEvent.SpawnPlacementCheck(type, habitat, reason, pos, RandomSource.create(17), true);
                    event.setResult(Event.Result.ALLOW);
                    DocumentedNaturalSpawns.placement(event);
                    helper.assertTrue(event.getResult() == (DocumentedNaturalSpawns.natural(reason) ? Event.Result.DENY : Event.Result.ALLOW),
                            "Natural removal or manual-spawn preservation failed for " + id + " / " + reason);
                }
            }
            var lobster = declared.stream().filter(s -> s.id().toString().equals("alexsmobs:lobster")).findFirst().orElseThrow();
            helper.assertTrue(lobster.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER
                            && lobster.depth() == DocumentedFauna.Depth.AT_OR_ABOVE_Y39,
                    "Lobsters must occupy the upper seabed");
            for (int depth : new int[]{3, 32}) {
                var bottom = AquaticPlantGenerationTests.fixture(helper, name, new HashMap<>(), depth, snowy,
                        Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
                var event = new MobSpawnEvent.SpawnPlacementCheck(lobster.type(), bottom, MobSpawnType.NATURAL, pos, RandomSource.create(17), false);
                DocumentedNaturalSpawns.placement(event);
                helper.assertTrue(event.getResult() == Event.Result.ALLOW,
                        "Water depth alone must not reject a lobster whose bed is above Y39");
            }
        }
        helper.assertTrue(DocumentedFauna.forBiome("desert_lake").stream().anyMatch(s -> s.id().toString().equals("hybrid_aquatic:hound_shark"))
                        && DocumentedFauna.forBiome("crimson_forest").stream().anyMatch(s -> s.id().toString().equals("aoemod:cubozoa")),
                "Desert hound sharks and Nether Cubozoa must retain their authored habitats");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void snowyLobstersAreRedForEverySpawnSourceAndReload(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs:lobster"));
        for (String name : BIOMES) {
            var habitat = AquaticPlantGenerationTests.fixture(helper, name, new HashMap<>(), 32, snowy);
            for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.SPAWN_EGG, MobSpawnType.BUCKET}) {
                Set<Integer> observed = new HashSet<>();
                for (int i = 0; i < 64; i++) {
                    var lobster = (EntityLobster) type.create(snowy);
                    lobster.getRandom().setSeed(23019L + i * 983L);
                    lobster.finalizeSpawn(habitat, snowy.getCurrentDifficultyAt(BlockPos.ZERO), reason, null, null);
                    observed.add(lobster.getVariant());
                    lobster.discard();
                }
                helper.assertTrue(observed.equals(Set.of(0)), "Snowy lobsters must always use solid red0, got " + observed);
            }
            var manual = (EntityLobster) type.create(snowy);
            manual.finalizeSpawn(habitat, snowy.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.SPAWN_EGG, null, null);
            helper.assertTrue(manual.getVariant() == 0, "Snowy egg lobsters must also be red");
            CompoundTag saved = new CompoundTag();
            manual.addAdditionalSaveData(saved);
            saved.putInt("Variant", 5);
            var restored = (EntityLobster) type.create(snowy);
            restored.readAdditionalSaveData(saved);
            net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns.finishedSpawn(
                    new net.minecraftforge.event.entity.EntityJoinLevelEvent(restored, snowy, true));
            helper.assertTrue(restored.getVariant() == 0, "Existing saved Snowy lobsters must also become red");
            manual.discard(); restored.discard();
        }
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var habitat = AquaticPlantGenerationTests.fixture(helper, "plains", new HashMap<>(), 8, plains);
        for (int i = 0; i < 32; i++) {
            var lobster = (EntityLobster) type.create(plains);
            lobster.finalizeSpawn(habitat, plains.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.NATURAL, null, null);
            boolean mountain = habitat.getBiome(BlockPos.ZERO).is(ModTags.Biomes.mountainBiomes);
            helper.assertTrue(lobster.getVariant() == 0 || mountain && lobster.getVariant() == 1,
                    "Other dimensions must retain their preexisting lobster palette");
            lobster.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void blueLobsterBucketDataAppliedAfterJoiningCannotOverrideSnowyRed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(12, 200, 12);
        var forced = new java.util.LinkedHashMap<net.minecraft.server.level.ServerLevel, Boolean>();
        for (var dimension : List.of(GameplayDimensions.SNOWY, GameplayDimensions.PLAINS)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            forced.put(level, level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(0, 0)));
            level.setChunkForced(0, 0, true);
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                        forced.keySet().stream().allMatch(level -> level.isPositionEntityTicking(pos)),
                        "Both bucket destinations must be entity-ticking before testing a real tracked join"))
                .thenExecute(() -> {
                    try {
                        for (var level : forced.keySet()) verifyPostJoinLobsterBucket(helper, level, pos);
                    } finally {
                        forced.forEach((level, wasForced) -> { if (!wasForced) level.setChunkForced(0, 0, false); });
                    }
                }).thenSucceed();
    }

    private static void verifyPostJoinLobsterBucket(GameTestHelper helper, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs:lobster"));
        var previous = level.getBlockState(pos);
        try {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            var lobster = (EntityLobster) type.spawn(level, pos, MobSpawnType.BUCKET);
            helper.assertTrue(lobster != null && level.getEntity(lobster.getUUID()) == lobster,
                    "Bucket fixture must join the destination level before receiving its saved color");
            try {
                CompoundTag blueBucket = new CompoundTag();
                blueBucket.putInt("BucketVariantTag", 1);
                // MobBucketItem follows precisely this order: spawn/addFreshEntity,
                // then loadFromBucketTag. No join handler runs after this assignment.
                lobster.loadFromBucketTag(blueBucket);
                lobster.setFromBucket(true);
                int expected = level.dimension().equals(GameplayDimensions.SNOWY) ? 0 : 1;
                helper.assertTrue(lobster.getVariant() == expected,
                        "Post-join bucket data must stay red only in Snowy; other dimensions preserve blue");
                var recaptured = lobster.getBucketItemStack();
                lobster.saveToBucketTag(recaptured);
                helper.assertTrue(recaptured.getOrCreateTag().getInt("BucketVariantTag") == expected,
                        "Recapturing must save the corrected regional lobster color");
            } finally { lobster.discard(); }
        } finally { level.setBlock(pos, previous, 3); }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedAquaticModAndOwnedContentAreAbsentFromRegistriesAndRecipes(GameTestHelper helper) {
        helper.assertFalse(ModList.get().isLoaded("aquatica"), "The removed aquatic dependency must not load");
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.getKeys().stream().noneMatch(id -> id.getNamespace().equals("aquatica"))
                        && ForgeRegistries.ITEMS.getKeys().stream().noneMatch(id -> id.getNamespace().equals("aquatica")),
                "Removed dependency entities/items remain registered");
        helper.assertFalse(ForgeRegistries.ENTITY_TYPES.containsKey(AOEMod.makeID("swordfish")), "Removed owned fish entity remains registered");
        for (String id : List.of("spawn_egg_swordfish", "raw_swordfish", "cooked_swordfish", "raw_isopod", "cooked_isopod")) {
            helper.assertFalse(ForgeRegistries.ITEMS.containsKey(AOEMod.makeID(id)), "Removed aquatic item remains registered: " + id);
        }
        for (String method : List.of("smelting", "smoking", "campfire_cooking")) {
            for (String dish : List.of("cooked_swordfish", "cooked_isopod")) {
                helper.assertTrue(helper.getLevel().getRecipeManager().byKey(AOEMod.makeID(method + "/" + dish)).isEmpty(),
                        "Removed aquatic recipe still loads: " + method + "/" + dish);
            }
        }
        helper.assertTrue(helper.getLevel().getServer().getResourceManager()
                        .getResource(AOEMod.makeID("loot_tables/entities/swordfish.json")).isEmpty(),
                "Removed fish loot table remains in the data pack");
        helper.succeed();
    }
}
