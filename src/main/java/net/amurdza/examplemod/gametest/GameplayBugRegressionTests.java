package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.entity.ai.*;
import net.amurdza.examplemod.event_handlers.BreedingItems;
import net.amurdza.examplemod.event_handlers.MobTierBalancing;
import net.amurdza.examplemod.event_handlers.MobAI;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.event_handlers.ItemsPlaceModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import java.util.ArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.server.level.ServerLevel;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GameplayBugRegressionTests {
    private static final String TEMPLATE = "plant_gameplay_empty";
    private GameplayBugRegressionTests() { }

    @GameTest(template = TEMPLATE)
    public static void waterDepthCountsSourcesAndWaterloggedBlocks(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(1, 1, 1);
        helper.setBlock(bottom, Blocks.WATER);
        helper.setBlock(bottom.above(), Blocks.WATER);
        helper.setBlock(bottom.above(2), Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        helper.setBlock(bottom.above(3), Blocks.AIR);
        BlockPos absolute = helper.absolutePos(bottom);
        helper.assertTrue(WorldGenUtils.getTotalWaterAbove(absolute, helper.getLevel()) == 3,
                "Source water and waterlogged surfaces must count toward water depth");
        helper.assertTrue(WorldGenUtils.getTotalWaterAbove(absolute, helper.getLevel(), 2) == 2,
                "Water-depth scans must respect their limit");
        helper.setBlock(bottom.above(), Blocks.STONE);
        helper.assertTrue(WorldGenUtils.getTotalWaterAbove(absolute, helper.getLevel()) == 1,
                "Solid non-water gaps must end the water column");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tamedPolarBearMayDefendAgainstHostileMobs(GameTestHelper helper) {
        PolarBear bear = EntityType.POLAR_BEAR.create(helper.getLevel());
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        bear.setTarget(zombie);
        helper.assertTrue(bear.getTarget() == null, "Untamed passive bear should still obey the global targeting filter");
        ((PolarBearTamingAccess) bear).aoe$setOwnerUUID(helper.makeMockSurvivalPlayer().getUUID());
        bear.setTarget(zombie);
        helper.assertTrue(bear.getTarget() == zombie, "Tamed bear must be allowed to target an owner's hostile attacker");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void returnedGhastFireballUsesItsDamageMultiplier(GameTestHelper helper) {
        var ghast = EntityType.GHAST.create(helper.getLevel());
        var fireball = EntityType.FIREBALL.create(helper.getLevel());
        Player player = helper.makeMockSurvivalPlayer();
        fireball.setOwner(player);
        LivingHurtEvent event = new LivingHurtEvent(ghast, helper.getLevel().damageSources().fireball(fireball, player), 10.0F);
        MobTierBalancing.onLivingHurt(event);
        helper.assertTrue(Math.abs(event.getAmount() - 3.0F) < 0.001F,
                "Returned fireball damage must be adjusted before the direct-melee filter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void alternateFoodsRespectFertilityAndCreativeInventory(GameTestHelper helper) {
        var old = MobConfig.MOB_INFO_BY_MOB.get(EntityType.RABBIT);
        try {
            ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
            Rabbit rabbit = EntityType.RABBIT.create(plains);
            rabbit.moveTo(0, 250, 0);
            rabbit.setAge(0);
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 3));
            MobConfig.MOB_INFO_BY_MOB.put(EntityType.RABBIT, rates(1, 0));
            var denied = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, rabbit);
            BreedingItems.extraOrCancelBreeding(denied);
            helper.assertTrue(denied.isCanceled() && denied.getCancellationResult() == InteractionResult.FAIL,
                    "Extra breeding food must reject an infertile adult");
            helper.assertTrue(!rabbit.isInLove() && player.getMainHandItem().getCount() == 3,
                    "Rejected food must neither consume inventory nor set love");
            MobConfig.MOB_INFO_BY_MOB.put(EntityType.RABBIT, rates(1, 1));
            var accepted = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, rabbit);
            BreedingItems.extraOrCancelBreeding(accepted);
            helper.assertTrue(rabbit.isInLove() && player.getMainHandItem().getCount() == 2,
                    "Valid survival feeding must consume exactly one food");
            rabbit.resetLove();
            player.getAbilities().instabuild = true;
            BreedingItems.extraOrCancelBreeding(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, rabbit));
            helper.assertTrue(rabbit.isInLove() && player.getMainHandItem().getCount() == 2,
                    "Creative feeding must keep its food");
            rabbit.resetLove();
            rabbit.setAge(-100);
            MobConfig.MOB_INFO_BY_MOB.put(EntityType.RABBIT, rates(0, 1));
            BreedingItems.extraOrCancelBreeding(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, rabbit));
            helper.assertTrue(rabbit.getAge() == -100 && player.getMainHandItem().getCount() == 2,
                    "Growth-disabled babies must not consume food or change age");
        } finally {
            restore(EntityType.RABBIT, old);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unconfiguredAnimalsRetainVanillaGrowth(GameTestHelper helper) {
        var old = MobConfig.MOB_INFO_BY_MOB.remove(EntityType.RABBIT);
        try {
            Rabbit rabbit = EntityType.RABBIT.create(helper.getLevel());
            rabbit.setAge(-100);
            rabbit.aiStep();
            helper.assertTrue(rabbit.getAge() == -99, "Unconfigured species must age naturally");
            rabbit.ageUp(1, false);
            helper.assertTrue(rabbit.getAge() == -79, "Unconfigured food growth must use a positive vanilla increment");
        } finally {
            restore(EntityType.RABBIT, old);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void horsemanRidersReceiveTheirTaggedStatsOnFirstJoin(GameTestHelper helper) {
        for (EntityType<?> type : new EntityType<?>[]{ModEntities.ZOMBIE_HORSEMAN.get(), ModEntities.SKELETON_HORSEMAN.get()}) {
            Mob mount = (Mob) type.spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), MobSpawnType.MOB_SUMMONED);
            helper.assertTrue(mount != null && mount.getFirstPassenger() instanceof Mob, "Horseman must spawn its rider");
            Mob rider = (Mob) mount.getFirstPassenger();
            double expected = type == ModEntities.ZOMBIE_HORSEMAN.get() ? 22 : 24;
            helper.assertTrue(rider.getAttributeValue(Attributes.MAX_HEALTH) == expected,
                    "Rider-specific stats must be applied on first join rather than ordinary zombie/skeleton stats");
            rider.discard();
            mount.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dolphinsUseConfiguredOffspringCount(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos origin = new BlockPos(10, 250, 10);
        boolean alreadyForced = plains.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(0, 0));
        plains.setChunkForced(0, 0, true);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(plains.isPositionEntityTicking(origin),
                "Dolphin fixture must be entity-ticking before mate lookup"))
                .thenExecute(() -> verifyDolphinOffspring(helper, plains, alreadyForced)).thenSucceed();
    }

    private static void verifyDolphinOffspring(GameTestHelper helper, ServerLevel plains, boolean alreadyForced) {
        var old = MobConfig.MOB_INFO_BY_MOB.get(EntityType.DOLPHIN);
        Dolphin first = EntityType.DOLPHIN.create(plains);
        Dolphin second = EntityType.DOLPHIN.create(plains);
        first.moveTo(10, 250, 10);
        second.moveTo(11, 250, 10);
        plains.addFreshEntity(first);
        plains.addFreshEntity(second);
        AABB bounds = first.getBoundingBox().inflate(4);
        try {
            MobConfig.MOB_INFO_BY_MOB.put(EntityType.DOLPHIN, rates(1, 2));
            var firstBreeding = (DolphinBreedingAccess) first;
            var secondBreeding = (DolphinBreedingAccess) second;
            firstBreeding.aoe$setAge(0);
            secondBreeding.aoe$setAge(0);
            firstBreeding.aoe$setInLove(null);
            secondBreeding.aoe$setInLove(null);
            DolphinBreedGoal goal = new DolphinBreedGoal(first, 1);
            helper.assertTrue(goal.canUse(), "Ready dolphins must find their mate");
            goal.start();
            for (int i = 0; i < 60; i++) goal.tick();
            var babies = plains.getEntitiesOfClass(Dolphin.class, bounds, dolphin -> ((DolphinBreedingAccess) dolphin).aoe$isBaby());
            helper.assertTrue(babies.size() == 2, "Fertility multiplier two must produce two dolphin offspring");
            babies.forEach(Entity::discard);
            MobConfig.MOB_INFO_BY_MOB.put(EntityType.DOLPHIN, rates(1, 0));
            firstBreeding.aoe$setAge(0);
            secondBreeding.aoe$setAge(0);
            firstBreeding.aoe$setInLove(null);
            secondBreeding.aoe$setInLove(null);
            goal.start();
            for (int i = 0; i < 60; i++) goal.tick();
            helper.assertTrue(plains.getEntitiesOfClass(Dolphin.class, bounds,
                    dolphin -> ((DolphinBreedingAccess) dolphin).aoe$isBaby()).isEmpty(), "Zero fertility must produce no dolphin baby");
        } finally {
            first.discard();
            second.discard();
            restore(EntityType.DOLPHIN, old);
            if (!alreadyForced) plains.setChunkForced(0, 0, false);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void boneShearsRecipeResolvesAndDeadSoupIsAbsent(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation("aoemod", "tools/bone_shears"));
        helper.assertTrue(recipe.isPresent() && recipe.get().getType() == RecipeType.CRAFTING,
                "Bone shears must load as a valid crafting recipe");
        helper.assertTrue(recipe.get().getResultItem(helper.getLevel().registryAccess()).is(ModItems.BONE_SHEARS.get()),
                "Bone shears recipe must produce registered bone shears");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation("aoemod", "food/glow_berry_soup")).isEmpty(),
                "Invalid unregistered soup recipe must not be loaded");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void growthBufferRejectsWholeShapesBeforeWriting(GameTestHelper helper) {
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = first.above();
        helper.getLevel().setBlock(first, Blocks.AIR.defaultBlockState(), 3);
        helper.getLevel().setBlock(second, Blocks.AIR.defaultBlockState(), 3);
        GrowthPlacementBuffer rejected = new GrowthPlacementBuffer(helper.getLevel());
        rejected.setBlock(first, Blocks.OAK_LOG.defaultBlockState(), 3);
        rejected.setBlock(second, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        helper.assertTrue(rejected.getBlockState(first).is(Blocks.OAK_LOG), "Feature reads must see their proposed trunk");
        helper.assertFalse(rejected.isValid((pos, state) -> !pos.equals(second)), "One forbidden leaf must reject the whole shape");
        helper.assertTrue(helper.getLevel().isEmptyBlock(first) && helper.getLevel().isEmptyBlock(second),
                "Rejected growth must leave neither trunk nor foliage behind");
        GrowthPlacementBuffer accepted = new GrowthPlacementBuffer(helper.getLevel());
        accepted.setBlock(first, Blocks.OAK_LOG.defaultBlockState(), 3);
        helper.assertTrue(accepted.isValid((pos, state) -> true), "Permitted growth should validate");
        accepted.commit();
        helper.assertTrue(helper.getLevel().getBlockState(first).is(Blocks.OAK_LOG), "Accepted growth must commit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void configuredTreesCommitCompletelyOrLeaveNoTrunk(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos origin = new BlockPos(24, 250, 24);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, 0, -5), origin.offset(5, 20, 5))) {
            plains.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        plains.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), 2);
        var features = plains.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        boolean spruce = features.getHolderOrThrow(TreeFeatures.SPRUCE).value().place(plains,
                plains.getChunkSource().getGenerator(), RandomSource.create(7), origin);
        helper.assertFalse(spruce, "Spruce must reject the plains dimension before committing any blocks");
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, 0, -5), origin.offset(5, 20, 5))) {
            helper.assertTrue(plains.isEmptyBlock(pos), "Rejected tree left a block at " + pos);
        }
        boolean birch = features.getHolderOrThrow(TreeFeatures.BIRCH).value().place(plains,
                plains.getChunkSource().getGenerator(), RandomSource.create(7), origin);
        helper.assertTrue(birch && plains.getBlockState(origin).is(Blocks.BIRCH_LOG),
                "An allowed birch must still generate through the buffered feature wrapper");
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -1, -5), origin.offset(5, 20, 5))) {
            plains.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void specializedAnimalGoalRemovalsRunAfterCommonRules(GameTestHelper helper) {
        for (EntityType<? extends Animal> type : List.of(EntityType.FOX, EntityType.WOLF, EntityType.BEE)) {
            Animal animal = type.create(helper.getLevel());
            MobAI.AIChanges(new EntityJoinLevelEvent(animal, helper.getLevel()));
            if (animal instanceof Bee) {
                helper.assertTrue(animal.targetSelector.getAvailableGoals().isEmpty(), "Bee attack targets must be removed");
            } else {
                for (var wrapped : animal.goalSelector.getAvailableGoals()) {
                    String name = wrapped.getGoal().getClass().getSimpleName();
                    helper.assertFalse(List.of("FoxEatBerriesGoal", "FoxPounceGoal", "FoxPanicGoal", "FoxSearchForItemsGoal",
                                    "PerchAndSearchGoal", "StalkPreyGoal", "WolfPanicGoal").contains(name),
                            "Specialized passive-animal removal was skipped for " + name);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacementLootPreservesSpecialDropsAndHonorsLooting(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var bossType = ModEntities.ILLAGER_LORD.get();
        var oldBoss = MobConfig.MOB_INFO_BY_MOB.get(bossType);
        var oldZombie = MobConfig.MOB_INFO_BY_MOB.get(EntityType.ZOMBIE);
        try {
            var boss = bossType.create(plains);
            boss.moveTo(0, 250, 0);
            MobConfig.MOB_INFO_BY_MOB.put(bossType, new MobConfig.MobInfo(null, null, List.of(dropRate(false))));
            var original = new ArrayList<ItemEntity>();
            for (Item item : List.of(ModItems.BOSS_SHIELD.get(), Items.WITHER_SKELETON_SKULL, Items.TOTEM_OF_UNDYING)) {
                original.add(new ItemEntity(plains, 0, 250, 0, new ItemStack(item)));
            }
            var special = new LivingDropsEvent(boss, plains.damageSources().generic(), original, 0, true);
            CustomMobDrops.addConfiguredDrops(special);
            for (Item item : List.of(ModItems.BOSS_SHIELD.get(), Items.WITHER_SKELETON_SKULL, Items.TOTEM_OF_UNDYING)) {
                helper.assertTrue(special.getDrops().stream().anyMatch(drop -> drop.getItem().is(item)),
                        "Replacement loot discarded a special drop: " + item);
            }
            var zombie = EntityType.ZOMBIE.create(plains);
            zombie.moveTo(0, 250, 0);
            for (boolean eligible : new boolean[]{false, true}) {
                MobConfig.MOB_INFO_BY_MOB.put(EntityType.ZOMBIE, new MobConfig.MobInfo(null, null, List.of(dropRate(eligible))));
                zombie.getRandom().setSeed(17);
                int total = 0;
                for (int i = 0; i < 16; i++) {
                    var event = new LivingDropsEvent(zombie, plains.damageSources().generic(), new ArrayList<>(), 3, true);
                    CustomMobDrops.addConfiguredDrops(event);
                    int count = event.getDrops().stream().filter(drop -> drop.getItem().is(Items.BONE))
                            .mapToInt(drop -> drop.getItem().getCount()).sum();
                    helper.assertTrue(count >= 2 && count <= (eligible ? 5 : 2), "Looting bonus exceeded its configured bounds");
                    total += count;
                }
                helper.assertTrue(eligible ? total > 32 : total == 32,
                        "Looting must affect only eligible entries, with a bonus for eligible drops");
            }
        } finally {
            restore(bossType, oldBoss);
            restore(EntityType.ZOMBIE, oldZombie);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedTallPlantDoesNotConsumeItsItem(GameTestHelper helper) {
        BlockPos base = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), helper.getLevel().getMaxBuildHeight() - 2,
                helper.absolutePos(BlockPos.ZERO).getZ());
        var oldBase = helper.getLevel().getBlockState(base);
        var oldPlant = helper.getLevel().getBlockState(base.above());
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SUNFLOWER, 2));
        try {
            helper.getLevel().setBlock(base, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            helper.getLevel().setBlock(base.above(), Blocks.AIR.defaultBlockState(), 3);
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, base,
                    new BlockHitResult(Vec3.atCenterOf(base), Direction.UP, base, false));
            ItemsPlaceModBlocks.sunflowerPlace1(event);
            helper.assertTrue(event.isCanceled() && event.getCancellationResult() == InteractionResult.FAIL,
                    "Tall flowers must reject placements with an upper half beyond build height");
            helper.assertTrue(player.getMainHandItem().getCount() == 2 && helper.getLevel().isEmptyBlock(base.above()),
                    "Rejected tall flower placement must keep the item and leave no lower half");
        } finally {
            helper.getLevel().setBlock(base.above(), oldPlant, 3);
            helper.getLevel().setBlock(base, oldBase, 3);
        }
        helper.succeed();
    }

    private static MobConfig.MobBiomeMultipliers dropRate(boolean looting) {
        return new MobConfig.MobBiomeMultipliers(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, Items.BONE, null, looting);
    }

    private static MobConfig.MobInfo rates(float growth, float fertility) {
        return new MobConfig.MobInfo(multiplier(growth), multiplier(fertility), List.of());
    }
    private static MobConfig.MobBiomeMultipliers multiplier(float value) {
        return new MobConfig.MobBiomeMultipliers(value, value, value, value, value, value, value, value, value, value, null, null);
    }
    private static void restore(EntityType<?> type, MobConfig.MobInfo old) {
        if (old == null) MobConfig.MOB_INFO_BY_MOB.remove(type); else MobConfig.MOB_INFO_BY_MOB.put(type, old);
    }
}
