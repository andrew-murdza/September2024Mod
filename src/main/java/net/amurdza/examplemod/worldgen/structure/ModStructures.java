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

    public static final RegistryObject<StructureType<LongNetherCaveStructure>> NETHER_CAVE =
            STRUCTURE_TYPES.register("nether_cave", () -> LongNetherCaveStructure.CODEC::codec);

    public static final RegistryObject<StructurePieceType> NETHER_CAVE_PIECE =
            PIECE_TYPES.register("nether_cave_piece", () -> NetherCavePiece::new);

    public static final RegistryObject<StructureType<LongMushroomCaveStructure>> MUSHROOM_CAVE =
            STRUCTURE_TYPES.register("mushroom_cave", () -> LongMushroomCaveStructure.CODEC::codec);

    public static final RegistryObject<StructurePieceType> MUSHROOM_CAVE_PIECE =
            PIECE_TYPES.register("mushroom_cave_piece", () -> MushroomCavePiece::new);

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
    }
}
