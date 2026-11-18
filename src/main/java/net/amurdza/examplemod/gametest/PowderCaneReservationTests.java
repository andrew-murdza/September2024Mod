package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.OrePlacementContext;
import net.amurdza.examplemod.worldgen.PowderCanePatches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowderCaneReservationTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void reservedGrovesRejectCompetingOreAndGlowstoneButPermitFiveTrees(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var patches = PowderCanePatches.plan(nether, -7, 8);
        helper.assertTrue(patches.size() == 1, "The test tile needs a complete dry reservation");
        var patch = patches.get(0);
        var chunk = nether.getChunk(Math.floorDiv(patch.x(), 16), Math.floorDiv(patch.z(), 16));
        Holder<Biome> basalt = nether.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:basalt_deltas")));
        Map<BlockPos, BlockState> writes = new HashMap<>();
        var region = new WorldGenRegion(nether, List.of(chunk), ChunkStatus.FEATURES, 0) {
            @Override public Holder<Biome> getBiome(BlockPos pos) { return basalt; }
            @Override public BlockState getBlockState(BlockPos pos) {
                return writes.getOrDefault(pos, (pos.getY() <= 8 || pos.getY() >= 25 ? Blocks.BASALT : Blocks.AIR)
                        .defaultBlockState());
            }
            @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
                if (!ensureCanWrite(pos)) return false;
                writes.put(pos.immutable(), state);
                return true;
            }
        };
        // Choose a reserved column outside the independently protected ramp passage.
        var terrain = net.amurdza.examplemod.worldgen.NetherRampDecorationGuard
                .terrain(nether.getChunkSource().randomState()).orElseThrow();
        BlockPos floor = null;
        for (int dx = -4; dx <= 4 && floor == null; dx++) for (int dz = -4; dz <= 4; dz++) {
            int x = patch.x() + dx, z = patch.z() + dz;
            if (net.amurdza.examplemod.worldgen.DimensionTerrain.netherRampDistance(z,
                    terrain.rampDetail().compute(new DensityFunction.SinglePointContext(x, 0, z))) >= 8) {
                floor = new BlockPos(x, 8, z);
                break;
            }
        }
        helper.assertTrue(floor != null, "Reserved terrain must offer a column away from the ramp walkway");
        BlockPos inside = floor;
        BlockPos outside = new BlockPos(patch.x() + (Math.floorMod(patch.x(), 16) < 8 ? 5 : -5), 8, patch.z());
        BlockState basaltState = Blocks.BASALT.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
        helper.assertFalse(PowderCanePatches.allowsOre(region, inside, basaltState, glowstone),
                "Floor ore blobs cannot replace a reserved cane substrate before vegetation runs");
        helper.assertTrue(PowderCanePatches.allowsOre(region, outside, basaltState, glowstone)
                        && PowderCanePatches.allowsOre(region, inside.below(), basaltState, glowstone)
                        && PowderCanePatches.allowsOre(region, inside, basaltState, Blocks.BLACKSTONE.defaultBlockState()),
                "The reservation must leave outside ores, buried ores, and compatible basalt materials unchanged");

        var target = OreConfiguration.target(new BlockMatchTest(Blocks.BASALT), glowstone);
        var configuration = new OreConfiguration(List.of(target), 8);
        helper.assertFalse(OrePlacementContext.run(region, () -> OreFeature.canPlaceOre(basaltState,
                        region::getBlockState, RandomSource.create(31), configuration, target, inside.mutable())),
                "The real OreFeature block-write predicate must enforce the reservation");
        helper.assertTrue(OrePlacementContext.run(nether, () -> OreFeature.canPlaceOre(basaltState,
                        region::getBlockState, RandomSource.create(31), configuration, target, inside.mutable())),
                "Ordinary runtime feature placement must not inherit the natural-generation reservation");

        BlockPos canopy = inside.atY(24);
        helper.assertFalse(PowderCanePatches.allowsGlowstone(region, canopy, glowstone),
                "Low hanging glowstone cannot occupy the grove's reserved headroom");
        helper.assertTrue(PowderCanePatches.allowsGlowstone(region, canopy.atY(36), glowstone)
                        && PowderCanePatches.allowsGlowstone(region, outside.atY(24), glowstone)
                        && PowderCanePatches.allowsGlowstone(nether, canopy, glowstone)
                        && PowderCanePatches.allowsGlowstone(region, canopy,
                        com.soytutta.mynethersdelight.common.registry.MNDBlocks.POWDERY_CANE.get().defaultBlockState()),
                "Upper/outside glowstone, player placement, and the reserved cane itself must remain permitted");
        helper.assertFalse(Feature.GLOWSTONE_BLOB.place(NoneFeatureConfiguration.INSTANCE, region,
                        nether.getChunkSource().getGenerator(), stationaryRandom(), canopy) || !writes.isEmpty(),
                "The actual hanging-glowstone feature must reject its whole conflicting shape before mutation");
        helper.assertTrue(Feature.GLOWSTONE_BLOB.place(NoneFeatureConfiguration.INSTANCE, region,
                        nether.getChunkSource().getGenerator(), stationaryRandom(), outside.atY(24)) && writes.size() == 1,
                "An otherwise identical hanging cluster outside the footprint must still generate");
        writes.clear();
        helper.assertTrue(PowderCanePatches.placePatch(region, patch) == 5,
                "A protected grove must still commit all five of its own mature canes");
        helper.succeed();
    }

    private static RandomSource stationaryRandom() {
        RandomSource delegate = RandomSource.create(31);
        // Keep the vanilla cluster at its origin to isolate reservation rejection from growth bounds.
        return (RandomSource) Proxy.newProxyInstance(PowderCaneReservationTests.class.getClassLoader(),
                new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                    if (method.getName().equals("nextInt")) return 0;
                    try { return method.invoke(delegate, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }
}
