package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.event_handlers.MobAI;
import net.amurdza.examplemod.event_handlers.TwinsAndFailedBreeding;
import net.amurdza.examplemod.mixins.accessor.TargetGoalTypeAccessor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProductionMobMappingTests {
    private static final String TEMPLATE = "plant_gameplay_empty";
    private ProductionMobMappingTests() { }

    @GameTest(template = TEMPLATE)
    public static void turtleTargetsAreRemovedWithoutRemovingPlayerTargets(GameTestHelper helper) {
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON}) {
            Monster mob = (Monster) type.create(helper.getLevel());
            var turtles = new NearestAttackableTargetGoal<>(mob, Turtle.class, true);
            var players = new NearestAttackableTargetGoal<>(mob, Player.class, true);
            mob.targetSelector.addGoal(1, turtles);
            mob.targetSelector.addGoal(2, players);
            helper.assertTrue(((TargetGoalTypeAccessor) turtles).aoemod$getTargetType() == Turtle.class,
                    "The mapped goal accessor must resolve the actual target type");
            MobAI.AIChanges(new EntityJoinLevelEvent(mob, helper.getLevel()));
            helper.assertFalse(mob.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() == turtles),
                    "Hostiles must no longer select turtles as their attack target");
            helper.assertTrue(mob.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() == players),
                    "Removing turtle targeting must preserve the separate player-target goal");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaOffspringUseBothParentsAndMutationVariants(GameTestHelper helper) {
        var oldRabbit = MobConfig.MOB_INFO_BY_MOB.remove(EntityType.RABBIT);
        var oldMooshroom = MobConfig.MOB_INFO_BY_MOB.remove(EntityType.MOOSHROOM);
        try {
            Rabbit first = EntityType.RABBIT.create(helper.getLevel());
            Rabbit second = EntityType.RABBIT.create(helper.getLevel());
            first.setVariant(Rabbit.Variant.WHITE);
            second.setVariant(Rabbit.Variant.BLACK);
            int[] branches = new int[3];
            for (int seed = 0; seed < 100; seed++) {
                Rabbit child = EntityType.RABBIT.create(helper.getLevel());
                child.setVariant(Rabbit.Variant.BROWN);
                first.getRandom().setSeed(seed);
                int roll = RandomSource.create(seed).nextInt(100);
                int branch = roll < 45 ? 0 : roll < 90 ? 1 : 2;
                Rabbit.Variant expected = branch == 0 ? Rabbit.Variant.WHITE : branch == 1 ? Rabbit.Variant.BLACK : Rabbit.Variant.BROWN;
                TwinsAndFailedBreeding.twinsOrInfertile(new BabyEntitySpawnEvent(first, second, child));
                helper.assertTrue(child.getVariant() == expected && child.getTags().contains("aoe.parentVariant"),
                        "Mapped variant handling must preserve both 45% parent choices and the 10% mutation branch");
                branches[branch]++;
            }
            helper.assertTrue(branches[0] > 0 && branches[1] > 0 && branches[2] > 0,
                    "Variant regression must exercise both parents and mutations");
            MushroomCow firstCow = EntityType.MOOSHROOM.create(helper.getLevel());
            MushroomCow secondCow = EntityType.MOOSHROOM.create(helper.getLevel());
            firstCow.setVariant(MushroomCow.MushroomType.BROWN);
            secondCow.setVariant(MushroomCow.MushroomType.BROWN);
            MushroomCow childCow = EntityType.MOOSHROOM.create(helper.getLevel());
            childCow.setVariant(MushroomCow.MushroomType.RED);
            firstCow.getRandom().setSeed(1); // Parent branch, not mutation.
            TwinsAndFailedBreeding.twinsOrInfertile(new BabyEntitySpawnEvent(firstCow, secondCow, childCow));
            helper.assertTrue(childCow.getVariant() == MushroomCow.MushroomType.BROWN && childCow.getTags().contains("aoe.parentVariant"),
                    "Mooshrooms need mapped variant calls even though they do not implement VariantHolder");
        } finally {
            if (oldRabbit != null) MobConfig.MOB_INFO_BY_MOB.put(EntityType.RABBIT, oldRabbit);
            if (oldMooshroom != null) MobConfig.MOB_INFO_BY_MOB.put(EntityType.MOOSHROOM, oldMooshroom);
        }
        helper.succeed();
    }
}
