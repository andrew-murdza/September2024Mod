package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomMobDrops {
    @SubscribeEvent
    public static void addConfiguredDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();

        List<MobConfig.MobBiomeMultipliers> dropEntries = MobConfig.mobDropEntries(entity);

        if (dropEntries.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        for (MobConfig.MobBiomeMultipliers entry : dropEntries) {
            Item item = getDropItem(entity, entry);
            float multiplier = MobConfig.mobAmount(entity, entry);

            dropItem(event, entity, item, multiplier);
        }
    }

    private static Item getDropItem(LivingEntity entity, MobConfig.MobBiomeMultipliers entry) {
        if (entity instanceof Sheep sheep && entry.hasItem(Items.WHITE_WOOL)) {
            return Helper.getWool(sheep);
        }

        return entry.selectedItem(entity);
    }

    private static void dropItem(
            LivingDropsEvent event,
            LivingEntity entity,
            Item item,
            float multiplier
    ) {
        if (item == null || multiplier <= 0.0F) {
            return;
        }

        int amount = Helper.computeIncrements(entity.getRandom(), multiplier);

        if (amount <= 0) {
            return;
        }

        ItemStack stack = new ItemStack(item, amount);

        ItemEntity itemEntity = new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                stack
        );

        event.getDrops().add(itemEntity);
    }
}