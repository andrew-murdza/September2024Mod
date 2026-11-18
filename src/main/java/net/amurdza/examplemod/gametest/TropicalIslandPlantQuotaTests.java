package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalIslandPlantQuotaTests {
    private static final TicketType<ChunkPos> MOUND_ENTITY_AUDIT = TicketType.create("aoemod_mound_entity_audit",
            java.util.Comparator.comparingLong(ChunkPos::toLong), 2400);
    private static final Set<String> FOREST_CROPS = Set.of("minecraft:red_mushroom", "minecraft:brown_mushroom",
            "minecraft:melon", "farmersdelight:wild_tomatoes", "pineapple_delight:pineapple_wild_crop",
            "minecraft:azalea", "minecraft:flowering_azalea", "farmersdelight:wild_carrots",
            "legumedelight:wild_beans");

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void fullyDecoratedIslandRetainsItsReservedPlantPatches(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var layout = DimensionTerrain.tropicalLayout(level.getChunkSource().randomState().router().temperature());
        var island = layout.island(4803, 119);
        var plan = TropicalIslandPlan.forIsland(layout, island);
        Set<TropicalIslandPlan.Chunk> chunks = new LinkedHashSet<>();
        chunks.addAll(plan.rainforest());
        chunks.addAll(plan.cherry());
        chunks.addAll(plan.mounds());
        chunks.addAll(plan.shallow());
        AOEMod.LOGGER.info("Generating {} full tropical chunks for actual plant quota audit at {},{}", chunks.size(), island.x(), island.z());
        // Complete every neighbouring owner's decoration before counting so a
        // later tree, grass pass, or water feature cannot silently delete roots.
        for (var chunk : chunks) level.getChunk(chunk.x(), chunk.z());
        int forestPatches = 0, moundPatches = 0, cherryPatches = 0, wetPatches = 0;
        for (var chunk : chunks) {
            String forest = plan.rainforestSpecies(chunk.x(), chunk.z());
            String mound = plan.moundSpecies(chunk.x(), chunk.z());
            String cherry = plan.cherryCrop(chunk.x(), chunk.z());
            String wet = plan.wetCrop(chunk.x(), chunk.z());
            for (String species : List.of(forest == null ? "" : forest, mound == null ? "" : mound,
                    cherry == null ? "" : cherry, wet == null ? "" : wet)) {
                if (species.isEmpty()) continue;
                boolean azalea = species.equals("minecraft:azalea") || species.equals("minecraft:flowering_azalea");
                boolean fixed = FOREST_CROPS.contains(species) || species.contains("wild_")
                        || species.equals("minecraft:sugar_cane");
                if (!fixed) continue;
                var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(species));
                int roots = 0;
                Set<BlockPos> wetRoots = new LinkedHashSet<>();
                for (int x = chunk.x() * 16; x < chunk.x() * 16 + 16; x++) {
                    for (int z = chunk.z() * 16; z < chunk.z() * 16 + 16; z++) for (int y = 60; y < level.getMaxBuildHeight(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        if (!state.is(block)) continue;
                        // Full-block crops such as melons correctly stop findGround;
                        // only cave-owned interior mushrooms are outside this quota.
                        if (net.amurdza.examplemod.worldgen.structure.BossLairPiece.isInsideSpiderCave(level, pos)) continue;
                        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) continue;
                        if (state.is(Blocks.SUGAR_CANE) && level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)) continue;
                        roots++;
                        if (species.equals(wet)) wetRoots.add(pos);
                    }
                }
                helper.assertTrue(azalea ? roots >= 6 && roots <= 9 : roots == (species.equals("minecraft:melon") ? 2 : 5),
                        "Actual reserved patch has wrong root count: " + species + " chunk=" + chunk + " roots=" + roots);
                if (!wetRoots.isEmpty()) {
                    for (var root : wetRoots) {
                        helper.assertTrue(plan.wetCropRoot(root.getX(), root.getZ()),
                                "Fresh wet crops must occupy their protected one-deep footprint: " + root);
                        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                var id = ForgeRegistries.BLOCKS.getKey(level.getBlockState(root.offset(dx, dy, dz)).getBlock());
                                helper.assertFalse(id.getPath().contains("pickerelweed") || id.getPath().contains("dripleaf"),
                                        "Neighboring aquatic vegetation blocked the reserved wet patch: " + root);
                            }
                        }
                    }
                    var pending = new java.util.ArrayDeque<BlockPos>();
                    pending.add(wetRoots.iterator().next());
                    while (!pending.isEmpty()) {
                        BlockPos root = pending.remove();
                        if (wetRoots.remove(root)) {
                            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) pending.add(root.relative(direction));
                        }
                    }
                    helper.assertTrue(wetRoots.isEmpty(), "All five naturally generated wet crop roots must touch: "
                            + species + " chunk=" + chunk);
                }
            }
            if (forest != null && FOREST_CROPS.contains(forest)) forestPatches++;
            if (mound != null && (mound.contains("mushroom") || mound.contains("wild_"))) moundPatches++;
            if (cherry != null) cherryPatches++;
            if (wet != null) wetPatches++;
        }
        helper.assertTrue(forestPatches == 132 && moundPatches == 64
                        && cherryPatches == 22 && wetPatches == 168,
                "Reserved plant patches must match the island's rainforest, mound, cherry and shallow habitats");
        // getChunk creates FULL terrain but leaves remote entity sections hidden
        // without a player/ticket. Temporarily make the eight selected mounds
        // accessible before counting their actual loaded animals. These tickets
        // also expire if an assertion fails, so the test never leaves forced chunks.
        var animalChunks = plan.mounds().subList(0, 8).stream().map(chunk -> new ChunkPos(chunk.x(), chunk.z())).toList();
        for (ChunkPos chunk : animalChunks) level.getChunkSource().addRegionTicket(MOUND_ENTITY_AUDIT, chunk, 2, chunk);
        double reach = net.amurdza.examplemod.worldgen.TropicalLayout.ISLAND_RADIUS + 16;
        var bounds = new net.minecraft.world.phys.AABB(island.x() - reach, 55, island.z() - reach,
                island.x() + reach, 100, island.z() + reach);
        helper.startSequence().thenWaitUntil(() -> {
            var animals = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, bounds,
                    net.amurdza.examplemod.worldgen.TropicalMoundFauna::isMoundAnimal);
            helper.assertTrue(animals.size() == 16, "Full natural generation needs 16 actual mound animals; got " + animals.size());
            for (int i = 0; i < 8; i++) {
                var chunk = plan.mounds().get(i);
                long origin = net.minecraft.world.level.ChunkPos.asLong(chunk.x(), chunk.z());
                String species = net.amurdza.examplemod.worldgen.TropicalMoundFauna.species(layout, chunk.x(), chunk.z());
                long count = animals.stream().filter(entity -> net.amurdza.examplemod.worldgen.TropicalMoundFauna.belongsToMound(entity, origin)
                        && ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString().equals(species)).count();
                helper.assertTrue(count == 2, "Natural mound " + chunk + " needs two " + species + "; got " + count);
            }
        }).thenExecute(() -> {
            for (ChunkPos chunk : animalChunks) level.getChunkSource().removeRegionTicket(MOUND_ENTITY_AUDIT, chunk, 2, chunk);
        }).thenSucceed();
    }
}
