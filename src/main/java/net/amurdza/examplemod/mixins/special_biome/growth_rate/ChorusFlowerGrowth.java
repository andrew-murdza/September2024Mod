package net.amurdza.examplemod.mixins.special_biome.growth_rate;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerGrowth {
    @ModifyArg(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/ChorusFlowerBlock;placeGrownFlower(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)V",
                    ordinal = 1
            ),
            index = 2,
            require = 0
    )
    public int grow(Level world, BlockPos pos, int age){
        if(Helper.isSpecialBiome(world,pos)){
            return Helper.withChance(world, Config.PLACE_CHORUS_FLOWER_CHANCE)?age-1:age;
        }
        return age;
    }
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoe$specialBiomeGrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!Helper.isSpecialBiome(level, pos)) return;

        // Decide if you want to allow vanilla to proceed this tick:
        if (!Helper.withChance(level, Config.CHORUS_FLOWER_GROW_CHANCE)) {
            ci.cancel(); // skip vanilla tick entirely
        }

        // Otherwise let vanilla run (or you can fully replicate chorus logic here if needed)
    }


    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
            ),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/ChorusFlowerBlock;AGE:Lnet/minecraft/world/level/block/state/properties/IntegerProperty;", opcode = Opcodes.GETSTATIC)
            ),
            require = 0
    )
    private boolean aoe$soilOverride(BlockState state, Block block) {
        return aOEMod1_20_1V2$isSoil(state, block);
    }

    @ModifyExpressionValue(
            method = "canSurvive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean aoe$allowExtraSoil(boolean original, BlockState blockstate) {
        return aOEMod1_20_1V2$isSoil(blockstate, Blocks.END_STONE);
    }

    @Unique
    private boolean aOEMod1_20_1V2$isSoil(BlockState instance, Block block){
        return instance.is(block)||instance.is(Blocks.SCULK)||instance.is(Blocks.SCULK_CATALYST);
    }
}
