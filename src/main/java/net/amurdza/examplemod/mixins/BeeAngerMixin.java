package net.amurdza.examplemod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeehiveBlock.class)
public class BeeAngerMixin {
    @Redirect(method = "playerDestroy",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I"))
    public int cancelAngerBees(Enchantment compoundtag, ItemStack resourcelocation1){
        return 1;
    }
    @Redirect(method = "use",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/CampfireBlock;isSmokeyPos(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    public boolean cancelAngerBees1(Level blockstate1, BlockPos blockpos){
        return true;
    }
}
