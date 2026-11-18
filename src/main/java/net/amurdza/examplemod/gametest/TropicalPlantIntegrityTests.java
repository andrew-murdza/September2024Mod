package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.CaveVinePlacement;
import net.amurdza.examplemod.worldgen.GrowthPlacementBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalPlantIntegrityTests {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    @GameTest(template = "plant_gameplay_empty")
    public static void replacingDoublePlantLowerHalfCleansUpperWithoutSeeds(GameTestHelper helper) {
        ServerLevel level = tropical(helper);
        BlockPos pos = site(helper, 0);
        prepareDouble(level, pos);
        level.setBlock(pos, Blocks.MELON.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.MELON), "Replacement melon was lost");
        helper.assertTrue(level.getBlockState(pos.above()).isAir(), "Upper half survived replacement");
        assertNoDrops(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void replacingUpperHalfAndBufferedPlacementCleanOnlyTheirPartner(GameTestHelper helper) {
        ServerLevel level = tropical(helper);
        BlockPos pos = site(helper, 1);
        prepareDouble(level, pos);
        var buffer = new GrowthPlacementBuffer(level);
        buffer.setBlock(pos.above(), Blocks.AZALEA.defaultBlockState(), 2);
        helper.assertTrue(buffer.getBlockState(pos).isAir(), "Buffer retained dependent lower half");
        helper.assertTrue(buffer.commit(), "Buffered replacement failed");
        helper.assertTrue(level.getBlockState(pos).isAir(), "Lower half remained after commit");
        helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.AZALEA), "New plant was removed");
        assertNoDrops(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void losingVineSupportRemovesWholeColumnWithoutBerries(GameTestHelper helper) {
        ServerLevel level = tropical(helper);
        BlockPos pos = site(helper, 2);
        clear(level, pos);
        level.setBlock(pos.above(10), Blocks.OAK_LEAVES.defaultBlockState(), QUIET);
        for (int y = 9; y >= 0; y--) level.setBlock(pos.above(y),
                (y == 0 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                        .setValue(BlockStateProperties.BERRIES, true)
                        .setValue(BlockStateProperties.WATERLOGGED, false), QUIET);
        level.setBlock(pos.above(10), Blocks.AIR.defaultBlockState(), 2);
        for (int y = 0; y < 10; y++) helper.assertTrue(level.isEmptyBlock(pos.above(y)), "Detached vine part remained");
        assertNoDrops(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void staleDetachedVineTickIsDropFreeButHarvestStillYieldsBerries(GameTestHelper helper) {
        ServerLevel level = tropical(helper);
        BlockPos anchor = site(helper, 3);
        // The GameTest runner keeps its Overworld grid active, not the matching
        // tropical chunks. Item entities in an untracked chunk are not visible
        // to a world query, even when setBlock has synchronously loaded terrain.
        BlockPos pos = new BlockPos(((anchor.getX() >> 4) + 1000) * 16 + 8, 220,
                ((anchor.getZ() >> 4) + 1000) * 16 + 8);
        ChunkPos chunk = new ChunkPos(pos);
        boolean alreadyForced = level.getForcedChunks().contains(chunk.toLong());
        level.setChunkForced(chunk.x, chunk.z, true);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.isPositionEntityTicking(pos),
                        "Harvest fixture must be entity-ticking before checking actual item drops"))
                .thenExecute(() -> {
                    try {
                        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                            for (int y = 0; y <= 12; y++) level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), QUIET);
                            level.setBlock(pos.offset(x, -1, z), Blocks.MOSS_BLOCK.defaultBlockState(), QUIET);
                        }
                        clear(level, pos);
                        var vine = Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.BERRIES, true)
                                .setValue(BlockStateProperties.AGE_25, 25).setValue(BlockStateProperties.WATERLOGGED, false);
                        level.setBlock(pos, vine, QUIET);
                        vine.tick(level, pos, level.random);
                        helper.assertTrue(level.isEmptyBlock(pos), "Unsupported vine survived its scheduled tick");
                        assertNoDrops(helper, level, pos);
                        level.setBlock(pos.above(), Blocks.OAK_LEAVES.defaultBlockState()
                                .setValue(BlockStateProperties.PERSISTENT, true), QUIET);
                        level.setBlock(pos, vine, QUIET);
                        helper.assertTrue(vine.canSurvive(level, pos), "The harvest control needs a genuinely supported vine");
                        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "VineHarvestQA"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        player.moveTo(Vec3.atCenterOf(pos).add(2, 0, 0));
                        var result = level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                        helper.assertTrue(result.consumesAction(), "Supported vine harvesting must consume the interaction");
                        helper.assertFalse(level.getBlockState(pos).getValue(BlockStateProperties.BERRIES),
                                "A successful harvest must remove berries from the supported vine");
                        int berries = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                                .filter(item -> item.getItem().is(Items.GLOW_BERRIES))
                                .mapToInt(item -> item.getItem().getCount()).sum();
                        helper.assertTrue(berries == 2, "Normal tropical harvesting must create its actual two configured berries; found " + berries);
                    } finally {
                        clear(level, pos);
                        if (!alreadyForced) level.setChunkForced(chunk.x, chunk.z, false);
                    }
                }).thenSucceed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalVineColumnsAreThinnedAcrossIndependentSeeds(GameTestHelper helper) {
        for (long seed : new long[]{0, 1, -1, 987654321}) {
            int kept = 0;
            for (int x = -128; x < 128; x++) for (int z = -128; z < 128; z++) {
                if (CaveVinePlacement.keepTropicalColumn(seed, x, z)) kept++;
            }
            double fraction = kept / 65536.0;
            helper.assertTrue(fraction > .037 && fraction < .047, "Vine retention is not approximately one in twenty-four: " + fraction);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void otherDimensionsKeepTheirOriginalReplacementAndVineFrequency(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        prepareDouble(level, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        helper.assertTrue(level.getBlockState(pos.above()).is(Blocks.LARGE_FERN), "Tropical cleanup leaked into another dimension");
        helper.assertTrue(CaveVinePlacement.keepColumn(level, pos), "Tropical vine thinning leaked into another dimension");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void leavesReplaceVinesWithoutLosingTheHangingSection(GameTestHelper helper) {
        var level = tropical(helper);
        for (boolean submerged : new boolean[]{false, true}) checkLeafClearance(helper, level, submerged);
        helper.succeed();
    }

    private static void checkLeafClearance(GameTestHelper helper, ServerLevel level, boolean submerged) {
        BlockPos base = site(helper, submerged ? 6 : 5);
        clear(level, base);
        level.setBlock(base.above(11), Blocks.OAK_LOG.defaultBlockState(), QUIET);
        for (int y = 10; y >= 0; y--) level.setBlock(base.above(y),
                (y == 0 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                        .setValue(BlockStateProperties.BERRIES, true)
                        .setValue(BlockStateProperties.WATERLOGGED, submerged), QUIET);
        var tree = new GrowthPlacementBuffer(level);
        helper.assertTrue(net.minecraft.world.level.levelgen.feature.TreeFeature.validTreePos(tree, base.above(4)),
                "A tropical tree must accept a vine in its intended crown");
        for (int y : new int[]{5, 4}) tree.setBlock(base.above(y), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true), QUIET);
        helper.assertTrue(tree.commit(), "The crown must commit as one tree");
        for (int y = 0; y < 4; y++) helper.assertTrue(
                net.amurdza.examplemod.worldgen.TropicalPlantIntegrity.isCaveVine(level.getBlockState(base.above(y))),
                "The lower vine must hang from the bottom leaf");
        for (int y = 6; y <= 9; y++) {
            var state = level.getBlockState(base.above(y));
            helper.assertTrue(submerged ? state.is(Blocks.WATER) : state.isAir(),
                    "The four cleared vine cells must retain the surrounding air or water");
        }
        helper.assertTrue(level.getBlockState(base.above(10)).is(Blocks.CAVE_VINES), "The upper vine must end in a living tip");
        assertNoDrops(helper, level, base);
    }

    private static ServerLevel tropical(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
    }

    private static BlockPos site(GameTestHelper helper, int offset) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(anchor.getX(), 220, anchor.getZ() + offset * 32);
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3, 12, 3)).forEach(ItemEntity::discard);
        for (int y = 12; y >= 0; y--) level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), QUIET);
        level.setBlock(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState(), QUIET);
    }

    private static void prepareDouble(ServerLevel level, BlockPos pos) {
        clear(level, pos);
        level.setBlock(pos, Blocks.LARGE_FERN.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), QUIET);
        level.setBlock(pos.above(), Blocks.LARGE_FERN.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), QUIET);
    }

    private static void assertNoDrops(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3, 12, 3)).isEmpty(), "Plant cleanup dropped items");
    }
}
