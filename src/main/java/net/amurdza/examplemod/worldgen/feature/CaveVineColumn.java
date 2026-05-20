package net.amurdza.examplemod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.util.RandomCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CaveVineColumn extends Feature<CaveVineConfig> {
    public CaveVineColumn(Codec<CaveVineConfig> p_66619_) {
        super(p_66619_);
    }
    public boolean place(FeaturePlaceContext<CaveVineConfig> context) {
        CaveVineConfig config = context.config();
        float chance=config.chance;
        IntProvider heightProvider=config.heightProvider;
        RandomSource random = context.random();
        WorldGenLevel level=context.level();
        ChunkAccess chunk=level.getChunk(context.origin());
        ChunkPos chunkPos=chunk.getPos();
        int minX=chunkPos.getMinBlockX();
        int minZ=chunkPos.getMinBlockZ();
        boolean flag=false;
        RandomCollection<Integer> stateCases=new RandomCollection<>();
        stateCases.add(1,0);
        stateCases.add(config.weepingVinesChance,1);
        stateCases.add(config.sporeBlossomChance,2);
        for(int i=0;i<16;i++){
            int x=minX+i;
            for(int j=0;j<16;j++){
                int z=minZ+j;
                int maxY=chunk.getMaxBuildHeight();//chunk.getHeight(water? Heightmap.Types.OCEAN_FLOOR_WG: Heightmap.Types.WORLD_SURFACE_WG,x,z)+1;
                BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos(x,maxY,z);
                List<Integer> caveVinesPoses=new ArrayList<>();
                for(int k=maxY;k>=chunk.getMinBuildHeight()+1;k--){
                    BlockState state=level.getBlockState(pos);

                    if(state.is(Blocks.CAVE_VINES)||state.is(Blocks.CAVE_VINES_PLANT)){
                        caveVinesPoses.add(k);
                    }
                    boolean shouldRun=random.nextFloat()<chance;
                    boolean weepingVines=stateCases.next(random)==1;
                    boolean sporeBlossom=stateCases.next(random)==2;
                    BlockPredicate pred=BlockPredicate.anyOf(BlockPredicate.solid(),
                            BlockPredicate.matchesTag(BlockTags.LEAVES));
                    boolean flag1=helper(level,pos,context,pred, heightProvider,shouldRun,weepingVines,sporeBlossom);
                    flag=flag||flag1;
                    pos.move(0,-1,0);
                }
                for(int k:caveVinesPoses){
                    BlockState blockState=Blocks.CAVE_VINES.defaultBlockState()
                            .setValue(BlockStateProperties.BERRIES,true)
                            .setValue(BlockStateProperties.AGE_25,25)
                            .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(new BlockPos(x,k,z)).is(FluidTags.WATER));
                    BlockPos vinePos = new BlockPos(x, k, z);

                    if (doesntHaveMossWithinFourBelow(level, vinePos)) {
                        level.setBlock(vinePos, blockState, 2);
                    }
                }
            }

        }
        return flag;
    }
    private final BiFunction<BlockState,Boolean,Boolean> tester= (state,weeping)->state.getFluidState().is(FluidTags.WATER)&&
            !weeping||state.isAir();
    private boolean helper(WorldGenLevel level, BlockPos pos, FeaturePlaceContext<CaveVineConfig> context,
                           BlockPredicate predicate, IntProvider heightProvider, boolean shouldRun,
                           boolean weepingVines, boolean sporeBlossom){
        BlockState state=level.getBlockState(pos);
        if(tester.apply(state,weepingVines||sporeBlossom)&&predicate.test(level,pos.above())&&shouldRun){
            return placeHelper(level,pos,
                    heightProvider.sample(context.random()),weepingVines,sporeBlossom);
        }
        return false;
    }
    private boolean placeHelper(WorldGenLevel level, BlockPos pos, int h, boolean weepingVines, boolean sporeblossom){
        if (sporeblossom) {
            h = 1;
        }

        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos pos1 = pos.mutable();

        for (int i = 0; i < h; i++) {
            BlockState state = level.getBlockState(pos1);

            // Stop at mushroom cap, logs, leaves, moss, stone, etc.
            // Do NOT replace it, and do NOT keep checking below it.
            if (!tester.apply(state, weepingVines || sporeblossom)) {
                break;
            }

            if ((weepingVines || sporeblossom) || doesntHaveMossWithinFourBelow(level, pos1)) {
                positions.add(pos1.immutable());
            }

            positions.add(pos1.immutable());
            pos1.move(0, -1, 0);
        }

        boolean flag = false;

        for (int i = 0; i < positions.size(); i++) {
            BlockPos placePos = positions.get(i);
            BlockState state = level.getBlockState(placePos);

            BlockState blockState = Blocks.CAVE_VINES.defaultBlockState()
                    .setValue(BlockStateProperties.BERRIES, true)
                    .setValue(BlockStateProperties.WATERLOGGED, state.getFluidState().is(FluidTags.WATER))
                    .setValue(BlockStateProperties.AGE_25, 25);

            if (weepingVines) {
                boolean isLast = i == positions.size() - 1;
                blockState = isLast
                        ? Blocks.WEEPING_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25, 25)
                        : Blocks.WEEPING_VINES_PLANT.defaultBlockState();
            }

            if (sporeblossom) {
                blockState = Blocks.SPORE_BLOSSOM.defaultBlockState();
            }

            level.setBlock(placePos, blockState, 2);
            flag = true;
        }

        return flag;
    }
    private boolean doesntHaveMossWithinFourBelow(WorldGenLevel level, BlockPos pos) {
        for (int dy = 1; dy <= 4; dy++) {
            BlockState state=level.getBlockState(pos.below(dy));
            if (state.is(Blocks.MOSS_BLOCK)||!state.getFluidState().isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
