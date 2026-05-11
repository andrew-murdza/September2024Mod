package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.entities.passive.fish.CharscaleMokiEntity;
import com.legacy.blue_skies.entities.util.base.SkySchoolingFishEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CharscaleMokiEntity.class)
public abstract class CharscaleMokiInLava extends SkySchoolingFishEntity {
    public CharscaleMokiInLava(EntityType<? extends SkySchoolingFishEntity> type, Level worldIn) {
        super(type, worldIn);
    }
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void aoemod$lavaPathing(EntityType<?> type, Level level, CallbackInfo ci) {
        this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
    }
}
