package net.amurdza.examplemod.worldgen;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.*;
import net.minecraft.world.ticks.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** A feature's proposed blocks and side effects stay private until its entire shape is valid. */
public final class GrowthPlacementBuffer implements WorldGenLevel {
    private record Placement(BlockState state, int flags, int recursion) { }
    private final WorldGenLevel level;
    private final Map<BlockPos, Placement> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final List<Runnable> effects = new ArrayList<>();
    private final LevelTickAccess<Block> blockTicks;
    private final LevelTickAccess<Fluid> fluidTicks;
    private boolean writable = true;

    public GrowthPlacementBuffer(WorldGenLevel level) {
        this.level = level;
        this.blockTicks = deferTicks(level.getBlockTicks());
        this.fluidTicks = deferTicks(level.getFluidTicks());
    }

    public boolean isValid(BiPredicate<BlockPos, BlockState> allowed) {
        return writable && blocks.entrySet().stream()
                .allMatch(entry -> allowed.test(entry.getKey(), entry.getValue().state()));
    }

    public boolean commit() {
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        Map<BlockPos, CompoundTag> previousEntities = new HashMap<>();
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            Placement placement = entry.getValue();
            BlockState old = level.getBlockState(pos);
            previous.put(pos, old);
            BlockEntity oldEntity = level.getBlockEntity(pos);
            if (oldEntity != null) previousEntities.put(pos, oldEntity.saveWithFullMetadata());
            // Notify neighbors and clients only after every proposed block was accepted.
            int quietFlags = (placement.flags() | 16) & ~3;
            if (old != placement.state() && !level.setBlock(pos, placement.state(), quietFlags, placement.recursion())) {
                rollback(previous, previousEntities);
                return false;
            }
        }
        blockEntities.forEach((pos, entity) -> {
            BlockEntity placed = level.getBlockEntity(pos);
            if (placed != null && placed.getType() == entity.getType()) {
                placed.load(entity.saveWithFullMetadata());
                placed.setChanged();
            }
        });
        if (level instanceof Level world) {
            blocks.forEach((pos, placement) -> world.markAndNotifyBlock(pos, world.getChunkAt(pos), previous.get(pos),
                    placement.state(), placement.flags(), placement.recursion()));
        }
        effects.forEach(Runnable::run);
        return true;
    }

    private void rollback(Map<BlockPos, BlockState> previous, Map<BlockPos, CompoundTag> previousEntities) {
        List<BlockPos> positions = new ArrayList<>(previous.keySet());
        Collections.reverse(positions);
        for (BlockPos pos : positions) {
            BlockState old = previous.get(pos);
            // Restoring a preexisting block must not be vetoed by a rule that forbids new placement there.
            level.getChunk(pos).setBlockState(pos, old, false);
            CompoundTag data = previousEntities.get(pos);
            if (data != null) {
                BlockEntity restored = BlockEntity.loadStatic(pos, old, data);
                if (restored != null) level.getChunk(pos).setBlockEntity(restored);
            }
            level.getLightEngine().checkBlock(pos);
        }
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        if (!level.ensureCanWrite(pos) || level.isOutsideBuildHeight(pos)) {
            writable = false;
            return false;
        }
        BlockPos immutable = pos.immutable();
        if (!getBlockState(pos).is(state.getBlock())) blockEntities.remove(immutable);
        blocks.put(immutable, new Placement(state, flags, recursion));
        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        Placement placement = blocks.get(pos);
        return placement == null ? level.getBlockState(pos) : placement.state();
    }

    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> test) { return test.test(getBlockState(pos)); }
    @Override public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> test) { return test.test(getFluidState(pos)); }
    @Override public boolean removeBlock(BlockPos pos, boolean moving) { return setBlock(pos, getFluidState(pos).createLegacyBlock(), 3); }
    @Override public boolean destroyBlock(BlockPos pos, boolean drops, Entity entity, int recursion) {
        // Tree and fungus generation only clears replaceable vegetation; no loot may escape a rejected attempt.
        return setBlock(pos, getFluidState(pos).createLegacyBlock(), 3, recursion);
    }
    @Override public boolean addFreshEntity(Entity entity) { effects.add(() -> level.addFreshEntity(entity)); return true; }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (!blocks.containsKey(pos)) return level.getBlockEntity(pos);
        if (!(getBlockState(pos).getBlock() instanceof EntityBlock block)) return null;
        return blockEntities.computeIfAbsent(pos.immutable(), p -> {
            BlockEntity entity = block.newBlockEntity(p, getBlockState(p));
            if (entity != null) entity.setLevel(level.getLevel());
            return entity;
        });
    }

    private <T> LevelTickAccess<T> deferTicks(LevelTickAccess<T> target) {
        return new LevelTickAccess<>() {
            private final List<ScheduledTick<T>> pending = new ArrayList<>();
            @Override public void schedule(ScheduledTick<T> tick) { pending.add(tick); effects.add(() -> target.schedule(tick)); }
            @Override public boolean hasScheduledTick(BlockPos pos, T type) {
                return pending.stream().anyMatch(tick -> tick.pos().equals(pos) && tick.type() == type) || target.hasScheduledTick(pos, type);
            }
            @Override public boolean willTickThisTick(BlockPos pos, T type) { return target.willTickThisTick(pos, type); }
            @Override public int count() { return target.count() + pending.size(); }
        };
    }

    @Override public void playSound(Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        BlockPos immutable = pos.immutable();
        effects.add(() -> level.playSound(player, immutable, sound, source, volume, pitch));
    }
    @Override public void addParticle(ParticleOptions particle, double x, double y, double z, double vx, double vy, double vz) {
        effects.add(() -> level.addParticle(particle, x, y, z, vx, vy, vz));
    }
    @Override public void levelEvent(Player player, int event, BlockPos pos, int data) {
        BlockPos immutable = pos.immutable();
        effects.add(() -> level.levelEvent(player, event, immutable, data));
    }
    @Override public void gameEvent(GameEvent event, Vec3 pos, GameEvent.Context context) {
        effects.add(() -> level.gameEvent(event, pos, context));
    }
    @Override public long getSeed() { return level.getSeed(); }
    @Override public boolean ensureCanWrite(BlockPos pos) { return level.ensureCanWrite(pos); }
    @Override public ServerLevel getLevel() { return level.getLevel(); }
    @Override public long nextSubTickCount() { return level.nextSubTickCount(); }
    @Override public LevelTickAccess<Block> getBlockTicks() { return blockTicks; }
    @Override public LevelTickAccess<Fluid> getFluidTicks() { return fluidTicks; }
    @Override public LevelData getLevelData() { return level.getLevelData(); }
    @Override public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) { return level.getCurrentDifficultyAt(pos); }
    @Override public MinecraftServer getServer() { return level.getServer(); }
    @Override public ChunkSource getChunkSource() { return level.getChunkSource(); }
    @Override public RandomSource getRandom() { return level.getRandom(); }
    @Override public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) { return level.getChunk(x, z, status, create); }
    @Override public boolean hasChunk(int x, int z) { return level.hasChunk(x, z); }
    @Override public int getHeight(Heightmap.Types type, int x, int z) { return level.getHeight(type, x, z); }
    @Override public int getSkyDarken() { return level.getSkyDarken(); }
    @Override public BiomeManager getBiomeManager() { return level.getBiomeManager(); }
    @Override public Holder<Biome> getBiome(BlockPos pos) { return level.getBiome(pos); }
    @Override public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) { return level.getUncachedNoiseBiome(x, y, z); }
    @Override public boolean isClientSide() { return level.isClientSide(); }
    @Override public int getSeaLevel() { return level.getSeaLevel(); }
    @Override public DimensionType dimensionType() { return level.dimensionType(); }
    @Override public RegistryAccess registryAccess() { return level.registryAccess(); }
    @Override public FeatureFlagSet enabledFeatures() { return level.enabledFeatures(); }
    @Override public float getShade(Direction direction, boolean shade) { return level.getShade(direction, shade); }
    @Override public LevelLightEngine getLightEngine() { return level.getLightEngine(); }
    @Override public WorldBorder getWorldBorder() { return level.getWorldBorder(); }
    @Override public List<Entity> getEntities(Entity entity, AABB bounds, Predicate<? super Entity> filter) { return level.getEntities(entity, bounds, filter); }
    @Override public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bounds, Predicate<? super T> filter) {
        return level.getEntities(type, bounds, filter);
    }
    @Override public List<? extends Player> players() { return level.players(); }
}
