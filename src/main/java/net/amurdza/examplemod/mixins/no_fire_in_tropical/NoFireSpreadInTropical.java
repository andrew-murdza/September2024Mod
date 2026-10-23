package net.amurdza.examplemod.mixins.no_fire_in_tropical;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class NoFireSpreadInTropical {
    private static final ResourceLocation SOUL_WEEDS = new ResourceLocation("nourished_nether", "soul_weeds");

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aoemod$removeFireInTropicalBiomes(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (aoemod$touchesProtectedFireTarget(level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
            return;
        }

        if (GameplayDimensions.is(level, GameplayDimensions.TROPICAL)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    private static boolean aoemod$touchesProtectedFireTarget(ServerLevel level, BlockPos firePos) {
        if (aoemod$isProtected(level.getBlockState(firePos))
                || aoemod$isProtected(level.getBlockState(firePos.below()))) {
            return true;
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
