package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PotionBrewing.class)
public interface PotionBrewingInvoker {
    @Invoker("addMix")
    static void aoe$addMix(Potion input, Item ingredient, Potion output) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
