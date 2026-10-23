package net.amurdza.examplemod.portal;

import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalTravel {
    private static final String COOLDOWN = "aoemod_wooden_portal_cooldown";
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    private PortalTravel() { }

    public static void forget(UUID player) { CHARGES.remove(player); }
    public static void clear() { CHARGES.clear(); }

    public static void inside(ServerPlayer player, BlockPos pos, PortalDestination destination) {
        if (!player.isAlive() || player.isSpectator() || !player.canChangeDimensions()
                || !destination.allowedFromSource(player.level().dimension())) {
            return;
        }
        long now = player.server.overworld().getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN) > now) {
            // Remaining in the arrival portal keeps the cooldown armed until the player steps out.
            player.getPersistentData().putLong(COOLDOWN, now + 100);
            CHARGES.remove(player.getUUID());
            return;
        }
        Charge old = CHARGES.get(player.getUUID());
        String route = player.level().dimension().location() + "|" + destination.id();
        long started = old != null && old.route.equals(route) && old.lastTick >= now - 1 ? old.started : now;
        CHARGES.put(player.getUUID(), new Charge(route, started, now));
        if (!player.getAbilities().instabuild && now - started < 80) {
            return;
        }
        var source = PortalFrame.find(player.level(), pos,
                player.level().getBlockState(pos).getValue(WoodenPortalBlock.AXIS));
        if (source.isEmpty() || !source.get().active(player.level(), destination)) {
            return;
        }
        CHARGES.remove(player.getUUID());
        // Also rate-limit a failed arrival search so a blocked dimension cannot regenerate chunks every tick.
        player.getPersistentData().putLong(COOLDOWN, now + 100);
        travel(player, source.get(), destination);
    }

    private static void travel(ServerPlayer player, PortalFrame source, PortalDestination destination) {
        ServerLevel targetLevel = player.server.getLevel(destination.dimension());
        PortalDestination returnDestination = PortalDestination.fromDimension(player.level().dimension()).orElse(null);
        if (targetLevel == null || returnDestination == null) {
            player.displayClientMessage(Component.translatable("portal.aoemod.destination_unavailable"), true);
            return;
        }
        PortalLinks links = PortalLinks.get(player.server);
        PortalLinks.Endpoint sourceEndpoint = new PortalLinks.Endpoint(player.level().dimension(), source);
        PortalLinks.Endpoint target = links.target(sourceEndpoint);
        if (target != null && target.dimension().equals(destination.dimension())) {
            // Explicitly load the saved portal's chunks before checking; an unloaded frame is not a broken one.
            for (BlockPos borderPos : target.frame().border()) {
                targetLevel.getChunkAt(borderPos);
            }
        }
        if (target == null || !target.dimension().equals(destination.dimension())
                || !target.frame().active(targetLevel, returnDestination)
                || !safeLanding(targetLevel, target.frame())) {
            PortalFrame arrival = createArrival(targetLevel, player, source.bottomLeft(), returnDestination);
            if (arrival == null) {
                player.displayClientMessage(Component.translatable("portal.aoemod.no_safe_arrival"), true);
                return;
            }
            target = new PortalLinks.Endpoint(destination.dimension(), arrival);
            links.connect(sourceEndpoint, target);
        }
        BlockPos feet = target.frame().bottomLeft();
        player.teleportTo(targetLevel, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setPortalCooldown();
    }

    public static boolean safeLanding(ServerLevel level, PortalFrame frame) {
        BlockPos feet = frame.bottomLeft();
        return level.getWorldBorder().isWithinBounds(feet)
                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty();
    }

    /** Searches only clear space. Existing terrain, builds, fluids, containers and portals are never replaced. */
    private static PortalFrame createArrival(ServerLevel level, ServerPlayer player, BlockPos source,
            PortalDestination returnDestination) {
        var border = level.getWorldBorder();
        int centerX = Mth.floor(Mth.clamp(source.getX(), border.getMinX() + 8, border.getMaxX() - 8));
        int centerZ = Mth.floor(Mth.clamp(source.getZ(), border.getMinZ() + 8, border.getMaxZ() - 8));
        int maxY = Math.min(level.getMaxBuildHeight() - 5,
                level.dimensionType().hasCeiling() ? level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 5
                        : level.getMaxBuildHeight() - 5);
        for (int radius = 0; radius <= 32; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    level.getChunk(x >> 4, z >> 4);
                    int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (!level.dimensionType().hasCeiling()) {
                        // Build a supported frame just above terrain, or a raised platform above water/trees.
                        for (int rise : new int[]{1, 5, 10}) {
                            int y = Math.min(maxY, Math.max(level.getMinBuildHeight() + 2, surface + rise));
                            PortalFrame frame = new PortalFrame(new BlockPos(x, y, z), Direction.Axis.X, 2, 3);
                            if (buildArrival(level, player, frame, returnDestination)) {
                                return frame;
                            }
                        }
                    } else {
                        // The custom Nether has a roof. Search below its logical ceiling, never above bedrock.
                        for (int y = Math.min(80, maxY); y > level.getMinBuildHeight() + 4; y--) {
                            PortalFrame frame = new PortalFrame(new BlockPos(x, y, z), Direction.Axis.X, 2, 3);
                            if (buildArrival(level, player, frame, returnDestination)) {
                                return frame;
                            }
                        }
                        for (int y = 81; y <= maxY; y++) {
                            PortalFrame frame = new PortalFrame(new BlockPos(x, y, z), Direction.Axis.X, 2, 3);
                            if (buildArrival(level, player, frame, returnDestination)) {
                                return frame;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean clearArrivalSpace(ServerLevel level, PortalFrame frame) {
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = frame.bottomLeft().offset(x, y, z);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                            || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()
                            || level.getBlockEntity(pos) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean buildArrival(ServerLevel level, ServerPlayer player, PortalFrame frame,
            PortalDestination returnDestination) {
        if (!clearArrivalSpace(level, frame)) {
            return false;
        }
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        BlockState planks = arrivalPlanks(PortalDestination.fromDimension(level.dimension()).orElseThrow())
                .defaultBlockState();
        Map<BlockPos, BlockState> planned = new LinkedHashMap<>();
        // A three-block-deep landing ledge supports both exits even over water or a Nether cavern.
        for (int x = -1; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                planned.put(frame.bottomLeft().offset(x, -1, z), planks);
            }
        }
        frame.border().forEach(pos -> planned.put(pos, planks));
        planned.put(frame.at(-1, 3), planks);
        planned.put(frame.at(2, 3), planks);
        for (BlockPos pos : planned.keySet()) {
            if (!level.mayInteract(player, pos)) {
                return false;
            }
            before.put(pos, level.getBlockState(pos));
        }
        for (var entry : planned.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 2);
            if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) {
                before.forEach((pos, state) -> level.setBlock(pos, state, 3));
                return false;
            }
        }
        if (!frame.activate(level, returnDestination)) {
            before.forEach((pos, state) -> level.setBlock(pos, state, 3));
            return false;
        }
        planned.forEach((pos, state) -> level.updateNeighborsAt(pos, state.getBlock()));
        return true;
    }

    public static Block arrivalPlanks(PortalDestination destination) {
        return switch (destination) {
            case SAVANNA -> Blocks.ACACIA_PLANKS;
            case SNOWY -> Blocks.SPRUCE_PLANKS;
            case DESERT -> ModBlocks.PALE_OAK_PLANKS.get();
            case NETHER -> Blocks.CRIMSON_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private record Charge(String route, long started, long lastTick) { }
}
