package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.serialization.Dynamic;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.RetiredBclibWorldgen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelStorageSource.class)
public abstract class RetiredBclibWorldgenLoading {
    @ModifyArg(method = "readWorldGenSettings", index = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/Dynamic;)Lcom/mojang/serialization/DataResult;", remap = false))
    private static <T> Dynamic<T> aoe$replaceRetiredGeneratorCodecs(Dynamic<T> settings) {
        if (ModList.get().isLoaded("bclib")) return settings;
        Dynamic<T> repaired = RetiredBclibWorldgen.migrate(settings);
        if (!settings.getValue().equals(repaired.getValue())) {
            AOEMod.LOGGER.info("Restored vanilla Nether/End generator codecs from retired BCLib world metadata");
        }
        return repaired;
    }
}
