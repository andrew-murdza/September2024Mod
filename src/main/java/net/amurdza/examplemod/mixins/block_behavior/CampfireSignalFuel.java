package net.amurdza.examplemod.mixins.block_behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireSignalFuel {
    @Unique private static final ResourceLocation AOEMOD_CHARCOAL = new ResourceLocation("quark", "charcoal_block");

    @Inject(method = "isSmokeSource", at = @At("HEAD"), cancellable = true)
    private void aoemod$signalFuel(BlockState fuel, CallbackInfoReturnable<Boolean> result) {
        if (fuel.is(Blocks.COAL_BLOCK) || AOEMOD_CHARCOAL.equals(ForgeRegistries.BLOCKS.getKey(fuel.getBlock())))
            result.setReturnValue(true);
    }
}
