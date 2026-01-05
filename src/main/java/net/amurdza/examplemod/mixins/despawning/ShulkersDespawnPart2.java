package net.amurdza.examplemod.mixins.despawning;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Shulker.class)
public class ShulkersDespawnPart2 extends ShulkersDespawnPart1 {
    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return true;
    }
}
