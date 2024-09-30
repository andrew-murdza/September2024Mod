package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlowLichenBlock.class)
public class GlowLichenMixin {
    @Inject(method = "performBonemeal", at=@At("HEAD"))
    public void grow(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, CallbackInfo info) {
        GlowLichenBlock block=(GlowLichenBlock) (Object) this;
        int numTries= Helper.isSpecialBiome(world,pos)? Config.GLOW_LICHEN_TRIES:1;
        for(int i=0;i<numTries;i++){
            block.getSpreader().spreadFromRandomFaceTowardRandomDirection(state, world, pos, random);
        }
    }
}
