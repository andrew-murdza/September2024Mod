package net.amurdza.examplemod.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.SmallAquaticPlants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoQuad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Texture-alpha-aware vertical columns, bounded to the plant occupied by the animal. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class SmallAquaticSeagrass {
    private static final ModelProperty<BlockPos> POSITION = new ModelProperty<>();
    private static final ModelProperty<List<AquaticPlantMask.Column>> MASK = new ModelProperty<>();
    private static volatile Set<BlockPos> dynamicCells = Set.of();
    private static final Map<Integer, Capture> captures = new HashMap<>();
    private static final Map<ResourceLocation, Alpha> textures = new HashMap<>();
    private static ClientLevel activeLevel;
    private static Capture current;

    private SmallAquaticSeagrass() { }

    private record Alpha(int width, int height, boolean[] opaque, Map<UVBounds, List<UVBounds>> regions) {
        boolean at(int x, int y) { return opaque[Math.floorMod(y, height) * width + Math.floorMod(x, width)]; }
    }
    private record UVBounds(double left, double top, double right, double bottom) { }

    private record Capture(Entity entity, int tick, Matrix4f inverse, Vec3 position, Alpha texture, List<AquaticPlantMask.Column> columns) { }

    public static void begin(Entity entity, PoseStack pose, float partialTick, ResourceLocation texture) {
        current = null;
        var minecraft = Minecraft.getInstance();
        if (!SmallAquaticPlants.affected(entity) || entity.level() != minecraft.level || minecraft.player == null
                || entity.distanceToSqr(minecraft.player) > 48 * 48) return;
        Alpha alpha = textures.computeIfAbsent(texture, SmallAquaticSeagrass::readAlpha);
        if (alpha == null) return;
        Vec3 position = new Vec3(Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()), Mth.lerp(partialTick, entity.zOld, entity.getZ()));
        // Both native HAPI renderers scale the pose immediately before calling GeoEntityRenderer.render.
        // Undo only its camera/translation basis; retain that already-applied individual body scale.
        float scale = BaseWaterAnimal.Companion.getScaleAdjustment((BaseWaterAnimal) entity, .05F);
        Matrix4f inverse = new Matrix4f().scaling(scale).mul(new Matrix4f(pose.last().pose()).invert());
        current = new Capture(entity, entity.tickCount, inverse, position, alpha, new ArrayList<>());
    }

    public static void end(Entity entity) {
        if (current != null && current.entity == entity) {
            // Starfish tint/overlay layers redraw the same geometry. Mask each opaque column once.
            captures.put(entity.getId(), new Capture(current.entity, current.tick, current.inverse, current.position,
                    current.texture, List.copyOf(new LinkedHashSet<>(current.columns))));
        }
        current = null;
    }

    /** The matrix includes animated bones, native size variation, rotations, and model corrections. */
    public static void quad(GeoQuad quad, Matrix4f pose) {
        if (current == null) return;
        var source = quad.vertices();
        Matrix4f world = new Matrix4f(current.inverse).mul(pose);
        Vec3[] vertices = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            Vector3f point = world.transformPosition(new Vector3f(source[i].position()));
            vertices[i] = current.position.add(point.x, point.y, point.z);
        }
        if (Math.abs(vertices[1].subtract(vertices[0]).cross(vertices[2].subtract(vertices[0])).y) < 1.0E-8) return;
        int across = -1, down = -1;
        for (int i = 1; i < 4; i++) {
            if (Math.abs(source[i].texV() - source[0].texV()) < 1.0E-6 && source[i].texU() != source[0].texU()) across = i;
            if (Math.abs(source[i].texU() - source[0].texU()) < 1.0E-6 && source[i].texV() != source[0].texV()) down = i;
        }
        if (across < 0 || down < 0) return;
        double u0 = source[0].texU(), v0 = source[0].texV(), u1 = source[across].texU(), v1 = source[down].texV();
        Alpha texture = current.texture;
        Vec3 du = vertices[across].subtract(vertices[0]).scale(1 / (u1 - u0));
        Vec3 dv = vertices[down].subtract(vertices[0]).scale(1 / (v1 - v0));
        var bounds = new UVBounds(Math.min(u0, u1), Math.min(v0, v1), Math.max(u0, u1), Math.max(v0, v1));
        for (var rectangle : texture.regions.computeIfAbsent(bounds, region -> rectangles(texture, region))) {
            double lowU = rectangle.left, highU = rectangle.right, lowV = rectangle.top, highV = rectangle.bottom;
            Vec3 a = vertices[0].add(du.scale(lowU - u0)).add(dv.scale(lowV - v0));
            Vec3 b = vertices[0].add(du.scale(highU - u0)).add(dv.scale(lowV - v0));
            Vec3 c = vertices[0].add(du.scale(highU - u0)).add(dv.scale(highV - v0));
            Vec3 d = vertices[0].add(du.scale(lowU - u0)).add(dv.scale(highV - v0));
            var column = AquaticPlantMask.column(a, b, c, d);
            if (column != null) current.columns.add(column);
        }
    }

    /** Combine adjacent opaque texels once per texture/face, so a solid shell needs one prism, not hundreds. */
    private static List<UVBounds> rectangles(Alpha texture, UVBounds region) {
        int left = Mth.floor(region.left * texture.width + 1.0E-5), right = Mth.ceil(region.right * texture.width - 1.0E-5);
        int top = Mth.floor(region.top * texture.height + 1.0E-5), bottom = Mth.ceil(region.bottom * texture.height - 1.0E-5);
        int width = right - left, height = bottom - top;
        if (width <= 0 || height <= 0) return List.of();
        boolean[] used = new boolean[width * height];
        List<UVBounds> result = new ArrayList<>();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (used[y * width + x] || !texture.at(left + x, top + y)) continue;
            int end = x + 1;
            while (end < width && !used[y * width + end] && texture.at(left + end, top + y)) end++;
            int down = y + 1;
            rows: while (down < height) {
                for (int i = x; i < end; i++) if (used[down * width + i] || !texture.at(left + i, top + down)) break rows;
                down++;
            }
            for (int row = y; row < down; row++) for (int column = x; column < end; column++) used[row * width + column] = true;
            result.add(new UVBounds(Math.max(region.left, (double) (left + x) / texture.width),
                    Math.max(region.top, (double) (top + y) / texture.height),
                    Math.min(region.right, (double) (left + end) / texture.width),
                    Math.min(region.bottom, (double) (top + down) / texture.height)));
        }
        return List.copyOf(result);
    }

    private static Alpha readAlpha(ResourceLocation location) {
        try (var stream = Minecraft.getInstance().getResourceManager().open(location); var image = NativeImage.read(stream)) {
            boolean[] opaque = new boolean[image.getWidth() * image.getHeight()];
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                opaque[y * image.getWidth() + x] = ((image.getPixelRGBA(x, y) >>> 24) & 255) >= 26;
            return new Alpha(image.getWidth(), image.getHeight(), opaque, new HashMap<>());
        } catch (IOException error) {
            AOEMod.LOGGER.warn("Cannot read aquatic plant mask texture {}", location, error);
            return null;
        }
    }

    public static boolean clippedAt(BlockPos pos) { return dynamicCells.contains(pos); }

    private static Map<BlockPos, List<AquaticPlantMask.Column>> masks(ClientLevel level) {
        Map<BlockPos, List<AquaticPlantMask.Column>> result = new HashMap<>();
        captures.values().removeIf(c -> !c.entity.isAlive() || c.entity.level() != level || c.entity.tickCount - c.tick > 2
                || Minecraft.getInstance().player == null || c.entity.distanceToSqr(Minecraft.getInstance().player) > 48 * 48);
        for (Capture capture : captures.values()) for (var column : capture.columns) {
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(column.minX()), Mth.floor(column.minY()), Mth.floor(column.minZ()),
                    Mth.floor(column.maxX() - 1.0E-7), Mth.floor(column.maxY()), Mth.floor(column.maxZ() - 1.0E-7))) {
                BlockState state = level.getBlockState(pos);
                if (!plant(state)) continue;
                result.computeIfAbsent(pos.immutable(), ignored -> new ArrayList<>()).add(column);
                if (state.is(Blocks.TALL_SEAGRASS) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER)
                    result.computeIfAbsent(pos.above(), ignored -> new ArrayList<>()).add(column);
            }
        }
        return result;
    }

    private static boolean plant(BlockState state) {
        return state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (activeLevel != level) { activeLevel = level; dynamicCells = Set.of(); captures.clear(); }
        if (level == null) return;
        Set<BlockPos> next = Set.copyOf(masks(level).keySet());
        Set<BlockPos> changed = new HashSet<>(next);
        changed.addAll(dynamicCells);
        changed.removeIf(pos -> next.contains(pos) == dynamicCells.contains(pos));
        dynamicCells = next;
        for (BlockPos pos : changed) minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    public static ModelData maskData(BlockPos pos, BlockState state) {
        var level = Minecraft.getInstance().level;
        Vec3 origin = Vec3.atLowerCornerOf(pos).add(state.getOffset(level, pos));
        var columns = masks(level).getOrDefault(pos, List.of()).stream().map(c -> c.relativeTo(origin)).toList();
        return ModelData.builder().with(POSITION, pos).with(MASK, columns).build();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || dynamicCells.isEmpty()) return;
        var masks = masks(level);
        var buffers = minecraft.renderBuffers().bufferSource();
        var type = RenderType.cutout();
        var vertices = buffers.getBuffer(type);
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        for (BlockPos pos : dynamicCells) {
            BlockState state = level.getBlockState(pos);
            if (!plant(state)) continue;
            Vec3 origin = Vec3.atLowerCornerOf(pos).add(state.getOffset(level, pos));
            var columns = masks.getOrDefault(pos, List.of()).stream().map(c -> c.relativeTo(origin)).toList();
            ModelData data = ModelData.builder().with(POSITION, pos).with(MASK, columns).build();
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            minecraft.getBlockRenderer().renderBatched(state, pos, level, pose, vertices, false, RandomSource.create(state.getSeed(pos)), data, type);
            pose.popPose();
        }
        buffers.endBatch(type);
    }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Models {
        @SubscribeEvent
        public static void bake(ModelEvent.ModifyBakingResult event) {
            textures.clear(); captures.clear();
            event.getModels().replaceAll((id, model) -> id instanceof ModelResourceLocation location
                    && location.getNamespace().equals("minecraft") && Set.of("seagrass", "tall_seagrass", "kelp", "kelp_plant").contains(location.getPath())
                    && !location.getVariant().equals("inventory") ? new ClippedModel(model) : model);
        }
    }

    private static final class ClippedModel extends BakedModelWrapper<BakedModel> {
        private ClippedModel(BakedModel original) { super(original); }

        @Override
        public ModelData getModelData(BlockAndTintGetter view, BlockPos pos, BlockState state, ModelData data) {
            return super.getModelData(view, pos, state, data).derive().with(POSITION, pos.immutable()).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType type) {
            var quads = originalModel.getQuads(state, side, random, data, type);
            var columns = data.get(MASK);
            if (columns != null) return quads.stream().flatMap(q -> AquaticPlantMask.clip(q, columns).stream()).toList();
            BlockPos pos = data.get(POSITION);
            return pos != null && dynamicCells.contains(pos) ? List.of() : quads;
        }
    }
}
