package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.AuthoredEncounterEvents;
import net.amurdza.examplemod.event_handlers.BossStructureProtection;
import net.amurdza.examplemod.event_handlers.MobAI;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.MiningPressFeedback;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaInteractionRevisionTests {
    private SavannaInteractionRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void campMessagesFollowRemainingDefendersAndClearedHarvestsAreSilent(GameTestHelper helper) {
        checkMessages(helper, camp(helper, 94000));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void mansionMessagesFollowRemainingDefendersAndClearedHarvestsAreSilent(GameTestHelper helper) {
        checkMessages(helper, mansion(helper, 95000));
        helper.succeed();
    }

    private static void checkMessages(GameTestHelper helper, Fixture fixture) {
        var player = new RecordingPlayer(fixture.level);
        player.moveTo(Vec3.atBottomCenterOf(fixture.crop));
        for (var reward : List.of(fixture.crop, fixture.chest)) {
            assertDeniedHitAndBreak(helper, fixture, player, reward, "rewards_locked", fixture.defenders.size());
            assertDeniedUse(helper, fixture, player, reward, "rewards_locked", fixture.defenders.size());
        }
        for (var permanent : List.of(fixture.wall, fixture.crop.below(), fixture.fence))
            assertDeniedHitAndBreak(helper, fixture, player, permanent, "protected", null);
        for (int i = 0; i < fixture.defenders.size() - 1; i++) die(fixture.defenders.get(i));
        assertDeniedHitAndBreak(helper, fixture, player, fixture.crop, "rewards_locked", 1);
        assertDeniedUse(helper, fixture, player, fixture.chest, "rewards_locked", 1);
        die(fixture.defenders.get(fixture.defenders.size() - 1));
        for (var permanent : List.of(fixture.wall, fixture.crop.below(), fixture.fence))
            assertDeniedHitAndBreak(helper, fixture, player, permanent, "protected", null);

        for (var reward : List.of(fixture.crop, fixture.chest)) {
            player.messages.clear();
            var hit = new PlayerInteractEvent.LeftClickBlock(player, reward, Direction.UP);
            MinecraftForge.EVENT_BUS.post(hit);
            helper.assertFalse(hit.isCanceled(),
                    "Unlocked reward mining click was canceled at " + reward + "; messages=" + player.messages);
            var beforeUse = fixture.level.getBlockState(reward);
            var use = useEvent(player, reward);
            MinecraftForge.EVENT_BUS.post(use);
            var afterUse = fixture.level.getBlockState(reward);
            // Other pack mods may consume the right click; this regression concerns protection feedback.
            helper.assertTrue(player.messages.isEmpty(),
                    "Unlocked reward emitted a protection warning at " + reward + ": hitCanceled=" + hit.isCanceled()
                            + ", useCanceled=" + use.isCanceled() + ", result=" + use.getCancellationResult()
                            + ", before=" + beforeUse + ", after=" + afterUse + ", messages=" + player.messages);
            // CustomBlockDrops may cancel the vanilla break while performing the real harvest itself.
            player.gameMode.destroyBlock(reward);
            helper.assertTrue(fixture.level.getBlockState(reward).isAir() && player.messages.isEmpty(),
                    "Actual unlocked reward harvesting must remove the block without a spurious message");
            MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(player, reward, Direction.UP));
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(fixture.level, reward,
                    fixture.level.getBlockState(reward), player));
            helper.assertTrue(player.messages.isEmpty(), "An extra mining packet for the harvested air must stay silent");
        }
        player.messages.clear();
        helper.assertFalse(player.gameMode.destroyBlock(fixture.fence), "The ordinary mining path must still protect fences after victory");
        helper.assertTrue(fixture.level.getBlockState(fixture.fence).is(fixture.prefix.equals("camp_") ? Blocks.ACACIA_FENCE : Blocks.OAK_FENCE),
                "Victory must not make construction harvestable");
        assertMessage(helper, player, fixture.prefix + "protected", null);
        if (fixture.prefix.equals("camp_")) {
            var chunk = fixture.level.getChunkAt(fixture.crop);
            var packet = new CompoundTag();
            SavannaCampProtectionSnapshot.write(chunk, packet);
            SavannaCampProtectionSnapshot.read(chunk, packet);
            helper.assertFalse(SavannaCampProtectionSnapshot.protects(fixture.level, fixture.crop),
                    "Client mining prediction must not treat harvested air as protected construction");
        }
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void campAndMansionGatesUnlockOnlyAfterTheFinalDefender(GameTestHelper helper) {
        for (var fixture : List.of(camp(helper, 96000), mansion(helper, 97000))) {
            var player = new RecordingPlayer(fixture.level);
            var gate = fixture.gate;
            assertGateSnapshot(helper, fixture, true);
            assertDeniedUse(helper, fixture, player, gate, "gates_locked", fixture.defenders.size());
            fixture.level.setBlock(gate, fixture.level.getBlockState(gate)
                    .setValue(FenceGateBlock.OPEN, true).setValue(FenceGateBlock.POWERED, true), 2);
            helper.assertTrue(fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN)
                            && fixture.level.getBlockState(gate).getValue(FenceGateBlock.POWERED),
                    "The saved-state probe must start with an open, powered gate");
            assertDeniedUse(helper, fixture, player, gate, "gates_locked", fixture.defenders.size());
            helper.assertTrue(!fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN)
                            && !fixture.level.getBlockState(gate).getValue(FenceGateBlock.POWERED),
                    "A canceled locked-gate interaction must also close and unpower a previously open gate");
            player.messages.clear();
            fixture.level.getBlockState(gate).use(fixture.level, player, InteractionHand.MAIN_HAND, hit(gate));
            helper.assertFalse(fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN),
                    "The native gate use method must also respect the encounter lock");
            assertMessage(helper, player, fixture.prefix + "gates_locked", fixture.defenders.size());
            var power = gate.east();
            fixture.level.setBlock(power.below(), fixture.level.getBlockState(fixture.wall), 18);
            // Stone-based levers are unavailable in savanna; each dimension permits its native wood button.
            var button = fixture.prefix.equals("camp_") ? Blocks.ACACIA_BUTTON : Blocks.OAK_BUTTON;
            fixture.level.setBlock(power, button.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.FLOOR)
                    .setValue(ButtonBlock.POWERED, true), 18);
            helper.assertTrue(fixture.level.getBlockState(power).is(button)
                            && fixture.level.getBlockState(power).getValue(ButtonBlock.POWERED),
                    "The power fixture must contain the permitted, pressed wooden button");
            helper.assertTrue(fixture.level.hasNeighborSignal(gate), "The redstone probe must actually power the gate");
            fixture.level.getBlockState(gate).neighborChanged(fixture.level, gate, button, power, false);
            helper.assertFalse(fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN),
                    "Redstone neighbor updates must not open a locked livestock gate");
            fixture.level.setBlock(power, Blocks.AIR.defaultBlockState(), 18);
            for (int i = 0; i < fixture.defenders.size() - 1; i++) die(fixture.defenders.get(i));
            assertGateSnapshot(helper, fixture, true);
            assertDeniedUse(helper, fixture, player, gate, "gates_locked", 1);
            die(fixture.defenders.get(fixture.defenders.size() - 1));
            assertGateSnapshot(helper, fixture, false);
            player.messages.clear();
            var unlocked = useEvent(player, gate);
            MinecraftForge.EVENT_BUS.post(unlocked);
            helper.assertFalse(unlocked.isCanceled(), "Cleared gate interactions must reach the native block");
            fixture.level.getBlockState(gate).use(fixture.level, player, InteractionHand.MAIN_HAND, hit(gate));
            helper.assertTrue(fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN) && player.messages.isEmpty(),
                    "A cleared gate must actually open without a warning");
            fixture.level.getBlockState(gate).use(fixture.level, player, InteractionHand.MAIN_HAND, hit(gate));
            helper.assertFalse(fixture.level.getBlockState(gate).getValue(FenceGateBlock.OPEN),
                    "Cleared gates must retain normal opening and closing behavior");
            assertDeniedHitAndBreak(helper, fixture, player, gate, "protected", null);
        }
        helper.succeed();
    }

    private static void assertGateSnapshot(GameTestHelper helper, Fixture fixture, boolean locked) {
        var chunk = fixture.level.getChunkAt(fixture.gate);
        var packet = new CompoundTag();
        boolean camp = fixture.prefix.equals("camp_");
        if (camp) {
            SavannaCampProtectionSnapshot.write(chunk, packet);
            SavannaCampProtectionSnapshot.read(chunk, packet);
        } else {
            MansionProtectionSnapshot.write(chunk, packet);
            MansionProtectionSnapshot.read(chunk, packet);
        }
        boolean predictedLock = camp ? SavannaCampProtectionSnapshot.rewardsLocked(fixture.level, fixture.gate)
                : MansionProtectionSnapshot.rewardsLocked(fixture.level, fixture.gate);
        helper.assertTrue(predictedLock == locked,
                "The exact client chunk packet must reject gate prediction until the final defender, then allow it");
        helper.assertTrue(camp ? SavannaCampProtectionSnapshot.protects(fixture.level, fixture.gate)
                        : MansionProtectionSnapshot.protects(fixture.level, fixture.gate),
                "Unlocked gate use must not allow the permanently protected fence to be mined");
        if (camp) SavannaCampProtectionSnapshot.read(chunk, new CompoundTag());
        else MansionProtectionSnapshot.read(chunk, new CompoundTag());
        helper.assertFalse(camp ? SavannaCampProtectionSnapshot.rewardsLocked(fixture.level, fixture.gate)
                        : MansionProtectionSnapshot.rewardsLocked(fixture.level, fixture.gate),
                "A replacement chunk without this structure must remove the old client interaction lock");
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void livestockNeverReturnsToItsPenAndClearedSavedStockNeedsNoLead(GameTestHelper helper) {
        var level = helper.getLevel();
        var home = new BlockPos(99000, 180, 99000);
        var id = UUID.randomUUID();
        var boundary = new AABB(home.offset(-4, -4, -4), home.offset(65, 8, 8));
        BossStructureBounds.get(level).record("mansion:stock-no-pen:" + id, boundary, id);
        var progress = AuthoredEncounterProgress.get(level);
        progress.register(id, home, 1, "mansion");
        var guard = EntityType.VINDICATOR.create(level);
        AuthoredEncounterProgress.markDefender(guard, id, home, 1, "mansion", 0);
        StructureSpawnedMobData.mark(guard, home);
        var sheep = EntityType.SHEEP.create(level);
        AuthoredEncounterProgress.markLivestock(sheep, id, home, 1, "mansion");
        StructureSpawnedMobData.mark(sheep, home);
        sheep.moveTo(Vec3.atBottomCenterOf(home.offset(30, 0, 0)));
        sheep.noPhysics = true;
        var position = sheep.position();
        tickHomeAndBoundary(sheep);
        helper.assertTrue(sheep.position().equals(position) && !sheep.hasRestriction()
                        && StructureSpawnedMobData.isMarked(sheep),
                "Locked stock may roam beyond its old pen radius while retaining whole-structure confinement");
        sheep.move(MoverType.SELF, new Vec3(80, 0, 0));
        helper.assertTrue(sheep.getBoundingBox().maxX <= boundary.maxX,
                "Removing the pen tether must not let locked livestock leave the structure");
        var savedWhileLocked = sheep.saveWithoutId(new CompoundTag());
        guard.moveTo(Vec3.atBottomCenterOf(home.offset(30, 0, 0)));
        tickHomeAndBoundary(guard);
        helper.assertTrue(guard.position().equals(Vec3.atBottomCenterOf(home)) && guard.hasRestriction(),
                "Monster residents must retain their home return and restriction");
        die(guard);
        helper.assertTrue(progress.isCleared(id), "The final recorded defender must clear this livestock encounter");
        // A previously saved animal may first load after the player has won, without a lead interaction.
        var restored = EntityType.SHEEP.create(level);
        restored.load(savedWhileLocked);
        restored.restrictTo(home, StructureSpawnedMobData.HOME_RADIUS);
        for (var animal : List.of(sheep, restored)) {
            String label = animal == sheep ? "original livestock" : "reloaded livestock";
            animal.moveTo(Vec3.atBottomCenterOf(home.offset(85, 0, 0)));
            var outside = animal.position();
            tickHomeAndBoundary(animal);
            helper.assertTrue(animal.position().equals(outside),
                    "Cleared " + label + " moved from " + outside + " to " + animal.position() + " without a lead");
            helper.assertFalse(animal.hasRestriction(),
                    "Cleared " + label + " retained its home restriction: " + animal.getPersistentData());
            helper.assertFalse(StructureSpawnedMobData.isMarked(animal),
                    "Cleared " + label + " retained its structure home tag: " + animal.getPersistentData());
            helper.assertTrue(id.equals(AuthoredEncounterProgress.encounterId(animal)),
                    "Released livestock must keep its encounter identity for boundary membership");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void heldHarvestPressStaysQuietUntilThePlayerReleasesIt(GameTestHelper helper) {
        var feedback = new MiningPressFeedback();
        helper.assertTrue(feedback.shouldExplainBlocked(), "A fresh press on construction must explain its protection");
        feedback.release();
        feedback.harvested();
        helper.assertFalse(feedback.shouldExplainBlocked(), "Continuing the harvest press onto the soil must not emit a warning");
        helper.assertFalse(feedback.shouldExplainBlocked(), "Holding the same press must remain quiet across multiple frames");
        feedback.release();
        helper.assertTrue(feedback.shouldExplainBlocked(), "A new deliberate press on soil must explain its permanent protection");
        helper.succeed();
    }

    private record Fixture(ServerLevel level, UUID id, BlockPos origin, String prefix, List<Mob> defenders,
                           BlockPos chest, BlockPos crop, BlockPos wall, BlockPos fence, BlockPos gate) { }

    private static Fixture camp(GameTestHelper helper, int coordinate) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var origin = new BlockPos(coordinate, 180, coordinate);
        var id = UUID.randomUUID();
        SavannaCampProgress.register(level, id, origin);
        var crop = origin.offset(SavannaOrcVillage.CENTER + 40, 3, SavannaOrcVillage.CENTER + 10);
        return fixture(helper, level, id, origin, "camp_", SavannaCampProgress.KIND, 127, crop);
    }

    private static Fixture mansion(GameTestHelper helper, int coordinate) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var crop = new BlockPos(coordinate, 185, coordinate);
        var basement = new MansionBasementPiece(crop.offset(0, 0, 20), Rotation.NONE, 11);
        var id = UUID.randomUUID();
        basement.encounter(id, 2);
        PlacedMansions.get(level).record(basement,
                List.of(BoundingBox.fromCorners(crop.offset(-2, -2, -2), crop.offset(8, 3, 2))));
        AuthoredEncounterProgress.get(level).register(id, basement.keyAnchor(), 2, "mansion");
        return fixture(helper, level, id, basement.keyAnchor(), "mansion_", "mansion", 2, crop);
    }

    private static Fixture fixture(GameTestHelper helper, ServerLevel level, UUID id, BlockPos origin, String prefix,
                                   String kind, int count, BlockPos crop) {
        var fixture = new Fixture(level, id, origin, prefix, new ArrayList<>(), crop.east(), crop,
                crop.east(2), crop.east(3), crop.east(4));
        // Load the fixture's actual block chunks; no entity insertion/UUID callback timing is involved.
        level.getChunkAt(crop);
        level.getChunkAt(crop.east(5));
        level.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState(), 18);
        level.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), 18);
        level.setBlock(fixture.chest, Blocks.CHEST.defaultBlockState(), 18);
        var wood = prefix.equals("camp_") ? Blocks.ACACIA_WOOD : Blocks.OAK_WOOD;
        var fence = prefix.equals("camp_") ? Blocks.ACACIA_FENCE : Blocks.OAK_FENCE;
        var gate = prefix.equals("camp_") ? Blocks.ACACIA_FENCE_GATE : Blocks.OAK_FENCE_GATE;
        level.setBlock(fixture.wall, wood.defaultBlockState(), 18);
        level.setBlock(fixture.fence, fence.defaultBlockState(), 18);
        level.setBlock(fixture.gate, gate.defaultBlockState(), 18);
        helper.assertTrue(level.getBlockState(crop).is(Blocks.WHEAT) && level.getBlockState(crop.below()).is(Blocks.FARMLAND)
                        && level.getBlockState(fixture.chest).is(Blocks.CHEST) && level.getBlockState(fixture.wall).is(wood)
                        && level.getBlockState(fixture.fence).is(fence) && level.getBlockState(fixture.gate).is(gate),
                "Every interaction probe must be physically present and permitted in its dimension");
        for (int i = 0; i < count; i++) {
            var defender = EntityType.VINDICATOR.create(level);
            AuthoredEncounterProgress.markDefender(defender, id, origin, count, kind, i);
            fixture.defenders.add(defender);
        }
        return fixture;
    }

    private static void assertDeniedHitAndBreak(GameTestHelper helper, Fixture fixture, RecordingPlayer player,
                                               BlockPos pos, String suffix, Integer remaining) {
        player.messages.clear();
        var hit = new PlayerInteractEvent.LeftClickBlock(player, pos, Direction.UP);
        MinecraftForge.EVENT_BUS.post(hit);
        helper.assertTrue(hit.isCanceled(), "Protected left clicks must be canceled at " + pos);
        assertMessage(helper, player, fixture.prefix + suffix, remaining);
        player.messages.clear();
        var breaking = new BlockEvent.BreakEvent(fixture.level, pos, fixture.level.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(breaking);
        helper.assertTrue(breaking.isCanceled(), "Protected break events must be canceled at " + pos);
        assertMessage(helper, player, fixture.prefix + suffix, remaining);
    }

    private static void assertDeniedUse(GameTestHelper helper, Fixture fixture, RecordingPlayer player,
                                       BlockPos pos, String suffix, int remaining) {
        player.messages.clear();
        var use = useEvent(player, pos);
        MinecraftForge.EVENT_BUS.post(use);
        helper.assertTrue(use.isCanceled(), "Locked interactions must be canceled at " + pos);
        assertMessage(helper, player, fixture.prefix + suffix, remaining);
    }

    private static void assertMessage(GameTestHelper helper, RecordingPlayer player, String key, Integer remaining) {
        helper.assertTrue(player.messages.size() == 1, "Expected one protection message, got " + player.messages);
        helper.assertTrue(player.messages.get(0).getContents() instanceof TranslatableContents,
                "Protection messages must use their localized player-facing text");
        var contents = (TranslatableContents) player.messages.get(0).getContents();
        helper.assertTrue(contents.getKey().equals("message.aoemod." + key),
                "Wrong protection reason: " + contents.getKey());
        var args = contents.getArgs();
        helper.assertTrue(remaining == null ? args.length == 0
                        : args.length == 1 && args[0] instanceof Number value && value.intValue() == remaining,
                "Reward and gate messages must show the actual remaining roster; permanent messages must not use it");
    }

    private static PlayerInteractEvent.RightClickBlock useEvent(RecordingPlayer player, BlockPos pos) {
        return new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit(pos));
    }

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }

    private static void die(Mob mob) {
        AuthoredEncounterEvents.died(new LivingDeathEvent(mob, mob.level().damageSources().generic()));
    }

    private static void tickHomeAndBoundary(Mob mob) {
        int previousId = mob.getId();
        mob.setId((int) -Math.floorMod(mob.level().getGameTime(), 20));
        try {
            var event = new LivingEvent.LivingTickEvent(mob);
            MobAI.onLivingTick(event);
            BossStructureProtection.tick(event);
        } finally {
            mob.setId(previousId);
        }
    }

    private static final class RecordingPlayer extends FakePlayer {
        private final List<Component> messages = new ArrayList<>();

        private RecordingPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "SavannaMessageQA"));
            gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        @Override public void displayClientMessage(Component message, boolean actionBar) {
            if (actionBar) messages.add(message.copy());
        }
    }
}
