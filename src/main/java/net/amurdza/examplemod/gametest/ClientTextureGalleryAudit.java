package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ArthropleuraEntity;
import net.amurdza.examplemod.portal.PortalDestination;
import net.amurdza.examplemod.portal.PortalFrame;
import net.amurdza.examplemod.portal.PortalTravel;
import net.amurdza.examplemod.portal.WoodenPortalBlock;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SnowyPlantSnowlogging;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.event.GeoRenderEvent;
import teamdraco.finsandstails.common.entities.RubberBellyGliderEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Opt-in world photography: real server-spawned, ticking animals and normal world block rendering. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTextureGalleryAudit {
    private static final String INPUT = System.getProperty("aoemod.gallery.views");
    private static final boolean BEFORE = Boolean.getBoolean("aoemod.gallery.before");
    private static final List<Map<String, Object>> CAPTURES = new ArrayList<>();
    private static final Set<String> BUILT_STAGES = new HashSet<>();
    private static final Set<UUID> UUID_RESOLVED = new HashSet<>();
    private static final int WATER_DEPTH = 20;
    private static final int STAGE_RADIUS = 16;
    private static JsonArray views;
    private static Path output;
    private static volatile boolean completed;
    private static int index, settledTicks, requestedIndex = -1;
    private static int chunkAcknowledgedTicks, spawnRequestedIndex = -1;
    private static volatile int readyIndex = -1;
    private static volatile String serverFailure;
    private static volatile String serverEntityState = "none";
    private static volatile Scene scene;
    private static Entity camera;
    private static long phaseStarted;
    private static long lastDiagnosticMillis;
    private static Path failureFrame;
    private static Path failureState;
    private static ServerLevel activeLevel;
    private static UUID activeMobId;

    private record Scene(String key, ResourceKey<Level> dimension, BlockPos anchor, int entityId,
                         UUID entityUuid, ResourceLocation entityType,
                         Vec3 target, double radius, boolean water, boolean benthic, long bornAtTick) { }

    private ClientTextureGalleryAudit() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (INPUT == null || completed || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            // A gallery launch is an unattended, disposable QA client. Do not let an accidental
            // Escape leave the integrated server paused until the per-view timeout expires.
            if (minecraft.screen instanceof PauseScreen && minecraft.getSingleplayerServer() != null) {
                AOEMod.LOGGER.info("World gallery resuming QA pause screen");
                minecraft.setScreen(null);
            }
            long now = System.currentTimeMillis();
            if (now - lastDiagnosticMillis >= 5_000) {
                lastDiagnosticMillis = now;
                AOEMod.LOGGER.info("World gallery heartbeat: {}", diagnosticState(minecraft));
            }
            if (minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null) return;
            if (views == null) initialize(minecraft);
            if (serverFailure != null) throw new IllegalStateException(serverFailure);
            if (index == views.size()) { finish(minecraft, null); return; }
            JsonObject view = views.get(index).getAsJsonObject();
            if (requestedIndex != index) { requestScene(minecraft, view); return; }
            if (System.currentTimeMillis() - phaseStarted > 120_000) throw new IllegalStateException("World view timed out: " + view.get("label"));
            Scene current = scene;
            if (readyIndex != index || current == null || !minecraft.level.dimension().equals(current.dimension())
                    || minecraft.screen != null || !minecraft.level.hasChunkAt(current.anchor())) return;
            if (current.entityId() == -2) {
                // Both entity tracking and block/frame validation follow the destination handshake.
                // Immersive Portals can otherwise send updates while the old client level is active.
                if (++chunkAcknowledgedTicks >= 20 && spawnRequestedIndex != index)
                    requestSpecimenPlacement(minecraft, view, current);
                return;
            }
            Entity subject = resolveSubject(minecraft, current);
            if (current.entityId() >= 0 && (!(subject instanceof LivingEntity living) || !living.isAlive())) return;
            if (current.water() && subject != null && !subject.isInWater()) { settledTicks = 0; return; }
            updateCamera(minecraft, view, current, subject);
            int minimum = view.has("settleTicks") ? view.get("settleTicks").getAsInt() : 50;
            // A real aquatic needs time to acquire swimming/fluid and animation state after spawning.
            if (subject != null && subject.tickCount < 90) return;
            if (++settledTicks < minimum) return;
            capture(minecraft, view, current, subject);
            index++;
            requestedIndex = -1;
            settledTicks = 0;
            writeReport("running", null);
        } catch (Throwable failure) {
            AOEMod.LOGGER.error("World texture gallery failed at {}", index, failure);
            finish(minecraft, failure.toString());
        }
    }

    private static void initialize(Minecraft minecraft) throws Exception {
        views = JsonParser.parseString(Files.readString(Path.of(INPUT))).getAsJsonArray();
        output = Path.of(System.getProperty("aoemod.gallery.output")).toAbsolutePath();
        Files.createDirectories(output);
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.hideGui = true;
        minecraft.options.bobView().set(false);
        minecraft.options.fov().set(50);
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.options.renderDistance().set(4);
        minecraft.options.simulationDistance().set(5);
        minecraft.options.entityDistanceScaling().set(2.0);
        if (BEFORE) { installOriginalTextures(minecraft); installOriginalArthropleura(minecraft); }
        else exportDerivedTextures(minecraft);
        writeReport("preparing_world", null);
    }

    private static ResourceKey<Level> dimension(JsonObject view) {
        if (view.has("dimension")) return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(view.get("dimension").getAsString()));
        String id = view.get("id").getAsString();
        if (id.equals("aoemod:nether_portal")) return GameplayDimensions.DESERT;
        if (id.equals("aoemod:overworld_portal")) return GameplayDimensions.PLAINS;
        if (!BEFORE && id.contains("azalea_leaves")) return GameplayDimensions.TROPICAL;
        return Level.OVERWORLD;
    }

    private static void requestScene(Minecraft minecraft, JsonObject view) throws Exception {
        requestedIndex = index;
        settledTicks = 0;
        chunkAcknowledgedTicks = 0;
        spawnRequestedIndex = -1;
        phaseStarted = System.currentTimeMillis();
        int requested = index;
        var dimension = dimension(view);
        String key = dimension.location() + "/" + view.get("id").getAsString()
                + "/" + view.get("properties") + "/" + view.get("variant") + "/" + snowy(view);
        var server = minecraft.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("World gallery requires its disposable integrated server");
        UUID playerId = minecraft.player.getUUID();
        server.execute(() -> {
            try {
                if (completed) return;
                var level = server.getLevel(dimension);
                var player = server.getPlayerList().getPlayer(playerId);
                if (level == null || player == null) throw new IllegalStateException("Missing gallery world/player");
                if (scene == null || !scene.key().equals(key)) {
                    if (activeLevel != null && activeMobId != null) {
                        Entity old = activeLevel.getEntity(activeMobId);
                        if (old != null) old.discard();
                    }
                    activeLevel = level;
                    activeMobId = null;
                    serverEntityState = "awaiting client destination/chunk acknowledgment";
                    boolean block = view.has("block") && view.get("block").getAsBoolean();
                    boolean water = !block && water(view);
                    BlockPos anchor = new BlockPos(block ? 104 : water ? 8 : 56, level.getMaxBuildHeight() - 47, 8);
                    scene = new Scene(key, level.dimension(), anchor, -2, null, null,
                            Vec3.atCenterOf(anchor), 1, water, false, level.getGameTime());
                }
                player.stopRiding();
                player.setGameMode(GameType.SPECTATOR);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 24000, 0, false, false, false));
                player.teleportTo(level, scene.anchor().getX() + .5, scene.anchor().getY() + 10,
                        scene.anchor().getZ() + 12.5, 180, 0);
                readyIndex = requested;
            } catch (Throwable failure) {
                serverFailure = "Preparing real world gallery view " + view.get("label") + ": " + failure;
                AOEMod.LOGGER.error("World gallery server setup failed", failure);
            }
        });
        writeReport("preparing_view", null);
    }

    private static void requestSpecimenPlacement(Minecraft minecraft, JsonObject view, Scene pending) {
        int requested = index;
        spawnRequestedIndex = requested;
        readyIndex = -1;
        var server = minecraft.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Gallery integrated server disappeared");
        server.execute(() -> {
            try {
                if (completed || scene != pending) return;
                ServerLevel level = server.getLevel(pending.dimension());
                if (level == null) throw new IllegalStateException("Missing acknowledged gallery dimension");
                boolean block = view.has("block") && view.get("block").getAsBoolean();
                prepareStage(level, pending.anchor(), block, pending.water());
                scene = block ? placeBlock(level, pending.anchor(), view, pending.key())
                        : spawnMob(level, pending.anchor(), view, pending.key(), pending.water());
                AOEMod.LOGGER.info("World gallery placed {} after client destination/chunk acknowledgment, entityId={}",
                        view.get("label"), scene.entityId());
                readyIndex = requested;
            } catch (Throwable failure) {
                serverFailure = "Placing acknowledged world gallery view " + view.get("label") + ": " + failure;
                AOEMod.LOGGER.error("World gallery acknowledged placement failed", failure);
            }
        });
    }

    private static void prepareStage(ServerLevel level, BlockPos anchor, boolean block, boolean water) {
        var server = level.getServer();
        server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        level.setDayTime(6000);
        level.setWeatherParameters(24000, 0, false, false);
        BlockState landFloor = galleryFloor(level, anchor.below());
        String stage = level.dimension().location() + "/" + anchor.getX();
        if (BUILT_STAGES.add(stage)) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = -STAGE_RADIUS; x <= STAGE_RADIUS; x++) {
                for (int z = -STAGE_RADIUS; z <= STAGE_RADIUS; z++) {
                    for (int y = -1; y <= WATER_DEPTH + 2; y++) {
                        pos.setWithOffset(anchor, x, y, z);
                        BlockState state;
                        if (y == -1) state = water ? (x % 5 == 0 && z % 5 == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_SANDSTONE)
                                .defaultBlockState() : landFloor;
                        else if (water && y < WATER_DEPTH) state = (Math.abs(x) == STAGE_RADIUS || Math.abs(z) == STAGE_RADIUS
                                ? Blocks.GLASS : Blocks.WATER).defaultBlockState();
                        else state = Blocks.AIR.defaultBlockState();
                        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
        if (block) {
            for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-4, 0, -4), anchor.offset(4, 7, 4)))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-4, -1, -4), anchor.offset(4, -1, 4)))
                level.setBlock(pos, landFloor, 18);
        }
        requirePlacedState(level, anchor.below(), water ? Blocks.SEA_LANTERN.defaultBlockState() : landFloor);
    }

    private static BlockState galleryFloor(ServerLevel level, BlockPos pos) {
        Block sourcePlanks = PortalTravel.arrivalPlanks(PortalDestination.fromDimension(level.dimension())
                .orElse(PortalDestination.OVERWORLD));
        for (Block block : List.of(Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.SMOOTH_SANDSTONE, sourcePlanks)) {
            BlockState candidate = block.defaultBlockState();
            if (BiomePlacementRestrictions.isAllowed(level, pos, candidate)) return candidate;
        }
        throw new IllegalStateException("No permitted gallery floor in " + level.dimension().location() + " at " + pos);
    }

    private static void requirePlacedState(ServerLevel level, BlockPos pos, BlockState expected) {
        BlockState actual = level.getBlockState(pos);
        if (!actual.equals(expected)) throw new IllegalStateException("Gallery block write rejected: dimension="
                + level.dimension().location() + ", position=" + pos.toShortString() + ", expected=" + expected
                + ", actual=" + actual + ", placementAllowed=" + BiomePlacementRestrictions.isAllowed(level, pos, expected)
                + ", hasChunk=" + level.hasChunkAt(pos) + ", inBorder=" + level.getWorldBorder().isWithinBounds(pos));
    }

    private static boolean water(JsonObject view) {
        if (view.has("habitat")) return view.get("habitat").getAsString().equals("water");
        String id = view.get("id").getAsString();
        return !id.startsWith("orcz:") && !id.startsWith("aoemod:") && !id.startsWith("outer_end:");
    }

    private static Scene spawnMob(ServerLevel level, BlockPos anchor, JsonObject view, String key, boolean water) {
        String id = view.get("id").getAsString();
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        if (type == null || !(type.create(level) instanceof Mob mob)) throw new IllegalArgumentException("Missing gallery mob: " + id);
        boolean benthic = id.contains("crab") || id.endsWith(":triops") || id.endsWith(":starfish");
        mob.moveTo(anchor.getX() + .5, anchor.getY() + (water && !benthic ? 7 : .05), anchor.getZ() + .5, 180, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);
        mob.setPersistenceRequired();
        mob.setInvulnerable(true);
        mob.setNoAi(!water);
        mob.setNoGravity(false);
        if (mob instanceof BaseWaterAnimal aquatic) {
            aquatic.setMoistness(aquatic.getMaxMoistness());
            aquatic.setCurrentRoll(0);
            aquatic.setPrevRoll(0);
            if (AquaticMobSizes.isTallCrab(aquatic)) aquatic.setSize(5);
        }
        if (mob instanceof RubberBellyGliderEntity glider) glider.setPuffed(false);
        if (mob instanceof StarfishEntity starfish) {
            starfish.setVariant(Type.valueOf(view.has("variant") ? view.get("variant").getAsString() : "MEDIUM"));
            starfish.setStarfishColor(0xE9A86F);
            starfish.setOverlayColor(0xAD552C);
        }
        if (id.equals("outer_end:stalker")) {
            CompoundTag tag = mob.saveWithoutId(new CompoundTag());
            tag.putString("color", BEFORE ? "rose" : "cobalt");
            mob.load(tag);
        }
        if (!level.addFreshEntity(mob)) throw new IllegalStateException("Gallery entity rejected: " + id);
        activeMobId = mob.getUUID();
        double radius = visualRadius(view, mob);
        return new Scene(key, level.dimension(), anchor, mob.getId(), mob.getUUID(), new ResourceLocation(id),
                mob.position(), radius, water, benthic, level.getGameTime());
    }

    private static double visualRadius(JsonObject view, LivingEntity mob) {
        if (view.has("visualRadius")) return view.get("visualRadius").getAsDouble();
        String id = view.get("id").getAsString();
        double radius = switch (id) {
            case "aoemod:arthropleura" -> 2.5;
            case "aoemod:spider_queen" -> 2.9;
            case "orcz:bigorcboss" -> 2.7;
            case "orcz:orcsavagemace", "orcz:orcshield" -> 2.0;
            case "outer_end:stalker" -> 2.4;
            case "hybrid_aquatic:flower_crab", "hybrid_aquatic:lightfoot_crab" -> BEFORE ? .9 : 1.8;
            case "hybrid_aquatic:spider_crab" -> 1.6;
            case "hybrid_aquatic:fiddler_crab", "alexsmobs:triops" -> .65;
            case "hybrid_aquatic:starfish" -> BEFORE && view.has("variant") && view.get("variant").getAsString().equals("SMALL") ? .35 : .7;
            case "hybrid_aquatic:arrow_squid", "hybrid_aquatic:tuna" -> 2.1;
            case "hybrid_aquatic:octopus", "finsandtails:rubber_belly_glider" -> 1.6;
            case "hybrid_aquatic:umbrella_octopus" -> .9;
            case "hybrid_aquatic:vampire_squid", "angling:anomalocaris" -> 1.2;
            case "angling:mahi_mahi", "ambientadditions:blue_spotted_stingray" -> 1.5;
            default -> Math.max(.8, Math.max(mob.getBbWidth(), mob.getBbHeight()) * .9);
        };
        return radius;
    }

    @SubscribeEvent
    public static void keepSwimming(TickEvent.ServerTickEvent event) {
        if (INPUT == null || completed || event.phase != TickEvent.Phase.END || activeLevel == null || activeMobId == null) return;
        Scene current = scene;
        Entity active = activeLevel.getEntity(activeMobId);
        if (activeLevel.getGameTime() % 100 == 0) serverEntityState = active == null ? "missing UUID " + activeMobId
                : "id=" + active.getId() + ", age=" + active.tickCount + ", alive=" + active.isAlive()
                + ", inWater=" + active.isInWater() + ", position=" + active.position();
        if (current == null || !(active instanceof Mob mob)) return;
        if (mob instanceof RubberBellyGliderEntity glider && glider.isPuffed()) glider.setPuffed(false);
        if (!current.water() || current.benthic() || activeLevel.getGameTime() % 30 != 0) return;
        double phase = activeLevel.getGameTime() * .045;
        mob.getNavigation().moveTo(current.anchor().getX() + .5 + Math.sin(phase) * 4,
                current.anchor().getY() + 7 + Math.sin(phase * .5),
                current.anchor().getZ() + .5 + Math.cos(phase) * 4, 1.0);
    }

    @SubscribeEvent
    public static void nativeBeforeCrabScale(GeoRenderEvent.Entity.Pre event) {
        if (INPUT == null || !BEFORE || !AquaticMobSizes.isTallCrab(event.getEntity())) return;
        float inverse = 1 / AquaticMobSizes.tallCrabScale(event.getEntity());
        event.getPoseStack().scale(inverse, inverse, inverse);
    }

    private static Scene placeBlock(ServerLevel level, BlockPos anchor, JsonObject view, String key) {
        String id = view.get("id").getAsString();
        var type = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (type == null || type == Blocks.AIR) throw new IllegalArgumentException("Missing gallery block: " + id);
        BlockState state = type.defaultBlockState();
        if (view.has("properties")) for (var entry : view.getAsJsonObject("properties").entrySet()) {
            Property<?> property = type.getStateDefinition().getProperty(entry.getKey());
            if (property == null) throw new IllegalArgumentException("Missing block property: " + entry.getKey());
            state = setProperty(state, property, entry.getValue().getAsString());
        }
        if (state.hasProperty(BlockStateProperties.PERSISTENT)) state = state.setValue(BlockStateProperties.PERSISTENT, true);
        Vec3 target = Vec3.atCenterOf(anchor);
        double radius = .9;
        if (id.endsWith("_portal")) {
            BlockPos left = anchor.west();
            boolean wooden = type instanceof WoodenPortalBlock;
            var frame = new PortalFrame(left, Direction.Axis.X, 2, 3);
            BlockState frameState = (wooden ? PortalTravel.arrivalPlanks(PortalDestination.fromDimension(level.dimension())
                    .orElse(PortalDestination.OVERWORLD)) : Blocks.OBSIDIAN).defaultBlockState();
            for (int x = -1; x <= 2; x++) for (int y = -1; y <= 3; y++) {
                if (x == -1 || x == 2 || y == -1 || y == 3) {
                    BlockPos pos = frame.at(x, y);
                    if (!BiomePlacementRestrictions.isAllowed(level, pos, frameState))
                        throw new IllegalStateException("Gallery portal frame material is unavailable: " + frameState
                                + " in " + level.dimension().location());
                    level.setBlock(pos, frameState, 18);
                    requirePlacedState(level, pos, frameState);
                }
            }
            if (wooden) {
                for (BlockPos pos : frame.interior()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                PortalDestination destination = ((WoodenPortalBlock) type).destination();
                if (!frame.activate(level, destination) || !frame.active(level, destination))
                    throw new IllegalStateException("World portal frame did not activate: " + id + "; "
                            + portalFrameDiagnostic(level, anchor, frame, destination));
            } else {
                for (BlockPos pos : frame.interior()) level.setBlock(pos, state, 18);
            }
            target = Vec3.atCenterOf(anchor).add(-.5, 1, 0);
            radius = 3.1;
        } else if (id.equals("hearthandharvest:corn_stalk")) {
            level.setBlock(anchor.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), 18);
            Property<?> section = type.getStateDefinition().getProperty("section");
            for (int y = 0; y < 3; y++) level.setBlock(anchor.above(y), setProperty(state, section, new String[]{"bottom", "middle", "top"}[y]), 18);
            target = Vec3.atCenterOf(anchor.above());
            radius = 1.75;
        } else if (snowy(view)) {
            for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-2, 0, -2), anchor.offset(2, 0, 2)))
                level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 18);
            if (!SnowyPlantSnowlogging.place(level, anchor, state)) throw new IllegalStateException("Cannot snowlog photographed berry: " + id);
            level.sendBlockUpdated(anchor, level.getBlockState(anchor), level.getBlockState(anchor), 3);
        } else level.setBlock(anchor, state, 18);
        if (view.has("visualRadius")) radius = view.get("visualRadius").getAsDouble();
        return new Scene(key, level.dimension(), anchor, -1, null, null, target, radius, false, false, level.getGameTime());
    }

    private static Map<String, Object> portalFrameDiagnostic(ServerLevel level, BlockPos anchor,
            PortalFrame frame, PortalDestination destination) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("sourceDimension", level.dimension().location().toString());
        detail.put("destination", destination.id());
        detail.put("sourceAllowed", destination.allowedFromSource(level.dimension()));
        detail.put("anchor", anchor.toShortString());
        detail.put("anchorHasChunk", level.hasChunkAt(anchor));
        detail.put("anchorInBorder", level.getWorldBorder().isWithinBounds(anchor));
        detail.put("buildHeight", List.of(level.getMinBuildHeight(), level.getMaxBuildHeight()));
        detail.put("frameValid", frame.valid(level));
        detail.put("frameActive", frame.active(level, destination));
        List<Map<String, Object>> contents = new ArrayList<>();
        List<BlockPos> positions = new ArrayList<>(frame.border());
        positions.addAll(frame.interior());
        for (BlockPos pos : positions) {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("position", pos.toShortString());
            block.put("part", frame.interior().contains(pos) ? "interior" : "border");
            block.put("hasChunk", level.hasChunkAt(pos));
            block.put("inWorldBorder", level.getWorldBorder().isWithinBounds(pos));
            block.put("outsideBuildHeight", level.isOutsideBuildHeight(pos));
            block.put("state", level.getBlockState(pos).toString());
            block.put("plank", level.getBlockState(pos).is(net.minecraft.tags.BlockTags.PLANKS));
            contents.add(block);
        }
        detail.put("frameContents", contents);
        return detail;
    }

    private static Entity resolveSubject(Minecraft minecraft, Scene current) {
        if (current == null || current.entityUuid() == null || minecraft.level == null
                || !minecraft.level.dimension().equals(current.dimension())) return null;
        Entity direct = minecraft.level.getEntity(current.entityId());
        if (matchesSubject(direct, current)) return direct;
        // Dimension bridges may give the client a different numeric entity ID. UUID and registry
        // type must both match; never substitute an unrelated nearby animal of the same species.
        for (Entity candidate : minecraft.level.entitiesForRendering()) {
            if (matchesSubject(candidate, current)) {
                if (UUID_RESOLVED.add(current.entityUuid())) AOEMod.LOGGER.info(
                        "World gallery resolved specimen by UUID: serverId={}, clientId={}, uuid={}, type={}",
                        current.entityId(), candidate.getId(), current.entityUuid(), current.entityType());
                return candidate;
            }
        }
        return null;
    }

    private static boolean matchesSubject(Entity candidate, Scene current) {
        return candidate != null && current.entityUuid().equals(candidate.getUUID())
                && current.entityType().equals(ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType()));
    }

    private static boolean snowy(JsonObject view) {
        return view.has("snowy") && view.get("snowy").getAsBoolean()
                || view.has("model") && view.get("model").getAsString().contains("snowy_");
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        if (property == null) throw new IllegalArgumentException("Missing world block property");
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    private static void updateCamera(Minecraft minecraft, JsonObject view, Scene current, Entity subject) {
        if (camera == null || camera.level() != minecraft.level) {
            camera = EntityType.ARMOR_STAND.create(minecraft.level);
            if (camera == null) throw new IllegalStateException("Cannot create local world camera");
            camera.setNoGravity(true);
            camera.setInvisible(true);
            ((LivingEntity) camera).addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 24000, 0, false, false, false));
        }
        Vec3 target = current.target();
        if (subject != null) target = subject.position().add(0, subject.getBbHeight() * .5, 0);
        double angle = view.get("angle").getAsDouble() + (subject == null ? 0 : subject.getYRot());
        double elevation = view.has("elevationDegrees") ? view.get("elevationDegrees").getAsDouble()
                : view.has("pitch") ? Math.abs(view.get("pitch").getAsDouble()) : 24;
        elevation = Mth.clamp(elevation, 8, 78);
        double distance = current.radius() / Math.sin(Math.toRadians(25)) * 1.35;
        double yawRadians = Math.toRadians(angle), elevationRadians = Math.toRadians(elevation);
        Vec3 eye = target.add(-Math.sin(yawRadians) * Math.cos(elevationRadians) * distance,
                Math.sin(elevationRadians) * distance, Math.cos(yawRadians) * Math.cos(elevationRadians) * distance);
        Vec3 aim = target.subtract(eye);
        float yaw = (float) (Mth.atan2(aim.z, aim.x) * Mth.RAD_TO_DEG - 90);
        float pitch = (float) (-Mth.atan2(aim.y, aim.horizontalDistance()) * Mth.RAD_TO_DEG);
        camera.absMoveTo(eye.x, eye.y - camera.getEyeHeight(), eye.z, yaw, pitch);
        camera.setOldPosAndRot();
        // LivingEntity.getViewYRot reads its head yaw, not Entity.yRot. Keep both
        // interpolation endpoints aligned so the world camera actually faces the specimen.
        LivingEntity livingCamera = (LivingEntity) camera;
        livingCamera.setYHeadRot(yaw);
        livingCamera.yHeadRotO = yaw;
        livingCamera.setYBodyRot(yaw);
        livingCamera.yBodyRotO = yaw;
        if (Math.abs(Mth.wrapDegrees(camera.getViewYRot(1) - yaw)) > .01F
                || Math.abs(camera.getViewXRot(1) - pitch) > .01F) {
            throw new IllegalStateException("World gallery camera rotation did not match intended aim");
        }
        minecraft.setCameraEntity(camera);
    }

    private static void capture(Minecraft minecraft, JsonObject view, Scene current, Entity subject) throws Exception {
        String label = view.get("label").getAsString();
        if (!label.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("Unsafe gallery filename");
        Path target = output.resolve(label + ".png");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", label); result.put("id", view.get("id").getAsString());
        result.put("angle", view.get("angle").getAsDouble()); result.put("dimension", current.dimension().location().toString());
        result.put("camera", List.of(camera.getX(), camera.getEyeY(), camera.getZ(), camera.getYRot(), camera.getXRot()));
        var renderedCamera = minecraft.gameRenderer.getMainCamera();
        result.put("renderCamera", List.of(renderedCamera.getPosition().x, renderedCamera.getPosition().y,
                renderedCamera.getPosition().z, renderedCamera.getYRot(), renderedCamera.getXRot()));
        result.put("visualRadius", current.radius()); result.put("settledTicks", settledTicks);
        if (subject != null) {
            result.put("entityPosition", List.of(subject.getX(), subject.getY(), subject.getZ()));
            result.put("entityAgeTicks", subject.tickCount); result.put("inWater", subject.isInWater());
            result.put("onGround", subject.onGround()); result.put("entityUuid", subject.getUUID().toString());
            result.put("serverEntityId", current.entityId()); result.put("clientEntityId", subject.getId());
        }
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(target); result.put("width", image.getWidth()); result.put("height", image.getHeight());
        }
        result.put("path", target.toString());
        CAPTURES.add(result);
        AOEMod.LOGGER.info("World gallery captured {}", label);
    }

    private static void writeReport(String status, String failure) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", status); report.put("capturedAt", Instant.now().toString()); report.put("before", BEFORE);
        report.put("source", "Actual Minecraft world framebuffer: live server-spawned mobs, water or land habitat, ordinary world-rendered blocks");
        report.put("requested", views == null ? 0 : views.size()); report.put("captures", CAPTURES);
        if (failure != null) report.put("failure", failure);
        if (failureFrame != null) report.put("failureFrame", failureFrame.toString());
        if (failureState != null) report.put("failureState", failureState.toString());
        if (output != null) Files.writeString(output.resolve("capture-report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
    }

    private static Map<String, Object> diagnosticState(Minecraft minecraft) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("index", index);
        state.put("label", views != null && index < views.size() ? views.get(index).getAsJsonObject().get("label").getAsString() : "not_loaded");
        state.put("requestedIndex", requestedIndex);
        state.put("readyIndex", readyIndex);
        state.put("chunkAcknowledgedTicks", chunkAcknowledgedTicks);
        state.put("spawnRequestedIndex", spawnRequestedIndex);
        state.put("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        state.put("overlay", minecraft.getOverlay() == null ? "none" : minecraft.getOverlay().getClass().getName());
        state.put("paused", minecraft.isPaused());
        state.put("dimension", minecraft.level == null ? "none" : minecraft.level.dimension().location().toString());
        Scene current = scene;
        state.put("sceneDimension", current == null ? "none" : current.dimension().location().toString());
        state.put("chunkReady", current != null && minecraft.level != null && minecraft.level.hasChunkAt(current.anchor()));
        state.put("entityId", current == null ? -1 : current.entityId());
        state.put("entityUuid", current == null || current.entityUuid() == null ? "none" : current.entityUuid().toString());
        Entity subject = resolveSubject(minecraft, current);
        state.put("clientEntityId", subject == null ? -1 : subject.getId());
        state.put("entityPresent", subject != null);
        state.put("entityAge", subject == null ? -1 : subject.tickCount);
        state.put("inWater", subject != null && subject.isInWater());
        state.put("alive", subject != null && subject.isAlive());
        state.put("settledTicks", settledTicks);
        state.put("phaseElapsedMillis", phaseStarted == 0 ? 0 : System.currentTimeMillis() - phaseStarted);
        state.put("serverFailure", serverFailure == null ? "none" : serverFailure);
        state.put("serverEntityState", serverEntityState);
        if (camera != null) {
            state.put("cameraYaw", camera.getYRot());
            state.put("cameraViewYaw", camera.getViewYRot(1));
            state.put("cameraPitch", camera.getViewXRot(1));
        }
        return state;
    }

    private static void saveFailureDiagnostics(Minecraft minecraft, String failure) {
        try {
            // Deliberately outside the capture directory: a failed frame is never an archive view.
            Path debug = minecraft.gameDirectory.toPath().resolve("gallery-debug");
            Files.createDirectories(debug);
            String prefix = String.format("failure-%04d-%d", index, System.currentTimeMillis());
            Path frame = debug.resolve(prefix + ".png");
            try (NativeImage screenshot = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                screenshot.writeToFile(frame);
            }
            failureFrame = frame.toAbsolutePath();
            Map<String, Object> state = diagnosticState(minecraft);
            state.put("failure", failure);
            state.put("capturedAt", Instant.now().toString());
            state.put("frame", failureFrame.toString());
            failureState = debug.resolve(prefix + ".json").toAbsolutePath();
            Files.writeString(failureState, new GsonBuilder().setPrettyPrinting().create().toJson(state) + "\n");
            AOEMod.LOGGER.error("World gallery failure diagnostics saved to {}", failureState);
        } catch (Exception error) {
            AOEMod.LOGGER.error("Cannot save world gallery failure diagnostics", error);
        }
    }

    private static void finish(Minecraft minecraft, String failure) {
        completed = true;
        if (failure != null) saveFailureDiagnostics(minecraft, failure);
        minecraft.setCameraEntity(minecraft.player);
        try { writeReport(failure == null ? "passed" : "failed", failure); }
        catch (Exception error) { AOEMod.LOGGER.error("Cannot write world gallery report", error); }
        if (Boolean.getBoolean("aoemod.gallery.exit")) minecraft.stop();
    }

    private static void installOriginalArthropleura(Minecraft minecraft) {
        var dispatcher = minecraft.getEntityRenderDispatcher();
        var context = new net.minecraft.client.renderer.entity.EntityRendererProvider.Context(dispatcher,
                minecraft.getItemRenderer(), minecraft.getBlockRenderer(), dispatcher.getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("aoemod", "arthropleura"));
        var renderer = new net.minecraft.client.renderer.entity.MobRenderer<ArthropleuraEntity, OriginalArthropleuraModel>(
                context, new OriginalArthropleuraModel(OriginalArthropleuraModel.createBodyLayer().bakeRoot()), .7F) {
            @Override public ResourceLocation getTextureLocation(ArthropleuraEntity entity) {
                return new ResourceLocation("minecraft", "textures/block/white_concrete.png");
            }
        };
        dispatcher.renderers = new java.util.HashMap<>(dispatcher.renderers);
        dispatcher.renderers.put(type, renderer);
        // The native stalker renderer has no emissive layer; the current mod adds that layer at reload.
        var stalkerType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("outer_end", "stalker"));
        var stalker = dispatcher.renderers.get(stalkerType);
        try {
            dispatcher.renderers.put(stalkerType, (net.minecraft.client.renderer.entity.EntityRenderer<?>) stalker.getClass()
                    .getConstructor(net.minecraft.client.renderer.entity.EntityRendererProvider.Context.class).newInstance(context));
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot restore native stalker renderer", failure); }
    }

    private static void exportDerivedTextures(Minecraft minecraft) throws Exception {
        String directory = System.getProperty("aoemod.gallery.derivedTextures");
        if (directory == null) return;
        for (String name : List.of("aoemod:textures/block/tropical_azalea_leaves.png",
                "aoemod:textures/block/tropical_flowering_azalea_leaves.png", "aoemod:textures/block/wooden_portal.png",
                "ambientadditions:textures/entity/blue_spotted_stingray.png")) {
            var id = new ResourceLocation(name);
            Path target = Path.of(directory).resolve(id.getNamespace()).resolve(id.getPath());
            Files.createDirectories(target.getParent());
            try (var stream = minecraft.getResourceManager().getResourceOrThrow(id).open()) {
                Files.copy(stream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            var metadata = minecraft.getResourceManager().getResource(new ResourceLocation(id.getNamespace(), id.getPath() + ".mcmeta"));
            if (metadata.isPresent()) try (var stream = metadata.get().open()) {
                Files.copy(stream, Path.of(target + ".mcmeta"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        var portal = new ResourceLocation("aoemod", "textures/block/wooden_portal.png");
        byte[] metadata;
        try (var input = minecraft.getResourceManager().getResourceOrThrow(new ResourceLocation("aoemod", "textures/block/wooden_portal.png.mcmeta")).open()) {
            metadata = input.readAllBytes();
        }
        for (PortalDestination destination : PortalDestination.values()) {
            Path target = Path.of(directory).resolve("portal-colors").resolve(destination.id() + "_portal.png");
            Files.createDirectories(target.getParent());
            try (var input = minecraft.getResourceManager().getResourceOrThrow(portal).open(); NativeImage texture = NativeImage.read(input)) {
                int color = destination.color();
                for (int y = 0; y < texture.getHeight(); y++) for (int x = 0; x < texture.getWidth(); x++) {
                    int pixel = texture.getPixelRGBA(x, y);
                    int red = (pixel & 255) * ((color >> 16) & 255) / 255;
                    int green = ((pixel >> 8) & 255) * ((color >> 8) & 255) / 255;
                    int blue = ((pixel >> 16) & 255) * (color & 255) / 255;
                    texture.setPixelRGBA(x, y, (pixel & 0xFF000000) | red | green << 8 | blue << 16);
                }
                texture.writeToFile(target);
            }
            Files.write(Path.of(target + ".mcmeta"), metadata);
        }
    }

    private static void installOriginalTextures(Minecraft minecraft) throws Exception {
        String directory = System.getProperty("aoemod.gallery.originalTextures");
        if (directory == null) return;
        Path root = Path.of(directory);
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".png")).toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                int slash = relative.indexOf('/');
                var id = new ResourceLocation(relative.substring(0, slash), relative.substring(slash + 1));
                try (var input = Files.newInputStream(path)) {
                    minecraft.getTextureManager().register(id, new DynamicTexture(NativeImage.read(input)));
                }
            }
        }
    }

    // Historical pre-revision model retained only for the explicit gallery export.
    private static final class OriginalArthropleuraModel extends EntityModel<ArthropleuraEntity> {
        private static final int SEGMENTS = 14;
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart[] segments = new ModelPart[SEGMENTS];
    
        public OriginalArthropleuraModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            for (int i = 0; i < SEGMENTS; i++) this.segments[i] = root.getChild("segment_" + i);
        }
    
        public static LayerDefinition createBodyLayer() {
            var mesh = new MeshDefinition();
            var root = mesh.getRoot();
            for (int i = 0; i < SEGMENTS; i++) {
                float width = i < 10 ? 9 - Math.abs(4 - i) * 0.22F : 8.3F - (i - 10) * 0.7F;
                var plate = root.addOrReplaceChild("segment_" + i, CubeListBuilder.create()
                        .addBox(-width / 2, -2.2F, -1.45F, width, 3, 2.9F),
                        PartPose.offset(0, 20.5F, -17 + i * 2.8F));
                plate.addOrReplaceChild("ridge", CubeListBuilder.create()
                        .addBox(-width / 2 - 0.5F, -2.4F, 0.3F, width + 1, 1, 1.2F), PartPose.ZERO);
                for (int pair = 0; pair < 2; pair++) {
                    for (int side : new int[]{-1, 1}) {
                        var leg = plate.addOrReplaceChild("leg_" + pair + "_" + side, CubeListBuilder.create()
                                .addBox(side < 0 ? -3.5F : 0, -0.35F, -0.35F, 3.5F, 0.7F, 0.7F),
                                PartPose.offsetAndRotation(side * (width / 2 - 0.5F), 0.8F, pair * 1.3F - 0.6F,
                                        0, 0, side * 0.4F));
                        leg.addOrReplaceChild("foot", CubeListBuilder.create()
                                .addBox(-0.35F, 0, -0.35F, 0.7F, 2, 0.7F),
                                PartPose.offsetAndRotation(side * 3.2F, 0, 0, 0, 0, side * -0.25F));
                    }
                }
            }
            var head = root.addOrReplaceChild("head", CubeListBuilder.create().addBox(-3.5F, -2, -3, 7, 3.5F, 4),
                    PartPose.offset(0, 20.5F, -19));
            head.addOrReplaceChild("eyes", CubeListBuilder.create()
                    .addBox(-3.65F, -1.4F, -2.5F, 0.3F, 0.65F, 1)
                    .addBox(3.35F, -1.4F, -2.5F, 0.3F, 0.65F, 1), PartPose.ZERO);
            for (int side : new int[]{-1, 1}) {
                var antenna = head.addOrReplaceChild("antenna_" + side, CubeListBuilder.create()
                        .addBox(-0.3F, -0.3F, -4, 0.6F, 0.6F, 4),
                        PartPose.offsetAndRotation(side * 2, -1, -2.5F, -0.25F, side * -0.5F, 0));
                var bend = antenna.addOrReplaceChild("bend", CubeListBuilder.create()
                        .addBox(-0.25F, -0.25F, -3, 0.5F, 0.5F, 3),
                        PartPose.offsetAndRotation(0, 0, -4, 0.15F, side * 0.65F, 0));
                bend.addOrReplaceChild("tip", CubeListBuilder.create().addBox(-0.2F, -0.2F, -2, 0.4F, 0.4F, 2),
                        PartPose.offsetAndRotation(0, 0, -3, 0.35F, side * 0.35F, 0));
            }
            return LayerDefinition.create(mesh, 16, 16);
        }
    
        @Override
        public void setupAnim(ArthropleuraEntity entity, float walk, float amount, float age, float yaw, float pitch) {
            this.head.yRot = Mth.clamp(yaw * Mth.DEG_TO_RAD, -0.3F, 0.3F);
            float stride = Math.min(amount * 2.5F, 1);
            for (int i = 0; i < SEGMENTS; i++) {
                for (int pair = 0; pair < 2; pair++) {
                    for (int side : new int[]{-1, 1}) {
                        var leg = this.segments[i].getChild("leg_" + pair + "_" + side);
                        float phase = walk * 2.4F - i * 0.65F - pair * 0.45F + (side < 0 ? Mth.PI : 0);
                        leg.yRot = Mth.sin(phase) * 0.5F * stride;
                        leg.zRot = side * (0.4F + Mth.cos(phase) * 0.22F * stride);
                    }
                }
            }
            for (int side : new int[]{-1, 1}) {
                this.head.getChild("antenna_" + side).yRot = side * -0.5F + Mth.sin(age * 0.07F) * 0.08F;
            }
        }
    
        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                                   float red, float green, float blue, float alpha) {
            pose.pushPose();
            if (this.young) {
                pose.translate(0, 0.75, 0);
                pose.scale(0.5F, 0.5F, 0.5F);
            }
            // Warm alternating plates distinguish the low, broad body from a generic spider.
            for (int i = 0; i < SEGMENTS; i++) {
                float shade = i % 2 == 0 ? 1 : 0.82F;
                this.segments[i].render(pose, vertices, light, overlay,
                        red * 0.45F * shade, green * 0.23F * shade, blue * 0.095F * shade, alpha);
            }
            this.head.render(pose, vertices, light, overlay, red * 0.28F, green * 0.13F, blue * 0.055F, alpha);
            pose.popPose();
        }
    }
}
