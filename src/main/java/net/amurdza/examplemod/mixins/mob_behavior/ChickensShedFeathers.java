package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public class ChickensShedFeathers {
    @Unique
    private int aoemod$featherTime = -1;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void aoemod$initFeatherTime(CallbackInfo ci) {
        Chicken chicken = (Chicken)(Object)this;

        if (this.aoemod$featherTime < 0) {
            this.aoemod$featherTime = chicken.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aoemod$dropFeathersLikeEggs(CallbackInfo ci) {
        Chicken chicken = (Chicken)(Object)this;

        if (chicken.level().isClientSide
                || !chicken.isAlive()
                || chicken.isBaby()
                || chicken.isChickenJockey()) {
            return;
        }
        float multiplier = MobConfig.mobGrowthChance(chicken);
        int count = Helper.computeIncrements(chicken.getRandom(),multiplier);
        this.aoemod$featherTime =- count;

        if (this.aoemod$featherTime <= 0) {
            chicken.spawnAtLocation(Items.FEATHER);
            chicken.gameEvent(GameEvent.ENTITY_PLACE);
            this.aoemod$featherTime = chicken.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$saveFeatherTime(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("AOEModFeatherDropTime", this.aoemod$featherTime);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$loadFeatherTime(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AOEModFeatherDropTime")) {
            this.aoemod$featherTime = tag.getInt("AOEModFeatherDropTime");
        }
    }
}