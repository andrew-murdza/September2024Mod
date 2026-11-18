package net.amurdza.examplemod.mixins.othermods.embeddium;

import net.amurdza.examplemod.client.SnowyGrassColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Embeddium owns a second tint cache, including its fast built-in grass path. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.WorldSlice", remap = false)
public abstract class SnowyForestGrassTint implements net.amurdza.examplemod.client.ExactBiomeTintView {
    @Shadow @Final public ClientLevel world;

    @Override public boolean aoemod$exactBiomeTints() { return SnowyGrassColors.unblended(world); }

    // The optional target cannot contribute a refmap entry; cover development and production names.
    @Inject(method = {"getBlockTint", "m_6171_"}, at = @At("HEAD"), cancellable = true)
    private void aoemod$forestGrass(BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> result) {
        if (SnowyGrassColors.unblended(world))
            result.setReturnValue(resolver.getColor(world.getBiome(pos).value(), pos.getX(), pos.getZ()));
    }

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private void aoemod$fastForestGrass(@Coerce Object source, int x, int y, int z, CallbackInfoReturnable<Integer> result) {
        if (!SnowyGrassColors.unblended(world) || !(source instanceof Enum<?> kind)) return;
        ColorResolver resolver = switch (kind.name()) {
            case "GRASS" -> BiomeColors.GRASS_COLOR_RESOLVER;
            case "FOLIAGE" -> BiomeColors.FOLIAGE_COLOR_RESOLVER;
            case "WATER" -> BiomeColors.WATER_COLOR_RESOLVER;
            default -> null;
        };
        if (resolver != null) result.setReturnValue(resolver.getColor(world.getBiome(new BlockPos(x, y, z)).value(), x, z));
    }
}
