package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.BossStructureProtection;
import net.amurdza.examplemod.event_handlers.NetherCitadelProtection;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.worldgen.BossStructureSpawnRules;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.AuthoredRewardLocks;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelLayout;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelPiece;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Checks the authored voxels, real registry rules, saved identity, and encounter protections. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherCitadelTests {
    private static final String[] BIOMES = {"basalt_deltas", "crimson_forest", "warped_forest", "soul_sand_valley"};
    private static final int R = NetherCitadelLayout.RADIUS, WIDTH = R * 2 + 1;
    private static final int HEIGHT = NetherCitadelLayout.MAX_Y - NetherCitadelLayout.MIN_Y + 1;

    private NetherCitadelTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeOreSectionWritesCannotReplaceCitadelSubstrates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var origin = new BlockPos(97000, NetherCitadelLayout.MIN_Y, 97000);
        var id = UUID.randomUUID();
        NetherCitadelProgress.register(level, id, origin);
        var host = Blocks.BLACKSTONE.defaultBlockState();
        var target = net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.target(
                new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(Blocks.BLACKSTONE),
                Blocks.ANCIENT_DEBRIS.defaultBlockState());
        var config = new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(List.of(target), 8, 0);
        java.util.function.Predicate<BlockPos> nativeAttempt = pos -> {
            var context = new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(),
                    level, level.getChunkSource().getGenerator(), RandomSource.create(137), pos, config);
            return net.amurdza.examplemod.worldgen.OrePlacementContext.runVein(context, () ->
                    net.minecraft.world.level.levelgen.feature.OreFeature.canPlaceOre(host, ignored -> host,
                            context.random(), config, target, pos.mutable()));
        };
        try {
            for (var pos : List.of(origin.atY(14), origin.offset(R, 10, R), origin.offset(-R, 10, -R)))
                helper.assertFalse(nativeAttempt.test(pos),
                        "The real native ore predicate must reject matching blackstone anywhere inside the citadel: " + pos);
            helper.assertTrue(nativeAttempt.test(origin.east(R + 1).atY(14)),
                    "A matching blackstone host immediately outside the boundary must remain eligible for ore generation");
            helper.assertTrue(nativeAttempt.test(origin.atY(NetherCitadelLayout.MIN_Y - 1)),
                    "The protection must not extend beneath the authored vertical envelope");
            helper.assertTrue(net.amurdza.examplemod.worldgen.OrePlacementContext.level() == null
                            && net.amurdza.examplemod.worldgen.OrePlacementContext.skipAirCheck() == null,
                    "The real ore invocation must release both thread-local scopes after protected and unprotected attempts");
        } finally {
            BossStructureBounds.get(level).remove(NetherCitadelProgress.KIND + ":" + origin.asLong());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeMagmaDeathsDoNotSplitAuthoredGuardsAndOrdinaryCubesStillSplit(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (boolean authored : new boolean[]{true, false}) {
            var cube = EntityType.MAGMA_CUBE.create(level);
            cube.setSize(2, true);
            cube.moveTo(Vec3.atBottomCenterOf(pos));
            if (authored) AuthoredEncounterProgress.markDefender(cube, UUID.randomUUID(), pos, 321,
                    NetherCitadelProgress.KIND, 0);
            var childSpawns = new java.util.concurrent.atomic.AtomicInteger();
            java.util.function.Consumer<EntityJoinLevelEvent> children = event -> {
                if (event.getEntity() instanceof MagmaCube && event.getLevel() == level) {
                    childSpawns.incrementAndGet();
                    // Inspect the native split without leaving test children in another test's fixture.
                    event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(children);
            try {
                cube.setHealth(0);
                cube.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
                helper.assertTrue(cube.isRemoved() && cube.getSize() == 2,
                        "Suppress only the split branch, preserving the authored guard's actual combat size");
                helper.assertTrue(authored ? childSpawns.get() == 0 : childSpawns.get() >= 2 && childSpawns.get() <= 4,
                        "Authored guards must not create uncounted children; ordinary magma cubes retain native splitting");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(children);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void residentInitializationKeepsAdultGuardsAndCannotCreateChickenJockeys(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var chickenSpawns = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<EntityJoinLevelEvent> chickens = event -> {
            if (event.getEntity().getType() == EntityType.CHICKEN && event.getLevel() == level) {
                chickenSpawns.incrementAndGet();
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(chickens);
        try {
            for (int attempt = 0; attempt < 32; attempt++) {
                var piglin = EntityType.ZOMBIFIED_PIGLIN.create(level);
                piglin.moveTo(Vec3.atBottomCenterOf(pos));
                piglin.setBaby(true);
                NetherCitadelProgress.finalizeResident(level, piglin, pos);
                helper.assertFalse(piglin.isBaby() || piglin.isPassenger(), "Authored zombie guards must be adult and unmounted");
                var hoglin = EntityType.HOGLIN.create(level);
                hoglin.moveTo(Vec3.atBottomCenterOf(pos));
                hoglin.setBaby(true);
                NetherCitadelProgress.finalizeResident(level, hoglin, pos);
                helper.assertFalse(hoglin.isBaby(), "Random native juvenile selection must not change the authored hoglin roster");
                var zoglin = EntityType.ZOGLIN.create(level);
                zoglin.moveTo(Vec3.atBottomCenterOf(pos));
                zoglin.setBaby(true);
                NetherCitadelProgress.finalizeResident(level, zoglin, pos);
                helper.assertFalse(zoglin.isBaby(), "Authored zoglins must be adult");
            }
            helper.assertTrue(chickenSpawns.get() == 0, "Finalizing residents must not add any chicken jockey mounts");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(chickens);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void nativeCreeperDetonationCompletesItsSlotEvenWhenTheBlastIsCancelled(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 2, 2));
        var progress = AuthoredEncounterProgress.get(level);
        for (boolean cancelBlast : new boolean[]{false, true}) {
            var id = UUID.randomUUID();
            progress.register(id, origin, 321, NetherCitadelProgress.KIND);
            for (int index = 0; index < 320; index++) {
                var defender = EntityType.SKELETON.create(level);
                AuthoredEncounterProgress.markDefender(defender, id, origin, 321, NetherCitadelProgress.KIND, index);
                progress.defeat(defender);
            }
            var creeper = EntityType.CREEPER.create(level);
            creeper.moveTo(Vec3.atBottomCenterOf(origin));
            creeper.setNoAi(true);
            var data = new CompoundTag();
            creeper.addAdditionalSaveData(data);
            data.putShort("Fuse", (short) 1);
            data.putByte("ExplosionRadius", (byte) 0);
            creeper.readAdditionalSaveData(data);
            AuthoredEncounterProgress.markDefender(creeper, id, origin, 321, NetherCitadelProgress.KIND, 320);
            progress.track(creeper);
            var completionEvents = new java.util.concurrent.atomic.AtomicInteger();
            var blastEvents = new java.util.concurrent.atomic.AtomicInteger();
            java.util.function.Consumer<AuthoredEncounterProgress.Cleared> complete = event -> {
                if (event.id().equals(id)) completionEvents.incrementAndGet();
            };
            java.util.function.Consumer<ExplosionEvent.Start> blast = event -> {
                if (event.getExplosion().getDirectSourceEntity() == creeper) {
                    blastEvents.incrementAndGet();
                    if (cancelBlast) event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(complete);
            MinecraftForge.EVENT_BUS.addListener(blast);
            try {
                creeper.ignite();
                creeper.tick();
                helper.assertTrue(blastEvents.get() == 1 && creeper.isRemoved(),
                        "Exercise the actual native detonation/discard path, including Forge cancellation=" + cancelBlast);
                helper.assertTrue(progress.isCleared(id) && progress.defeatedCount(id) == 321 && completionEvents.get() == 1,
                        "A detonated final Creeper must unlock the encounter and broadcast completion exactly once");
                helper.assertFalse(progress.defeat(creeper), "A repeated death notification cannot count the detonated Creeper twice");
                helper.assertTrue(completionEvents.get() == 1, "Duplicate defeat processing cannot broadcast a second completion");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(complete);
                MinecraftForge.EVENT_BUS.unregister(blast);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void existingWitherStructureIdentifierCreatesTheWholeFourLayerCitadel(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation("aoemod:wither_cave"));
        helper.assertTrue(structure != null, "The existing Wither locator identifier must remain registered");
        var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
                level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                        .get(new ResourceLocation("aoemod:wither_caves")).placement();
        int accepted = 0;
        search: for (int rx = -4; rx <= 4; rx++) for (int rz = -4; rz <= 4; rz++) {
            var chunk = placement.getPotentialStructureChunk(level.getSeed(), rx * placement.spacing(), rz * placement.spacing());
            var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), random,
                    level.getStructureManager(), level.getSeed(), chunk, level, structure.biomes()::contains);
            var candidate = structure.findValidGenerationPoint(context);
            if (candidate.isEmpty()) continue;
            var pieces = candidate.orElseThrow().getPiecesBuilder().build().pieces();
            helper.assertTrue(pieces.size() == 1 && pieces.get(0) instanceof NetherCitadelPiece,
                    "New Wither structures must use the citadel, retaining the old cave loader only for saved starts");
            var box = pieces.get(0).getBoundingBox();
            helper.assertTrue(box.minY() == NetherCitadelLayout.MIN_Y && box.maxY() == NetherCitadelLayout.MAX_Y
                            && box.minX() >= chunk.getMiddleBlockX() - 112 && box.maxX() <= chunk.getMiddleBlockX() + 112
                            && box.minZ() >= chunk.getMiddleBlockZ() - 112 && box.maxZ() <= chunk.getMiddleBlockZ() + 112,
                    "A complete fortress must remain inside the Nether height and eight-chunk structure-reference reach");
            for (int y : new int[]{16, 48, 80, 112})
                helper.assertTrue(structure.biomes().contains(generator.getBiomeSource().getNoiseBiome(
                                chunk.getMiddleBlockX() >> 2, y >> 2, chunk.getMiddleBlockZ() >> 2, random.sampler())),
                        "Structure configuration must admit every occupied Nether layer, including Y" + y);
            if (++accepted == 3) break search;
        }
        helper.assertTrue(accepted == 3, "At least three natural grid candidates must support an accessible four-layer fortress");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void everyAuthoredMaterialIsAllowedAtItsActualBiomeHeight(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        helper.assertTrue(nether != null, "The custom Nether must be registered");
        var view = layoutView(nether);
        Map<Integer, Map<BlockState, BlockPos>> paletteByHeight = new HashMap<>();
        int authored = 0;
        for (int y = NetherCitadelLayout.MIN_Y; y <= NetherCitadelLayout.MAX_Y; y++) {
            var palette = paletteByHeight.computeIfAbsent(y, ignored -> new IdentityHashMap<>());
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                var state = NetherCitadelLayout.stateAt(x, y, z);
                if (state == null || state.isAir()) continue;
                authored++;
                palette.putIfAbsent(state, new BlockPos(x, y, z));
            }
        }
        var rejected = new ArrayList<String>();
        var observed = new HashSet<Integer>();
        paletteByHeight.forEach((y, states) -> states.forEach((state, pos) -> {
            observed.add(Math.floorDiv(y, 32));
            if (!BiomePlacementRestrictions.isAllowed(view, pos, state))
                rejected.add(ForgeRegistries.BLOCKS.getKey(state.getBlock()) + " at Y" + y);
        }));
        helper.assertTrue(rejected.isEmpty(), "Citadel writes must survive the real placement guard: " + rejected);
        helper.assertTrue(observed.equals(Set.of(0, 1, 2, 3)), "Construction must occupy all four Nether layers");
        helper.assertTrue(authored > 50_000, "The fortress must have substantial built architecture");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void fixedRosterUsesLocalSpeciesAndHasSpaceForEveryActualEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var view = layoutView(level);
        var residents = NetherCitadelLayout.residents();
        helper.assertTrue(residents.size() == 321 && residents.size() == NetherCitadelProgress.DEFENDERS,
                "The fixed roster is 320 guards plus one Wither");
        var positions = new HashSet<BlockPos>();
        int bosses = 0;
        int[] layerCounts = new int[4];
        List<Set<String>> layerSpecies = List.of(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        for (var resident : residents) {
            var pos = resident.pos();
            helper.assertTrue(positions.add(pos), "Two authored residents share a spawn: " + pos);
            int layer = Math.floorDiv(pos.getY(), 32);
            helper.assertTrue(layer >= 0 && layer < 4, "Resident outside the four Nether strata: " + pos);
            if (resident.id().equals("minecraft:wither")) {
                bosses++;
                helper.assertTrue(layer == 0, "The Wither must occupy the final basalt chamber");
            } else {
                layerCounts[layer]++;
                layerSpecies.get(layer).add(resident.id());
                var allowed = new HashSet<>(DocumentedMonsters.forBiome(BIOMES[layer]));
                DocumentedFauna.forBiome(BIOMES[layer]).forEach(species -> allowed.add(species.id().toString()));
                helper.assertTrue(allowed.contains(resident.id()), "Nonlocal resident " + resident.id() + " in " + BIOMES[layer]);
            }
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(resident.id()));
            helper.assertTrue(type != null && type.create(level) instanceof Mob, "Missing resident type " + resident.id());
            var mob = (Mob) type.create(level);
            if (mob instanceof MagmaCube cube) cube.setSize(2, true);
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            var below = NetherCitadelLayout.stateAt(pos.getX(), pos.getY() - 1, pos.getZ());
            helper.assertTrue(below != null && below.isFaceSturdy(view, pos.below(), Direction.UP),
                    "Resident requires a solid spawn support: " + resident.id() + " " + pos);
            var body = mob.getBoundingBox();
            var bodyShape = Shapes.create(body);
            for (int x = (int) Math.floor(body.minX); x < Math.ceil(body.maxX); x++)
                for (int y = (int) Math.floor(body.minY); y < Math.ceil(body.maxY); y++)
                    for (int z = (int) Math.floor(body.minZ); z < Math.ceil(body.maxZ); z++) {
                        var block = new BlockPos(x, y, z);
                        var state = NetherCitadelLayout.stateAt(x, y, z);
                        helper.assertTrue(state != null && state.getFluidState().isEmpty(),
                                "Resident body leaves the dry authored volume: " + resident.id() + " " + block);
                        helper.assertFalse(Shapes.joinIsNotEmpty(bodyShape,
                                        state.getCollisionShape(view, block).move(x, y, z), BooleanOp.AND),
                                "Resident intersects construction: " + resident.id() + " " + block);
                    }
        }
        helper.assertTrue(bosses == 1, "Exactly one Wither belongs to the encounter");
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(layerCounts[i] == 80, "Each biome layer contributes 80 guards: " + BIOMES[i]);
            helper.assertTrue(layerSpecies.get(i).size() >= (i == 1 ? 1 : 2),
                    "Each layer with multiple local hostile species must have a mixed garrison: " + BIOMES[i]);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void ordinaryWalkingConnectsTheEntranceToEveryGuardAndReward(GameTestHelper helper) {
        var view = layoutView(helper.getLevel());
        byte[] cells = new byte[WIDTH * WIDTH * HEIGHT];
        var collision = new IdentityHashMap<BlockState, Boolean>();
        for (int y = NetherCitadelLayout.MIN_Y; y <= NetherCitadelLayout.MAX_Y; y++)
            for (int z = -R; z <= R; z++) for (int x = -R; x <= R; x++) {
                var state = NetherCitadelLayout.stateAt(x, y, z);
                if (state != null) cells[index(x, y, z)] = collision.computeIfAbsent(state,
                        value -> !value.getCollisionShape(view, BlockPos.ZERO).isEmpty()) ? (byte) 2 : (byte) 1;
            }
        byte[] reached = new byte[cells.length];
        int[] queue = new int[cells.length];
        var entrance = NetherCitadelLayout.entrance();
        helper.assertTrue(walkable(cells, entrance.getX(), entrance.getY(), entrance.getZ()),
                "The main entrance must admit an ordinary two-block-tall player: " + entrance);
        int first = index(entrance.getX(), entrance.getY(), entrance.getZ());
        reached[first] = 1;
        queue[0] = first;
        int head = 0, tail = 1;
        while (head < tail) {
            int current = queue[head++];
            int x = current % WIDTH - R;
            int z = current / WIDTH % WIDTH - R;
            int y = current / (WIDTH * WIDTH) + NetherCitadelLayout.MIN_Y;
            for (Direction direction : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                int nx = x + direction.getStepX(), ny = y + dy, nz = z + direction.getStepZ();
                if (!walkable(cells, nx, ny, nz)) continue;
                if (dy > 0 && cell(cells, x, y + 2, z) != 1) continue;
                if (dy < 0 && cell(cells, nx, y + 1, nz) != 1) continue;
                int next = index(nx, ny, nz);
                if (reached[next] != 0) continue;
                reached[next] = 1;
                queue[tail++] = next;
            }
        }
        for (var entry : NetherCitadelLayout.entrances())
            helper.assertTrue(nearReached(reached, entry, 1), "Disconnected exterior entry: " + entry);
        for (var resident : NetherCitadelLayout.residents())
            helper.assertTrue(nearReached(reached, resident.pos(), resident.id().equals("minecraft:ghast") ? 3 : 1),
                    "A defender has no walkable route from the entrance: " + resident.id() + " " + resident.pos());
        for (var chest : NetherCitadelLayout.chests()) {
            var front = chest.pos().relative(chest.facing());
            helper.assertTrue(nearReached(reached, front, 1), "A reward chest cannot be approached: " + chest.pos());
            var backing = NetherCitadelLayout.stateAt(chest.pos().getX() - chest.facing().getStepX(),
                    chest.pos().getY(), chest.pos().getZ() - chest.facing().getStepZ());
            helper.assertTrue(backing != null && !backing.isAir(), "Chests must be against architecture: " + chest.pos());
        }
        helper.assertTrue(tail > 88 * 88 * 5,
                "Connected usable floor area must exceed the five-storey 88-square mansion: " + tail);
        AOEMod.LOGGER.info("Nether citadel navigation: {} reachable standing cells, {} residents, {} chests",
                tail, NetherCitadelLayout.residents().size(), NetherCitadelLayout.chests().size());
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedIdentityAndDistinctDeathsNeverRefillTheRoster(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(93000, NetherCitadelLayout.MIN_Y, 93000);
        var piece = new NetherCitadelPiece(origin, 193);
        var context = StructurePieceSerializationContext.fromLevel(level);
        var restored = new NetherCitadelPiece(context, piece.createTag(context));
        helper.assertTrue(restored.origin().equals(piece.origin()) && restored.encounterId().equals(piece.encounterId())
                        && restored.getBoundingBox().equals(piece.getBoundingBox()),
                "Piece identity and complete four-layer envelope must survive serialization");
        var progress = new AuthoredEncounterProgress();
        var id = piece.encounterId();
        progress.register(id, origin, 321, NetherCitadelProgress.KIND);
        progress.queueMissingDefenders(id);
        helper.assertTrue(progress.pendingDefenders(id).cardinality() == 321, "A fresh encounter queues exactly its fixed roster");
        Mob finalDefender = null;
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 321; i++) {
            var mob = EntityType.WITHER_SKELETON.create(level);
            mob.setUUID(NetherCitadelProgress.entityId(id, i));
            helper.assertTrue(ids.add(mob.getUUID()), "Every authored slot has a distinct deterministic UUID");
            AuthoredEncounterProgress.markDefender(mob, id, origin, 321, NetherCitadelProgress.KIND, i);
            progress.track(mob);
            helper.assertFalse(progress.canPlaceDefender(id, i), "A tracked but unloaded resident must not respawn");
            if (i < 320) {
                helper.assertFalse(progress.defeat(mob), "Rewards cannot unlock before all 321 unique defenders die");
                helper.assertFalse(progress.defeat(mob), "Duplicate death notifications must not advance the roster");
            } else finalDefender = mob;
        }
        var reloaded = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        reloaded.queueMissingDefenders(id);
        helper.assertTrue(!reloaded.isCleared(id) && reloaded.defeatedCount(id) == 320
                        && reloaded.pendingDefenders(id).isEmpty(),
                "Reload preserves 320 deaths and the last unloaded live defender without replacements");
        helper.assertTrue(reloaded.defeat(finalDefender) && reloaded.isCleared(id), "Only the final distinct death unlocks rewards");
        reloaded.queueMissingDefenders(id);
        for (int i = 0; i < 321; i++) helper.assertFalse(reloaded.canPlaceDefender(id, i), "Victory cannot replenish slot " + i);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void pieceWritesOnlyItsClipAndSavedChunksNeverRebuildOrRefillLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(96008, NetherCitadelLayout.MIN_Y, 96008);
        var piece = new NetherCitadelPiece(origin, 7193);
        var authoredChest = NetherCitadelLayout.chests().get(0);
        var chestPos = new BlockPos(origin.getX() + authoredChest.pos().getX(), authoredChest.pos().getY(),
                origin.getZ() + authoredChest.pos().getZ());
        var chunk = new ChunkPos(chestPos);
        var fullClip = new BoundingBox(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), 127, chunk.getMaxBlockZ());
        var slice = new BoundingBox(chunk.getMinBlockX(), chestPos.getY(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), chestPos.getY(), chunk.getMaxBlockZ());
        var states = new HashMap<BlockPos, BlockState>();
        var chests = new HashMap<BlockPos, ChestBlockEntity>();
        final BoundingBox[] activeClip = {slice};
        final int[] writes = {0};
        var view = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "setBlock" -> {
                        var pos = ((BlockPos) args[0]).immutable();
                        var state = (BlockState) args[1];
                        helper.assertTrue(activeClip[0].isInside(pos), "Piece wrote outside its requested clip: " + pos);
                        states.put(pos, state);
                        writes[0]++;
                        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
                            var chest = new ChestBlockEntity(pos, state);
                            chest.setLevel(level);
                            chests.put(pos, chest);
                        } else chests.remove(pos);
                        yield true;
                    }
                    case "getBlockState" -> states.getOrDefault((BlockPos) args[0], Blocks.AIR.defaultBlockState());
                    case "getBlockEntity" -> chests.get((BlockPos) args[0]);
                    default -> throw new UnsupportedOperationException("Unexpected chunk writer query: " + method.getName());
                });
        var generator = level.getChunkSource().getGenerator();
        piece.postProcess(view, level.structureManager(), generator, RandomSource.create(3), slice, chunk, origin);
        int partialWrites = writes[0];
        activeClip[0] = fullClip;
        piece.postProcess(view, level.structureManager(), generator, RandomSource.create(3), fullClip, chunk, origin);
        helper.assertTrue(partialWrites > 0 && writes[0] > partialWrites,
                "A partial clip must not mark the whole chunk complete and leave permanent missing architecture");
        var inventory = chests.get(chestPos);
        helper.assertTrue(inventory != null, "The real writer must create its authored wall chest");
        var lootBefore = inventory.saveWithoutMetadata();
        helper.assertTrue(lootBefore.getString("LootTable").equals("aoemod:chests/bosses/nether_citadel"),
                "The wall chest must use the citadel's registered reward table");
        var context = StructurePieceSerializationContext.fromLevel(level);
        var reloaded = new NetherCitadelPiece(context, piece.createTag(context));
        int beforeReload = writes[0];
        reloaded.postProcess(view, level.structureManager(), generator, RandomSource.create(91), fullClip, chunk, origin);
        helper.assertTrue(writes[0] == beforeReload && inventory.saveWithoutMetadata().equals(lootBefore),
                "Reloading a completed chunk must neither rebuild harvested blocks nor reset the chest seed/content");
        BossStructureBounds.get(level).remove(NetherCitadelProgress.KIND + ":" + origin.asLong());
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rewardAutomationAndSixFaceBoundariesShareTheSameEncounter(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var origin = new BlockPos(94000, NetherCitadelLayout.MIN_Y, 94000);
        var id = UUID.randomUUID();
        var home = origin.offset(0, 5, 0);
        var chest = new ChestBlockEntity(home, Blocks.CHEST.defaultBlockState());
        chest.setLevel(level);
        chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 7));
        var encounter = NetherCitadelProgress.register(level, id, origin);
        var boundary = NetherCitadelProgress.boundary(encounter);
        helper.assertTrue(AuthoredRewardLocks.chestLocked(chest) && NetherCitadelProtection.rewardsLocked(level, home),
                "Inventory automation and player interactions must share the encounter reward lock");
        helper.assertTrue(chest.getItem(0).isEmpty() && chest.removeItem(0, 3).isEmpty(),
                "The actual chest inventory mixin must hide rewards from automated reads and extraction");
        var resident = EntityType.WITHER_SKELETON.create(level);
        resident.moveTo(Vec3.atBottomCenterOf(home));
        StructureSpawnedMobData.mark(resident, home);
        AuthoredEncounterProgress.markDefender(resident, id, origin, 321, NetherCitadelProgress.KIND, 0);
        var ally = EntityType.BLAZE.create(level);
        AuthoredEncounterProgress.markDefender(ally, id, origin, 321, NetherCitadelProgress.KIND, 1);
        var friendlyAttack = new LivingAttackEvent(resident, level.damageSources().mobAttack(ally), 4);
        NetherCitadelProtection.friendlyFire(friendlyAttack);
        helper.assertTrue(friendlyAttack.isCanceled(), "The mixed authored garrison must not kill its own encounter partners");
        helper.assertTrue(BossStructureSpawnRules.blocked(level, home, EntityType.ZOMBIE),
                "Ambient spawns cannot add extra enemies inside the fixed encounter");
        var move = BossStructureProtection.restrictMovement(resident, new Vec3(-1000, 1000, -1000));
        var finalBox = resident.getBoundingBox().move(move);
        helper.assertTrue(finalBox.minX >= boundary.minX && finalBox.minZ >= boundary.minZ && finalBox.maxY <= boundary.maxY,
                "An authored defender cannot leave through corners or the roof");
        var outsider = EntityType.ZOMBIE.create(level);
        outsider.moveTo(boundary.minX - 3, home.getY(), home.getZ());
        var incoming = BossStructureProtection.restrictMovement(outsider, new Vec3(8, 0, 0));
        helper.assertFalse(outsider.getBoundingBox().move(incoming).intersects(boundary), "Outsiders cannot enter the citadel");
        var progress = AuthoredEncounterProgress.get(level);
        for (int i = 0; i < 321; i++) {
            var defender = i == 0 ? resident : EntityType.WITHER_SKELETON.create(level);
            AuthoredEncounterProgress.markDefender(defender, id, origin, 321, NetherCitadelProgress.KIND, i);
            progress.defeat(defender);
        }
        helper.assertFalse(AuthoredRewardLocks.chestLocked(chest) || NetherCitadelProtection.rewardsLocked(level, home),
                "The final defender releases chests through both access paths");
        helper.assertTrue(chest.getItem(0).is(Items.GOLD_INGOT) && chest.getItem(0).getCount() == 7,
                "Blocked automated extraction must preserve the reward for victory");
        helper.assertTrue(BossStructureSpawnRules.blocked(level, home, EntityType.ZOMBIE),
                "Victory must not reopen the citadel to ambient respawning");
        BossStructureBounds.get(level).remove(NetherCitadelProgress.KIND + ":" + origin.asLong());
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void realConstructionStaysProtectedWhileNetherCropsAndMessagesUnlock(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var origin = unoccupiedFixture(level);
        var masonry = origin.atY(103);
        var crop = masonry.east(2);
        level.getChunk(masonry);
        level.setBlock(masonry, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 18);
        level.setBlock(crop.below(), Blocks.SOUL_SAND.defaultBlockState(), 18);
        level.setBlock(crop, Blocks.NETHER_WART.defaultBlockState(), 18);
        helper.assertTrue(level.getBlockState(crop).is(Blocks.NETHER_WART), "The actual Nether crop fixture must survive biome placement rules");
        var nativeCrops = new ArrayList<BlockPos>();
        int nativeIndex = 0;
        for (String name : List.of("powdery_cane", "powdery_cannon", "bullet_pepper")) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight", name));
            helper.assertTrue(block != null && block != Blocks.AIR, "Missing authored Nether garden plant: " + name);
            for (var state : block.getStateDefinition().getPossibleStates())
                helper.assertTrue(AuthoredRewardLocks.isRewardBlock(state), "Every native cane section/stage must be a reward crop: " + state);
            var pos = origin.offset(-4 + nativeIndex++ * 2, 0, 4).atY(22);
            level.setBlock(pos.below(), Blocks.BASALT.defaultBlockState(), 18);
            if (name.equals("bullet_pepper")) {
                level.setBlock(pos.below(2), Blocks.BASALT.defaultBlockState(), 18);
                level.setBlock(pos.below(), ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight:powdery_cane"))
                        .defaultBlockState(), 18);
            }
            level.setBlock(pos, block.defaultBlockState(), 18);
            helper.assertTrue(level.getBlockState(pos).is(block), "The native reward fixture must be placed in the basalt habitat: " + name);
            nativeCrops.add(pos);
        }
        var fungi = new ArrayList<BlockPos>();
        for (boolean warped : new boolean[]{false, true}) {
            var fungus = warped ? Blocks.WARPED_FUNGUS : Blocks.CRIMSON_FUNGUS;
            var soil = warped ? Blocks.WARPED_NYLIUM : Blocks.CRIMSON_NYLIUM;
            var pos = origin.offset(warped ? -2 : -4, 0, 6).atY(warped ? 79 : 47);
            level.setBlock(pos.below(), soil.defaultBlockState(), 18);
            level.setBlock(pos, fungus.defaultBlockState(), 18);
            helper.assertTrue(level.getBlockState(pos).is(fungus),
                    "The cultivated fungus fixture must survive in its documented forest habitat: " + fungus);
            nativeCrops.add(pos);
            fungi.add(pos);
        }
        var id = UUID.randomUUID();
        NetherCitadelProgress.register(level, id, origin);
        helper.assertTrue(NetherCitadelProgress.at(level, crop).id().equals(id),
                "The interaction fixture must query its own encounter, without an overlapping natural citadel");
        helper.assertTrue(AuthoredRewardLocks.protectedBlock(level, masonry) && AuthoredRewardLocks.protectedBlock(level, crop),
                "The low-level destruction guard must protect both construction and uncleared crops");
        for (var pos : nativeCrops) helper.assertTrue(AuthoredRewardLocks.protectedBlock(level, pos)
                        && !level.destroyBlock(pos, false),
                "Cane stems, cannons, pepper caps and cultivated fungi must remain locked before victory: " + pos);
        var clientProtection = new CompoundTag();
        var fixtureChunk = level.getChunkAt(masonry);
        net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.write(fixtureChunk, clientProtection);
        net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.read(fixtureChunk, clientProtection);
        for (var pos : fungi) {
            helper.assertTrue(net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.protects(level, pos),
                    "Chunk snapshots must prevent ghost harvesting of locked cultivated fungi");
            var message = NetherCitadelProtection.breakingMessage(level, pos).orElseThrow();
            helper.assertTrue(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                            && text.getKey().equals("message.aoemod.nether_citadel_rewards_locked"),
                    "Cultivated fungi are locked crop rewards, not permanently protected decoration");
        }
        helper.assertTrue(NetherCitadelProtection.remainingEnemies(level, crop) == 321,
                "Locked rewards must report the full remaining roster initially");
        var constructionMessage = NetherCitadelProtection.breakingMessage(level, masonry).orElseThrow();
        helper.assertTrue(constructionMessage.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                        && contents.getKey().equals("message.aoemod.nether_citadel_protected"),
                "Construction gets the permanent-building message instead of an enemy-unlock message");
        var progress = AuthoredEncounterProgress.get(level);
        for (int i = 0; i < 321; i++) {
            var defender = EntityType.SKELETON.create(level);
            AuthoredEncounterProgress.markDefender(defender, id, origin, 321, NetherCitadelProgress.KIND, i);
            progress.defeat(defender);
            if (i == 319) helper.assertTrue(NetherCitadelProtection.remainingEnemies(level, crop) == 1
                            && NetherCitadelProtection.rewardsLocked(level, crop),
                    "A single living or unloaded defender must keep the crop locked");
        }
        helper.assertTrue(AuthoredRewardLocks.protectedBlock(level, masonry)
                        && !AuthoredRewardLocks.protectedBlock(level, crop)
                        && NetherCitadelProtection.breakingMessage(level, crop).isEmpty(),
                "Victory permits crop harvesting without an obsolete warning, while masonry remains protected");
        net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.write(fixtureChunk, clientProtection);
        net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.read(fixtureChunk, clientProtection);
        for (var pos : fungi) helper.assertTrue(
                !net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.protects(level, pos)
                        && net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot.rewardsUnlocked(level, pos),
                "The refreshed chunk snapshot must release cultivated fungi after the final defender");
        for (var pos : nativeCrops) helper.assertTrue(!AuthoredRewardLocks.protectedBlock(level, pos)
                        && NetherCitadelProtection.breakingMessage(level, pos).isEmpty()
                        && level.destroyBlock(pos, false),
                "Victory must release every native powder-cane section and cultivated fungus without a stale warning: " + pos);
        helper.assertTrue(level.destroyBlock(crop, false), "The actual low-level destroyBlock hook must permit cleared crop harvesting");
        helper.assertFalse(level.destroyBlock(masonry, false), "The actual low-level destroyBlock hook must still reject construction harvesting");
        BossStructureBounds.get(level).remove(NetherCitadelProgress.KIND + ":" + origin.asLong());
        helper.succeed();
    }

    private static BlockPos unoccupiedFixture(ServerLevel level) {
        for (int attempt = 0; attempt < 16; attempt++) {
            var candidate = new BlockPos(94504 + attempt * 512, NetherCitadelLayout.MIN_Y, 94504);
            // Loading a Nether chunk can legitimately generate a natural citadel. Keep the
            // synthetic encounter's interaction probes outside every existing reservation.
            level.getChunk(candidate);
            var probes = new AABB(candidate.getX() - 8, NetherCitadelLayout.MIN_Y, candidate.getZ() - 8,
                    candidate.getX() + 9, NetherCitadelLayout.MAX_Y + 1, candidate.getZ() + 9);
            if (BossStructureBounds.get(level).nearby(probes).isEmpty()) return candidate;
        }
        throw new IllegalStateException("Could not find a Nether chunk outside natural boss reservations for the interaction fixture");
    }

    private static WorldGenLevel layoutView(ServerLevel level) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "getBlockState" -> stateOrAir((BlockPos) args[0]);
                    case "getFluidState" -> stateOrAir((BlockPos) args[0]).getFluidState();
                    case "getBiome" -> {
                        var pos = (BlockPos) args[0];
                        yield generator.getBiomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2,
                                pos.getZ() >> 2, random.sampler());
                    }
                    case "getMinBuildHeight" -> 0;
                    case "getMaxBuildHeight", "getHeight" -> 128;
                    case "registryAccess" -> level.registryAccess();
                    default -> throw new UnsupportedOperationException("Unexpected layout query: " + method.getName());
                });
    }

    private static BlockState stateOrAir(BlockPos pos) {
        var state = NetherCitadelLayout.stateAt(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    private static int index(int x, int y, int z) {
        return ((y - NetherCitadelLayout.MIN_Y) * WIDTH + z + R) * WIDTH + x + R;
    }

    private static int cell(byte[] cells, int x, int y, int z) {
        return x < -R || x > R || z < -R || z > R || y < NetherCitadelLayout.MIN_Y || y > NetherCitadelLayout.MAX_Y
                ? 0 : cells[index(x, y, z)];
    }

    private static boolean walkable(byte[] cells, int x, int y, int z) {
        return cell(cells, x, y - 1, z) == 2 && cell(cells, x, y, z) == 1 && cell(cells, x, y + 1, z) == 1;
    }

    private static boolean nearReached(byte[] reached, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
            for (int dy = -1; dy <= 1; dy++) if (cell(reached, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz) == 1) return true;
        return false;
    }
}
