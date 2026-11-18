package net.amurdza.examplemod.client;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalPlantLighting;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Choose the vibrant palette using render-region model data, never a global current-world lookup. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TropicalAzaleaModels {
    private static final ModelProperty<Boolean> TROPICAL = new ModelProperty<>();
    private static final ResourceLocation PLAIN = AOEMod.makeID("block/tropical_azalea_leaves");
    private static final ResourceLocation FLOWERING = AOEMod.makeID("block/tropical_flowering_azalea_leaves");

    @SubscribeEvent
    public static void additional(ModelEvent.RegisterAdditional event) {
        event.register(PLAIN);
        event.register(FLOWERING);
    }

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        BakedModel plain = event.getModels().get(PLAIN), flowering = event.getModels().get(FLOWERING);
        if (plain == null || flowering == null) throw new IllegalStateException("Missing tropical azalea palette models");
        event.getModels().replaceAll((id, model) -> {
            if (!(id instanceof ModelResourceLocation variant) || !id.getNamespace().equals("minecraft")
                    || variant.getVariant().equals("inventory")) return model;
            return switch (id.getPath()) {
                case "azalea_leaves" -> new PaletteModel(model, plain);
                case "flowering_azalea_leaves" -> new PaletteModel(model, flowering);
                default -> model;
            };
        });
    }

    private static final class PaletteModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel tropical;
        private PaletteModel(BakedModel original, BakedModel tropical) { super(original); this.tropical = tropical; }

        @Override
        public ModelData getModelData(BlockAndTintGetter view, BlockPos pos, BlockState state, ModelData data) {
            boolean isTropical = view instanceof LevelReader reader ? GameplayDimensions.is(reader, GameplayDimensions.TROPICAL)
                    : view instanceof TropicalPlantLighting.DimensionContext context
                    && GameplayDimensions.TROPICAL.equals(context.aoemod$lightingDimension());
            return super.getModelData(view, pos, state, data).derive().with(TROPICAL, isTropical).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                       ModelData data, @Nullable RenderType type) {
            return (Boolean.TRUE.equals(data.get(TROPICAL)) ? tropical : originalModel).getQuads(state, side, random, data, type);
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            return (Boolean.TRUE.equals(data.get(TROPICAL)) ? tropical : originalModel).getParticleIcon(data);
        }
    }
}
