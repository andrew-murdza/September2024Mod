package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.github.alexthe666.alexsmobs.entity.EntityRoadrunner;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityRoadrunner.class,remap = false)
public class RoadRunnerShedRate {

    @Shadow public int timeUntilNextFeather;

    @Redirect(method = "aiStep",at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityRoadrunner;isAlive()Z"))
    private boolean hi(EntityRoadrunner instance){
        if (!instance.isAlive()) {
            return false;
        }

        float multiplier = MobConfig.mobGrowthChance(instance);
        int ticks = Helper.computeIncrements(instance.getRandom(),multiplier);

        if (ticks > 0) {
            this.timeUntilNextFeather -= ticks;
        }

        return false;
    }
}
