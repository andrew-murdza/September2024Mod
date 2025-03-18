package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.material.MapColor;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(BWGWood.class)
public abstract class FloweringLeavesDontGrow {
    @Shadow
    private static Supplier<LeavesBlock> registerLeaves(String key, MapColor mapColor) {
        return null;
    }
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/potionstudios/biomeswevegone/world/level/block/wood/BWGWood;registerLeaves(Ljava/lang/String;Lnet/minecraft/world/level/material/MapColor;Ljava/util/function/Supplier;F)Ljava/util/function/Supplier;",ordinal = 0))
    private static Supplier<LeavesBlock> hi(String key, MapColor mapColor, Supplier<LeavesBlock> ripeLeaves, float chance){
        return registerLeaves("flowering_baobab", MapColor.COLOR_GREEN);
    }
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/potionstudios/biomeswevegone/world/level/block/wood/BWGWood;registerLeaves(Ljava/lang/String;Lnet/minecraft/world/level/material/MapColor;Ljava/util/function/Supplier;F)Ljava/util/function/Supplier;",ordinal = 1))
    private static Supplier<LeavesBlock> hi1(String key, MapColor mapColor, Supplier<LeavesBlock> ripeLeaves, float chance){
        return registerLeaves("flowering_orchard", MapColor.COLOR_GREEN);
    }
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/potionstudios/biomeswevegone/world/level/block/wood/BWGWood;registerLeaves(Ljava/lang/String;Lnet/minecraft/world/level/material/MapColor;Ljava/util/function/Supplier;F)Ljava/util/function/Supplier;",ordinal = 2))
    private static Supplier<LeavesBlock> hi2(String key, MapColor mapColor, Supplier<LeavesBlock> ripeLeaves, float chance){
        return registerLeaves("flowering_yucca", MapColor.COLOR_GREEN);
    }
}
