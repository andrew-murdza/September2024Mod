package net.amurdza.examplemod.mixins.growth_rate;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class MyceliumGrassSpreadRate extends Block  {
    public MyceliumGrassSpreadRate(Properties pProperties) {
        super(pProperties);
    }

    @ModifyConstant(method = "randomTick", constant = @Constant(intValue = 4))
    public int randomTick1(int constant, BlockState state, ServerLevel world, BlockPos pos, RandomSource random){
        return 4;
    }

    @Inject(method = "canBeGrass", at=@At(value = "RETURN"),cancellable = true)
    private static void randomTick2(BlockState pState, LevelReader pLevelReader, BlockPos pPos,
                                    CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue()){
            Holder<Biome> biome = pLevelReader.getBiome(pPos);
            if(biome.is(ModTags.Biomes.mushroomCaves)||biome.is(ModTags.Biomes.desertBiomes)
                    ||biome.is(ModTags.Biomes.deepDarkBiomes)||biome.is(ModTags.Biomes.netherBiomes)){
                cir.setReturnValue(false);
            }
        }
    }
}
