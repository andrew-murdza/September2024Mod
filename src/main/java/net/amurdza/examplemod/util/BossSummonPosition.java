package net.amurdza.examplemod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Find a supported room floor near a boss without jumping to the roof heightmap. */
public final class BossSummonPosition {
    private BossSummonPosition() { }

    @Nullable
    public static BlockPos find(ServerLevel level, Mob boss, EntityType<?> type, RandomSource random) {
        BlockPos origin = boss.blockPosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(11) - 5, dz = random.nextInt(11) - 5;
            if (dx * dx + dz * dz < 4) continue;
            for (int dy : new int[]{0, -1, 1, -2, 2, -3, -4, -5, -6, -7, -8}) {
                BlockPos feet = origin.offset(dx, dy, dz);
                if (!level.hasChunkAt(feet) || !level.getBlockState(feet.below())
                        .isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                var box = type.getDimensions().makeBoundingBox(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                if (!level.noCollision(box) || level.containsAnyLiquid(box)) continue;
                // A nearby clear floor can still belong to another room or lie
                // above a low ceiling. Keep helpers in the boss's open chamber.
                Vec3 from = Vec3.atBottomCenterOf(origin).add(0, .5, 0);
                Vec3 to = Vec3.atBottomCenterOf(feet).add(0, .5, 0);
                if (level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, boss)).getType() == HitResult.Type.MISS) return feet;
            }
        }
        return null;
    }
}
