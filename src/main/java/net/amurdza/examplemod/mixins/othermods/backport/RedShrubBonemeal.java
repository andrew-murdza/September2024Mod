package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.level.blocks.ActualBushBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActualBushBlock.class)
public abstract class RedShrubBonemeal {
    @Inject(method = {"getShape", "m_5940_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aoe$redShrubUsesRootShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(new ResourceLocation("minecraft", "red_shrub"))) {
            cir.setReturnValue(Blocks.CRIMSON_ROOTS.defaultBlockState().getShape(level, pos, context));
        }
    }

    // The dependency method implements a vanilla interface; include dev and production names.
    @Inject(method = {"isValidBonemealTarget", "m_7370_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aoe$redShrubBehavesLikeRoots(LevelReader level, BlockPos pos, BlockState state,
                                            boolean client, CallbackInfoReturnable<Boolean> cir) {
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(new ResourceLocation("minecraft", "red_shrub"))) {
            cir.setReturnValue(false);
        }
    }
}
