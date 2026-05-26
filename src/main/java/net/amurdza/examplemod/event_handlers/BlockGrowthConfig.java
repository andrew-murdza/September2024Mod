package net.amurdza.examplemod.event_handlers;

import com.legacy.blue_skies.registries.SkiesBlocks;
import com.teamabnormals.upgrade_aquatic.core.registry.UABlocks;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.HashMap;
import java.util.Map;

public final class BlockGrowthConfig {
    private BlockGrowthConfig() {}

    /**
     * Main structure: biomeTag -> (block -> multiplier)
     */
    public static final Map<TagKey<Biome>, Map<Block, Float>> GROWTH_MULTIPLIER_BY_TAG_BY_BLOCK = new HashMap<>();

    /**
     * A "default" per-block multiplier when a biome matches none of our known tags.
     * (We store it as its own map, not a tag.)
     */
    public static final Map<Block, Float> DEFAULT_OTHER_BIOMES = new HashMap<>();

    public static void init() {
        // Ensure tag maps exist
        ensureTag(ModTags.Biomes.tropicalBiomes);
        ensureTag(ModTags.Biomes.savannaBiomes);
        ensureTag(ModTags.Biomes.mountainBiomes);
        ensureTag(ModTags.Biomes.mushroomCaves);
        ensureTag(ModTags.Biomes.desertBiomes);

        ensureTag(ModTags.Biomes.deepDarkBiomes);
        ensureTag(ModTags.Biomes.netherBiomes);
        ensureTag(ModTags.Biomes.basaltDeltasBiomes);
        ensureTag(ModTags.Biomes.crimsonForestBiomes);
        ensureTag(ModTags.Biomes.warpedForestBiomes);
        ensureTag(ModTags.Biomes.soulSandValleyBiomes);

        // Register everything
        registerVanillaBlocks();
        registerModdedBlocks();
        registerSaplings();
    }

    private static void ensureTag(TagKey<Biome> tag) {
        GROWTH_MULTIPLIER_BY_TAG_BY_BLOCK.computeIfAbsent(tag, t -> new HashMap<>());
    }

    private static void put(TagKey<Biome> tag, Block block, float value) {
        ensureTag(tag);
        GROWTH_MULTIPLIER_BY_TAG_BY_BLOCK.get(tag).put(block, value);
    }

    private static void putDefault(Block block, float value) {
        DEFAULT_OTHER_BIOMES.put(block, value);
    }

    /**
     * Adds a block and fills all relevant tag entries at once.
     * Any tag not specified here should be treated as "other" via DEFAULT_OTHER_BIOMES,
     * but for sanity we explicitly set all tags you care about.
     */
    private static void addToMap(
            Block block,
            float tropical,
            float savanna,
            float mountains,
            float mountainCaves,
            float desert,
            float deepDark,
            float netherGeneric,
            float basaltDeltas,
            float crimsonForest,
            float warpedForest,
            float soulSandValley,
            float other
    ) {
        put(ModTags.Biomes.tropicalBiomes, block, tropical);
        put(ModTags.Biomes.savannaBiomes, block, savanna);
        put(ModTags.Biomes.mountainBiomes, block, mountains);
        put(ModTags.Biomes.mushroomCaves, block, mountainCaves);
        put(ModTags.Biomes.desertBiomes, block, desert);

        put(ModTags.Biomes.deepDarkBiomes, block, deepDark);
        put(ModTags.Biomes.netherBiomes, block, netherGeneric);
        put(ModTags.Biomes.basaltDeltasBiomes, block, basaltDeltas);
        put(ModTags.Biomes.crimsonForestBiomes, block, crimsonForest);
        put(ModTags.Biomes.warpedForestBiomes, block, warpedForest);
        put(ModTags.Biomes.soulSandValleyBiomes, block, soulSandValley);

        putDefault(block, other);
    }

// ------------------------------------------------------------
// Your rules encoded as “templates” (UPDATED: jungle -> tropical)
// ------------------------------------------------------------

    private static void addKelp(Block block) {
        // kelp: 0.7 mountains/tropical, 0.28 savanna, 0 desert/nether, 0.14 other
        addToMap(block,
                0.7f,   // tropical
                0.28f,  // savanna
                0.7f,   // mountains
                0.7f,   // mountain caves
                0.0f,   // desert
                0.0f,   // deep dark
                0.0f,   // nether generic
                0.0f,   // basalt deltas
                0.0f,   // crimson forest
                0.0f,   // warped forest
                0.0f,   // soul sand valley
                0.0f   // other
        );
    }

    private static void addWeepingTwistingWart(Block block, float crimson, float warped, float soul) {
        // weeping/twisting/nether wart:
        // 0.7 tropical, 0 desert, 0.05 basalt deltas,
        // 0.1 in corresponding nether biome tag, 0 other.
        addToMap(block,
                0.7f,   // tropical
                0.0f,   // savanna
                0.0f,   // mountains
                0.0f,   // mountain caves
                0.0f,   // desert
                0.0f,   // deep dark
                0.0f,   // nether generic
                0.05f,  // basalt deltas
                crimson,// crimson forest
                warped, // warped forest
                soul,   // soul sand valley
                0.0f    // other
        );
    }

    private static void addAmethyst(Block block) {
        // amethyst: 1 in mountains, 0 otherwise
        addToMap(block,
                0.0f,  // tropical
                0.0f,  // savanna
                1.0f,  // mountains
                1.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );
    }

    private static void addMushroom(Block block) {
        // mushroom:
        // 0.2 mountains, 0.8 mountain caves, 0.5 tropical, 0.2 savanna, 0.2 desert/nether, 0.2 other
        addToMap(block,
                0.8f,  // tropical
                0.2f,  // savanna
                0.2f,  // mountains
                0.8f,  // mountain caves
                0.2f,  // desert
                0.2f,   // deep dark
                0.2f,  // nether generic
                0.2f,  // basalt deltas
                0.2f,  // crimson forest
                0.2f,  // warped forest
                0.2f,  // soul sand valley
                0.2f   // other
        );
    }

    private static void addPointedDripstone() {
        // pointed dripstone: 0.7 mountains, 0.1 desert, 0 otherwise
        addToMap(Blocks.POINTED_DRIPSTONE,
                0.0f,  // tropical
                0.0f,  // savanna
                0.7f,  // mountains
                0.7f,  // mountain caves
                0.1f,  // desert
                0.1f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );
    }

    private static void addOverworldCrops(Block block, boolean tropicalOnly) {
        // crops:
        // tropical 1, savanna 0.6, mountains 0, desert 0, nether 0, other 0.34
        // BUT for some blocks you want "tropical only": 1 in tropical and 0 elsewhere.
        float other = tropicalOnly ? 0.0f : 0.34f;

        addToMap(block,
                1.0f, // tropical
                0.6f, // savanna
                0.0f, // mountains
                0.0f, // mountain caves
                0.0f, // desert
                0.0f,   // deep dark
                0.0f, // nether generic
                0.0f, // basalt deltas
                0.0f, // crimson forest
                0.0f, // warped forest
                0.0f, // soul sand valley
                other // other
        );

        if (tropicalOnly) {
            // override "other" already 0; also force 0 for savanna if you truly mean tropical-only
            put(ModTags.Biomes.savannaBiomes, block, 0.0f);
        }
    }

    private static void addTropicalOnly(Block block) {
        // cave vines, bamboo, bamboo sapling, grape vine: 1 in tropical, 0 otherwise
        addToMap(block,
                1.0f,  // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );
    }

    private static void addTropicalOnlyCocoaLike(Block block) {
        // jungle sweet berries and cocoa: 0.8 in tropical, 0 otherwise
        addToMap(block,
                0.8f,  // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );
    }

    private static void addSoulBerries(Block block) {
        // soul berries:
        // 0.2 in tropical and soul sand valley,
        // 0.1 warped/crimson,
        // 0.05 basalt deltas
        addToMap(block,
                0f,  // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.05f, // basalt deltas
                0.1f,  // crimson forest
                0.1f,  // warped forest
                0.2f,  // soul sand valley
                0.0f   // other
        );
    }

    // ---------------------------
// registerSaplings() UPDATED
// ---------------------------
    private static void registerSaplings() {
        // Spruce: 0.7 tropical, 0.26 mountains, 0 other
        addToMap(Blocks.SPRUCE_SAPLING,
                0.7f, 0.0f, 0.26f, 0.26f, 0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );

        // Acacia: 0.30 savanna, 0.70 tropical, 0 other
        addToMap(Blocks.ACACIA_SAPLING,
                0.70f, 0.30f, 0.0f, 0.0f, 0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );

        addToMap(Blocks.OAK_SAPLING,
                0.7f, 0.2f, 0.0f, 0.0f, 0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.15f
        );

        addToMap(Blocks.BIRCH_SAPLING,
                0.7f, 0.2f, 0.0f, 0.0f, 0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.15f
        );

        addToMap(Blocks.CHERRY_SAPLING,
                0.7f,0.0f,0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );

        addToMap(Blocks.MANGROVE_PROPAGULE,
                0.7f,0.0f,0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );

        addToMap(Blocks.DARK_OAK_SAPLING,
                0.5f,0.0f,0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );

        addToMap(Blocks.JUNGLE_SAPLING,
                1.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,
                0.0f
        );
    }

    // ------------------------------------------------------------
    // Vanilla registration
    // ------------------------------------------------------------
    private static void registerVanillaBlocks() {
        // NOTE: put your imports for Blocks in your actual project.
        // These are the blocks you asked about earlier.

        // Kelp
        addKelp(Blocks.KELP);
        addKelp(Blocks.KELP_PLANT);

        // Weeping / twisting / wart
        addWeepingTwistingWart(Blocks.WEEPING_VINES, 0.1f, 0.0f, 0.0f);
        addWeepingTwistingWart(Blocks.WEEPING_VINES_PLANT, 0.1f, 0.0f, 0.0f);

        addWeepingTwistingWart(Blocks.TWISTING_VINES, 0.0f, 0.1f, 0.0f);
        addWeepingTwistingWart(Blocks.TWISTING_VINES_PLANT, 0.0f, 0.1f, 0.0f);

        addWeepingTwistingWart(Blocks.NETHER_WART, 0.0f, 0.0f, 0.1f);

        // Amethyst buds (driven by budding amethyst random ticks, but you want a “block chance” entry)
        addAmethyst(Blocks.SMALL_AMETHYST_BUD);
        addAmethyst(Blocks.MEDIUM_AMETHYST_BUD);
        addAmethyst(Blocks.LARGE_AMETHYST_BUD);
        addAmethyst(Blocks.AMETHYST_CLUSTER);
        addAmethyst(Blocks.BUDDING_AMETHYST);

        // Mushroom spread (apply to mushroom blocks)
        addMushroom(Blocks.BROWN_MUSHROOM);
        addMushroom(Blocks.RED_MUSHROOM);

        // Pointed dripstone
        addPointedDripstone();

        // Sweet berries (regular): 0 in desert/nether, 0.8 jungle/tropical, else 0.2
        addToMap(Blocks.SWEET_BERRY_BUSH,
                0.8f,  // tropical
                0.2f,  // savanna
                0.2f,  // mountains
                0.2f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.2f   // other
        );

        // Cocoa (jungle only 0.8)
        addTropicalOnlyCocoaLike(Blocks.COCOA);

        // Cactus
        addToMap(Blocks.CACTUS,
                16.0f, // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                4.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );

        addOverworldCrops(Blocks.PUMPKIN_STEM, true);
        addOverworldCrops(Blocks.ATTACHED_PUMPKIN_STEM, true);
        addOverworldCrops(Blocks.MELON_STEM, true);
        addOverworldCrops(Blocks.ATTACHED_MELON_STEM, true);
        addOverworldCrops(Blocks.WHEAT, false);
        addOverworldCrops(Blocks.CARROTS, false);
        addOverworldCrops(Blocks.POTATOES, false);
        addOverworldCrops(Blocks.BEETROOTS, false); // your global ÷3 is applied in the event

        addTropicalOnly(Blocks.CAVE_VINES);
        addTropicalOnly(Blocks.CAVE_VINES_PLANT);

        addTropicalOnly(Blocks.VINE);

        addToMap(Blocks.TURTLE_EGG,
                0.2f,  // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );

        // Sea pickle values (replace the existing addToMap(...) for SEA_PICKLE)
        addToMap(Blocks.SEA_PICKLE,
                1.0f,  // tropical
                0.4f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );

        addOverworldCrops(Blocks.PITCHER_CROP, false);

        addTropicalOnly(Blocks.BAMBOO);
        addTropicalOnly(Blocks.BAMBOO_SAPLING);
    }

    // ------------------------------------------------------------
    // Modded registration
    // ------------------------------------------------------------
    private static void registerModdedBlocks() {

        addTropicalOnlyCocoaLike(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get());
        addTropicalOnlyCocoaLike(UABlocks.MULBERRY_VINE.get()); // "copy from cocoa"
        addSoulBerries(ModBlocks.SOUL_BERRY_BUSH.get());

        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.ONION_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.TOMATO_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.CABBAGE_CROP.get(), true);
        addOverworldCrops(vectorwing.farmersdelight.common.registry.ModBlocks.RICE_CROP.get(), true);

        // Pickerelweed: 0.6 in tropical, 0 otherwise (replace the existing addToMap for PICKERELWEED)
        addToMap(UABlocks.PICKERELWEED.get(),
                0.6f,  // tropical
                0.0f,  // savanna
                0.0f,  // mountains
                0.0f,  // mountain caves
                0.0f,  // desert
                0.0f,   // deep dark
                0.0f,  // nether generic
                0.0f,  // basalt deltas
                0.0f,  // crimson forest
                0.0f,  // warped forest
                0.0f,  // soul sand valley
                0.0f   // other
        );

        // BWG mushrooms etc (copy from mushrooms -> your mushroom rule)
        addMushroom(GlimmeringWealdModule.glow_shroom);
        addMushroom(SkiesBlocks.snowcap_pinhead);
    }

}
