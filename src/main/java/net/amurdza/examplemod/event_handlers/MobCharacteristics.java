package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobCharacteristics {
    @SubscribeEvent
    public static void CharacteristicChangesEventHandler(EntityJoinLevelEvent event) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Entity entity=event.getEntity();
        Level level=event.getLevel();
        BlockPos pos=entity.getOnPos();
        if(!level.isClientSide&&entity instanceof Mob mob&&Helper.isSpecialBiome(level,pos)){
            if(mob.getTags().contains("aoe.checkedCharacteristics")){//||mob.getTags().contains("aoe.bred")){
                return;
            }
            mob.addTag("aoe.checkedCharacteristics");

            //Handled by another mod
//            if(mob instanceof MushroomCow){
//                Method setMushroomType=MushroomCow.class.getDeclaredMethod("setVariant", MushroomCow.MushroomType.class);
//                setMushroomType.setAccessible(true);
//                setMushroomType.invoke(mob, Helper.select(level,MushroomCow.MushroomType.RED,MushroomCow.MushroomType.BROWN));
//            }
            if(mob instanceof Axolotl){
                ((Axolotl) mob).setVariant(Helper.select(level,Axolotl.Variant.values()));
            }
            else if(mob instanceof Frog){
                ((Frog) mob).setVariant(Helper.select(level,FrogVariant.COLD,FrogVariant.TEMPERATE,FrogVariant.WARM));
            }
            else if(mob instanceof Rabbit){
                ((Rabbit)mob).setVariant(Rabbit.Variant.byId(level.random.nextInt(5)));
            }
            else if(mob instanceof Fox){
                Method setFoxType=Fox.class.getDeclaredMethod("setVariant", Fox.Type.class);
                setFoxType.setAccessible(true);
                setFoxType.invoke(mob,Helper.select(level,Fox.Type.RED,Fox.Type.SNOW));
            }
            else if(mob instanceof Llama){
                Method setStrength=Llama.class.getDeclaredMethod("setStrength",int.class);
                setStrength.setAccessible(true);
                setStrength.invoke(mob,5);
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.LLAMA_HEALTH);
            }
            else if(mob instanceof Horse){
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.HORSE_HEALTH);
                mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.HORSE_SPEED);
                mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(Config.HORSE_JUMP_STRENGTH);
            }
            else if(mob instanceof Donkey|| mob instanceof Mule){
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.DONKEY_HEALTH);
                mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.DONKEY_SPEED);
                mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(Config.DONKEY_JUMP_STRENGTH);
            }
        }
    }
}
