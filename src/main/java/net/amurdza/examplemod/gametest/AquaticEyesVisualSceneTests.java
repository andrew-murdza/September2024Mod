package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import dev.hybridlabs.aquatic.entity.cephalopod.UmbrellaOctopusEntity;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent screenshot actors only when explicitly requested in a disposable GameTest world. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticEyesVisualSceneTests {
    private static final String MARKER = "aoemod:aquatic_eyes_visual";
    private static final BlockPos ORIGIN = new BlockPos(480, 180, 480);

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void exportUmbrellaSideEyesAndTripodfishInZeroLight(GameTestHelper helper) {
        if (!Boolean.getBoolean("aoemod.aquaticEyesVisual")) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        // Keep the four remote aquarium chunks loaded and entity-ticking. Without
        // tickets, GameTests unload/save the actors before the delayed assertions.
        for (int chunkX = ORIGIN.getX() >> 4; chunkX <= ((ORIGIN.getX() + 56) >> 4); chunkX++) {
            level.setChunkForced(chunkX, ORIGIN.getZ() >> 4, true);
            level.getChunk(chunkX, ORIGIN.getZ() >> 4);
        }
        for (var actor : level.getEntitiesOfClass(Mob.class, new AABB(ORIGIN, ORIGIN.offset(58, 8, 10)),
                mob -> mob.getTags().contains(MARKER) || mob.getPersistentData().getBoolean(MARKER))) actor.discard();
        List<Map<String, Object>> views = new ArrayList<>();
        Map<Mob, Vec3> fixtures = new java.util.LinkedHashMap<>();
        String[] colors = {"orange", "purple"};
        int[] variants = {2, 4};
        for (int index = 0; index < colors.length; index++) {
            BlockPos floor = ORIGIN.offset(index * 12, 0, 0);
            aquarium(level, floor, false);
            var octopus = (UmbrellaOctopusEntity) actor(helper, level, "hybrid_aquatic:umbrella_octopus",
                    floor.offset(4, 2, 4), variants[index], 0);
            octopus.setSize(5);
            fixtures.put(octopus, octopus.position());
            Vec3 eye = octopus.position().add(0, 0.42, 0);
            // Opposite oblique views expose both original eye faces without looking through glass.
            views.add(view("umbrella-" + colors[index] + "-right-eye", level,
                    eye.add(1.65, 0.18, 0.65), eye, 28));
            views.add(view("umbrella-" + colors[index] + "-left-eye", level,
                    eye.add(-1.65, 0.18, 0.65), eye, 28));
        }
        BlockPos darkFloor = ORIGIN.offset(36, 0, 0);
        aquarium(level, darkFloor, true);
        var darkFish = actor(helper, level, "alexscaves:tripodfish", darkFloor.offset(4, 2, 4), 20, 0);
        fixtures.put(darkFish, darkFish.position());
        var darkTarget = darkFish.position().add(0, darkFish.getBbHeight() * 0.5, 0);
        views.add(view("tripodfish-emissive-in-zero-light", level,
                darkTarget.add(2.1, 0.15, 1.3), darkTarget, 40));
        BlockPos litFloor = ORIGIN.offset(48, 0, 0);
        aquarium(level, litFloor, false);
        var litFish = actor(helper, level, "alexscaves:tripodfish", litFloor.offset(4, 2, 4), 21, 0);
        fixtures.put(litFish, litFish.position());
        var litTarget = litFish.position().add(0, litFish.getBbHeight() * 0.5, 0);
        views.add(view("tripodfish-lit-reference", level,
                litTarget.add(2.1, 0.15, 1.3), litTarget, 40));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(level.getBrightness(LightLayer.BLOCK, darkFish.blockPosition()) == 0
                            && level.getBrightness(LightLayer.SKY, darkFish.blockPosition()) == 0,
                    "The dark reference aquarium must have zero block light and skylight");
            fixtures.forEach((fixture, anchor) -> assertFullySubmerged(helper, level, fixture, anchor));
            try {
                Path output = Path.of("aquatic-eyes-views.json").toAbsolutePath();
                Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views) + "\n");
                level.getServer().saveEverything(true, true, true);
                AOEMod.LOGGER.info("Aquatic eye/glow visual fixtures ready: {}", output);
            } catch (java.io.IOException failure) {
                throw new IllegalStateException("Cannot export aquatic eye/glow views", failure);
            }
            helper.succeed();
        });
    }

    private static void assertFullySubmerged(GameTestHelper helper, ServerLevel level, Mob actor, Vec3 anchor) {
        helper.assertTrue(!actor.isRemoved() && level.getEntity(actor.getUUID()) == actor
                        && actor.isNoAi() && actor.isNoGravity() && actor.tickCount > 0
                        && actor.position().distanceToSqr(anchor) < 1.0E-8,
                "The saved aquatic fixture must remain live, ticked and at its camera anchor: " + actor.getType()
                        + " removed=" + actor.getRemovalReason() + " noAI=" + actor.isNoAi()
                        + " noGravity=" + actor.isNoGravity() + " ticks=" + actor.tickCount
                        + " position=" + actor.position() + " anchor=" + anchor);
        var bounds = actor.getBoundingBox();
        // Remote GameTest chunks need not entity-tick before the screenshot client visits.
        // isInWater() is a last-tick cache; inspect every occupied fluid cell instead.
        final double epsilon = 1.0E-6;
        var min = new BlockPos(Mth.floor(bounds.minX + epsilon), Mth.floor(bounds.minY + epsilon), Mth.floor(bounds.minZ + epsilon));
        var max = new BlockPos(Mth.floor(bounds.maxX - epsilon), Mth.floor(bounds.maxY - epsilon), Mth.floor(bounds.maxZ - epsilon));
        for (var cell : BlockPos.betweenClosed(min, max)) {
            var fluid = level.getFluidState(cell);
            double requiredSurface = Math.min(bounds.maxY, cell.getY() + 1.0);
            helper.assertTrue(fluid.is(FluidTags.WATER)
                            && cell.getY() + fluid.getHeight(level, cell) + epsilon >= requiredSurface,
                    "The entire visual actor must be covered by actual water: " + actor.getType()
                            + " position=" + actor.position() + " bounds=" + bounds + " dry cell=" + cell);
        }
        AOEMod.LOGGER.info("Aquatic visual submersion verified: {} position={} bounds={} ticks={} cachedWater={}",
                actor.getType(), actor.position(), bounds, actor.tickCount, actor.isInWater());
    }

    private static void aquarium(ServerLevel level, BlockPos floor, boolean dark) {
        for (int x = 0; x <= 8; x++) for (int y = 0; y <= 6; y++) for (int z = 0; z <= 8; z++) {
            boolean shell = x == 0 || x == 8 || z == 0 || z == 8 || y == 0 || y == 6;
            var block = !shell ? Blocks.WATER : dark ? Blocks.BLACK_CONCRETE
                    : y == 6 ? Blocks.SEA_LANTERN : y == 0 || z == 0 ? Blocks.WHITE_CONCRETE : Blocks.GLASS;
            level.setBlock(floor.offset(x, y, z), block.defaultBlockState(), 18);
        }
    }

    private static Mob actor(GameTestHelper helper, ServerLevel level, String id, BlockPos position, int variant, float yaw) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        var actor = (Mob) type.create(level);
        actor.setUUID(new UUID(0xA0E20261122L, variant));
        actor.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, yaw, 0);
        actor.setYBodyRot(yaw); actor.setYHeadRot(yaw);
        actor.setDeltaMovement(Vec3.ZERO);
        actor.setNoAi(true); actor.setNoGravity(true); actor.setPersistenceRequired();
        actor.setInvulnerable(true); actor.setSilent(true);
        actor.addTag(MARKER);
        actor.getPersistentData().putBoolean(MARKER, true);
        helper.assertTrue(level.addFreshEntity(actor), "Could not add aquatic visual actor " + id);
        return actor;
    }

    /** Exported positions are player feet; callers supply the desired camera/eye point. */
    private static Map<String, Object> view(String name, ServerLevel level, Vec3 camera, Vec3 target, int fov) {
        Vec3 feet = camera.subtract(0, 1.62, 0);
        Vec3 delta = target.subtract(camera);
        return Map.of("label", name, "dimension", level.dimension().location().toString(),
                "x", feet.x, "y", feet.y, "z", feet.z, "fov", fov,
                "yaw", Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                "pitch", -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
    }
}
