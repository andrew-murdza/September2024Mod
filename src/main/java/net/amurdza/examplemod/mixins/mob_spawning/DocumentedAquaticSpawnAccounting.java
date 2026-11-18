package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.SpawnState.class)
public final class DocumentedAquaticSpawnAccounting {
    @Redirect(method = "afterSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"))
    private MobCategory aoemod$aquaticCounter(EntityType<?> type, Mob mob, ChunkAccess chunk) {
        return DocumentedFauna.spawnCategory(mob, type.getCategory());
    }
}
