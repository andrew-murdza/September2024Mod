package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.block.BlockLeafcutterAntChamber;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLeafcutterAntChamber.class)
public class AntChambersDropPupa {
    @Redirect(method = "use",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/block/BlockLeafcutterAntChamber;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void hi(Level level, BlockPos blockPos, ItemStack itemStack){
        int count=Helper.isSpecialBiome(level,blockPos)?Config.ANT_FOOD_COUNT:1;
        Block.popResource(level,blockPos,new ItemStack(AMItemRegistry.GONGYLIDIA.get(),count));
        if(Helper.withChance(level,Config.PUPA_CHANCE)){
            Block.popResource(level,blockPos, new ItemStack(AMItemRegistry.LEAFCUTTER_ANT_PUPA.get()));
        }
    }
}
