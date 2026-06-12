package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobStatsConfig;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.MushroomCow;
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
            else if(mob instanceof Llama){
                Method setStrength=Llama.class.getDeclaredMethod("setStrength",int.class);
                setStrength.setAccessible(true);
                setStrength.invoke(mob,5);
                Integer hp = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_LLAMA_HEALTH);
                if(hp!= null){
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                }

                Float speed = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_LLAMA_SPEED);
                if(speed!= null){
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                }
            }
            else if(mob instanceof Horse){
                Integer value = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_HORSE_HEALTH);
                if(value!= null){
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(value);
                }

                Float speed = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_HORSE_SPEED);
                if(speed != null){
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                }

                Float jumpStrength = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_HORSE_JUMP_STRENGTH);
                if(jumpStrength != null){
                    mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpStrength);
                }
            }
            else if(mob instanceof Donkey|| mob instanceof Mule){
                Integer value = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_DONKEY_HEALTH);
                if(value!= null){
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(value);
                }

                Float speed = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_DONKEY_SPEED);
                if(speed != null){
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                }

                Float jumpStrength = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_DONKEY_JUMP_STRENGTH);
                if(jumpStrength != null){
                    mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpStrength);
                }
            }
            if(mob instanceof MushroomCow){
                Method setMushroomType=MushroomCow.class.getDeclaredMethod("setVariant", MushroomCow.MushroomType.class);
                setMushroomType.setAccessible(true);
                setMushroomType.invoke(mob, Helper.select(level,MushroomCow.MushroomType.RED,MushroomCow.MushroomType.BROWN));
            }
        }
    }
}
