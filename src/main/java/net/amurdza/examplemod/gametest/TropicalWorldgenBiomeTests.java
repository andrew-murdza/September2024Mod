package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalWorldgenBiomeTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void directlyConstructedWorldgenRegionUsesExactShoreBiomesAtEveryAltitude(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var random = level.getChunkSource().randomState();
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var layout = DimensionTerrain.tropicalLayout(random.router().temperature());
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, level, registry, null);
        // The real constructor creates a fresh BiomeManager(this, seed), rather
        // than calling the server manager's withDifferentSource. Override only
        // the raw quart source to avoid loading any live terrain in this audit.
        var region = new WorldGenRegion(level, List.of(chunk), ChunkStatus.FEATURES, 0) {
            @Override
            public Holder<Biome> getNoiseBiome(int x, int y, int z) {
                return source.getNoiseBiome(x, y, z, random.sampler());
            }
        };
        var unscoped = new BiomeManager((x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler()),
                BiomeManager.obfuscateSeed(level.getSeed()));
        int columns = 0, changedSeamColumns = 0;
        var habitats = new HashSet<>();
        for (int column = 0; column <= 1; column++) {
            var island = layout.island(column, 0);
            for (int direction = 0; direction < 8; direction++) {
                double angle = direction * Math.PI / 4;
                for (int radius = 0; radius <= 400; radius++) {
                    BlockPos pos = new BlockPos((int) Math.round(island.x() + radius * Math.cos(angle)), 63,
                            (int) Math.round(island.z() + radius * Math.sin(angle)));
                    var original = unscoped.getBiome(pos);
                    var expected = original;
                    if (original.is(TropicalChannelSurface.MANGROVE) || original.is(TropicalChannelSurface.RAINFOREST)
                            || original.is(TropicalChannelSurface.CHERRY)) {
                        expected = registry.getHolderOrThrow(switch (layout.sample(pos.getX(), pos.getZ()).biome()) {
                            case 3 -> TropicalChannelSurface.CHERRY;
                            case 2 -> TropicalChannelSurface.RAINFOREST;
                            default -> TropicalChannelSurface.MANGROVE;
                        });
                    }
                    if (!expected.equals(original)) changedSeamColumns++;
                    habitats.add(expected.unwrapKey().orElseThrow());
                    for (int y : new int[]{63, 120}) {
                        helper.assertTrue(region.getBiome(pos.atY(y)).equals(expected),
                                "WorldGenRegion vegetation lookup must match the exact displayed shore biome at " + pos.atY(y));
                    }
                    columns++;
                }
            }
        }
        helper.assertTrue(habitats.size() == 4 && changedSeamColumns > 0,
                "The constructor regression must cover all four habitats and actual quart-versus-contour disagreements");
        AOEMod.LOGGER.info("Direct WorldGenRegion tropical biome audit: {} columns, {} corrected seam columns",
                columns, changedSeamColumns);
        helper.succeed();
    }
}
