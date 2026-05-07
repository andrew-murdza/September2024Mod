package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.Config;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

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

        if (--this.aoemod$featherTime <= 0) {
            float multiplier = aoemod$getFeatherMultiplier(chicken);
            int attempts = aoemod$getAttempts(multiplier, chicken.getRandom());

            for (int i = 0; i < attempts; i++) {
                chicken.spawnAtLocation(Items.FEATHER);
            }

            if (attempts > 0) {
                chicken.gameEvent(GameEvent.ENTITY_PLACE);
            }

            this.aoemod$featherTime = chicken.getRandom().nextInt(6000) + 6000;
        }
    }

    @Unique
    private float aoemod$getFeatherMultiplier(Chicken chicken) {
        Holder<Biome> biome = chicken.level().getBiome(chicken.blockPosition());

        float multiplier = 1.0F;

        for (Map.Entry<TagKey<Biome>, Float> entry : Config.FEATHER_RATES.entrySet()) {
            if (biome.is(entry.getKey())) {
                multiplier *= entry.getValue();
            }
        }

        return multiplier;
    }

    @Unique
    private int aoemod$getAttempts(float multiplier, RandomSource random) {
        if (multiplier <= 0.0F) {
            return 0;
        }

        int attempts = (int)Math.floor(multiplier);
        float remainder = multiplier - attempts;

        if (random.nextFloat() < remainder) {
            attempts++;
        }

        return attempts;
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