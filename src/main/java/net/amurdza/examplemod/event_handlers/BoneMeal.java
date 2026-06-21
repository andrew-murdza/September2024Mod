package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.config.BoneMealConfig;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.registry.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BoneMeal {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level0 = event.getLevel();
        if (!(level0 instanceof ServerLevel level)) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BONE_MEAL)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        Holder<Biome> biome = level.getBiome(pos);

        // This is just for mushrooms in plains/savanna/desert
        if (state.is(Blocks.RED_MUSHROOM)||state.is(Blocks.BROWN_MUSHROOM)) {
            if(!biome.is(ModTags.Biomes.tropicalBiomes)&&!biome.is(ModTags.Biomes.mushroomCaves)
                    &&!biome.is(ModTags.Biomes.mountainBiomes)){
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
        }

        // If vanilla would not even consider this a bonemeal target and it's not one of your custom targets,
        // let vanilla handle it (we don't cancel).
        if (!isEligibleBonemealTarget(level, pos, state)) return;



        // We will handle everything ourselves, but we still want the ONE sound + ONE particle burst.
        // Cancel vanilla bonemeal behavior so we don't double-trigger or consume twice.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        // Consume exactly 1 bonemeal (even if our multiplier says "fail to work"),
        // since the player "used" it and you explicitly want effects even on failure.
        if (event.getEntity() != null && !event.getEntity().getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play the bone meal sound + particles ONCE.
        playBonemealEffectsOnce(level, pos, event);

        // Compute number of "attempts" this click produces, based on biome multiplier.
        float mult = getBiomeMultiplier(level, pos);

        int attempts = computeAttempts(mult, level.random);
        if (attempts <= 0) {
            // "Fail" case: effects already played; nothing else happens.
            return;
        }

        // Apply bonemeal logic up to 'attempts' times, but only keep going while:
        // - vanilla says isValidBonemealTarget is true, OR
        // - it matches one of your custom if-branches that should keep allowing repeats
        //   (duplicatedByBonemeal / moss / sand / mycelium / soul sand / sugarcaneCactusLike / nether wart / tall flowers).
        for (int i = 0; i < attempts; i++) {

            // If we did nothing this iteration, stop early.
            if (!applyOnce(level,pos)) break;
        }
    }

// ----------------------------
    // Attempts logic (your rules)
    // ----------------------------

    /**
     * Rules:
     * - if mult < 1: there is (1 - mult) chance of "no work" (attempts=0), else attempts=1
     * - if mult > 1: base = 1 + floor(mult - 1) = floor(mult)
     *               then +1 more with probability frac(mult)
     * - if mult == 1: attempts=1
     */
    private static int computeAttempts(float mult, RandomSource rand) {
        if (mult <= 0.0f) return 0;

        if (mult < 1.0f) {
            return (rand.nextFloat() < mult) ? 1 : 0;
        }

        int base = Mth.floor(mult); // works for 1.0 => 1, 2.3 => 2, etc.
        float frac = mult - base;
        if (frac > 0.0f && rand.nextFloat() < frac) base += 1;
        return base;
    }

    // ----------------------------
    // Biome multiplier lookup
    // ----------------------------

    /**
     * Gets the configured multiplier by checking which biome tags match at (pos).
     * If multiple tags match, this returns the MAX value among matches (usually what you want for "best match wins").
     * If none match, returns 1.0f.
     */
    private static float getBiomeMultiplier(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Block block=level.getBlockState(pos).getBlock();
        boolean mushroom=block instanceof MushroomBlock
                || block == Blocks.MYCELIUM || block== GlimmeringWealdModule.glow_shroom;
        float best = -1f;
        for (var entry : BoneMealConfig.BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.entrySet()) {
            TagKey<Biome> tag = entry.getKey();
            float v = mushroom ? entry.getValue().mushroom():entry.getValue().plant();
            if (biomeHolder.is(tag)) {
                if (v > best) best = v;
            }
        }
        return best;
    }

    // ----------------------------
    // Effects once
    // ----------------------------

    private static void playBonemealEffectsOnce(ServerLevel level, BlockPos pos, PlayerInteractEvent.RightClickBlock event) {
        // Particle burst (same event id vanilla uses for bone meal particles)
        level.levelEvent(1505, pos, 0);

        // Sound once
        level.playSound(
                null,
                pos,
                SoundEvents.BONE_MEAL_USE,
                SoundSource.BLOCKS,
                1.0f,
                1.0f
        );

        // Optional: game event (helps sculk sensors etc.)
        if (event.getEntity() != null) {
            level.gameEvent(event.getEntity(), GameEvent.ITEM_INTERACT_FINISH, pos);
        }
    }
    // ----------------------------
    // Eligibility / repetition rules
    // ----------------------------

    /**
     * We only intercept bonemeal if:
     * - vanilla would treat it as bonemealable, OR
     * - it's one of your explicit special cases.
     */
    private static boolean isEligibleBonemealTarget(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if(block instanceof BonemealableBlock block1){
            return block1.isValidBonemealTarget(level,pos,state,false);
        }

        if (state.is(ModTags.Blocks.duplicatedByBonemeal)) return true;

        //Floors
        if(Helper.isBlock(
                state, Blocks.RED_SAND, Blocks.SAND, Blocks.MYCELIUM, Blocks.SCULK)){
            return true;
        }
        if(Helper.isBlockTag(state, BlockTags.NYLIUM, ModTags.Blocks.soulSediments, ModTags.Blocks.basaltStones)){
            return true;
        }

        //Crops
        if(Helper.isBlock(
                state, Blocks.CACTUS, Blocks.SUGAR_CANE)){
            return true;
        }
        if(state.is(Blocks.NETHER_WART)&&state.getValue(BlockStateProperties.AGE_3)<3){
            return true;
        }

        return isFlowerLike(block);
    }

    private static boolean isFlowerLike(Block block){
        return block instanceof FlowerBlock || block instanceof DoublePlantBlock
                || Helper.isBlock(
                block,
                ModBlocks.DESERT_TALL_GRASS.get(),
                Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS, Blocks.NETHER_SPROUTS
        );
    }

    // ----------------------------
    // Apply once (your existing logic + vanilla fallback)
    // ----------------------------

    private static boolean applyOnce(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (state.is(ModTags.Blocks.duplicatedByBonemeal)) {
            Block.popResource(level, pos, new ItemStack(block));
            return true;
        }

        if (isBiomeSurfaceBonemealTarget(level, pos, state)) {
//            if (isCorrectLandGroundForBiome(level,pos)) {
//                Block.popResource(level, pos, new ItemStack(block));
//            }
            growBiomeSurface(level, pos);
            return true;
        }

        if (block == Blocks.ROOTED_DIRT) {
            growRootedDirt(level, level.random, pos);
            return true;
        }

        if (block == Blocks.SMALL_DRIPLEAF) {
            growSmallDripLeaf(level, pos);
            return true;
        }

        if (isFlowerLike(block)) {
            growFlowers(level, pos);
            return true;
        }

        if (state.is(ModTags.Blocks.sugarCaneCactusLike)) {
            return growSugarcaneCactus(level, pos, block);
        }

        if (block == Blocks.NETHER_WART) {
            return growNetherWart(pos, level);
        }

        if (block instanceof SeaPickleBlock && state.getValue(SeaPickleBlock.PICKLES) < 4) {
            level.setBlockAndUpdate(
                    pos,
                    state.setValue(SeaPickleBlock.PICKLES, state.getValue(SeaPickleBlock.PICKLES) + 1)
            );
            return true;
        }

        if (block instanceof BonemealableBlock b) {
            if (b.isValidBonemealTarget(level, pos, state, false)) {
                if(b.isBonemealSuccess(level, level.random, pos, state)){
                    b.performBonemeal(level, level.random, pos, state);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isFluid(Level level, BlockPos pos, FluidType fluidType) {
        BlockState state = level.getBlockState(pos);
        switch (fluidType){
            case AIR -> {
                return state.isAir();
            }
            case WATER -> {
                return state.is(Blocks.WATER);
            }
            case LAVA -> {
                return state.is(Blocks.LAVA);
            }
            case AIR_OR_WATER -> {
                return state.isAir() || state.is(Blocks.WATER);
            }
        }
        return false;
    }

    private enum FluidType {
        WATER,
        AIR_OR_WATER,
        AIR,
        LAVA
    }

    private static void placeBoneMeal(Level world, BlockPos pos, int tries, Function<BlockPos, Boolean> run, FluidType fluidType, boolean isDouble) {
        if(world instanceof ServerLevel&&!world.isClientSide) {
            BlockPos blockPos = pos.above();
            RandomSource random = world.random;
            label48:
            for(int i = 0; i < tries; ++i) {
                BlockPos blockPos2 = blockPos;
                for(int j = 0; j < i / 4; ++j) {
                    blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                    if(world.getBlockState(blockPos2).isFaceSturdy(world, blockPos2, Direction.UP)) {
                        continue label48;
                    }
                }
                if (isFluid(world, blockPos2, fluidType)&&(!isDouble||isFluid(world,blockPos2.above(),fluidType))) {
                    run.apply(blockPos2);
                }
            }
        }
    }

    //Cause block to increase in age
    private static boolean growNetherWart(BlockPos pos, Level world) {
        if(world.getBlockState(pos).getValue(BlockStateProperties.AGE_3) < 3) {
            world.setBlockAndUpdate(pos, world.getBlockState(pos).cycle(BlockStateProperties.AGE_3));
            return true;
        }
        return false;
    }

    private static boolean growSugarcaneCactus(Level world, BlockPos pos, Block block) {
        if(world.getBlockState(pos.below()).is(block)) {
            if(world.getBlockState(pos.below().below()).is(block)) {
                return false;
            } else {
                return growSugarcaneCactus1(world, pos.above(), block);
            }
        } else {
            boolean flag = growSugarcaneCactus1(world, pos.above(), block);
            flag = growSugarcaneCactus1(world, pos.above().above(), block) || flag;
            return flag;
        }
    }

    private static boolean growSugarcaneCactus1(Level world, BlockPos pos, Block block) {
        if(world.isEmptyBlock(pos)) {
            world.setBlockAndUpdate(pos, block.defaultBlockState());
            BlockState blockState = block.defaultBlockState();// Not needed: .setValue(AGE, 0);
            world.setBlockAndUpdate(pos, blockState);
            //blockState.updateNeighbourShapes().neighborUpdate(world, pos, block, pos, false);
            return true;
        }
        return false;
    }

    private static void growRootedDirt(Level world, RandomSource random, BlockPos pos) {
        if(world instanceof ServerLevel&&!world.isClientSide) {
            BlockPos blockPos = pos.below();
            label48:
            for(int i = 0; i < 20; ++i) {
                BlockPos blockPos2 = blockPos;
                for(int j = 0; j < i / 4; ++j) {
                    blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                    if(!world.getBlockState(blockPos2.above()).is(Blocks.ROOTED_DIRT) || world.getBlockState(blockPos2).isFaceSturdy(world, blockPos2, Direction.UP)) {//UP Might be a problem
                        continue label48;
                    }
                }
                if(world.isEmptyBlock(blockPos2)) {
                    world.setBlockAndUpdate(blockPos2, Blocks.HANGING_ROOTS.defaultBlockState());
                }
            }
        }
    }



    private static boolean createDripLeaf(Level level, BlockPos pos) {
        Direction direction = Helper.select(level,Helper.HORIZONTAL_DIRECTIONS);
        BlockState dripLeaf = Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        if(!level.isClientSide){
            level.setBlockAndUpdate(pos, dripLeaf);
            level.setBlockAndUpdate(pos.above(), dripLeaf.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
        return true;
    }

    private static void growFlowers(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        FluidType fluidType = FluidType.AIR;
        FluidState fluidState = state.getFluidState();
        if(fluidState.is(FluidTags.WATER)){
            fluidType = FluidType.WATER;
        }
        else if(fluidState.is(FluidTags.LAVA)){
            fluidType = FluidType.LAVA;
        }

        Function<BlockPos, Boolean> func = pos1 -> world.setBlockAndUpdate(pos1, state);

        boolean doublePlantBlock = state.getBlock() instanceof DoublePlantBlock;
        if (doublePlantBlock) {
            if(state.getValue(BlockStateProperties.HALF)== Half.TOP){
                pos = pos.below();
            }
            func = pos1 -> {
                DoublePlantBlock.placeAt(world, state, pos1, 2);
                return true;
            };
        }

        placeBoneMeal(world, pos.below(), 20, func, fluidType, doublePlantBlock);
    }

    private static void growSmallDripLeaf(ServerLevel level, BlockPos pos) {
        placeBoneMeal(level,pos, 10, pos1->createDripLeaf(level,pos1),FluidType.AIR_OR_WATER,false);
    }

    private record BiomeBonemealFeatures(
            ResourceKey<ConfiguredFeature<?, ?>> floor,
            ResourceKey<ConfiguredFeature<?, ?>> fullLiquid,
            ResourceKey<ConfiguredFeature<?, ?>> shallowLiquid,
            boolean lava
    ) {}

    private static boolean isBiomeSurfaceBonemealTarget(ServerLevel level, BlockPos pos, BlockState state) {
        Holder<Biome> biome = level.getBiome(pos);

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return state.is(Blocks.MYCELIUM);
        }

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return state.is(Blocks.MOSS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)
                || biome.is(ModTags.Biomes.plainsBiomes)
                || biome.is(ModTags.Biomes.mountainBiomes)
                || biome.is(ModTags.Biomes.desertBiomes)) {
            return state.is(Blocks.GRASS_BLOCK);
        }

        if(biome.is(ModTags.Biomes.deepDarkBiomes)){
            return state.is(Blocks.SCULK);
        }

        if(biome.is(ModTags.Biomes.desertBiomes)){
            return state.is(Blocks.RED_SAND)||state.is(Blocks.SAND);
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return state.is(Blocks.CRIMSON_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return state.is(Blocks.WARPED_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return state.is(ModTags.Blocks.soulSediments);
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return state.is(ModTags.Blocks.basaltStones);
        }

        return false;
    }

    private static void growBiomeSurface(ServerLevel level, BlockPos sourcePos) {
        ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        RandomSource random = level.random;
        int tries = 128;

        BlockPos start = sourcePos.above();

        label48:
        for (int i = 0; i < tries; ++i) {
            BlockPos target = start;

            for (int j = 0; j < i / 4; ++j) {
                target = target.offset(
                        random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1
                );

                if (cannotTryBiomeSurfacePlacement(level, target)) {
                    continue label48;
                }
            }

            if (cannotTryBiomeSurfacePlacement(level, target)) continue;

            BiomeBonemealFeatures features = biomeBonemealFeaturesAt(level, target);
            if (features == null) continue;

            BlockState replaceState = level.getBlockState(target);
            BlockState aboveState = level.getBlockState(target.above());
            BlockPos groundPos = target.below();

            ResourceKey<ConfiguredFeature<?, ?>> featureKey;

            if (replaceState.isAir()) {
                if (!isCorrectLandGroundForBiome(level, groundPos)) continue;
                featureKey = features.floor();
            } else if (isCorrectLiquid(replaceState, features.lava())) {
                if (!level.getBlockState(groundPos).isFaceSturdy(level, groundPos, Direction.UP)) continue;

                boolean liquidAbove = isCorrectLiquid(aboveState, features.lava());
                featureKey = liquidAbove ? features.fullLiquid() : features.shallowLiquid();
            } else {
                continue;
            }

            ConfiguredFeature<?, ?> configured = level.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getOrThrow(featureKey);

            configured.place(level,chunkGenerator,random,target);
        }
    }

    private static boolean cannotTryBiomeSurfacePlacement(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);

        if (!(state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA))) {
            return true;
        }

        return state.isFaceSturdy(level, target, Direction.UP);
    }

    private static boolean isCorrectLiquid(BlockState state, boolean lava) {
        return lava ? state.is(Blocks.LAVA) : state.is(Blocks.WATER);
    }

    private static boolean isCorrectLandGroundForBiome(ServerLevel level, BlockPos groundPos) {
        BlockState ground = level.getBlockState(groundPos);
        Holder<Biome> biome = level.getBiome(groundPos);

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return ground.is(Blocks.MYCELIUM);
        }

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return ground.is(Blocks.MOSS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)
                || biome.is(ModTags.Biomes.plainsBiomes)
                || biome.is(ModTags.Biomes.mountainBiomes)) {
            return ground.is(Blocks.GRASS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.deepDarkBiomes)) {
            return ground.is(Blocks.SCULK);
        }

        if (biome.is(ModTags.Biomes.desertBiomes)) {
            return ground.is(Blocks.SAND);
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return ground.is(Blocks.CRIMSON_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return ground.is(Blocks.WARPED_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL);
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return ground.is(Blocks.BASALT)
                    || ground.is(Blocks.POLISHED_BASALT)
                    || ground.is(Blocks.BLACKSTONE);
        }

        return false;
    }

    private static BiomeBonemealFeatures biomeBonemealFeaturesAt(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.MUSHROOM_CAVES_FLOOR,
                    ModConfiguredFeatures.MUSHROOM_CAVES_SEAFLOOR,
                    ModConfiguredFeatures.MUSHROOM_CAVES_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.RAINFOREST_FLOOR,
                    ModConfiguredFeatures.RAINFOREST_SEAFLOOR,
                    ModConfiguredFeatures.RAINFOREST_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.SAVANNA_FLOOR,
                    ModConfiguredFeatures.SAVANNA_SEAFLOOR,
                    ModConfiguredFeatures.SAVANNA_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.plainsBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.PLAINS_FLOOR,
                    ModConfiguredFeatures.PLAINS_SEAFLOOR,
                    ModConfiguredFeatures.PLAINS_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.mountainBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.MOUNTAINS_FLOOR,
                    ModConfiguredFeatures.MOUNTAINS_SEAFLOOR,
                    ModConfiguredFeatures.MOUNTAINS_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.deepDarkBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.DEEP_DARK_FLOOR,
                    ModConfiguredFeatures.DEEP_DARK_SEAFLOOR,
                    ModConfiguredFeatures.DEEP_DARK_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.desertBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.DESERT_FLOOR,
                    ModConfiguredFeatures.DESERT_SEAFLOOR,
                    ModConfiguredFeatures.DESERT_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.CRIMSON_FOREST_FLOOR,
                    ModConfiguredFeatures.CRIMSON_FOREST_SEAFLOOR,
                    ModConfiguredFeatures.CRIMSON_FOREST_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.WARPED_FOREST_FLOOR,
                    ModConfiguredFeatures.WARPED_FOREST_SEAFLOOR,
                    ModConfiguredFeatures.WARPED_FOREST_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_FLOOR,
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_SEAFLOOR,
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.BASALT_DELTAS_FLOOR,
                    ModConfiguredFeatures.BASALT_DELTAS_SEAFLOOR,
                    ModConfiguredFeatures.BASALT_DELTAS_SEAFLOOR_SHALLOW,
                    true
            );
        }

        return null;
    }
}
