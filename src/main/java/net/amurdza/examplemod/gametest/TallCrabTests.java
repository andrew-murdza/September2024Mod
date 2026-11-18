package net.amurdza.examplemod.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.DoubleSupplier;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TallCrabTests {
    private TallCrabTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void restoredCrabSizesKeepNativeMetadataAndCollisionDimensions(GameTestHelper helper) {
        for (var level : List.of(helper.getLevel(), helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL))) {
            for (String id : List.of("flower_crab", "lightfoot_crab")) {
                var crab = (BaseWaterAnimal) ForgeRegistries.ENTITY_TYPES
                        .getValue(new ResourceLocation("hybrid_aquatic", id)).create(level);
                for (int size : new int[]{-5, 0, 5}) {
                    crab.setSize(size);
                    crab.refreshDimensions();
                    helper.assertTrue(crab.getSize() == size, "Crabs retain their native individual sizes");
                    var expected = crab.getDimensions(crab.getPose());
                    helper.assertTrue(Math.abs(crab.getBbHeight() - expected.height) < .0001
                                    && Math.abs(crab.getBbWidth() - expected.width) < .0001,
                            "Crab collision must use the upstream body dimensions");
                }
                crab.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void raisedCrabsShareSteeperStanceAndClearShortSeagrassAboveMud(GameTestHelper helper) throws Exception {
        JsonArray lightfootBones = resource("geo/crustacean/lightfoot_crab/lightfoot_crab.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (String id : List.of("flower_crab", "lightfoot_crab")) {
            JsonArray bones = resource("geo/crustacean/" + id + "/" + id + ".geo.json")
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            JsonObject torso = bone(bones, "torso");
            JsonArray shell = torso.getAsJsonArray("cubes").get(0).getAsJsonObject().getAsJsonArray("size");
            helper.assertTrue(shell.get(0).getAsDouble() == 5 && shell.get(1).getAsDouble() == 2
                            && shell.get(2).getAsDouble() == 4,
                    "Raise the native shell without vertically stretching its texture or body proportions");
            var crab = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id))
                    .create(helper.getLevel());
            double scale = AquaticMobSizes.tallCrabScale(crab);
            helper.assertTrue(Math.abs(scale - (id.equals("flower_crab") ? 1.925 : 2.2)) < .00001,
                    "Both crab scale multipliers must grow by ten percent");
            JsonObject shellCube = torso.getAsJsonArray("cubes").get(0).getAsJsonObject();
            double shellTop = shellCube.getAsJsonArray("origin").get(1).getAsDouble() + shell.get(1).getAsDouble();
            helper.assertTrue(Math.abs(shellTop * 1.25 * scale / 16 - 17.0 / 16) < .0001,
                    "Both solid shell tops must stand one pixel above the full short-seagrass block");
            BlockPos support = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.getLevel().setBlock(support, Blocks.MUD.defaultBlockState(), 2);
            double mudTop = Blocks.MUD.defaultBlockState().getCollisionShape(helper.getLevel(), support,
                    CollisionContext.of(crab)).max(Direction.Axis.Y);
            helper.assertTrue(mudTop == 1 && mudTop + shellTop * 1.25 * scale / 16 > 2,
                    "Mud's recessed vanilla collision cannot lower the shell beneath the seagrass above its visible surface");
            double shadowRadius = 0;
            Vector3d bodyPivot = vector(bone(bones, "body").getAsJsonArray("pivot"));
            bodyPivot.x = -bodyPivot.x;
            Matrix3d bodyRotation = new Matrix3d().rotateY(Math.PI / 2);
            int legs = 0;
            for (JsonElement element : bones) {
                JsonObject leg = element.getAsJsonObject();
                if (!leg.get("name").getAsString().contains("_leg_")) continue;
                legs++;
                helper.assertTrue(leg.get("rotation").equals(bone(lightfootBones, leg.get("name").getAsString()).get("rotation")),
                        "Each flower leg must use the corresponding lightfoot resting angle");
                Vector3d shaft = rotation(leg.getAsJsonArray("rotation"), null).transform(new Vector3d(1, 0, 0));
                double angle = Math.toDegrees(Math.atan2(Math.abs(shaft.y), Math.hypot(shaft.x, shaft.z)));
                helper.assertTrue(Math.abs(angle - 45) < .00001, "Both species need the steeper 45-degree stance");
                JsonObject cube = leg.getAsJsonArray("cubes").get(0).getAsJsonObject();
                Vector3d size = vector(cube.getAsJsonArray("size"));
                helper.assertTrue(size.y == 1 && size.z == 1,
                        "All eight legs, including both flower crab rear legs, must have a solid 1x1 cross-section");
                Vector3d pivot = vector(leg.getAsJsonArray("pivot"));
                pivot.x = -pivot.x;
                Vector3d origin = vector(cube.getAsJsonArray("origin"));
                Matrix3d legRotation = rotation(leg.getAsJsonArray("rotation"), null);
                for (int mask = 0; mask < 8; mask++) {
                    Vector3d corner = new Vector3d(-(origin.x + ((mask & 1) != 0 ? size.x : 0)),
                            origin.y + ((mask & 2) != 0 ? size.y : 0),
                            origin.z + ((mask & 4) != 0 ? size.z : 0));
                    legRotation.transform(corner.sub(pivot)).add(pivot);
                    bodyRotation.transform(corner.sub(bodyPivot)).add(bodyPivot);
                    shadowRadius = Math.max(shadowRadius, Math.hypot(corner.x, corner.z) * 1.25 * scale / 16);
                }
            }
            helper.assertTrue(legs == 8, "Both crabs must retain all eight native legs");
            helper.assertTrue(Math.abs(shadowRadius - AquaticMobSizes.tallCrabShadowRadius(crab)) < .0001,
                    "Each crab's shadow must end at its outermost resting leg tips");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lightfootClawsUseFlowerGeometryAndAnimationWithTheirOwnTexture(GameTestHelper helper) throws Exception {
        JsonArray flower = resource("geo/crustacean/flower_crab/flower_crab.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        JsonArray lightfoot = resource("geo/crustacean/lightfoot_crab/lightfoot_crab.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        double liftDifference = bone(flower, "body").getAsJsonArray("pivot").get(1).getAsDouble()
                - bone(lightfoot, "body").getAsJsonArray("pivot").get(1).getAsDouble();
        JsonObject flowerAnimations = resource("animations/entity/crustacean/flower_crab/flower_crab.animation.json")
                .getAsJsonObject("animations");
        JsonObject lightfootAnimations = resource("animations/entity/crustacean/lightfoot_crab/lightfoot_crab.animation.json")
                .getAsJsonObject("animations");
        BufferedImage flowerTexture = nativeTexture("flower_crab"), lightfootTexture = nativeTexture("lightfoot_crab");
        for (String name : List.of("left_claw", "right_claw")) {
            JsonObject source = bone(flower, name), target = bone(lightfoot, name);
            helper.assertTrue(source.get("rotation").equals(target.get("rotation")), "Claw resting rotations must match");
            Vector3d pivot = vector(source.getAsJsonArray("pivot")).sub(vector(target.getAsJsonArray("pivot")));
            helper.assertTrue(Math.abs(pivot.x) < .0001 && Math.abs(pivot.z) < .0001
                    && Math.abs(pivot.y - liftDifference) < .0001, "Claw attachment points must match relative to the shell");
            JsonArray sourceCubes = source.getAsJsonArray("cubes"), targetCubes = target.getAsJsonArray("cubes");
            helper.assertTrue(sourceCubes.size() == targetCubes.size(), "Both claws need the same number of cubes");
            for (int index = 0; index < sourceCubes.size(); index++) {
                JsonObject original = sourceCubes.get(index).getAsJsonObject();
                JsonObject copy = targetCubes.get(index).getAsJsonObject();
                helper.assertTrue(original.get("size").equals(copy.get("size")) && !copy.has("rotation"),
                        "Lightfoot must reuse the flower claw cubes without the old per-cube rotations");
                helper.assertTrue(mirrored(original) == mirrored(copy), "Both species must mirror the same claw faces");
                Vector3d offset = vector(original.getAsJsonArray("origin")).sub(vector(copy.getAsJsonArray("origin")));
                helper.assertTrue(Math.abs(offset.x) < .0001 && Math.abs(offset.z) < .0001
                        && Math.abs(offset.y - liftDifference) < .0001, "Claw cube placement must match relative to the shell");
                for (String face : List.of("north", "south", "east", "west", "up", "down")) {
                    for (int u = 0; u < 12; u++) for (int v = 0; v < 12; v++) {
                        boolean sourceOpaque = opaque(flowerTexture, original, face, (u + .5) / 12, (v + .5) / 12);
                        boolean targetOpaque = opaque(lightfootTexture, copy, face, (u + .5) / 12, (v + .5) / 12);
                        helper.assertTrue(sourceOpaque == targetOpaque,
                                "The claw opening and inside wall must match flower opacity on " + name + "/" + index + "/" + face);
                    }
                }
            }
            for (String animation : flowerAnimations.keySet()) {
                helper.assertTrue(flowerAnimations.getAsJsonObject(animation).getAsJsonObject("bones").get(name)
                                .equals(lightfootAnimations.getAsJsonObject(animation).getAsJsonObject("bones").get(name)),
                        "Claw animation must match for " + animation);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void flowerRearLegsReuseAllSixOpaqueFrontLegTextureFaces(GameTestHelper helper) throws Exception {
        JsonArray bones = resource("geo/crustacean/flower_crab/flower_crab.geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        BufferedImage texture = nativeTexture("flower_crab");
        for (String side : List.of("left", "right")) {
            JsonObject front = bone(bones, side + "_leg_1").getAsJsonArray("cubes").get(0).getAsJsonObject();
            JsonObject rear = bone(bones, side + "_leg_4").getAsJsonArray("cubes").get(0).getAsJsonObject();
            helper.assertTrue(front.get("uv").equals(rear.get("uv")) && mirrored(front) == mirrored(rear),
                    "Rear legs must use the same complete front leg texture, including both side faces");
            for (String face : List.of("north", "south", "east", "west", "up", "down")) {
                for (int u = 0; u < 12; u++) for (int v = 0; v < 12; v++) {
                    helper.assertTrue(opaque(texture, rear, face, (u + .5) / 12, (v + .5) / 12),
                            "No flower rear leg face may contain a transparent hole: " + side + "/" + face);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void realCrabIdleAndWalkExpressionsKeepFourFeetGrounded(GameTestHelper helper) throws Exception {
        MolangRuntime molang = new MolangRuntime();
        try {
            for (String id : List.of("flower_crab", "lightfoot_crab")) {
                JsonArray bones = resource("geo/crustacean/" + id + "/" + id + ".geo.json")
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
                JsonObject animations = resource("animations/entity/crustacean/" + id + "/" + id + ".animation.json")
                        .getAsJsonObject("animations");
                for (String name : List.of("misc.idle", "move.walk")) {
                    JsonObject poses = animations.getAsJsonObject(name).getAsJsonObject("bones");
                    JsonObject body = poses.getAsJsonObject("body");
                    helper.assertTrue(body.getAsJsonArray("rotation").get(0).getAsDouble() == 0
                                    && body.getAsJsonArray("rotation").get(2).getAsDouble() == 0
                                    && body.getAsJsonArray("position").get(1).getAsDouble() == 0,
                            "The supporting body cannot roll, pitch or bob its planted feet below the ground");
                    for (int frame = 0; frame <= 240; frame++) {
                        double time = frame / 200.0;
                        molang.time(time);
                        int planted = 0;
                        for (JsonElement element : bones) {
                            JsonObject leg = element.getAsJsonObject();
                            String legName = leg.get("name").getAsString();
                            if (!legName.contains("_leg_")) continue;
                            Vector3d pivot = vector(leg.getAsJsonArray("pivot"));
                            pivot.x = -pivot.x;
                            Matrix3d rotation = rotation(leg.getAsJsonArray("rotation"),
                                    poses.getAsJsonObject(legName).getAsJsonArray("rotation"), molang);
                            double bottom = Double.POSITIVE_INFINITY;
                            for (JsonElement cubeElement : leg.getAsJsonArray("cubes")) {
                                JsonObject cube = cubeElement.getAsJsonObject();
                                Vector3d origin = vector(cube.getAsJsonArray("origin"));
                                Vector3d size = vector(cube.getAsJsonArray("size"));
                                for (int mask = 0; mask < 8; mask++) {
                                    Vector3d corner = new Vector3d(-(origin.x + ((mask & 1) != 0 ? size.x : 0)),
                                            origin.y + ((mask & 2) != 0 ? size.y : 0),
                                            origin.z + ((mask & 4) != 0 ? size.z : 0));
                                    rotation.transform(corner.sub(pivot)).add(pivot);
                                    bottom = Math.min(bottom, corner.y);
                                }
                            }
                            helper.assertTrue(bottom >= 0, "Animated foot penetrated ground: " + id + "/" + name + "/" + legName);
                            if (bottom < .026) planted++;
                        }
                        helper.assertTrue(planted >= 4, "The crab must keep at least four planted feet through its walking cycle");
                    }
                }
            }
        } finally {
            molang.time(0);
        }
        helper.succeed();
    }

    private static Matrix3d rotation(JsonArray original, JsonArray animation) throws Exception {
        return rotation(original, animation, null);
    }

    private static Matrix3d rotation(JsonArray original, JsonArray animation, MolangRuntime molang) throws Exception {
        double[] angles = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            angles[axis] = original.get(axis).getAsDouble();
            if (animation != null) angles[axis] += molang.value(animation.get(axis));
        }
        return new Matrix3d().rotateZ(Math.toRadians(angles[2])).rotateY(Math.toRadians(-angles[1]))
                .rotateX(Math.toRadians(-angles[0]));
    }

    /** GeckoLib embeds this math dependency at runtime, outside Forge's compile classpath. */
    private static final class MolangRuntime {
        private final Object parser;
        private final Method parse;
        private final Method setValue;
        private final Method getValue;

        private MolangRuntime() throws Exception {
            Class<?> parserType = Class.forName("software.bernie.geckolib.core.molang.MolangParser");
            parser = parserType.getField("INSTANCE").get(null);
            parse = parserType.getMethod("parseJson", JsonElement.class);
            setValue = parserType.getMethod("setValue", String.class, DoubleSupplier.class);
            getValue = Class.forName("com.eliotlash.mclib.math.IValue").getMethod("get");
        }

        private void time(double time) throws Exception {
            setValue.invoke(parser, "query.anim_time", (DoubleSupplier) () -> time);
        }

        private double value(JsonElement expression) throws Exception {
            return ((Number) getValue.invoke(parse.invoke(null, expression))).doubleValue();
        }
    }

    private static Vector3d vector(JsonArray array) {
        return new Vector3d(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static boolean mirrored(JsonObject cube) {
        return cube.has("mirror") && cube.get("mirror").getAsBoolean();
    }

    private static BufferedImage nativeTexture(String id) throws Exception {
        // Dependency resources live in another Forge module and need not be
        // visible through this mod's classloader on the dedicated server.
        var path = ModList.get().getModFileById("hybrid_aquatic").getFile().findResource(
                "assets", "hybrid_aquatic", "textures", "entity", "crustacean", id, id + ".png");
        try (var stream = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IllegalStateException("Invalid native crab texture " + path);
            return image;
        }
    }

    /** Sample the actual native texture alpha, including GeckoLib's mirrored per-face convention. */
    private static boolean opaque(BufferedImage image, JsonObject cube, String face, double u, double v) {
        double[] rectangle;
        if (cube.get("uv").isJsonObject()) {
            JsonObject definition = cube.getAsJsonObject("uv").getAsJsonObject(face);
            if (definition == null) return false;
            JsonArray start = definition.getAsJsonArray("uv"), size = definition.getAsJsonArray("uv_size");
            rectangle = new double[]{start.get(0).getAsDouble(), start.get(1).getAsDouble(),
                    size.get(0).getAsDouble(), size.get(1).getAsDouble()};
        } else {
            // Box UVs do not swap mirrored top/bottom vertices; per-face UVs do.
            if (mirrored(cube)) face = face.equals("up") ? "down" : face.equals("down") ? "up" : face;
            JsonArray start = cube.getAsJsonArray("uv"), size = cube.getAsJsonArray("size");
            double a = start.get(0).getAsDouble(), b = start.get(1).getAsDouble();
            double x = size.get(0).getAsDouble(), y = size.get(1).getAsDouble(), z = size.get(2).getAsDouble();
            rectangle = switch (face) {
                case "north" -> new double[]{a + z, b + z, x, y};
                case "south" -> new double[]{a + z * 2 + x, b + z, x, y};
                case "east" -> new double[]{a, b + z, z, y};
                case "west" -> new double[]{a + z + x, b + z, z, y};
                case "up" -> new double[]{a + z, b, x, z};
                case "down" -> new double[]{a + z + x, b + z, x, -z};
                default -> throw new IllegalArgumentException(face);
            };
        }
        if (rectangle[2] == 0 || rectangle[3] == 0) return false;
        int pixel = image.getRGB((int) Math.floor(rectangle[0] + rectangle[2] * u),
                (int) Math.floor(rectangle[1] + rectangle[3] * v));
        return (pixel >>> 24) != 0;
    }

    private static JsonObject bone(JsonArray bones, String name) {
        for (JsonElement value : bones) if (value.getAsJsonObject().get("name").getAsString().equals(name))
            return value.getAsJsonObject();
        throw new IllegalStateException("Missing model bone " + name);
    }

    private static JsonObject resource(String path) throws Exception {
        try (var stream = TallCrabTests.class.getResourceAsStream("/assets/aoemod/" + path)) {
            if (stream == null) throw new IllegalStateException("Missing tall crab resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
