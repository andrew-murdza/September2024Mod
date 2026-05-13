package net.amurdza.examplemod.mixins.othermods.unusualfishmod;

import codyhuh.unusualfishmod.common.entity.Prawn;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class PrawnInWater2 {
    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void aoemod$prawnNavigation(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        if ((Object)this instanceof Prawn prawn) {
            SemiAquaticPathNavigator navigator = new SemiAquaticPathNavigator(prawn, level) {
                public boolean isStableDestination(BlockPos pos) {
                    return this.level.getBlockState(pos).getFluidState().isEmpty();
                }
            };

            cir.setReturnValue(navigator);
        }
    }
}
