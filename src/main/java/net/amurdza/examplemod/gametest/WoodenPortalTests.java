package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.portal.PortalDestination;
import net.amurdza.examplemod.portal.PortalFrame;
import net.amurdza.examplemod.portal.PortalLinks;
import net.amurdza.examplemod.portal.PortalTravel;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WoodenPortalTests {
    private WoodenPortalTests() { }

    @GameTest(template = "wooden_portal_empty")
    public static void exactDimensionRoutes(GameTestHelper helper) {
        helper.assertTrue(PortalDestination.allowedFrom(Level.OVERWORLD).size() == 5,
                "Overworld must offer exactly the five AOE overworld dimensions");
        for (PortalDestination source : PortalDestination.values()) {
            for (PortalDestination destination : PortalDestination.values()) {
                boolean expected = source != destination
                        && (source == PortalDestination.NETHER ? destination == PortalDestination.DESERT
                        : destination != PortalDestination.NETHER || source == PortalDestination.DESERT);
                helper.assertTrue(destination.allowedFromSource(source.dimension()) == expected,
                        "Wrong route " + source + " -> " + destination);
            }
        }
        helper.assertTrue(PortalDestination.allowedFrom(Level.NETHER).isEmpty()
                        && PortalDestination.allowedFrom(Level.END).isEmpty(),
                "Vanilla Nether/End must not gain AOE routes");
        helper.succeed();
    }

    @GameTest(template = "wooden_portal_empty")
    public static void mixedPlanksActivateBothAxes(GameTestHelper helper) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalFrame frame = frame(helper, axis == Direction.Axis.X ? 2 : 8, axis);
            build(helper, frame);
            helper.assertTrue(frame.valid(helper.getLevel()), "Both burnable and nonburnable planks must form a frame");
            helper.assertTrue(PortalFrame.touching(helper.getLevel(), frame.at(-1, -1)).isPresent(),
                    "Clicking a corner must find the frame");
            helper.assertTrue(frame.activate(helper.getLevel(), PortalDestination.TROPICAL),
                    "A valid frame must activate");
            helper.assertTrue(frame.active(helper.getLevel(), PortalDestination.TROPICAL),
                    "Every cell must have the selected destination and axis");
        }
        helper.succeed();
    }

    @GameTest(template = "wooden_portal_empty")
    public static void rejectBrokenObstructedAndNonPlankFrames(GameTestHelper helper) {
        PortalFrame frame = frame(helper, 2, Direction.Axis.X);
        build(helper, frame);
        helper.assertTrue(!frame.activate(helper.getLevel(), PortalDestination.NETHER),
                "An intact Overworld frame must reject the AOE Nether destination");
        helper.assertTrue(frame.interior().stream().allMatch(pos -> helper.getLevel().getBlockState(pos).isAir()),
                "A forbidden destination must leave the opening untouched");
        helper.getLevel().setBlockAndUpdate(frame.at(-1, 1), Blocks.OBSIDIAN.defaultBlockState());
        helper.assertTrue(!frame.valid(helper.getLevel()), "Obsidian is not a wooden frame");
        helper.getLevel().setBlockAndUpdate(frame.at(-1, 1), Blocks.WARPED_PLANKS.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(frame.at(0, 1), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!frame.valid(helper.getLevel()), "An obstructed opening must be rejected");
        helper.getLevel().setBlockAndUpdate(frame.at(0, 1), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(frame.at(0, frame.height()), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!frame.valid(helper.getLevel()), "A missing top plank must be rejected");
        helper.assertTrue(!frame.activate(helper.getLevel(), PortalDestination.NETHER),
                "A forbidden destination must never fill the frame");
        helper.succeed();
    }

    @GameTest(template = "wooden_portal_empty")
    public static void breakingFrameRemovesEntirePortal(GameTestHelper helper) {
        PortalFrame frame = frame(helper, 2, Direction.Axis.X);
        build(helper, frame);
        helper.assertTrue(frame.activate(helper.getLevel(), PortalDestination.TROPICAL), "Activation failed");
        helper.getLevel().setBlockAndUpdate(frame.at(0, frame.height()), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(frame.interior().stream().allMatch(pos -> helper.getLevel().getBlockState(pos).isAir()),
                    "Breaking one frame block must clear every portal cell");
            helper.succeed();
        });
    }

    @GameTest(template = "wooden_portal_empty")
    public static void portalLinksSurviveSaveAndReload(GameTestHelper helper) {
        PortalLinks data = new PortalLinks();
        PortalLinks.Endpoint source = new PortalLinks.Endpoint(Level.OVERWORLD, frame(helper, 2, Direction.Axis.X));
        PortalLinks.Endpoint target = new PortalLinks.Endpoint(GameplayDimensions.TROPICAL,
                new PortalFrame(new BlockPos(123, 70, -456), Direction.Axis.Z, 2, 3));
        data.connect(source, target);
        PortalLinks restored = PortalLinks.load(data.save(new CompoundTag()));
        helper.assertTrue(target.equals(restored.target(source)), "Outbound link lost after reload");
        helper.assertTrue(source.equals(restored.target(target)), "Return link lost after reload");
        helper.succeed();
    }

    @GameTest(template = "wooden_portal_empty")
    public static void arrivalSearchPreservesBuildsAndFluids(GameTestHelper helper) {
        PortalFrame frame = new PortalFrame(helper.absolutePos(new BlockPos(3, 5, 3)), Direction.Axis.X, 2, 3);
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    helper.getLevel().setBlockAndUpdate(frame.bottomLeft().offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.assertTrue(PortalTravel.clearArrivalSpace(helper.getLevel(), frame), "Clear air must be usable");
        helper.getLevel().setBlockAndUpdate(frame.at(0, 0), Blocks.CHEST.defaultBlockState());
        helper.assertTrue(!PortalTravel.clearArrivalSpace(helper.getLevel(), frame), "A container must never be overwritten");
        helper.getLevel().setBlockAndUpdate(frame.at(0, 0), Blocks.LAVA.defaultBlockState());
        helper.assertTrue(!PortalTravel.clearArrivalSpace(helper.getLevel(), frame), "A lava arrival must be rejected");
        helper.getLevel().setBlockAndUpdate(frame.at(0, 0), Blocks.WATER.defaultBlockState());
        helper.assertTrue(!PortalTravel.clearArrivalSpace(helper.getLevel(), frame), "An underwater arrival must be rejected");
        helper.assertTrue(PortalTravel.arrivalPlanks(PortalDestination.NETHER) == Blocks.CRIMSON_PLANKS,
                "The Nether return frame must use nonburnable local planks");
        helper.succeed();
    }


    @GameTest(template = "wooden_portal_empty", timeoutTicks = 500)
    public static void playerTravelsThroughDesertToNetherAndBack(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel desert = overworld.getServer().getLevel(GameplayDimensions.DESERT);
        ServerLevel nether = overworld.getServer().getLevel(GameplayDimensions.NETHER);
        helper.assertTrue(desert != null && nether != null, "The AOE Desert and Nether dimensions must be loaded");
        PortalFrame home = frame(helper, 2, Direction.Axis.X);
        PortalFrame desertFrame = new PortalFrame(new BlockPos(104, desert.getMaxBuildHeight() - 12, 104),
                Direction.Axis.X, 2, 3);
        PortalFrame netherFrame = new PortalFrame(new BlockPos(104, nether.getMaxBuildHeight() - 12, 104),
                Direction.Axis.Z, 2, 3);
        // FakePlayer has no client chunk tracking. Retain both remote fixtures throughout the cooldown.
        net.minecraft.world.level.ChunkPos remoteChunk = new net.minecraft.world.level.ChunkPos(desertFrame.bottomLeft());
        boolean desertAlreadyForced = desert.getForcedChunks().contains(remoteChunk.toLong());
        boolean netherAlreadyForced = nether.getForcedChunks().contains(remoteChunk.toLong());
        desert.setChunkForced(remoteChunk.x, remoteChunk.z, true);
        nether.setChunkForced(remoteChunk.x, remoteChunk.z, true);
        Map<BlockPos, BlockState> desertBefore = snapshot(desert, desertFrame);
        Map<BlockPos, BlockState> netherBefore = snapshot(nether, netherFrame);
        PortalLinks links = PortalLinks.get(overworld.getServer());
        PortalLinks.Endpoint homeEndpoint = new PortalLinks.Endpoint(overworld.dimension(), home);
        PortalLinks.Endpoint desertEndpoint = new PortalLinks.Endpoint(desert.dimension(), desertFrame);
        PortalLinks.Endpoint netherEndpoint = new PortalLinks.Endpoint(nether.dimension(), netherFrame);
        FakePlayer player = new FakePlayer(overworld, new GameProfile(UUID.randomUUID(), "PortalRouteTest"));
        player.getAbilities().instabuild = true;
        player.getAbilities().mayBuild = true;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.moveTo(home.bottomLeft().getX() + 0.5, home.bottomLeft().getY(), home.bottomLeft().getZ() + 0.5);
        overworld.addNewPlayer(player);
        Runnable cleanup = () -> {
            PortalTravel.forget(player.getUUID());
            player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            links.disconnect(homeEndpoint);
            links.disconnect(desertEndpoint);
            links.disconnect(netherEndpoint);
            desertBefore.forEach((pos, state) -> desert.setBlock(pos, state, 3));
            netherBefore.forEach((pos, state) -> nether.setBlock(pos, state, 3));
            if (!desertAlreadyForced) desert.setChunkForced(remoteChunk.x, remoteChunk.z, false);
            if (!netherAlreadyForced) nether.setChunkForced(remoteChunk.x, remoteChunk.z, false);
        };
        try {
            build(helper, home);
            buildExternal(desert, desertFrame);
            buildExternal(nether, netherFrame);
            helper.assertTrue(home.activate(overworld, PortalDestination.DESERT), "Home portal activation failed");
            helper.assertTrue(desertFrame.activate(desert, PortalDestination.OVERWORLD), "Desert return activation failed");
            helper.assertTrue(netherFrame.activate(nether, PortalDestination.DESERT), "Nether return activation failed");
            links.connect(homeEndpoint, desertEndpoint);
            PortalTravel.inside(player, home.bottomLeft(), PortalDestination.NETHER);
            helper.assertTrue(player.level() == overworld, "Direct Overworld to AOE Nether travel must be refused");
            PortalTravel.inside(player, home.bottomLeft(), PortalDestination.DESERT);
            helper.assertTrue(player.level() == desert && player.blockPosition().equals(desertFrame.bottomLeft()),
                    "Real player teleport must land in the linked Desert portal");
            helper.assertTrue(PortalTravel.safeLanding(desert, desertFrame), "Desert arrival must be safe");
            PortalTravel.inside(player, desertFrame.bottomLeft(), PortalDestination.OVERWORLD);
            helper.assertTrue(player.level() == desert, "Arrival cooldown must prevent immediate return loops");
            player.moveTo(desertFrame.bottomLeft().getX() + 0.5, desertFrame.bottomLeft().getY(),
                    desertFrame.bottomLeft().getZ() + 2.5);
            helper.runAfterDelay(101, () -> {
                try {
                    helper.assertTrue(desertFrame.activate(desert, PortalDestination.NETHER),
                            "Desert to Nether activation failed: loaded=" + desert.hasChunkAt(desertFrame.bottomLeft())
                                    + ", border=" + desertFrame.border().stream().map(desert::getBlockState).toList()
                                    + ", interior=" + desertFrame.interior().stream().map(desert::getBlockState).toList());
                    links.connect(desertEndpoint, netherEndpoint);
                    PortalTravel.inside(player, desertFrame.bottomLeft(), PortalDestination.NETHER);
                    helper.assertTrue(player.level() == nether && player.blockPosition().equals(netherFrame.bottomLeft()),
                            "The Desert route must teleport a player into the linked AOE Nether portal");
                    helper.assertTrue(PortalTravel.safeLanding(nether, netherFrame), "Nether arrival must be safe");
                    PortalTravel.inside(player, netherFrame.bottomLeft(), PortalDestination.DESERT);
                    helper.assertTrue(player.level() == nether, "The Nether arrival must also apply the cooldown");
                    player.moveTo(netherFrame.bottomLeft().getX() + 2.5, netherFrame.bottomLeft().getY(),
                            netherFrame.bottomLeft().getZ() + 0.5);
                    helper.runAfterDelay(101, () -> {
                        try {
                            PortalTravel.inside(player, netherFrame.bottomLeft(), PortalDestination.DESERT);
                            helper.assertTrue(player.level() == desert && player.blockPosition().equals(desertFrame.bottomLeft()),
                                    "Leaving and returning after cooldown must use the existing linked Desert frame");
                            helper.succeed();
                        } finally {
                            cleanup.run();
                        }
                    });
                } catch (RuntimeException | Error exception) {
                    cleanup.run();
                    throw exception;
                }
            });
        } catch (RuntimeException | Error exception) {
            cleanup.run();
            throw exception;
        }
    }

    @GameTest(template = "wooden_portal_empty")
    public static void failedArrivalLeavesNoPartialFrame(GameTestHelper helper) {
        PortalFrame frame = new PortalFrame(helper.absolutePos(new BlockPos(3, 5, 3)), Direction.Axis.X, 2, 3);
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortalRollbackTest"));
        Map<BlockPos, BlockState> original = snapshot(helper.getLevel(), frame);
        helper.assertTrue(PortalTravel.clearArrivalSpace(helper.getLevel(), frame), "Fixture must start clear");
        helper.assertTrue(!PortalTravel.buildArrival(helper.getLevel(), player, frame, PortalDestination.NETHER),
                "Forbidden final activation must fail even after the arrival frame was placed");
        helper.assertTrue(original.entrySet().stream().allMatch(entry ->
                        helper.getLevel().getBlockState(entry.getKey()).equals(entry.getValue())),
                "Failed activation must roll back every generated plank and portal cell");
        helper.getLevel().setBlockAndUpdate(frame.bottomLeft(), Blocks.CHEST.defaultBlockState());
        helper.assertTrue(!PortalTravel.buildArrival(helper.getLevel(), player, frame, PortalDestination.TROPICAL),
                "An obstructed destination must not create a partial frame");
        helper.assertTrue(helper.getLevel().getBlockState(frame.bottomLeft()).is(Blocks.CHEST),
                "Arrival generation must preserve the blocking container");
        helper.assertTrue(frame.border().stream().allMatch(pos -> helper.getLevel().getBlockState(pos).isAir()),
                "No planks may remain after a blocked arrival attempt");
        helper.succeed();
    }

    private static Map<BlockPos, BlockState> snapshot(ServerLevel level, PortalFrame frame) {
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = frame.bottomLeft().offset(x, y, z);
                    before.put(pos, level.getBlockState(pos));
                }
            }
        }
        // The Z-axis frame extends sideways along Z rather than X.
        frame.border().forEach(pos -> before.putIfAbsent(pos, level.getBlockState(pos)));
        frame.interior().forEach(pos -> before.putIfAbsent(pos, level.getBlockState(pos)));
        return before;
    }

    private static void buildExternal(ServerLevel level, PortalFrame frame) {
        BlockState planks = PortalTravel.arrivalPlanks(PortalDestination.fromDimension(level.dimension()).orElseThrow())
                .defaultBlockState();
        frame.interior().forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        frame.border().forEach(pos -> level.setBlockAndUpdate(pos, planks));
    }

    private static PortalFrame frame(GameTestHelper helper, int x, Direction.Axis axis) {
        return new PortalFrame(helper.absolutePos(new BlockPos(x, 3, 3)), axis, 2, 3);
    }

    private static void build(GameTestHelper helper, PortalFrame frame) {
        frame.interior().forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        int index = 0;
        for (BlockPos pos : frame.border()) {
            var planks = index++ % 2 == 0 ? Blocks.OAK_PLANKS : Blocks.CRIMSON_PLANKS;
            helper.getLevel().setBlockAndUpdate(pos, planks.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(frame.at(-1, -1), Blocks.WARPED_PLANKS.defaultBlockState());
    }
}
