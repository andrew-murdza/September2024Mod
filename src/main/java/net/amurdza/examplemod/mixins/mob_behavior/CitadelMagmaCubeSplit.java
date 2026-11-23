package net.amurdza.examplemod.mixins.mob_behavior;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProgress;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The authored size-two guards keep their combat size but cannot create extra untracked residents on death. */
@Mixin(Slime.class)
public abstract class CitadelMagmaCubeSplit {
    @ModifyExpressionValue(method = "remove", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Slime;getSize()I"))
    private int aoemod$skipAuthoredCitadelSplit(int size) {
        if ((Object) this instanceof MagmaCube cube && !cube.level().isClientSide
                && AuthoredEncounterProgress.isDefender(cube)
                && NetherCitadelProgress.KIND.equals(AuthoredEncounterProgress.kind(cube))) return 1;
        return size;
    }
}
