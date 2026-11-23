package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticFaunaRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void loadedWarmWaterRostersUseLobstersAndAmbientStarfish(GameTestHelper helper) {
        for (String biome : List.of("savanna_lake", "plains_lake", "mangrove_swamp", "coral_reef")) {
            boolean warm = biome.equals("savanna_lake") || biome.equals("plains_lake");
            String replacement = warm ? "alexsmobs:lobster" : "hybrid_aquatic:starfish";
            var dimension = biome.equals("savanna_lake") ? GameplayDimensions.SAVANNA
                    : biome.equals("plains_lake") ? GameplayDimensions.PLAINS : GameplayDimensions.TROPICAL;
            var level = helper.getLevel().getServer().getLevel(dimension);
            var loaded = level.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID(biome));
            Set<String> actual = Arrays.stream(MobCategory.values())
                    .flatMap(category -> loaded.getMobSettings().getMobs(category).unwrap().stream())
                    .map(spawn -> ForgeRegistries.ENTITY_TYPES.getKey(spawn.type).toString()).collect(Collectors.toSet());
            helper.assertTrue(actual.contains(replacement), "Missing requested bottom animal from the loaded biome: " + biome);
            var habitat = AquaticPlantGenerationTests.fixture(helper, biome, new HashMap<>(), 8, level,
                    Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
            for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                var placement = new MobSpawnEvent.SpawnPlacementCheck(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(replacement)),
                        habitat, reason, new BlockPos(0, 101, 0), RandomSource.create(17), true);
                DocumentedNaturalSpawns.placement(placement);
                helper.assertTrue(placement.getResult() == Event.Result.ALLOW,
                        "Requested lake/reef animal must pass actual spawn placement: " + replacement);
            }
            for (String removed : warm ? List.of("hybrid_aquatic:flower_crab", "upgrade_aquatic:perch")
                    : List.of("collectorsreap:chieftain_crab", "hybrid_aquatic:lightfoot_crab", "ecologics:coconut_crab", "angling:sea_slug")) {
                helper.assertFalse(actual.contains(removed), "Removed aquatic still appears in " + biome + ": " + removed);
                ResourceLocation removedId = new ResourceLocation(removed);
                if (removedId.getNamespace().equals("collectorsreap") || removedId.getNamespace().equals("angling")) {
                    helper.assertFalse(ForgeRegistries.ENTITY_TYPES.containsKey(removedId), "Retired dependency mob must be unregistered");
                    continue;
                }
                for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                    var event = new MobSpawnEvent.SpawnPlacementCheck(ForgeRegistries.ENTITY_TYPES.getValue(removedId),
                            habitat, reason, new BlockPos(0, 101, 0), RandomSource.create(17), true);
                    DocumentedNaturalSpawns.placement(event);
                    helper.assertTrue(event.getResult() == Event.Result.DENY, "Removed aquatic must not bypass natural spawning: " + removed);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyBottomSpeciesSplitAtTheSettledSpawnHeight(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        for (String biome : List.of("snowy_forest", "dappled_forest", "mountain_lake", "mushroom_caves")) {
            for (String id : List.of("alexsmobs:lobster", "hybrid_aquatic:spider_crab")) {
                var species = DocumentedFauna.forBiome(biome).stream().filter(entry -> entry.id().toString().equals(id)).findFirst().orElseThrow();
                helper.assertTrue(species.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER, "Both crustaceans must stand on the floor");
                for (int y : new int[]{37, 38, 39, 40}) {
                    var blocks = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
                    for (int wetY = y; wetY <= 63; wetY++) {
                        blocks.put(new BlockPos(0, wetY, 0), Blocks.WATER.defaultBlockState());
                    }
                    var habitat = AquaticPlantGenerationTests.fixture(helper, biome, blocks, 0, level,
                            Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState());
                    boolean expected = id.equals("alexsmobs:lobster") == (y >= 39);
                    for (var reason : List.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION)) {
                        var event = new MobSpawnEvent.SpawnPlacementCheck(species.type(), habitat, reason,
                                new BlockPos(0, 60, 0), RandomSource.create(17), true);
                        DocumentedNaturalSpawns.placement(event);
                        helper.assertTrue(event.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                                "Wrong floor elevation for " + id + " in " + biome + " at " + y);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void aquaticCollisionUsesVisibleSoilTopWithoutChangingSlabsOrLandAnimals(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
        var fish = EntityType.COD.create(level);
        var cow = EntityType.COW.create(level);
        helper.assertTrue(fish != null && cow != null, "Collision fixture needs native entities");
        for (var block : List.of(Blocks.MUD, Blocks.SOUL_SAND)) {
            var state = block.defaultBlockState();
            double fishTop = state.getCollisionShape(level, floor, CollisionContext.of(fish)).max(Direction.Axis.Y);
            double cowTop = state.getCollisionShape(level, floor, CollisionContext.of(cow)).max(Direction.Axis.Y);
            double contextFree = state.getCollisionShape(level, floor, CollisionContext.empty()).max(Direction.Axis.Y);
            helper.assertTrue(fishTop == 1.0 && cowTop == .875 && contextFree == .875,
                    "Only aquatic entity collision should stand at the visible soil top: " + block);
            helper.setBlock(new BlockPos(1, 1, 1), block);
            fish.setPos(floor.getX() + .5, floor.getY() + 1.5, floor.getZ() + .5);
            fish.move(MoverType.SELF, new Vec3(0, -1, 0));
            helper.assertTrue(Math.abs(fish.getY() - floor.getY() - 1.0) < 1.0e-6,
                    "Normal collision movement must stop above the visible " + block + " surface without teleporting");
        }
        var slab = Blocks.STONE_SLAB.defaultBlockState();
        helper.assertTrue(slab.getCollisionShape(level, floor, CollisionContext.of(fish)).max(Direction.Axis.Y) == .5,
                "Real half-height blocks must retain their normal collision shape");
        fish.discard();
        cow.discard();
        helper.succeed();
    }
}
