package net.amurdza.examplemod.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.scouter.netherdepthsupgrade.entity.AbstractLavaSchoolingFish;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.CubozoaEntity;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.registry.RetainedCharnia;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Exercise the retained content without either removed mod, including loaded worldgen data. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetainedEndAndOreTests {
    private static final String TEMPLATE = "plant_gameplay_empty";
    private static final Set<ResourceLocation> CHARNIAS = Set.of(
            new ResourceLocation("betterend", "charnia_light_blue"), new ResourceLocation("betterend", "charnia_cyan"));

    private RetainedEndAndOreTests() { }

    @GameTest(template = TEMPLATE)
    public static void removedModsLeaveOnlyTheTwoRetainedPlants(GameTestHelper helper) {
        helper.assertFalse(ModList.get().isLoaded("betterend"), "The full BetterEnd mod must be absent at runtime");
        helper.assertFalse(ModList.get().isLoaded("bclib"), "BCLib must not remain as an unused runtime dependency");
        for (String notice : new String[]{"LICENSE", "LICENSE.ASSETS", "ATTRIBUTION.txt"}) {
            assertResource(helper, "META-INF/licenses/betterend/" + notice);
        }
        helper.assertTrue(ForgeRegistries.BLOCKS.getKeys().stream().filter(id -> id.getNamespace().equals("betterend"))
                        .collect(Collectors.toSet()).equals(CHARNIAS),
                "Only the two saved charnia block IDs should remain in the BetterEnd namespace");
        helper.assertTrue(ForgeRegistries.ITEMS.getKeys().stream().filter(id -> id.getNamespace().equals("betterend"))
                        .collect(Collectors.toSet()).equals(CHARNIAS),
                "Only the two charnia items should remain in the BetterEnd namespace");
        for (var id : CHARNIAS) {
            String root = "assets/betterend/";
            JsonObject states = resourceJson(root + "blockstates/" + id.getPath() + ".json");
            helper.assertTrue(states.has("variants"), "Charnia blockstate variants must be packaged: " + id);
            JsonObject model = resourceJson(root + "models/block/" + id.getPath() + ".json");
            helper.assertTrue(model.get("parent").getAsString().equals("betterend:block/charnia"),
                    "Retained charnias must retain their original shared shape");
            JsonObject parent = resourceJson(root + "models/block/charnia.json");
            helper.assertTrue(parent.has("elements") && !parent.has("parent"),
                    "The shared original model must be self-contained without BCLib");
            JsonObject item = resourceJson(root + "models/item/" + id.getPath() + ".json");
            helper.assertTrue(Set.of("item/generated", "minecraft:item/generated")
                            .contains(item.get("parent").getAsString()), "The charnia item needs only a vanilla model parent");
            assertResource(helper, root + "textures/block/" + id.getPath() + ".png");
            assertResource(helper, root + "textures/item/" + id.getPath() + ".png");
            assertResource(helper, "data/betterend/loot_tables/blocks/" + id.getPath() + ".json");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void charniasRequireSourceWaterAndNeverBecomeTallSeagrass(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (Block plant : new Block[]{RetainedCharnia.LIGHT_BLUE.get(), RetainedCharnia.CYAN.get()}) {
            level.setBlock(pos.below(), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            var context = new DirectionalPlaceContext(level, pos, Direction.DOWN, new ItemStack(plant), Direction.UP);
            BlockState state = plant.getStateForPlacement(context);
            helper.assertTrue(state != null && state.canSurvive(level, pos), "Charnia must plant in source water above sand");
            helper.assertTrue(state.getFluidState().isSource() && state.getFluidState().is(FluidTags.WATER),
                    "Placed charnia must preserve source water");
            level.setBlock(pos, Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 4), Block.UPDATE_CLIENTS);
            helper.assertTrue(plant.getStateForPlacement(context) == null, "Flowing water must not accept charnia");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(plant.getStateForPlacement(context) == null, "Dry ground must not accept charnia");
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, 2);
            helper.assertFalse(((SeagrassBlock) plant).isValidBonemealTarget(level, pos, state, false),
                    "Charnia must not advertise vanilla seagrass fertilization");
            helper.assertFalse(BoneMealItem.growCrop(boneMeal, level, pos), "Bone meal must leave the retained plant alone");
            helper.assertTrue(boneMeal.getCount() == 2 && level.getBlockState(pos).is(plant),
                    "Bone meal must neither be consumed nor convert charnia to tall seagrass");
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertFalse(state.canSurvive(level, pos), "Charnia must still need a supporting floor");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void retainedCubozoaSpawnsInLavaAsSulfurAndKeepsItsBucketVariant(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        helper.assertTrue(nether != null, "The AoE Nether must be loaded");
        CubozoaEntity cubozoa = ModEntities.CUBOZOA.get().create(nether);
        helper.assertTrue(cubozoa instanceof AbstractLavaSchoolingFish && cubozoa.fireImmune(),
                "Retained cubozoa needs the lava-fish movement, breathing and fire immunity");
        cubozoa.moveTo(0.5, 12, 0.5, 0, 0);
        cubozoa.finalizeSpawn(nether, nether.getCurrentDifficultyAt(cubozoa.blockPosition()), MobSpawnType.NATURAL, null, null);
        helper.assertTrue(cubozoa.getVariant() == CubozoaEntity.Variant.VARIANT_1,
                "Naturally spawned Nether cubozoa must use the sulfur variant");
        var defaultOutsideNether = ModEntities.CUBOZOA.get().create(helper.getLevel());
        helper.assertTrue(defaultOutsideNether.getVariantId() == 1,
                "New entities, including creative defaults outside the Nether, must already be sulfur");
        CompoundTag oldSave = cubozoa.saveWithoutId(new CompoundTag());
        oldSave.putInt("Variant", 0);
        oldSave.putByte("Scale", (byte) 11);
        defaultOutsideNether.load(oldSave);
        helper.assertTrue(defaultOutsideNether.getVariantId() == 1 && defaultOutsideNether.getByteScale() == 11,
                "Loading a legacy purple cubozoa must migrate its appearance while preserving size");
        defaultOutsideNether.setVariant(CubozoaEntity.Variant.DEFAULT);
        helper.assertTrue(defaultOutsideNether.getVariantId() == 1,
                "The legacy variant enum must remain compatible without re-enabling the removed appearance");
        helper.assertTrue(defaultOutsideNether.saveWithoutId(new CompoundTag()).getInt("Variant") == 1,
                "The next entity save must record the migrated sulfur appearance");
        helper.assertFalse(cubozoa.hurt(nether.damageSources().lava(), 4), "Lava must not damage cubozoa");
        ItemStack bucket = cubozoa.getBucketItemStack();
        helper.assertTrue(bucket.is(ModItems.BUCKET_CUBOZOA.get())
                        && ((BucketItem) bucket.getItem()).getFluid() == Fluids.LAVA,
                "Cubozoa must be retained in a lava bucket");
        helper.assertTrue(bucket.hasTag() && bucket.getTag().getInt("Variant") == 1
                        && bucket.getTag().getByte("Scale") == cubozoa.getByteScale(),
                "Catching cubozoa must retain its sulfur appearance and size");
        ServerLevel fixture = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        fixture.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        fixture.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        fixture.setBlock(pos.above(), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        var legacyBucket = bucket.copy();
        legacyBucket.getOrCreateTag().putInt("Variant", 0);
        for (ItemStack source : new ItemStack[]{bucket, legacyBucket, new ItemStack(ModItems.BUCKET_CUBOZOA.get())}) {
            var beforeRelease = fixture.getEntitiesOfClass(CubozoaEntity.class, new AABB(pos).inflate(3)).stream()
                    .map(CubozoaEntity::getUUID).collect(Collectors.toSet());
            ((BucketItem) source.getItem()).checkExtraContent(helper.makeMockSurvivalPlayer(), fixture, source, pos);
            var released = fixture.getEntitiesOfClass(CubozoaEntity.class, new AABB(pos).inflate(3),
                    fish -> !beforeRelease.contains(fish.getUUID()));
            helper.assertTrue(released.size() == 1, "Actual caught, legacy and creative buckets must each release one cubozoa");
            helper.assertTrue(released.get(0).getVariantId() == 1 && released.get(0).fromBucket()
                            && released.get(0).requiresCustomPersistence(),
                    "Every actual bucket release must use sulfur and protect the mob against despawning");
            if (source.hasTag() && source.getTag().contains("Scale")) {
                helper.assertTrue(released.get(0).getByteScale() == source.getTag().getByte("Scale"),
                        "Legacy and sulfur buckets must preserve their caught cubozoa's size");
            }
            released.get(0).discard();
        }
        var eggSpawn = ModEntities.CUBOZOA.get().spawn(fixture, new ItemStack(ModEntities.CUBOZOA_SPAWN_EGG.get()),
                helper.makeMockSurvivalPlayer(), pos, MobSpawnType.SPAWN_EGG, true, false);
        helper.assertTrue(eggSpawn != null && eggSpawn.getVariantId() == 1,
                "The ordinary creative spawn egg must also select sulfur outside the Nether");
        eggSpawn.discard();
        helper.assertTrue(SpawnPlacements.checkSpawnRules(ModEntities.CUBOZOA.get(), fixture, MobSpawnType.NATURAL,
                pos, RandomSource.create(3)), "Two blocks of lava must satisfy the cubozoa placement predicate");
        fixture.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(SpawnPlacements.checkSpawnRules(ModEntities.CUBOZOA.get(), fixture, MobSpawnType.NATURAL,
                pos, RandomSource.create(3)), "A cubozoa may not spawn partly above lava");
        fixture.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        fixture.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(SpawnPlacements.checkSpawnRules(ModEntities.CUBOZOA.get(), fixture, MobSpawnType.NATURAL,
                pos, RandomSource.create(3)), "Water must no longer satisfy cubozoa spawning");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soulSedimentOresUseTheMatchingGroundAndShovel(GameTestHelper helper) {
        var originals = Map.of(ModBlocks.SOUL_SAND_QUARTZ_ORE.get(), Blocks.SOUL_SAND,
                ModBlocks.SOUL_SAND_GOLD_ORE.get(), Blocks.SOUL_SAND,
                ModBlocks.SOUL_SOIL_QUARTZ_ORE.get(), Blocks.SOUL_SOIL,
                ModBlocks.SOUL_SOIL_GOLD_ORE.get(), Blocks.SOUL_SOIL);
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var shovel = new ItemStack(Items.IRON_SHOVEL);
        var pickaxe = new ItemStack(Items.IRON_PICKAXE);
        for (var entry : originals.entrySet()) {
            BlockState ore = entry.getKey().defaultBlockState(), soil = entry.getValue().defaultBlockState();
            helper.assertTrue(ore.getDestroySpeed(level, pos) == soil.getDestroySpeed(level, pos),
                    "Soul ore hardness must match its host ground: " + ore);
            helper.assertTrue(ore.getSoundType(level, pos, null) == soil.getSoundType(level, pos, null),
                    "Soul ore sounds must match its host ground: " + ore);
            helper.assertTrue(ore.is(BlockTags.MINEABLE_WITH_SHOVEL) && !ore.is(BlockTags.MINEABLE_WITH_PICKAXE),
                    "Soul ores must use shovel tags rather than pickaxe tags: " + ore);
            helper.assertTrue(shovel.getDestroySpeed(ore) > pickaxe.getDestroySpeed(ore),
                    "An actual shovel must break soul ore faster than an equal-tier pickaxe: " + ore);
            helper.assertTrue(ore.requiresCorrectToolForDrops() == soil.requiresCorrectToolForDrops(),
                    "Soul ores must preserve their host's tool requirement: " + ore);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allNetherOresRejectAllAirExposure(GameTestHelper helper) {
        var level = helper.getLevel();
        var configurations = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (String biome : new String[]{"crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"}) {
            Set<ResourceLocation> features = biomeFeatures(level, biome);
            for (String mineral : new String[]{"quartz", "nether_gold", "ancient_debris"}) {
                String name = "nether_" + biome + "_" + mineral;
                var configured = configurations.get(AOEMod.makeID("dimension_ores/" + name));
                helper.assertTrue(configured != null && configured.config() instanceof OreConfiguration,
                        "Expected an ordinary loaded ore feature: " + name);
                var ore = (OreConfiguration) configured.config();
                helper.assertTrue(ore.discardChanceOnAirExposure == 1F,
                        "Every Nether habitat must discard exposed ore blocks: " + name);
                helper.assertTrue(features.contains(AOEMod.makeID("dimension_ores/nether/" + biome + "_" + mineral)),
                        "The biome must actually use its revised ore placement: " + name);
                // Run the native placement decision with every target's real host block.
                for (var target : ore.targetStates) {
                    BlockState host = oreHost(target);
                    helper.assertTrue(OreFeature.canPlaceOre(host, ignored -> host, RandomSource.create(5), ore, target,
                            BlockPos.ZERO.mutable()), "Enclosed host blocks must remain eligible: " + name);
                    helper.assertFalse(OreFeature.canPlaceOre(host, ignored -> Blocks.AIR.defaultBlockState(),
                            RandomSource.create(5), ore, target, BlockPos.ZERO.mutable()),
                            "A real placement attempt adjacent to air must fail: " + name);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sulfurUsesTheCinnabarDesertBlobDistribution(GameTestHelper helper) {
        var level = helper.getLevel();
        var desertLevel = level.getServer().getLevel(GameplayDimensions.DESERT);
        var possibleBiomes = java.util.List.copyOf(desertLevel.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
        // Cross-biome ordering is part of Minecraft's generation contract. Merely
        // checking that both biomes contain sulfur misses a reversed dependency.
        var sortedFeatures = FeatureSorter.buildFeaturesPerStep(possibleBiomes,
                biome -> biome.value().getGenerationSettings().features(), true);
        helper.assertFalse(sortedFeatures.isEmpty(),
                "All Desert dimension biomes must share an acyclic loaded feature order");
        var registry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature sulfur = registry.get(AOEMod.makeID("dimension_blobs/desert/sulfur"));
        PlacedFeature cinnabar = registry.get(AOEMod.makeID("dimension_blobs/desert/cinnabar"));
        helper.assertTrue(sulfur != null && cinnabar != null, "Both desert blob placements must be loaded");
        var placementCodec = PlacementModifier.CODEC.listOf();
        helper.assertTrue(placementCodec.encodeStart(JsonOps.INSTANCE, sulfur.placement()).result().orElseThrow()
                        .equals(placementCodec.encodeStart(JsonOps.INSTANCE, cinnabar.placement()).result().orElseThrow()),
                "Sulfur must use the same count, height, spread and biome filter as cinnabar");
        var sulfurOre = (OreConfiguration) sulfur.feature().value().config();
        var cinnabarOre = (OreConfiguration) cinnabar.feature().value().config();
        helper.assertTrue(sulfurOre.size == cinnabarOre.size && sulfurOre.discardChanceOnAirExposure == cinnabarOre.discardChanceOnAirExposure,
                "Sulfur and cinnabar must use the same blob size and exposure behavior");
        helper.assertTrue(sulfurOre.targetStates.size() == 1 && sulfurOre.targetStates.get(0).target
                        .test(Blocks.STONE.defaultBlockState(), RandomSource.create(1))
                        && !sulfurOre.targetStates.get(0).target.test(Blocks.NETHERRACK.defaultBlockState(), RandomSource.create(1)),
                "The new sulfur blob must replace desert stone rather than Nether rock");
        helper.assertTrue(ForgeRegistries.BLOCKS.getKey(sulfurOre.targetStates.get(0).state.getBlock())
                        .equals(new ResourceLocation("minecraft", "sulfur")), "The blob must place sulfur");
        helper.assertTrue(biomeFeatures(level, "desert").contains(AOEMod.makeID("dimension_blobs/desert/sulfur")),
                "Desert generation must include the new sulfur blob");
        for (String biome : new String[]{"desert", "mesa", "deep_dark"}) {
            var features = biomeFeatures(level, biome);
            helper.assertTrue(features.contains(AOEMod.makeID("dimension_blobs/desert/sulfur"))
                            == features.contains(AOEMod.makeID("dimension_blobs/desert/cinnabar")),
                    "Sulfur must follow cinnabar's complete biome distribution in the Desert dimension: " + biome);
        }
        for (String biome : new String[]{"basalt_deltas", "crimson_forest", "warped_forest", "soul_sand_valley"}) {
            helper.assertTrue(biomeFeatures(level, biome).stream().noneMatch(id -> id.getPath().contains("sulfur")),
                    "Sulfur must not remain in the Nether feature list: " + biome);
        }
        helper.succeed();
    }

    private static Set<ResourceLocation> biomeFeatures(ServerLevel level, String biome) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID(biome))
                .getGenerationSettings().features().stream().flatMap(set -> set.stream())
                .flatMap(holder -> holder.unwrapKey().stream()).map(key -> key.location()).collect(Collectors.toSet());
    }

    private static BlockState oreHost(OreConfiguration.TargetBlockState target) {
        return java.util.stream.Stream.of(Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.BASALT,
                        Blocks.BLACKSTONE).map(Block::defaultBlockState)
                .filter(state -> target.target.test(state, RandomSource.create(1))).findFirst().orElseThrow();
    }

    private static JsonObject resourceJson(String name) {
        try (var stream = RetainedEndAndOreTests.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("Missing retained resource: " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read retained resource: " + name, failure);
        }
    }

    private static void assertResource(GameTestHelper helper, String name) {
        helper.assertTrue(RetainedEndAndOreTests.class.getClassLoader().getResource(name) != null,
                "Missing original retained asset: " + name);
    }
}
