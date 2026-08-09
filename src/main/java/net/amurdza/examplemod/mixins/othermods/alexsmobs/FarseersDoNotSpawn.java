package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityFarseer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityFarseer.class, remap = false)
public class FarseersDoNotSpawn {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private void aoemod$disableFarseerSpawnRules(
            LevelAccessor level,
            MobSpawnType spawnType,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }

    @Inject(method = "checkFarseerSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void aoemod$disableFarseerNaturalSpawn(
            EntityType<? extends Monster> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }
}
