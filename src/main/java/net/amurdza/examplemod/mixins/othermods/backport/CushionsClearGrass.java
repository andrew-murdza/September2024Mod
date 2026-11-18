package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.level.entities.decoration.BlockAttachedEntity;
import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cushion entities are placed before grass decoration, so clear their footprint once they tick. */
@Mixin(BlockAttachedEntity.class)
public abstract class CushionsClearGrass {
    @Inject(method = "tick", at = @At("HEAD"))
    private void aoemod$clearGrassInsideCushion(CallbackInfo ci) {
        if (!((Object) this instanceof Cushion cushion) || cushion.level().isClientSide) return;
        var bounds = cushion.getBoundingBox().deflate(1.0E-7);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            var state = cushion.level().getBlockState(pos);
            if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
                cushion.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
