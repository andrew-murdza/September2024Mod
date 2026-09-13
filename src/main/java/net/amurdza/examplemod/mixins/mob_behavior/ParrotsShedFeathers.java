package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Parrot.class)
public class ParrotsShedFeathers {
    @Unique
    private int aoemod$featherTime = -1;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void aoemod$initFeatherTime(CallbackInfo callback) {
        Parrot parrot = (Parrot) (Object) this;

        if (this.aoemod$featherTime < 0) {
            this.aoemod$featherTime = parrot.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aoemod$dropFeathersLikeChickens(CallbackInfo callback) {
        Parrot parrot = (Parrot) (Object) this;

        if (parrot.level().isClientSide || !parrot.isAlive() || parrot.isBaby()) {
            return;
        }

        int count = Helper.computeIncrements(parrot.getRandom(), MobConfig.mobGrowthChance(parrot));
        this.aoemod$featherTime -= count;

        if (this.aoemod$featherTime <= 0) {
            parrot.spawnAtLocation(Items.FEATHER);
            parrot.gameEvent(GameEvent.ENTITY_PLACE);
            this.aoemod$featherTime = parrot.getRandom().nextInt(6000) + 6000;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$saveFeatherTime(CompoundTag tag, CallbackInfo callback) {
        tag.putInt("AOEModFeatherDropTime", this.aoemod$featherTime);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$loadFeatherTime(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains("AOEModFeatherDropTime")) {
            this.aoemod$featherTime = tag.getInt("AOEModFeatherDropTime");
        }
    }
}
