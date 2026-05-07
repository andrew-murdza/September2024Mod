package net.amurdza.examplemod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.amurdza.examplemod.AOEMod.LOGGER;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class NoiseCalibrationCommand {

    private static final ResourceKey<Biome> SOUL_SAND_VALLEY =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", "soul_sand_valley"));

    private static final ResourceKey<NormalNoise.NoiseParameters> SURFACE_NOISE =
            ResourceKey.create(Registries.NOISE, new ResourceLocation("minecraft", "surface"));

    private static final ResourceKey<NormalNoise.NoiseParameters> GRAVEL_NOISE =
            ResourceKey.create(Registries.NOISE, new ResourceLocation("minecraft", "gravel"));

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aoemod_noise_calibrate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("y", IntegerArgumentType.integer(-128, 320))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(16, 4096))
                                        .then(Commands.argument("step", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> {
                                                    ServerLevel level = ctx.getSource().getLevel();
                                                    BlockPos center = BlockPos.containing(ctx.getSource().getPosition());

                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                                    int step = IntegerArgumentType.getInteger(ctx, "step");

                                                    calibrate(level, center, y, radius, step,
                                                            msg -> {
                                                                LOGGER.info(msg.getString());
                                                                ctx.getSource().sendSuccess(() -> msg, false);
                                                            }
                                                    );
                                                    return 1;
                                                }))))
        );
    }

    private static void calibrate(
            ServerLevel level,
            BlockPos center,
            int y,
            int radius,
            int step,
            MessageSender sender
    ) {
        RandomState randomState = level.getChunkSource().randomState();

        NormalNoise surfaceNoise = randomState.getOrCreateNoise(SURFACE_NOISE);
        NormalNoise gravelNoise = randomState.getOrCreateNoise(GRAVEL_NOISE);

        List<Double> surfaceValues = new ArrayList<>();
        List<Sample> samples = new ArrayList<>();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                pos.set(x, y, z);

                if (!level.getBiome(pos).is(SOUL_SAND_VALLEY)) {
                    continue;
                }

                double surface = surfaceNoise.getValue(x, y, z);
                double gravel = gravelNoise.getValue(x, y, z);

                surfaceValues.add(surface);
                samples.add(new Sample(surface, gravel));
            }
        }

        if (samples.isEmpty()) {
            sender.send(Component.literal("No aoemod:soul_sand_valley samples found at y=" + y + "."));
            return;
        }

        Collections.sort(surfaceValues);

        /*
         * Top 30% surface noise becomes soul sand.
         * Therefore threshold is the 70th percentile.
         */
        double sandThreshold = percentile(surfaceValues, 0.70);

        List<Double> gravelValuesInsideNonSand = new ArrayList<>();

        for (Sample sample : samples) {
            if (sample.surface < sandThreshold) {
                gravelValuesInsideNonSand.add(sample.gravel);
            }
        }

        Collections.sort(gravelValuesInsideNonSand);

        /*
         * Sludge should be 10% total.
         * Since it is chosen only from the 70% non-sand region:
         *
         * 10 / 70 = 0.142857...
         *
         * So sludge should be the top 14.2857% of gravel values inside non-sand.
         * That means threshold is the 85.7142857th percentile.
         */
        double sludgeThreshold = percentile(gravelValuesInsideNonSand, 0.8571428571428571);

        int sand = 0;
        int sludge = 0;
        int soil = 0;

        for (Sample sample : samples) {
            if (sample.surface >= sandThreshold) {
                sand++;
            } else if (sample.gravel >= sludgeThreshold) {
                sludge++;
            } else {
                soil++;
            }
        }

        int total = samples.size();

        sender.send(Component.literal("Sample count: " + total));
        sender.send(Component.literal("Sand threshold for minecraft:surface: " + sandThreshold));
        sender.send(Component.literal("Sludge threshold for minecraft:gravel: " + sludgeThreshold));
        sender.send(Component.literal("Predicted soul_sand: " + percent(sand, total) + "%"));
        sender.send(Component.literal("Predicted soul_sludge: " + percent(sludge, total) + "%"));
        sender.send(Component.literal("Predicted soul_soil: " + percent(soil, total) + "%"));
        sender.send(Component.literal("Use these in your surface_rule JSON."));
    }

    private static double percentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute percentile of empty list.");
        }

        double index = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sortedValues.get(lower);
        }

        double weight = index - lower;
        return sortedValues.get(lower) * (1.0 - weight) + sortedValues.get(upper) * weight;
    }

    private static double percent(int count, int total) {
        return Math.round((count * 10000.0 / total)) / 100.0;
    }

    private record Sample(double surface, double gravel) {}

    @FunctionalInterface
    private interface MessageSender {
        void send(Component message);
    }
}