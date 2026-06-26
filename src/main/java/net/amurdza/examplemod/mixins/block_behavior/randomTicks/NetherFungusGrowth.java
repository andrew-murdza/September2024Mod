package net.amurdza.examplemod.mixins.block_behavior.randomTicks;

import net.amurdza.examplemod.block.BlockHelper;
import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FungusBlock.class)
public abstract class NetherFungusGrowth extends BushBlock {

    public NetherFungusGrowth(Properties pProperties) {
        super(pProperties);
    }

    /**
     * Makes crimson/warped fungus receive random ticks.
     *
     * Vanilla FungusBlock does not randomly tick by default, so adding randomTick
     * alone is not enough.
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * Gives nether fungus mushroom-like random growth behavior.
     */
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockHelper.spreadMushroom(pState, pLevel, pPos, pRandom,
                (level,pos,state,random) -> ((FungusBlock)state.getBlock()).performBonemeal(level,random,pos,state));
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void aoemod$useTemplateGiantFungus(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = null;

        if (state.is(Blocks.CRIMSON_FUNGUS)) {
            featureKey = ModConfiguredFeatures.CRIMSON_FUNGUS_TREE_FROM_FUNGUS;
        } else if (state.is(Blocks.WARPED_FUNGUS)) {
            featureKey = ModConfiguredFeatures.WARPED_FUNGUS_TREE_FROM_FUNGUS;
        }

        if (featureKey == null) {
            return;
        }

        ci.cancel();

        ConfiguredFeature<?, ?> feature = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(featureKey);

        if (feature != null) {
            feature.place(level, level.getChunkSource().getGenerator(), random, pos);
        }
    }
}
