package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(EndDragonFight.class)
public class EndDragonFight1 {
    @Shadow @Nullable private BlockPos portalLocation;

    @Redirect(method = "spawnExitPortal",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getSeaLevel()I"))
    private int hi(ServerLevel instance){
        assert portalLocation != null;
        return WorldGenUtils.getSeaLevelWorldGen(portalLocation,instance);
    }
}
