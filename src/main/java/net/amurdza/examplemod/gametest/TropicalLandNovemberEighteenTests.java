package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.PlantGenerationSupport;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.TropicalBamboo;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.mcreator.biodiverse.entity.CassowaryEntity;
import net.mcreator.biodiverse.init.BiodiverseModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalLandNovemberEighteenTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void everyEligibleY75ChunkGetsBambooUnlessItHasAY75Cherry(GameTestHelper helper) {
        for (int ground : new int[]{74, 75, 76}) for (int chunkX = -2; chunkX <= 2; chunkX++) {
            var chunk = new ChunkPos(chunkX, -1);
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            var world = fixture(helper, blocks, ground, false);
            int count = TropicalBamboo.placeChunk(world, chunk);
            helper.assertTrue(count == (ground == 75 ? 10 : 0), "Only Y75 terrace chunks receive bamboo");
            for (var entry : blocks.entrySet()) if (entry.getValue().is(Blocks.BAMBOO)) {
                BlockPos pos = entry.getKey();
                helper.assertTrue(pos.getY() >= 76 && new ChunkPos(pos).equals(chunk), "Bamboo escaped its terrace/chunk");
            }
            if (ground != 75) continue;
            blocks.clear();
            blocks.put(new BlockPos(chunk.getMinBlockX() + 8, 76, chunk.getMinBlockZ() + 8), Blocks.CHERRY_LOG.defaultBlockState());
            helper.assertTrue(TropicalBamboo.placeChunk(world, chunk) == 0, "A Y75 cherry must suppress that chunk's bamboo");
            blocks.clear();
            blocks.put(new BlockPos(chunk.getMinBlockX() + 8, 77, chunk.getMinBlockZ() + 8), Blocks.CHERRY_LOG.defaultBlockState());
            helper.assertTrue(TropicalBamboo.placeChunk(world, chunk) == 10, "A cherry above the terrace must not suppress Y75 bamboo");
        }
        for (int available = 1; available < 10; available++) {
            Map<BlockPos, BlockState> sliver = new HashMap<>();
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                sliver.put(new BlockPos(x, 76, z), Blocks.STONE.defaultBlockState());
            for (int i = 0; i < available; i++) sliver.put(new BlockPos(5 + i % 3, 76, 5 + i / 3), Blocks.AIR.defaultBlockState());
            var fragment = fixture(helper, sliver, 75, false);
            helper.assertTrue(TropicalBamboo.placeChunk(fragment, new ChunkPos(0, 0)) == available,
                    "Every narrow terrace fragment needs all available stems: " + available);
            helper.assertTrue(sliver.entrySet().stream().filter(e -> e.getValue().is(Blocks.BAMBOO)).allMatch(e -> e.getKey().getY() >= 76),
                    "Partial patches must never move below the Y75 terrace");
        }
        Map<BlockPos, BlockState> edge = new HashMap<>();
        var world = fixture(helper, edge, 75, true);
        helper.assertTrue(TropicalBamboo.placeChunk(world, new ChunkPos(0, 0)) == 10, "A mixed biome chunk still needs its eligible bamboo");
        helper.assertTrue(edge.entrySet().stream().filter(e -> e.getValue().is(Blocks.BAMBOO)).allMatch(e -> e.getKey().getX() < 8),
                "Every bamboo stem must remain in cherry grove");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void patchesStayInTheirBiomeAndY75HasNoFlowersOrWildCrops(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var terrace = fixture(helper, blocks, 75, false);
        for (String species : new String[]{"minecraft:allium", "minecraft:peony", "minecraft:pink_petals",
                "farmersdelight:wild_carrots", "farmersdelight:wild_onions", "legumedelight:wild_beans"}) {
            helper.assertFalse(DocumentedFloraFeature.placePlant(terrace, new BlockPos(2, 76, 2), species, RandomSource.create(3)),
                    "Y75 must exclude flowers and wild crops: " + species);
        }
        helper.assertFalse(PlantGenerationSupport.isAllowed(terrace, new BlockPos(2, 77, 2),
                        Blocks.PEONY.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                "The upper half must obey its Y75 flower root");
        helper.assertTrue(PlantGenerationSupport.isAllowed(terrace, new BlockPos(2, 76, 2), Blocks.CHERRY_LEAVES.defaultBlockState()),
                "Flower-tagged tree leaves must remain permitted");
        var boundary = fixture(helper, blocks, 80, true);
        int count = TropicalVegetation.placePatch(boundary, new BlockPos(7, 81, 8), "cherry_grove", "minecraft:allium", RandomSource.create(27));
        helper.assertTrue(count > 0 && blocks.keySet().stream().allMatch(pos -> pos.getX() < 8),
                "Cherry flower patches must not spill into rainforest");
        blocks.clear();
        count = TropicalVegetation.placePatch(boundary, new BlockPos(8, 81, 8), "rainforest", "minecraft:rose_bush", RandomSource.create(27));
        helper.assertTrue(count > 0 && blocks.keySet().stream().allMatch(pos -> pos.getX() >= 8),
                "Rainforest flowers must not spill into cherry grove");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void adultCassowariesLayNativeEggsAndShedFeathersWithSavedTimers(GameTestHelper helper) {
        var level = helper.getLevel();
        var bird = (CassowaryEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("biodiverse:cassowary")).create(level);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        bird.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        CompoundTag data = new CompoundTag();
        bird.addAdditionalSaveData(data);
        data.putInt("AOEModCassowaryEggTime", 0);
        data.putInt("AOEModFeatherDropTime", 0);
        bird.readAdditionalSaveData(data);
        var bounds = bird.getBoundingBox().inflate(2);
        level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
        bird.aiStep();
        var drops = level.getEntitiesOfClass(ItemEntity.class, bounds);
        helper.assertTrue(drops.stream().filter(item -> item.getItem().is(BiodiverseModItems.CASSOWARY_EGG.get())).count() == 1,
                "An adult must lay a native hatchable cassowary egg when its timer expires");
        helper.assertTrue(drops.stream().filter(item -> item.getItem().is(Items.FEATHER)).count() == 1,
                "An adult must shed one feather when its timer expires");
        bird.addAdditionalSaveData(data);
        for (String key : new String[]{"AOEModCassowaryEggTime", "AOEModFeatherDropTime"}) {
            helper.assertTrue(data.getInt(key) >= 6000 && data.getInt(key) < 12000, "Reset timer must match chickens: " + key);
        }
        var restored = (CassowaryEntity) bird.getType().create(level);
        restored.readAdditionalSaveData(data);
        CompoundTag saved = new CompoundTag();
        restored.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getInt("AOEModCassowaryEggTime") == data.getInt("AOEModCassowaryEggTime")
                && saved.getInt("AOEModFeatherDropTime") == data.getInt("AOEModFeatherDropTime"), "Yield timers must survive reload");
        drops.forEach(ItemEntity::discard);
        data.putInt("Age", -24000);
        data.putInt("AOEModCassowaryEggTime", 0);
        data.putInt("AOEModFeatherDropTime", 0);
        bird.readAdditionalSaveData(data);
        bird.aiStep();
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(), "Babies must not produce eggs or feathers");
        helper.succeed();
    }

    private static WorldGenLevel fixture(GameTestHelper helper, Map<BlockPos, BlockState> blocks, int ground, boolean boundary) {
        var world = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var registry = world.registryAccess().registryOrThrow(Registries.BIOME);
        var cherry = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("cherry_grove")));
        var forest = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID("rainforest")));
        java.util.function.Function<BlockPos, BlockState> stateAt = pos -> blocks.getOrDefault(pos,
                pos.getY() <= ground ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        return (WorldGenLevel) Proxy.newProxyInstance(TropicalLandNovemberEighteenTests.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getLevel": return world;
                        case "getBiome": return boundary && ((BlockPos) args[0]).getX() >= 8 ? forest : cherry;
                        case "getHeight": return args == null || args.length == 0 ? world.getHeight() : ground + 1;
                        case "getBlockState": return stateAt.apply((BlockPos) args[0]);
                        case "getFluidState": return stateAt.apply((BlockPos) args[0]).getFluidState();
                        case "isEmptyBlock": return stateAt.apply((BlockPos) args[0]).isAir();
                        case "isStateAtPosition": return ((Predicate<BlockState>) args[1]).test(stateAt.apply((BlockPos) args[0]));
                        case "setBlock": blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]); return true;
                        default:
                            try { return method.invoke(world, args); }
                            catch (InvocationTargetException exception) { throw exception.getCause(); }
                    }
                });
    }
}
