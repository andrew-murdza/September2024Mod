package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.AquaticMobSizes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Plant interactions shared by the three native-sized bottom dwellers. */
public final class SmallAquaticPlants {
    private SmallAquaticPlants() { }

    public static boolean affected(Entity entity) {
        return AquaticMobSizes.usesNativeSize(entity);
    }

    public static boolean blocksMovement(BlockState state) {
        return state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }
}
