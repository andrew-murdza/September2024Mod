package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.BossStructureProtection;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.BossLairPiece;
import net.amurdza.examplemod.worldgen.structure.MansionBasementPiece;
import net.amurdza.examplemod.worldgen.structure.PlacedMansions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import twilightforest.init.TFEntities;

import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossStructureProtectionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void allMobFamiliesRespectAllSixFacesAndOnlyPiglinsAndVillagersEnter(GameTestHelper helper) {
        var level = helper.getLevel();
        var box = new AABB(81000, 180, 81000, 81020, 200, 81020);
        BossStructureBounds.get(level).record("lair:test:faces", box, null);
        for (var type : List.of(EntityType.ZOMBIE, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.BAT, EntityType.SQUID, EntityType.SHEEP, EntityType.HOGLIN)) {
            for (int face = 0; face < 6; face++) {
                var mob = (Mob) type.create(level);
                double x = face == 0 ? box.minX - 3 : face == 1 ? box.maxX + 3 : 81010;
                double y = face == 2 ? box.minY - 4 : face == 3 ? box.maxY + 3 : 185;
                double z = face == 4 ? box.minZ - 3 : face == 5 ? box.maxZ + 3 : 81010;
                mob.moveTo(x, y, z); mob.noPhysics = true;
                var movement = new Vec3(face == 0 ? 8 : face == 1 ? -8 : 0,
                        face == 2 ? 8 : face == 3 ? -8 : 0, face == 4 ? 8 : face == 5 ? -8 : 0);
                mob.move(MoverType.SELF, movement);
                helper.assertFalse(mob.getBoundingBox().intersects(box), "An outsider crossed a boss boundary: " + type + " face " + face);
            }
        }
        for (var type : List.of(EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.VILLAGER)) {
            var mob = (Mob) type.create(level);
            mob.moveTo(80997, 185, 81010); mob.noPhysics = true;
            mob.move(MoverType.SELF, new Vec3(8, 0, 0));
            helper.assertTrue(mob.getBoundingBox().intersects(box), "Piglins and villagers must retain entry permission");
        }
        var restored = BossStructureBounds.load(BossStructureBounds.get(level).save(new CompoundTag()));
        helper.assertFalse(restored.nearby(box).isEmpty(), "Boundary persistence must survive world reload");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void authoredMonstersAndStockCannotLeaveEvenInBoatsButClearedStockCan(GameTestHelper helper) {
        var level = helper.getLevel();
        var id = UUID.randomUUID();
        var home = new BlockPos(82005, 185, 82005);
        var box = new AABB(82000, 180, 82000, 82020, 200, 82020);
        BossStructureBounds.get(level).record("mansion:test:residents", box, id);
        var progress = AuthoredEncounterProgress.get(level);
        progress.register(id, home, 1, "mansion");
        var guard = EntityType.VINDICATOR.create(level);
        guard.moveTo(Vec3.atBottomCenterOf(home)); guard.noPhysics = true;
        AuthoredEncounterProgress.markDefender(guard, id, home, 1, "mansion", 0);
        StructureSpawnedMobData.mark(guard, home);
        guard.move(MoverType.SELF, new Vec3(-12, 30, -12));
        helper.assertTrue(guard.getBoundingBox().minX >= box.minX && guard.getBoundingBox().maxY <= box.maxY
                && guard.getBoundingBox().minZ >= box.minZ, "Authored monsters must not leave horizontally or vertically");
        var sheep = EntityType.SHEEP.create(level);
        sheep.moveTo(Vec3.atBottomCenterOf(home)); sheep.noPhysics = true;
        AuthoredEncounterProgress.markLivestock(sheep, id, home, 1, "mansion");
        StructureSpawnedMobData.mark(sheep, home);
        var damage = new LivingAttackEvent(sheep, level.damageSources().generic(), 1);
        BossStructureProtection.attack(damage);
        helper.assertTrue(damage.isCanceled(), "Locked livestock must be invulnerable");
        var boat = EntityType.BOAT.create(level);
        boat.moveTo(81990, 185, 82005);
        sheep.startRiding(boat, true); sheep.moveTo(81990, 185, 82005);
        tickBoundary(sheep);
        helper.assertTrue(!sheep.isPassenger() && box.contains(sheep.position()), "Boats must not carry locked stock outside");
        progress.defeat(guard);
        sheep.move(MoverType.SELF, new Vec3(-30, 0, 0));
        helper.assertFalse(sheep.getBoundingBox().intersects(box), "After the final defender, livestock can leave");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void brainTargetsAndAmbientMansionSpawnsCannotBypassProtection(GameTestHelper helper) {
        var level = helper.getLevel();
        var entrance = new BlockPos(83000, 180, 83000);
        var cellar = new MansionBasementPiece(entrance, Rotation.NONE, 11);
        PlacedMansions.get(level).record(cellar, List.of(new BoundingBox(83000, 180, 83000, 83020, 200, 83020)));
        var victim = EntityType.SHEEP.create(level);
        victim.moveTo(83010, 185, 83010);
        var hoglin = EntityType.HOGLIN.create(level);
        hoglin.moveTo(82990, 185, 83010);
        hoglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, victim);
        BossStructureProtection.tick(new LivingEvent.LivingTickEvent(hoglin));
        helper.assertFalse(hoglin.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET), "Brain-only attackers must forget interior targets");
        var attack = new LivingAttackEvent(victim, level.damageSources().mobAttack(hoglin), 1);
        BossStructureProtection.attack(attack);
        helper.assertTrue(attack.isCanceled(), "Already-started attacks from outsiders must fail");
        var check = new MobSpawnEvent.FinalizeSpawn(victim, level, victim.getX(), victim.getY(), victim.getZ(),
                level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.EVENT, null, null, null);
        DocumentedNaturalSpawns.finalizeSpawn(check);
        helper.assertTrue(check.isSpawnCancelled(), "Event spawners cannot refill a mansion");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeYetiSpawnerBossIsAResidentRatherThanAnOutsider(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = new BlockPos(84000, 180, 84000);
        var lair = new BossLairPiece("yeti", center, 17);
        lair.registerBoundary(level);
        var boss = TFEntities.ALPHA_YETI.get().create(level);
        boss.moveTo(Vec3.atBottomCenterOf(center.above()));
        BossStructureProtection.nativeBossJoined(new EntityJoinLevelEvent(boss, level));
        var target = EntityType.SHEEP.create(level);
        target.moveTo(Vec3.atBottomCenterOf(center.offset(5, 1, 0)));
        helper.assertTrue(StructureSpawnedMobData.isMarked(boss), "The native one-time spawner's boss needs its authored home");
        helper.assertFalse(BossStructureProtection.forbiddenTarget(boss, target), "The actual Yeti boss must retain attacks within its lair");
        helper.succeed();
    }

    private static void tickBoundary(Mob mob) {
        int id = mob.getId();
        mob.setId((int) -mob.level().getGameTime());
        BossStructureProtection.tick(new LivingEvent.LivingTickEvent(mob));
        mob.setId(id);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedLegacyCampGainsBoundariesWithoutChangingItsRoster(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(85000, 180, 85000);
        var context = net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        var tag = new net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece(level.getStructureManager(), origin).createTag(context);
        tag.putInt("SettlementVersion", 10); tag.remove("CampEncounterId");
        var legacy = new net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece(context, tag);
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                        .get(new net.minecraft.resources.ResourceLocation("aoemod:orc_camp")),
                new net.minecraft.world.level.ChunkPos(origin), 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(List.of(legacy)));
        BossStructureBounds.discover(level, start);
        var bounds = legacy.getBoundingBox();
        var home = bounds.getCenter().atY(185);
        var resident = EntityType.ZOMBIE.create(level);
        resident.moveTo(Vec3.atBottomCenterOf(home)); resident.noPhysics = true;
        StructureSpawnedMobData.mark(resident, home);
        var victim = EntityType.SHEEP.create(level);
        victim.moveTo(Vec3.atBottomCenterOf(home));
        helper.assertFalse(BossStructureProtection.forbiddenTarget(resident, victim), "Saved home tags identify legacy defenders");
        resident.move(MoverType.SELF, new Vec3(-1000, 0, 0));
        helper.assertTrue(resident.getBoundingBox().minX >= bounds.minX(), "Legacy defenders cannot leave their saved structure");
        helper.assertTrue(legacy.encounterId() == null, "Boundary migration must not invent a new roster or reset old progression");
        helper.succeed();
    }
}
