package net.amurdza.examplemod.mixins.mob_behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(targets = "net.minecraft.world.entity.monster.Guardian$GuardianAttackSelector")
public class GuardiansDontAttackSquid {
    @Shadow
    @Final
    private Guardian guardian;

    /**
     * @author amurdza
     * @reason easiest way to do what I want
     */
    @Overwrite
    public boolean test(@Nullable LivingEntity pEntity) {
        return (pEntity instanceof Player || pEntity instanceof Axolotl)
                && pEntity.distanceToSqr(this.guardian) > 9.0D;
    }
}
