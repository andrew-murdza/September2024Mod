package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.potionstudios.biomeswevegone.world.level.block.plants.bush.DesertPlantBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DesertPlantBlock.class)
public class NoCactusDamage {
    @Redirect(method = "entityInside",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getType()Lnet/minecraft/world/entity/EntityType;"))
    private EntityType<?> hi(Entity instance){
        return EntityType.RABBIT;
    }

}
