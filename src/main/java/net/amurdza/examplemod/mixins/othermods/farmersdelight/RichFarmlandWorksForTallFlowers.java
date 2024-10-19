package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.block.RichSoilBlock;
import vectorwing.farmersdelight.common.utility.MathUtils;

@Mixin(RichSoilBlock.class)
public class RichFarmlandWorksForTallFlowers {
    @Redirect(method = "randomTick",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(BlockState instance, TagKey tagKey, BlockState state, ServerLevel level, BlockPos pos, RandomSource rand){
        if(instance.getBlock() instanceof TallFlowerBlock growable){
            if ((double) MathUtils.RAND.nextFloat() <= Configuration.RICH_SOIL_BOOST_CHANCE.get() && growable.isValidBonemealTarget(level, pos.above(), instance, false)
                    && ForgeHooks.onCropsGrowPre(level, pos.above(), instance, true)) {
                growable.performBonemeal(level, level.random, pos.above(), instance);
                level.levelEvent(2005, pos.above(), 0);
                ForgeHooks.onCropsGrowPost(level, pos.above(), instance);
            }
        }
        if(instance.is(ModTags.Blocks.duplicatedByBonemeal)&&!(instance.getBlock() instanceof BonemealableBlock)
                &&(double) MathUtils.RAND.nextFloat() <= Configuration.RICH_SOIL_BOOST_CHANCE.get()
                &&ForgeHooks.onCropsGrowPre(level, pos.above(), instance, true)){
            Block.popResource(level,pos.above(),new ItemStack(instance.getBlock()));
            level.levelEvent(2005, pos.above(), 0);
        }
        return instance.is(tagKey);
    }
}
