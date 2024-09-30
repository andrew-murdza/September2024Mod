package net.amurdza.examplemod.mixins;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(BoneMealItem.class)
public class BoneMealMixin {
    @Redirect(method = "growWaterPlant",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean useOnGround(Holder<Biome> instance, TagKey<Biome> key, ItemStack stack, Level world, BlockPos blockPos, @Nullable Direction facing){
        return instance.is(key)|| Helper.isSpecialBiome(world,blockPos);
    }
    @Redirect(method = "growWaterPlant",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",ordinal = 5))
    private static int useOnGround1(RandomSource random, int n, ItemStack stack, Level world, BlockPos blockPos, @Nullable Direction facing){
        return random.nextInt(Helper.isSpecialBiome(world,blockPos)? Config.CHANCE_OF_TALL_SEAGRASS_BONEMEAL :n);
    }

    //Sponges in mountains
//    @Redirect(method = "growWaterPlant",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
//    private static boolean useOnGround2(Level world, BlockPos pos, BlockState state, int flags){
//        if(BiomeScores.isMountains(world,pos)&&world.random.nextInt(20)==0){
//            state=Blocks.WET_SPONGE.defaultBlockState();
//        }
//        return world.setBlockAndUpdate(pos,state);
//    }
}
