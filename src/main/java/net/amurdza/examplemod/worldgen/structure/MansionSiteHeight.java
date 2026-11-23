package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.worldgen.CropTerrainHeights;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.IntBinaryOperator;

/** Samples only the flat forest footprint; the descending ramp never raises the mansion. */
public final class MansionSiteHeight {
    private MansionSiteHeight() { }

    public static int highestGround(Structure.GenerationContext context, BoundingBox house) {
        IntBinaryOperator heights;
        if (context.chunkGenerator() instanceof NoiseBasedChunkGenerator generator) {
            var columns = new CropTerrainHeights(context.heightAccessor(), generator, context.randomState());
            heights = (x, z) -> columns.height(x, z) - 1;
        } else {
            heights = (x, z) -> context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState()) - 1;
        }
        return highestGround(house, heights);
    }

    public static int highestGround(BoundingBox house, IntBinaryOperator heights) {
        return highestGround(house, MansionEstateShape.WIDE_CIRCLE_VERSION, heights);
    }

    public static int highestGround(BoundingBox house, int layoutVersion, IntBinaryOperator heights) {
        var shape = new MansionEstateShape(house, layoutVersion);
        double centerX = shape.centerX(), centerZ = shape.centerZ();
        // Only the flat forest determines the estate elevation. The descending
        // ramp may extend as far as needed, but hills outside the forest must
        // neither raise the mansion nor enlarge the height-sampling circle.
        double radius = shape.halfWidth();
        int highest = Integer.MIN_VALUE;
        for (int x = (int) Math.ceil(centerX - radius); x <= (int) Math.floor(centerX + radius); x++) {
            for (int z = (int) Math.ceil(centerZ - radius); z <= (int) Math.floor(centerZ + radius); z++) {
                if (shape.contains(x, z))
                    highest = Math.max(highest, heights.applyAsInt(x, z));
            }
        }
        return highest;
    }
}
