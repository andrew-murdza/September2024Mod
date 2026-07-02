package net.amurdza.examplemod.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.function.Supplier;

public class StrippableRotatedPillarBlock extends RotatedPillarBlock {
    private final Supplier<? extends RotatedPillarBlock> stripped;

    public StrippableRotatedPillarBlock(Properties properties, Supplier<? extends RotatedPillarBlock> stripped) {
        super(properties);
        this.stripped = stripped;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
        if (toolAction == ToolActions.AXE_STRIP) {
            Direction.Axis axis = state.getValue(AXIS);
            return stripped.get().defaultBlockState().setValue(AXIS, axis);
        }
        return super.getToolModifiedState(state, context, toolAction, simulate);
    }
}
