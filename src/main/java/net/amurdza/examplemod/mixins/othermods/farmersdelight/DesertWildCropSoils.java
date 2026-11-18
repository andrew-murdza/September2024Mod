package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.WildCropBlock;

@Mixin(WildCropBlock.class)
public abstract class DesertWildCropSoils {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoe$desertWildCropSoils(BlockState ground, BlockGetter level, BlockPos pos,
                                        CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey((Block) (Object) this);
        if (id == null) return;
        if (id.toString().equals("vintagedelight:wild_peanuts")
                && (ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND))) {
            cir.setReturnValue(ground.is(Blocks.SAND));
        }
    }
}
