package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnConditions {
    @SubscribeEvent
    public static void updateSpawnConditions(final FMLCommonSetupEvent event) {
        addSpawn(EntityType.AXOLOTL,true,event);
        addSpawn(EntityType.GLOW_SQUID,true,(p,q,r,s,t)->glowSquid(q,s),event);
        addSpawn(EntityType.SLIME,false, Monster::checkMonsterSpawnRules,event);

        addSpawn(EntityType.PILLAGER,false,Monster::checkMonsterSpawnRules,event);
    }
    private static void addSpawn(FMLCommonSetupEvent event, SpawnPlacements.Type type, Heightmap.Types heightType , EntityType entityType, SpawnPlacements.SpawnPredicate func){
        event.enqueueWork(()->{
            try{
                SpawnPlacements.register(entityType,type,heightType,func);
            }
            catch (IllegalStateException exception){

            }
        });
    }
    private static void addSpawn(EntityType entityType, boolean inWater, FMLCommonSetupEvent event){
        SpawnPlacements.Type type = inWater? SpawnPlacements.Type.IN_WATER:SpawnPlacements.Type.ON_GROUND;
        Heightmap.Types motionBlocking = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        addSpawn(event,type,motionBlocking,entityType,(p,q,r,s,t)->true);
    }
    private static void addSpawn(EntityType entityType, boolean inWater, SpawnPlacements.SpawnPredicate func, FMLCommonSetupEvent event){
        SpawnPlacements.Type type = inWater? SpawnPlacements.Type.IN_WATER:SpawnPlacements.Type.ON_GROUND;
        Heightmap.Types motionBlocking = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        addSpawn(event,type,motionBlocking,entityType,(p,q,r,s,t)->true);
    }
    private static boolean glowSquid(ServerLevelAccessor level, BlockPos pos) {
        return Helper.isSpecialBiome(level,pos)||pos.getY() <= level.getSeaLevel()-33;
    }
}
