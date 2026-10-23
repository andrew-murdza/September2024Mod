package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public class NoBaseFirePlacementInTropical {
    private static final ResourceLocation SOUL_WEEDS = new ResourceLocation("nourished_nether", "soul_weeds");

    @Inject(method = "canBePlacedAt", at = @At("HEAD"), cancellable = true)
    private static void aoemod$cannotPlaceFireInTropical(
            Level pLevel, BlockPos pPos, Direction pDirection, CallbackInfoReturnable<Boolean> cir
    ) {
        if (aoemod$touchesProtectedFireTarget(pLevel, pPos, pDirection)) {
            cir.setReturnValue(false);
            return;
        }

        if (GameplayDimensions.is(pLevel, GameplayDimensions.TROPICAL)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean aoemod$touchesProtectedFireTarget(Level level, BlockPos firePos, Direction placementDirection) {
        if (aoemod$isProtected(level.getBlockState(firePos))
                || aoemod$isProtected(level.getBlockState(firePos.below()))) {
            return true;
        }

        if (placementDirection != null) {
            return aoemod$isProtected(level.getBlockState(firePos.relative(placementDirection.getOpposite())));
        }

        for (Direction direction : Direction.values()) {
            if (aoemod$isProtected(level.getBlockState(firePos.relative(direction)))) {
                return true;
            }
        }

        return false;
    }

    private static boolean aoemod$isProtected(BlockState state) {
        return state.is(ModBlocks.PALE_MOSS_BLOCK.get())
                || state.is(ModBlocks.PALE_MOSS_CARPET.get())
                || state.is(ModBlocks.PALE_HANGING_MOSS.get())
                || SOUL_WEEDS.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }
}
