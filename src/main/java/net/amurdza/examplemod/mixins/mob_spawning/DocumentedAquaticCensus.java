package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.worldgen.AquaticSpawnCensus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Count the custom dimensions' small and large aquatic budgets without changing global entity types. */
@Mixin(NaturalSpawner.class)
public final class DocumentedAquaticCensus {
    @Redirect(method = "createState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getClassification(Z)Lnet/minecraft/world/entity/MobCategory;", remap = false))
    private static MobCategory aoemod$aquaticCensus(Entity entity, boolean forSpawnCount) {
        return AquaticSpawnCensus.category(entity, entity.getClassification(forSpawnCount));
    }
}
