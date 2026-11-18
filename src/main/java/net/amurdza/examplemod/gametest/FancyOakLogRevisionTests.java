package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FancyOakLogRevisionTests {
    private static final BlockPos ROOT = new BlockPos(8, 101, 8);

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void nativeFancyCrownsKeepTheirSupportsWithoutRedundantLowerLogs(GameTestHelper helper) {
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(new ResourceLocation(AOEMod.MOD_ID, "rainforest/tree/natural_fancy_oak"));
        var config = (TreeConfiguration) feature.config();
        int removedNativeLogs = 0;
        for (long seed = 0; seed < 64; seed++) {
            Skeleton nativeTree = trunk(helper, config, seed, false);
            Skeleton tropical = trunk(helper, config, seed, true);
            helper.assertTrue(!nativeTree.crowns().isEmpty() && nativeTree.crowns().equals(tropical.crowns()),
                    "Pruning must retain every native crown anchor in its original position, seed=" + seed);
            Set<BlockPos> protectedLogs = new HashSet<>(nativeTree.crowns());
            nativeTree.logs().stream().filter(pos -> pos.getX() == ROOT.getX() && pos.getZ() == ROOT.getZ())
                    .forEach(protectedLogs::add);
            helper.assertTrue(tropical.logs().containsAll(protectedLogs),
                    "The complete rooted trunk and all foliage-supporting branch tips must survive, seed=" + seed);
            helper.assertTrue(reachable(tropical.logs()).equals(tropical.logs()),
                    "Every retained branch must remain face-connected to the root, seed=" + seed);
            for (BlockPos log : tropical.logs()) {
                if (protectedLogs.contains(log) || !tropical.logs().contains(log.above())) continue;
                Set<BlockPos> without = new HashSet<>(tropical.logs()); without.remove(log);
                helper.assertFalse(reachable(without).equals(without),
                        "An unprotected lower log still has a duplicate route and should be absent: " + log + ", seed=" + seed);
            }
            Set<BlockPos> removed = new HashSet<>(nativeTree.logs()); removed.removeAll(tropical.logs());
            helper.assertTrue(removed.stream().allMatch(pos -> tropical.logs().stream().anyMatch(higher ->
                            higher.getX() == pos.getX() && higher.getZ() == pos.getZ() && higher.getY() > pos.getY())),
                    "Native branch reduction must remove only lower duplicates, seed=" + seed);
            removedNativeLogs += removed.size();
        }
        helper.assertTrue(removedNativeLogs > 0,
                "This seeded regression must exercise native lower-log pruning, rather than only added connectors");
        AOEMod.LOGGER.info("Fancy-oak pruning regression: all crown anchors preserved across 64 trees; {} redundant native lower logs removed.",
                removedNativeLogs);
        helper.succeed();
    }

    private static Skeleton trunk(GameTestHelper helper, TreeConfiguration config, long seed, boolean tropical) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        var world = TropicalTreeShapeTests.fixture(helper, blocks, "rainforest", 0, tropical);
        RandomSource random = RandomSource.create(seed);
        int height = config.trunkPlacer.getTreeHeight(random);
        var foliage = config.trunkPlacer.placeTrunk(world,
                (pos, state) -> blocks.put(pos.immutable(), state), random, height, ROOT, config);
        Set<BlockPos> logs = new HashSet<>();
        blocks.forEach((pos, state) -> { if (state.is(BlockTags.LOGS)) logs.add(pos); });
        return new Skeleton(Set.copyOf(logs), foliage.stream().map(attachment -> attachment.pos().immutable()).toList());
    }

    /** Independent graph traversal: this test does not use the production pruning or connectivity helper. */
    private static Set<BlockPos> reachable(Set<BlockPos> logs) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(); queue.add(ROOT);
        while (!queue.isEmpty()) {
            BlockPos next = queue.removeFirst();
            if (!logs.contains(next) || !visited.add(next)) continue;
            for (Direction direction : Direction.values()) queue.add(next.relative(direction));
        }
        return visited;
    }

    private record Skeleton(Set<BlockPos> logs, List<BlockPos> crowns) { }
}
