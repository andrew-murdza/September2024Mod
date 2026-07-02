package net.amurdza.examplemod.mixins.trees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;

@Mixin(ForkingTrunkPlacer.class)
public abstract class ConnectedForkingTrunks extends TrunkPlacer {
    public ConnectedForkingTrunks(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    /**
     * Adds a same-height connector log before every horizontal step so acacia
     * forks connect face-to-face instead of only diagonally touching.
     *
     * @author amurdza
     * @reason Acacia branches should not rely on diagonal-only log contact.
     */
    @Overwrite
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
            LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> blockSetter,
            RandomSource random,
            int freeTreeHeight,
            BlockPos pos,
            TreeConfiguration config
    ) {
        setDirtAt(level, blockSetter, random, pos.below(), config);
        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int bendStart = freeTreeHeight - random.nextInt(4) - 1;
        int bendSteps = 3 - random.nextInt(3);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int x = pos.getX();
        int z = pos.getZ();
        OptionalInt lastLogY = OptionalInt.empty();

        for (int yOffset = 0; yOffset < freeTreeHeight; ++yOffset) {
            int y = pos.getY() + yOffset;
            int previousX = x;
            int previousZ = z;
            boolean stepped = false;

            if (yOffset >= bendStart && bendSteps > 0) {
                x += direction.getStepX();
                z += direction.getStepZ();
                --bendSteps;
                stepped = true;
            }

            if (stepped) {
                this.placeLog(level, blockSetter, random, mutable.set(previousX, y, previousZ), config);
            }

            if (this.placeLog(level, blockSetter, random, mutable.set(x, y, z), config)) {
                lastLogY = OptionalInt.of(y + 1);
            }
        }

        if (lastLogY.isPresent()) {
            attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(x, lastLogY.getAsInt(), z), 1, false));
        }

        x = pos.getX();
        z = pos.getZ();
        Direction branchDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        if (branchDirection != direction) {
            int branchStart = bendStart - random.nextInt(2) - 1;
            int branchSteps = 1 + random.nextInt(3);
            lastLogY = OptionalInt.empty();

            for (int yOffset = branchStart; yOffset < freeTreeHeight && branchSteps > 0; --branchSteps) {
                if (yOffset >= 1) {
                    int y = pos.getY() + yOffset;
                    int previousX = x;
                    int previousZ = z;
                    x += branchDirection.getStepX();
                    z += branchDirection.getStepZ();

                    this.placeLog(level, blockSetter, random, mutable.set(previousX, y, previousZ), config);

                    if (this.placeLog(level, blockSetter, random, mutable.set(x, y, z), config)) {
                        lastLogY = OptionalInt.of(y + 1);
                    }
                }

                ++yOffset;
            }

            if (lastLogY.isPresent()) {
                attachments.add(new FoliagePlacer.FoliageAttachment(new BlockPos(x, lastLogY.getAsInt(), z), 0, false));
            }
        }

        return attachments;
    }
}
