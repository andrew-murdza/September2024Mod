package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.mobs.entity.Crab;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobCharacteristics {
    @SubscribeEvent
    public static void CharacteristicChangesEventHandler(EntityJoinLevelEvent event) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Entity entity=event.getEntity();
        Level level=event.getLevel();
        BlockPos pos=entity.getOnPos();

        if(!level.isClientSide&&entity instanceof Mob mob){
            if(mob.getTags().contains("aoe.checkedCharacteristics")){//||mob.getTags().contains("aoe.bred")){
                return;
            }
            mob.addTag("aoe.checkedCharacteristics");

            if(mob instanceof Axolotl){
                ((Axolotl) mob).setVariant(Helper.select(level,Axolotl.Variant.values()));
            }
            else if(mob instanceof Frog){
                ((Frog) mob).setVariant(Helper.select(level,FrogVariant.COLD,FrogVariant.TEMPERATE,FrogVariant.WARM));
            }
//            else if(mob instanceof Rabbit){
//                ((Rabbit)mob).setVariant(Rabbit.Variant.byId(level.random.nextInt(5)));
//            }
//            else if(mob instanceof Fox){
//                Method setFoxType=Fox.class.getDeclaredMethod("setVariant", Fox.Type.class);
//                setFoxType.setAccessible(true);
//                setFoxType.invoke(mob,Helper.select(level,Fox.Type.RED,Fox.Type.SNOW));
//            }
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
            if(mob instanceof MushroomCow){
                Method setMushroomType=MushroomCow.class.getDeclaredMethod("setVariant", MushroomCow.MushroomType.class);
                setMushroomType.setAccessible(true);
                setMushroomType.invoke(mob, Helper.select(level,MushroomCow.MushroomType.RED,MushroomCow.MushroomType.BROWN));
            }
            if(mob instanceof Crab crab){
                Holder<Biome> biome=level.getBiome(pos);
                int variant=0;
                if(biome.is(ModTags.Biomes.tropicalBiomes)){
                    variant=mob.getRandom().nextInt(3);
                }
                else if(biome.is(ModTags.Biomes.mountainBiomes)||biome.is(ModTags.Biomes.mushroomCaves)){
                    variant=mob.getRandom().nextInt(2);
                }
                Field variantField = Crab.class.getDeclaredField("VARIANT");
                variantField.setAccessible(true);
                crab.getEntityData().set((EntityDataAccessor<Integer>) variantField.get(crab), variant);
            }
        }
    }
}
