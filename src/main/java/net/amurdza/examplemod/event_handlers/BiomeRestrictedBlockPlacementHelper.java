package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BiomeRestrictedBlockPlacementHelper {
    private BiomeRestrictedBlockPlacementHelper() {}

    public static Optional<Component> getFailureMessage(Level level, net.minecraft.core.BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        Holder<Biome> biome = level.getBiome(pos);

        Optional<String> requiredBiome = getRequiredBiomeDescription(level, state, biome);

        if (requiredBiome.isEmpty()) {
            return Optional.empty();
        }

        String biomeName = biome.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("this biome");

        return Optional.of(
                Component.empty()
                        .append(block.getName())
                        .append(" cannot be placed in ")
                        .append(Component.literal(biomeName))
        );
    }

    private static Optional<String> getRequiredBiomeDescription(Level level, BlockState state, Holder<Biome> biome) {
        boolean rainforest = biome.is(ModTags.Biomes.tropicalBiomes);
        boolean desert = biome.is(ModTags.Biomes.desertBiomes);
        boolean grove = biome.is(ModTags.Biomes.mountainBiomes); // mountains = grove
        boolean savanna = biome.is(ModTags.Biomes.savannaBiomes);
        boolean plains = biome.is(ModTags.Biomes.plainsBiomes);
        boolean nether = biome.is(ModTags.Biomes.netherBiomes) || level.dimension() == Level.NETHER;
        boolean crimson = biome.is(ModTags.Biomes.crimsonForestBiomes);
        boolean warped = biome.is(ModTags.Biomes.warpedForestBiomes);
        boolean soulSandValley = biome.is(ModTags.Biomes.soulSandValleyBiomes);
        boolean mushroomCaves = biome.is(ModTags.Biomes.mushroomCaves);
        boolean deepDark = biome.is(ModTags.Biomes.deepDarkBiomes);

        if (state.is(ModTags.Blocks.crimsonOnly) && !crimson) {
            return Optional.of("crimson forest");
        }

        if (state.is(ModTags.Blocks.deepDarkOnly) && !deepDark) {
            return Optional.of("deep dark");
        }

        if (state.is(ModTags.Blocks.desertOnly) && !desert) {
            return Optional.of("desert");
        }

        if (state.is(ModTags.Blocks.groveOnly) && !grove) {
            return Optional.of("grove");
        }

        if (state.is(ModTags.Blocks.mushroomCavesOnly) && !mushroomCaves) {
            return Optional.of("mushroom caves");
        }

        if (state.is(ModTags.Blocks.netherOnly) && !nether) {
            return Optional.of("nether");
        }

        if (state.is(ModTags.Blocks.netherNotWarped) && (!nether || warped)) {
            return Optional.of("nether except warped forest");
        }

        if (
                state.is(ModTags.Blocks.notRainforest)
                        && !state.is(ModTags.Blocks.notRainforestExceptions)
                        && rainforest
        ) {
            return Optional.of("not rainforest");
        }

        if (state.is(ModTags.Blocks.overworldOnly) && nether) {
            return Optional.of("overworld");
        }

        if (state.is(ModTags.Blocks.plainsOnly) && !plains) {
            return Optional.of("plains");
        }

        if (state.is(ModTags.Blocks.rainforestDesertOnly) && !(rainforest || desert)) {
            return Optional.of("rainforest or desert");
        }

        if (state.is(ModTags.Blocks.rainforestGroveOnly) && !(rainforest || grove)) {
            return Optional.of("rainforest or grove");
        }

        if (state.is(ModTags.Blocks.rainforestOnly) && !rainforest) {
            return Optional.of("rainforest");
        }

        if (state.is(ModTags.Blocks.rainforestPlainsOnly) && !(rainforest || plains)) {
            return Optional.of("rainforest or plains");
        }

        if (state.is(ModTags.Blocks.rainforestSavannaGroveOnly) && !(rainforest || savanna || grove)) {
            return Optional.of("rainforest, savanna, or grove");
        }

        if (state.is(ModTags.Blocks.rainforestSavannaPlains) && !(rainforest || savanna || plains)) {
            return Optional.of("rainforest, savanna, or plains");
        }

        if (state.is(ModTags.Blocks.rainforestSavannaPlainsGroveOnly) && !(rainforest || savanna || plains || grove)) {
            return Optional.of("rainforest, savanna, plains, or grove");
        }

        if (state.is(ModTags.Blocks.savannaOnly) && !savanna) {
            return Optional.of("savanna");
        }

        if (state.is(ModTags.Blocks.soulSandValleyOnly) && !soulSandValley) {
            return Optional.of("soul sand valley");
        }

        if (state.is(ModTags.Blocks.warpedCrimsonOnly) && !(warped || crimson)) {
            return Optional.of("warped forest or crimson forest");
        }

        if (state.is(ModTags.Blocks.warpedOnly) && !warped) {
            return Optional.of("warped forest");
        }

        return Optional.empty();
    }
}