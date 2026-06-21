package net.amurdza.examplemod.mixins.othermods.born_in_chaos;

import net.mcreator.borninchaosv.entity.SirPumpkinheadEntity;
import net.mcreator.borninchaosv.init.BornInChaosV1ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SirPumpkinheadEntity.class, remap = false)
public abstract class SirPumpkinheadSpawnsNormally {

    /**
     * Replaces Born in Chaos' seasonal spawn predicate with vanilla monster spawn rules.
     * @author amurdza
     *
     * @reason simplest way to do what I wanted
     */
    @Overwrite(remap = false)
    public static void init() {
        SpawnPlacements.register(
                (EntityType<? extends Monster>) BornInChaosV1ModEntities.SIR_PUMPKINHEAD.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules
        );
    }
}