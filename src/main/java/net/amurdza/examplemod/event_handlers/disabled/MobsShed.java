package net.amurdza.examplemod.event_handlers.disabled;


import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobsShed {
//    @SubscribeEvent
//    public void onLivingTick(LivingEvent.LivingTickEvent event) {
//        LivingEntity entity = event.getEntity();
//        if(Helper.isSpecialBiome(entity)& Config.SHED_DATA.containsKey(event.getClass())){
//            List<Config.ShedInfoObject> list=Config.SHED_DATA.get(entity);
//            for(Config.ShedInfoObject info: list){
//                shedItem(entity, info.d,info.item);
//            }
//            if(entity instanceof Chicken chicken){
//                chicken.eggTime=100;
//            }
//            else if(entity instanceof EntityEmu emu){
//                emu.timeUntilNextEgg=100;
//            }
//            else if(entity instanceof EntityGrizzlyBear bear){
//                bear.timeUntilNextFur=100;
//            }
//        }
//    }
//    private void shedItem(LivingEntity entity, double chance, Item item){
//        if (!entity.level().isClientSide&&!entity.isBaby()&&entity.level().getRandom().nextFloat()<chance) {
//            entity.spawnAtLocation(item);
//        }
//    }
}
