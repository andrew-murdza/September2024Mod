package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frog.class)
public class FrogsShedSlime {
    @Unique
    private int aoemod$slimeTime = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aoemod$initSlimeTime(CallbackInfo ci) {
        Frog frog = (Frog)(Object)this;

        if (this.aoemod$slimeTime < 0) {
            this.aoemod$slimeTime = frog.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aoemod$dropSlimeLikeEggs(CallbackInfo ci) {
        Frog frog = (Frog)(Object)this;

        if (frog.level().isClientSide
                || !frog.isAlive()
                || frog.isBaby()) {
            return;
        }

        float multiplier = MobConfig.mobAmountForItem(frog, Items.SLIME_BALL);
        int count = Helper.computeIncrements(frog.getRandom(), multiplier);

        if (count <= 0) {
            return;
        }

        this.aoemod$slimeTime -= count;

        if (this.aoemod$slimeTime <= 0) {
            frog.spawnAtLocation(Items.SLIME_BALL);
            frog.gameEvent(GameEvent.ENTITY_PLACE);
            this.aoemod$slimeTime = frog.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$saveSlimeTime(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("AOEModSlimeDropTime", this.aoemod$slimeTime);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$loadSlimeTime(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AOEModSlimeDropTime")) {
            this.aoemod$slimeTime = tag.getInt("AOEModSlimeDropTime");
        } else if (tag.contains("AOEModFeatherDropTime")) {
            this.aoemod$slimeTime = tag.getInt("AOEModFeatherDropTime");
        }
    }
}
