package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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

/** Persistent fixtures and real terrain views, only in an explicitly requested disposable QA world. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalFishVisualSceneTests {
    private static final String MARKER = "aoemod:tropical_fish_visual";

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void exportMaximumStarfishMasks(GameTestHelper helper) throws Exception {
        if (!Boolean.getBoolean("aoemod.tropicalFishVisual")) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var server = level.getServer();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        List<Map<String, Object>> views = new ArrayList<>();
        BlockPos floor = new BlockPos(320, 180, 320);
        for (var mob : level.getEntitiesOfClass(Mob.class, new AABB(floor, floor.offset(22, 6, 12)),
                actor -> actor.getPersistentData().getBoolean(MARKER))) mob.discard();
        for (int x = 0; x <= 22; x++) for (int z = 0; z <= 12; z++) {
            level.setBlock(floor.offset(x, 0, z), Blocks.WHITE_CONCRETE.defaultBlockState(), 18);
            for (int y = 1; y <= 5; y++) {
                boolean wall = x == 0 || x == 22 || z == 0 || z == 12;
                level.setBlock(floor.offset(x, y, z), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 18);
            }
        }
        Type[] variants = {Type.MEDIUM, Type.CROWN_OF_THORNS};
        for (int index = 0; index < variants.length; index++) {
            BlockPos plant = floor.offset(4 + index * 5, 1, 4);
            level.setBlock(plant, Blocks.SEAGRASS.defaultBlockState(), 18);
            // This second, unoccupied block must stay fully visible behind the animal.
            level.setBlock(plant.north(2), Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 18);
            level.setBlock(plant.north(2).above(), Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 18);
            var actor = (StarfishEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", "starfish")).create(level);
            actor.setVariant(variants[index]);
            actor.setSize(5);
            actor.setUUID(new UUID(0xA0E20261120L, index + 1));
            actor.moveTo(plant.getX() + .5, plant.getY(), plant.getZ() + .5, 0, 0);
            actor.setYBodyRot(0); actor.setYHeadRot(0);
            actor.setNoAi(true); actor.setNoGravity(true); actor.setPersistenceRequired();
            actor.setInvulnerable(true); actor.setSilent(true);
            actor.getPersistentData().putBoolean(MARKER, true);
            helper.assertTrue(level.addFreshEntity(actor) && actor.getSize() == 5,
                    "Could not add maximum-sized starfish visual actor: " + variants[index]);
            views.add(view("small-aquatic-mask-starfish-" + variants[index].name().toLowerCase(java.util.Locale.ROOT), level,
                    new Vec3(plant.getX() + .5, plant.getY() - 1.05, plant.getZ() + 3.4),
                    new Vec3(plant.getX() + .5, plant.getY() + .15, plant.getZ() + .5), 40));
        }
        views.add(view("small-aquatic-mask-oblique", level, new Vec3(floor.getX() + 1.8, floor.getY() + .5, floor.getZ() + 7.5),
                new Vec3(floor.getX() + 4.5, floor.getY() + 1.15, floor.getZ() + 4.5), 45));
        views.add(view("small-aquatic-background-control", level, new Vec3(floor.getX() + 12, floor.getY() + 3, floor.getZ() + 10),
                new Vec3(floor.getX() + 9, floor.getY() + 1.2, floor.getZ() + 4), 60));
        Path output = Path.of("tropical-fish-views.json").toAbsolutePath();
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(views) + "\n");
        server.saveEverything(true, true, true);
        AOEMod.LOGGER.info("Tropical/fish visual scene ready: {}", output);
        helper.succeed();
    }

    private static Map<String, Object> view(String name, ServerLevel level, Vec3 feet, Vec3 target, int fov) {
        Vec3 delta = target.subtract(feet.add(0, 1.62, 0));
        return Map.of("label", name, "dimension", level.dimension().location().toString(),
                "x", feet.x, "y", feet.y, "z", feet.z, "fov", fov,
                "yaw", Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                "pitch", -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
    }
}
