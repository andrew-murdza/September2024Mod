package net.amurdza.examplemod.mixins.othermods.backport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native Backport dry grass keeps the original AoE grasses' sand-only soil. */
@Mixin(DeadBushBlock.class)
public abstract class DryGrassOnSand {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void aoemod$dryGrassSoil(BlockState ground, BlockGetter level, BlockPos pos,
                                    CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey((Block) (Object) this);
        if (id != null && id.getNamespace().equals("minecraft")
                && (id.getPath().equals("short_dry_grass") || id.getPath().equals("tall_dry_grass"))) {
            cir.setReturnValue(ground.is(Blocks.SAND));
        }
    }
}
