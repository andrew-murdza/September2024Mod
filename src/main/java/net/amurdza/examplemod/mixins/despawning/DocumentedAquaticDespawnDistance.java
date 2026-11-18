package net.amurdza.examplemod.mixins.despawning;

import net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Match the natural spawn range while retaining vanilla ownership and persistence checks. */
@Mixin(Mob.class)
public final class DocumentedAquaticDespawnDistance {
    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"))
    private MobCategory aoemod$aquaticDespawnRange(EntityType<?> type) {
        return AquaticSpawnDistanceRules.category(((Mob) (Object) this).level(), type);
    }
}
