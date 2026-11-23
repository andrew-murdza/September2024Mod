package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.BandStructurePlacement;
import net.amurdza.examplemod.worldgen.structure.IslandBossPlacement;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPlacement;
import net.amurdza.examplemod.worldgen.structure.PlateauLairPlacement;
import net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement;
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

    public static final RegistryObject<StructurePlacementType<PlateauLairPlacement>> PLATEAU_LAIR =
            STRUCTURE_PLACEMENT_TYPES.register("plateau_lair", () -> () -> PlateauLairPlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<GoblinFortressPlacement>> GOBLIN_FORTRESS =
            STRUCTURE_PLACEMENT_TYPES.register("goblin_fortress", () -> () -> GoblinFortressPlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<IslandBossPlacement>> ISLAND_BOSS =
            STRUCTURE_PLACEMENT_TYPES.register("island_boss", () -> () -> IslandBossPlacement.CODEC);

    public static final RegistryObject<StructurePlacementType<SavannaCampPlacement>> SAVANNA_CAMP =
            STRUCTURE_PLACEMENT_TYPES.register("savanna_camp", () -> () -> SavannaCampPlacement.CODEC);
}
