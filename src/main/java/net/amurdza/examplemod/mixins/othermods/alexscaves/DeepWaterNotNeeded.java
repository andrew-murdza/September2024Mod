package net.amurdza.examplemod.mixins.othermods.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ACEntityRegistry.class)
public class DeepWaterNotNeeded {
    @Redirect(method = "spawnPlacements",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",ordinal = 18),remap = false)
    private static <T extends Entity> void hi(SpawnPlacementRegisterEvent instance, EntityType<T> entityType,
                                              SpawnPlacements.Type placementType, Heightmap.Types heightmap,
                                              SpawnPlacements.SpawnPredicate<T> predicate,
                                              SpawnPlacementRegisterEvent.Operation operation){
        helper(instance,ACEntityRegistry.LANTERNFISH.get(),0);
    }
    @Redirect(method = "spawnPlacements",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",ordinal = 19),remap = false)
    private static <T extends Entity> void hi1(SpawnPlacementRegisterEvent instance, EntityType<T> entityType,
                                              SpawnPlacements.Type placementType, Heightmap.Types heightmap,
                                              SpawnPlacements.SpawnPredicate<T> predicate,
                                              SpawnPlacementRegisterEvent.Operation operation){
        helper(instance,ACEntityRegistry.SEA_PIG.get(),3);
    }
    @Redirect(method = "spawnPlacements",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",ordinal = 20),remap = false)
    private static <T extends Entity> void hi2(SpawnPlacementRegisterEvent instance, EntityType<T> entityType,
                                              SpawnPlacements.Type placementType, Heightmap.Types heightmap,
                                              SpawnPlacements.SpawnPredicate<T> predicate,
                                              SpawnPlacementRegisterEvent.Operation operation){
        helper(instance,ACEntityRegistry.HULLBREAKER.get(),20);
    }
    @Redirect(method = "spawnPlacements",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",ordinal = 21),remap = false)
    private static <T extends Entity> void hi3(SpawnPlacementRegisterEvent instance, EntityType<T> entityType,
                                              SpawnPlacements.Type placementType, Heightmap.Types heightmap,
                                              SpawnPlacements.SpawnPredicate<T> predicate,
                                              SpawnPlacementRegisterEvent.Operation operation){
        helper(instance,ACEntityRegistry.GOSSAMER_WORM.get(),8);
    }
    @Redirect(method = "spawnPlacements",at= @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;Lnet/minecraftforge/event/entity/SpawnPlacementRegisterEvent$Operation;)V",ordinal = 21),remap = false)
    private static <T extends Entity> void hi4(SpawnPlacementRegisterEvent instance, EntityType<T> entityType,
                                               SpawnPlacements.Type placementType, Heightmap.Types heightmap,
                                               SpawnPlacements.SpawnPredicate<T> predicate,
                                               SpawnPlacementRegisterEvent.Operation operation){
        helper(instance,ACEntityRegistry.TRIPODFISH.get(),0);
    }
    @Unique
    private static void helper(SpawnPlacementRegisterEvent instance, EntityType type, int k){
        instance.register(type, SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (p,q,r,s,t)->checkSurfaceWaterAnimalSpawnRules1(p,q,r,s,t,k), SpawnPlacementRegisterEvent.Operation.AND);
    }
    @Unique
    private static boolean checkSurfaceWaterAnimalSpawnRules1(EntityType<?> pWaterAnimal, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom, int k) {
        return WorldGenUtils.getTotalWaterAbove(pPos,pLevel)>=Math.max(k,0);//k-10//WorldGenUtils.getTotalWaterInColumn(pPos,5,pLevel)>=k&&
    }
}
