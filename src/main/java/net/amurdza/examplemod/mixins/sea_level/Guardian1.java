package net.amurdza.examplemod.mixins.sea_level;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(Guardian.class)
public class Guardian1 {
//    @Redirect(method = "checkGuardianSpawnRules",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;canSeeSkyFromBelowWater(Lnet/minecraft/core/BlockPos;)Z"))
//    private static boolean hi(LevelAccessor instance, BlockPos blockPos){
//        return true;
//    }
/**
 * @author
 * @reason
 */
@Overwrite
    public static boolean checkGuardianSpawnRules(EntityType<? extends Guardian> pGuardian, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
        return (pLevel.getDifficulty() != Difficulty.PEACEFUL && (pSpawnType == MobSpawnType.SPAWNER ||
                pLevel.getFluidState(pPos).is(FluidTags.WATER))
                && pLevel.getFluidState(pPos.below()).is(FluidTags.WATER));
    }

}
