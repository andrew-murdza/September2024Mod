package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomMobDrops {
    @SubscribeEvent
    public static void addConfiguredDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();

        DropInfo dropInfo = MobConfig.dropInfo(entity);

        if(dropInfo == null){
            return;
        }

        event.setCanceled(true);

        dropItem(
                event,
                entity,
                getMeatItem(entity, dropInfo),
                MobConfig.mobMeatAmount(entity)
        );

        dropItem(
                event,
                entity,
                entity instanceof Sheep sheep ? Helper.getWool(sheep) : dropInfo.skinItem(),
                MobConfig.mobSkinAmount(entity)
        );

        dropItem(
                event,
                entity,
                dropInfo.boneItem(),
                MobConfig.mobBoneAmount(entity)
        );
    }

    private static Item getMeatItem(LivingEntity entity, DropInfo dropInfo) {
        if (entity.isOnFire() && dropInfo.cookedMeatItem() != null) {
            return dropInfo.cookedMeatItem();
        }

        return dropInfo.meatItem();
    }

    private static void dropItem(
            LivingDropsEvent event,
            LivingEntity entity,
            Item item,
            float multiplier
    ) {
        if (item == null) {
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