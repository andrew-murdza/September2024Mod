package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.mcreator.biodiverse.entity.CassowaryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CassowaryFamilyTests {
    @SuppressWarnings("unchecked")
    private static EntityType<CassowaryEntity> type() {
        return (EntityType<CassowaryEntity>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("biodiverse:cassowary"));
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void placedEggHatchesThroughScheduledTicksInsteadOfRandomTicks(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var egg = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biodiverse:cassowary_egg"));
        helper.assertTrue(egg != null && egg != Blocks.AIR, "The native cassowary egg must remain registered");
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, egg.defaultBlockState(), 3);
        var blockEntity = level.getBlockEntity(pos);
        helper.assertTrue(blockEntity != null && blockEntity.getPersistentData().getDouble("age") == 60,
                "Placing an egg must initialize its sixty-step incubation countdown");
        helper.assertFalse(level.getBlockState(pos).isRandomlyTicking(),
                "Increasing randomTickSpeed must not accelerate scheduled egg incubation");
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(pos, egg),
                "Placement must schedule the egg's first incubation tick");
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(2);
        var before = level.getEntitiesOfClass(CassowaryEntity.class, bounds).stream()
                .map(CassowaryEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        // Call the same block tick used every 200 game ticks without making QA wait ten minutes.
        for (int i = 0; i < 60; i++) {
            level.getBlockState(pos).tick(level, pos, level.random);
        }
        helper.assertTrue(level.getBlockState(pos).is(egg) && blockEntity.getPersistentData().getDouble("age") == 0,
                "The egg must finish all sixty countdown steps before hatching on the next scheduled tick");
        level.getBlockState(pos).tick(level, pos, level.random);
        var hatchlings = level.getEntitiesOfClass(CassowaryEntity.class, bounds,
                bird -> !before.contains(bird.getUUID()));
        try {
            helper.assertTrue(level.getBlockState(pos).isAir() && hatchlings.size() == 1,
                    "The sixty-first scheduled tick must replace the egg with one cassowary");
            helper.assertTrue(hatchlings.get(0).isBaby() && hatchlings.get(0).getTexture().equals("cassowarybaby"),
                    "The hatchling must immediately be a baby with the baby appearance");
        } finally {
            hatchlings.forEach(CassowaryEntity::discard);
            level.removeBlock(pos, false);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void clickingAdultWithNativeSpawnEggCreatesOneBaby(GameTestHelper helper) {
        var level = helper.getLevel();
        var adult = type().create(level);
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        var egg = ForgeSpawnEggItem.fromEntityType(type());
        helper.assertTrue(egg != null, "Cassowary must have its native spawn egg");
        var stack = new ItemStack(egg, 2);
        var position = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2));
        adult.moveTo(position.x, position.y, position.z, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var before = level.getEntitiesOfClass(CassowaryEntity.class, adult.getBoundingBox().inflate(2))
                .stream().map(CassowaryEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        var result = adult.interact(player, InteractionHand.MAIN_HAND);
        var children = level.getEntitiesOfClass(CassowaryEntity.class, adult.getBoundingBox().inflate(2),
                child -> child.isBaby() && !before.contains(child.getUUID()));
        helper.assertTrue(result.consumesAction() && children.size() == 1,
                "Clicking the adult with its native spawn egg must create exactly one baby");
        var baby = children.get(0);
        helper.assertTrue(stack.getCount() == 1 && baby.getTexture().equals("cassowarybaby"),
                "Use exactly one egg and immediately select the baby appearance");
        baby.discard();
        adult.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void allRequestedFruitsFeedCassowariesAndTemptThem(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var bird = type().create(level);
        bird.moveTo(8.5, 100, 8.5, 0, 0);
        var player = FakePlayerFactory.getMinecraft(level);
        player.getAbilities().instabuild = false;
        for (String name : new String[]{"minecraft:apple", "fruitsdelight:orange", "fruitsdelight:pear",
                "fruitsdelight:cranberry", "hearthandharvest:cherry", "alexsmobs:banana", "aoemod:plum"}) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
            helper.assertTrue(item != null && item != Items.AIR && bird.isFood(new ItemStack(item)),
                    "Requested fruit must exist and be accepted: " + name);
            bird.setAge(0);
            bird.resetLove();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
            helper.assertTrue(bird.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                            && bird.isInLove() && player.getMainHandItem().getCount() == 1,
                    "Every requested fruit must actually feed an adult: " + name);
        }
        helper.assertTrue(bird.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof TemptGoal
                        && g.getPriority() < 5), "Fruit temptation must outrank the native resting goal");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 2));
        bird.setBaby(true);
        int previousAge = bird.getAge();
        helper.assertTrue(bird.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction()
                        && bird.getAge() > previousAge && player.getMainHandItem().getCount() == 1,
                "Feeding an apple must actually consume fruit and grow the baby");
        bird.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void babyFindsItsAdultAndNavigatesTowardsIt(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 3; z++) {
            level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 0; y <= 3; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        var adult = type().create(level);
        var baby = type().create(level);
        adult.setAge(0);
        baby.setBaby(true);
        adult.moveTo(origin.getX() + 7.5, origin.getY(), origin.getZ() + 1.5, 0, 0);
        baby.moveTo(origin.getX() + .5, origin.getY(), origin.getZ() + 1.5, 0, 0);
        // These newly created entities have not had a physics tick yet. The
        // fixture already provides a solid floor; ground navigation requires
        // that contact state before it can calculate its first path.
        adult.setOnGround(true);
        baby.setOnGround(true);
        level.addFreshEntity(adult);
        level.addFreshEntity(baby);
        try {
            var goal = baby.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal())
                    .filter(g -> g instanceof FollowParentGoal).map(g -> (FollowParentGoal) g).findFirst().orElseThrow();
            helper.assertTrue(goal.getFlags().contains(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE),
                    "Parent following must interrupt the lower-priority native rest/stroll goals");
            helper.assertTrue(goal.canUse(), "A baby seven blocks away must find its adult");
            goal.start();
            goal.tick();
            helper.assertTrue(baby.getNavigation().getPath() != null && goal.canContinueToUse(),
                    "The family goal must create a real path to its adult");
            goal.stop();
        } finally {
            baby.discard();
            adult.discard();
        }
        helper.succeed();
    }
}
