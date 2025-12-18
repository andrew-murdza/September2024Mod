package net.amurdza.examplemod.worldgen.structure;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AOEMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, AOEMod.MOD_ID);

    public static final RegistryObject<StructureType<MountainLakeStructure>> MOUNTAIN_LAKE_STRUCTURE =
            STRUCTURE_TYPES.register("mountain_lake", () -> () -> MountainLakeStructure.CODEC);

    public static final RegistryObject<StructurePieceType> MOUNTAIN_LAKE_PIECE =
            PIECE_TYPES.register("mountain_lake_piece", () -> MountainLakePiece::new);

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
    }
}
