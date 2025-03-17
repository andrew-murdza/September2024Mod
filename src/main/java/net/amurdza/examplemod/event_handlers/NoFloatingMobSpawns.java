package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import samebutdifferent.ecologics.registry.ModEntityTypes;

@Mod.EventBusSubscriber(
        modid = AOEMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class NoFloatingMobSpawns {
    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        SpawnPlacements.register(EntityType.SNIFFER, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ModEntityTypes.COCONUT_CRAB.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(IafEntityRegistry.SEA_SERPENT.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoFloatingMobSpawns::checkSeaSerpentSpawnRules);
        SpawnPlacements.register(IafEntityRegistry.HIPPOCAMPUS.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                NoFloatingMobSpawns::checkSeaSerpentSpawnRules);
    }
    public static boolean checkSeaSerpentSpawnRules(EntityType pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        int N=3;
        for(int i=-N;i<=N;i++){
            for(int j=-N;j<=N;j++){
                if(!pLevel.getFluidState(pPos.below()).is(FluidTags.WATER)||!pLevel.getBlockState(pPos.below()).is(Blocks.WATER)){
                    return false;
                }
            }
        }
        return true;
    }

}
