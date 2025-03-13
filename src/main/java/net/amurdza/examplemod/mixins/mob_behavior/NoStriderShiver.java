package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Strider.class)
public class NoStriderShiver {
    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hi(BlockState instance, TagKey tagKey){
        return true;
    }
}
