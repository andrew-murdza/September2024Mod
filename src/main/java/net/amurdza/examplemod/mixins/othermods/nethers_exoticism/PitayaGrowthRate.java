package net.amurdza.examplemod.mixins.othermods.nethers_exoticism;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.mcreator.nethersexoticism.procedures.PitayaStemDownUpdateTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PitayaStemDownUpdateTickProcedure.class)
public class PitayaGrowthRate {
    @Redirect(method = "execute",at= @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),remap = false)
    private static double hi(final LevelAccessor world, double x, double y, double z){
        return Helper.isSpecialBiome(world,new BlockPos((int) x, (int) y, (int) z))?Helper.withChanceToInt(world,Config.PITAYA_GROWTH_CHANCE):Math.random();
    }
}
