package net.amurdza.examplemod.mixins.othermods.twilightforest;

import net.amurdza.examplemod.event_handlers.GoblinFortressProtection;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.GoblinFortressPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.monster.Yeti;
import twilightforest.entity.projectile.IceBomb;

import java.util.List;

/** Preserve the native slowing/damage zone without terrain edits or silent roster removals. */
@Mixin(IceBomb.class)
public abstract class GoblinFortressIceBomb {
    @Inject(method = "doTerrainEffect", at = @At("HEAD"), cancellable = true, remap = false)
    private void aoemod$keepFortressTerrain(BlockPos pos, CallbackInfo callback) {
        var bomb = (IceBomb) (Object) this;
        if (bomb.level() instanceof ServerLevel level && GoblinFortressProtection.boundaryAt(level, pos) != null)
            callback.cancel();
    }

    @Redirect(method = "hitNearbyEntities", remap = false, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<LivingEntity> aoemod$keepAuthoredYetis(Level world, Class<LivingEntity> type, AABB box) {
        var entities = world.getEntitiesOfClass(type, box);
        if (!(world instanceof ServerLevel level)) return entities;
        // This native branch writes two blocks at the Yeti's previous position
        // and discard() bypasses LivingDeathEvent. Authored defenders must never
        // vanish this way, even if displaced outside the estate. Ordinary Yetis
        // also cannot convert where either ice block would change the fortress.
        return entities.stream().filter(entity -> {
            if (!(entity instanceof Yeti)) return true;
            if (AuthoredEncounterProgress.isDefender(entity)
                    && GoblinFortressPiece.ENCOUNTER_KIND.equals(AuthoredEncounterProgress.kind(entity))) return false;
            var conversion = BlockPos.containing(entity.xOld, entity.yOld, entity.zOld);
            return GoblinFortressProtection.boundaryAt(level, conversion) == null
                    && GoblinFortressProtection.boundaryAt(level, conversion.above()) == null;
        }).toList();
    }
}
