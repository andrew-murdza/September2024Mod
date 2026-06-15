package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.BandStructurePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructurePlacementTypes {
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AOEMod.MOD_ID);

    public static final RegistryObject<StructurePlacementType<BandStructurePlacement>> Z_BANDS =
            STRUCTURE_PLACEMENT_TYPES.register("z_bands",
                    () -> () -> BandStructurePlacement.CODEC);
}