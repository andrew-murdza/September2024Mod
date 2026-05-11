package net.amurdza.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class DensityRangeCommand {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aoe_density_ranges")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> run(ctx.getSource(), 2_000_000, -64, 320, 30000))
        );
    }

    private static int run(CommandSourceStack source, int samples, int minY, int maxY, int radius) {
        Registry<DensityFunction> registry =
            source.getServer().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);

        DensityFunction h = registry.getOrThrow(
                ResourceKey.create(
                        Registries.DENSITY_FUNCTION,
                        new ResourceLocation("aoemod", "h_jagged_half_negative")
                )
        );
        DensityFunction n = registry.getOrThrow(
                ResourceKey.create(
                        Registries.DENSITY_FUNCTION,
                        new ResourceLocation("minecraft", "overworld/base_3d_noise")
                )
        );

        RandomSource random = RandomSource.create(123456789L);

        Stats hStats = new Stats();
        Stats nStats = new Stats();

        for (int i = 0; i < samples; i++) {
            int x = random.nextInt(radius * 2 + 1) - radius;
            int y = minY + random.nextInt(maxY - minY + 1);
            int z = random.nextInt(radius * 2 + 1) - radius;

            DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, y, z);

            hStats.accept(h.compute(context), x, y, z);
            nStats.accept(n.compute(context), x, y, z);
        }

        source.sendSuccess(() -> Component.literal(
            "Sampled " + samples + " points\n" +
            "H = half_negative(jagged): " + hStats + "\n" +
            "N = base_3d_noise: " + nStats
        ), false);

        return 1;
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