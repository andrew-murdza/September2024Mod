package net.amurdza.examplemod.worldgen;

import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.ArrayList;
import java.util.List;

/** Plant masking and tall-plant collision apply only to the retained starfish. */
public final class SmallAquaticPlants {
    private SmallAquaticPlants() { }

    public static boolean affected(Entity entity) {
        return entity instanceof StarfishEntity;
    }

    public static boolean blocksMovement(BlockState state) {
        return state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    /** Spawn validation must not rely on cached empty plant collision shapes. */
    public static boolean hasSpace(LevelReader level, Entity entity, AABB bounds) {
        if (!affected(entity)) return true;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(bounds.minX + 1.0E-7), Mth.floor(bounds.minY + 1.0E-7),
                Mth.floor(bounds.minZ + 1.0E-7), Mth.floor(bounds.maxX - 1.0E-7),
                Mth.floor(bounds.maxY - 1.0E-7), Mth.floor(bounds.maxZ - 1.0E-7))) {
            if (blocksMovement(level.getBlockState(pos))) return false;
        }
        return true;
    }

    public static Vec3 restrictMovement(Entity entity, Vec3 movement) {
        if (!affected(entity) || movement.lengthSqr() < 1.0E-12 || entity.noPhysics) return movement;
        AABB bounds = entity.getBoundingBox();
        AABB swept = bounds.expandTowards(movement).inflate(1.0E-7);
        List<VoxelShape> obstacles = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(swept.minX), Mth.floor(swept.minY), Mth.floor(swept.minZ),
                Mth.floor(swept.maxX), Mth.floor(swept.maxY), Mth.floor(swept.maxZ))) {
            if (entity.level().hasChunkAt(pos) && blocksMovement(entity.level().getBlockState(pos))) obstacles.add(Shapes.create(new AABB(pos)));
        }
        if (obstacles.isEmpty()) return movement;
        double y = Shapes.collide(Direction.Axis.Y, bounds, obstacles, movement.y);
        bounds = bounds.move(0, y, 0);
        boolean zFirst = Math.abs(movement.x) < Math.abs(movement.z);
        double x = movement.x, z = movement.z;
        if (zFirst) { z = Shapes.collide(Direction.Axis.Z, bounds, obstacles, z); bounds = bounds.move(0, 0, z); }
        x = Shapes.collide(Direction.Axis.X, bounds, obstacles, x);
        bounds = bounds.move(x, 0, 0);
        if (!zFirst) z = Shapes.collide(Direction.Axis.Z, bounds, obstacles, z);
        return new Vec3(x, y, z);
    }
}
