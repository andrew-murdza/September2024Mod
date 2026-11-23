package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.client.AquaticPlantMask;
import net.amurdza.examplemod.client.SmallAquaticSeagrass;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Geometry checks run by the screenshot client, where client-only baked model classes are available. */
public final class ClientAquaticMaskAudit {
    private ClientAquaticMaskAudit() { }

    public static Map<String, Object> verifyScene(Minecraft minecraft, String label) {
        Type variant = label.endsWith("crown_of_thorns") ? Type.CROWN_OF_THORNS : Type.MEDIUM;
        int x = variant == Type.CROWN_OF_THORNS ? 329 : 324;
        BlockPos occupied = new BlockPos(x, 181, 324);
        var actors = minecraft.level.getEntitiesOfClass(StarfishEntity.class, new net.minecraft.world.phys.AABB(occupied).inflate(.5));
        require(actors.size() == 1 && actors.get(0).getVariant() == variant && actors.get(0).getSize() == 5,
                "Visual fixture must contain the expected starfish style at maximum size: " + variant);
        BlockState state = minecraft.level.getBlockState(occupied);
        require(state.is(Blocks.SEAGRASS), "Occupied plant block must stay unchanged at " + occupied);
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        var original = quads(model, state, ModelData.EMPTY);
        var masked = quads(model, state, SmallAquaticSeagrass.maskData(occupied, state));
        double before = surfaceArea(original), after = surfaceArea(masked);
        require(SmallAquaticSeagrass.clippedAt(occupied), "No captured animated model mask at " + occupied);
        require(after > .001 && after < before - .00001, "Occupied plant must lose only a portion of its surface: " + before + " -> " + after);
        double maximum = masked.stream().flatMapToDouble(q -> {
            int[] raw = q.getVertices(); int stride = raw.length / 4;
            return java.util.stream.IntStream.range(0, 4).mapToDouble(i -> Float.intBitsToFloat(raw[i * stride + 1]));
        }).max().orElseThrow();
        require(maximum > .99, "Unoccupied plant pixels must still reach the top of the block");
        for (BlockPos background : List.of(occupied.north(2), occupied.north(2).above())) {
            var backgroundState = minecraft.level.getBlockState(background);
            require(backgroundState.is(Blocks.TALL_SEAGRASS), "Background double plant must remain unchanged");
            var backgroundModel = minecraft.getBlockRenderer().getBlockModel(backgroundState);
            var untouched = quads(backgroundModel, backgroundState, ModelData.EMPTY);
            var actual = quads(backgroundModel, backgroundState, SmallAquaticSeagrass.maskData(background, backgroundState));
            require(!SmallAquaticSeagrass.clippedAt(background) && actual.size() == untouched.size(), "A different background block was masked");
            for (int i = 0; i < actual.size(); i++) require(Arrays.equals(actual.get(i).getVertices(), untouched.get(i).getVertices()),
                    "Background plant geometry or UVs changed");
        }
        return Map.of("occupied", occupied.toShortString(), "originalArea", before, "maskedArea", after,
                "maximumPlantHeight", maximum, "bothBackgroundHalvesUnchanged", true,
                "starfishStyle", variant.name(), "nativeSize", actors.get(0).getSize());
    }

    private static List<BakedQuad> quads(BakedModel model, BlockState state, ModelData data) {
        List<BakedQuad> result = new ArrayList<>(model.getQuads(state, null, RandomSource.create(91), data, RenderType.cutout()));
        for (Direction side : Direction.values()) result.addAll(model.getQuads(state, side, RandomSource.create(91), data, RenderType.cutout()));
        return result;
    }

    private static double surfaceArea(List<BakedQuad> quads) {
        double total = 0;
        for (var quad : quads) {
            int[] raw = quad.getVertices(); int stride = raw.length / 4;
            Vec3[] v = new Vec3[4];
            for (int i = 0; i < 4; i++) v[i] = new Vec3(Float.intBitsToFloat(raw[i * stride]),
                    Float.intBitsToFloat(raw[i * stride + 1]), Float.intBitsToFloat(raw[i * stride + 2]));
            total += v[1].subtract(v[0]).cross(v[2].subtract(v[0])).length() * .5;
            total += v[2].subtract(v[0]).cross(v[3].subtract(v[0])).length() * .5;
        }
        return total;
    }

    public static List<String> verify() {
        int[] raw = new int[32];
        float[][] points = {{0, 0, .5F}, {1, 0, .5F}, {1, 1, .5F}, {0, 1, .5F}};
        for (int i = 0; i < 4; i++) {
            for (int axis = 0; axis < 3; axis++) raw[i * 8 + axis] = Float.floatToRawIntBits(points[i][axis]);
            raw[i * 8 + 3] = -1;
            raw[i * 8 + 4] = Float.floatToRawIntBits(points[i][0]);
            raw[i * 8 + 5] = Float.floatToRawIntBits(points[i][1]);
        }
        var plant = new BakedQuad(raw, 0, Direction.NORTH, null, true);
        var middle = column(.25, .75, .25, .75, .25);
        var clipped = AquaticPlantMask.clip(plant, List.of(middle));
        require(Math.abs(area(clipped) - .625) < .00001, "Only the upper middle footprint must disappear");
        require(contains(clipped, .1, .95) && contains(clipped, .9, .95), "Unoccupied side blades must remain full height");
        require(contains(clipped, .5, .1) && !contains(clipped, .5, .8), "The column must start at the model surface");
        var behind = column(.25, .75, 1.1, 1.2, .1);
        require(Math.abs(area(AquaticPlantMask.clip(plant, List.of(behind))) - 1) < .00001,
                "A plant plane behind the model's world footprint must remain visible");
        var separated = AquaticPlantMask.clip(plant, List.of(column(.1, .3, .25, .75, .25), column(.7, .9, .25, .75, .25)));
        require(contains(separated, .5, .95) && !contains(separated, .2, .95) && !contains(separated, .8, .95),
                "Transparent gaps between opaque model pixels must remain open");
        for (var quad : clipped) {
            int[] vertices = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                require(Math.abs(Float.intBitsToFloat(vertices[i * 8]) - Float.intBitsToFloat(vertices[i * 8 + 4])) < .00001
                                && Math.abs(Float.intBitsToFloat(vertices[i * 8 + 1]) - Float.intBitsToFloat(vertices[i * 8 + 5])) < .00001,
                        "Plant texture UVs must be clipped, never compressed");
            }
        }
        return List.of("Opaque footprint only", "Full-height side blades", "Surface-height lower bound", "Background preserved", "Transparent gaps preserved", "UVs preserved");
    }

    private static AquaticPlantMask.Column column(double left, double right, double front, double back, double y) {
        return AquaticPlantMask.column(new Vec3(left, y, front), new Vec3(right, y, front), new Vec3(right, y, back), new Vec3(left, y, back));
    }

    private static double area(List<BakedQuad> quads) {
        double result = 0;
        for (var quad : quads) {
            int[] raw = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                result += .5 * (Float.intBitsToFloat(raw[i * 8]) * Float.intBitsToFloat(raw[next * 8 + 1])
                        - Float.intBitsToFloat(raw[next * 8]) * Float.intBitsToFloat(raw[i * 8 + 1]));
            }
        }
        return Math.abs(result);
    }

    private static boolean contains(List<BakedQuad> quads, double x, double y) {
        for (var quad : quads) {
            int[] raw = quad.getVertices();
            boolean positive = false, negative = false;
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                double ax = Float.intBitsToFloat(raw[i * 8]), ay = Float.intBitsToFloat(raw[i * 8 + 1]);
                double bx = Float.intBitsToFloat(raw[next * 8]), by = Float.intBitsToFloat(raw[next * 8 + 1]);
                double cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax);
                positive |= cross > .000001; negative |= cross < -.000001;
            }
            if (!(positive && negative)) return true;
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Aquatic mask geometry: " + message);
    }
}
