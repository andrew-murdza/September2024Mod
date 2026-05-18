package net.amurdza.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public class AoeBiomeBandTestCommand {
    private static final String[] BIOMES = {
            "warm_ocean",
            "jungle",
            "savanna",
            "plains",
            "grove",
            "badlands"
    };

    private static final double DEFAULT_WARM_OCEAN_CUTOFF = -0.70;
    private static final double DEFAULT_JUNGLE_CUTOFF = -0.35;
    private static final double DEFAULT_SAVANNA_CUTOFF = -0.12;

    private static double warmOceanCutoff = DEFAULT_WARM_OCEAN_CUTOFF;
    private static double jungleCutoff = DEFAULT_JUNGLE_CUTOFF;
    private static double savannaCutoff = DEFAULT_SAVANNA_CUTOFF;

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("aoe_biome_band_test")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("start_seed", LongArgumentType.longArg())
                                .then(Commands.argument("seed_count", IntegerArgumentType.integer(1, 10000))
                                        .then(Commands.argument("noise_scale", DoubleArgumentType.doubleArg(0.00001, 1.0))
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(100, 100000))
                                                        .then(Commands.argument("step", IntegerArgumentType.integer(1, 64))
                                                                .then(Commands.argument("first_octave", IntegerArgumentType.integer(-20, 20))
                                                                        .executes(ctx -> run(
                                                                                ctx.getSource(),
                                                                                LongArgumentType.getLong(ctx, "start_seed"),
                                                                                IntegerArgumentType.getInteger(ctx, "seed_count"),
                                                                                DoubleArgumentType.getDouble(ctx, "noise_scale"),
                                                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                                                IntegerArgumentType.getInteger(ctx, "step"),
                                                                                IntegerArgumentType.getInteger(ctx, "first_octave")
                                                                        ))))))))
        );

        dispatcher.register(
                Commands.literal("aoe_show_cutoffs")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> showCutoffs(ctx.getSource()))
        );

        dispatcher.register(
                Commands.literal("aoe_reset_cutoffs")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> resetCutoffs(ctx.getSource()))
        );

        dispatcher.register(
                Commands.literal("aoe_set_warm_ocean_cutoff")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(-10.0, 10.0))
                                .executes(ctx -> setWarmOceanCutoff(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "value")
                                )))
        );

        dispatcher.register(
                Commands.literal("aoe_set_jungle_cutoff")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(-10.0, 0.0))
                                .executes(ctx -> setJungleCutoff(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "value")
                                )))
        );

        dispatcher.register(
                Commands.literal("aoe_set_savanna_cutoff")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(-10.0, 0.0))
                                .executes(ctx -> setSavannaCutoff(
                                        ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "value")
                                )))
        );
    }

    private static int run(
            CommandSourceStack source,
            long startSeed,
            int seedCount,
            double noiseScale,
            int radius,
            int step,
            int firstOctave
    ) throws CommandSyntaxException {
        int originX = source.getPlayerOrException().blockPosition().getX();
        int originZ = source.getPlayerOrException().blockPosition().getZ();

        List<Run> allRuns = new ArrayList<>();

        for (int s = 0; s < seedCount; s++) {
            long seed = startSeed + s;

            NormalNoise noise = NormalNoise.create(
                    RandomSource.create(seed),
                    firstOctave,
                    1
            );

            scanLine(noise, noiseScale, originX, originZ,  1,  0, radius, step, "+X", seed, allRuns);
            scanLine(noise, noiseScale, originX, originZ, -1,  0, radius, step, "-X", seed, allRuns);
            scanLine(noise, noiseScale, originX, originZ,  0,  1, radius, step, "+Z", seed, allRuns);
            scanLine(noise, noiseScale, originX, originZ,  0, -1, radius, step, "-Z", seed, allRuns);
        }

        source.sendSuccess(() -> Component.literal("=== AOE simplified biome band test ==="), false);
        source.sendSuccess(() -> Component.literal(
                "start_seed=" + startSeed +
                        ", seed_count=" + seedCount +
                        ", noise_scale=" + noiseScale +
                        ", radius=" + radius +
                        ", step=" + step +
                        ", first_octave=" + firstOctave
        ), false);

        sendCutoffSummary(source);

        for (int i = 0; i < BIOMES.length; i++) {
            final int biomeIndex = i;

            List<Integer> widths = allRuns.stream()
                    .filter(r -> r.biomeIndex == biomeIndex)
                    .map(r -> r.width)
                    .sorted()
                    .toList();

            if (widths.isEmpty()) {
                source.sendSuccess(() -> Component.literal(BIOMES[biomeIndex] + ": not found"), false);
                continue;
            }

            int min = widths.get(0);
            int p5 = percentile(widths, 0.05);
            int p25 = percentile(widths, 0.25);
            int p50 = percentile(widths, 0.50);
            int p75 = percentile(widths, 0.75);
            int p95 = percentile(widths, 0.95);
            int max = widths.get(widths.size() - 1);
            double avg = widths.stream().mapToInt(v -> v).average().orElse(0.0);

            source.sendSuccess(() -> Component.literal(
                    BIOMES[biomeIndex] +
                            ": count=" + widths.size() +
                            ", min=" + min +
                            ", p5=" + p5 +
                            ", p25=" + p25 +
                            ", p50=" + p50 +
                            ", p75=" + p75 +
                            ", p95=" + p95 +
                            ", max=" + max +
                            ", avg=" + String.format(Locale.ROOT, "%.1f", avg) +
                            checkWidth(biomeIndex, p5, p95)
            ), false);
        }

        return 1;
    }

    private static void scanLine(
            NormalNoise noise,
            double noiseScale,
            int originX,
            int originZ,
            int dx,
            int dz,
            int radius,
            int step,
            String label,
            long seed,
            List<Run> allRuns
    ) {
        int previousBiome = -1;
        int runStart = 0;

        for (int d = 0; d <= radius; d += step) {
            int x = originX + dx * d;
            int z = originZ + dz * d;

            double value = noise.getValue(x * noiseScale, 0.0, z * noiseScale);
            int biome = classify(value);

            if (previousBiome == -1) {
                previousBiome = biome;
                runStart = d;
            } else if (biome != previousBiome) {
                allRuns.add(new Run(seed, label, previousBiome, d - runStart));
                previousBiome = biome;
                runStart = d;
            }
        }

        if (previousBiome != -1) {
            allRuns.add(new Run(seed, label, previousBiome, radius - runStart));
        }
    }

    private static int classify(double value) {
        if (value < warmOceanCutoff) {
            return 0; // warm_ocean
        }

        if (value < jungleCutoff) {
            return 1; // jungle
        }

        if (value < savannaCutoff) {
            return 2; // savanna
        }

        if (value < -savannaCutoff) {
            return 3; // plains
        }

        if (value < -jungleCutoff) {
            return 4; // grove
        }

        return 5; // badlands
    }

    private static int percentile(List<Integer> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int index = (int)Math.round(p * (sorted.size() - 1));
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static String checkWidth(int biomeIndex, int p5, int p95) {
        String biome = BIOMES[biomeIndex];

        if (biome.equals("warm_ocean") || biome.equals("badlands")) {
            return " edge biome, ignored";
        }

        return p5 >= 400 && p95 <= 800 ? " OK" : " BAD, target 400-800";
    }

    private static int showCutoffs(CommandSourceStack source) {
        sendCutoffSummary(source);
        return 1;
    }

    private static int resetCutoffs(CommandSourceStack source) {
        warmOceanCutoff = DEFAULT_WARM_OCEAN_CUTOFF;
        jungleCutoff = DEFAULT_JUNGLE_CUTOFF;
        savannaCutoff = DEFAULT_SAVANNA_CUTOFF;

        source.sendSuccess(() -> Component.literal("Cutoffs reset."), false);
        sendCutoffSummary(source);
        return 1;
    }

    private static int setWarmOceanCutoff(CommandSourceStack source, double value) {
        warmOceanCutoff = value;
        source.sendSuccess(() -> Component.literal("Set warm ocean cutoff = " + value), false);
        sendCutoffSummary(source);
        return 1;
    }

    private static int setJungleCutoff(CommandSourceStack source, double value) {
        jungleCutoff = value;
        source.sendSuccess(() -> Component.literal("Set jungle cutoff = " + value), false);
        sendCutoffSummary(source);
        return 1;
    }

    private static int setSavannaCutoff(CommandSourceStack source, double value) {
        savannaCutoff = value;
        source.sendSuccess(() -> Component.literal("Set savanna cutoff = " + value), false);
        sendCutoffSummary(source);
        return 1;
    }

    private static void sendCutoffSummary(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Cutoffs:"
                        + " warm_ocean→jungle=" + warmOceanCutoff
                        + ", jungle→savanna=" + jungleCutoff
                        + ", savanna→plains=" + savannaCutoff
                        + ", plains→grove=" + (-savannaCutoff)
                        + ", grove→badlands=" + (-jungleCutoff)
        ), false);
    }

    private record Run(long seed, String line, int biomeIndex, int width) {}
}