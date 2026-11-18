package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.AuthoredEncounterEvents;
import net.amurdza.examplemod.event_handlers.SavannaCampProtection;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.SavannaCampSpawnRules;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampProtectionTests {
    private SavannaCampProtectionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void all127DistinctDefeatsSurviveReloadWithoutCountingMissingOrDuplicateResidents(GameTestHelper helper) {
        var level = savanna(helper);
        var id = UUID.randomUUID();
        var origin = new BlockPos(43000, 180, 43000);
        var progress = new AuthoredEncounterProgress();
        progress.register(id, origin, 127, SavannaCampProgress.KIND);
        var residents = defenders(level, id, origin);
        for (var resident : residents) progress.track(resident);
        for (int i = 0; i < 126; i++) helper.assertFalse(progress.defeat(residents.get(i)),
                "No proper subset of the authored127 may unlock rewards");
        progress = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        helper.assertTrue(progress.defeatedCount(id) == 126 && !progress.isCleared(id),
                "Unloading every entity must preserve126 defeats and the remaining defender");
        helper.assertFalse(progress.defeat(residents.get(0)), "Repeated deaths must not double-count a resident");
        var duplicate = EntityType.ZOMBIE.create(level);
        SavannaCampProgress.markResident(duplicate, id, origin, 126);
        helper.assertFalse(progress.defeat(duplicate), "A different entity copied into the same roster slot must not unlock the encounter");
        var savedEntity = residents.get(126).saveWithoutId(new CompoundTag());
        var reloaded = EntityType.ZOMBIE.create(level);
        reloaded.load(savedEntity);
        helper.assertTrue(progress.defeat(reloaded) && progress.isCleared(id),
                "The actual remaining defender must retain its identity across entity reload and unlock exactly once");
        helper.assertFalse(progress.defeat(reloaded), "The clear transition may be emitted only once");
        var cleared = AuthoredEncounterProgress.load(progress.save(new CompoundTag()));
        helper.assertTrue(cleared.isCleared(id) && cleared.defeatedCount(id) == 127,
                "The completed encounter must remain clear after a world-data reload");
        var replacement = UUID.randomUUID();
        cleared.register(replacement, origin, 127, SavannaCampProgress.KIND);
        helper.assertFalse(cleared.isCleared(replacement), "A new camp at the same location must start its own encounter");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cancelledDeathsCannotAdvanceThePersistentEncounter(GameTestHelper helper) {
        var level = savanna(helper);
        var id = UUID.randomUUID();
        var origin = new BlockPos(43500, 180, 43500);
        var mob = EntityType.ZOMBIE.create(level);
        SavannaCampProgress.markResident(mob, id, origin, 0);
        var progress = AuthoredEncounterProgress.get(level);
        progress.track(mob);
        var cancelled = new LivingDeathEvent(mob, level.damageSources().generic());
        cancelled.setCanceled(true);
        AuthoredEncounterEvents.died(cancelled);
        helper.assertTrue(progress.defeatedCount(id) == 0, "A revived or canceled death must not count as a defeat");
        AuthoredEncounterEvents.died(new LivingDeathEvent(mob, level.damageSources().generic()));
        helper.assertTrue(progress.defeatedCount(id) == 1 && !progress.isCleared(id),
                "A real accepted death records one slot, never the number of currently loaded mobs");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void chestAutomationCropHarvestAndLivestockUnlockOnlyAfterAllDefenders(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(44000, 180, 44000);
        var id = UUID.randomUUID();
        var camp = SavannaCampProgress.register(level, id, origin);
        var center = origin.offset(SavannaOrcVillage.CENTER, 3, SavannaOrcVillage.CENTER);
        var chestPos = center;
        var cropPos = center.offset(40, 0, 10);
        var mushroomPos = center.offset(65, 0, 65);
        var wallPos = center.offset(-5, 0, -5);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 18);
        level.setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), 18);
        level.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), 18);
        level.setBlock(mushroomPos.below(), Blocks.PODZOL.defaultBlockState(), 18);
        level.setBlock(mushroomPos, Blocks.RED_MUSHROOM.defaultBlockState(), 18);
        level.setBlock(wallPos, Blocks.ACACIA_WOOD.defaultBlockState(), 18);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var player = FakePlayerFactory.getMinecraft(level);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
        var sheep = EntityType.SHEEP.create(level);
        sheep.moveTo(center.getX() + .5, center.getY(), center.getZ() + .5, 0, 0);
        StructureSpawnedMobData.mark(sheep, center);
        SavannaCampProgress.markLivestock(sheep, id, origin);
        var progress = AuthoredEncounterProgress.get(level);
        progress.track(sheep);
        var chunk = level.getChunkAt(chestPos);
        var packet = new CompoundTag();
        SavannaCampProtectionSnapshot.write(chunk, packet); SavannaCampProtectionSnapshot.read(chunk, packet);
        helper.assertTrue(SavannaCampProtectionSnapshot.protects(level, chestPos), "Chunk snapshots must block locked chest prediction");
        for (boolean cleared : new boolean[]{false, true}) {
            if (cleared) for (var defender : defenders(level, id, origin)) progress.defeat(defender);
            for (var pos : List.of(chestPos, cropPos, mushroomPos, wallPos)) {
                boolean reward = pos.equals(chestPos) || pos.equals(cropPos);
                var event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
                SavannaCampProtection.breakBlock(event);
                helper.assertTrue(event.isCanceled() == (!cleared || !reward),
                        "Only cleared chests and fenced-field crops may be mined; forest mushrooms and construction remain protected");
            }
            var click = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, cropPos,
                    new BlockHitResult(Vec3.atCenterOf(cropPos), Direction.UP, cropPos, false));
            SavannaCampProtection.use(click);
            helper.assertTrue(click.isCanceled() != cleared, "Right-click crop harvesting must obey the same encounter lock");
            var attack = new LivingAttackEvent(sheep, level.damageSources().playerAttack(player), 4);
            SavannaCampProtection.attack(attack);
            helper.assertTrue(attack.isCanceled() != cleared, "Penned stock cannot be killed before the last defender dies");
            var lead = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, sheep);
            SavannaCampProtection.interact(lead);
            helper.assertTrue(lead.isCanceled() != cleared, "Penned stock cannot be leashed before clear");
            helper.assertTrue(StructureSpawnedMobData.isMarked(sheep) != cleared,
                    "Claimed stock must lose the old home tether so it can actually leave");
            var extracted = new InvWrapper(chest).extractItem(0, 1, true);
            helper.assertTrue(extracted.isEmpty() != cleared, "Forge inventory extraction must not bypass locked chests");
            if (!cleared) helper.assertTrue(chest.removeItem(0, 1).isEmpty() && chest.removeItemNoUpdate(0).isEmpty(),
                    "Direct vanilla container removals must not bypass the lock");
            var current = new CompoundTag();
            SavannaCampProtectionSnapshot.write(chunk, current); SavannaCampProtectionSnapshot.read(chunk, current);
            helper.assertTrue(SavannaCampProtectionSnapshot.protects(level, chestPos) != cleared,
                    "A fresh chunk snapshot must carry the exact cleared chest state");
        }
        helper.assertTrue(chest.getItem(0).getCount() == 3, "Locked extraction attempts must preserve the original inventory");
        helper.assertFalse(level.destroyBlock(wallPos, true), "Nonplayer destruction must not remove permanent camp construction");
        helper.assertFalse(level.removeBlock(wallPos, false), "Direct removals must not bypass permanent camp construction");
        ((net.amurdza.examplemod.mixins.accessor.FireBlockAccessor) Blocks.FIRE).aoemod$tryCatchFire(level, wallPos, 1,
                net.minecraft.util.RandomSource.create(71), 0, Direction.UP);
        helper.assertTrue(level.getBlockState(wallPos).is(Blocks.ACACIA_WOOD),
                "Vanilla fire must not bypass protection by replacing construction directly");
        level.setBlock(wallPos.above(), Blocks.FIRE.defaultBlockState(), 18);
        helper.assertTrue(level.removeBlock(wallPos.above(), false), "Protection must never prevent extinguishing fire");
        helper.assertTrue(level.destroyBlock(cropPos, false), "Cleared fenced crops must be physically harvestable");
        player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        helper.assertTrue(player.gameMode.destroyBlock(wallPos), "Creative editing must retain its established bypass");
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void externalLivestockAndUndeadCannotCrossOrTargetInsideCamp(GameTestHelper helper) {
        var level = savanna(helper);
        var origin = new BlockPos(45000, 180, 45000);
        var id = UUID.randomUUID();
        var camp = SavannaCampProgress.register(level, id, origin);
        var boundary = SavannaCampProgress.boundary(camp);
        var center = origin.offset(SavannaOrcVillage.CENTER, 3, SavannaOrcVillage.CENTER);
        PlacedOrcVillages.get(level).record(center, SavannaOrcVillage.RADIUS, "square");
        var player = FakePlayerFactory.getMinecraft(level);
        player.moveTo(center.getX() + .5, center.getY(), center.getZ() + .5, 0, 0);
        for (var type : List.of(EntityType.ZOMBIE, EntityType.SHEEP)) for (int side = 0; side < 4; side++) {
            var mob = (Mob) type.create(level);
            double x = side == 0 ? boundary.minX - 2 : side == 1 ? boundary.maxX + 2 : center.getX();
            double z = side == 2 ? boundary.minZ - 2 : side == 3 ? boundary.maxZ + 2 : center.getZ();
            mob.moveTo(x, center.getY(), z, 0, 0); mob.noPhysics = true;
            var movement = new Vec3(side == 0 ? 4 : side == 1 ? -4 : 0, 0, side == 2 ? 4 : side == 3 ? -4 : 0);
            mob.move(MoverType.SELF, movement);
            helper.assertFalse(mob.getBoundingBox().intersects(boundary),
                    "The real movement hook must prevent outside livestock and undead from crossing any camp side");
            var target = new LivingChangeTargetEvent(mob, player, LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
            SavannaCampProtection.target(target);
            helper.assertTrue(target.getNewTarget() == null, "Outside mobs must not acquire a target inside the camp");
            var attack = new LivingAttackEvent(player, level.damageSources().mobAttack(mob), 4);
            SavannaCampProtection.attack(attack);
            helper.assertTrue(attack.isCanceled(), "An already-fired outside attack must not damage targets inside the camp");
        }
        for (String name : new String[]{"orcz:orcshield", "orcz:orcsavagemace", "orcz:bigorcboss"}) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(name));
            helper.assertTrue(SavannaCampSpawnRules.blocked(level, center, type),
                    "Ordinary natural Orcz spawns must not add defenders to the authored camp");
        }
        var sheep = EntityType.SHEEP.create(level);
        var home = new BlockPos((int) boundary.minX + 2, center.getY(), center.getZ());
        sheep.moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 0, 0); sheep.noPhysics = true;
        SavannaCampProgress.markLivestock(sheep, id, origin); StructureSpawnedMobData.mark(sheep, home);
        sheep.move(MoverType.SELF, new Vec3(-8, 0, 0));
        helper.assertTrue(sheep.getBoundingBox().minX >= boundary.minX, "Locked authored livestock must stay inside even if pushed or pulled");
        for (var defender : defenders(level, id, origin)) AuthoredEncounterProgress.get(level).defeat(defender);
        sheep.move(MoverType.SELF, new Vec3(-8, 0, 0));
        helper.assertTrue(sheep.getBoundingBox().maxX < boundary.minX, "Cleared authored livestock must be able to leave the camp");
        helper.succeed();
    }

    private static List<Mob> defenders(ServerLevel level, UUID id, BlockPos origin) {
        var result = new ArrayList<Mob>();
        for (int i = 0; i < 127; i++) {
            var mob = EntityType.ZOMBIE.create(level);
            SavannaCampProgress.markResident(mob, id, origin, i); result.add(mob);
        }
        return result;
    }

    private static ServerLevel savanna(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
    }
}
