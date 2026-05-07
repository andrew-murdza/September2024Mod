package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class NoTropicalFire {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();

        if ((event.getPlacedBlock().is(Blocks.FIRE) || event.getPlacedBlock().is(Blocks.SOUL_FIRE))
                && level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity().level().getBiome(event.getEntity().blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            event.setCanceled(true);
            event.getEntity().clearFire();
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.level().getBiome(entity.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            return;
        }

        if (event.getSource().is(DamageTypes.ON_FIRE)
                || event.getSource().is(DamageTypes.IN_FIRE)
                || event.getSource().is(DamageTypes.LAVA)
                || event.getSource().is(DamageTypes.HOT_FLOOR)
                || event.getSource().is(DamageTypes.LIGHTNING_BOLT)) {
            event.setCanceled(true);
            entity.clearFire();
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().getBiome(entity.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            entity.clearFire();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().getBiome(entity.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            entity.clearFire();
        }
    }
}
