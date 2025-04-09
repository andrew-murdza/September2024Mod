package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
import com.github.alexthe666.alexsmobs.entity.EntityOrca;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class WaterMammalsDontNeedAir {
    @SubscribeEvent
    public void hi(LivingBreatheEvent event){
        LivingEntity entity=event.getEntity();
        if(entity instanceof Dolphin||entity instanceof EntityCachalotWhale||entity instanceof EntityOrca){
            event.setCanBreathe(false);
        }
    }
}
