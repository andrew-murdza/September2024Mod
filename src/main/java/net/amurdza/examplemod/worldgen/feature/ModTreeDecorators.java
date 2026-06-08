package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumSet;

public class ModTreeDecorators {

    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
            DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, AOEMod.MOD_ID);

    public static final RegistryObject<TreeDecoratorType<ModTrunkVineDecorator>> WEIGHTED_MULTIFACE_ON_LOGS =
            TREE_DECORATORS.register("trunk_vine_decorator",
                    () -> new TreeDecoratorType<>(ModTrunkVineDecorator.CODEC));

    public static final RegistryObject<TreeDecoratorType<CaveVineTreeDecorator>> CAVE_VINES =
            TREE_DECORATORS.register("cave_vines",
                    () -> new TreeDecoratorType<>(CaveVineTreeDecorator.CODEC));

    public static void register(IEventBus modEventBus) {
        TREE_DECORATORS.register(modEventBus);
    }
}