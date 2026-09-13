package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import samebutdifferent.ecologics.registry.ModBlocks;

@EventBusSubscriber(modid = AOEMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class BlockGrowthRate {

    private static final IntegerProperty[] AGE_PROPERTIES = new IntegerProperty[]{
            BlockStateProperties.AGE_1,
            BlockStateProperties.AGE_2,
            BlockStateProperties.AGE_3,
            BlockStateProperties.AGE_4,
            BlockStateProperties.AGE_5,
            BlockStateProperties.AGE_7,
            BlockStateProperties.AGE_15,
            BlockStateProperties.AGE_25
    };

    private static final int[] MAX_AGES = new int[]{1, 2, 3, 4, 5, 7, 15, 25};

    @SubscribeEvent
    public static void blockGrowthPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (block == Blocks.AIR) {
            BlockState belowState = level.getBlockState(pos.below());

            if (belowState.is(Blocks.CAVE_VINES)) {
                state = belowState;
                block = Blocks.CAVE_VINES;
                pos = pos.below();
            } else if (belowState.is(Blocks.CAVE_VINES_PLANT)) {
                state = belowState;
                block = Blocks.CAVE_VINES_PLANT;
                pos = pos.below();
            }
        }

        float mult = BlockConfig.blockGrowthChance(level, pos, state);

        /*
         * Untracked blocks keep vanilla/modded behavior. Tracked blocks with
         * no matching biome value are denied below.
         */
        if (!BlockConfig.hasGrowthConfig(block)) {
            return;
        }

        if (block instanceof CropBlock || block instanceof StemBlock) {
            BlockPos below = pos.below();

            if (!level.getBlockState(below).isFertile(level, below)) {
                mult *= 0.5F;
            }
        }

        if (mult <= 0.0F) {
            event.setResult(Event.Result.DENY);
            return;
        }

        int increments = Helper.computeIncrements(level.random, mult);

        if (increments <= 0) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (increments == 1) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        if (!hasAnyAgeProperty(state)) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        event.setResult(Event.Result.DENY);

        applyAgeIncrements(level, pos, state, increments);
        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
    }

    private static boolean hasAnyAgeProperty(BlockState state) {
        for (IntegerProperty property : AGE_PROPERTIES) {
            if (state.hasProperty(property)) {
                return true;
            }
        }

        return false;
    }

    private static void applyAgeIncrements(ServerLevel level, BlockPos pos, BlockState state, int increments) {
        for (int i = 0; i < AGE_PROPERTIES.length; i++) {
            IntegerProperty prop = AGE_PROPERTIES[i];

            if (!state.hasProperty(prop)) {
                continue;
            }

            int max = MAX_AGES[i];
            int oldAge = state.getValue(prop);
            int newAge = oldAge + increments;

            boolean columnPlant = state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE);

            if (columnPlant) {
                if (state.is(Blocks.SUGAR_CANE)) {
                    pos = pos.above();
                }

                int k;

                for (k = 1; k < 5; k++) {
                    if (!level.getBlockState(pos.below(k + 1)).is(state.getBlock())) {
                        break;
                    }
                }

                if (k < 3) {
                    if (newAge > max) {
                        BlockState resetState = state.setValue(prop, 0);
                        BlockState newTopState = state.is(Blocks.SUGAR_CANE)
                                ? resetState.setValue(BlockStateProperties.WATERLOGGED, false)
                                : resetState;
                        level.setBlockAndUpdate(pos.below(), resetState);
                        level.setBlockAndUpdate(pos, newTopState);
                    } else {
                        level.setBlockAndUpdate(pos.below(), state.setValue(prop, newAge));
                    }
                } else if (state.is(Blocks.CACTUS)) {
                    level.setBlockAndUpdate(pos, ModBlocks.PRICKLY_PEAR.get().defaultBlockState());
                }
            } else {
                level.setBlock(pos, state.setValue(prop, Math.min(newAge, max)), 4);
            }

            return;
        }
    }
}
