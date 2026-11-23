package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalAquaticSpawnTests {
    private static final List<String> BOTTOM_SPECIES = List.of(
            "hybrid_aquatic:starfish");

    @GameTest(template = "plant_gameplay_empty")
    public static void reefAndMangrovePoolsAdmitShallowSeabedFaunaAndDolphins(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (String biome : List.of("mangrove_swamp", "coral_reef")) {
            for (String id : List.of("hybrid_aquatic:starfish", "minecraft:dolphin")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                int depth = id.equals("minecraft:dolphin") ? 3 : 1;
                var water = AquaticPlantGenerationTests.fixture(helper, biome, new HashMap<>(), depth, tropical,
                        Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState());
                BlockPos feet = new BlockPos(0, depth == 3 ? 102 : 101, 0);
                var species = DocumentedFauna.find(water, feet, type);
                helper.assertTrue(species != null && NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type),
                                water, feet, type) && SpawnPlacements.checkSpawnRules(type, water, MobSpawnType.NATURAL,
                                feet, RandomSource.create(42)), "Natural spawning chain failed for " + id + " in " + biome);
                helper.assertTrue(water.getBiome(feet).value().getMobSettings().getMobs(DocumentedFauna.spawnCategory(type))
                                .unwrap().stream().anyMatch(row -> row.type == type && row.getWeight().asInt() == DocumentedFauna.spawnWeight(species)),
                        "Missing active aquatic pool entry for " + id + " in " + biome);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalTurtlesStopAtTwoResidentsPerCoast(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var player = new net.minecraftforge.common.util.FakePlayer(tropical,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TurtleCoast"));
        var pos = tropical.getSharedSpawnPos().atY(104);
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        tropical.players().add(player);
        var retained = new java.util.ArrayList<net.minecraft.world.entity.animal.Turtle>();
        var candidate = net.minecraft.world.entity.EntityType.TURTLE.create(tropical);
        try {
            var water = AquaticPlantGenerationTests.fixture(helper, "coral_reef", new HashMap<>(), 8, tropical,
                    Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState());
            candidate.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            for (int i = 0; i < 2; i++) {
                var resident = net.minecraft.world.entity.EntityType.TURTLE.create(tropical);
                resident.setPos(pos.getX() + 6 + i * 4, pos.getY(), pos.getZ());
                retained.add(resident);
                helper.assertTrue(tropical.addFreshEntity(resident), "Could not admit coastal turtle fixture");
            }
            var check = new net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck(candidate, water,
                    MobSpawnType.NATURAL, null);
            net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns.position(check);
            helper.assertTrue(check.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                    "The ordinary natural spawn event must deny a third resident");
        } finally {
            retained.forEach(net.minecraft.world.entity.Entity::discard);
            candidate.discard();
            tropical.players().remove(player);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bottomAnimalsStandAboveWaterloggedRoots(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos root = new BlockPos(0, 101, 0);
        BlockPos candidate = root.above(5);
        for (String biome : List.of("coral_reef", "mangrove_swamp")) {
            var blocks = new HashMap<BlockPos, BlockState>();
            blocks.put(root, Blocks.MANGROVE_ROOTS.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true));
            blocks.put(root.above(), Blocks.SEAGRASS.defaultBlockState());
            var water = AquaticPlantGenerationTests.fixture(helper, biome, blocks, 8, tropical,
                    Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState());
            helper.assertTrue(water.getFluidState(root).is(FluidTags.WATER)
                            && !water.getBlockState(root).getCollisionShape(water, root).isEmpty()
                            && water.getBlockState(root).isFaceSturdy(water, root, Direction.UP),
                    "The fixture must contain a solid, waterlogged supporting root");
            for (String id : BOTTOM_SPECIES) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                var species = DocumentedFauna.find(water, candidate, type);
                helper.assertTrue(species != null, "Missing bottom species " + biome + "/" + id);
                helper.assertTrue(DocumentedFauna.groundedSpawnPos(water, candidate, species).equals(root.above()),
                        "Bottom animals must settle above roots, not inside them: " + id);
                helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), water, candidate, type)
                                && SpawnPlacements.checkSpawnRules(type, water, MobSpawnType.NATURAL, candidate, RandomSource.create(19)),
                        "Waterlogged supporting roots must not prevent the natural placement chain: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bottomAnimalsDescendThroughPassableWaterPlants(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos feet = new BlockPos(0, 101, 0);
        BlockPos candidate = feet.above(5);
        for (BlockState plant : List.of(Blocks.SEAGRASS.defaultBlockState(), Blocks.KELP_PLANT.defaultBlockState())) {
            var blocks = new HashMap<BlockPos, BlockState>();
            for (int y = 0; y < 5; y++) blocks.put(feet.above(y), plant);
            var water = AquaticPlantGenerationTests.fixture(helper, "coral_reef", blocks, 8, tropical,
                    Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState());
            helper.assertTrue(plant.getFluidState().is(FluidTags.WATER)
                            && plant.getCollisionShape(water, feet).isEmpty(),
                    "The fixture must contain passable aquatic plants");
            for (String id : BOTTOM_SPECIES) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                var species = DocumentedFauna.find(water, candidate, type);
                helper.assertTrue(species != null, "Missing bottom species " + id);
                helper.assertTrue(DocumentedFauna.groundedSpawnPos(water, candidate, species).equals(feet)
                                && DocumentedFauna.hasSpace(water, candidate, species),
                        "Passable water plants must remain part of the connected spawning column: " + id);
            }
        }
        helper.succeed();
    }
}
