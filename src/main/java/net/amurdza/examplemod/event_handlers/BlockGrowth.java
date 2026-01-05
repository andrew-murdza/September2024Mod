package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.block.NetherCropBlock;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Map;

@EventBusSubscriber(modid = AOEMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class BlockGrowth {

    private static final IntegerProperty[] AGE_PROPERTIES = new IntegerProperty[]{
            BlockStateProperties.AGE_1,
            BlockStateProperties.AGE_2,
            BlockStateProperties.AGE_3,
            BlockStateProperties.AGE_4,
            BlockStateProperties.AGE_5,
            BlockStateProperties.AGE_7,
            BlockStateProperties.AGE_15,
            BlockStateProperties.AGE_25
    };

    private static final int[] MAX_AGES = new int[]{1, 2, 3, 4, 5, 7, 15, 25};

    @SubscribeEvent
    public static void blockGrowthPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        // Only handle blocks we know about:
        float mult = getMultiplier(level, pos, block);
        if (mult < 0f) return; // not tracked, let vanilla/mods handle it

        // Beetroot global /3
        if (block == Blocks.BEETROOTS) {
            mult = mult / 3.0f;
        }

        // Optional: keep your "not fertile halves chance" rule for CropBlock / StemBlock
        // (This scales the multiplier, which effectively scales the chance of allowing vanilla growth)
        if (block instanceof CropBlock || block instanceof StemBlock) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFertile(level, below)) {
                mult *= 0.5f;
            }
        }

        // If it doesn't grow here:
        if (mult <= 0.0f) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Compute how many "growth increments" we want on this tick:
        // - for 0<m<1: increments is either 0 or 1 (prob=m)
        // - for m>=1: increments is 1 + floor(m-1) + (rand < frac ? 1 : 0)
        int increments = computeIncrements(level.random, mult);
        if (increments <= 0) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (increments == 1) {
            // Let vanilla proceed normally this tick.
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // If we want >1 increments, we must apply ourselves (for age-based blocks).
        // For non-age blocks (cactus/sugarcane/bamboo/etc), we can't safely "multi-grow" here;
        // so we simply allow vanilla once.
        if (!hasAnyAgeProperty(state)) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // Apply multi-increment by denying vanilla growth and setting the age forward ourselves.
        event.setResult(Event.Result.DENY);

        applyAgeIncrements(level, pos, state, increments);
        // Call Forge post hook similar to your old code
        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
    }

    private static int computeIncrements(RandomSource rand, float mult) {
        if (mult < 1.0f) {
            return (rand.nextFloat() < mult) ? 1 : 0;
        }
        int base = 1 + Mth.floor(mult - 1.0f);
        float frac = (mult - 1.0f) - Mth.floor(mult - 1.0f);
        if (frac > 0.0f && rand.nextFloat() < frac) base += 1;
        return base;
    }

    private static boolean hasAnyAgeProperty(BlockState state) {
        for (IntegerProperty p : AGE_PROPERTIES) {
            if (state.hasProperty(p)) return true;
        }
        return false;
    }

    private static void applyAgeIncrements(ServerLevel level, BlockPos pos, BlockState state, int increments) {
        for (int i = 0; i < AGE_PROPERTIES.length; i++) {
            IntegerProperty prop = AGE_PROPERTIES[i];
            if (!state.hasProperty(prop)) continue;

            int max = MAX_AGES[i];
            int oldAge = state.getValue(prop);
            int newAge = oldAge + increments;

            // Special case you had: sugar cane uses pos.above() in your old system
            // (but sugar cane doesn't use AGE properties; this is mostly harmless)

            if (newAge > max) {
                // Wrap-around behavior you used before:
                // reset this block to age 0 and "replant" default above/below logic.
                // For ordinary crops this rarely matters; for stems it can.
                level.setBlockAndUpdate(pos, state.setValue(prop, 0));
            } else {
                level.setBlock(pos, state.setValue(prop, newAge), 4);
            }
            return;
        }
    }

    /**
     * Returns:
     * - >=0 : multiplier for this block in this biome bucket
     * - -1  : not tracked, let vanilla/mod behavior happen
     */
    private static float getMultiplier(ServerLevel level, BlockPos pos, Block block) {
        // If we didn't initialize, treat as untracked
        if (BlockGrowthConfig.GROWTH_MULTIPLIER_BY_TAG_BY_BLOCK.isEmpty() && BlockGrowthConfig.DEFAULT_OTHER_BIOMES.isEmpty()) {
            return -1f;
        }

        Holder<Biome> biome = level.getBiome(pos);

        // Determine which bucket/tag to use (priority order)
        TagKey<Biome> tag = pickBucket(biome);

        // Look up value
        Float v = null;
        if (tag != null) {
            Map<Block, Float> map = BlockGrowthConfig.GROWTH_MULTIPLIER_BY_TAG_BY_BLOCK.get(tag);
            if (map != null) v = map.get(block);
        }

        if (v == null) v = BlockGrowthConfig.DEFAULT_OTHER_BIOMES.get(block);
        return (v == null) ? -1f : v;
    }

    private static TagKey<Biome> pickBucket(Holder<Biome> biome) {
        // Nether sub-biomes (specific)
        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) return ModTags.Biomes.basaltDeltasBiomes;
        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) return ModTags.Biomes.crimsonForestBiomes;
        if (biome.is(ModTags.Biomes.warpedForestBiomes)) return ModTags.Biomes.warpedForestBiomes;
        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) return ModTags.Biomes.soulSandValleyBiomes;

        // Big buckets
        if (biome.is(ModTags.Biomes.tropicalBiomes)) return ModTags.Biomes.tropicalBiomes;
        if (biome.is(ModTags.Biomes.savannaBiomes)) return ModTags.Biomes.savannaBiomes;

        // Mountain caves before mountains
        if (biome.is(ModTags.Biomes.mushroomCaves)) return ModTags.Biomes.mushroomCaves;
        if (biome.is(ModTags.Biomes.mountainBiomes)) return ModTags.Biomes.mountainBiomes;

        if (biome.is(ModTags.Biomes.desertBiomes)) return ModTags.Biomes.desertBiomes;
        if (biome.is(ModTags.Biomes.netherBiomes)) return ModTags.Biomes.netherBiomes;

        // else: use DEFAULT_OTHER_BIOMES
        return null;
    }
}