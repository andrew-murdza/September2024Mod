package net.amurdza.examplemod.mixins.othermods.nourishednether;

import net.mcreator.nourishednether.block.SoulWeedsBlock;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(SoulWeedsBlock.class)
public abstract class SoulWeedsUseLootTable {
    @Redirect(method = "getDrops", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", remap = false))
    private boolean aoemod$keepEmptyLootResult(List<ItemStack> drops) {
        // The dependency falls back to dropping itself when the loot table returns nothing.
        // Preserve empty seed rolls, so only shears collect the weeds themselves.
        return false;
    }
}
