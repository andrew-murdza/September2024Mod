package net.amurdza.examplemod.mixins;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Axolotl.class)
public class AxolotlMixin {
    @Redirect(method = "checkAxolotlSpawnRules",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean hi(BlockState instance, TagKey<Block> tagKey){
        return true;
    }
}
