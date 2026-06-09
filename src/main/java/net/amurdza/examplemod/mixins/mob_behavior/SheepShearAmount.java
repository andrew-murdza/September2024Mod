package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Sheep.class)
public abstract class SheepShearAmount extends Entity {
    public SheepShearAmount(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "shear",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"
            )
    )
    private int aoe$modifyWoolAmount(RandomSource random, int bound) {
        float multiplier = MobConfig.mobSkinAmount(this);
        int newAmount = Helper.computeIncrements(random,multiplier);
        return newAmount - 1;
    }
}