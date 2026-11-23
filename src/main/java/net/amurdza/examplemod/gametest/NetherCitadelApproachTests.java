package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelApproach;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real seeded base-column checks; never generates a neighbour to survey a doorway. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherCitadelApproachTests {
    private NetherCitadelApproachTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 1200)
    public static void acceptedSitesMeetNativeTerrainWithoutCliffsOrBlockedMouths(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int accepted = 0, verifiedMouths = 0;
        for (int candidate = 0; candidate < 16 && accepted < 3; candidate++) {
            var origin = new BlockPos(8 + candidate % 4 * 512, NetherCitadelLayout.MIN_Y,
                    8 + candidate / 4 * 512);
            var approach = NetherCitadelApproach.sample(origin, generator, random, level);
            if (!approach.hasUpperEntrance()) continue;
            accepted++;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                for (int cross = -6; cross <= 6; cross++) {
                    int worldX = origin.getX() + (side.getAxis() == Direction.Axis.X ? side.getStepX() * 113 : cross);
                    int worldZ = origin.getZ() + (side.getAxis() == Direction.Axis.Z ? side.getStepZ() * 113 : cross);
                    var nativeColumn = generator.getBaseColumn(worldX, worldZ, level, random);
                    for (int layer = 0; layer < 4; layer++) {
                        if (!approach.isOpen(side, layer)) continue;
                        int nativeFloor = approach.edgeFloor(side, layer, cross);
                        helper.assertTrue(!nativeColumn.getBlock(nativeFloor).isAir()
                                        && nativeColumn.getBlock(nativeFloor).getFluidState().isEmpty()
                                        && nativeColumn.getBlock(nativeFloor + 1).isAir()
                                        && nativeColumn.getBlock(nativeFloor + 2).isAir()
                                        && nativeColumn.getBlock(nativeFloor + 3).isAir(),
                                "An accepted mouth needs dry support and three clear native blocks beyond the write boundary");
                        int previous = approach.floorAt(side, layer, 96, cross);
                        for (int radius = 97; radius <= 112; radius++) {
                            int floor = approach.floorAt(side, layer, radius, cross);
                            helper.assertTrue(Math.abs(floor - previous) <= 1, "Every radial approach step must be walkable");
                            if (cross > -6) helper.assertTrue(Math.abs(floor - approach.floorAt(side, layer, radius, cross - 1)) <= 1,
                                    "The width of an approach must not contain a lateral cliff");
                            int x = side.getAxis() == Direction.Axis.X ? side.getStepX() * radius : cross;
                            int z = side.getAxis() == Direction.Axis.Z ? side.getStepZ() * radius : cross;
                            helper.assertTrue(approach.stateAt(x, floor, z) != null && !approach.stateAt(x, floor, z).isAir()
                                            && approach.stateAt(x, floor + 1, z).isAir()
                                            && approach.stateAt(x, floor + 2, z).isAir(),
                                    "The sampled route must actually author support and player headroom");
                            previous = floor;
                        }
                        helper.assertTrue(Math.abs(previous - nativeFloor) <= 1,
                                "The last authored column must meet the untouched outside terrain within one step");
                    }
                }
                for (int layer = 0; layer < 4; layer++) {
                    if (approach.isOpen(side, layer)) { verifiedMouths++; continue; }
                    int x = side.getStepX() * 97, z = side.getStepZ() * 97;
                    int floor = NetherCitadelLayout.floorHeights()[layer * 2];
                    var gate = approach.stateAt(x, floor + 1, z);
                    helper.assertTrue(gate != null && !gate.isAir(), "Unsafe rock-bank mouths must be closed");
                }
            }
        }
        helper.assertTrue(accepted == 3 && verifiedMouths >= 3,
                "Verify actual native terrain at three independently accepted citadel sites");
        helper.succeed();
    }
}
