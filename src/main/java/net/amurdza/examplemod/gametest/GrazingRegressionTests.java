package net.amurdza.examplemod.gametest;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.entity.EntityRhinoceros;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GrazingRegressionTests {
    private GrazingRegressionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void bisonRegrowsFurWithoutConsumingGrass(GameTestHelper helper) {
        var bison = AMEntityRegistry.BISON.get().create(helper.getLevel());
        BlockPos feet = prepare(helper, bison);
        DoublePlantBlock.placeAt(helper.getLevel(), Blocks.TALL_GRASS.defaultBlockState(), feet, 3);
        BlockState lower = helper.getLevel().getBlockState(feet);
        BlockState upper = helper.getLevel().getBlockState(feet.above());
        bison.setSheared(true);
        for (int bite = 0; bite < 5; bite++) {
            bite(bison, EntityBison.ANIMATION_EAT, 30);
            assertGrass(helper, feet);
            helper.assertTrue(helper.getLevel().getBlockState(feet).equals(lower)
                            && helper.getLevel().getBlockState(feet.above()).equals(upper),
                    "Bison grazing must preserve both tall-grass halves");
        }
        helper.assertFalse(bison.isSheared(), "Five actual grazing bites must still regrow bison fur");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void otherAlexsMobsGrazersStillHealWithoutConsumingGrass(GameTestHelper helper) {
        var kangaroo = AMEntityRegistry.KANGAROO.get().create(helper.getLevel());
        BlockPos feet = prepare(helper, kangaroo);
        kangaroo.setHealth(1);
        bite(kangaroo, EntityKangaroo.ANIMATION_EAT_GRASS, 20);
        helper.assertTrue(kangaroo.getHealth() > 1, "Kangaroo grazing must still heal");
        assertGrass(helper, feet);

        var rhino = AMEntityRegistry.RHINOCEROS.get().create(helper.getLevel());
        feet = prepare(helper, rhino);
        rhino.setHealth(1);
        // Rhinos advance their animation before testing the tick-30 bite;
        // the other grazers advance it at the end of their tick.
        bite(rhino, EntityRhinoceros.ANIMATION_EAT_GRASS, 29);
        helper.assertTrue(rhino.getHealth() > 1, "Rhinoceros grazing must still heal");
        assertGrass(helper, feet);

        var tusklin = AMEntityRegistry.TUSKLIN.get().create(helper.getLevel());
        feet = prepare(helper, tusklin);
        tusklin.setHealth(1);
        tusklin.getRandom().setSeed(12345L);
        // Tusklins only feed on one third of rutting bites; exercise their real random branch.
        for (int attempt = 0; attempt < 40 && tusklin.getHealth() == 1; attempt++) {
            bite(tusklin, EntityTusklin.ANIMATION_RUT, 23);
            assertGrass(helper, feet);
        }
        helper.assertTrue(tusklin.getHealth() > 1, "Tusklin rutting must still provide its feeding benefit");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sheepKeepsPlantsAndSoilWhileRegrowingWool(GameTestHelper helper) {
        var sheep = EntityType.SHEEP.create(helper.getLevel());
        BlockPos feet = prepare(helper, sheep);
        for (BlockState plant : new BlockState[]{Blocks.GRASS.defaultBlockState(), Blocks.AIR.defaultBlockState()}) {
            helper.getLevel().setBlockAndUpdate(feet, plant);
            sheep.setSheared(true);
            var goal = new EatBlockGoal(sheep);
            goal.start();
            helper.assertTrue(goal.getEatAnimationTick() > 0, "Sheep must still start its eating animation");
            for (int tick = 0; tick < 40; tick++) goal.tick();
            helper.assertFalse(sheep.isSheared(), "Sheep must still regrow wool after eating");
            helper.assertTrue(helper.getLevel().getBlockState(feet).equals(plant),
                    "Sheep grazing must leave its grass plant unchanged");
            assertGrass(helper, feet);
        }
        helper.succeed();
    }

    private static BlockPos prepare(GameTestHelper helper, Mob mob) {
        helper.assertTrue(mob != null, "The pinned grazing mob must be registered");
        BlockPos feet = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(feet.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5, 0, 0);
        return feet;
    }

    private static <T extends Mob & IAnimatedEntity> void bite(T mob, Animation animation, int tick) {
        mob.setAnimation(animation);
        mob.setAnimationTick(tick);
        mob.tick();
        if (mob.getAnimation() != animation || mob.getAnimationTick() <= tick) {
            throw new IllegalStateException("Grazing must retain and advance the original animation");
        }
    }

    private static void assertGrass(GameTestHelper helper, BlockPos feet) {
        helper.assertTrue(helper.getLevel().getBlockState(feet.below()).is(Blocks.GRASS_BLOCK),
                "Eating must not replace the grass block with dirt");
    }
}
