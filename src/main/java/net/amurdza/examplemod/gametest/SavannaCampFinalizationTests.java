package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.PlacedOrcVillages;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampFinalizationTests {
    private SavannaCampFinalizationTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void customSpawnerFinalizationCannotBypassCampRules(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos center = new BlockPos(26000, 95, 26000);
        PlacedOrcVillages.get(level).record(center);
        for (var type : new EntityType<?>[]{EntityType.PHANTOM, EntityType.ZOMBIE, EntityType.SKELETON}) {
            for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION,
                    MobSpawnType.SPAWNER, MobSpawnType.REINFORCEMENT, MobSpawnType.EVENT, MobSpawnType.JOCKEY}) {
                Mob mob = (Mob) type.create(level);
                mob.moveTo(center, 0, 0);
                ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(center), reason, null, null);
                helper.assertTrue(mob.isSpawnCancelled(), "Actual Forge finalization must cancel camp undead: " + type + "/" + reason);
                helper.assertFalse(level.addFreshEntity(mob), "Canceled initialization alone must not let the entity join the camp");
                mob.discard();
            }
        }
        for (var reason : new MobSpawnType[]{MobSpawnType.COMMAND, MobSpawnType.SPAWN_EGG, MobSpawnType.STRUCTURE,
                MobSpawnType.CONVERSION}) {
            var zombie = EntityType.ZOMBIE.create(level);
            zombie.moveTo(center, 0, 0);
            ForgeEventFactory.onFinalizeSpawn(zombie, level, level.getCurrentDifficultyAt(center), reason, null, null);
            helper.assertFalse(zombie.isSpawnCancelled(), "Manual, authored and conversion routes remain unchanged: " + reason);
            zombie.discard();
        }
        for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.STRUCTURE}) {
            var sheep = EntityType.SHEEP.create(level);
            sheep.moveTo(center, 0, 0);
            ForgeEventFactory.onFinalizeSpawn(sheep, level, level.getCurrentDifficultyAt(center), reason, null, null);
            helper.assertTrue(sheep.isSpawnCancelled() == (reason != MobSpawnType.STRUCTURE),
                    "Only the authored camp livestock may finalize inside a pen: " + reason);
            if (reason == MobSpawnType.STRUCTURE) helper.assertTrue(level.addFreshEntity(sheep), "Authored livestock must still join");
            sheep.discard();
        }
        var outside = EntityType.PHANTOM.create(level);
        BlockPos beyondCamp = center.offset(61, 0, 0);
        outside.moveTo(beyondCamp, 0, 0);
        ForgeEventFactory.onFinalizeSpawn(outside, level, level.getCurrentDifficultyAt(beyondCamp), MobSpawnType.NATURAL, null, null);
        helper.assertFalse(outside.isSpawnCancelled(), "Camp exclusion must end at the footprint boundary");
        outside.discard();
        helper.succeed();
    }
}
