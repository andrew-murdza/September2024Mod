package net.amurdza.examplemod.worldgen.blockstates;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlockStateProviderTypes {
    public static final DeferredRegister<BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.BLOCK_STATE_PROVIDER_TYPES, AOEMod.MOD_ID);

    public static final BlockStateProviderType<DensityRangeBlockStateProvider> DENSITY_RANGE =
            new BlockStateProviderType<>(DensityRangeBlockStateProvider.CODEC);

    static {
        BLOCK_STATE_PROVIDER_TYPES.register("density_range", () -> DENSITY_RANGE);
    }

    public static void register(IEventBus eventBus) {
        BLOCK_STATE_PROVIDER_TYPES.register(eventBus);
    }
}