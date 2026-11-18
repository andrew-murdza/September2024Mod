package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeGrassBreakTests {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void creativeGrassBreakRemovesBothHalvesWithoutSeedsEvenWithShears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos pos = new BlockPos(32008, 220, 32008);
        ChunkPos chunk = new ChunkPos(pos);
        boolean forced = level.getForcedChunks().contains(chunk.toLong());
        level.setChunkForced(chunk.x, chunk.z, true);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.isPositionEntityTicking(pos),
                "The drop audit requires an entity-ticking chunk")).thenExecute(() -> {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "GrassBreakQA"));
            player.moveTo(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
            try {
                clearDrops(level, pos);
                player.setGameMode(GameType.CREATIVE);
                for (Block plant : new Block[]{Blocks.TALL_GRASS, Blocks.LARGE_FERN}) {
                    for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                        for (boolean shears : new boolean[]{false, true}) {
                            player.setItemInHand(InteractionHand.MAIN_HAND,
                                    shears ? new ItemStack(Items.SHEARS) : ItemStack.EMPTY);
                            for (int trial = 0; trial < 64; trial++) {
                                plant(level, pos, plant);
                                player.gameMode.destroyBlock(half == DoubleBlockHalf.LOWER ? pos : pos.above());
                                helper.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()),
                                        "Creative breaking must remove both halves: " + plant + "/" + half + "/shears=" + shears);
                                helper.assertTrue(dropCount(level, pos) == 0,
                                        "Creative breaking leaked seed or plant drops: " + plant + "/" + half + "/shears=" + shears);
                            }
                        }
                    }
                }
                // Positive controls exercise the same real break event and item
                // queries, proving the fixture is capable of observing drops.
                player.setGameMode(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                for (int trial = 0; trial < 128; trial++) {
                    plant(level, pos, Blocks.TALL_GRASS);
                    player.gameMode.destroyBlock(pos);
                }
                helper.assertTrue(dropCount(level, pos) > 0, "Survival grass must still yield regional seeds");
                clearDrops(level, pos);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
                plant(level, pos, Blocks.TALL_GRASS);
                player.gameMode.destroyBlock(pos);
                helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                                .anyMatch(item -> item.getItem().is(Items.GRASS)),
                        "Survival shears must still harvest grass items");
            } finally {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), QUIET);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
                clearDrops(level, pos);
                if (!forced) level.setChunkForced(chunk.x, chunk.z, false);
            }
        }).thenSucceed();
    }

    private static void plant(ServerLevel level, BlockPos pos, Block plant) {
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), QUIET);
        level.setBlock(pos, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), QUIET);
        level.setBlock(pos.above(), plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), QUIET);
    }

    private static int dropCount(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                .mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static void clearDrops(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
    }
}
