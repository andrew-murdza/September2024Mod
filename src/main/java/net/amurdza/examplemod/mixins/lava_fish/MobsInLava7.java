package net.amurdza.examplemod.mixins.lava_fish;

import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(value = SpawnPlacementRegisterEvent.class, remap = false)
public abstract class MobsInLava7 {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$convertExistingWaterPlacementsToLava(
            Map<EntityType<?>, SpawnPlacementRegisterEvent.MergedSpawnPredicate<?>> map,
            CallbackInfo ci
    ) {
        for (Map.Entry<EntityType<?>, SpawnPlacementRegisterEvent.MergedSpawnPredicate<?>> entry : map.entrySet()) {
            EntityType<?> entityType = entry.getKey();
            SpawnPlacementRegisterEvent.MergedSpawnPredicate<?> predicate = entry.getValue();

            if (LavaMobs.isLavaMob(entityType) && predicate.getSpawnType() == SpawnPlacements.Type.IN_WATER) {
                ((MobsInLava8) predicate).aoemod$setSpawnType(SpawnPlacements.Type.IN_LAVA);
            }
        }
    }

    @Inject(
            method = "register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends Entity> void aoemod$replaceWaterPlacementWithLavaPlacement(
            EntityType<T> entityType,
            @Nullable SpawnPlacements.Type placementType,
            @Nullable Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> predicate,
            SpawnPlacementRegisterEvent.Operation operation,
            CallbackInfo ci
    ) {
        if (LavaMobs.isLavaMob(entityType) && placementType == SpawnPlacements.Type.IN_WATER) {
            ci.cancel();

            ((SpawnPlacementRegisterEvent) (Object) this).register(
                    entityType,
                    SpawnPlacements.Type.IN_LAVA,
                    heightmap,
                    predicate,
                    operation
            );
        }
    }
}