package net.amurdza.examplemod.mixins.lava_fish;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SpawnPlacementRegisterEvent.MergedSpawnPredicate.class, remap = false)
public interface MobsInLava8 {

    @Accessor("spawnType")
    void aoemod$setSpawnType(SpawnPlacements.Type spawnType);
}