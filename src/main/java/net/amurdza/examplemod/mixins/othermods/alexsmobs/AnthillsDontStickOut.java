package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.world.FeatureLeafcutterAnthill;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FeatureLeafcutterAnthill.class, remap = false)
public class AnthillsDontStickOut {

    /*
     * Original mound box:
     * heightPos.offset(-airs, 0, -j) to heightPos.offset(airs, 3, j)
     *
     * New mound box:
     * heightPos.offset(-airs, -3, -j) to heightPos.offset(airs, 0, j)
     *
     * This keeps the top of the mound at ground level.
     */
    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 0
            ),
            remap = true
    )
    private BlockPos aoemod$moundBottomBelowGround(BlockPos heightPos, int x, int y, int z) {
        return heightPos.offset(x, y - 3, z);
    }

    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;",
                    ordinal = 1
            ),
            remap = true
    )
    private BlockPos aoemod$moundTopAtGround(BlockPos heightPos, int x, int y, int z) {
        return heightPos.offset(x, y - 3, z);
    }

    /*
     * Original anthill block position:
     * heightPos.above(outOfGround)
     *
     * New anthill block position:
     * heightPos
     */
    @Redirect(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;above(I)Lnet/minecraft/core/BlockPos;",
                    ordinal = 2
            ),
            remap = true
    )
    private BlockPos aoemod$anthillAtGroundLevel(BlockPos heightPos, int outOfGround) {
        return heightPos;
    }
}