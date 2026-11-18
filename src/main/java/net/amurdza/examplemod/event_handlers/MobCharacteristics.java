package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobStatsConfig;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FrogVariant;
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

import net.amurdza.examplemod.mixins.accessor.LlamaStrengthInvoker;
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class MobCharacteristics {
    private static final String PARENT_VARIANT_TAG = "aoe.parentVariant";
    private static final String HORSE_HEALTH_TAG = "aoe.checkedHorseHealthV2";

    @SubscribeEvent
    public static void CharacteristicChangesEventHandler(EntityJoinLevelEvent event) {
        Entity entity=event.getEntity();
        Level level=event.getLevel();

        if(!level.isClientSide&&entity instanceof Mob mob){
            if (mob instanceof Horse horse) {
                initializeHorseHealthAndTemper(horse);
            }
            if(mob.getTags().contains("aoe.checkedCharacteristics")){//||mob.getTags().contains("aoe.bred")){
                return;
            }
            mob.addTag("aoe.checkedCharacteristics");
            boolean hasParentVariant = mob.getTags().contains(PARENT_VARIANT_TAG);

            if(mob instanceof Axolotl && !hasParentVariant){
                ((Axolotl) mob).setVariant(Helper.select(level,Axolotl.Variant.values()));
            }
            else if(mob instanceof Frog && !hasParentVariant){
                ((Frog) mob).setVariant(Helper.select(level,FrogVariant.COLD,FrogVariant.TEMPERATE,FrogVariant.WARM));
            }
            else if(mob instanceof Llama){
                ((LlamaStrengthInvoker) mob).aoe$setStrength(5);
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
                Float speed = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_HORSE_SPEED);
                if(speed != null){
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                }

                Float jumpStrength = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_HORSE_JUMP_STRENGTH);
                if(jumpStrength != null){
                    mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpStrength);
                }
            }
            else if(mob instanceof Mule){
                Integer value = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_MULE_HEALTH);
                if(value!= null){
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(value);
                }

                Float speed = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_MULE_SPEED);
                if(speed != null){
                    mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
                }

                Float jumpStrength = Helper.getBiomeValue(mob.level(),mob.blockPosition(),MobStatsConfig.BIOME_TO_MULE_JUMP_STRENGTH);
                if(jumpStrength != null){
                    mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpStrength);
                }
            }
            else if(mob instanceof Donkey){
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
        }
    }

    private static void initializeHorseHealthAndTemper(Horse horse) {
        if (GameplayDimensions.is(horse.level(), GameplayDimensions.SAVANNA)
                || horse.level().getBiome(horse.blockPosition()).is(ModTags.Biomes.savannaBiomes)) {
            // Match a wild horse fed up to its temper cap; the player still has to mount and tame it.
            horse.setTemper(horse.getMaxTemper());
        }
        if (horse.getTags().contains(HORSE_HEALTH_TAG)) {
            return;
        }
        Integer health = configuredHorseHealth(horse);
        if (health == null) {
            return;
        }

        // The old handler raised only the maximum, leaving newly spawned horses with empty hearts.
        // A separate marker also repairs previously processed horses, without healing saved damage
        // or repeatedly changing their health when they reload or travel to another dimension.
        float previousMax = horse.getMaxHealth();
        float previousHealth = horse.getHealth();
        float missingHealth = Math.max(0.0F, previousMax - previousHealth);
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        float newMax = horse.getMaxHealth();
        // A lower regional cap preserves the injured fraction instead of killing a living horse.
        float adjustedHealth = newMax < previousMax ? previousHealth * newMax / previousMax : newMax - missingHealth;
        horse.setHealth(previousHealth > 0.0F ? adjustedHealth : 0.0F);
        horse.addTag(HORSE_HEALTH_TAG);
    }

    private static Integer configuredHorseHealth(Horse horse) {
        if (horse.getTags().contains("aoe.checkedCharacteristics")) {
            // Legacy horses retain their original speed and jump after travelling. Use those
            // regional traits so migration does not replace a moved horse's native health.
            double speed = horse.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            double jump = horse.getAttribute(Attributes.JUMP_STRENGTH).getBaseValue();
            for (var entry : MobStatsConfig.BIOME_TO_HORSE_HEALTH.entrySet()) {
                Float configuredSpeed = MobStatsConfig.BIOME_TO_HORSE_SPEED.get(entry.getKey());
                Float configuredJump = MobStatsConfig.BIOME_TO_HORSE_JUMP_STRENGTH.get(entry.getKey());
                if (configuredSpeed != null && configuredJump != null
                        && Math.abs(speed - configuredSpeed) < 0.000001D
                        && Math.abs(jump - configuredJump) < 0.000001D) {
                    return entry.getValue();
                }
            }
            double existingHealth = horse.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            for (int configuredHealth : MobStatsConfig.BIOME_TO_HORSE_HEALTH.values()) {
                if (existingHealth == configuredHealth) return configuredHealth;
            }
        }
        return Helper.getBiomeValue(horse.level(), horse.blockPosition(), MobStatsConfig.BIOME_TO_HORSE_HEALTH);
    }
}
