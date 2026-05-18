package net.amurdza.examplemod.worldgen.carver;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraftforge.registries.DeferredRegister;

public class ModCarvers {
    public static final DeferredRegister<WorldCarver<?>> CARVERS =
            DeferredRegister.create(Registries.CARVER, AOEMod.MOD_ID);

//    public static final RegistryObject<WorldCarver<CaveCarverConfiguration>> LONG_NETHER_LAYER_CAVE =
//            CARVERS.register("long_nether_layer_cave",
//                    () -> new LongNetherLayerCaveCarver(CaveCarverConfiguration.CODEC));
}