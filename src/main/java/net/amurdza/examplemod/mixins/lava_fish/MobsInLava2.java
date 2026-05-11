package net.amurdza.examplemod.mixins.lava_fish;

import com.scouter.netherdepthsupgrade.entity.NDUMobType;
import net.amurdza.examplemod.lava_fish.LavaMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterAnimal.class)
public abstract class MobsInLava2 {
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
                    level.getFluidState(pos).is(FluidTags.LAVA)
                            && level.getFluidState(pos.below()).is(FluidTags.LAVA)
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

    @Redirect(method = "handleAirSupply", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/WaterAnimal;isInWaterOrBubble()Z"))
    boolean hi(WaterAnimal instance){
        return LavaMobs.isLavaMob(instance)&&instance.isInLava()||!LavaMobs.isLavaMob(instance)&&instance.isInWaterOrBubble();
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

            // Optional: make water bad for converted lava mobs
            self.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
        }
    }
}
