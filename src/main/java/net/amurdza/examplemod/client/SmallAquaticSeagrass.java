package net.amurdza.examplemod.client;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only the temporary rendered blades change; plant blocks and their drops remain intact. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class SmallAquaticSeagrass {
    private static final ModelProperty<Boolean> CLIPPED = new ModelProperty<>();
    private static volatile Set<BlockPos> clipped = Set.of();
    private static ClientLevel activeLevel;

    private SmallAquaticSeagrass() { }

    public static boolean clippedAt(BlockPos pos) {
        return clipped.contains(pos);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (activeLevel != level) {
            activeLevel = level;
            clipped = Set.of();
        }
        if (level == null) return;
        Set<BlockPos> next = new HashSet<>();
        for (var entity : level.entitiesForRendering()) {
            if (!entity.isAlive() || !SmallAquaticPlants.affected(entity)) continue;
            // Include the native model's legs, which extend slightly beyond its body box.
            AABB box = entity.getBoundingBox().inflate(.15, 0, .15);
            for (BlockPos pos : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY),
                    (int) Math.floor(box.minZ), (int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ))) {
                if (level.getBlockState(pos).is(Blocks.SEAGRASS) && box.intersects(new AABB(pos))) next.add(pos.immutable());
            }
        }
        if (next.equals(clipped)) return;
        Set<BlockPos> changed = new HashSet<>(next);
        changed.addAll(clipped);
        changed.removeIf(pos -> next.contains(pos) == clipped.contains(pos));
        clipped = Set.copyOf(next);
        // Rebuild only crossed plant cells when the animal enters/leaves, including
        // reappearance after removal. Both vanilla and Embeddium consume model data.
        for (BlockPos pos : changed) minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Models {
        @SubscribeEvent
        public static void bake(ModelEvent.ModifyBakingResult event) {
            event.getModels().replaceAll((id, model) -> id instanceof ModelResourceLocation location
                    && location.getNamespace().equals("minecraft") && location.getPath().equals("seagrass")
                    && !location.getVariant().equals("inventory") ? new ClippedModel(model) : model);
        }
    }

    private static final class ClippedModel extends BakedModelWrapper<BakedModel> {
        private ClippedModel(BakedModel original) { super(original); }

        @Override
        public ModelData getModelData(BlockAndTintGetter view, BlockPos pos, BlockState state, ModelData data) {
            return super.getModelData(view, pos, state, data).derive().with(CLIPPED, clipped.contains(pos)).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType type) {
            var quads = originalModel.getQuads(state, side, random, data, type);
            return Boolean.TRUE.equals(data.get(CLIPPED)) ? quads.stream().map(SmallAquaticSeagrass::clip).toList() : quads;
        }
    }

    public static BakedQuad clip(BakedQuad quad) {
        int[] original = quad.getVertices(), vertices = original.clone();
        int stride = vertices.length / 4;
        float bottom = Float.POSITIVE_INFINITY, top = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float y = Float.intBitsToFloat(original[vertex * stride + 1]);
            bottom = Math.min(bottom, y);
            top = Math.max(top, y);
        }
        float cutoff = Math.min(top, bottom + .125F);
        if (top <= cutoff) return quad;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float y = Float.intBitsToFloat(original[index + 1]);
            if (y <= cutoff) continue;
            // Vanilla seagrass consists of vertical cross planes. Find the foot
            // of this top vertex so the texture is cropped, never squashed.
            int foot = -1;
            for (int other = 0; other < 4; other++) {
                int candidate = other * stride;
                if (Float.intBitsToFloat(original[candidate + 1]) == bottom
                        && original[candidate] == original[index] && original[candidate + 2] == original[index + 2]) foot = candidate;
            }
            if (foot < 0) return quad;
            float fraction = (cutoff - bottom) / (y - bottom);
            vertices[index + 1] = Float.floatToRawIntBits(cutoff);
            for (int uv = 4; uv <= 5; uv++) {
                float lower = Float.intBitsToFloat(original[foot + uv]);
                vertices[index + uv] = Float.floatToRawIntBits(lower + fraction * (Float.intBitsToFloat(original[index + uv]) - lower));
            }
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
}
