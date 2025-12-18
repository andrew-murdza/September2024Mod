package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MountainLakeStructure extends Structure {

    public static final Codec<MountainLakeStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    settingsCodec(instance),
                    MountainLakeConfig.CODEC.fieldOf("lake").forGetter(s -> s.lakeConfig)
            ).apply(instance, MountainLakeStructure::new)
    );

    private final MountainLakeConfig lakeConfig;

    public MountainLakeStructure(StructureSettings settings, MountainLakeConfig lakeConfig) {
        super(settings);
        this.lakeConfig = lakeConfig;
    }

    @Override
    public @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {

        BlockPos chunkCenter = ctx.chunkPos().getWorldPosition().offset(8, 0, 8);

        return Optional.of(new GenerationStub(chunkCenter, (StructurePiecesBuilder builder) -> {
            RandomSource rand = ctx.random();

            int radius = randInt(rand, lakeConfig.radiusMin(), lakeConfig.radiusMax());
            int depth  = randInt(rand, lakeConfig.depthMin(), lakeConfig.depthMax());

            final int rampW = lakeConfig.rampW();

            // ✅ constants (not in config)
            final int waterlineOffset = -1; // water surface = minSurfaceY
            final int rimH = 0;            // no extra "ceiling" carve above water via rimH

            // ----------------------------
            // Min scan ONLY near bowl edge (radius + 2) to ignore ramp
            // ----------------------------
            int minSurfaceY = Integer.MAX_VALUE;

            final int scanR = radius + 2;
            final int scanR2 = scanR * scanR;
            final int step = 2;

            for (int dx = -scanR; dx <= scanR; dx += step) {
                for (int dz = -scanR; dz <= scanR; dz += step) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > scanR2) continue;

                    int x = chunkCenter.getX() + dx;
                    int z = chunkCenter.getZ() + dz;

                    int h = ctx.chunkGenerator().getFirstOccupiedHeight(
                            x, z,
                            Heightmap.Types.WORLD_SURFACE_WG,
                            ctx.heightAccessor(),
                            ctx.randomState()
                    );

                    if (h < minSurfaceY) minSurfaceY = h;
                }
            }

            if (minSurfaceY == Integer.MAX_VALUE) return;

            BlockPos origin = new BlockPos(chunkCenter.getX(), minSurfaceY, chunkCenter.getZ());

            // Water level is "level with land" (minSurfaceY), clamped to allowed range
            int waterY = Mth.clamp(minSurfaceY + waterlineOffset, lakeConfig.minWaterY(), lakeConfig.maxWaterY());

            // ----------------------------
            // Bounding box includes ramp + pad
            // ----------------------------
            final int pad = 10;
            int rTotal = radius + rampW + pad;

            int minX = origin.getX() - rTotal;
            int maxX = origin.getX() + rTotal;
            int minZ = origin.getZ() - rTotal;
            int maxZ = origin.getZ() + rTotal;

            int minY = waterY - depth - 16;
            int maxY = waterY + rimH + 96;

            builder.addPiece(new MountainLakePiece(
                    origin,
                    waterY,
                    radius,
                    depth,
                    rimH,
                    rampW,
                    lakeConfig.seafloorBlocks(),
                    lakeConfig.rampFill(),
                    minX, minY, minZ, maxX, maxY, maxZ
            ));
        }));
    }

    @Override
    public @NotNull StructureType<?> type() {
        return ModStructures.MOUNTAIN_LAKE_STRUCTURE.get();
    }

    private static int randInt(RandomSource rand, int a, int b) {
        if (a == b) return a;
        if (a > b) { int t = a; a = b; b = t; }
        return a + rand.nextInt(b - a + 1);
    }
}
