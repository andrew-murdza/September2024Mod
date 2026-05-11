package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.entities.passive.fish.HorizofinTunidEntity;
import com.legacy.blue_skies.entities.util.base.SkySchoolingFishEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorizofinTunidEntity.class)
public abstract class HorizofinTunidInLava extends SkySchoolingFishEntity {
    protected HorizofinTunidInLava(EntityType<? extends SkySchoolingFishEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void aoemod$lavaPathing(EntityType<?> type, Level level, CallbackInfo ci) {
        this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
    }
}
