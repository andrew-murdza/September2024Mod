package net.amurdza.examplemod.mixins.block_placement;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmBlock.class)
public class FarmlandNotKilledByPumpkinMelon {
    @Redirect(method = "canSurvive",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolid()Z"))
    private boolean hi(BlockState instance){
        return instance.isSolid()&&!instance.is(Blocks.PUMPKIN)&&!instance.is(Blocks.MELON);
    }
}
