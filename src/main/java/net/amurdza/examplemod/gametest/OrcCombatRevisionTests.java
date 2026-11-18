package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.event_handlers.OrcCombatRules;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrcCombatRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void selectedOrcsKeepNativeCombatWithoutMoraleOrFleeing(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var nativeEffects = ForgeRegistries.MOB_EFFECTS.getEntries().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals("orcz")).map(entry -> entry.getValue()).toList();
        helper.assertFalse(nativeEffects.isEmpty(), "The native Orcz psychology effects must be present for this regression");
        for (String id : List.of("orcz:orcsavagemace", "orcz:orcshield")) {
            Mob orc = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(level);
            var target = EntityType.COW.create(level);
            try {
                Set<?> nativeTargets = Set.copyOf(orc.targetSelector.getAvailableGoals());
                var nativeMelee = orc.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal())
                        .filter(MeleeAttackGoal.class::isInstance).findFirst().orElseThrow();
                // Simulate loaded state from the source mod before our join repair.
                orc.getActiveEffectsMap().put(nativeEffects.get(0), new MobEffectInstance(nativeEffects.get(0), 500));
                orc.getPersistentData().putBoolean("hitrun_flee", true);
                orc.getPersistentData().putBoolean("block_ready", true);
                OrcCombatRules.joined(new EntityJoinLevelEvent(orc, level, true));
                helper.assertTrue(orc.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() == nativeMelee)
                                && Set.copyOf(orc.targetSelector.getAvailableGoals()).equals(nativeTargets),
                        "Native melee and target selection must survive the psychology removal: " + id);
                helper.assertTrue(orc.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof AvoidEntityGoal),
                        "Selected orcs must not flee due to source-mod morale, health, or spores");
                helper.assertTrue(orc.getActiveEffects().isEmpty() && !orc.getPersistentData().contains("hitrun_flee")
                                && !orc.getPersistentData().contains("block_ready"), "Saved psychology state must be cleared");
                for (var effect : nativeEffects) {
                    helper.assertFalse(orc.addEffect(new MobEffectInstance(effect, 500)), "Orc psychology must not be reapplied: " + effect);
                }
                helper.assertTrue(orc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200)),
                        "Ordinary potion effects must remain usable");
                orc.setTarget(target);
                orc.setHealth(2);
                var before = orc.getPersistentData().copy();
                Class.forName("com.duke.orcz.procedures.OrcsTickingProcedure")
                        .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                        .invoke(null, level, orc.getX(), orc.getY(), orc.getZ(), orc);
                for (String procedure : List.of("OrcshurtsProcedure", "EnragemoraleProcedure", "MoralegoblinsProcedure")) {
                    Class.forName("com.duke.orcz.procedures." + procedure)
                            .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class, Entity.class)
                            .invoke(null, level, orc.getX(), orc.getY(), orc.getZ(), orc, target);
                }
                helper.assertTrue(before.equals(orc.getPersistentData()) && orc.getActiveEffects().size() == 1
                                && orc.hasEffect(MobEffects.MOVEMENT_SPEED),
                        "Native tick/hurt/death procedures must not restore psychology or its buffs/debuffs");
            } finally { target.discard(); orc.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void selectedOrcsReplaceNativeEquipmentLootWithSavannaBonesAndLeather(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        for (String id : List.of("orcz:orcsavagemace", "orcz:orcshield")) {
            Mob orc = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(level);
            try {
                for (int looting : new int[]{0, 3}) {
                    var drops = new ArrayList<ItemEntity>();
                    drops.add(new ItemEntity(level, 0, 0, 0, new ItemStack(Items.IRON_INGOT, 4)));
                    drops.add(new ItemEntity(level, 0, 0, 0, new ItemStack(Items.IRON_AXE)));
                    var event = new LivingDropsEvent(orc, level.damageSources().generic(), drops, looting, true);
                    CustomMobDrops.addConfiguredDrops(event);
                    var items = event.getDrops().stream().collect(Collectors.toMap(drop -> drop.getItem().getItem(), drop -> drop.getItem().getCount()));
                    helper.assertTrue(items.equals(java.util.Map.of(Items.BONE, 2, Items.LEATHER, 1)),
                            "Selected orcs must replace native metals/equipment with only bone and leather: " + id);
                    for (var item : items.keySet()) helper.assertTrue(
                            DimensionMaterialAvailability.isAvailable(level, item, BiomePlacementRestrictions.SAVANNA),
                            "Each selected orc drop must be allowed in Savanna: " + item);
                    event.getDrops().forEach(Entity::discard);
                }
            } finally { orc.discard(); }
        }
        helper.succeed();
    }
}
