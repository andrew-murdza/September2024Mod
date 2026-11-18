package net.amurdza.examplemod.gametest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnGenerationBoundaryTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void cornCollisionCannotReadBeyondPopulationChunk(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        var chunk = level.getChunk(140, -140);
        int x = chunk.getPos().getMinBlockX() + 8;
        int z = chunk.getPos().getMaxBlockZ();
        int y = 180;
        var corn = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:corn_stalk"));
        helper.assertTrue(corn instanceof CornStalkBlock, "The crash regression needs the actual Hearth & Harvest corn");
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -9; dz <= 0; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    var pos = new BlockPos(x + dx, y + dy, z + dz);
                    chunk.setBlockState(pos, (dy == -1 ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), false);
                }
            }
        }
        chunk.setBlockState(new BlockPos(x, y, z), corn.defaultBlockState().setValue(CornStalkBlock.AGE, 3), false);
        Holder<Biome> plains = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:plains")));
        // ChunkStatus.SPAWN supplies only the population chunk; already-loaded server
        // neighbors must not be read through this deliberately limited region.
        var region = new WorldGenRegion(level, List.of(chunk), ChunkStatus.SPAWN, -1) {
            @Override public Holder<Biome> getBiome(BlockPos pos) { return plains; }
        };
        var cow = EntityType.COW.create(level);
        helper.assertTrue(cow != null, "Could not create the spawn candidate");
        cow.moveTo(x + 0.5, y, z - 0.1, 0, 0);
        var edge = new MobSpawnEvent.PositionCheck(cow, region, MobSpawnType.CHUNK_GENERATION, null);
        DocumentedNaturalSpawns.position(edge);
        helper.assertTrue(edge.getResult() == Event.Result.DENY,
                "A collision query beside edge corn must reject the candidate before reading an absent chunk");
        var species = new DocumentedFauna.Species(new ResourceLocation("minecraft:cow"),
                DocumentedFauna.Habitat.LAND, DocumentedFauna.Depth.ANY);
        helper.assertFalse(DocumentedFauna.hasSpace(region, new BlockPos(x, y, z), species),
                "The earlier habitat check must also reject neighbor-dependent edge shapes safely");
        cow.moveTo(x + 0.5, y, z - 7.5, 0, 0);
        var interior = new MobSpawnEvent.PositionCheck(cow, region, MobSpawnType.CHUNK_GENERATION, null);
        DocumentedNaturalSpawns.position(interior);
        helper.assertTrue(interior.getResult() == Event.Result.ALLOW,
                "An unobstructed candidate wholly inside the same generation chunk must still spawn");
        chunk.setBlockState(cow.blockPosition().above(), Blocks.STONE.defaultBlockState(), false);
        var blocked = new MobSpawnEvent.PositionCheck(cow, region, MobSpawnType.CHUNK_GENERATION, null);
        DocumentedNaturalSpawns.position(blocked);
        helper.assertTrue(blocked.getResult() == Event.Result.DENY,
                "Bounds safety must retain full entity collision checks inside available chunks");
        helper.succeed();
    }
}
