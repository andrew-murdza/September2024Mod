package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.block.BlockLeafcutterAntChamber;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLeafcutterAntChamber.class)
public class AntsDontGetAngryWithHarvest {
    @Redirect(method = "use",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block hi(BlockState instance){
        return Blocks.SHROOMLIGHT;
    }
}
