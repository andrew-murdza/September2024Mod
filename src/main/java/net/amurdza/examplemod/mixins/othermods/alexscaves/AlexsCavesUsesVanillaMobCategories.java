package net.amurdza.examplemod.mixins.othermods.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ACEntityRegistry.class, remap = false)
public class AlexsCavesUsesVanillaMobCategories {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/MobCategory;create(Ljava/lang/String;Ljava/lang/String;IZZI)Lnet/minecraft/world/entity/MobCategory;",
                    ordinal = 0
            )
    )
    private static MobCategory aoemod$replaceAlexsCavesCategories1(
            String name, String id, int maxNumberOfCreatureIn, boolean isPeacefulCreatureIn, boolean isAnimalIn, int despawnDistance
    ) {
        return MobCategory.CREATURE;
    }

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/MobCategory;create(Ljava/lang/String;Ljava/lang/String;IZZI)Lnet/minecraft/world/entity/MobCategory;",
                    ordinal = 0
            )
    )
    private static MobCategory aoemod$replaceAlexsCavesCategories2(
            String name, String id, int maxNumberOfCreatureIn, boolean isPeacefulCreatureIn, boolean isAnimalIn, int despawnDistance
    ) {
        return MobCategory.WATER_CREATURE;
    }
}