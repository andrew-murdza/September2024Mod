package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.IllagerMansionProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionReplacementTests {
    private static final Map<Block, Block> REPLACEMENTS = Map.ofEntries(
            Map.entry(Blocks.DARK_OAK_DOOR, Blocks.OAK_DOOR),
            Map.entry(Blocks.DARK_OAK_FENCE, Blocks.OAK_FENCE),
            Map.entry(Blocks.DARK_OAK_FENCE_GATE, Blocks.OAK_FENCE_GATE),
            Map.entry(Blocks.DARK_OAK_LEAVES, Blocks.OAK_LEAVES),
            Map.entry(Blocks.DARK_OAK_LOG, Blocks.OAK_LOG),
            Map.entry(Blocks.DARK_OAK_PLANKS, Blocks.OAK_PLANKS),
            Map.entry(Blocks.DARK_OAK_SAPLING, Blocks.OAK_SAPLING),
            Map.entry(Blocks.DARK_OAK_STAIRS, Blocks.OAK_STAIRS),
            Map.entry(Blocks.DIAMOND_BLOCK, Blocks.GOLD_BLOCK),
            Map.entry(Blocks.LAPIS_BLOCK, Blocks.BLUE_WOOL),
            Map.entry(Blocks.OBSIDIAN, Blocks.STONE_BRICKS),
            Map.entry(Blocks.LAVA, Blocks.WATER),
            Map.entry(Blocks.MELON, Blocks.PUMPKIN),
            Map.entry(Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM),
            Map.entry(Blocks.LILY_PAD, Blocks.AIR),
            Map.entry(Blocks.REDSTONE_WIRE, Blocks.AIR),
            Map.entry(Blocks.POTTED_ALLIUM, Blocks.POTTED_LILY_OF_THE_VALLEY),
            Map.entry(Blocks.POTTED_BLUE_ORCHID, Blocks.POTTED_CORNFLOWER),
            Map.entry(Blocks.POTTED_RED_TULIP, Blocks.POTTED_POPPY),
            Map.entry(Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_AZURE_BLUET));

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 300)
    public static void allNativeMansionTemplatesConvertWithoutMutatingTheirSource(GameTestHelper helper) {
        ServerLevel plains = plains(helper);
        var manager = plains.getStructureManager();
        var templates = manager.listTemplates().filter(id -> id.getNamespace().equals("minecraft")
                && id.getPath().startsWith("woodland_mansion/")).toList();
        helper.assertTrue(templates.size() == 73, "Audit must include all 73 native mansion templates: " + templates.size());
        Set<Block> seen = new HashSet<>();
        int alliums = 0, saplings = 0;
        for (var id : templates) {
            var template = manager.getOrCreate(id);
            CompoundTag before = template.save(new CompoundTag());
            for (var original : contents(plains, template)) {
                var converted = IllagerMansionProcessor.INSTANCE.processBlock(plains, BlockPos.ZERO, BlockPos.ZERO,
                        original, original, new StructurePlaceSettings());
                Block source = original.state().getBlock();
                if (REPLACEMENTS.containsKey(source) || source == Blocks.VINE) seen.add(source);
                helper.assertTrue(converted.pos().equals(original.pos())
                                && converted.state().equals(expected(original.state())),
                        "Wrong material or lost block properties in " + id + " at " + original.pos());
                if (!converted.state().is(Blocks.STRUCTURE_BLOCK) && !converted.state().is(Blocks.JIGSAW)) {
                    helper.assertTrue(BiomePlacementRestrictions.isAllowed(plains, original.pos(), converted.state()),
                            "Converted mansion material is still unavailable in Plains: " + id + " / "
                                    + BuiltInRegistries.BLOCK.getKey(converted.state().getBlock()));
                }
                assertChestNbt(helper, original, converted);
                if (original.nbt() != null) for (Tag entry : original.nbt().getList("Items", Tag.TAG_COMPOUND)) {
                    CompoundTag item = (CompoundTag) entry;
                    if (item.getString("id").equals("minecraft:allium")) alliums += item.getByte("Count");
                    if (item.getString("id").equals("minecraft:dark_oak_sapling")) saplings += item.getByte("Count");
                }
                var outside = IllagerMansionProcessor.INSTANCE.processBlock(helper.getLevel(), BlockPos.ZERO,
                        BlockPos.ZERO, original, original, new StructurePlaceSettings());
                helper.assertTrue(outside.state().equals(original.state())
                                && java.util.Objects.equals(outside.nbt(), original.nbt()),
                        "Native mansion materials and chest contents must remain unchanged outside Plains");
            }
            helper.assertTrue(before.equals(template.save(new CompoundTag())), "Processor mutated cached native template " + id);
        }
        helper.assertTrue(seen.size() == 21 && seen.containsAll(REPLACEMENTS.keySet()) && seen.contains(Blocks.VINE),
                "Audit must exercise every one of the 21 disallowed block types");
        helper.assertTrue(alliums == 8 && saplings == 28, "Fixed native chests must retain their eight flowers and 28 saplings");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void rotatedAndReloadedFarmsChestsAndLaddersKeepTheirContents(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        BlockPos origin = helper.absolutePos(new BlockPos(1024, 0, 3072)).atY(220);
        int variant = 0;
        for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
            boolean reload = variant++ % 2 == 0;
            var farm = place(level, "1x2_a8", origin, rotation, mirror, reload);
            checkActualMaterials(helper, level, farm);
            helper.assertTrue(count(level, farm.getBoundingBox(), Blocks.PUMPKIN) == 32,
                    "The native two-floor farm must contain exactly 32 pumpkins: " + rotation + "/" + mirror);
            checkFarmStems(helper, level, farm);
            for (String room : new String[]{"1x1_b5", "1x2_a4", "1x2_a6"}) {
                var piece = place(level, room, origin, rotation, mirror, reload);
                checkActualMaterials(helper, level, piece);
                for (var raw : contents(level, piece.template())) {
                    BlockPos at = worldPos(piece, raw.pos());
                    if (raw.state().is(Blocks.VINE)) {
                        BlockState ladder = level.getBlockState(at);
                        helper.assertTrue(ladder.equals(expected(raw.state()).mirror(mirror).rotate(rotation))
                                        && ladder.canSurvive(level, at),
                                "Each ladder must face away from its original vine attachment and retain solid support");
                    }
                    if (raw.state().is(Blocks.CHEST) && raw.nbt() != null) {
                        helper.assertTrue(level.getBlockEntity(at) instanceof ChestBlockEntity,
                                "Placed fixed chest is missing its block entity");
                        var chest = (ChestBlockEntity) level.getBlockEntity(at);
                        var expectedNbt = replacedChest(raw.nbt());
                        var expectedItems = expectedNbt.getList("Items", Tag.TAG_COMPOUND);
                        int itemCount = 0;
                        for (Tag tag : expectedItems) {
                            CompoundTag item = (CompoundTag) tag;
                            var actual = chest.getItem(item.getByte("Slot"));
                            helper.assertTrue(actual.save(new CompoundTag()).equals(withoutSlot(item)),
                                    "Fixed chest must preserve item slot, count and tags in " + room);
                            itemCount += actual.getCount();
                        }
                        helper.assertTrue(itemCount == (room.equals("1x1_b5") ? 8 : room.equals("1x2_a4") ? 28 : 1),
                                "Fixed chest total changed in " + room);
                    }
                }
                if (room.equals("1x1_b5")) helper.assertTrue(count(level, piece.getBoundingBox(), Blocks.LADDER) == 6,
                        "The allium room must replace exactly six vines with six ladders");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void bothPrisonDoorsWorkAfterRotationMirrorAndPieceReload(GameTestHelper helper) {
        ServerLevel level = plains(helper);
        BlockPos base = helper.absolutePos(new BlockPos(1536, 0, 3072)).atY(220);
        List<DoorControls> rooms = new ArrayList<>();
        int variant = 0;
        for (Rotation rotation : Rotation.values()) for (Mirror mirror : Mirror.values()) {
            BlockPos origin = base.offset(variant % 4 * 48, 0, variant / 4 * 48);
            var piece = place(level, "1x2_b1", origin, rotation, mirror, variant++ % 2 == 0);
            checkActualMaterials(helper, level, piece);
            helper.assertTrue(count(level, piece.getBoundingBox(), Blocks.REDSTONE_WIRE) == 0
                            && count(level, piece.getBoundingBox(), Blocks.LEVER) == 2,
                    "The paired-door prison must have two direct controls and no redstone dust");
            BlockState lever = Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).mirror(mirror).rotate(rotation);
            var controls = new DoorControls(worldPos(piece, new BlockPos(2, 2, 12)), worldPos(piece, new BlockPos(5, 2, 12)),
                    worldPos(piece, new BlockPos(3, 1, 11)), worldPos(piece, new BlockPos(4, 1, 11)));
            for (BlockPos pos : List.of(controls.leftLever(), controls.rightLever())) {
                helper.assertTrue(level.getBlockState(pos).equals(lever) && lever.canSurvive(level, pos),
                        "Both controls must mount on the prison entrance side after every transform");
            }
            rooms.add(controls);
        }
        // Let placement neighbor updates settle before using the real lever block.
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            for (var room : rooms) {
                useLever(helper, level, room.leftLever());
                assertDoor(helper, level, room.leftDoor(), true);
                assertDoor(helper, level, room.rightDoor(), false);
                useLever(helper, level, room.rightLever());
                assertDoor(helper, level, room.rightDoor(), true);
            }
        }).thenIdle(2).thenExecute(() -> {
            for (var room : rooms) {
                assertDoor(helper, level, room.leftDoor(), true);
                assertDoor(helper, level, room.rightDoor(), true);
                useLever(helper, level, room.leftLever());
                useLever(helper, level, room.rightLever());
                assertDoor(helper, level, room.leftDoor(), false);
                assertDoor(helper, level, room.rightDoor(), false);
            }
        }).thenSucceed();
    }

    private static ServerLevel plains(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
    }

    private static List<StructureBlockInfo> contents(ServerLevel level, StructureTemplate template) {
        CompoundTag data = template.save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        List<StructureBlockInfo> blocks = new ArrayList<>();
        for (Tag entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            var pos = block.getList("pos", Tag.TAG_INT);
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), palette.getCompound(block.getInt("state")));
            blocks.add(new StructureBlockInfo(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state,
                    block.contains("nbt", Tag.TAG_COMPOUND) ? block.getCompound("nbt") : null));
        }
        return blocks;
    }

    private static BlockState expected(BlockState state) {
        if (REPLACEMENTS.containsKey(state.getBlock())) return REPLACEMENTS.get(state.getBlock()).withPropertiesOf(state);
        if (state.is(Blocks.VINE)) {
            // Native mansion vines attach either north or west; both orientations occur.
            Direction facing = state.getValue(VineBlock.NORTH) ? Direction.SOUTH : Direction.EAST;
            return Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        return state;
    }

    private static CompoundTag replacedChest(CompoundTag original) {
        CompoundTag expected = original.copy();
        for (Tag entry : expected.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) entry;
            if (item.getString("id").equals("minecraft:allium")) item.putString("id", "minecraft:lily_of_the_valley");
            if (item.getString("id").equals("minecraft:dark_oak_sapling")) item.putString("id", "minecraft:oak_sapling");
        }
        return expected;
    }

    private static void assertChestNbt(GameTestHelper helper, StructureBlockInfo before, StructureBlockInfo after) {
        CompoundTag expected = before.state().is(Blocks.CHEST) && before.nbt() != null ? replacedChest(before.nbt()) : before.nbt();
        if ((before.state().is(Blocks.CHEST) || before.state().is(Blocks.TRAPPED_CHEST))
                && (expected == null || !expected.contains("LootTable", Tag.TAG_STRING)
                && expected.getList("Items", Tag.TAG_COMPOUND).isEmpty())) {
            expected = expected == null ? new CompoundTag() : expected.copy();
            expected.remove("Items");
            expected.putString("LootTable", "aoemod:chests/illager_mansion");
        }
        helper.assertTrue(java.util.Objects.equals(expected, after.nbt()), "Preserve fixed chest supplies and assign loot only to empty generated chests");
    }

    private static CompoundTag withoutSlot(CompoundTag original) {
        CompoundTag item = original.copy();
        item.remove("Slot");
        return item;
    }

    private static WoodlandMansionPiece place(ServerLevel level, String room, BlockPos origin,
                                                Rotation rotation, Mirror mirror, boolean reload) {
        var piece = new WoodlandMansionPiece(level.getStructureManager(), room, origin, rotation, mirror);
        if (reload) piece = new WoodlandMansionPiece(level.getStructureManager(),
                piece.createTag(StructurePieceSerializationContext.fromLevel(level)));
        var box = piece.getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            level.getChunkAt(pos).setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
        }
        // Exercise native per-chunk clipping, including rooms straddling chunk borders.
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            ChunkPos chunk = new ChunkPos(x, z);
            BoundingBox clip = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    RandomSource.create(42), clip, chunk, origin);
        }
        return piece;
    }

    private static BlockPos worldPos(WoodlandMansionPiece piece, BlockPos local) {
        return StructureTemplate.calculateRelativePosition(piece.placeSettings(), local).offset(piece.templatePosition());
    }

    private static void checkActualMaterials(GameTestHelper helper, ServerLevel level, WoodlandMansionPiece piece) {
        for (BlockPos pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(),
                piece.getBoundingBox().minZ(), piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(!REPLACEMENTS.containsKey(state.getBlock()) && !state.is(Blocks.VINE),
                    "Forbidden material survived real mansion placement at " + pos + ": " + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
        for (var raw : contents(level, piece.template())) {
            if (raw.state().is(Blocks.DARK_OAK_PLANKS)) helper.assertTrue(level.getBlockState(worldPos(piece, raw.pos())).is(Blocks.OAK_PLANKS),
                    "Mansion plank floors and walls must remain full blocks at every original plank position");
        }
    }

    private static int count(ServerLevel level, BoundingBox box, Block block) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(pos).is(block)) count++;
        }
        return count;
    }

    private static void checkFarmStems(GameTestHelper helper, ServerLevel level, WoodlandMansionPiece farm) {
        int sourceStems = 0;
        List<String> invalid = new ArrayList<>();
        for (var raw : contents(level, farm.template())) {
            if (!raw.state().is(Blocks.ATTACHED_PUMPKIN_STEM) && !raw.state().is(Blocks.ATTACHED_MELON_STEM)) continue;
            sourceStems++;
            BlockPos pos = worldPos(farm, raw.pos());
            BlockState expectedStem = Blocks.ATTACHED_PUMPKIN_STEM.withPropertiesOf(raw.state())
                    .mirror(farm.placeSettings().getMirror()).rotate(farm.getRotation());
            BlockState actual = level.getBlockState(pos);
            BlockPos fruit = pos.relative(expectedStem.getValue(BlockStateProperties.HORIZONTAL_FACING));
            // Native updateShape detaches a stem to age seven when its fruit
            // belongs to a chunk that has not been placed yet. Completing that
            // neighboring chunk preserves the mature stem instead of reattaching it.
            boolean validStem = actual.equals(expectedStem) || actual.is(Blocks.PUMPKIN_STEM)
                    && actual.getValue(BlockStateProperties.AGE_7) == 7;
            if (!validStem || !level.getBlockState(fruit).is(Blocks.PUMPKIN)) {
                invalid.add("local=" + raw.pos() + " world=" + pos + " actual=" + actual
                        + " expected=" + expectedStem + " fruit=" + fruit + "/" + level.getBlockState(fruit));
            }
        }
        int attached = count(level, farm.getBoundingBox(), Blocks.ATTACHED_PUMPKIN_STEM);
        int mature = count(level, farm.getBoundingBox(), Blocks.PUMPKIN_STEM);
        String detail = farm.getRotation() + "/" + farm.placeSettings().getMirror()
                + " source=" + sourceStems + " attached=" + attached + " mature=" + mature + " invalid=" + invalid;
        helper.assertTrue(sourceStems == 32 && attached + mature == 32 && invalid.isEmpty(),
                "Farm must preserve all 32 pumpkin stems in their original positions with correctly positioned fruit: " + detail);
        AOEMod.LOGGER.info("Mansion farm stem audit: {}", detail);
    }

    private static void useLever(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        BlockState lever = level.getBlockState(pos);
        helper.assertTrue(lever.is(Blocks.LEVER), "Lever did not survive placement updates");
        ((LeverBlock) Blocks.LEVER).use(lever, level, pos, helper.makeMockSurvivalPlayer(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), lever.getValue(BlockStateProperties.HORIZONTAL_FACING), pos, false));
    }

    private static void assertDoor(GameTestHelper helper, ServerLevel level, BlockPos lower, boolean open) {
        for (BlockPos pos : List.of(lower, lower.above())) {
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(state.is(Blocks.IRON_DOOR) && state.getValue(BlockStateProperties.OPEN) == open
                            && state.getValue(BlockStateProperties.POWERED) == open,
                    "Both halves of the directly controlled door must be " + (open ? "open" : "closed") + " at " + pos);
        }
    }

    private record DoorControls(BlockPos leftLever, BlockPos rightLever, BlockPos leftDoor, BlockPos rightDoor) { }
}
