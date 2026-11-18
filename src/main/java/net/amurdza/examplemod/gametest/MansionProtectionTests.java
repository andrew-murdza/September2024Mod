package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.BossDeathChests;
import net.amurdza.examplemod.event_handlers.MansionProtection;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MansionProtectionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void legacyDoubleDoorOpensByHandAndMasonryIsProtected(GameTestHelper h) {
        var level = h.getLevel();
        var entrance = h.absolutePos(new BlockPos(20, 0, -25)).atY(170);
        var basement = new MansionBasementPiece(entrance, Rotation.CLOCKWISE_90, 2);
        var room = BoundingBox.fromCorners(entrance, entrance.offset(6, 5, 6));
        var manual = PlacedMansions.get(level);
        manual.record(basement, List.of(room));
        var restored = PlacedMansions.load(manual.save(new CompoundTag()));
        h.assertTrue(restored.at(basement.firstDoor()).stream().anyMatch(l -> l.basement().isDoor(basement.secondDoor())),
                "Command-placed rotated doors must retain their mansion association after restart");
        for (var pos : List.of(basement.firstDoor(), basement.secondDoor())) {
            level.setBlock(pos.below(), Blocks.STONE_BRICKS.defaultBlockState(), 18);
            var door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST)
                    .setValue(DoorBlock.HINGE, pos.equals(basement.firstDoor()) ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            level.setBlock(pos, door, 18);
            level.setBlock(pos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
        }
        var player = h.makeMockSurvivalPlayer();
        var pos = basement.firstDoor();
        use(player, pos.above(), ItemStack.EMPTY);
        for (var leaf : List.of(pos, basement.secondDoor())) {
            h.assertTrue(level.getBlockState(leaf).getValue(DoorBlock.OPEN) && level.getBlockState(leaf.above()).getValue(DoorBlock.OPEN),
                    "Empty-hand interaction must operate both halves of each leaf");
        }
        use(player, basement.secondDoor(), ItemStack.EMPTY);
        h.assertFalse(level.getBlockState(pos).getValue(DoorBlock.OPEN), "Interacting with the second leaf must close the double door");
        for (var target : List.of(pos, pos.below(), entrance.above())) {
            var breaking = new BlockEvent.BreakEvent(level, target, level.getBlockState(target), player);
            MansionProtection.breakBlock(breaking);
            h.assertTrue(breaking.isCanceled(), "Survival could break a protected mansion block");
            var creative = new BlockEvent.BreakEvent(level, target, level.getBlockState(target), h.makeMockPlayer());
            MansionProtection.breakBlock(creative);
            h.assertFalse(creative.isCanceled(), "Creative cannot edit its mansion");
        }
        var ordinaryDoor = entrance.offset(12, 0, 0);
        h.assertTrue(MansionProtection.door(level, ordinaryDoor) == null, "Protection leaked outside the mansion");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void retiringTheKeyPreservesLordLootButNeverDropsOrListsAKey(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var lord = ModEntities.ILLAGER_LORD.get().create(level);
        lord.moveTo(pos.getX(), pos.getY(), pos.getZ());
        lord.getPersistentData().putLong("MansionKeyAnchor", pos.asLong());
        lord.getPersistentData().putString("MansionDimension", level.dimension().location().toString());
        var drops = new ArrayList<ItemEntity>();
        h.assertTrue(BossDeathChests.handle(new LivingDropsEvent(lord, level.damageSources().generic(), drops, 0, false)),
                "The lord must retain its normal death reward");
        h.assertFalse(drops.stream().anyMatch(drop -> drop.getItem().is(ModItems.MANSION_KEY.get())),
                "Even a saved lord carrying the old anchor must no longer drop a key");
        h.assertFalse(net.amurdza.examplemod.util.RetainedContent.isKeptItem(new ItemStack(ModItems.MANSION_KEY.get())),
                "The retired key must leave the creative catalog");
        lord.discard();
        h.succeed();
    }

    private static void use(Player player, BlockPos pos, ItemStack key) {
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        MansionProtection.use(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
    }
}
