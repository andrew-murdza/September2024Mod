package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit isolated-client QA. The local fixture is restored before another frame can render. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientGatePredictionAudit {
    private static final String REPORT = System.getProperty("aoemod.qa.gatePredictionReport");
    private static boolean attempted;

    private ClientGatePredictionAudit() { }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (REPORT == null || attempted || event.phase != TickEvent.Phase.END
                || minecraft.level == null || minecraft.player == null || minecraft.player.isCreative()
                || minecraft.screen != null || minecraft.getOverlay() != null
                || !minecraft.level.dimension().equals(GameplayDimensions.PLAINS)) return;
        var playerChunk = minecraft.player.chunkPosition();
        if (minecraft.level.getChunkSource().getChunk(playerChunk.x, playerChunk.z, ChunkStatus.FULL, false) == null) return;
        attempted = true;
        var result = new LinkedHashMap<String, Object>();
        try {
            result.put("checks", verify(minecraft));
            result.put("passed", true);
        } catch (Throwable failure) {
            result.put("passed", false);
            result.put("error", failure.toString());
        }
        try {
            Path report = Path.of(REPORT).toAbsolutePath();
            Files.createDirectories(report.getParent());
            Files.writeString(report, new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot write client gate QA report", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> verify(Minecraft minecraft) throws ReflectiveOperationException {
        var level = minecraft.level;
        BlockPos pos = null;
        for (int offset = 3; offset <= 32; offset++) {
            var candidate = minecraft.player.blockPosition().above(offset);
            if (candidate.getY() >= level.getMaxBuildHeight()) break;
            if (level.isEmptyBlock(candidate) && level.getBlockEntity(candidate) == null) { pos = candidate; break; }
        }
        require(pos != null, "No local air cell available for the temporary gate fixture");
        var chunk = level.getChunkAt(pos);
        var previousBlock = level.getBlockState(pos);
        var field = MansionProtectionSnapshot.class.getDeclaredField("RECEIVED");
        field.setAccessible(true);
        var snapshots = (Map<LevelChunk, MansionProtectionSnapshot>) field.get(null);
        var previousSnapshot = snapshots.get(chunk);
        var bounds = BoundingBox.fromCorners(pos, pos);
        var gate = Blocks.OAK_FENCE_GATE.defaultBlockState();
        try {
            // Only the ClientLevel is changed. Calling BlockState.use directly sends no interaction packet.
            level.setBlock(pos, gate, 18);
            require(level.getBlockState(pos).is(Blocks.OAK_FENCE_GATE), "Client gate fixture was not placed");
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            snapshots.put(chunk, new MansionProtectionSnapshot(List.of(bounds), List.of()));
            require(MansionProtectionSnapshot.rewardsLocked(level, pos), "Client fixture lock was not installed");
            InteractionResult locked = level.getBlockState(pos).use(level, minecraft.player, InteractionHand.MAIN_HAND, hit);
            require(locked == InteractionResult.FAIL && level.getBlockState(pos).equals(gate),
                    "Locked gate changed during the real client use method: " + locked + "/" + level.getBlockState(pos));
            snapshots.put(chunk, new MansionProtectionSnapshot(List.of(bounds), List.of(), List.of(bounds)));
            require(!MansionProtectionSnapshot.rewardsLocked(level, pos), "Cleared snapshot retained its gate use lock");
            InteractionResult opened = level.getBlockState(pos).use(level, minecraft.player, InteractionHand.MAIN_HAND, hit);
            require(opened.consumesAction() && level.getBlockState(pos).getValue(FenceGateBlock.OPEN),
                    "Cleared client gate did not open normally");
            InteractionResult closed = level.getBlockState(pos).use(level, minecraft.player, InteractionHand.MAIN_HAND, hit);
            require(closed.consumesAction() && !level.getBlockState(pos).getValue(FenceGateBlock.OPEN),
                    "Cleared client gate did not close normally");
            require(MansionProtectionSnapshot.protects(level, pos), "Cleared gate lost permanent mining protection");
            return List.of("Actual ClientLevel gate use rejects the locked snapshot before opening",
                    "Cleared snapshot permits native opening and closing", "Cleared gate remains unmineable");
        } finally {
            try { level.setBlock(pos, previousBlock, 18); }
            finally {
                if (previousSnapshot == null) snapshots.remove(chunk);
                else snapshots.put(chunk, previousSnapshot);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Client gate prediction: " + message);
    }
}
