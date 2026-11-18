package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlainsSpawnFollowupTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void plainsNaturalAnimalsRequireDirtTaggedActualSupport(GameTestHelper helper) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos feet = new BlockPos(8216, 190, 8216);
        var chunk = habitat(plains, feet, "plains");
        var supports = new BlockState[]{Blocks.OAK_PLANKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                Blocks.STONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState(),
                Blocks.PODZOL.defaultBlockState(), Blocks.MYCELIUM.defaultBlockState()};
        for (var support : supports) {
            chunk.setBlockState(feet.below(), support, false);
            boolean dirt = support.is(BlockTags.DIRT);
            for (var species : DocumentedFauna.forBiome("plains")) {
                if (species.habitat() != DocumentedFauna.Habitat.LAND) continue;
                for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                    helper.assertTrue(SpawnPlacements.checkSpawnRules(species.type(), plains, reason, feet, plains.random) == dirt,
                            "Native placement must distinguish mansion roofs from all dirt-tag substrates: " + species.id() + " / " + support);
                    var mob = (Mob) species.type().create(plains);
                    mob.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                    var position = new MobSpawnEvent.PositionCheck(mob, plains, reason, null);
                    // Ensure an earlier source-mod approval cannot admit a roof animal.
                    position.setResult(Event.Result.ALLOW);
                    DocumentedNaturalSpawns.position(position);
                    helper.assertTrue(position.getResult() == (dirt ? Event.Result.ALLOW : Event.Result.DENY),
                            "The actual entity position gate must independently enforce dirt support: " + species.id() + " / " + support);
                    mob.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void plainsSoilRulePreservesManualSpawnsOtherDimensionsAndWaterFauna(GameTestHelper helper) {
        var plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos feet = new BlockPos(8248, 190, 8216);
        var chunk = habitat(plains, feet, "plains");
        chunk.setBlockState(feet.below(), Blocks.OAK_PLANKS.defaultBlockState(), false);
        for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.SPAWN_EGG, MobSpawnType.COMMAND, MobSpawnType.BREEDING}) {
            var placement = new MobSpawnEvent.SpawnPlacementCheck(EntityType.COW, plains, reason, feet, plains.random, true);
            DocumentedNaturalSpawns.placement(placement);
            helper.assertTrue(placement.getResult() == Event.Result.DEFAULT, "Manual/breeding placement policy must remain unchanged");
            var cow = EntityType.COW.create(plains);
            cow.moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
            var position = new MobSpawnEvent.PositionCheck(cow, plains, reason, null);
            DocumentedNaturalSpawns.position(position);
            helper.assertTrue(position.getResult() == Event.Result.DEFAULT, "Manual/breeding positions must not be rejected by the natural soil rule");
            cow.discard();
        }
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var other = habitat(snowy, feet, "dappled_forest");
        other.setBlockState(feet.below(), Blocks.OAK_PLANKS.defaultBlockState(), false);
        var yak = DocumentedFauna.find(snowy, feet, EntityType.COW);
        helper.assertTrue(yak != null && DocumentedFauna.hasSpace(snowy, feet, yak),
                "The Plains dirt-tag requirement must not alter another dimension's land habitats");
        chunk = habitat(plains, feet, "plains_lake");
        for (int y = -2; y <= 2; y++) chunk.setBlockState(feet.offset(0, y, 0), Blocks.WATER.defaultBlockState(), false);
        chunk.setBlockState(feet.below(3), Blocks.STONE.defaultBlockState(), false);
        var cod = DocumentedFauna.find(plains, feet, EntityType.COD);
        helper.assertTrue(cod != null && DocumentedFauna.hasSpace(plains, feet, cod),
                "Plains fish must retain open-water spawning above a stone lake bed");
        helper.succeed();
    }

    private static LevelChunk habitat(ServerLevel level, BlockPos feet, String name) {
        LevelChunk chunk = level.getChunkAt(feet);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, AOEMod.makeID(name)));
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-3, 0, -3), feet.offset(3, 5, 3)))
            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
        return chunk;
    }
}
