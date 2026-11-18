package net.amurdza.examplemod.worldgen.structure;

import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.registries.ForgeRegistries;

/** Sixteen authored tents inside a square stockade; saved older camps retain their geometry. */
public final class SavannaOrcCampPiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = new ResourceLocation("aoemod", "orc_village");
    public static final int WIDTH = SavannaOrcVillage.SIZE;
    public static final int DEPTH = SavannaOrcVillage.SIZE;
    // Append the restored boss to preserve the saved placement bits of both guards.
    private static final String[] RESIDENTS = {"orcz:orcshield", "orcz:orcsavagemace", "orcz:bigorcboss",
            "orcz:orcshield", "orcz:orcsavagemace", "orcz:orcshield", "orcz:orcsavagemace"};
    private static final BlockPos[] RESIDENT_POSITIONS = {
            new BlockPos(40, 3, 68), new BlockPos(50, 3, 68), new BlockPos(45, 3, 20),
            new BlockPos(33, 3, 23), new BlockPos(54, 3, 23), new BlockPos(33, 3, 52), new BlockPos(64, 3, 52)};
    private static final BlockPos[] LEGACY_RESIDENT_POSITIONS = {
            new BlockPos(8, 3, 24), new BlockPos(17, 3, 21), new BlockPos(17, 3, 14),
            new BlockPos(-11, 3, 10), new BlockPos(-11, 3, 31), new BlockPos(44, 3, 10), new BlockPos(44, 3, 31)};
    private static final int[][] PEN_OFFSETS = {
            // The hay stack reaches (6, 5). Wider stock such as emus and bison
            // overlaps it when centered at (7.5, 5.5), so use the clear row.
            {3, 5}, {7, 7}, {10, 6}, {3, 10}, {7, 10}, {10, 11}, {6, 16}, {10, 17}};
    private final boolean authoredVillage;
    private final boolean expandedVillage;
    private final boolean circularVillage;
    private final boolean squareVillage;
    private final boolean expandedSquareVillage;
    private final boolean compactSquareVillage;
    private final boolean revisedSquareVillage;
    private final boolean shallowGateVillage;
    private final boolean defendedVillage;
    private final java.util.UUID campEncounterId;
    private SavannaOrcVillage.GroundPlan groundPlan;
    private LegacySavannaOrcVillageV10.GroundPlan revisedSquareGroundPlan;
    private LegacySavannaOrcVillageV8.GroundPlan compactSquareGroundPlan;
    private LegacySavannaOrcVillageV7.GroundPlan expandedSquareGroundPlan;
    private LegacySavannaOrcVillageV6.GroundPlan squareGroundPlan;
    private LegacySavannaOrcVillageV5.GroundPlan circularGroundPlan;
    private java.util.BitSet villageResidents = new java.util.BitSet();
    private java.util.BitSet villageCushions = new java.util.BitSet();
    private java.util.BitSet villageAnimals = new java.util.BitSet();
    private int residentsPlaced;
    private int cushionsPlaced;
    private int animalsPlaced;
    private final boolean rebuiltSettlement;
    private final boolean terrainRamps;
    private final BoundingBox settlementBounds;

    public SavannaOrcCampPiece(StructureTemplateManager templates, BlockPos groundCenter) {
        super(ModStructures.ORC_CAMP_PIECE.get(), 0, templates, TEMPLATE, TEMPLATE.toString(), settings(),
                groundCenter.offset(-SavannaOrcVillage.CENTER, 0, -SavannaOrcVillage.CENTER));
        authoredVillage = true;
        expandedVillage = true;
        circularVillage = false;
        squareVillage = true;
        expandedSquareVillage = true;
        compactSquareVillage = true;
        revisedSquareVillage = true;
        shallowGateVillage = true;
        defendedVillage = true;
        campEncounterId = java.util.UUID.randomUUID();
        rebuiltSettlement = true;
        terrainRamps = true;
        settlementBounds = authoredVillage ? SavannaOrcVillage.bounds(templatePosition)
                : SavannaOrcSettlement.bounds(templatePosition, boundingBox.maxY(), rebuiltSettlement, terrainRamps);
        boundingBox = settlementBounds;
    }

    public SavannaOrcCampPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.ORC_CAMP_PIECE.get(), compatibleTemplate(tag), context.structureTemplateManager(), ignored -> settings());
        authoredVillage = tag.getInt("SettlementVersion") >= 3 || TEMPLATE.toString().equals(tag.getString("Template"));
        expandedVillage = tag.getInt("SettlementVersion") >= 4;
        circularVillage = tag.getInt("SettlementVersion") == 5;
        squareVillage = tag.getInt("SettlementVersion") >= 6;
        expandedSquareVillage = tag.getInt("SettlementVersion") >= 7;
        compactSquareVillage = tag.getInt("SettlementVersion") >= 8;
        revisedSquareVillage = tag.getInt("SettlementVersion") >= 9;
        shallowGateVillage = tag.getInt("SettlementVersion") >= 10;
        defendedVillage = tag.getInt("SettlementVersion") >= 11;
        campEncounterId = defendedVillage ? (tag.hasUUID("CampEncounterId") ? tag.getUUID("CampEncounterId")
                : java.util.UUID.nameUUIDFromBytes(("savanna-camp-v11:" + templatePosition.asLong())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))) : null;
        villageResidents = java.util.BitSet.valueOf(tag.getLongArray("VillageResidents"));
        villageCushions = java.util.BitSet.valueOf(tag.getLongArray("VillageCushions"));
        villageAnimals = java.util.BitSet.valueOf(tag.getLongArray("VillageAnimals"));
        residentsPlaced = tag.getInt("ResidentsPlaced");
        cushionsPlaced = tag.getInt("CushionsPlaced");
        animalsPlaced = tag.getInt("AnimalsPlaced");
        terrainRamps = tag.getInt("SettlementVersion") >= 2;
        rebuiltSettlement = tag.getInt("SettlementVersion") >= 1 || "aoemod:orc_settlement".equals(tag.getString("Template"));
        settlementBounds = authoredVillage ? (revisedSquareVillage ? SavannaOrcVillage.bounds(templatePosition)
                : compactSquareVillage ? LegacySavannaOrcVillageV8.bounds(templatePosition)
                : expandedSquareVillage ? LegacySavannaOrcVillageV7.bounds(templatePosition)
                : squareVillage ? LegacySavannaOrcVillageV6.bounds(templatePosition)
                : circularVillage ? LegacySavannaOrcVillageV5.bounds(templatePosition)
                : expandedVillage ? LegacySavannaOrcVillageV4.bounds(templatePosition) : LegacySavannaOrcVillageV3.bounds(templatePosition))
                : SavannaOrcSettlement.bounds(templatePosition, boundingBox.maxY(), rebuiltSettlement, terrainRamps);
        boundingBox = settlementBounds;
    }

    private static CompoundTag compatibleTemplate(CompoundTag original) {
        if (original.getInt("SettlementVersion") < 11 && TEMPLATE.toString().equals(original.getString("Template"))) {
            CompoundTag copy = original.copy();
            copy.putString("Template", original.getInt("SettlementVersion") >= 10 ? "aoemod:orc_village_v10"
                    : original.getInt("SettlementVersion") >= 9 ? "aoemod:orc_village_v9"
                    : original.getInt("SettlementVersion") >= 8 ? "aoemod:orc_village_v8"
                    : original.getInt("SettlementVersion") >= 7 ? "aoemod:orc_village_v7"
                    : original.getInt("SettlementVersion") >= 6 ? "aoemod:orc_village_v6"
                    : original.getInt("SettlementVersion") >= 5 ? "aoemod:orc_village_v5"
                    : original.getInt("SettlementVersion") >= 4 ? "aoemod:orc_village_v4" : "aoemod:orc_village_v3");
            return copy;
        }
        return original;
    }

    @Override
    public BoundingBox getBoundingBox() {
        // Native placement temporarily changes its working box to the template
        // core. Other chunks must still see the foundation depth throughout.
        // Outside placement this is also the inherited box, so move() updates
        // this same object together with templatePosition.
        return settlementBounds == null ? super.getBoundingBox() : settlementBounds;
    }

    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings().setIgnoreEntities(false)
                // The composed template already includes the complete perimeter
                // topology, including neighbors beyond this chunk's clip.
                .setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(SavannaOrcCampProcessor.INSTANCE);
    }

    @Override
    protected synchronized void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("ResidentsPlaced", residentsPlaced);
        tag.putInt("CushionsPlaced", cushionsPlaced);
        tag.putInt("AnimalsPlaced", animalsPlaced);
        if (rebuiltSettlement) tag.putInt("SettlementVersion", defendedVillage ? 11 : shallowGateVillage ? 10 : revisedSquareVillage ? 9 : compactSquareVillage ? 8 : expandedSquareVillage ? 7 : squareVillage ? 6 : circularVillage ? 5 : expandedVillage ? 4 : authoredVillage ? 3 : terrainRamps ? 2 : 1);
        if (campEncounterId != null) tag.putUUID("CampEncounterId", campEncounterId);
        if (authoredVillage) {
            tag.putLongArray("VillageResidents", villageResidents.toLongArray());
            tag.putLongArray("VillageCushions", villageCushions.toLongArray());
            tag.putLongArray("VillageAnimals", villageAnimals.toLongArray());
        }
    }

    @Override
    public synchronized void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        // /place structure has no saved StructureStart. Keep its authored area
        // available to the same plant and ambient-spawn rules after reopening.
        if (authoredVillage && level instanceof net.minecraft.server.level.ServerLevel server) {
            int center = revisedSquareVillage ? SavannaOrcVillage.CENTER : compactSquareVillage ? LegacySavannaOrcVillageV8.CENTER : expandedSquareVillage ? LegacySavannaOrcVillageV7.CENTER : squareVillage ? LegacySavannaOrcVillageV6.CENTER : expandedVillage ? LegacySavannaOrcVillageV5.CENTER : LegacySavannaOrcVillageV3.CENTER;
            int radius = revisedSquareVillage ? SavannaOrcVillage.RADIUS : compactSquareVillage ? LegacySavannaOrcVillageV8.RADIUS : expandedSquareVillage ? LegacySavannaOrcVillageV7.RADIUS : squareVillage ? LegacySavannaOrcVillageV6.RADIUS : expandedVillage ? LegacySavannaOrcVillageV5.RADIUS : LegacySavannaOrcVillageV3.RADIUS;
            PlacedOrcVillages.get(server).record(templatePosition.offset(center, 2, center), radius,
                    squareVillage ? "square" : circularVillage ? "circle" : "cubic");
        }
        if (defendedVillage) {
            if (level instanceof net.minecraft.server.level.ServerLevel server) SavannaCampProgress.register(server, campEncounterId, templatePosition);
            if (groundPlan == null) groundPlan = SavannaOrcVillage.planGround(level, templatePosition);
            // Native structure placement precedes biome vegetation. Establish
            // the mound first, then retain the authored blocks on its surface.
            SavannaOrcVillage.placeGround(level, chunk, templatePosition, groundPlan);
        } else if (revisedSquareVillage) {
            if (revisedSquareGroundPlan == null) revisedSquareGroundPlan = LegacySavannaOrcVillageV10.planGround(level, templatePosition);
            LegacySavannaOrcVillageV10.placeGround(level, chunk, templatePosition, revisedSquareGroundPlan);
        } else if (compactSquareVillage) {
            if (compactSquareGroundPlan == null) compactSquareGroundPlan = LegacySavannaOrcVillageV8.planGround(level, templatePosition);
            LegacySavannaOrcVillageV8.placeGround(level, chunk, templatePosition, compactSquareGroundPlan);
        } else if (expandedSquareVillage) {
            if (expandedSquareGroundPlan == null) expandedSquareGroundPlan = LegacySavannaOrcVillageV7.planGround(level, templatePosition);
            LegacySavannaOrcVillageV7.placeGround(level, chunk, templatePosition, expandedSquareGroundPlan);
        } else if (squareVillage) {
            if (squareGroundPlan == null) squareGroundPlan = LegacySavannaOrcVillageV6.planGround(level, templatePosition);
            LegacySavannaOrcVillageV6.placeGround(level, chunk, templatePosition, squareGroundPlan);
        } else if (circularVillage) {
            if (circularGroundPlan == null) circularGroundPlan = LegacySavannaOrcVillageV5.planGround(level, templatePosition);
            LegacySavannaOrcVillageV5.placeGround(level, chunk, templatePosition, circularGroundPlan);
        }
        super.postProcess(level, structures, generator, random, chunk, chunkPos, pivot);
        boundingBox = settlementBounds;
        // The completed ground plan identifies exactly which existing reservations
        // this placement overwrote; SPAWN later needs only this chunk's saved mask.
        net.amurdza.examplemod.worldgen.SavannaCampColumns.record(level.getChunk(chunkPos.x, chunkPos.z), this);
        if (usesNativeVegetation()) net.amurdza.examplemod.worldgen.SavannaCampVegetation.prepareMoundChunk(level, chunkPos);
        if (authoredVillage) {
            if (defendedVillage) {
                SavannaOrcVillage.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals, campEncounterId);
            } else if (revisedSquareVillage) {
                LegacySavannaOrcVillageV10.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else if (compactSquareVillage) {
                LegacySavannaOrcVillageV8.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else if (expandedSquareVillage) {
                LegacySavannaOrcVillageV7.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else if (squareVillage) {
                LegacySavannaOrcVillageV6.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else if (circularVillage) {
                LegacySavannaOrcVillageV5.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else if (expandedVillage) {
                LegacySavannaOrcVillageV4.placeGround(level, chunk, templatePosition);
                LegacySavannaOrcVillageV4.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            } else {
                LegacySavannaOrcVillageV3.placeGround(level, chunk, templatePosition);
                LegacySavannaOrcVillageV3.populate(level, chunk, templatePosition, villageResidents, villageCushions, villageAnimals);
            }
            return;
        }
        SavannaOrcSettlement.place(level, chunk, templatePosition, rebuiltSettlement, terrainRamps);
        supportFoundation(level, chunk);
        for (int i = 0; i < RESIDENTS.length; i++) {
            BlockPos pos = templatePosition.offset((rebuiltSettlement ? RESIDENT_POSITIONS : LEGACY_RESIDENT_POSITIONS)[i]);
            if ((residentsPlaced & (1 << i)) == 0 && chunk.isInside(pos)
                    && BossLairPiece.spawn(level, RESIDENTS[i], pos)) residentsPlaced |= 1 << i;
        }
        int selection = OrcCampStock.selection(level.getSeed(), templatePosition);
        for (int i = 0; rebuiltSettlement && i < 16; i++) {
            int[] offset = PEN_OFFSETS[i % 8];
            BlockPos pos = templatePosition.offset((i < 8 ? 14 : 64) + offset[0], 3, 28 + offset[1]);
            if ((animalsPlaced & (1 << i)) != 0 || !chunk.isInside(pos)) continue;
            if (BossLairPiece.spawn(level, OrcCampStock.animal(selection, i / 4), pos)) animalsPlaced |= 1 << i;
        }
        var seats = SavannaOrcCampProcessor.cushionSeats(template);
        for (int i = 0; i < seats.size(); i++) {
            BlockPos pos = templatePosition.offset(seats.get(i));
            if ((cushionsPlaced & (1 << i)) != 0 || !chunk.isInside(pos)) continue;
            BlockPos support = pos.below();
            var supportShape = level.getBlockState(support).getShape(level, support);
            if (supportShape.isEmpty()) continue;
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft", "cushion"));
            if (type == null || !(type.create(level.getLevel()) instanceof Cushion cushion)) continue;
            cushion.setColor(DyeColor.RED);
            cushion.setPos(pos.getX() + .5, support.getY() + supportShape.max(Direction.Axis.Y), pos.getZ() + .5);
            if (level.addFreshEntity(cushion)) cushionsPlaced |= 1 << i;
        }
    }

    private void supportFoundation(WorldGenLevel level, BoundingBox chunk) {
        for (int x = Math.max(chunk.minX(), boundingBox.minX()); x <= Math.min(chunk.maxX(), boundingBox.maxX()); x++) {
            for (int z = Math.max(chunk.minZ(), boundingBox.minZ()); z <= Math.min(chunk.maxZ(), boundingBox.maxZ()); z++) {
                if (rebuiltSettlement && !SavannaOrcSettlement.containsGround(x - templatePosition.getX(), z - templatePosition.getZ())) continue;
                BlockPos base = new BlockPos(x, templatePosition.getY(), z);
                if (rebuiltSettlement) {
                    if (SavannaOrcSettlement.containsEntrance(x - templatePosition.getX(), z - templatePosition.getZ())) continue;
                    boolean water = level.getBlockState(base.above(2)).is(Blocks.WATER);
                    // A camp may sit eight blocks above the lowest native surface.
                    // Its five soil layers can therefore continue below the former apron.
                    for (int dy = 1; dy >= -13; dy--) {
                        BlockPos pos = base.above(dy);
                        if (!chunk.isInside(pos)) continue;
                        var material = water || dy < -2 ? Blocks.MUD : dy >= 0 ? Blocks.ROOTED_DIRT : Blocks.DIRT;
                        if (dy < -2 && level.getBlockState(pos).is(Blocks.MUD)) break;
                        level.setBlock(pos, material.defaultBlockState(), 2);
                    }
                    continue;
                }
                if (!level.getBlockState(base).isSolidRender(level, base)) continue;
                for (int depth = 1; depth <= 8; depth++) {
                    BlockPos pos = base.below(depth);
                    if (!chunk.isInside(pos)) break;
                    var state = level.getBlockState(pos);
                    if (!state.canBeReplaced() && !state.is(BlockTags.LEAVES)) break;
                    level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
                }
            }
        }
    }

    public boolean containsSurfaceColumn(BlockPos pos) {
        int x = pos.getX() - templatePosition.getX(), z = pos.getZ() - templatePosition.getZ();
        return authoredVillage ? (revisedSquareVillage ? SavannaOrcVillage.contains(x, z)
                : compactSquareVillage ? LegacySavannaOrcVillageV8.contains(x, z)
                : expandedSquareVillage ? LegacySavannaOrcVillageV7.contains(x, z)
                : squareVillage ? LegacySavannaOrcVillageV6.contains(x, z)
                : circularVillage ? LegacySavannaOrcVillageV5.contains(x, z)
                : expandedVillage ? LegacySavannaOrcVillageV4.contains(x, z) : LegacySavannaOrcVillageV3.contains(x, z))
                : rebuiltSettlement ? SavannaOrcSettlement.containsGround(x, z) : getBoundingBox().isInside(pos);
    }

    public boolean isCircularVillage() {
        return circularVillage;
    }

    public boolean isCompactSquareVillage() {
        return compactSquareVillage;
    }

    public boolean isRevisedSquareVillage() {
        return revisedSquareVillage;
    }

    public boolean isDefendedVillage() { return defendedVillage; }

    public java.util.UUID encounterId() { return campEncounterId; }

    public boolean isShallowGateVillage() {
        return shallowGateVillage;
    }

    public boolean isExpandedSquareVillage() {
        return expandedSquareVillage;
    }

    public boolean isSquareVillage() {
        return squareVillage;
    }

    public boolean usesNativeVegetation() {
        return circularVillage || squareVillage;
    }

    public boolean hasMoundColumn(BlockPos pos) {
        int x = pos.getX() - templatePosition.getX(), z = pos.getZ() - templatePosition.getZ();
        return defendedVillage ? SavannaOrcVillage.hasMoundColumn(x, z, groundPlan)
                : revisedSquareVillage ? LegacySavannaOrcVillageV10.hasMoundColumn(x, z, revisedSquareGroundPlan)
                : compactSquareVillage ? LegacySavannaOrcVillageV8.hasMoundColumn(x, z, compactSquareGroundPlan)
                : expandedSquareVillage ? LegacySavannaOrcVillageV7.hasMoundColumn(x, z, expandedSquareGroundPlan)
                : squareVillage ? LegacySavannaOrcVillageV6.hasMoundColumn(x, z, squareGroundPlan)
                : circularVillage && LegacySavannaOrcVillageV5.hasMoundColumn(x, z, circularGroundPlan);
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox chunk) { }
}
