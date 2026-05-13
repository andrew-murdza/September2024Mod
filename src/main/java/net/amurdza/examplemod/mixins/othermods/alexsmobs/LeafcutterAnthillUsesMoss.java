package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.world.FeatureLeafcutterAnthill;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FeatureLeafcutterAnthill.class, remap = false)
public abstract class LeafcutterAnthillUsesMoss {

    @Redirect(
            method = "place",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;COARSE_DIRT:Lnet/minecraft/world/level/block/Block;",
                    remap = true
            )
    )
    private Block aoemod$coarseDirtToMoss() {
        return Blocks.MOSS_BLOCK;
    }

    @Redirect(
            method = "place",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;DIRT:Lnet/minecraft/world/level/block/Block;",
                    remap = true
            )
    )
    private Block aoemod$dirtToMoss() {
        return Blocks.MOSS_BLOCK;
    }
}