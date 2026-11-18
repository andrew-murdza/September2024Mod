package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.TropicalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Optional;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalMushroomOrderTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void actualRainforestPassFinishesTreeCrownsBeforeGiantMushrooms(GameTestHelper helper) {
        var probe = TropicalTreeShapeTests.fixture(helper, new HashMap<>(), "rainforest", 0, true);
        var layout = DimensionTerrain.tropicalLayout(probe.getLevel().getChunkSource().randomState().router().temperature());
        var plan = TropicalIslandPlan.forIsland(layout, layout.island(0, 0));
        var interior = plan.rainforest().stream().filter(chunk -> {
            var sample = layout.sample(chunk.x() * 16 + 8, chunk.z() * 16 + 8);
            return sample.distance() > 48 && sample.distance() < sample.forestEdge() - 12;
        }).toList();
        helper.assertTrue(!interior.isEmpty(), "The seeded island must provide interior rainforest chunks");
        int mushrooms = 0, trees = 0;
        for (int sample = 0; sample < 64; sample++) {
            var chunk = interior.get(sample % interior.size());
            var blocks = new RecordedBlocks();
            var level = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, true);
            TropicalVegetation.place(new FeaturePlaceContext<>(Optional.empty(), level,
                    level.getLevel().getChunkSource().getGenerator(), RandomSource.create(50881L + sample * 104729L),
                    new BlockPos(chunk.x() * 16, 0, chunk.z() * 16), NoneFeatureConfiguration.INSTANCE));
            helper.assertFalse(blocks.lateTree,
                    "No tree trunk or leafy crown may be generated after a giant mushroom in its chunk; sample=" + sample);
            mushrooms += blocks.mushrooms;
            trees += blocks.trees;
        }
        helper.assertTrue(mushrooms > 0 && trees > 0,
                "The order audit must exercise actual giant mushrooms and woody trees, not an empty canopy pass");
        helper.succeed();
    }

    private static final class RecordedBlocks extends HashMap<BlockPos, BlockState> {
        private int mushrooms, trees;
        private boolean lateTree;

        @Override
        public BlockState put(BlockPos pos, BlockState state) {
            if (state.getBlock() instanceof HugeMushroomBlock) {
                mushrooms++;
            } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                trees++;
                if (mushrooms > 0) lateTree = true;
            }
            return super.put(pos, state);
        }
    }
}
