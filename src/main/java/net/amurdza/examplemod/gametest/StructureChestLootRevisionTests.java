package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.accessor.ChestLootTableAccess;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureChestLootRevisionTests {
    private static final ResourceLocation MANSION = AOEMod.makeID("chests/illager_mansion");

    @GameTest(template = "plant_gameplay_empty")
    public static void emptyMansionStorageChestsReceiveLootWithoutReplacingAuthoredContents(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var empty = new CompoundTag(); empty.put("Items", new ListTag());
        var fixed = new CompoundTag(); var items = new ListTag();
        var stack = new ItemStack(Items.BREAD, 7).save(new CompoundTag()); stack.putByte("Slot", (byte) 4); items.add(stack); fixed.put("Items", items);
        var custom = new CompoundTag(); custom.putString("LootTable", "minecraft:chests/simple_dungeon");
        for (var block : List.of(Blocks.CHEST, Blocks.TRAPPED_CHEST)) for (var tag : new CompoundTag[]{null, empty, fixed, custom}) {
            var original = new StructureBlockInfo(BlockPos.ZERO, block.defaultBlockState(), tag);
            var before = tag == null ? null : tag.copy();
            var result = IllagerMansionProcessor.INSTANCE.processBlock(level, BlockPos.ZERO, BlockPos.ZERO, original, original, new StructurePlaceSettings());
            if (tag == null || tag == empty) {
                h.assertTrue(result.nbt() != null && result.nbt().getString("LootTable").equals(MANSION.toString())
                                && !result.nbt().contains("Items"), "A generated empty mansion chest must retain a pending reward table");
                var chest = detached(level); chest.load(result.nbt());
                h.assertTrue(!chest.isEmpty(), "Processed chest NBT must actually generate loot when opened");
            } else h.assertTrue(result.nbt().equals(tag), "Fixed contents and custom loot tables must remain unchanged");
            h.assertTrue(java.util.Objects.equals(before, tag), "Processing must not mutate the shared template NBT");
            var outside = IllagerMansionProcessor.INSTANCE.processBlock(h.getLevel(), BlockPos.ZERO, BlockPos.ZERO, original, original, new StructurePlaceSettings());
            h.assertTrue(java.util.Objects.equals(outside.nbt(), tag), "Other dimensions must retain their native chest assignment");
        }
        var template = level.getStructureManager().getOrCreate(new ResourceLocation("minecraft", "woodland_mansion/" + MansionRoomCatalogue.newTemplate("1x2_a9")));
        var storage = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST);
        h.assertTrue(!storage.isEmpty(), "Exercise the actual mansion storage room");
        for (var info : storage) {
            var result = IllagerMansionProcessor.INSTANCE.processBlock(level, BlockPos.ZERO, BlockPos.ZERO, info, info, new StructurePlaceSettings());
            h.assertTrue(result.nbt().getString("LootTable").equals(MANSION.toString()), "Every empty storage-room chest needs generated supplies");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void regionalStructureTablesStillFillRealChestInventories(GameTestHelper h) {
        var tables = Map.ofEntries(
                Map.entry("illager_mansion", GameplayDimensions.PLAINS), Map.entry("illager_mansion_pantry", GameplayDimensions.PLAINS),
                Map.entry("illager_mansion_smith", GameplayDimensions.PLAINS), Map.entry("orc_camp", GameplayDimensions.SAVANNA),
                Map.entry("yeti_lair", GameplayDimensions.SNOWY), Map.entry("spider_lair", GameplayDimensions.TROPICAL),
                Map.entry("wither_lair", GameplayDimensions.NETHER), Map.entry("bosses/nether_citadel", GameplayDimensions.NETHER));
        for (var entry : tables.entrySet()) {
            var level = h.getLevel().getServer().getLevel(entry.getValue());
            for (int seed = 1; seed <= 16; seed++) {
                var chest = detached(level); chest.setLootTable(AOEMod.makeID("chests/" + entry.getKey()), seed);
                chest.unpackLootTable(null);
                h.assertTrue(!chest.isEmpty() && ((ChestLootTableAccess) chest).aoemod$pendingLootTable() == null,
                        "Structure chest must generate and consume its table exactly once: " + entry.getKey() + "/" + seed);
                var before = chest.saveWithoutMetadata(); chest.unpackLootTable(null);
                h.assertTrue(before.equals(chest.saveWithoutMetadata()), "Reopening must never refill a looted chest");
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void creativeSingleAndDoubleChestMenusExposeLootWithoutUnlockingHoppers(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var cellar = new MansionBasementPiece(new BlockPos(6500, 150, 6500));
        var id = UUID.randomUUID(); cellar.encounter(id, 1);
        var upperRoom = BoundingBox.fromCorners(cellar.world(-4, 7, 0), cellar.world(4, 14, 8));
        PlacedMansions.get(level).record(cellar, List.of(upperRoom));
        var progress = AuthoredEncounterProgress.get(level); progress.register(id, cellar.keyAnchor(), 1, "mansion");
        boolean[] creative = {true};
        Player owner = new Player(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "loot-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return creative[0]; }
        };
        var single = placed(level, cellar.world(-10, 1, 22), ChestType.SINGLE);
        var left = placed(level, cellar.world(-10, 1, 24), ChestType.LEFT);
        var right = placed(level, cellar.world(-9, 1, 24), ChestType.RIGHT);
        owner.setPos(single.getBlockPos().getX() + 0.5, single.getBlockPos().getY(), single.getBlockPos().getZ() + 0.5);
        for (var chest : List.of(single, left, right)) {
            var pristine = chest.saveWithoutMetadata();
            h.assertTrue(pristine.getList("Items", Tag.TAG_COMPOUND).isEmpty()
                            && !pristine.contains("LootTable") && !pristine.contains("LootTableSeed")
                            && ((ChestLootTableAccess) chest).aoemod$pendingLootTable() == null,
                    "The fixture must start with a fresh, unseeded chest at " + chest.getBlockPos() + ": " + pristine);
            chest.setLootTable(MANSION, chest.getBlockPos().asLong());
        }
        var singleMenu = (ChestMenu) single.createMenu(1, owner.getInventory(), owner);
        h.assertTrue(singleMenu.getContainer() == single, "Lid counters must still see the physical chest");
        int slot = firstFilled(singleMenu, 27);
        h.assertTrue(slot >= 0, "Creative must see generated loot in a locked single chest");
        assertAutomationLocked(h, single, slot);
        singleMenu.clicked(slot, 0, ClickType.PICKUP, owner);
        h.assertTrue(!singleMenu.getCarried().isEmpty(), "Creative slot clicks must operate on the real reward inventory");
        singleMenu.clicked(slot, 0, ClickType.PICKUP, owner);
        singleMenu.removed(owner);
        h.assertTrue(singleMenu.getSlot(slot).getItem().isEmpty(), "A closed menu must lose its access scope");
        var expectedItems = new ArrayList<ItemStack>();
        for (var chest : List.of(left, right)) {
            var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
                    .withParameter(LootContextParams.THIS_ENTITY, owner).withLuck(owner.getLuck()).create(LootContextParamSets.CHEST);
            expectedItems.addAll(level.getServer().getLootData().getLootTable(MANSION).getRandomItems(params, chest.getBlockPos().asLong()));
        }
        var pair = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, left.getBlockState(), level, left.getBlockPos(), true);
        h.assertTrue(pair instanceof CompoundContainer, "Use the normal connected double-chest inventory");
        left.unpackLootTable(owner); right.unpackLootTable(owner);
        var doubleMenu = ChestMenu.sixRows(2, owner.getInventory(), pair);
        h.assertTrue(doubleMenu.getContainer() == pair, "Double-chest lid counters must retain their backing inventory");
        h.assertTrue(menuTotals(doubleMenu).equals(totals(expectedItems)),
                "The visible double chest must contain both complete seeded rolls after stack compaction: expected="
                        + totals(expectedItems) + ", actual=" + menuTotals(doubleMenu));
        // Vanilla orders RIGHT before LEFT. Compaction may intentionally leave
        // one physical half empty; each visible slot must still match its NBT.
        for (int half = 0; half < 2; half++) {
            var savedHalf = detached(level); savedHalf.load((half == 0 ? right : left).saveWithoutMetadata());
            for (int index = 0; index < 27; index++)
                h.assertTrue(ItemStack.matches(doubleMenu.getSlot(half * 27 + index).getItem(), savedHalf.getItem(index)),
                        "Creative menu differs from its physical half at " + half + "/" + index);
        }
        var leftBefore = left.saveWithoutMetadata(); var rightBefore = right.saveWithoutMetadata();
        doubleMenu.removed(owner);
        left.unpackLootTable(owner); right.unpackLootTable(owner);
        doubleMenu = ChestMenu.sixRows(3, owner.getInventory(), pair);
        h.assertTrue(leftBefore.equals(left.saveWithoutMetadata()) && rightBefore.equals(right.saveWithoutMetadata())
                        && menuTotals(doubleMenu).equals(totals(expectedItems)), "Reopening must preserve both original rolls without adding items");
        // Exercise scoped access to both halves even when the compacted reward
        // itself fits entirely in the first half.
        h.assertTrue(doubleMenu.getSlot(26).getItem().isEmpty() && doubleMenu.getSlot(53).getItem().isEmpty(),
                "Both sentinel slots must be empty before exercising scoped writes");
        doubleMenu.getSlot(26).set(new ItemStack(Items.PAPER, 3));
        doubleMenu.getSlot(53).set(new ItemStack(Items.STICK, 5));
        h.assertTrue(doubleMenu.getSlot(26).getItem().is(Items.PAPER) && doubleMenu.getSlot(53).getItem().is(Items.STICK),
                "Creative reads and writes must reach both halves of the menu");
        assertAutomationLocked(h, left, 26); assertAutomationLocked(h, right, 26);
        int moved = firstFilled(doubleMenu, 54);
        h.assertTrue(!doubleMenu.quickMoveStack(owner, moved).isEmpty(), "Creative shift-click must use the scoped inventory");
        int retained = firstFilled(doubleMenu, 54);
        h.assertTrue(retained >= 0, "Keep a populated slot to verify immediate access revocation");
        creative[0] = false;
        h.assertTrue(!doubleMenu.stillValid(owner) && doubleMenu.getSlot(retained).getItem().isEmpty(),
                "Changing to survival must revoke the creative menu immediately");
        doubleMenu.removed(owner);
        try { CreativeRewardChestContainer.filling(single, () -> { throw new IllegalStateException("test scope cleanup"); }); }
        catch (IllegalStateException expected) { }
        assertAutomationLocked(h, single, 0);
        var defender = EntityType.VINDICATOR.create(level);
        AuthoredEncounterProgress.markDefender(defender, id, cellar.keyAnchor(), 1, "mansion", 0); progress.track(defender); progress.defeat(defender);
        h.assertTrue(!single.isEmpty() && !AuthoredRewardLocks.chestLocked(single), "Victory must reveal the original generated inventory to ordinary access");
        h.succeed();
    }

    private static int firstFilled(ChestMenu menu, int count) {
        for (int slot = 0; slot < count; slot++) if (!menu.getSlot(slot).getItem().isEmpty()) return slot;
        return -1;
    }
    private static Map<String, Integer> menuTotals(ChestMenu menu) {
        var items = new ArrayList<ItemStack>();
        for (int slot = 0; slot < 54; slot++) items.add(menu.getSlot(slot).getItem());
        return totals(items);
    }
    private static Map<String, Integer> totals(List<ItemStack> items) {
        var totals = new HashMap<String, Integer>();
        for (var item : items) if (!item.isEmpty()) {
            var key = item.copy(); key.setCount(1);
            totals.merge(key.save(new CompoundTag()).toString(), item.getCount(), Integer::sum);
        }
        return totals;
    }
    private static void assertAutomationLocked(GameTestHelper h, ChestBlockEntity chest, int slot) {
        h.assertTrue(chest.getItem(slot).isEmpty() && chest.removeItem(slot, 1).isEmpty() && chest.removeItemNoUpdate(slot).isEmpty(),
                "Creative inspection must not unlock direct reads or removals");
        var handler = chest.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new AssertionError("Missing chest handler"));
        h.assertTrue(handler.extractItem(slot, 1, false).isEmpty(), "Forge item handlers must remain locked during creative inspection");
    }
    private static ChestBlockEntity detached(ServerLevel level) {
        var chest = new ChestBlockEntity(new BlockPos(0, level.getMaxBuildHeight() + 10, 0), Blocks.CHEST.defaultBlockState());
        chest.setLevel(level); return chest;
    }
    private static ChestBlockEntity placed(ServerLevel level, BlockPos pos, ChestType type) {
        level.getChunkAt(pos); level.setBlock(pos.below(), Blocks.STONE_BRICKS.defaultBlockState(), 18);
        // Reused QA terrain can contain the previous run's inventory. Setting
        // the same chest state retains its block entity, so remove it first.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, type), 18);
        return (ChestBlockEntity) level.getBlockEntity(pos);
    }
}
