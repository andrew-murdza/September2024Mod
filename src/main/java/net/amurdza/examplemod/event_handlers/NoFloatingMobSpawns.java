package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.world.entity.Mob.checkMobSpawnRules;
import static net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn;

@Mod.EventBusSubscriber(
        modid = AOEMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class NoFloatingMobSpawns {
    @SubscribeEvent
    public static void initializeAttributes(SpawnPlacementRegisterEvent event) {

        addSpawn(event, EntityType.SNIFFER, false, Animal::checkAnimalSpawnRules);

        addSpawn(event, EntityType.CAMEL, false,
                (p,q,r,s,t)->camel(q,s));

        addSpawn(event, IafEntityRegistry.HIPPOCAMPUS.get(), true,
                NoFloatingMobSpawns::checkSeaSerpentSpawnRules);

        addSpawn(event, IafEntityRegistry.DREAD_KNIGHT.get(), false,
                Monster::checkMonsterSpawnRules);

        addSpawn(event, IafEntityRegistry.DREAD_THRALL.get(), false,
                Monster::checkMonsterSpawnRules);

        addSpawn(event, IafEntityRegistry.GORGON.get(), false,
                Monster::checkMonsterSpawnRules);

        addSpawn(event, EntityType.AXOLOTL, true,
                NoFloatingMobSpawns::checkSurfaceWaterAnimalSpawnRules);

        addSpawn(event, EntityType.TURTLE, true,
                NoFloatingMobSpawns::checkSurfaceWaterAnimalSpawnRules);

        addSpawn(event, EntityType.GLOW_SQUID, true,
                NoFloatingMobSpawns::checkWaterAnimalSpawnRules);

        addSpawn(event, EntityType.SLIME, false,
                NoFloatingMobSpawns::checkMonsterSpawnRules);

        addSpawn(event, EntityType.PILLAGER, false,
                NoFloatingMobSpawns::checkMonsterSpawnRules);

        addSpawn(event, EntityType.ZOGLIN, false,
                NoFloatingMobSpawns::checkMonsterSpawnRules);

        addSpawn(event, EntityType.PIGLIN_BRUTE, false,
                NoFloatingMobSpawns::checkMonsterSpawnRules);

        addSpawn(event, EntityType.SHULKER, false,
                (p,q,r,s,t)->shulker(q,s,t));
    }
    private static <T extends Entity> void addSpawn(
            SpawnPlacementRegisterEvent event,
            EntityType<T> entityType,
            boolean inWater,
            SpawnPlacements.SpawnPredicate<T> predicate
    ) {
        event.register(
                entityType,
                inWater ? SpawnPlacements.Type.IN_WATER : SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                predicate,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

    public static boolean checkSeaSerpentSpawnRules(EntityType pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return WorldGenUtils.getTotalWaterInColumn(pPos,0,pLevel)>=6;//3, >=4
    }

    public static boolean checkMonsterSpawnRules(EntityType<? extends Mob> pType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && isDarkEnoughToSpawn(pLevel, pPos, pRandom) && checkMobSpawnRules(pType, pLevel, pSpawnType, pPos, pRandom);
    }

    public static boolean checkSurfaceWaterAnimalSpawnRules(EntityType<? extends LivingEntity> pType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        int i = WorldGenUtils.getSeaLevelWorldGen(pPos,pLevel);
        int j = i - 13;
        return pPos.getY() >= j && pPos.getY() <= i && pLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && pLevel.getBlockState(pPos.above()).is(Blocks.WATER);
    }

    public static boolean checkWaterAnimalSpawnRules(EntityType<? extends LivingEntity> pType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return pLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && pLevel.getFluidState(pPos.below(2)).is(FluidTags.WATER)
                && pLevel.getBlockState(pPos.above()).is(Blocks.WATER);
    }
    private static boolean camel(ServerLevelAccessor level, BlockPos pos) {
        BlockState state=level.getBlockState(pos.below());
        return (state.is(Blocks.SAND) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) && level.getRawBrightness(pos, 0) > 8;
    }
    private static boolean shulker(ServerLevelAccessor pLevel, BlockPos pPos, RandomSource pRandom) {
        for(Direction direction: Direction.values()){
            if(pLevel.getBlockState(pPos.relative(direction)).is(BlockTags.TERRACOTTA)){
                return pLevel.getDifficulty() != Difficulty.PEACEFUL && isDarkEnoughToSpawn(pLevel, pPos, pRandom);
            }
        }
        return false;
    }

}
