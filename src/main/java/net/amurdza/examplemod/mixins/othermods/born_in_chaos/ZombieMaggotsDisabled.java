package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.procedures.ZombieMaggotsProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ZombieMaggotsProcedure.class, remap = false)
public abstract class ZombieMaggotsDisabled {
    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void aoemod$disableBornInChaosZombieMaggots(
            Event event,
            LevelAccessor level,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
