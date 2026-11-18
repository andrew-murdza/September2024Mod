package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.block.PickerelweedBlock;
import com.teamabnormals.upgrade_aquatic.common.block.PickerelweedDoublePlantBlock;
import com.teamabnormals.upgrade_aquatic.common.block.PickerelweedPlantBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PickerelweedPlantBlock.class, PickerelweedDoublePlantBlock.class, PickerelweedBlock.class})
public abstract class PickerelweedDoesNotSlow {
    @Redirect(method = "entityInside", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"), require = 1)
    private void aoemod$preserveMovementSpeed(Entity entity, BlockState state, Vec3 multiplier) {
        // Pickerelweed keeps its appearance, waterlogging and harvesting behavior.
    }
}
