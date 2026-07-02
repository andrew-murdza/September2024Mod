package net.amurdza.examplemod.mixins.othermods.quark;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.violetmoon.quark.content.automation.module.ChainsConnectBlocksModule;

import java.util.function.Predicate;

@Mixin(value = ChainsConnectBlocksModule.ChainConnection.class, remap = false)
public class ChainsConnectCopperChains {
    @Shadow
    @Mutable
    public static Predicate<BlockState> PREDICATE;

    @Unique
    private static final ResourceLocation AOEMOD_COPPER_CHAIN = new ResourceLocation(AOEMod.MOD_ID, "copper_chain");

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void aoemod$allowCopperChains(CallbackInfo ci) {
        PREDICATE = ChainsConnectCopperChains::aoemod$isSupportedChain;
    }

    @Unique
    private static boolean aoemod$isSupportedChain(BlockState state) {
        if (state.is(Blocks.CHAIN)) {
            return true;
        }

        return AOEMOD_COPPER_CHAIN.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }
}
