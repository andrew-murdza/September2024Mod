package net.amurdza.examplemod.mixins.othermods.twilightforest;

//import net.amurdza.examplemod.Config;
//import net.minecraft.tags.BlockTags;
//import net.minecraft.world.item.context.UseOnContext;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.state.BlockState;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//import twilightforest.init.TFBlocks;
//import twilightforest.item.MagicBeansItem;
//
//@Mixin(MagicBeansItem.class)
//public class BeanstalkOnDirtAndOnlyOverworld {
//    @Redirect(method = "useOn",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
//    private boolean hi(BlockState instance, Block block, UseOnContext context){
//        Level level=context.getLevel();
//        boolean isOverworld=level.dimensionType().MAX_Y>=Config.MAX_BEANSTALK_Y;
//        return instance.is(TFBlocks.UBEROUS_SOIL.get())||instance.is(BlockTags.DIRT)&&isOverworld;
//    }
//
//}
