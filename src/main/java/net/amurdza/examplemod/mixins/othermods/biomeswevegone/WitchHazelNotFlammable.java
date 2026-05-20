package net.amurdza.examplemod.mixins.othermods.biomeswevegone;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.potionstudios.biomeswevegone.world.level.block.BWGBlocks;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FireBlock.class,remap = false)
public class WitchHazelNotFlammable {

    @Inject(method = "canCatchFire", at = @At("HEAD"), cancellable = true)
    private void aoemod$witchHazelCannotBurn(BlockGetter world, BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (september2024Mod$isWitchHazel(world.getBlockState(pos))) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean september2024Mod$isWitchHazel(BlockState state) {
        Block block = state.getBlock();

        return block == BWGWood.WITCH_HAZEL.planks()
                || block == BWGWood.WITCH_HAZEL.slab()
                || block == BWGWood.WITCH_HAZEL.stairs()
                || block == BWGWood.WITCH_HAZEL.logstem()
                || block == BWGWood.WITCH_HAZEL.strippedLogStem()
                || block == BWGWood.WITCH_HAZEL.wood()
                || block == BWGWood.WITCH_HAZEL.strippedWood()
                || block == BWGWood.WITCH_HAZEL.sign()
                || block == BWGWood.WITCH_HAZEL.wallSign()
                || block == BWGWood.WITCH_HAZEL.hangingSign()
                || block == BWGWood.WITCH_HAZEL.wallHangingSign()
                || block == BWGWood.WITCH_HAZEL.pressurePlate()
                || block == BWGWood.WITCH_HAZEL.trapdoor()
                || block == BWGWood.WITCH_HAZEL.button()
                || block == BWGWood.WITCH_HAZEL.fenceGate()
                || block == BWGWood.WITCH_HAZEL.fence()
                || block == BWGWood.WITCH_HAZEL.door()
                || block == BWGWood.WITCH_HAZEL.bookshelf()
                || block == BWGWood.WITCH_HAZEL.craftingTable()
                || block == BWGWood.WITCH_HAZEL.leaves()
                || block == BWGWood.WITCH_HAZEL.sapling().getBlock()
                || block == BWGWood.WITCH_HAZEL.sapling().getPottedBlock()
                || block == BWGWood.BLOOMING_WITCH_HAZEL_LEAVES.get()
                || block == BWGBlocks.WITCH_HAZEL_BRANCH.get()
                || block == BWGBlocks.WITCH_HAZEL_BLOSSOM.get();
    }
}
