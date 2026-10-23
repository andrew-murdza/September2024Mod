package net.amurdza.examplemod.portal;

import net.amurdza.examplemod.registry.ModPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Nether-sized rectangular openings: 2..21 wide, 3..21 tall, with optional corners. */
public record PortalFrame(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
    public static final int MAX_SIZE = 21;

    public PortalFrame {
        bottomLeft = bottomLeft.immutable();
        if (axis == Direction.Axis.Y || width < 2 || width > MAX_SIZE || height < 3 || height > MAX_SIZE) {
            throw new IllegalArgumentException("Invalid wooden portal frame");
        }
    }

    public BlockPos at(int horizontal, int vertical) {
        return axis == Direction.Axis.X ? bottomLeft.offset(horizontal, vertical, 0)
                : bottomLeft.offset(0, vertical, horizontal);
    }

    public List<BlockPos> interior() {
        List<BlockPos> positions = new ArrayList<>(width * height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                positions.add(at(x, y));
            }
        }
        return positions;
    }

    public List<BlockPos> border() {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            positions.add(at(x, -1));
            positions.add(at(x, height));
        }
        for (int y = 0; y < height; y++) {
            positions.add(at(-1, y));
            positions.add(at(width, y));
        }
        return positions;
    }

    private static boolean interiorState(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof WoodenPortalBlock;
    }

    private static boolean plank(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).is(BlockTags.PLANKS);
    }

    public static Optional<PortalFrame> find(Level level, BlockPos inside, Direction.Axis axis) {
        if (axis == Direction.Axis.Y || !interiorState(level, inside)) {
            return Optional.empty();
        }
        BlockPos bottom = inside;
        for (int i = 0; i < MAX_SIZE && interiorState(level, bottom.below()); i++) {
            bottom = bottom.below();
        }
        if (!plank(level, bottom.below())) {
            return Optional.empty();
        }
        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        for (int i = 0; i < MAX_SIZE && interiorState(level, bottom.relative(left)); i++) {
            bottom = bottom.relative(left);
        }
        if (!plank(level, bottom.relative(left))) {
            return Optional.empty();
        }
        Direction right = left.getOpposite();
        int width = 0;
        while (width <= MAX_SIZE && interiorState(level, bottom.relative(right, width))) {
            if (!plank(level, bottom.relative(right, width).below())) {
                return Optional.empty();
            }
            width++;
        }
        if (width < 2 || width > MAX_SIZE || !plank(level, bottom.relative(right, width))) {
            return Optional.empty();
        }
        for (int y = 0; y <= MAX_SIZE; y++) {
            boolean top = true;
            for (int x = 0; x < width; x++) {
                top &= plank(level, bottom.relative(right, x).above(y));
            }
            if (top) {
                return y >= 3 ? Optional.of(new PortalFrame(bottom, axis, width, y)) : Optional.empty();
            }
            if (y == MAX_SIZE || !plank(level, bottom.relative(left).above(y))
                    || !plank(level, bottom.relative(right, width).above(y))) {
                return Optional.empty();
            }
            for (int x = 0; x < width; x++) {
                if (!interiorState(level, bottom.relative(right, x).above(y))) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<PortalFrame> touching(Level level, BlockPos clicked) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            // Includes diagonal neighbors, so clicking any corner is sufficient.
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos candidate = axis == Direction.Axis.X ? clicked.offset(x, y, 0)
                            : clicked.offset(0, y, x);
                    Optional<PortalFrame> found = find(level, candidate, axis);
                    if (found.isPresent() && (found.get().border().contains(clicked)
                            || found.get().interior().contains(clicked) || isCorner(found.get(), clicked))) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isCorner(PortalFrame frame, BlockPos pos) {
        return pos.equals(frame.at(-1, -1)) || pos.equals(frame.at(frame.width, -1))
                || pos.equals(frame.at(-1, frame.height)) || pos.equals(frame.at(frame.width, frame.height));
    }

    public boolean valid(Level level) {
        return find(level, bottomLeft, axis).filter(this::equals).isPresent();
    }

    public boolean active(Level level, PortalDestination destination) {
        return valid(level) && interior().stream().allMatch(pos -> {
            BlockState state = level.getBlockState(pos);
            return state.is(ModPortals.block(destination).get()) && state.getValue(WoodenPortalBlock.AXIS) == axis;
        });
    }

    public boolean activate(ServerLevel level, PortalDestination destination) {
        if (!destination.allowedFromSource(level.dimension()) || !valid(level)) {
            return false;
        }
        BlockState portal = ModPortals.block(destination).get().defaultBlockState()
                .setValue(WoodenPortalBlock.AXIS, axis);
        // Delay neighbor validation until the complete rectangle has been filled.
        java.util.Map<BlockPos, BlockState> before = new java.util.LinkedHashMap<>();
        interior().forEach(pos -> before.put(pos, level.getBlockState(pos)));
        for (BlockPos pos : interior()) {
            level.setBlock(pos, portal, 2);
            if (!level.getBlockState(pos).equals(portal)) {
                before.forEach((restorePos, state) -> level.setBlock(restorePos, state, 3));
                return false;
            }
        }
        for (BlockPos pos : interior()) {
            level.updateNeighborsAt(pos, portal.getBlock());
            level.scheduleTick(pos, portal.getBlock(), 1);
        }
        return true;
    }
}
