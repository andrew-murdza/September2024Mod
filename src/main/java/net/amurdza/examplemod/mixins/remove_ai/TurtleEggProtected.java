package net.amurdza.examplemod.mixins.remove_ai;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TurtleEggBlock.class)
public class TurtleEggProtected {
    @Redirect(method = "canDestroyEgg",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/event/ForgeEventFactory;getMobGriefingEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean protectEgg(Level level, Entity entity){
        return !Helper.isSpecialBiome((LivingEntity) entity)&&net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,entity);
    }
}
