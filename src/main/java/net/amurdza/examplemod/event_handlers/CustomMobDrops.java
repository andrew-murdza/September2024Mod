package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
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

        /*
         * LivingDropsEvent already contains the vanilla drops here.
         * Keep only vanilla drops that should survive the replacement,
         * then clear everything else.
         */
        List<ItemEntity> preservedVanillaDrops = new ArrayList<>();

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();

            if (shouldPreserveVanillaDrop(stack)) {
                preservedVanillaDrops.add(drop);
            }
        }

        event.getDrops().clear();
        event.getDrops().addAll(preservedVanillaDrops);

        /*
         * Add your custom configured drops.
         */
        for (MobConfig.MobBiomeMultipliers entry : dropEntries) {
            Item item = getDropItem(entity, entry);
            float multiplier = MobConfig.mobAmount(entity, entry);

            dropItem(event, entity, item, multiplier);
        }

        /*
         * Replace armor equipment drops with resource compensation.
         */
        dropArmorReplacementItems(event, entity);
    }

    private static boolean shouldPreserveVanillaDrop(ItemStack stack) {
        Item item = stack.getItem();

        return stack.is(ItemTags.MUSIC_DISCS)
                || item == Items.SKELETON_SKULL
                || item == Items.ZOMBIE_HEAD
                || item == Items.CREEPER_HEAD
                || item == Items.PIGLIN_HEAD
                || item == Items.PLAYER_HEAD
                || item == Items.DRAGON_HEAD
                || item == Items.DRAGON_EGG;
    }

    private static Item getDropItem(LivingEntity entity, MobConfig.MobBiomeMultipliers entry) {
        if (entity instanceof Sheep sheep && entry.hasItem(Items.WHITE_WOOL)) {
            return Helper.getWool(sheep);
        }

        return entry.selectedItem(entity);
    }

    private static void dropArmorReplacementItems(LivingDropsEvent event, LivingEntity entity) {
        Level level = event.getEntity().level();
        BlockPos pos = event.getEntity().blockPosition();
        Float value = Helper.getBiomeValue(level,pos,MobConfig.BIOME_TO_NUGGETS_FROM_ARMOR);
        float count = value != null ? value : 0;
        int ingotCount = Mth.floor(count/9);
        int nuggetCount = (int) count % 9;
        for (ItemStack armorStack : entity.getArmorSlots()) {
            if (armorStack.isEmpty()) {
                continue;
            }

            if (!(armorStack.getItem() instanceof ArmorItem armorItem)) {
                continue;
            }

            if (armorItem.getMaterial() == ArmorMaterials.LEATHER) {
                if (entity.getRandom().nextFloat() < 0.10F) {
                    dropSingleItem(event, entity, Items.LEATHER);
                }
            } else if (
                    armorItem.getMaterial() == ArmorMaterials.IRON
                            || armorItem.getMaterial() == ArmorMaterials.CHAIN
            ) {
                dropItem(event, entity, Items.IRON_NUGGET, nuggetCount);
                dropItem(event, entity, Items.IRON_INGOT, ingotCount);
            } else if (armorItem.getMaterial() == ArmorMaterials.GOLD) {
                dropItem(event, entity, Items.GOLD_NUGGET, nuggetCount);
                dropItem(event, entity, Items.GOLD_INGOT, ingotCount);
            } else if (armorItem.getMaterial() == ArmorMaterials.DIAMOND) {
                dropItem(event, entity, Items.DIAMOND,
                        Helper.computeIncrements(event.getEntity().getRandom(),(float)nuggetCount / 9 + ingotCount));
            } else if (armorItem.getMaterial() == ArmorMaterials.NETHERITE) {
                dropItem(event, entity, Items.NETHERITE_INGOT,
                        Helper.computeIncrements(event.getEntity().getRandom(),(float)nuggetCount / 9 + ingotCount));
            }
        }
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

        dropStack(event, entity, new ItemStack(item, amount));
    }

    private static void dropSingleItem(
            LivingDropsEvent event,
            LivingEntity entity,
            Item item
    ) {
        dropStack(event, entity, new ItemStack(item, 1));
    }

    private static void dropStack(
            LivingDropsEvent event,
            LivingEntity entity,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

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