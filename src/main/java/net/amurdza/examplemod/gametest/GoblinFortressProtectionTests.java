package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressProtectionTests {
    private GoblinFortressProtectionTests() { }

    private record Fixture(UUID id, String key, BlockPos origin) { }

    private static Fixture fixture(GameTestHelper helper, int count) {
        var origin = helper.absolutePos(new BlockPos(1, 2, 1));
        var id = UUID.randomUUID();
        String key = GoblinFortressPiece.ENCOUNTER_KIND + ":" + id;
        AuthoredEncounterProgress.get(helper.getLevel()).register(id, origin, count, GoblinFortressPiece.ENCOUNTER_KIND);
        BossStructureBounds.get(helper.getLevel()).record(key,
                new AABB(origin.getX(), origin.getY() - 1, origin.getZ(),
                        origin.getX() + 5, origin.getY() + 4, origin.getZ() + 5), id);
        return new Fixture(id, key, origin);
    }

    private static void defeat(GameTestHelper helper, Fixture fixture, int count, int index) {
        var defender = EntityType.ZOMBIE.create(helper.getLevel());
        AuthoredEncounterProgress.markDefender(defender, fixture.id, fixture.origin, count,
                GoblinFortressPiece.ENCOUNTER_KIND, index);
        AuthoredEncounterProgress.get(helper.getLevel()).defeat(defender);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lastDistinctDefenderUnlocksHarvestButNeverConstruction(GameTestHelper helper) {
        var level = helper.getLevel();
        var fixture = fixture(helper, GoblinFortressPiece.DEFENDER_COUNT);
        try {
            var chest = fixture.origin;
            var crop = chest.east();
            var mushroom = chest.south();
            var gate = chest.south(2);
            var stone = chest.east(2);
            var soil = chest.east(3);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 2);
            level.setBlock(mushroom, Blocks.RED_MUSHROOM.defaultBlockState(), 2);
            level.setBlock(gate, Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), 2);
            level.setBlock(stone, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
            helper.assertTrue(GoblinFortressProtection.remaining(level, chest) == GoblinFortressPiece.DEFENDER_COUNT,
                    "An unloaded roster is not an empty, defeated fortress");
            for (int index = 0; index < GoblinFortressPiece.DEFENDER_COUNT - 1; index++)
                defeat(helper, fixture, GoblinFortressPiece.DEFENDER_COUNT, index);
            helper.assertTrue(GoblinFortressProtection.remaining(level, chest) == 1
                            && GoblinFortressProtection.rewardsLocked(level, chest)
                            && GoblinFortressProtection.isProtected(level, crop),
                    "Every reward must stay locked with one living or unloaded defender");
            var restored = AuthoredEncounterProgress.load(AuthoredEncounterProgress.get(level).save(new CompoundTag()));
            helper.assertTrue(!restored.isCleared(fixture.id)
                            && restored.defeatedCount(fixture.id) == GoblinFortressPiece.DEFENDER_COUNT - 1,
                    "The remaining defender count must survive saving and loading");
            defeat(helper, fixture, GoblinFortressPiece.DEFENDER_COUNT, 0);
            helper.assertTrue(GoblinFortressProtection.remaining(level, chest) == 1,
                    "A duplicate defender index cannot unlock the fortress");
            defeat(helper, fixture, GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.DEFENDER_COUNT - 1);
            helper.assertTrue(GoblinFortressProtection.remaining(level, chest) == 0
                            && !GoblinFortressProtection.rewardsLocked(level, chest)
                            && !GoblinFortressProtection.isProtected(level, chest)
                            && !GoblinFortressProtection.isProtected(level, crop)
                            && !GoblinFortressProtection.isProtected(level, mushroom),
                    "The final authored death unlocks chests, crops, and mushroom harvest");
            helper.assertTrue(GoblinFortressProtection.isProtected(level, gate)
                            && GoblinFortressProtection.isProtected(level, stone)
                            && GoblinFortressProtection.isProtected(level, soil),
                    "Victory must preserve the gates, building material, and farmland");
        } finally {
            BossStructureBounds.get(level).remove(fixture.key);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bucketPlacementAndGateUseRespectTheEstateBoundary(GameTestHelper helper) {
        var level = helper.getLevel();
        var fixture = fixture(helper, 1);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FortressQA"));
        player.setGameMode(GameType.SURVIVAL);
        try {
            var pos = fixture.origin;
            level.setBlock(pos, Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), 2);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var use = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            GoblinFortressProtection.use(use);
            helper.assertTrue(use.isCanceled(), "Locked pen gates must reject ordinary interaction");
            var bucket = new FillBucketEvent(player, new ItemStack(Items.BUCKET), level, hit);
            GoblinFortressProtection.bucket(bucket);
            helper.assertTrue(bucket.isCanceled(), "Empty buckets must not drain the fishing or irrigation water");
            var place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos),
                    Blocks.STONE.defaultBlockState(), player);
            GoblinFortressProtection.place(place);
            helper.assertTrue(place.isCanceled(), "Block placement inside the estate must fail");
            var breakBlock = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
            GoblinFortressProtection.breakBlock(breakBlock);
            helper.assertTrue(breakBlock.isCanceled(), "The locked gate cannot be harvested");
            defeat(helper, fixture, 1, 0);
            var unlockedUse = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            GoblinFortressProtection.use(unlockedUse);
            helper.assertFalse(unlockedUse.isCanceled(), "Victory must allow the gate to open");
            player.setGameMode(GameType.CREATIVE);
            var creativeBreak = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
            GoblinFortressProtection.breakBlock(creativeBreak);
            helper.assertFalse(creativeBreak.isCanceled(), "Creative editing must retain its explicit bypass");
        } finally {
            BossStructureBounds.get(level).remove(fixture.key);
            player.setGameMode(GameType.SURVIVAL);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pistonsFluidsAndTramplingCannotAlterTheEstate(GameTestHelper helper) {
        var level = helper.getLevel();
        var fixture = fixture(helper, 1);
        try {
            var inside = fixture.origin;
            var outside = inside.west();
            var piston = new PistonEvent.Pre(level, outside, Direction.EAST, PistonEvent.PistonMoveType.EXTEND);
            GoblinFortressProtection.piston(piston);
            helper.assertTrue(piston.isCanceled(), "An outside piston cannot extend into protected air");
            var fluid = new BlockEvent.FluidPlaceBlockEvent(level, inside, outside, Blocks.OBSIDIAN.defaultBlockState());
            GoblinFortressProtection.fluid(fluid);
            helper.assertTrue(fluid.isCanceled(), "Lava conversion must not rewrite the estate");
            helper.assertTrue(GoblinFortressProtection.blocksFluid(level, outside, inside)
                            && GoblinFortressProtection.blocksFluid(level, inside, outside),
                    "Flow must not cross either side of the protected fishing grounds");
            helper.assertFalse(GoblinFortressProtection.blocksFluid(level, outside.west(), outside.west(2)),
                    "Unrelated natural water must keep flowing");
            var trample = new BlockEvent.FarmlandTrampleEvent(level, inside, Blocks.FARMLAND.defaultBlockState(),
                    3, EntityType.ZOMBIE.create(level));
            GoblinFortressProtection.trample(trample);
            helper.assertTrue(trample.isCanceled(), "Defenders must not ruin their own crop foundations");
        } finally {
            BossStructureBounds.get(level).remove(fixture.key);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void redstoneCannotReleaseStockBeforeTheFinalDefender(GameTestHelper helper) {
        var level = helper.getLevel();
        var fixture = fixture(helper, 1);
        try {
            var gate = fixture.origin.east();
            var power = gate.east();
            level.setBlock(gate, Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), 2);
            level.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
            Blocks.SPRUCE_FENCE_GATE.neighborChanged(level.getBlockState(gate), level, gate, Blocks.REDSTONE_BLOCK, power, false);
            helper.assertFalse(level.getBlockState(gate).getValue(FenceGateBlock.OPEN),
                    "The native redstone path must not open a locked livestock gate");
            defeat(helper, fixture, 1, 0);
            Blocks.SPRUCE_FENCE_GATE.neighborChanged(level.getBlockState(gate), level, gate, Blocks.REDSTONE_BLOCK, power, false);
            helper.assertTrue(level.getBlockState(gate).getValue(FenceGateBlock.OPEN),
                    "Native redstone operation must resume after the encounter clears");
        } finally {
            BossStructureBounds.get(level).remove(fixture.key);
        }
        helper.succeed();
    }
}
