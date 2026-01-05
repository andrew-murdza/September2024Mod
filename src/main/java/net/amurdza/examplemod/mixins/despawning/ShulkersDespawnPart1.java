package net.amurdza.examplemod.mixins.despawning;

import net.minecraft.world.entity.animal.AbstractGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractGolem.class)
public class ShulkersDespawnPart1 {
    @Overwrite
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }
}
