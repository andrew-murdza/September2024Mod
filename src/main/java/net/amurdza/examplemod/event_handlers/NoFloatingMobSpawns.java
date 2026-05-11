package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import samebutdifferent.ecologics.registry.ModEntityTypes;
import teamdraco.finsandstails.registry.FTEntities;

@Mod.EventBusSubscriber(
        modid = AOEMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class NoFloatingMobSpawns {
    @SubscribeEvent
    public static void initializeAttributes(SpawnPlacementRegisterEvent event) {
        event.register(EntityType.SNIFFER, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntityTypes.COCONUT_CRAB.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(IafEntityRegistry.SEA_SERPENT.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoFloatingMobSpawns::checkSeaSerpentSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(IafEntityRegistry.HIPPOCAMPUS.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoFloatingMobSpawns::checkSeaSerpentSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(IafEntityRegistry.DREAD_KNIGHT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(IafEntityRegistry.DREAD_THRALL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(FTEntities.RUBBER_BELLY_GLIDER.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoFloatingMobSpawns::checkSurfaceWaterAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    public static boolean checkSeaSerpentSpawnRules(EntityType pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return WorldGenUtils.getTotalWaterInColumn(pPos,0,pLevel)>=6;//3, >=4
    }

    public static boolean checkSurfaceWaterAnimalSpawnRules(EntityType<? extends LivingEntity> pType, ServerLevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        int i = pLevel.getSeaLevel();
        int j = i - 13;
        return pPos.getY() >= j && pPos.getY() <= i && pLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && pLevel.getBlockState(pPos.above()).is(Blocks.WATER);
    }

}
