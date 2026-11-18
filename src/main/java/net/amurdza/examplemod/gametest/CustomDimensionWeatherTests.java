package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CustomDimensionWeatherTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void weatherCommandsFromAllSixDimensionsUpdateTheirSharedWeather(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerLevel owner = server.overworld();
        ServerLevelData data = (ServerLevelData) owner.getLevelData();
        int oldClear = data.getClearWeatherTime(), oldRain = data.getRainTime(), oldThunder = data.getThunderTime();
        boolean oldRaining = data.isRaining(), oldThundering = data.isThundering();
        try {
            for (var dimension : GameplayDimensions.ALL) {
                ServerLevel level = server.getLevel(dimension);
                helper.assertTrue(level != null, "Every gameplay dimension must be present for weather-command validation");
                command(helper, level, "thunder", 417);
                assertWeather(helper, level, 0, 417, true, true);
                command(helper, level, "rain", 293);
                assertWeather(helper, level, 0, 293, true, false);
                command(helper, level, "clear", 631);
                assertWeather(helper, level, 631, 0, false, false);
            }
            command(helper, owner, "thunder", 823);
            assertWeather(helper, owner, 0, 823, true, true);
            command(helper, owner, "clear", 197);
            assertWeather(helper, owner, 197, 0, false, false);
        } finally {
            data.setClearWeatherTime(oldClear);
            data.setRainTime(oldRain);
            data.setThunderTime(oldThunder);
            data.setRaining(oldRaining);
            data.setThundering(oldThundering);
        }
        helper.succeed();
    }

    private static void command(GameTestHelper helper, ServerLevel level, String weather, int ticks) {
        var server = level.getServer();
        int result = server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "weather " + weather + " " + ticks + "t");
        helper.assertTrue(result == ticks, "Actual weather command failed in " + level.dimension().location());
    }

    private static void assertWeather(GameTestHelper helper, ServerLevel level, int clear, int wet,
                                       boolean rain, boolean thunder) {
        for (ServerLevel checked : new ServerLevel[]{level, level.getServer().overworld()}) {
            var data = (ServerLevelData) checked.getLevelData();
            helper.assertTrue(data.getClearWeatherTime() == clear && data.getRainTime() == wet
                            && data.getThunderTime() == wet && data.isRaining() == rain && data.isThundering() == thunder,
                    "Weather flags and exact command duration must reach the authoritative and derived data: "
                            + checked.dimension().location());
        }
    }
}
