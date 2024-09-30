package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BreedingEvent {
    @SubscribeEvent
    public static void twinsOrInfertile(BabyEntitySpawnEvent event){
        AgeableMob parentA= (AgeableMob) event.getParentA();
        AgeableMob parentB= (AgeableMob) event.getParentB();
        ServerLevel world= (ServerLevel) parentA.level();
        BlockPos pos=parentA.getOnPos();
        if(event.getChild()!=null&&Helper.isSpecialBiome(world,pos)){
            String mobName=event.getChild().getEncodeId();
            if(mobName!=null){
                int index=Config.TWIN_MOBS.indexOf(mobName);
                int index1=Config.PARTIALLY_INFERTILE_MOBS.indexOf(mobName);
                if(index1<0||!Helper.withChance(world,Config.INFERTILE_CHANCES.get(index1))){
                    if(index>=0&&Helper.withChance(world,Config.TWIN_CHANCES.get(index))){
                        event.getChild().addTag("aoe.bred");
                        AgeableMob secondChild= parentA.getBreedOffspring(world, parentB);
                        secondChild.addTag("aoe.bred");
                        secondChild.moveTo(parentA.getX(),parentA.getY(),parentA.getZ(),0.0F,0.0F);
                        secondChild.setBaby(true);
                        world.addFreshEntity(secondChild);
                    }
                }
                else{
                    event.setCanceled(true);
                }
            }
        }
    }
}
