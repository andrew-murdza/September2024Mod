package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntityDreadLich;
import com.github.alexthe666.iceandfire.entity.EntityDreadThrall;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes ordinary Dread Liches summon only Dread Thralls. */
@Mixin(value = EntityDreadLich.class, remap = false)
public abstract class DreadLichThrallSummons {
    @Redirect(
            method = "performRangedAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/iceandfire/entity/EntityDreadLich;getRandomNewMinion()Lnet/minecraft/world/entity/Mob;"
            ),
            remap = false
    )
    private Mob aoemod$onlySummonDreadThralls(EntityDreadLich lich) {
        return new EntityDreadThrall(IafEntityRegistry.DREAD_THRALL.get(), lich.level());
    }

    @Inject(method = "getRandomNewMinion", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$forceRandomNewMinionToDreadThrall(CallbackInfoReturnable<Mob> cir) {
        EntityDreadLich lich = (EntityDreadLich) (Object) this;
        cir.setReturnValue(new EntityDreadThrall(IafEntityRegistry.DREAD_THRALL.get(), lich.level()));
    }
}
