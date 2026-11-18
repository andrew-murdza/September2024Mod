package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native crab limits must not truncate an authored school to one or two individuals. */
@Mixin(NaturalSpawner.class)
public final class CommonAquaticSchoolSizes {
    @Redirect(method = "spawnCategoryForPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;getMaxSpawnClusterSize()I"))
    private static int aoemod$schoolLimit(Mob mob) {
        int limit = commonLimit(mob);
        return limit > 0 ? limit : mob.getMaxSpawnClusterSize();
    }

    @Redirect(method = "spawnCategoryForPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isMaxGroupSizeReached(I)Z"))
    private static boolean aoemod$schoolComplete(Mob mob, int count) {
        int limit = commonLimit(mob);
        return limit > 0 ? count >= limit : mob.isMaxGroupSizeReached(count);
    }

    private static int commonLimit(Mob mob) {
        if (!GameplayDimensions.isCustom(mob.level())
                || DocumentedFauna.spawnCategory(mob.getType()) != ModMobCategories.COMMON_AQUATIC) return 0;
        return DocumentedFauna.forBiome("coral_reef").stream().filter(species -> species.type() == mob.getType())
                .mapToInt(species -> DocumentedFauna.groupSize(species).maximum).findFirst().orElse(0);
    }
}
