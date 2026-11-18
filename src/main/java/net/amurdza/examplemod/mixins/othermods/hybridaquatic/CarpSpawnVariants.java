package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.entity.fish.CarpEntity;
import dev.hybridlabs.aquatic.entity.fish.CarpEntity.Companion.Type;
import dev.hybridlabs.aquatic.entity.fish.CarpEntity.Companion.PatternTextures;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CarpEntity.class)
public abstract class CarpSpawnVariants {
    @Shadow(remap = false)
    private void setPatternTexture(PatternTextures texture) { throw new AssertionError(); }

    // Hybrid Aquatic keeps its custom names, but this vanilla override is SRG-named in the release JAR.
    @Inject(method = {"finalizeSpawn", "m_6518_"}, at = @At("RETURN"), remap = false)
    private void aoemod$brownWildCarp(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                    SpawnGroupData group, CompoundTag data, CallbackInfoReturnable<SpawnGroupData> result) {
        if (DocumentedNaturalSpawns.natural(reason) && (GameplayDimensions.is(level, GameplayDimensions.PLAINS)
                || GameplayDimensions.is(level, GameplayDimensions.SAVANNA))) {
            ((CarpEntity) (Object) this).setVariant(Type.COMMON);
            setPatternTexture(PatternTextures.NONE);
        }
    }
}
