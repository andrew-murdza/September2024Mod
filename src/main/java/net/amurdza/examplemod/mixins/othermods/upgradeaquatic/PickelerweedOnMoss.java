package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.block.PickerelweedPlantBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PickerelweedPlantBlock.class)
public class PickelerweedOnMoss {
    @Redirect(method = "isValidGround",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block hi(BlockState instance){
        return instance.is(BlockTags.DIRT)||instance.is(Blocks.CLAY)?Blocks.DIRT:Blocks.AIR;
    }

}
