package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.entity.SenorPumpkinEntity;
import net.mcreator.borninchaosv.init.BornInChaosV1ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SenorPumpkinEntity.class, remap = false)
public abstract class SenorPumpkinSpawnsNormally {

    /**
     * Replaces Born in Chaos' seasonal spawn predicate with vanilla monster spawn rules.
     * @author amurdza
     * @reason simplest solution for me
     */
    @Overwrite(remap = false)
    public static void init() {
        SpawnPlacements.register(
                (EntityType<? extends Monster>) BornInChaosV1ModEntities.SENOR_PUMPKIN.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules
        );
    }
}