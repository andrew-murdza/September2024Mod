package net.amurdza.examplemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Aquatic bodies stand on the visible surface of recessed soils such as mud and soul sand. */
public final class AquaticGroundSupport {
    private AquaticGroundSupport() { }

    public static VoxelShape collision(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context, VoxelShape original) {
        if (original.isEmpty() || original.max(Direction.Axis.Y) >= 1
                || !(context instanceof EntityCollisionContext entityContext)
                || !isAquatic(entityContext.getEntity())) return original;
        // Both shapes must describe a complete visible supporting block. This
        // excludes slabs, stairs, snow layers, plants, and genuine openings.
        if (!Block.isShapeFullBlock(state.getBlockSupportShape(level, pos))
                || !Block.isShapeFullBlock(state.getVisualShape(level, pos, CollisionContext.empty()))) return original;
        return Shapes.block();
    }

    private static boolean isAquatic(Entity entity) {
        if (entity == null) return false;
        MobCategory category = entity.getType().getCategory();
        return entity instanceof WaterAnimal || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || DocumentedFauna.isWaterSpecies(entity.getType());
    }
}
