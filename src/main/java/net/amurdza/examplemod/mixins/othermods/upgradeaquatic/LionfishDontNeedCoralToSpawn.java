package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.entity.animal.Lionfish;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Lionfish.class)
public class LionfishDontNeedCoralToSpawn {
    @Redirect(method = "checkLionfishSpawnRules",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean hi(BlockState instance, TagKey tagKey){
        return true;
    }
}
