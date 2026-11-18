package net.amurdza.examplemod.mixins.othermods.twilightforest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import twilightforest.entity.monster.UpperGoblinKnight;

@Mixin(value = UpperGoblinKnight.class, remap = false)
public abstract class GoblinKnightSingleBody extends Monster {
    protected GoblinKnightSingleBody(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 20.0D))
    private static double aoemod$moderatePermanentArmor(double original) {
        // A helmet/chestplate-scale defense, rather than a full diamond suit.
        return 8.0D;
    }

    @Override protected net.minecraft.network.chat.Component getTypeName() {
        // Keep the saved Twilight Forest ID while naming the complete mob through
        // our own namespace, independent of the source mod's language-pack order.
        return net.minecraft.network.chat.Component.translatable("entity.aoemod.goblin_knight");
    }

    @Override public EntityDimensions getDimensions(Pose pose) { return EntityDimensions.scalable(1.1F, 2.65F); }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) { return 2.25F; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aoemod$initializeCombinedBody(CallbackInfo ci) {
        // Entity's constructor caches the registered upper knight's short size;
        // it does not consult the combined model's getDimensions override.
        this.refreshDimensions();
    }

    // Requested gameplay differences from the restored bundled baseline.
    // One body has one health pool. Native damage handling is directional and can
    // block, disable the shield or strip armor; the combined knight uses ordinary damage.
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true, remap = true)
    private void aoemod$ordinaryDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(super.hurt(source, damage));
    }

    @Inject(method = "takeHitOnShield", at = @At("HEAD"), cancellable = true)
    private void aoemod$cosmeticShield(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
    // Native save loading restores equipment via these setters, including knights stripped by earlier versions.
    @ModifyVariable(method = {"setHasArmor", "setHasShield"}, at = @At("HEAD"), argsOnly = true)
    private boolean aoemod$restoreEquipment(boolean equipped) {
        return true;
    }

    @Inject(method = {"breakArmor", "damageShield", "breakShield"}, at = @At("HEAD"), cancellable = true)
    private void aoemod$keepEquipment(CallbackInfo ci) {
        ci.cancel();
    }
}
