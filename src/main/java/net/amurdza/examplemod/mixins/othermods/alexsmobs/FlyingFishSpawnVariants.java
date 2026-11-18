package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityFlyingFish;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFlyingFish.class)
public final class FlyingFishSpawnVariants {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"), remap = false)
    private void aoemod$blueSnowyFish(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                    SpawnGroupData group, CompoundTag data, CallbackInfoReturnable<SpawnGroupData> result) {
        if (DocumentedNaturalSpawns.natural(reason) && GameplayDimensions.is(level, GameplayDimensions.SNOWY)) {
            // Texture 0 is the all-blue palette; 1 has orange and 2 has yellow fins.
            ((EntityFlyingFish) (Object) this).setVariant(0);
        }
    }
}
