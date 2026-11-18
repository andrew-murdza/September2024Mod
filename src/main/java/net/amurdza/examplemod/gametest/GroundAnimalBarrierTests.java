package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GroundAnimalBarrierTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void bisonAndElephantsWalkThroughOpenGatesAndStayInsideClosedPens(GameTestHelper helper) {
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos fixture = helper.absolutePos(BlockPos.ZERO).atY(200);
        // A broad lane keeps each animal fully supported and prevents clipping around the barrier ends.
        for (BlockPos pos : BlockPos.betweenClosed(fixture.offset(-6, -1, -4), fixture.offset(6, 5, 4))) {
            var expected = pos.getY() < fixture.getY() ? Blocks.DIRT : Blocks.AIR;
            savanna.setBlock(pos, expected.defaultBlockState(), 18);
            helper.assertTrue(savanna.getBlockState(pos).is(expected), "Savanna rejected the fixture at " + pos);
        }
        var failures = new java.util.ArrayList<String>();
        for (String id : new String[]{"alexsmobs:bison", "alexsmobs:elephant"}) {
            Animal animal = (Animal) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(savanna);
            helper.assertTrue(animal != null, "Missing ground animal " + id);
            animal.setMaxUpStep(2.5F);
            helper.assertTrue(animal.getStepHeight() == 1,
                    "The effective Forge getter must cap steps across vanilla, Alex's Caves, and portal collision");
            animal.setNoAi(true);
            for (BlockState boundary : new BlockState[]{Blocks.DIRT.defaultBlockState(),
                    Blocks.ACACIA_FENCE.defaultBlockState(), Blocks.ACACIA_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST),
                    Blocks.ACACIA_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST).setValue(FenceGateBlock.OPEN, true)}) {
                for (int z = -4; z <= 4; z++) {
                    savanna.setBlock(fixture.offset(0, 0, z), boundary, 18);
                    savanna.setBlock(fixture.offset(0, 1, z), Blocks.AIR.defaultBlockState(), 18);
                    helper.assertTrue(savanna.getBlockState(fixture.offset(0, 0, z)).equals(boundary),
                            "Savanna rejected the boundary " + boundary);
                }
                boolean open = boundary.is(Blocks.ACACIA_FENCE_GATE) && boundary.getValue(FenceGateBlock.OPEN);
                advance(animal, fixture);
                boolean passed = animal.getX() > fixture.getX() + 2;
                if (passed != (open || boundary.is(Blocks.DIRT))) failures.add(
                        id + " traversed the wrong boundary: " + boundary + ", position=" + animal.position() + ", box=" + animal.getBoundingBox());
            }
            for (int z = -4; z <= 4; z++) for (int y = 0; y < 2; y++) {
                savanna.setBlock(fixture.offset(0, y, z), Blocks.DIRT.defaultBlockState(), 18);
                helper.assertTrue(savanna.getBlockState(fixture.offset(0, y, z)).is(Blocks.DIRT), "Missing wall block");
            }
            advance(animal, fixture);
            if (animal.getBoundingBox().maxX > fixture.getX() + .01
                    || Math.abs(animal.getY() - fixture.getY()) >= .01) failures.add(
                    id + " crossed the two-block wall, position=" + animal.position());
            animal.discard();
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rabbitJumpJustClearsOneBlock(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var feet = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int y = 0; y <= 3; y++) level.setBlock(feet.above(y), Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(feet.below(), Blocks.STONE.defaultBlockState(), 18);
        var rabbit = net.minecraft.world.entity.EntityType.RABBIT.create(level);
        rabbit.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5, 0, 0);
        rabbit.setOnGround(true);
        var jump = net.minecraft.world.entity.animal.Rabbit.class.getDeclaredMethod("jumpFromGround");
        jump.setAccessible(true);
        jump.invoke(rabbit);
        double peak = 0;
        for (int step = 0; step < 10; step++) {
            rabbit.travel(Vec3.ZERO);
            peak = Math.max(peak, rabbit.getY() - feet.getY());
        }
        rabbit.discard();
        helper.assertTrue(peak > 1 && peak < 1.15, "Rabbit should just clear a full block; actual rise was " + peak);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void coalAndCharcoalGiveCampfiresSignalSmoke(GameTestHelper helper) {
        var level = helper.getLevel();
        var fire = helper.absolutePos(new BlockPos(2, 2, 2));
        var charcoal = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("quark", "charcoal_block"));
        level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 3);
        for (var fuel : new net.minecraft.world.level.block.Block[]{Blocks.HAY_BLOCK, Blocks.DIRT, Blocks.COAL_BLOCK, Blocks.DIRT, charcoal}) {
            level.setBlock(fire.below(), fuel.defaultBlockState(), 3);
            helper.assertTrue(level.getBlockState(fire).getValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE) == (fuel != Blocks.DIRT),
                    "Campfire must update its smoke after changing its fuel to " + fuel);
        }
        helper.succeed();
    }

    private static void advance(Animal animal, BlockPos fixture) {
        animal.setPos(fixture.getX() - 4, fixture.getY(), fixture.getZ() + .5);
        animal.setOnGround(true);
        for (int step = 0; step < 32; step++) {
            animal.move(MoverType.SELF, new Vec3(.25, -.08, 0));
        }
    }
}
