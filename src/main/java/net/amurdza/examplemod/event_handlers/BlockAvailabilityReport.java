package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A review aid generated from the actual registered blocks, tags, and loaded recipes. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class BlockAvailabilityReport {
    private static final List<ResourceKey<Level>> DIMENSIONS = List.of(GameplayDimensions.TROPICAL,
            GameplayDimensions.SAVANNA, GameplayDimensions.PLAINS, GameplayDimensions.SNOWY,
            GameplayDimensions.DESERT, GameplayDimensions.NETHER);

    private BlockAvailabilityReport() { }

    @SubscribeEvent
    public static void writeReports(ServerStartedEvent event) {
        List<ServerLevel> levels = DIMENSIONS.stream().map(event.getServer()::getLevel).toList();
        if (levels.stream().anyMatch(level -> level == null)) {
            AOEMod.LOGGER.warn("Skipping block availability report: not all six AOE dimensions are loaded");
            return;
        }
        List<Block> blocks = ForgeRegistries.BLOCKS.getValues().stream()
                .sorted(Comparator.comparing(block -> ForgeRegistries.BLOCKS.getKey(block).toString())).toList();
        List<String> all = new ArrayList<>();
        List<String> none = new ArrayList<>();
        StringBuilder table = new StringBuilder("| Block ID | Tropical | Savanna | Plains | Snowy | Desert | Nether |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- |\n");
        StringBuilder ticking = new StringBuilder("# Registered random-ticking blocks without a growth override\n\n")
                .append("Generated from every possible block state in the loaded registry. This includes installed backports and dependency mods. ")
                .append("Registered compatibility copies and hidden dependency content remain listed even when normal acquisition is disabled. ")
                .append("A block is listed if any state requests random ticks and it is absent from `BlockGrowthConfig`. ")
                .append("Tick eligibility does not guarantee meaningful work: some blocks inherit an empty random-tick method. ")
                .append("See `RANDOM_TICK_REVIEW.md` for the behavior distinction.\n\n")
                .append("| Block ID | Random-ticking states | Total states |\n| --- | --- | --- |\n");
        for (Block block : blocks) {
            String id = ForgeRegistries.BLOCKS.getKey(block).toString();
            int mask = BiomePlacementRestrictions.allowedMask(block);
            int count = 0;
            table.append("| `").append(id).append("` |");
            for (ServerLevel level : levels) {
                int dimension = BiomePlacementRestrictions.dimensionMask(level);
                boolean allowed = (mask & dimension) != 0
                        && DimensionMaterialAvailability.isAvailable(level, BiomePlacementRestrictions.materialItem(block), dimension);
                if (allowed) count++;
                table.append(allowed ? " Yes |" : " — |");
            }
            table.append('\n');
            if (count == DIMENSIONS.size()) all.add(id);
            if (count == 0) none.add(id);
            long randomStates = block.getStateDefinition().getPossibleStates().stream()
                    .filter(state -> state.isRandomlyTicking()).count();
            if (randomStates > 0 && !BlockConfig.hasGrowthConfig(block)) {
                ticking.append("| `").append(id).append("` | ").append(randomStates).append(" | ")
                        .append(block.getStateDefinition().getPossibleStates().size()).append(" |\n");
            }
        }
        StringBuilder report = new StringBuilder("# Loaded block availability\n\n")
                .append("Generated from the running server's block registry, tags, and recipes. Includes all installed backports. ")
                .append("‘All’ and ‘none’ refer to the six AOE dimensions, not vanilla or other mods' dimensions, which retain their original placement. ")
                .append("These are dimension/material eligibility results, not a guarantee of placement on every surface. ")
                .append("This does not promise that a block is obtainable: hidden dependency content and technical blocks remain in the registry and are listed too. ")
                .append("Mycelium, nylium, netherrack, soul sediments and living Nether plants also have local biome checks. ")
                .append("Block survival, collisions, water, support, and two-block clearance still apply.\n\n")
                .append("An entry in ‘none’ can be intentional when no dimension allows every ingredient and the finished block. ")
                .append("The six dimension documents take priority over older placement annotations. ")
                .append("Recipe ingredients and building restrictions can still exclude a finished product even when individual ingredients occur elsewhere. ")
                .append("Local biome requirements are checked when placing or growing the block; this dimension summary does not remove them.\n\n")
                .append("## Allowed in all six dimensions\n\n");
        appendList(report, all);
        report.append("\n## Allowed in none of the six dimensions\n\n");
        appendList(report, none);
        report.append("\n## Every registered block\n\n").append(table);
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path projectDirectory = Files.exists(workingDirectory.resolve("build.gradle")) ? workingDirectory
                : workingDirectory.getFileName().toString().equals("run") ? workingDirectory.getParent() : workingDirectory;
        Path directory = projectDirectory.resolve("build/reports");
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("BLOCK_AVAILABILITY.md"), report, StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("RANDOM_TICK_BLOCKS.md"), ticking, StandardCharsets.UTF_8);
            AOEMod.LOGGER.info("Wrote block availability and random-tick reports to {}", directory);
        } catch (IOException exception) {
            AOEMod.LOGGER.error("Unable to write block review reports", exception);
        }
    }

    private static void appendList(StringBuilder target, List<String> blocks) {
        if (blocks.isEmpty()) target.append("None.\n");
        for (String id : blocks) target.append("- `").append(id).append("`\n");
    }
}
