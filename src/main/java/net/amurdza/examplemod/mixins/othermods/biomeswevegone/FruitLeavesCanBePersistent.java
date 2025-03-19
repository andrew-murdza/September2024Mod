package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.potionstudios.biomeswevegone.world.level.block.plants.tree.leaves.BWGFruitLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BWGFruitLeavesBlock.class)
public class FruitLeavesCanBePersistent {
    @Redirect(method = "isRandomlyTicking",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    private Comparable hi(BlockState instance, Property property){
        return false;
    }
}
