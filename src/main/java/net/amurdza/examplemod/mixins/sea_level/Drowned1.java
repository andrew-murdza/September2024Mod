package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Drowned.class)
public abstract class Drowned1 extends Entity {
    public Drowned1(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Redirect(method = "addBehaviourGoals",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSeaLevel()I"))
    private int hi(Level instance){
        return WorldGenUtils.getSeaLevelWorldGen(blockPosition(),level());
    }
    @Redirect(method = "isDeepEnoughToSpawn",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getSeaLevel()I"))
    private static int hi1(LevelAccessor instance, LevelAccessor pLevel, BlockPos pPos){
        return WorldGenUtils.getTotalWaterAbove(pPos,pLevel)>=5?300:-300;
    }
}
