package net.amurdza.examplemod.worldgen.feature;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTreeDecorators {

    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
            DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, AOEMod.MOD_ID);

    public static final RegistryObject<TreeDecoratorType<ModTrunkVineDecorator>> WEIGHTED_MULTIFACE_ON_LOGS =
            TREE_DECORATORS.register("trunk_vine_decorator",
                    () -> new TreeDecoratorType<>(ModTrunkVineDecorator.CODEC));

    public static void register(IEventBus modEventBus) {
        TREE_DECORATORS.register(modEventBus);
    }
}
