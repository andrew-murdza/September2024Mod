package net.amurdza.examplemod.mixins.othermods.quark;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.violetmoon.quark.content.world.block.GlowShroomBlock;
import org.violetmoon.zeta.block.ZetaBushBlock;
import org.violetmoon.zeta.module.ZetaModule;

@Mixin(GlowShroomBlock.class)
public abstract class GlowshroomWithoutDeeplate extends ZetaBushBlock {
    public GlowshroomWithoutDeeplate(String regname, @Nullable ZetaModule module, ResourceKey<CreativeModeTab> tab, Properties properties) {
        super(regname, module, tab, properties);
    }

    @Redirect(method = "mayPlaceOn",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block hi(BlockState instance, BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos){
        return instance.canSustainPlant(world, pos, Direction.UP, this)? Blocks.DEEPSLATE:Blocks.AIR;
    }
}
