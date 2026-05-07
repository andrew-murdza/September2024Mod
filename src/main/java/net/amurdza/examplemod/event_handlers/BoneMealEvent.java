package net.amurdza.examplemod.event_handlers;

import com.legacy.blue_skies.blocks.natural.SkyMushroomBlock;
import com.legacy.blue_skies.world.general_features.BaseLargeMushroomFeature;
import com.teamabnormals.upgrade_aquatic.core.registry.UABlocks;
import dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.TreeFromStructureNBTFeature;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.block.CharniaBlock;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.block.SeaPickleNew;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.util.RandomCollection;
import net.amurdza.examplemod.worldgen.feature.BouddhasHandFeature;
import net.amurdza.examplemod.worldgen.feature.ModConfiguredFeatures;
import net.amurdza.examplemod.worldgen.feature.RandomSelectionFeature;
import net.amurdza.examplemod.worldgen.feature.RandomSelectionFeatureConfig;
import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BoneMealEvent {

    //Need to Update to Use Netherific Plants Instead of NetherWart
    private static void growSoulSand(Level world, BlockPos pos) {
        RandomCollection<BlockState> states=new RandomCollection<>();
        states.add(1,NourishedNetherModBlocks.GHOULFLOWER.get().defaultBlockState());
        states.add(4,NourishedNetherModBlocks.SOUL_WEEDS.get().defaultBlockState());
        BiFunction<BlockPos, RandomSource, Boolean> func = (pos1,random) -> world.setBlockAndUpdate(pos1, states.next(random));
        placeBoneMeal(world, pos, blockState -> blockState.is(Blocks.SOUL_SAND)||blockState.is(Blocks.SOUL_SOIL), 10, func);
    }

//    static RandomCollection<BiFunction<Level, BlockPos, Boolean>> mossPlants = new RandomCollection<>();
//    static {
//        mossPlants.add(1, placeBlock(Blocks.RED_MUSHROOM));
//        mossPlants.add(1, placeBlock(Blocks.BROWN_MUSHROOM));
//        Block[] flowers = new Block[]{Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.BLUE_ORCHID,
//                Blocks.AZURE_BLUET, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.ORANGE_TULIP, Blocks.RED_TULIP,
//                Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM};
//        for(Block flower : flowers) {
//            mossPlants.add(1, placeBlock(flower));
//        }
//        mossPlants.add(100, placeBlock(Blocks.GRASS));
//        mossPlants.add(20, placeBlock(Blocks.FERN));
//        mossPlants.add(0.3, placeBlock(Blocks.DEAD_BUSH));
//        mossPlants.add(1, BoneMealEvent::createDripLeaf);
//        mossPlants.add(6, placeBlock(Blocks.AZALEA));
//        mossPlants.add(2, placeBlock(Blocks.FLOWERING_AZALEA));
//        mossPlants.add(20, placeDoubleBlock(Blocks.LARGE_FERN));
//        mossPlants.add(20, placeDoubleBlock(Blocks.TALL_GRASS));
//        mossPlants.add(1, placeDoubleBlock(Blocks.SUNFLOWER));
//        mossPlants.add(1, placeDoubleBlock(Blocks.LILAC));
//        mossPlants.add(1, placeDoubleBlock(Blocks.ROSE_BUSH));
//        mossPlants.add(1, placeDoubleBlock(Blocks.PEONY));
//    }

//    // Block placer methods
//    private static BiFunction<Level, BlockPos, Boolean> placeBlock(BlockState state) {
//        return (level, pos) -> {
//            boolean successful=Helper.isOkToPlace(level,pos,state);
//            if(successful&&!level.isClientSide){
//                level.setBlockAndUpdate(pos, state);
//            }
//            return successful;
//        };
//    }

//    private static BiFunction<Level, BlockPos, Boolean> placeDoubleBlock(Block block) {
//        return (level, pos) -> {
//            boolean successful=Helper.isOkToPlace(level,pos,block)&&Helper.isOkToPlace(level,pos.above(),block);
//            if(successful&&!level.isClientSide){
//                DoublePlantBlock.placeAt(level, block.defaultBlockState(), pos, 2);
//            }
//            return successful;
//        };
//    }

//    private static BiFunction<Level, BlockPos, Boolean> placeBlock(Block block) {
//        return placeBlock(block.defaultBlockState());
//    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level0 = event.getLevel();
        if (!(level0 instanceof ServerLevel level)) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BONE_MEAL)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Blacklist: you said cancel outright.
        if (Config.BLACKLISTED_USE_ITEMS.contains(block.asItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (block instanceof SaplingBlock) {
            return;
        }

        // If vanilla would not even consider this a bonemeal target and it's not one of your custom targets,
        // let vanilla handle it (we don't cancel).
        if (isNotEligibleBonemealTarget(level, pos, state)) return;



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

//            // Re-check continuation condition based on UPDATED state.
//            BlockState newState = level.getBlockState(pos);
//            if (isNotEligibleBonemealTarget(level, pos, newState)) break;
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
        boolean mushroom=block instanceof MushroomBlock || block instanceof FungusBlock || block instanceof SkyMushroomBlock;
        float best = -1f;
        for (var entry : Config.BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.entrySet()) {
            TagKey<Biome> tag = entry.getKey();
            float v = mushroom ? entry.getValue().mushroom():entry.getValue().plant();
            if (biomeHolder.is(tag)) {
                if (v > best) best = v;
            }
        }
        return best<0?1f:best;
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
    private static boolean isNotEligibleBonemealTarget(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (Config.BLACKLISTED_USE_ITEMS.contains(block.asItem())) return false;

        if (state.is(ModTags.Blocks.duplicatedByBonemeal)) return false;
        if (block == Blocks.MOSS_BLOCK) return false;
        if (block == Blocks.RED_SAND || block == Blocks.SAND) return false;
        if (block == Blocks.MYCELIUM) return false;
        if (block == Blocks.SOUL_SAND) return false;
        if (state.is(ModTags.Blocks.sugarCaneCactusLike)) return false;
        if (block == Blocks.NETHER_WART) return false;
        if (block == Blocks.ROOTED_DIRT) return false;
        if (block == Blocks.WET_SPONGE) return false;
        if (block == Blocks.SMALL_DRIPLEAF) return false;

        if (block == UABlocks.TALL_PICKERELWEED.get()) return false;
        if (block instanceof CharniaBlock) return false;

        if (state.is(ModTags.Blocks.netherFlowers)) return false;
        if (state.is(ModTags.Blocks.netherRootsPlaceable)) return false;
        if (block instanceof SeaPickleNew && state.getValue(SeaPickleNew.PICKLES) < 4 && state.getValue(BlockStateProperties.WATERLOGGED)) return false;

        if (
                ((block instanceof FlowerBlock
                        || block instanceof DoublePlantBlock
                        || block instanceof TallGrassBlock)
                        && !(block instanceof TallSeagrassBlock)
                        && !(block instanceof SmallDripleafBlock))
                        || Helper.isBlock(
                        block,
                        ModBlocks.DESERT_TALL_GRASS.get(),
                        Blocks.TALL_GRASS,
                        Blocks.LARGE_FERN,
                        Blocks.DEAD_BUSH
                )
        ) {
            return false;
        }

        if (block instanceof GrassBlock) return false;

        if (block instanceof BonemealableBlock b) {
            return !b.isValidBonemealTarget(level, pos, state, false);
        }

        return true;
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
            if (state.is(BlockTags.DIRT)||state.is(BlockTags.SAND)||state.is(BlockTags.NYLIUM)||state.is(Blocks.SAND)
                    ||state.is(Blocks.SOUL_SOIL)||state.is(Blocks.SOUL_SAND)||state.is(NourishedNetherModBlocks.SOUL_SLUDGE.get())) {
                Block.popResource(level, pos, new ItemStack(block));
            }
            growBiomeSurface(level, pos);
            return true;
        }

        if (block instanceof CharniaBlock) {
            BiFunction<BlockState, BlockPos, Boolean> func =
                    (state1, pos1) -> state1.isFaceSturdy(level, pos1, Direction.UP);

            growFlowers(level, pos, func);
            return true;
        }

        if (block == Blocks.WET_SPONGE) {
            growSponge(level, pos);
            return true;
        }

        if (block == Blocks.ROOTED_DIRT) {
            growRootedDirt(level, level.random, pos);
            return true;
        }

        if (block == UABlocks.TALL_PICKERELWEED.get() || block instanceof TallFlowerBlock) {
            return true;
        }

        if (block == Blocks.SMALL_DRIPLEAF) {
            growSmallDripLeaf(level, pos);
            return true;
        }

        if (state.is(ModTags.Blocks.netherFlowers)) {
            List<Block> blocks = new ArrayList<>(List.of(
                    Blocks.WARPED_NYLIUM,
                    Blocks.CRIMSON_NYLIUM
            ));

            if (block == Blocks.CRIMSON_ROOTS) {
                blocks.addAll(List.of(
                        Blocks.BLACKSTONE,
                        Blocks.BASALT,
                        Blocks.POLISHED_BASALT,
                        Blocks.SOUL_SOIL,
                        NourishedNetherModBlocks.SOUL_SLUDGE.get()
                ));
            }

            Function<BlockState, Boolean> func = state1 -> blocks.contains(state1.getBlock());
            growFlowers(level, pos, func);
            return true;
        }

        if (
                ((block instanceof FlowerBlock
                        || block instanceof DoublePlantBlock)
                        && !(block instanceof TallSeagrassBlock)
                        && !(block instanceof SmallDripleafBlock))
                        || Helper.isBlock(
                        block,
                        ModBlocks.DESERT_TALL_GRASS.get(),
                        Blocks.TALL_GRASS,
                        Blocks.LARGE_FERN,
                        Blocks.DEAD_BUSH
                )
        ) {
            growFlowers(level, pos);
            return true;
        }

        boolean waterAbove = isFluid(level, pos.above(), true);

        if (!waterAbove) {
            if (block == Blocks.RED_SAND || block == Blocks.SAND) {
                growSand(level, pos, block != Blocks.SAND);
                return true;
            }

            if (block == Blocks.MYCELIUM) {
                growMycelium(level, pos);
                return true;
            }

            if (block == Blocks.SOUL_SAND) {
                growSoulSand(level, pos);
                return true;
            }

            if (state.is(ModTags.Blocks.sugarCaneCactusLike)) {
                return growSugarcaneCactus(level, pos, block);
            }

            if (block == Blocks.NETHER_WART) {
                return growNetherWart(pos, level);
            }

            if (block instanceof GrassBlock grassBlock) {
                grassBlock.performBonemeal(level, level.random, pos, state);
                return true;
            }

            if (block instanceof BonemealableBlock b) {
                if (b.isValidBonemealTarget(level, pos, state, false)
                        && b.isBonemealSuccess(level, level.random, pos, state)) {
                    b.performBonemeal(level, level.random, pos, state);
                    return true;
                }
            }
        }
        if (block instanceof SeaPickleNew && state.getValue(SeaPickleNew.PICKLES) < 4) {
            level.setBlockAndUpdate(
                    pos,
                    state.setValue(SeaPickleNew.PICKLES, state.getValue(SeaPickleNew.PICKLES) + 1)
            );
            return true;
        }

        return false;
    }








    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, Function<BlockPos, Boolean> run) {
        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, (a,b)->run.apply(a), false, false);
    }
    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, BiFunction<BlockPos, RandomSource, Boolean> run) {
        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, run, false, false);
    }


    private static boolean isFluid(Level level, BlockPos pos, boolean isWater) {
        if(!isWater) {
            return level.isEmptyBlock(pos);
        }
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    private static void placeBoneMeal(Level world, BlockPos pos, BiFunction<BlockState, BlockPos, Boolean> check, int tries, BiFunction<BlockPos, RandomSource, Boolean> run, boolean twoBlocks, boolean isWater) {
        if(world instanceof ServerLevel&&!world.isClientSide) {
            BlockPos blockPos = pos.above();
            RandomSource random = world.random;
            label48:
            for(int i = 0; i < tries; ++i) {
                BlockPos blockPos2 = blockPos;
                for(int j = 0; j < i / 4; ++j) {
                    blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                    if(!check.apply(world.getBlockState(blockPos2.below()),
                            blockPos2.below()) || world.getBlockState(blockPos2).isFaceSturdy(world, blockPos2, Direction.UP)) {
                        continue label48;
                    }
                }
                if (isFluid(world, blockPos2, isWater)) {

                    int requiredDepth = twoBlocks? 2: 1;

                    BlockState originState = world.getBlockState(pos);
                    boolean deepWater = originState.is(ModTags.Blocks.needsDeepWater);

                    if(deepWater){
                        requiredDepth++;
                    }

                    boolean valid = true;

                    for (int k = 1; k < requiredDepth; k++) {
                        if (!isFluid(world, blockPos2.above(k), isWater)) {
                            valid = false;
                            break;
                        }
                    }

                    if (valid) {
                        run.apply(blockPos2, random);
                    }
                }
            }
        }
    }

    private static void placeBoneMeal(
            Level world,
            BlockPos pos,
            BiFunction<BlockState, BlockPos, Boolean> check,
            int tries,
            Function<BlockPos, Boolean> run,
            boolean twoBlocks,
            boolean isWater
    ){
        placeBoneMeal(
                world,
                pos,
                check,
                tries,
                (p,r) -> run.apply(p),
                twoBlocks,
                isWater
        );
    }

    //Cause block to increase in age
    private static boolean growNetherWart(BlockPos pos, Level world) {
        boolean flag = false;
        for(int i = 0; i < 2; i++) {
            if(world.getBlockState(pos).getValue(BlockStateProperties.AGE_3) < 3) {
                world.setBlockAndUpdate(pos, world.getBlockState(pos).cycle(BlockStateProperties.AGE_3));
                flag = true;
            }
        }
        return flag;
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

    //Specific growth methods to spread blocks
    private static void growSand(Level world, BlockPos pos, boolean red) {
        RandomCollection<BlockState> states=new RandomCollection<>();
        if(!red){
            states.add(3,ModBlocks.DESERT_GRASS.get().defaultBlockState());
        }
        states.add(1,Blocks.DEAD_BUSH.defaultBlockState());
        BiFunction<BlockPos, RandomSource, Boolean> func = (pos1,random) -> world.setBlockAndUpdate(pos1, states.next(random));
        placeBoneMeal(world, pos, blockState -> blockState.is(Blocks.SAND)||red&&blockState.is(Blocks.RED_SAND), 10, func);
    }



    private static void growMycelium(Level world, BlockPos pos) {
        BlockState brown = Blocks.BROWN_MUSHROOM.defaultBlockState();
        BlockState red = Blocks.RED_MUSHROOM.defaultBlockState();
        BlockState glow = GlimmeringWealdModule.glow_shroom.defaultBlockState();
        RandomSource random = world.random;
        BlockState state=Helper.select(random,red,brown,glow);
        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
        placeBoneMeal(world, pos, blockState -> blockState.is(Blocks.MYCELIUM), 10, func);
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
                if(world.isEmptyBlock(blockPos2)&&Helper.isOkToPlace(world,blockPos2,Blocks.HANGING_ROOTS)) {
                    world.setBlockAndUpdate(blockPos2, Blocks.HANGING_ROOTS.defaultBlockState());
                }
            }
        }
    }



    private static boolean createDripLeaf(Level level, BlockPos pos) {
        Direction direction = Helper.select(level,Helper.HORIZONTAL_DIRECTIONS);
        BlockState dripLeaf = Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        boolean successful=Helper.isOkToPlace(level,pos,dripLeaf)&&Helper.isOkToPlace(level,pos.above(),dripLeaf);
        if(successful&&!level.isClientSide){
            level.setBlockAndUpdate(pos, dripLeaf);
            level.setBlockAndUpdate(pos.above(), dripLeaf.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        }
        return successful;
    }

    private static void growFlowers(Level world, BlockPos pos, BiFunction<BlockState, BlockPos, Boolean> check) {
        BlockState state = world.getBlockState(pos);

        boolean isWater = state.getFluidState().is(FluidTags.WATER);

        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);

        boolean b = state.getBlock() instanceof DoublePlantBlock;
        if (b) {
            BlockPos pos2 = pos.below();
            BlockState state1 = world.getBlockState(pos2);

            if (state1.isFaceSturdy(world, pos2, Direction.UP)) {
                pos = pos.below();
            }

            func = (pos1) -> {
                DoublePlantBlock.placeAt(world, state, pos1, 2);
                return true;
            };
        }

        placeBoneMeal(world, pos.below(), check, 10, func, b, isWater);
    }


    private static void growFlowers(Level world, BlockPos pos, Function<BlockState, Boolean> check) {
        BlockState state = world.getBlockState(pos);

        boolean isWater = state.getFluidState().is(FluidTags.WATER);

        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);

        boolean b = state.getBlock() instanceof DoublePlantBlock;
        if (b) {
            BlockPos pos2 = pos.below();
            BlockState state1 = world.getBlockState(pos2);

            if (state1.isFaceSturdy(world, pos2, Direction.UP)) {
                pos = pos.below();
            }

            func = (pos1) -> {
                DoublePlantBlock.placeAt(world, state, pos1, 2);
                return true;
            };
        }

        placeBoneMeal(world, pos.below(), check, 10, func, b, isWater);
    }

    private static void growFlowers(Level world, BlockPos pos) {
        TagKey<Block> goalTag = world.getBlockState(pos).is(ModBlocks.DESERT_TALL_GRASS.get()) ? BlockTags.SAND : BlockTags.DIRT;
        growFlowers(world, pos, blockState -> blockState.is(goalTag));
    }

    private static void growSmallDripLeaf(ServerLevel level, BlockPos pos) {
        Function<BlockState,Boolean> check=state ->
                state.is(Blocks.CLAY)||state.is(BlockTags.DIRT);
        placeBoneMeal(level,pos,check, 10, pos1->createDripLeaf(level,pos1));
    }

    private static void growSponge(Level world, BlockPos pos) {
        BlockState state = Blocks.WET_SPONGE.defaultBlockState();
        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
        placeBoneMeal(world, pos.below(), (state1, pos1) -> state1.isFaceSturdy(world, pos1, Direction.UP), 10, func, false, true);
    }

    private record BiomeBonemealFeatures(
            ResourceKey<ConfiguredFeature<?, ?>> floor,
            ResourceKey<ConfiguredFeature<?, ?>> flowers,
            ResourceKey<ConfiguredFeature<?, ?>> fullLiquid,
            ResourceKey<ConfiguredFeature<?, ?>> shallowLiquid,
            boolean lava
    ) {}

    private static boolean isBiomeSurfaceBonemealTarget(ServerLevel level, BlockPos pos, BlockState state) {
        Holder<Biome> biome = level.getBiome(pos);

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return state.is(Blocks.MOSS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)
                || biome.is(ModTags.Biomes.plainsBiomes)
                || biome.is(ModTags.Biomes.mountainBiomes)
                || biome.is(ModTags.Biomes.desertBiomes)) {
            return state.is(Blocks.GRASS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return state.is(Blocks.CRIMSON_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return state.is(Blocks.WARPED_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return state.is(Blocks.MYCELIUM);
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return state.is(Blocks.BASALT)
                    || state.is(Blocks.POLISHED_BASALT)
                    || state.is(Blocks.BLACKSTONE);
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

                if (random.nextInt(8) == 0 && features.flowers() != null) {
                    placeSingleFlowerFromPatch(level, chunkGenerator, random, target, features.flowers());
                } else {
                    placeConfiguredFeatureSkippingBadTypes(level, chunkGenerator, random, target, features.floor(), 16);
                }

                continue;
            } else if (isCorrectLiquid(replaceState, features.lava())) {
                if (!level.getBlockState(groundPos).isFaceSturdy(level, groundPos, Direction.UP)) continue;

                boolean liquidAbove = isCorrectLiquid(aboveState, features.lava());
                featureKey = liquidAbove ? features.fullLiquid() : features.shallowLiquid();
            } else {
                continue;
            }

            placeConfiguredFeatureSkippingBadTypes(level, chunkGenerator, random, target, featureKey, 16);
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

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return ground.is(Blocks.MOSS_BLOCK);
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)
                || biome.is(ModTags.Biomes.plainsBiomes)
                || biome.is(ModTags.Biomes.mountainBiomes)
                || biome.is(ModTags.Biomes.desertBiomes)) {
            return ground.is(Blocks.SAND);
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return ground.is(Blocks.CRIMSON_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return ground.is(Blocks.WARPED_NYLIUM);
        }

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return ground.is(Blocks.MYCELIUM);
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

        if (biome.is(ModTags.Biomes.tropicalBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.RAINFOREST_FLOOR,
                    null,
                    ModConfiguredFeatures.RAINFOREST_SEAFLOOR,
                    ModConfiguredFeatures.RAINFOREST_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.savannaBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.SAVANNA_FLOOR,
                    ModConfiguredFeatures.SAVANNA_FLOWERS,
                    ModConfiguredFeatures.SAVANNA_SEAFLOOR,
                    ModConfiguredFeatures.SAVANNA_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.plainsBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.PLAINS_FLOOR,
                    ModConfiguredFeatures.PLAINS_FLOWERS,
                    ModConfiguredFeatures.PLAINS_SEAFLOOR,
                    ModConfiguredFeatures.PLAINS_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.mountainBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.MOUNTAINS_FLOOR,
                    ModConfiguredFeatures.MOUNTAINS_FLOWERS,
                    ModConfiguredFeatures.MOUNTAINS_SEAFLOOR,
                    ModConfiguredFeatures.MOUNTAINS_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.desertBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.DESERT_FLOOR,
                    null,
                    ModConfiguredFeatures.DESERT_SEAFLOOR,
                    ModConfiguredFeatures.DESERT_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.crimsonForestBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.CRIMSON_FOREST_FLOOR,
                    null,
                    ModConfiguredFeatures.CRIMSON_FOREST_SEAFLOOR,
                    ModConfiguredFeatures.CRIMSON_FOREST_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.warpedForestBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.WARPED_FOREST_FLOOR,
                    null,
                    ModConfiguredFeatures.WARPED_FOREST_SEAFLOOR,
                    ModConfiguredFeatures.WARPED_FOREST_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.mushroomCaves)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.MUSHROOM_CAVES_FLOOR,
                    null,
                    ModConfiguredFeatures.MUSHROOM_CAVES_SEAFLOOR,
                    ModConfiguredFeatures.MUSHROOM_CAVES_SEAFLOOR_SHALLOW,
                    false
            );
        }

        if (biome.is(ModTags.Biomes.soulSandValleyBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_FLOOR,
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_FLOWERS,
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_SEAFLOOR,
                    ModConfiguredFeatures.SOUL_SAND_VALLEY_SEAFLOOR_SHALLOW,
                    true
            );
        }

        if (biome.is(ModTags.Biomes.basaltDeltasBiomes)) {
            return new BiomeBonemealFeatures(
                    ModConfiguredFeatures.BASALT_DELTAS_FLOOR,
                    null,
                    ModConfiguredFeatures.BASALT_DELTAS_SEAFLOOR,
                    ModConfiguredFeatures.BASALT_DELTAS_SEAFLOOR_SHALLOW,
                    true
            );
        }

        return null;
    }


    private static void placeConfiguredFeatureSkippingBadTypes(
            ServerLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos pos,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            int maxRerolls
    ) {
        ConfiguredFeature<?, ?> configured = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(key);

        if (configured.feature() instanceof RandomSelectionFeature randomSelectionFeature
                && configured.config() instanceof RandomSelectionFeatureConfig config) {

            FeaturePlaceContext<RandomSelectionFeatureConfig> context =
                    new FeaturePlaceContext<>(
                            Optional.empty(),
                            level,
                            chunkGenerator,
                            random,
                            pos,
                            config
                    );

            randomSelectionFeature.placeSkippingFeatures(
                    context,
                    BoneMealEvent::isBadBonemealFeature,
                    maxRerolls
            );
            return;
        }

        if (isBadBonemealFeature(configured.feature())) {
            return;
        }

        configured.place(level, chunkGenerator, random, pos);
    }

    private static boolean isBadBonemealFeature(Feature<?> feature) {
        return feature instanceof NoOpFeature
                || feature instanceof HugeFungusFeature
                || feature instanceof AbstractHugeMushroomFeature
                || feature instanceof BaseLargeMushroomFeature
                || feature instanceof TreeFromStructureNBTFeature
                || feature instanceof BouddhasHandFeature;
    }

    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, Function<BlockPos, Boolean> run, boolean twoBlocks, boolean isWater) {
        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, (a, b) -> run.apply(a), twoBlocks, isWater);
    }

    private static boolean placeSingleFlowerFromPatch(
            ServerLevel level,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BlockPos pos,
            ResourceKey<ConfiguredFeature<?, ?>> flowerPatchKey
    ) {
        ConfiguredFeature<?, ?> configured = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(flowerPatchKey);

        if (configured.feature() instanceof RandomSelectionFeature
                && configured.config() instanceof RandomSelectionFeatureConfig config) {

            RandomCollection<PlacedFeature> choices = new RandomCollection<>();

            for (WeightedPlacedFeature weighted : config.features) {
                PlacedFeature placed = weighted.feature.value();

                PlacedFeature singleFlower = placed.getFeatures()
                        .filter(cf -> cf.config() instanceof RandomPatchConfiguration)
                        .map(cf -> ((RandomPatchConfiguration) cf.config()).feature().value())
                        .findFirst()
                        .orElse(null);

                if (singleFlower != null) {
                    choices.add(weighted.chance, singleFlower);
                }
            }

            PlacedFeature chosen = choices.next(random);
            return chosen.place(level, chunkGenerator, random, pos);
        }

        if (configured.config() instanceof RandomPatchConfiguration patch) {
            return patch.feature().value().place(level, chunkGenerator, random, pos);
        }

        return false;
    }
}
