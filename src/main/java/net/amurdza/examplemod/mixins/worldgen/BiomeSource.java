package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MultiNoiseBiomeSource.class,priority = 1001)
public abstract class BiomeSource {
    @Shadow protected abstract Climate.ParameterList<Holder<Biome>> parameters();

    @Unique
    private Holder<Biome> september2024PartialMod$getNoiseBiomeHelper(int pX, int pY, int pZ, Climate.Sampler pSampler){
        List<Integer> ys=List.of(-8,0,8,16);
        Holder<Biome> biome=parameters().findValue(pSampler.sample(pX,pY,pZ));
        List<Pair<Climate.ParameterPoint,Holder<Biome>>> list=parameters().values();
        if(!biome.get().hasPrecipitation()){
            for(int i=0;i<ys.size();i++){
                if(pY<=ys.get(i)){
                    return list.get(list.size()-1-i).getSecond();
                }
            }
        }
        return biome;
    }
    @Inject(
            method = {"getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"},
            at = {@At("RETURN")},
            cancellable = true)
    public void getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        cir.setReturnValue(september2024PartialMod$getNoiseBiomeHelper(x,y,z,sampler));
    }
}
