package net.amurdza.examplemod.mixins.commands;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherCommand.class)
public final class CustomDimensionWeatherCommand {
    @Redirect(method = {"setClear", "setRain", "setThunder"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setWeatherParameters(IIZZ)V"))
    private static void aoemod$writeSharedWeather(ServerLevel level, int clearTicks, int weatherTicks,
                                                  boolean raining, boolean thundering) {
        // Our dimensions read Overworld weather through DerivedLevelData, whose
        // weather setters intentionally do nothing. Write to that shared owner.
        ServerLevel owner = GameplayDimensions.isCustom(level) ? level.getServer().overworld() : level;
        owner.setWeatherParameters(clearTicks, weatherTicks, raining, thundering);
    }
}
