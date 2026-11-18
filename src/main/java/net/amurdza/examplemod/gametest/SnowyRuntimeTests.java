package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.EndFishEntity;
import net.amurdza.examplemod.portal.PortalTravel;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyPlantSnowlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.snow.block.entity.SnowBlockEntity;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyRuntimeTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void inventoryBucketEntitiesLoadTheirVariants(GameTestHelper helper) {
        // BucketLib creates every bucketable entity when opening creative inventory.
        // Exercise that constructor and the nested enum used by its synchronized data.
        String expectedSnapshot = System.getProperty("aoemod.runtimeSnapshotExpected");
        if (expectedSnapshot != null && !expectedSnapshot.isBlank()) {
            for (Class<?> type : new Class<?>[]{EndFishEntity.class, EndFishEntity.Variant.class, PortalTravel.class}) {
                assertRuntimeSnapshot(helper, type, expectedSnapshot);
            }
        }
        var fish = ModEntities.END_FISH.get().create(helper.getLevel());
        helper.assertTrue(fish != null, "Inventory must be able to construct an End Fish");
        for (int id : new int[]{0, 1, 2, 5, 6, 7}) {
            fish.setVariant(EndFishEntity.Variant.byId(id));
            var bucket = fish.getBucketItemStack();
            helper.assertTrue(bucket.is(ModItems.BUCKET_END_FISH.get())
                            && bucket.getOrCreateTag().getInt("Variant") == id,
                    "Inventory fish bucket lost variant " + id);
        }
        helper.succeed();
    }

    private static void assertRuntimeSnapshot(GameTestHelper helper, Class<?> type, String expectedSnapshot) {
        String expected = Path.of(expectedSnapshot).toAbsolutePath().normalize().toString()
                .replace('\\', '/').toLowerCase(Locale.ROOT);
        var source = type.getProtectionDomain().getCodeSource();
        helper.assertTrue(source != null, "Runtime class has no code source: " + type.getName());
        try {
            // Forge uses a union: URL for transformed module classes. Its URI path
            // retains the real root while decoding any spaces in the project path.
            String actual = source.getLocation().toURI().getPath().replace('\\', '/').toLowerCase(Locale.ROOT);
            helper.assertTrue(actual.contains(expected + "/") || actual.endsWith(expected),
                    type.getName() + " loaded outside the isolated runtime: " + actual);
            AOEMod.LOGGER.info("Runtime snapshot verified for {}: {}", type.getName(), source.getLocation());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid runtime class source for " + type.getName(), exception);
        }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowloggedBerriesKeepSpeciesAgeAndSnowAfterHarvest(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        for (var berry : new net.minecraft.world.level.block.Block[]{Blocks.SWEET_BERRY_BUSH, ModBlocks.BLUE_BERRY_BUSH.get()}) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            var mature = berry.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3);
            helper.assertTrue(SnowyPlantSnowlogging.place(level, pos, mature), "Berry must generate snowlogged");
            helper.assertTrue(level.getBlockEntity(pos) instanceof SnowBlockEntity, "Missing snow container");
            var snow = (SnowBlockEntity) level.getBlockEntity(pos);
            CompoundTag saved = snow.saveWithFullMetadata();
            var loaded = new SnowBlockEntity(pos, level.getBlockState(pos));
            loaded.load(saved);
            helper.assertTrue(loaded.getContainedState() == mature, "Snow save/load lost berry species or maturity");
            var player = FakePlayerFactory.getMinecraft(level);
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            helper.assertTrue(level.getBlockEntity(pos) instanceof SnowBlockEntity, "Harvest must preserve snowlogging");
            var picked = ((SnowBlockEntity) level.getBlockEntity(pos)).getContainedState();
            helper.assertTrue(picked.is(berry) && picked.getValue(SweetBerryBushBlock.AGE) == 1,
                    "Harvest must preserve species and reset berry growth");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void attackingSnowloggedTaigaPlantsUsesBeetrootSeedsAndKeepsSnow(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos pos = taigaTestPosition(level);
        var below = level.getBlockState(pos.below());
        var previous = level.getBlockState(pos);
        var above = level.getBlockState(pos.above());
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AABB dropsBox = new AABB(pos).inflate(2);
        try {
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (Block plant : new Block[]{Blocks.GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN}) {
                int seeds = 0;
                for (int trial = 0; trial < 512; trial++) {
                    prepareSnowloggedPlant(helper, level, pos, plant);
                    var snow = level.getBlockState(pos);
                    snow.getBlock().attack(snow, level, pos, player);
                    helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW), "Attacking the plant must leave its snow layer");
                    if (plant instanceof DoublePlantBlock) {
                        helper.assertTrue(level.getBlockState(pos.above()).isAir(), "Removing a snowlogged lower half must remove its upper half");
                    }
                    for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, dropsBox)) {
                        helper.assertTrue(drop.getItem().is(Items.BEETROOT_SEEDS), "Snowlogged taiga plant dropped a non-beetroot item: " + drop.getItem());
                        seeds += drop.getItem().getCount();
                        drop.discard();
                    }
                }
                helper.assertTrue(seeds > 0, "Repeated attacks must actually produce beetroot seeds for " + plant);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            prepareSnowloggedPlant(helper, level, pos, Blocks.FERN);
            var snow = level.getBlockState(pos);
            snow.getBlock().attack(snow, level, pos, player);
            var sheared = level.getEntitiesOfClass(ItemEntity.class, dropsBox);
            helper.assertTrue(sheared.size() == 1 && sheared.get(0).getItem().is(Items.FERN),
                    "Shears must retain the native fern item drop instead of rolling seeds");
            helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW), "Shearing must also retain snow");
        } finally {
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, dropsBox)) drop.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.setBlock(pos.above(), above, 2);
            level.setBlock(pos, previous, 2);
            level.setBlock(pos.below(), below, 2);
        }
        helper.succeed();
    }

    private static void prepareSnowloggedPlant(GameTestHelper helper, ServerLevel level, BlockPos pos, Block plant) {
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        var state = plant.defaultBlockState();
        if (plant instanceof DoublePlantBlock) DoublePlantBlock.placeAt(level, state, pos, 2);
        helper.assertTrue(SnowyPlantSnowlogging.place(level, pos, state), "Test plant must be inside a native snow container");
    }

    private static BlockPos taigaTestPosition(ServerLevel level) {
        var ridges = level.getChunkSource().randomState().router().ridges();
        for (int i = 0; i < 256; i++) {
            var pos = new BlockPos(64000 + i * 128, 308, 64000);
            if (Math.abs(ridges.compute(new DensityFunction.SinglePointContext(pos.getX(), 0, pos.getZ()))) > 0.4
                    && GameplayDimensions.biomeIs(level, pos, "snowy_forest")) return pos;
        }
        throw new IllegalStateException("Cannot find a snowy taiga fixture");
    }
}
