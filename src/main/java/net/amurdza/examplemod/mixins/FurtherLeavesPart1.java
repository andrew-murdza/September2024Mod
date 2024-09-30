package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Config;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockStateProperties.class)
public class FurtherLeavesPart1 {
    @ModifyConstant(method = "<clinit>",constant = @Constant(intValue = 7,ordinal = 1))
    private static int hi(int i){
        return Config.MAX_LEAVES_DISTANCE;
    }
}
