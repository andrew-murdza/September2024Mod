package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.BossStructureBounds;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.projectile.FallingIce;
import twilightforest.entity.projectile.IceBomb;
import twilightforest.init.TFEntities;
import twilightforest.init.TFMobEffects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Invoke the bundled boss and projectile methods, including their direct block-write paths. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoblinFortressBossTests {
    private GoblinFortressBossTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeYetiKeepsTheCeilingAndItsTimedIceAttack(GameTestHelper helper) throws Exception {
        try (var arena = new Arena(helper)) {
            var ceiling = arena.base.above(6);
            arena.set(ceiling, Blocks.BLUE_ICE.defaultBlockState());
            AlphaYeti boss = TFEntities.ALPHA_YETI.get().create(arena.level);
            helper.assertTrue(boss != null, "The native Alpha Yeti must exist");
            invoke(boss, "makeBlockFall", new Class<?>[]{BlockPos.class, int.class}, ceiling, 4);
            var attacks = arena.level.getEntitiesOfClass(FallingIce.class, new AABB(ceiling).inflate(2));
            helper.assertTrue(attacks.size() == 1, "The preserved ceiling must still produce one native falling-ice attack");
            var ice = attacks.get(0); arena.entities.add(ice);
            helper.assertTrue(arena.level.getBlockState(ceiling).is(Blocks.BLUE_ICE)
                            && ice.getBlockState().is(Blocks.BLUE_ICE) && ice.getBoundingBox().maxY < ceiling.getY(),
                    "The projectile must hang below the intact ceiling, using the original ice state");
            double initialY = ice.getY();
            for (int tick = 0; tick < 3; tick++) ice.tick();
            helper.assertTrue(ice.isNoGravity() && ice.getY() == initialY,
                    "The requested native warning hang-time must remain intact");
            ice.tick();
            helper.assertTrue(!ice.isNoGravity() && ice.getY() < initialY,
                    "Native gravity must start once the warning expires");

            var outside = ceiling.east(7);
            arena.set(outside.below(), Blocks.AIR.defaultBlockState());
            arena.set(outside, Blocks.BLUE_ICE.defaultBlockState());
            invoke(boss, "makeBlockFall", new Class<?>[]{BlockPos.class, int.class}, outside, 4);
            arena.entities.addAll(arena.level.getEntitiesOfClass(FallingIce.class, new AABB(outside).inflate(2)));
            helper.assertTrue(arena.level.getBlockState(outside).isAir(),
                    "Outside the fortress, the native attack must still extract its original ceiling block");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeFallingIceHurtsAndDisappearsWithoutPlacingFortressBlocks(GameTestHelper helper) {
        try (var arena = new Arena(helper)) {
            var ice = new FallingIce(arena.level, arena.base.getX() + .5, arena.base.getY() + 7,
                    arena.base.getZ() + .5, Blocks.BLUE_ICE.defaultBlockState(), 0);
            arena.add(ice);
            for (int tick = 0; tick < 100 && !ice.isRemoved(); tick++) ice.tick();
            helper.assertTrue(ice.isRemoved() && arena.level.getBlockState(arena.base.above()).isAir()
                            && arena.level.getBlockState(arena.base).is(Blocks.STONE_BRICKS),
                    "A real falling-ice landing must discard its entity while leaving the floor and walking space unchanged");

            var victim = EntityType.PIG.create(arena.level);
            helper.assertTrue(victim != null, "The native damage target must exist");
            victim.moveTo(arena.base.getX() + .5, arena.base.getY() + 1, arena.base.getZ() + .5, 0, 0);
            arena.add(victim);
            var damaging = new FallingIce(arena.level, victim.getX(), victim.getY(), victim.getZ(), Blocks.BLUE_ICE.defaultBlockState(), 0);
            // GameTest servers may use Peaceful; exercise the native Normal
            // damage multiplier on this fixture without changing world settings.
            damaging.damagePerDifficulty[arena.level.getDifficulty().getId()] = 1;
            float before = victim.getHealth();
            damaging.causeFallDamage(9, 1, arena.level.damageSources().fall());
            helper.assertTrue(victim.getHealth() < before, "Preserving architecture must not remove native falling-ice damage");

            var outside = arena.base.east(7);
            arena.set(outside, Blocks.STONE_BRICKS.defaultBlockState());
            arena.set(outside.above(), Blocks.AIR.defaultBlockState());
            var ordinary = new FallingIce(arena.level, outside.getX() + .5, outside.getY() + 2,
                    outside.getZ() + .5, Blocks.BLUE_ICE.defaultBlockState(), 0);
            arena.add(ordinary);
            for (int tick = 0; tick < 100 && !ordinary.isRemoved(); tick++) ordinary.tick();
            helper.assertTrue(ordinary.isRemoved() && arena.level.getBlockState(outside.above()).is(Blocks.BLUE_ICE),
                    "Outside the estate, native falling ice must still place its block on landing");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeIceBombPreservesTerrainAndDefendersButStillDamagesAndSlows(GameTestHelper helper) throws Exception {
        try (var arena = new Arena(helper)) {
            var water = arena.base.offset(1, 1, 1);
            arena.set(water, Blocks.WATER.defaultBlockState());
            var bomb = new IceBomb(arena.level, Vec3.atCenterOf(arena.base.above()));
            invoke(bomb, "doTerrainEffect", new Class<?>[]{BlockPos.class}, water);
            invoke(bomb, "doTerrainEffect", new Class<?>[]{BlockPos.class}, arena.base.above());
            helper.assertTrue(arena.level.getBlockState(water).is(Blocks.WATER)
                            && arena.level.getBlockState(arena.base.above()).isAir(),
                    "Native ice bombs must neither freeze authored water nor build snow inside the estate");

            var yeti = TFEntities.YETI.get().create(arena.level);
            var insideOrdinaryYeti = TFEntities.YETI.get().create(arena.level);
            var victim = EntityType.PIG.create(arena.level);
            helper.assertTrue(yeti != null && insideOrdinaryYeti != null && victim != null, "All real native targets must exist");
            yeti.moveTo(arena.base.getX() - .5, arena.base.getY() + 1, arena.base.getZ() + .5, 0, 0);
            insideOrdinaryYeti.moveTo(arena.base.getX() + .5, arena.base.getY() + 1, arena.base.getZ() - .5, 0, 0);
            insideOrdinaryYeti.xOld = insideOrdinaryYeti.getX(); insideOrdinaryYeti.yOld = insideOrdinaryYeti.getY();
            insideOrdinaryYeti.zOld = insideOrdinaryYeti.getZ();
            victim.moveTo(arena.base.getX() + 1.5, arena.base.getY() + 1, arena.base.getZ() + .5, 0, 0);
            AuthoredEncounterProgress.markDefender(yeti, arena.encounter, arena.base,
                    GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.ENCOUNTER_KIND, 0);
            arena.add(yeti); arena.add(insideOrdinaryYeti); arena.add(victim);
            AuthoredEncounterProgress.get(arena.level).track(yeti);
            float before = victim.getHealth();
            invoke(bomb, "hitNearbyEntities", new Class<?>[0]);
            helper.assertTrue(!yeti.isRemoved() && yeti.isAlive()
                            && !arena.level.getBlockState(yeti.blockPosition()).is(Blocks.ICE),
                    "An authored regular Yeti must not be silently discarded and replaced by ice");
            helper.assertTrue(!insideOrdinaryYeti.isRemoved()
                            && !arena.level.getBlockState(insideOrdinaryYeti.blockPosition()).is(Blocks.ICE)
                            && !arena.level.getBlockState(insideOrdinaryYeti.blockPosition().above()).is(Blocks.ICE),
                    "An ordinary Yeti inside the fortress must not bypass terrain protection by converting into two ice blocks");
            helper.assertTrue(victim.getHealth() < before && victim.hasEffect(TFMobEffects.FROSTY.get()),
                    "Other living targets must still receive the native ice-bomb damage and slowing effect");
            helper.assertTrue(AuthoredEncounterProgress.get(arena.level).defeatedCount(arena.encounter) == 0,
                    "Protecting a living defender must not fabricate a victory or death record");

            var outside = arena.base.east(7).above();
            arena.set(outside.below(), Blocks.STONE_BRICKS.defaultBlockState());
            arena.set(outside, Blocks.WATER.defaultBlockState());
            invoke(bomb, "doTerrainEffect", new Class<?>[]{BlockPos.class}, outside);
            helper.assertTrue(arena.level.getBlockState(outside).is(Blocks.ICE),
                    "Native terrain freezing outside the fortress must remain unchanged");
            arena.set(outside, Blocks.AIR.defaultBlockState());
            arena.set(outside.above(), Blocks.AIR.defaultBlockState());
            var ordinaryYeti = TFEntities.YETI.get().create(arena.level);
            helper.assertTrue(ordinaryYeti != null, "The outside native Yeti must exist");
            ordinaryYeti.moveTo(outside.getX() + .5, outside.getY(), outside.getZ() + .5, 0, 0);
            // The native conversion intentionally uses the previous position.
            ordinaryYeti.xOld = ordinaryYeti.getX(); ordinaryYeti.yOld = ordinaryYeti.getY(); ordinaryYeti.zOld = ordinaryYeti.getZ();
            arena.add(ordinaryYeti);
            // A displaced authored defender still belongs to the fixed roster,
            // even beyond its normal movement boundary.
            yeti.moveTo(outside.getX() + 1.5, outside.getY(), outside.getZ() + .5, 0, 0);
            yeti.xOld = yeti.getX(); yeti.yOld = yeti.getY(); yeti.zOld = yeti.getZ();
            var outsideBomb = new IceBomb(arena.level, Vec3.atCenterOf(outside));
            invoke(outsideBomb, "hitNearbyEntities", new Class<?>[0]);
            helper.assertTrue(ordinaryYeti.isRemoved() && arena.level.getBlockState(outside).is(Blocks.ICE),
                    "Ordinary non-fortress Yetis must retain Twilight Forest's native conversion behavior");
            helper.assertTrue(!yeti.isRemoved() && yeti.isAlive(),
                    "An authored fortress defender displaced outside its estate must not be silently discarded either");
        }
        helper.succeed();
    }

    private static void invoke(Object target, String name, Class<?>[] parameters, Object... args) throws Exception {
        var method = target.getClass().getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static final class Arena implements AutoCloseable {
        private final ServerLevel level;
        private final BlockPos base;
        private final UUID encounter = UUID.randomUUID();
        private final String key = GoblinFortressPiece.ENCOUNTER_KIND + ":" + encounter;
        private final Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        private final ArrayList<Entity> entities = new ArrayList<>();

        private Arena(GameTestHelper helper) {
            level = helper.getLevel();
            base = helper.absolutePos(new BlockPos(4, 0, 4)).atY(244);
            for (var pos : BlockPos.betweenClosed(base.offset(-2, 0, -2), base.offset(2, 8, 2)))
                set(pos.immutable(), (pos.getY() == base.getY() ? Blocks.STONE_BRICKS : Blocks.AIR).defaultBlockState());
            AuthoredEncounterProgress.get(level).register(encounter, base,
                    GoblinFortressPiece.DEFENDER_COUNT, GoblinFortressPiece.ENCOUNTER_KIND);
            BossStructureBounds.get(level).record(key, new AABB(base.offset(-2, 0, -2), base.offset(3, 9, 3)), encounter);
        }

        private void set(BlockPos pos, BlockState state) {
            previous.putIfAbsent(pos, level.getBlockState(pos));
            level.setBlock(pos, state, 2);
        }

        private void add(Entity entity) {
            if (!level.addFreshEntity(entity)) throw new IllegalStateException("Native test entity insertion failed: " + entity.getType());
            entities.add(entity);
        }

        @Override public void close() {
            entities.forEach(Entity::discard);
            BossStructureBounds.get(level).remove(key);
            previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
    }
}
