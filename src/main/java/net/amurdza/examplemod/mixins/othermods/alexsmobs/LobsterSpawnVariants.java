package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(EntityLobster.class)
public class LobsterSpawnVariants {
    @Redirect(method = "finalizeSpawn", at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityLobster;setVariant(I)V",remap = false),remap = false)
    private void hi(EntityLobster instance, int variant, ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag){
        variant=0;
        if(worldIn.getBiome(instance.blockPosition()).is(ModTags.Biomes.mountainBiomes)){
            variant=instance.getRandom().nextInt(2);
        }
        instance.setVariant(variant);
    }
}
