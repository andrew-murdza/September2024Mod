package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public final class DocumentedSpawnPosition {
    @Inject(method = "isSpawnPositionOk", at = @At("HEAD"), cancellable = true)
    private static void aoemod$documentedHabitat(SpawnPlacements.Type placement, LevelReader level, BlockPos pos,
                                                EntityType<?> type, CallbackInfoReturnable<Boolean> result) {
        if (type == null) return;
        var species = DocumentedFauna.find(level, pos, type);
        if (species != null) result.setReturnValue(level.getWorldBorder().isWithinBounds(pos)
                && DocumentedFauna.hasSpace(level, pos, species));
        else if (type == EntityType.DROWNED && GameplayDimensions.isCustom(level)
                && DocumentedMonsters.allows(level, pos, type)) {
            result.setReturnValue(level.getWorldBorder().isWithinBounds(pos) && DocumentedMonsters.hasSpace(level, pos));
        }
    }
}
