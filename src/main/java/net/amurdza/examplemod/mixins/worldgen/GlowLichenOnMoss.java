package net.amurdza.examplemod.mixins.worldgen;

import com.teamabnormals.upgrade_aquatic.core.registry.UABlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(CaveFeatures.class)
public class GlowLichenOnMoss {
    @Redirect(method = "bootstrap",at= @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderSet;direct(Ljava/util/function/Function;[Ljava/lang/Object;)Lnet/minecraft/core/HolderSet$Direct;",ordinal = 0))
    private static <E,T> HolderSet.Direct<T> hi(Function<E, Holder<T>> pHolderFactory, E[] pValues){
        List<E> blocks= new ArrayList<>(List.of(pValues));
        blocks.addAll((List<E>)List.of(Blocks.MOSS_BLOCK, Blocks.FIRE_CORAL_BLOCK,Blocks.TUBE_CORAL_BLOCK,Blocks.HORN_CORAL_BLOCK,
                Blocks.BUBBLE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, UABlocks.ACAN_CORAL_BLOCK,
                UABlocks.BRANCH_CORAL_BLOCK,UABlocks.CHROME_CORAL_BLOCK,UABlocks.STAR_CORAL_BLOCK,
                UABlocks.PETAL_CORAL_BLOCK,UABlocks.ROCK_CORAL_BLOCK,UABlocks.PILLOW_CORAL_BLOCK,
                UABlocks.SILK_CORAL_BLOCK, UABlocks.MOSS_CORAL_BLOCK));
        return HolderSet.direct(pHolderFactory,blocks);
    }
}
