package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.event_handlers.NoFloatingMobSpawns;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnPlacements.class)
public class StridersSpawnOnMoss {
    @Redirect(method = "<clinit>",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/SpawnPlacements;register(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;)V",ordinal = 47))
    private static <T extends Entity> void hi(EntityType<T> pEntityType, SpawnPlacements.Type pDecoratorType, Heightmap.Types pHeightMapType, SpawnPlacements.SpawnPredicate<T> pDecoratorPredicate){
        SpawnPlacements.register(EntityType.STRIDER, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, StridersSpawnOnMoss::striderCond);
    }
    private static boolean striderCond(EntityType pEntityType, ServerLevelAccessor pServerLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        BlockState state=pServerLevel.getBlockState(pPos.below());
        return Helper.isSpecialBiome(pServerLevel,pPos)&&state.is(Blocks.MOSS_BLOCK)||state.is(BlockTags.NYLIUM);
    }
}
