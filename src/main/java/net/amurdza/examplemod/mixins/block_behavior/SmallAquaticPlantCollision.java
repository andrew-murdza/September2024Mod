package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.worldgen.SmallAquaticPlants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class SmallAquaticPlantCollision {
    // Decide before the cached vanilla shape and other cancellable RETURN hooks (including aquatic soil support).
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void aoemod$keepSmallAquaticsOutOfTallPlants(BlockGetter level, BlockPos pos, CollisionContext context,
                                                       CallbackInfoReturnable<VoxelShape> cir) {
        if (SmallAquaticPlants.blocksMovement((BlockState) (Object) this)
                && context instanceof EntityCollisionContext entityContext
                && SmallAquaticPlants.affected(entityContext.getEntity())) cir.setReturnValue(Shapes.block());
    }
}
