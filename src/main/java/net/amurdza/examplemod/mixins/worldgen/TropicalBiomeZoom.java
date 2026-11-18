package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalBiomeZoomState;
import net.amurdza.examplemod.worldgen.TropicalChannelSurface;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.event_handlers.TropicalBiomeSync;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the displayed Y63 biome for every altitude in a tropical X/Z column. */
@Mixin(BiomeManager.class)
public abstract class TropicalBiomeZoom implements TropicalBiomeZoomState {
    @Unique private boolean aoemod$tropicalColumns;
    @Unique private Level aoemod$level;
    @Unique private volatile TropicalLayout aoemod$layout;
    @Unique private volatile Holder<Biome> aoemod$mangrove;
    @Unique private Holder<Biome> aoemod$rainforest;
    @Unique private Holder<Biome> aoemod$cherry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$recognizeTropicalLevel(BiomeManager.NoiseBiomeSource source, long seed, CallbackInfo ci) {
        // Chunk decoration constructs its own manager from a WorldGenRegion;
        // it does not copy the ServerLevel's manager with withDifferentSource.
        Level level = source instanceof Level direct ? direct
                : source instanceof WorldGenRegion region ? region.getLevel() : null;
        aoemod$tropicalColumns = level != null && level.dimension().equals(GameplayDimensions.TROPICAL);
        if (aoemod$tropicalColumns) aoemod$level = level;
    }

    @Override
    public void aoemod$setTropicalColumns(boolean tropical) {
        aoemod$tropicalColumns = tropical;
    }

    @Override
    public void aoemod$setTropicalLayout(TropicalLayout layout, Holder<Biome> mangrove, Holder<Biome> rainforest,
                                       Holder<Biome> cherry) {
        aoemod$cherry = cherry;
        aoemod$rainforest = rainforest;
        aoemod$mangrove = mangrove;
        aoemod$layout = layout;
        aoemod$tropicalColumns = true;
    }

    @Unique
    private TropicalLayout aoemod$layout() {
        if (aoemod$level != null) {
            if (aoemod$mangrove == null) {
                var registry = aoemod$level.registryAccess().registryOrThrow(Registries.BIOME);
                aoemod$rainforest = registry.getHolderOrThrow(TropicalChannelSurface.RAINFOREST);
                aoemod$cherry = registry.getHolderOrThrow(TropicalChannelSurface.CHERRY);
                aoemod$mangrove = registry.getHolderOrThrow(TropicalChannelSurface.MANGROVE);
            }
            if (aoemod$level.isClientSide) return TropicalBiomeSync.clientLayout();
            if (aoemod$layout == null && aoemod$level instanceof ServerLevel server) {
                aoemod$layout = DimensionTerrain.tropicalLayout(server.getChunkSource().randomState().router().temperature());
            }
        }
        return aoemod$layout;
    }

    @Inject(method = "withDifferentSource", at = @At("RETURN"))
    private void aoemod$preserveWorldgenScope(BiomeManager.NoiseBiomeSource source,
                                             CallbackInfoReturnable<BiomeManager> cir) {
        if (!(source instanceof Level) && !(source instanceof WorldGenRegion)) {
            ((TropicalBiomeZoomState) cir.getReturnValue()).aoemod$setTropicalColumns(aoemod$tropicalColumns);
            if (aoemod$tropicalColumns && aoemod$layout() != null) {
                ((TropicalBiomeZoomState) cir.getReturnValue()).aoemod$setTropicalLayout(
                        aoemod$layout(), aoemod$mangrove, aoemod$rainforest, aoemod$cherry);
            }
        }
    }

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void aoemod$useSeaLevelColumn(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (aoemod$tropicalColumns && pos.getY() != TropicalChannelSurface.WATER_SURFACE_Y) {
            cir.setReturnValue(((BiomeManager) (Object) this).getBiome(
                    pos.atY(TropicalChannelSurface.WATER_SURFACE_Y)));
        }
    }

    @Inject(method = "getBiome", at = @At("RETURN"), cancellable = true)
    private void aoemod$followExactLandContour(BlockPos pos, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!aoemod$tropicalColumns || pos.getY() != TropicalChannelSurface.WATER_SURFACE_Y) return;
        var biome = cir.getReturnValue();
        // Keep the reef zoom, but align both land seams with physical terraces.
        // Otherwise quart-biome smoothing leaks cherry grove onto Y74 ground.
        if (!biome.is(TropicalChannelSurface.MANGROVE) && !biome.is(TropicalChannelSurface.RAINFOREST)
                && !biome.is(TropicalChannelSurface.CHERRY)) return;
        TropicalLayout layout = aoemod$layout();
        if (layout != null) {
            cir.setReturnValue(switch (layout.sample(pos.getX(), pos.getZ()).biome()) {
                case 3 -> aoemod$cherry;
                case 2 -> aoemod$rainforest;
                default -> aoemod$mangrove;
            });
        }
    }
}
