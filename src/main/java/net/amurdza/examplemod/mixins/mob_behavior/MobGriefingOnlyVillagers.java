package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ForgeEventFactory.class, remap = false)
public class MobGriefingOnlyVillagers {
    @Inject(method = "getMobGriefingEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aoemod$onlyVillagersMayMobGrief(Level level, Entity entity,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Mob && !(entity instanceof Villager)) {
            cir.setReturnValue(false);
        }
    }
}
