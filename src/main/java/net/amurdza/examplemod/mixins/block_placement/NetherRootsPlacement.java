package net.amurdza.examplemod.mixins.block_placement;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RootsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RootsBlock.class)
public abstract class NetherRootsPlacement extends BushBlock {

    protected NetherRootsPlacement(Properties properties) {
        super(properties);
    }

    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    protected void mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        if (this == Blocks.WARPED_ROOTS) {
            cir.setReturnValue(pState.is(Blocks.WARPED_NYLIUM)
                    &&((LevelReader)pLevel).getBiome(pPos.above()).is(ModTags.Biomes.warpedForestBiomes));
        }

        if (this == Blocks.CRIMSON_ROOTS) {
            Holder<Biome> biome = ((LevelReader)pLevel).getBiome(pPos.above());
            cir.setReturnValue((pState.is(Blocks.CRIMSON_NYLIUM)
                    || pState.is(ModTags.Blocks.soulSediments)
                    || pState.is(ModTags.Blocks.basaltStones))
                    &&(biome.is(ModTags.Biomes.crimsonForestBiomes)||
                    biome.is(ModTags.Biomes.soulSandValleyBiomes)||biome.is(ModTags.Biomes.basaltDeltasBiomes)));
        }
    }
}