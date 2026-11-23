package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.TreeGrowthPlacementRules;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyEcologyTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void forestsHaveDistinctAnimalsAndNativeCoats(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var registry = snowy.registryAccess().registryOrThrow(Registries.BIOME);
        for (String name : new String[]{"snowy_forest", "dappled_forest", "cold_ocean"}) {
            var biome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
            boolean taiga = name.equals("snowy_forest");
            boolean cold = !name.equals("dappled_forest");
            helper.assertTrue(biome.is(BiomeTags.SPAWNS_WHITE_RABBITS) == cold, "Rabbit coat tag differs in " + name);
            helper.assertTrue(biome.is(BiomeTags.SPAWNS_SNOW_FOXES) == cold, "Fox coat tag differs in " + name);
            Set<String> expected = switch (name) {
                case "snowy_forest" -> Set.of("minecraft:goat", "biodiverse:reindeer", "minecraft:rabbit",
                        "minecraft:polar_bear", "minecraft:fox");
                case "dappled_forest" -> Set.of("aoemod:yak", "minecraft:rabbit", "minecraft:fox");
                default -> Set.of();
            };
            var declared = DocumentedFauna.forBiome(name).stream()
                    .filter(species -> species.habitat() == DocumentedFauna.Habitat.LAND)
                    .map(species -> species.id().toString()).collect(Collectors.toSet());
            helper.assertTrue(declared.equals(expected), "Incorrect land fauna in " + name + ": " + declared);
            var loaded = Arrays.stream(MobCategory.values()).flatMap(category -> biome.value().getMobSettings()
                            .getMobs(category).unwrap().stream())
                    .map(spawn -> ForgeRegistries.ENTITY_TYPES.getKey(spawn.type).toString()).collect(Collectors.toSet());
            helper.assertTrue(loaded.containsAll(expected), "The loaded biome is missing requested animals: " + name);
            helper.assertTrue(loaded.contains("aoemod:yak") != cold, "Yaks must spawn only in dappled forest");
            helper.assertTrue(loaded.contains("minecraft:goat") == taiga && loaded.contains("biodiverse:reindeer") == taiga,
                    "Goats and reindeer must stay in snowy taiga");
            if (taiga) {
                var herdIds = Set.of("minecraft:rabbit", "minecraft:goat", "biodiverse:reindeer");
                var pool = biome.value().getMobSettings().getMobs(MobCategory.CREATURE);
                var herds = pool.unwrap().stream().filter(entry -> herdIds.contains(
                        ForgeRegistries.ENTITY_TYPES.getKey(entry.type).toString())).toList();
                helper.assertTrue(herds.size() == 3 && herds.stream().map(entry -> entry.type).distinct().count() == 3
                                && herds.stream().allMatch(entry -> entry.getWeight().asInt() == 10
                                && entry.minCount == 2 && entry.maxCount == 4),
                        "Snowy rabbits, goats and reindeer must each have one equally weighted herd of two to four");
            }
            boolean ocean = name.equals("cold_ocean");
            helper.assertTrue(!loaded.contains("iceandfire:hippocampus") && loaded.contains("aoemod:nautilus") == ocean,
                    "The retained nautilus belongs to cold ocean, while forests and all Snowy biomes exclude hippocampi: " + name);
            var habitat = habitat(snowy, name);
            for (int i = 0; i < 12; i++) {
                var rabbit = EntityType.RABBIT.create(snowy);
                rabbit.finalizeSpawn(habitat, snowy.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.CHUNK_GENERATION, null, null);
                boolean snowRabbit = rabbit.getVariant() == Rabbit.Variant.WHITE || rabbit.getVariant() == Rabbit.Variant.WHITE_SPLOTCHED;
                helper.assertTrue(snowRabbit == cold, "Rabbit spawned with the wrong coat in " + name);
            }
            var fox = EntityType.FOX.create(snowy);
            fox.finalizeSpawn(habitat, snowy.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.CHUNK_GENERATION, null, null);
            helper.assertTrue((fox.getVariant() == Fox.Type.SNOW) == cold, "Fox spawned with the wrong coat in " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyForestFoxesOverrideInheritedRedGroupCoats(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var forest = habitat(snowy, "snowy_forest");
        for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.SPAWN_EGG}) {
            var fox = EntityType.FOX.create(snowy);
            fox.setVariant(Fox.Type.RED);
            fox.finalizeSpawn(forest, snowy.getCurrentDifficultyAt(BlockPos.ZERO), reason,
                    new Fox.FoxGroupData(Fox.Type.RED), null);
            helper.assertTrue(fox.getVariant() == Fox.Type.SNOW,
                    "A red parent/group or egg must not override the Snowy forest coat: " + reason);
            fox.discard();
        }
        var dappledFox = EntityType.FOX.create(snowy);
        dappledFox.finalizeSpawn(habitat(snowy, "dappled_forest"), snowy.getCurrentDifficultyAt(BlockPos.ZERO),
                MobSpawnType.NATURAL, new Fox.FoxGroupData(Fox.Type.RED), null);
        helper.assertTrue(dappledFox.getVariant() == Fox.Type.RED, "The warmer dappled forest must keep its red foxes");
        dappledFox.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void grassSeedsAndPlantOriginsFollowTheirForest(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var taiga = habitat(snowy, "snowy_forest");
        var dappled = habitat(snowy, "dappled_forest");
        var pos = new BlockPos(0, 100, 0);
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN}) {
            var taigaDrops = BlockConfig.blockDropEntries(taiga, pos, block.defaultBlockState());
            var dappledDrops = BlockConfig.blockDropEntries(dappled, pos, block.defaultBlockState());
            helper.assertFalse(taigaDrops.isEmpty() || dappledDrops.isEmpty(), "Both forests must have a seed pool");
            helper.assertTrue(taigaDrops.stream().allMatch(entry -> entry.item() == Items.BEETROOT_SEEDS), "Taiga drops beetroot seeds");
            helper.assertTrue(dappledDrops.stream().allMatch(entry -> entry.item() == Items.WHEAT_SEEDS),
                    "Dappled drops wheat seeds");
            helper.assertTrue(BlockConfig.blockDropAmount(block.defaultBlockState(), taigaDrops.get(0))
                            == BlockConfig.blockDropAmount(block.defaultBlockState(), dappledDrops.get(0)),
                    "Changing seed species must preserve the configured drop chance");
        }
        var shrub = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:red_shrub"));
        var poplar = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:poplar_sapling"));
        var poplarLog = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:poplar_log"));
        var oatCrop = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("vintagedelight:oat_crop"));
        for (var plant : new net.minecraft.world.level.block.Block[]{shrub, poplar, Blocks.WHEAT, oatCrop,
                Blocks.PUMPKIN, Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.SUGAR_CANE}) {
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(dappled, pos, plant.defaultBlockState()), "Dappled plant rejected: " + plant);
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(taiga, pos, plant.defaultBlockState()), "Snowy's shared placement list must accept the dappled plant in taiga: " + plant);
        }
        for (var bush : new net.minecraft.world.level.block.Block[]{Blocks.SWEET_BERRY_BUSH,
                net.amurdza.examplemod.registry.ModBlocks.BLUE_BERRY_BUSH.get()}) {
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(taiga, pos, bush.defaultBlockState()), "Berry bush rejected in taiga");
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(dappled, pos, bush.defaultBlockState()), "Players can place Snowy berries in dappled forest");
        }
        helper.assertTrue(TreeGrowthPlacementRules.isAllowed(dappled, pos, pos.above(), poplarLog.defaultBlockState()), "Poplar must grow in dappled forest");
        helper.assertTrue(TreeGrowthPlacementRules.isAllowed(taiga, pos, pos.above(), poplarLog.defaultBlockState()), "A player-planted poplar can grow in taiga");
        for (var habitat : new ServerLevelAccessor[]{taiga, dappled}) {
            helper.assertTrue(TreeGrowthPlacementRules.isAllowed(habitat, pos, pos.above(), Blocks.SPRUCE_LOG.defaultBlockState()),
                    "The reference spruce trees must be able to grow in either forest");
        }
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        helper.assertFalse(BiomePlacementRestrictions.isAllowed(habitat(nether, "crimson_forest"), pos, shrub.defaultBlockState()),
                "Red shrubs must no longer belong in crimson forest");
        helper.assertTrue(BiomePlacementRestrictions.isAllowed(dappled, pos, shrub.defaultBlockState()),
                "Red shrubs belong in dappled forest");
        helper.succeed();
    }

    private static ServerLevelAccessor habitat(ServerLevel level, String name) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
        return (ServerLevelAccessor) Proxy.newProxyInstance(SnowyEcologyTests.class.getClassLoader(),
                new Class<?>[]{ServerLevelAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "getBiome" -> biome;
                    default -> method.invoke(level, args);
                });
    }
}
