package net.amurdza.examplemod.mixins.special_biome.safety;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Animal.class)
public class AnimalsDontRetaliateInSpecialBiomePartII extends AnimalsDontRetaliateInSpecialBiomePartI{
    @Shadow private int inLove;

    @Override
    protected boolean hi(DamageSource instance, TagKey<DamageType> pDamageTypeKey){
        return super.hi(instance,pDamageTypeKey)&&
                !(instance.getEntity() instanceof Player player&& Helper.isSpecialBiome(player));
    }
}
