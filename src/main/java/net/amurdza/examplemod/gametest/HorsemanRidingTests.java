package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.event_handlers.MobTierBalancing;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HorsemanRidingTests {
    private HorsemanRidingTests() {}

    @GameTest(template = "plant_gameplay_empty")
    public static void horsemenDoNotBuckOffTheirRiders(GameTestHelper helper) {
        for (EntityType<? extends AbstractHorse> type : horsemen()) {
            AbstractHorse mount = type.spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)),
                    MobSpawnType.MOB_SUMMONED);
            helper.assertTrue(mount != null && mount.isVehicle(), "Horseman must spawn with a rider");
            Entity rider = mount.getFirstPassenger();
            try {
                assertDoesNotBuck(helper, mount, rider);
                rider.stopRiding();
                helper.assertFalse(rider.isPassenger(), "Explicit dismounts must remain possible");
            } finally {
                rider.discard();
                mount.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void skeletonHorsemenHavePlainBowsAndNoArmorIncludingOldSaves(GameTestHelper helper) {
        var mount = ModEntities.SKELETON_HORSEMAN.get().spawn(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)), MobSpawnType.MOB_SUMMONED);
        helper.assertTrue(mount != null && mount.getFirstPassenger() instanceof Mob, "Skeleton horseman must have a rider");
        Mob rider = (Mob) mount.getFirstPassenger();
        try {
            assertPlainRider(helper, rider);
            ItemStack legacyBow = new ItemStack(Items.BOW);
            legacyBow.enchant(Enchantments.POWER_ARROWS, 3);
            rider.setItemSlot(EquipmentSlot.MAINHAND, legacyBow);
            rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            rider.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            rider.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            rider.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            rider.setCanPickUpLoot(true);
            MobTierBalancing.onMobJoin(new EntityJoinLevelEvent(rider, helper.getLevel(), true));
            assertPlainRider(helper, rider);
        } finally {
            rider.discard();
            mount.discard();
        }
        helper.succeed();
    }

    private static void assertPlainRider(GameTestHelper helper, Mob rider) {
        helper.assertTrue(rider.getMainHandItem().is(Items.BOW) && !rider.getMainHandItem().isEnchanted(),
                "Skeleton horseman riders must carry an unenchanted bow");
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.MAINHAND) helper.assertTrue(rider.getItemBySlot(slot).isEmpty(),
                    "Skeleton horseman riders must not carry armor or offhand equipment: " + slot);
        }
        helper.assertFalse(rider.canPickUpLoot(), "Riders must not pick up armor or enchanted weapons again");
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyUntamedHorsemenKeepTheirRidersAfterReload(GameTestHelper helper) {
        for (EntityType<? extends AbstractHorse> type : horsemen()) {
            AbstractHorse mount = type.spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)),
                    MobSpawnType.MOB_SUMMONED);
            helper.assertTrue(mount != null && mount.isVehicle(), "Horseman must spawn with a rider");
            Entity rider = mount.getFirstPassenger();
            CompoundTag saved = new CompoundTag();
            helper.assertTrue(mount.save(saved), "Horseman and its passenger must save");
            saved.putBoolean("Tame", false);
            rider.discard();
            mount.discard();

            AbstractHorse restored = (AbstractHorse) EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
            helper.assertTrue(restored != null && restored.isVehicle(), "Reload must preserve the saved rider");
            Entity restoredRider = restored.getFirstPassenger();
            try {
                assertDoesNotBuck(helper, restored, restoredRider);
                helper.assertTrue(restored.saveWithoutId(new CompoundTag()).getBoolean("Tame"),
                        "The repaired tame state must persist in the next save");
            } finally {
                restoredRider.discard();
                restored.discard();
            }
        }
        helper.succeed();
    }

    private static void assertDoesNotBuck(GameTestHelper helper, AbstractHorse mount, Entity rider) {
        RunAroundLikeCrazyGoal bucking = (RunAroundLikeCrazyGoal) mount.goalSelector.getAvailableGoals().stream()
                .map(goal -> goal.getGoal()).filter(RunAroundLikeCrazyGoal.class::isInstance).findFirst().orElseThrow();
        helper.assertFalse(bucking.canUse(), "A horseman's inherited bucking goal must not start");
        mount.getRandom().setSeed(0L);
        // Exercise the actual ejection path repeatedly, rather than checking only the tame flag.
        for (int tick = 0; tick < 512; tick++) {
            bucking.tick();
            helper.assertTrue(rider.getVehicle() == mount, "Horseman rider was ejected at tick " + tick);
        }
    }

    private static List<EntityType<? extends AbstractHorse>> horsemen() {
        return List.of(ModEntities.SKELETON_HORSEMAN.get(), ModEntities.ZOMBIE_HORSEMAN.get());
    }
}
