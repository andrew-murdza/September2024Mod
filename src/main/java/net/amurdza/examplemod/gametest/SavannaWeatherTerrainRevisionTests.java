package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.SavannaHorsemanSpawns;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DesertBands;
import net.amurdza.examplemod.worldgen.SavannaBands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaWeatherTerrainRevisionTests {
    private SavannaWeatherTerrainRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void seededSavannaCurvatureMatchesDesertWithoutChangingRiverProfiles(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var savanna = server.getLevel(GameplayDimensions.SAVANNA);
        var desert = server.getLevel(GameplayDimensions.DESERT);
        var savannaSettings = ((NoiseBasedChunkGenerator) savanna.getChunkSource().getGenerator()).generatorSettings().value();
        var desertSettings = ((NoiseBasedChunkGenerator) desert.getChunkSource().getGenerator()).generatorSettings().value();
        var noises = savanna.registryAccess().lookupOrThrow(Registries.NOISE);
        var savannaKey = ResourceKey.create(Registries.NOISE, new ResourceLocation("aoemod:terrain/savanna/land"));
        var desertKey = ResourceKey.create(Registries.NOISE, new ResourceLocation("aoemod:terrain/desert/land"));
        helper.assertTrue(noises.getOrThrow(savannaKey).value().firstOctave() + 2
                        == noises.getOrThrow(desertKey).value().firstOctave(),
                "Savanna's fourfold field scale must compensate for its two-lower octave");
        double oldCurvature = 0, newCurvature = 0, desertCurvature = 0;
        for (long seed : new long[]{0, 12345, 98765, -8152788754161584091L}) {
            var savannaRandom = RandomState.create(savannaSettings, noises, seed);
            var desertRandom = RandomState.create(desertSettings, noises, seed);
            var bands = new SavannaBands(savannaRandom.router().ridges());
            DesertBands[] desertBands = {null};
            desertRandom.router().temperature().mapAll(function -> {
                if (function instanceof DesertBands found) desertBands[0] = found;
                return function;
            });
            helper.assertTrue(desertBands[0] != null, "Read the active Desert bend field");
            var noise = savannaRandom.getOrCreateNoise(savannaKey);
            for (int x = -16384; x <= 16384; x += 16) {
                helper.assertTrue(Math.abs(savannaRandom.router().ridges().compute(
                                new DensityFunction.SinglePointContext(x, 0, 0)) - noise.getValue(x * 4.0, 0, 0)) < 1e-12,
                        "The loaded Savanna field must have Desert's effective wavelength");
                double oldA = formerBend(noise, x - 8), oldB = formerBend(noise, x), oldC = formerBend(noise, x + 8);
                oldCurvature += square(oldA - 2 * oldB + oldC);
                newCurvature += square(bands.bend(x - 8) - 2 * bands.bend(x) + bands.bend(x + 8));
                desertCurvature += square(desertBands[0].bend(x - 8) - 2 * desertBands[0].bend(x) + desertBands[0].bend(x + 8));
                double center = -bands.bend(x);
                helper.assertTrue(Math.abs(bands.shoreDistance(x, center + 40)) < 1e-9
                                && Math.abs(bands.shoreDistance(x, center + 240) - 200) < 1e-9,
                        "Reduced bends must retain the revised 80-block river and 400-block dry divide");
            }
        }
        double reduction = Math.sqrt(newCurvature / oldCurvature);
        double desertRatio = Math.sqrt(newCurvature / desertCurvature);
        helper.assertTrue(reduction < .4, "The actual seeded Savanna contours must bend substantially less: " + reduction);
        helper.assertTrue(desertRatio > .65 && desertRatio < 1.45,
                "Savanna and Desert must have comparable measured curvature across seeds: " + desertRatio);
        AOEMod.LOGGER.info("Savanna curvature: updated/former RMS={}, updated/Desert RMS={}", reduction, desertRatio);
        helper.succeed();
    }

    private static double formerBend(net.minecraft.world.level.levelgen.synth.NormalNoise noise, int x) {
        return 80 * Math.tanh(noise.getValue(x * 4 * 4.7, 0, 0))
                + 40 * Math.tanh(noise.getValue(x * 4 * 4.7, 0, 8192 * 4.7));
    }

    private static double square(double value) { return value * value; }

    @GameTest(template = "plant_gameplay_empty")
    public static void horsemanThunderFollowsOverworldStormsAndClearCommandsImmediately(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var owner = level.getServer().overworld();
        var data = (ServerLevelData) owner.getLevelData();
        int oldClear = data.getClearWeatherTime(), oldRain = data.getRainTime(), oldThunder = data.getThunderTime();
        boolean oldRaining = data.isRaining(), oldThundering = data.isThundering();
        float oldRainLevel = owner.getRainLevel(1);
        owner.setRainLevel(1);
        float oldThunderLevel = owner.getThunderLevel(1);
        owner.setRainLevel(oldRainLevel);
        long oldTime = owner.getDayTime();
        var probe = new LightningProbe();
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            for (long time : new long[]{6000, 18000}) {
                owner.setDayTime(time);
                weather(helper, level, "clear");
                owner.setRainLevel(0);
                owner.setThunderLevel(0);
                horseman(level, MobSpawnType.NATURAL, false);
                helper.assertTrue(probe.count == 0, "Clear days and nights must never add horseman thunder");
            }
            weather(helper, level, "rain");
            owner.setRainLevel(1);
            owner.setThunderLevel(0);
            horseman(level, MobSpawnType.NATURAL, false);
            helper.assertTrue(probe.count == 0, "Ordinary rain must not sound like a thunderstorm");
            weather(helper, level, "thunder");
            owner.setThunderLevel(1);
            horseman(level, MobSpawnType.NATURAL, false);
            helper.assertTrue(probe.count == 1, "Natural horsemen retain their effect during actual Overworld thunder");
            horseman(level, MobSpawnType.COMMAND, false);
            horseman(level, MobSpawnType.NATURAL, true);
            helper.assertTrue(probe.count == 1, "Commands and disk reloads must not create extra lightning");
            weather(helper, level, "clear");
            helper.assertTrue(owner.getThunderLevel(1) > .9, "Exercise clear while storm interpolation still lingers");
            horseman(level, MobSpawnType.NATURAL, false);
            helper.assertTrue(probe.count == 1, "A clear command must immediately prevent the next horseman's thunder");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(probe);
            data.setClearWeatherTime(oldClear);
            data.setRainTime(oldRain);
            data.setThunderTime(oldThunder);
            data.setRaining(oldRaining);
            data.setThundering(oldThundering);
            owner.setRainLevel(oldRainLevel);
            owner.setThunderLevel(oldThunderLevel);
            owner.setDayTime(oldTime);
        }
        helper.succeed();
    }

    private static void weather(GameTestHelper helper, ServerLevel level, String weather) {
        int result = level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "weather " + weather + " 1200t");
        helper.assertTrue(result == 1200, "The real Savanna weather command must succeed");
    }

    private static void horseman(ServerLevel level, MobSpawnType type, boolean loaded) {
        var horse = ModEntities.SKELETON_HORSEMAN.get().create(level);
        BlockPos pos = new BlockPos(0, 220, 0);
        horse.moveTo(pos, 0, 0);
        try {
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), type, null, null);
            SavannaHorsemanSpawns.join(new EntityJoinLevelEvent(horse, level, loaded));
        } finally {
            horse.getPassengers().forEach(entity -> entity.discard());
            horse.discard();
        }
    }

    public static final class LightningProbe {
        private int count;

        @SubscribeEvent
        public void join(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof LightningBolt) {
                count++;
                event.setCanceled(true);
            }
        }
    }
}
