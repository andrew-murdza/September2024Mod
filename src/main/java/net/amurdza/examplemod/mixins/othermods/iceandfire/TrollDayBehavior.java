package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntityTroll;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityTroll.class,remap = false)
public abstract class TrollDayBehavior extends Monster {
    protected TrollDayBehavior(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * Prevent troll from turning to stone.
     */
    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/iceandfire/entity/EntityStoneStatue;buildStatueEntity(Lnet/minecraft/world/entity/LivingEntity;)Lcom/github/alexthe666/iceandfire/entity/EntityStoneStatue;"
            ),
            cancellable = true
    )
    private void aoemod$preventStone(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Make troll passive during daytime like spiders.
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void aoemod$clearTargetInDay(CallbackInfo ci) {
        EntityTroll troll = (EntityTroll)(Object)this;

        if (troll.level().isDay()) {
            float brightness = troll.level().getBrightness(
                    LightLayer.SKY,
                    troll.blockPosition()
            );

            if (brightness > 0.5F &&
                    troll.level().canSeeSky(troll.blockPosition())) {
                this.setTarget(null);
            }
        }
    }
}
