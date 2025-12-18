package net.amurdza.examplemod.mixins.lava_and_water;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.Objects;

@Mixin(BucketItem.class)
public class LavaNotPlaceableInTropical {
    @Redirect(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private boolean hi(DimensionType instance, @Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult, @Nullable ItemStack container){
        return september2024Mod$shouldPrevent(pLevel,pPos,container);
    }
    @Redirect(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/Fluid;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(Fluid instance, TagKey<Fluid> pTag, @Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult, @Nullable ItemStack container){
        return september2024Mod$shouldPrevent(pLevel,pPos,container);
    }
    @Unique
    private boolean september2024Mod$shouldPrevent(Level pLevel, BlockPos pPos, @Nullable ItemStack container){
        if (pLevel.getBiome(pPos).is(ModTags.Biomes.tropicalBiomes) && Objects.requireNonNull(container).is(Items.LAVA_BUCKET))
            return true;
        if (!pLevel.getBiome(pPos).is(ModTags.Biomes.netherBiomes)) return false;
        assert container != null;
        return container.is(Items.WATER_BUCKET);
    }
}
