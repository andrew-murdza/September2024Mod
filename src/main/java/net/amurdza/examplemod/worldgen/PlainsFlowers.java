package net.amurdza.examplemod.worldgen;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.structure.MansionForestFloor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Vanilla Plains' noise-varied flower palette in compact, sparse patches. */
public final class PlainsFlowers {
    public static final int FLOWERS_PER_PATCH = 5;
    public static final int REGION_CHUNKS = 4;
    public static final List<String> FLOWERS = List.of("minecraft:dandelion", "minecraft:poppy",
            "minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:cornflower",
            "minecraft:lily_of_the_valley");
    // Vanilla flower_plain chooses a state for each flower, not once for the
    // whole patch. Map orange/red/pink/white tulips to oxeye/poppy/cornflower/
    // azure, retaining its noise and dandelion weighting. Lilies get one slot
    // in each palette, matching every other non-dandelion's combined weight.
    private static final NoiseThresholdProvider PALETTE = new NoiseThresholdProvider(2345L,
            new NormalNoise.NoiseParameters(0, 1.0D), 0.005F, -0.8F, 1.0F / 3.0F,
            Blocks.DANDELION.defaultBlockState(),
            List.of(Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.POPPY.defaultBlockState(),
                    Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(),
                    Blocks.LILY_OF_THE_VALLEY.defaultBlockState()),
            List.of(Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(),
                    Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                    Blocks.LILY_OF_THE_VALLEY.defaultBlockState()));

    private PlainsFlowers() { }

    public static boolean replacesScatteredPlant(String name) {
        return FLOWERS.contains(name) || name.equals("minecraft:sunflower");
    }

    public static boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        return placeChunk(context.level(), chunkX, chunkZ,
                MansionForestFloor.forChunk(context.level(), new net.minecraft.world.level.ChunkPos(chunkX, chunkZ))) > 0;
    }

    public static int placeChunk(WorldGenLevel level, int chunkX, int chunkZ, List<MansionForestFloor.Forest> forests) {
        boolean forest = forests.stream().anyMatch(value -> value.contains(chunkX * 16 + 8, chunkZ * 16 + 8));
        int planted = 0;
        for (var patch : flowerPatches(level.getSeed(), chunkX, chunkZ, forest)) planted += placePatch(level, patch, forests);
        return planted;
    }

    public static List<FlowerPatch> flowerPatches(long seed, int chunkX, int chunkZ) {
        return flowerPatches(seed, chunkX, chunkZ, false);
    }

    /** One selected chunk in every 4x4 region; a second independent slot in forests. */
    public static List<FlowerPatch> flowerPatches(long seed, int chunkX, int chunkZ, boolean forest) {
        int regionX = Math.floorDiv(chunkX, REGION_CHUNKS), regionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);
        RandomSource region = RandomSource.create(hash(seed ^ 0x74d81L, regionX, regionZ));
        int first = region.nextInt(16), second = (first + 1 + region.nextInt(15)) % 16;
        int index = Math.floorMod(chunkX, REGION_CHUNKS) + REGION_CHUNKS * Math.floorMod(chunkZ, REGION_CHUNKS);
        if (index != first && (!forest || index != second)) return List.of();
        RandomSource random = RandomSource.create(hash(seed, chunkX, chunkZ));
        return List.of(new FlowerPatch(chunkX * 16 + 1 + random.nextInt(10),
                chunkZ * 16 + 1 + random.nextInt(10), random.nextLong()));
    }

    public static int placePatch(WorldGenLevel level, FlowerPatch patch) {
        return placePatch(level, patch, MansionForestFloor.forChunk(level,
                new net.minecraft.world.level.ChunkPos(patch.x() >> 4, patch.z() >> 4)));
    }

    private static int placePatch(WorldGenLevel level, FlowerPatch patch, List<MansionForestFloor.Forest> forests) {
        RandomSource random = RandomSource.create(patch.seed());
        int[] offsets = new int[36];
        for (int i = 0; i < offsets.length; i++) offsets[i] = i;
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), swap = offsets[i]; offsets[i] = offsets[j]; offsets[j] = swap;
        }
        List<BlockPos> roots = new ArrayList<>();
        int existing = 0;
        for (int offset : offsets) {
            int x = patch.x() + offset % 6, z = patch.z() + offset / 6;
            int rootY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            for (var forest : forests) {
                Integer forestRoot = forest.rootY(level, x, z);
                if (forestRoot != null) { rootY = forestRoot; break; }
            }
            BlockPos pos = new BlockPos(x, rootY, z);
            var current = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
            if (current != null && FLOWERS.contains(current.toString())) existing++;
            if (!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                    || !GameplayDimensions.biomeIs(level, pos, "plains")
                    || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            roots.add(pos);
            if (roots.size() > FLOWERS_PER_PATCH) roots.remove(roots.size() - 1);
        }
        if (existing >= FLOWERS_PER_PATCH) return 0;
        int needed = FLOWERS_PER_PATCH - existing;
        if (roots.size() < needed) return 0;
        while (roots.size() > needed) roots.remove(roots.size() - 1);
        List<String> mixture = new ArrayList<>();
        for (BlockPos pos : roots) {
            mixture.add(ForgeRegistries.BLOCKS.getKey(PALETTE.getState(random, pos).getBlock()).toString());
        }
        // Commit all five flowers together without replacing grass or crops.
        GrowthPlacementBuffer buffer = new GrowthPlacementBuffer(level);
        for (int i = 0; i < roots.size(); i++) {
            if (!DocumentedFloraFeature.placePlant(buffer, roots.get(i), mixture.get(i), random)) return 0;
        }
        return buffer.commit() ? needed : 0;
    }

    private static long hash(long seed, int x, int z) {
        return TropicalLayout.mix(seed ^ 0x2f10b1c85d7L ^ TropicalLayout.mix(x * 0x632be59bd9b4e019L)
                ^ TropicalLayout.mix(z * 0x9e3779b97f4a7c15L));
    }

    /** x/z are the lower corner of a six-by-six footprint, not its center. */
    public record FlowerPatch(int x, int z, long seed) { }
}
