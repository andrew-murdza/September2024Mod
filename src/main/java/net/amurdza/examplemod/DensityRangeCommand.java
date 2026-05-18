package net.amurdza.examplemod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class DensityRangeCommand {
    private static final ResourceKey<NormalNoise.NoiseParameters> JAGGED_NOISE =
            ResourceKey.create(Registries.NOISE, new ResourceLocation("minecraft", "jagged"));

    private static final ResourceKey<DensityFunction> BASE_3D_NOISE =
            ResourceKey.create(Registries.DENSITY_FUNCTION, new ResourceLocation("minecraft", "overworld/base_3d_noise"));

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aoe_density_ranges")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> run(ctx.getSource(), 2_000_000, -64, 320, 30_000))
        );
    }

    private static int run(CommandSourceStack source, int samples, int minY, int maxY, int radius) {
        ServerLevel level = source.getLevel();
        RandomState randomState = level.getChunkSource().randomState();

        NormalNoise jagged = randomState.getOrCreateNoise(JAGGED_NOISE);

        Registry<DensityFunction> registry =
                source.getServer().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);

        DensityFunction base3dNoise = registry.getOrThrow(BASE_3D_NOISE);

        RandomSource random = RandomSource.create(123456789L);

        Stats hStats = new Stats();
        Stats nStats = new Stats();

        for (int i = 0; i < samples; i++) {
            int x = random.nextInt(radius * 2 + 1) - radius;
            int y = minY + random.nextInt(maxY - minY + 1);
            int z = random.nextInt(radius * 2 + 1) - radius;

            double rawJagged = jagged.getValue(x / 1500.0, 0.0, z / 1500.0);
            double h = halfNegative(rawJagged);

            DensityFunction.FunctionContext context =
                    new DensityFunction.SinglePointContext(x, y, z);

            double n = base3dNoise.compute(context);

            hStats.accept(h, x, y, z);
            nStats.accept(n, x, y, z);
        }

        source.sendSuccess(() -> Component.literal(
                "Sampled " + samples + " points\n" +
                        "H = half_negative(jagged): " + hStats + "\n" +
                        "N = base_3d_noise: " + nStats
        ), false);

        return 1;
    }

    private static double halfNegative(double value) {
        return value < 0.0 ? value / 2.0 : value;
    }

    private static class Stats {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        double sumSq = 0.0;
        int count = 0;

        int minX, minY, minZ;
        int maxX, maxY, maxZ;

        void accept(double v, int x, int y, int z) {
            if (v < min) {
                min = v;
                minX = x;
                minY = y;
                minZ = z;
            }

            if (v > max) {
                max = v;
                maxX = x;
                maxY = y;
                maxZ = z;
            }

            sum += v;
            sumSq += v * v;
            count++;
        }

        @Override
        public String toString() {
            double mean = sum / count;
            double variance = sumSq / count - mean * mean;
            double std = Math.sqrt(Math.max(0.0, variance));

            return String.format(
                    "min=%.8f at (%d,%d,%d), max=%.8f at (%d,%d,%d), mean=%.8f, std=%.8f",
                    min, minX, minY, minZ,
                    max, maxX, maxY, maxZ,
                    mean, std
            );
        }
    }
}