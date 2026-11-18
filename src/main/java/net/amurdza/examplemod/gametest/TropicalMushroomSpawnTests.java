package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalMushroomSpawnTests {
    private static final BlockPos FEET = new BlockPos(8, 101, 8);

    @GameTest(template = "plant_gameplay_empty")
    public static void giantMushroomCanopyRejectsNativeAndDocumentedTropicalSpawns(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var tropical = AquaticPlantGenerationTests.fixture(helper, "rainforest", blocks, 0);
        var overworld = AquaticPlantGenerationTests.fixture(helper, "rainforest", blocks, 0,
                helper.getLevel().getServer().overworld());
        var land = new DocumentedFauna.Species(new ResourceLocation("minecraft:ocelot"),
                DocumentedFauna.Habitat.LAND, DocumentedFauna.Depth.ANY);
        for (Block mushroom : new Block[]{Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM}) {
            blocks.put(FEET.below(), mushroom.defaultBlockState());
            helper.assertFalse(mushroom.defaultBlockState().isValidSpawn(tropical, FEET.below(), EntityType.ZOMBIE),
                    "Native spawn-support checks must treat tropical mushroom caps and stems as canopy");
            helper.assertFalse(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,
                            tropical, FEET, EntityType.ZOMBIE),
                    "Actual native monster-position checks must reject mushroom roofs");
            helper.assertFalse(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,
                            tropical, FEET, EntityType.OCELOT),
                    "The documented-fauna override cannot restore natural canopy spawning");
            helper.assertFalse(DocumentedFauna.hasSpace(tropical, FEET, land),
                    "Documented land fauna must reject tropical mushroom caps and stems");
            // Common setup already gives mushroom caps the leaf predicate:
            // ocelots/parrots are allowed, ordinary monsters are not.
            helper.assertTrue(mushroom.defaultBlockState().isValidSpawn(overworld, FEET.below(), EntityType.OCELOT)
                            && NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,
                            overworld, FEET, EntityType.OCELOT) && DocumentedFauna.hasSpace(overworld, FEET, land),
                    "The same mushroom block retains its original Overworld ocelot support");
        }
        blocks.put(FEET.below(), Blocks.MOSS_BLOCK.defaultBlockState());
        helper.assertTrue(DocumentedFauna.hasSpace(tropical, FEET, land)
                        && NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, tropical, FEET, EntityType.OCELOT),
                "Real tropical ground must remain a valid surface for allowed land fauna");
        var zombie = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, tropical, MobSpawnType.NATURAL,
                FEET, RandomSource.create(42), true);
        DocumentedNaturalSpawns.placement(zombie);
        helper.assertTrue(zombie.getResult() == Event.Result.DENY,
                "Unlisted monsters must remain excluded even on otherwise valid tropical ground");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void mushroomEventGuardsCoverMonstersWithoutChangingSpawnEggsOrOtherDimensions(GameTestHelper helper) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(FEET.below(), Blocks.RED_MUSHROOM_BLOCK.defaultBlockState());
        var tropical = AquaticPlantGenerationTests.fixture(helper, "rainforest", blocks, 0);
        var overworld = AquaticPlantGenerationTests.fixture(helper, "rainforest", blocks, 0,
                helper.getLevel().getServer().overworld());
        for (EntityType<?> type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.OCELOT}) {
            for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                var placement = new MobSpawnEvent.SpawnPlacementCheck(type, tropical, reason, FEET,
                        RandomSource.create(42), true);
                placement.setResult(Event.Result.ALLOW);
                DocumentedNaturalSpawns.placement(placement);
                helper.assertTrue(placement.getResult() == Event.Result.DENY,
                        "Natural monster and documented animal placement cannot override the mushroom support veto");
                var mob = (net.minecraft.world.entity.Mob) type.create(tropical.getLevel());
                helper.assertTrue(mob != null, "Could not create a natural spawn candidate");
                mob.setPos(FEET.getX() + 0.5, FEET.getY(), FEET.getZ() + 0.5);
                var position = new MobSpawnEvent.PositionCheck(mob, tropical, reason, null);
                position.setResult(Event.Result.ALLOW);
                DocumentedNaturalSpawns.position(position);
                helper.assertTrue(position.getResult() == Event.Result.DENY,
                        "Natural monster and documented animal position checks cannot permit a mushroom roof");
            }
        }
        var egg = new MobSpawnEvent.SpawnPlacementCheck(EntityType.OCELOT, tropical, MobSpawnType.SPAWN_EGG,
                FEET, RandomSource.create(42), true);
        DocumentedNaturalSpawns.placement(egg);
        helper.assertTrue(egg.getResult() == Event.Result.DEFAULT,
                "Deliberate spawn eggs retain their existing behavior");
        var external = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, overworld, MobSpawnType.NATURAL,
                FEET, RandomSource.create(42), true);
        DocumentedNaturalSpawns.placement(external);
        helper.assertTrue(external.getResult() == Event.Result.DEFAULT,
                "External dimensions retain their original event decisions");
        var snowy = AquaticPlantGenerationTests.fixture(helper, "mushroom_caves", blocks, 0,
                helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY));
        helper.assertTrue(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState().isValidSpawn(snowy, FEET.below(), EntityType.OCELOT),
                "Snowy mushroom-cave blocks retain the original leaf-like ocelot support");
        helper.assertFalse(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState().isValidSpawn(snowy, FEET.below(), EntityType.MOOSHROOM),
                "The preexisting native leaf predicate must still reject mooshrooms outside the documented override");
        helper.succeed();
    }
}
