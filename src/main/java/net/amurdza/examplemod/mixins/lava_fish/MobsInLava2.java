package net.amurdza.examplemod.mixins.lava_fish;

import com.scouter.netherdepthsupgrade.entity.NDUMobType;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterAnimal.class)
public abstract class MobsInLava2 extends PathfinderMob {
    protected MobsInLava2(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "checkSurfaceWaterAnimalSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void aoemod$lavaWaterAnimalSpawnRules(
            EntityType<? extends WaterAnimal> type,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LavaMobs.isLavaMob(type)) {
            cir.setReturnValue(
                    level.getFluidState(pos.below()).is(FluidTags.LAVA)
                            && level.getFluidState(pos.above()).is(Fluids.LAVA)
            );
        }
    }

    @Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
    private void hi(
            CallbackInfoReturnable<MobType> cir
    ) {
        if (LavaMobs.isLavaMob(this)) {
            cir.setReturnValue(NDUMobType.LAVA);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aoemod$lavaPathMalus(
            EntityType<? extends WaterAnimal> type,
            Level level,
            CallbackInfo ci
    ) {
        if (LavaMobs.isLavaMob(type)) {
            WaterAnimal self = (WaterAnimal) (Object) this;
            self.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
            self.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        }
    }

    @Redirect(method = "handleAirSupply",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/WaterAnimal;isInWaterOrBubble()Z"))
    private boolean hi(WaterAnimal instance){
        return LavaMobs.isLavaMob(instance)? instance.isInLava(): instance.isInWaterOrBubble();
    }
}
