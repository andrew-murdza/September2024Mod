package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(EntityLobster.class)
public class LobsterSpawnVariants {
    // Bucket NBT is applied after the entity joins its level, so the invariant
    // must hold at the setter as well as during spawning and save migration.
    @ModifyVariable(method = "setVariant", at = @At("HEAD"), argsOnly = true, remap = false)
    private int aoemod$keepSnowyLobstersRed(int variant) {
        EntityLobster lobster = (EntityLobster) (Object) this;
        return GameplayDimensions.is(lobster.level(), GameplayDimensions.SNOWY) ? 0 : variant;
    }

    @Redirect(method = "finalizeSpawn", at= @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityLobster;setVariant(I)V",remap = false),remap = false)
    private void aoe$regionalVariant(EntityLobster instance, int variant, ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        if (GameplayDimensions.is(worldIn, GameplayDimensions.SNOWY)) {
            variant = 0;
        } else {
            variant = 0;
            if (worldIn.getBiome(instance.blockPosition()).is(ModTags.Biomes.mountainBiomes)) {
                variant = instance.getRandom().nextInt(2);
            }
        }
        instance.setVariant(variant);
    }
}
