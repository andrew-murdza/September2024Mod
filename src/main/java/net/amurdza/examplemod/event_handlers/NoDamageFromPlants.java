package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class NoDamageFromPlants {
    @SubscribeEvent
    public static void cancelPlantBlockDamage(LivingAttackEvent event){
        DamageSource source=event.getSource();
        if(source.is(DamageTypes.SWEET_BERRY_BUSH)||source.is(DamageTypes.CACTUS)){
            event.setCanceled(true);
        }
    }
}
