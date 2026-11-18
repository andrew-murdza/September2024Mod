package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.worldgen.AquaticGroundSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.spongepowered.asm.mixin.Unique;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply the entity-specific floor after vanilla's cached collision-shape lookup. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AquaticSoilCollision {
    @Unique private static final Map<VoxelShape, VoxelShape> AOEMOD_FENCE_SHAPES =
            java.util.Collections.synchronizedMap(new IdentityHashMap<>());

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"), cancellable = true)
    private void aoemod$aquaticFloor(BlockGetter level, BlockPos pos, CollisionContext context,
                                     CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState) (Object) this;
        VoxelShape original = cir.getReturnValue();
        if (!original.isEmpty() && (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES))
                && context instanceof EntityCollisionContext collision && collision.getEntity() instanceof Animal animal
                && !(animal.getNavigation() instanceof FlyingPathNavigation) && !(animal.getNavigation() instanceof WaterBoundPathNavigation)) {
            cir.setReturnValue(AOEMOD_FENCE_SHAPES.computeIfAbsent(original, shape -> Shapes.or(shape, shape.move(0, .5, 0))));
        } else {
            cir.setReturnValue(AquaticGroundSupport.collision(state, level, pos, context, original));
        }
    }
}
