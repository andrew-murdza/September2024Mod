package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.LongMushroomCaveStructure;
import net.amurdza.examplemod.worldgen.structure.LongNetherCaveStructure;
import net.amurdza.examplemod.worldgen.structure.MushroomCavePiece;
import net.amurdza.examplemod.worldgen.structure.NetherCavePiece;
import net.amurdza.examplemod.worldgen.structure.BossLairStructure;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionStructure;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.amurdza.examplemod.worldgen.structure.MansionBossPiece;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AOEMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, AOEMod.MOD_ID);

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSOR_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, AOEMod.MOD_ID);
    public static final RegistryObject<StructureProcessorType<IllagerMansionProcessor>> MANSION_PROCESSOR =
            PROCESSOR_TYPES.register("plains_mansion_materials", () -> () -> IllagerMansionProcessor.CODEC);
    public static final RegistryObject<StructureProcessorType<SavannaOrcCampProcessor>> SAVANNA_ORC_CAMP_PROCESSOR =
            PROCESSOR_TYPES.register("savanna_orc_camp_materials", () -> () -> SavannaOrcCampProcessor.CODEC);

    public static final RegistryObject<StructureType<net.amurdza.examplemod.worldgen.structure.GoblinFortressStructure>> GOBLIN_FORTRESS =
            STRUCTURE_TYPES.register("goblin_fortress", () -> () -> net.amurdza.examplemod.worldgen.structure.GoblinFortressStructure.CODEC);
    public static final RegistryObject<StructurePieceType> GOBLIN_FORTRESS_PIECE =
            PIECE_TYPES.register("goblin_fortress", () -> net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece::new);

    public static final RegistryObject<StructureType<BossLairStructure>> BOSS_LAIR =
            STRUCTURE_TYPES.register("boss_lair", () -> () -> BossLairStructure.CODEC);
    public static final RegistryObject<StructurePieceType> BOSS_LAIR_PIECE =
            PIECE_TYPES.register("boss_lair_piece", () -> BossLairPiece::new);
    public static final RegistryObject<StructurePieceType> ORC_CAMP_PIECE =
            PIECE_TYPES.register("savanna_orc_camp_piece", () -> SavannaOrcCampPiece::new);
    public static final RegistryObject<StructureType<IllagerMansionStructure>> ILLAGER_MANSION =
            STRUCTURE_TYPES.register("illager_mansion", () -> () -> IllagerMansionStructure.CODEC);
    public static final RegistryObject<StructurePieceType> MANSION_BOSS_PIECE =
            PIECE_TYPES.register("mansion_boss_piece", () -> MansionBossPiece::new);
    public static final RegistryObject<StructurePieceType> MANSION_RESIDENTS_PIECE =
            PIECE_TYPES.register("mansion_residents", () -> net.amurdza.examplemod.worldgen.structure.MansionResidentsPiece::new);
    public static final RegistryObject<StructurePieceType> MANSION_BASEMENT_PIECE =
            PIECE_TYPES.register("mansion_basement", () -> MansionBasementPiece::load);
    public static final RegistryObject<StructurePieceType> MANSION_ENVELOPE_PIECE =
            PIECE_TYPES.register("mansion_envelope", () -> net.amurdza.examplemod.worldgen.structure.MansionEnvelopePiece::new);

    public static final RegistryObject<StructureType<LongNetherCaveStructure>> NETHER_CAVE =
            STRUCTURE_TYPES.register("nether_cave", () -> LongNetherCaveStructure.CODEC::codec);

    public static final RegistryObject<StructurePieceType> NETHER_CAVE_PIECE =
            PIECE_TYPES.register("nether_cave_piece", () -> NetherCavePiece::new);

    public static final RegistryObject<StructureType<LongMushroomCaveStructure>> MUSHROOM_CAVE =
            STRUCTURE_TYPES.register("mushroom_cave", () -> LongMushroomCaveStructure.CODEC::codec);

    public static final RegistryObject<StructurePieceType> MUSHROOM_CAVE_PIECE =
            PIECE_TYPES.register("mushroom_cave_piece", () -> MushroomCavePiece::new);

    public static final RegistryObject<StructureType<net.amurdza.examplemod.worldgen.structure.NetherCitadelStructure>> NETHER_CITADEL =
            STRUCTURE_TYPES.register("nether_citadel", () -> () -> net.amurdza.examplemod.worldgen.structure.NetherCitadelStructure.CODEC);
    public static final RegistryObject<StructurePieceType> NETHER_CITADEL_PIECE =
            PIECE_TYPES.register("nether_citadel_piece", () -> net.amurdza.examplemod.worldgen.structure.NetherCitadelPiece::new);
    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
        PROCESSOR_TYPES.register(bus);
    }
}
