package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Slime.class)
public class SlimeSpawnsLikeMonsters {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static boolean checkSlimeSpawnRules(EntityType<Slime> pSlime, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom){
        if(!(pLevel instanceof ServerLevelAccessor serverLevelAccessor))
            return false;
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(serverLevelAccessor, pPos, pRandom) && Mob.checkMobSpawnRules(pSlime, pLevel, pSpawnType, pPos, pRandom);
    }
}
