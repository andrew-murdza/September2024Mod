package net.amurdza.examplemod.event_handlers;

import com.legacy.blue_skies.blocks.natural.SkyMushroomBlock;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.util.RandomCollection;
import net.amurdza.examplemod.worldgen.feature.ModConfiguredFeatures;
import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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



    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level0 = event.getLevel();
        if (!(level0 instanceof ServerLevel level)) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BONE_MEAL)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // If vanilla would not even consider this a bonemeal target and it's not one of your custom targets,
        // let vanilla handle it (we don't cancel).
        if (isNotEligibleBonemealTarget(level, pos, state)) return;

        // Blacklist: you said cancel outright.
        if (Config.BLACKLISTED_USE_ITEMS.contains(block.asItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

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

            // Re-check continuation condition based on UPDATED state.
            BlockState newState = level.getBlockState(pos);
            if (isNotEligibleBonemealTarget(level, pos, newState)) break;
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

        // Your explicit "specials" that should be eligible even if not BonemealableBlock.
        if (state.is(ModTags.Blocks.duplicatedByBonemeal)) return false;
        if (block == Blocks.MOSS_BLOCK) return false;
        if (block == Blocks.RED_SAND || block == Blocks.SAND) return false;
        if (block == Blocks.MYCELIUM) return false;
        if (block == Blocks.SOUL_SAND) return false;
        if (state.is(ModTags.Blocks.sugarCaneCactusLike)) return false;
        if (block == Blocks.NETHER_WART) return false;
        // If you have Upgrade Aquatic etc:
        if (isYourTallCancelCase(block)) return false;

        // Vanilla bonemeal target check:
        if (block instanceof BonemealableBlock b) {
            // isClientSide=false because we are server-side here
            return !b.isValidBonemealTarget(level, pos, state, false);
        }

        return true;
    }

    private static boolean isYourTallCancelCase(Block block) {
        // Replace UABlocks.TALL_PICKERELWEED.get() with your actual reference if needed.
        // Example:
        // return block == UABlocks.TALL_PICKERELWEED.get() || block instanceof TallFlowerBlock;

        return block instanceof TallFlowerBlock;
    }

    // ----------------------------
    // Apply once (your existing logic + vanilla fallback)
    // ----------------------------

    private static boolean applyOnce(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1) duplicateByBonemeal: just drop an item, do not change the block
        if (state.is(ModTags.Blocks.duplicatedByBonemeal)) {
            Block.popResource(level, pos, new ItemStack(block));
            return true;
        }

        // 2) Moss special
        if (block == Blocks.MOSS_BLOCK) {
            growMoss(level, pos);
            return true;
        }

        // 3) If fluid above is NOT allowed (matches your original guard)
        if (!isFluid(level, pos.above(), true)) {

            if (block == Blocks.RED_SAND || block == Blocks.SAND) {
                growSand(level, pos);
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

            // 4) Vanilla fallback: if it is bonemealable, try to bonemeal it once
            if (block instanceof BonemealableBlock b) {
                // Mirror vanilla: must be valid + success roll, then perform
                if (b.isValidBonemealTarget(level, pos, state, false)
                        && b.isBonemealSuccess(level, level.random, pos, state)) {
                    b.performBonemeal(level, level.random, pos, state);
                    return true;
                }
            }
        }

        return false;
    }








    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, Function<BlockPos, Boolean> run) {
        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, (a,b)->run.apply(a), false, false);
    }
    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, BiFunction<BlockPos, RandomSource, Boolean> run) {
        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, run, false, false);
    }

//
//    private static void placeBoneMeal(Level world, BlockPos pos, Function<BlockState, Boolean> check, int tries, Function<BlockPos, Boolean> run, boolean twoBlocks) {
//        placeBoneMeal(world, pos, (state1, pos1) -> check.apply(state1), tries, (a,b)->run.apply(a), twoBlocks, false);
//    }


    private static boolean isFluid(Level level, BlockPos pos, boolean isWater) {
        if(!isWater) {
            return level.isEmptyBlock(pos);
        }
        return level.getBlockState(pos).is(Blocks.WATER);
    }
    private static void growMoss(Level level, BlockPos pos){
        ConfiguredFeature<?,?> feature=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.RAINFOREST_FLOOR_KEY);
        ConfiguredFeature<?,?> featureWater=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.RAINFOREST_FLOOR_KEY);
        ServerLevel level1=(ServerLevel) level;
        ChunkGenerator chunkGenerator=(level1).getChunkSource().getGenerator();
        int tries=128;
        Predicate<BlockPos> tester= pos1->level.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK)&&
                isFluid(level,pos1.above(),true)&&isFluid(level,pos1,true)||
                isFluid(level,pos1.above(),false)&&isFluid(level,pos1,false);
        if(level instanceof ServerLevel&&!level.isClientSide) {
            BlockPos blockPos = pos.above();
            RandomSource random = level.random;
            label48:
            for(int i = 0; i < tries; ++i) {
                BlockPos blockPos2 = blockPos;
                for(int j = 0; j < i / 4; ++j) {
                    blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                    if(!tester.test(blockPos2)) {
                        continue label48;
                    }
                }
                if(tester.test(blockPos2)) {
                    (level.getBlockState(blockPos2).is(Blocks.WATER)?featureWater:feature).place(level1,
                            chunkGenerator,level.random,blockPos2);
                }
            }
        }
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
                if(isFluid(world, blockPos2, isWater) && (!twoBlocks || isFluid(world, blockPos2.above(), isWater))) {
                    run.apply(blockPos2,random);
                }
            }
        }
    }

//    @SubscribeEvent
//    public static void boneMealEvent(BonemealEvent event) {
//        if(event.getLevel() instanceof ServerLevel level){
//            BlockPos pos=event.getPos();
//            BlockState state = level.getBlockState(pos);
//            Block block = state.getBlock();
//            boolean flag = true;
//            if(Config.BLACKLISTED_USE_ITEMS.contains(block.asItem())){
//                event.setCanceled(true);
//            }
//            else if(state.is(ModTags.Blocks.duplicatedByBonemeal)) {
//                Block.popResource(level, pos, new ItemStack(block));
//            }
//            else if(block==Blocks.MOSS_BLOCK){
//                growMoss(level,pos);
//                event.setCanceled(true);
//            }
//            else if(!isFluid(level,pos.above(),true)){
//                if(block==Blocks.RED_SAND||block==Blocks.SAND){//Assumes dead bushes can always grow on sand regardless of biome
//                    growSand(level,pos);
//                }
//                else if(block==Blocks.MYCELIUM){
//                    growMycelium(level,pos);
//                }
//                else if(block==Blocks.SOUL_SAND){
//                    growSoulSand(level,pos);
//                }
//                else if(state.is(ModTags.Blocks.sugarCaneCactusLike)){
//                    flag = growSugarcaneCactus(level, pos, block);
//                }
//                else if(block==Blocks.NETHER_WART){
//                    flag =growNetherWart(pos,level);
//                }
////                else if(block==Blocks.ROOTED_DIRT){
////                    growRootedDirt(level,level.random,pos);
////                }
//                else if(block== UABlocks.TALL_PICKERELWEED.get()||block instanceof TallFlowerBlock){
//                    event.setCanceled(true);
//                }
////                else if((block instanceof FlowerBlock || block instanceof DoublePlantBlock
////                        || block==Blocks.DEAD_BUSH || block instanceof TallGrassBlock)&&
////                        !(block instanceof TallSeagrassBlock|| block instanceof SmallDripleafBlock||
////                                block instanceof WildRiceBlock)){
////                    //state.is(BlockTags.FLOWERS), Helper.isBlock(block, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.DEAD_BUSH)
////                    growFlowers(level, pos);
////                }
////                else if(block==Blocks.WET_SPONGE){
////                    growSponge(level,pos);
////                }
////                else if(state.is(ModTags.Blocks.netherFlowers)){
////                    List<Block> blocks= new ArrayList<>(List.of(Blocks.WARPED_NYLIUM, Blocks.CRIMSON_NYLIUM, Blocks.SOUL_SOIL));
////                    if(block==Blocks.CRIMSON_ROOTS)
////                        blocks.addAll(List.of(Blocks.BLACKSTONE,Blocks.BASALT,Blocks.POLISHED_BASALT));
////                    Function<BlockState, Boolean> func = state1 -> blocks.contains(state1.getBlock());
////                    growFlowers(level, pos, func);
////                }
////                else if(state.is(ModTags.Blocks.crimsonRootsGroundBlocks)){
////                    growCrimsonRoots(level,pos);
////                }
////                else if(block == Blocks.GRASS_BLOCK){
////                    ((GrassBlock)block).performBonemeal(level, level.random, pos, Blocks.GRASS.defaultBlockState());
////                }
////                else if(block==Blocks.SMALL_DRIPLEAF){
////                    growSmallDripLeaf(level, level.random, pos);
////                }
//                else {
//                    flag = false;
//                }
//                if(flag) {
//                    event.setResult(Event.Result.DENY);
//                }
//            }
//        }
//    }


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
    private static void growSand(Level world, BlockPos pos) {
        BlockState state = Blocks.DEAD_BUSH.defaultBlockState();
        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
        placeBoneMeal(world, pos, blockState -> blockState.is(BlockTags.SAND), 10, func);
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

//    private static void growRootedDirt(Level world, RandomSource random, BlockPos pos) {
//        if(world instanceof ServerLevel&&!world.isClientSide) {
//            BlockPos blockPos = pos.below();
//            label48:
//            for(int i = 0; i < 20; ++i) {
//                BlockPos blockPos2 = blockPos;
//                for(int j = 0; j < i / 4; ++j) {
//                    blockPos2 = blockPos2.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
//                    if(!world.getBlockState(blockPos2.above()).is(Blocks.ROOTED_DIRT) || world.getBlockState(blockPos2).isFaceSturdy(world, blockPos2, Direction.UP)) {//UP Might be a problem
//                        continue label48;
//                    }
//                }
//                if(world.isEmptyBlock(blockPos2)&&Helper.isOkToPlace(world,blockPos2,Blocks.HANGING_ROOTS)) {
//                    world.setBlockAndUpdate(blockPos2, Blocks.HANGING_ROOTS.defaultBlockState());
//                }
//            }
//        }
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

//    private static boolean createDripLeaf(Level level, BlockPos pos) {
//        Direction direction = Helper.select(level,Helper.HORIZONTAL_DIRECTIONS);
//        BlockState dripLeaf = Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
//        boolean successful=Helper.isOkToPlace(level,pos,dripLeaf)&&Helper.isOkToPlace(level,pos.above(),dripLeaf);
//        if(successful&&!level.isClientSide){
//            level.setBlockAndUpdate(pos, dripLeaf);
//            level.setBlockAndUpdate(pos.above(), dripLeaf.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
//        }
//        return successful;
//    }


//    private static void growFlowers(Level world, BlockPos pos, Function<BlockState, Boolean> check) {
//        BlockState state = world.getBlockState(pos);
//        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
//        boolean b = state.getBlock() instanceof DoublePlantBlock;
//        if(b) {
//            BlockPos pos2 = pos.below();
//            BlockState state1 = world.getBlockState(pos2);
//            if(state1.isFaceSturdy(world, pos2, Direction.UP)) {
//                pos = pos.below();
//            }
//            func = (pos1) -> {
//                DoublePlantBlock.placeAt(world, state, pos1, 2);
//                return true;
//            };
//        }
//        placeBoneMeal(world, pos.below(), check, 10, func, b);
//    }

//    private static void growFlowers(Level world, BlockPos pos) {
//        growFlowers(world, pos, blockState -> blockState.is(BlockTags.DIRT));
//    }

//    private static void growSmallDripLeaf(ServerLevel level, RandomSource random, BlockPos pos) {
//        Function<BlockState,Boolean> check=state ->
//                state.is(Blocks.CLAY)||state.is(BlockTags.DIRT);
//        placeBoneMeal(level,pos,check, 10, pos1->createDripLeaf(level,pos1));
//    }

//    private static void growSponge(Level world, BlockPos pos) {
//        BlockState state = Blocks.WET_SPONGE.defaultBlockState();
//        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
//        placeBoneMeal(world, pos.below(), (state1, pos1) -> state1.isFaceSturdy(world, pos1, Direction.UP), 10, func, false, true);
//    }
//
//    private static void growCrimsonRoots(Level world, BlockPos pos) {
//        BlockState state = Blocks.CRIMSON_ROOTS.defaultBlockState();
//        Function<BlockPos, Boolean> func = (pos1) -> world.setBlockAndUpdate(pos1, state);
//        Function<BlockState, Boolean> pred;
//        pred = blockState -> state.is(ModTags.Blocks.netherRootsPlaceable);
//        placeBoneMeal(world, pos, pred, 10, func);
//    }
}
