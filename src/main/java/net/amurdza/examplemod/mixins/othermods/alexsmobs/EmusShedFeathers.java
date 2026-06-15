package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityEmu.class,remap = false)
public class EmusShedFeathers {
    @Unique
    private int aoemod$featherTime = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aoemod$initFeatherTime(CallbackInfo ci) {
        EntityEmu emu = (EntityEmu)(Object)this;

        if (this.aoemod$featherTime < 0) {
            this.aoemod$featherTime = emu.getRandom().nextInt(12000) + 12000;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aoemod$dropFeathersLikeEggs(CallbackInfo ci) {
        EntityEmu emu = (EntityEmu)(Object)this;

        if (emu.level().isClientSide
                || !emu.isAlive()
                || emu.isBaby()) {
            return;
        }
        float multiplier = MobConfig.mobGrowthChance(emu);
        int count = Helper.computeIncrements(emu.getRandom(),multiplier);
        this.aoemod$featherTime -= count;

        if (this.aoemod$featherTime <= 0) {
            if(count >0){
                emu.spawnAtLocation(Items.FEATHER);
                emu.gameEvent(GameEvent.ENTITY_PLACE);
                this.aoemod$featherTime = emu.getRandom().nextInt(12000) + 12000;
            }

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
