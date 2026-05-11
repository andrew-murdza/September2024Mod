package net.amurdza.examplemod.mixins.othermods.dawnera;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.xishnikus.thedawnera.common.entity.TDEMobCategory;

@Mixin(
        value=TDEMobCategory.class,
        remap = false
)
public class DawnEraVanillaMobCategories {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lru/xishnikus/thedawnera/common/entity/TDEMobCategory;create(Ljava/lang/String;Ljava/lang/String;IZZI)Lnet/minecraft/world/entity/MobCategory;"
            )
    )
    private static MobCategory aoemod$replaceDawnEraCategories(
            String name,
            String id,
            int max,
            boolean peaceful,
            boolean animal,
            int despawnDistance
    ) {
        return switch (id) {

            case "dawnera_small_herbivore",
                 "dawnera_average_herbivore",
                 "dawnera_big_herbivore",
                 "dawnera_giant_herbivore",
                 "dawnera_small_carnivore",
                 "dawnera_average_carnivore",
                 "dawnera_big_carnivore",
                 "dawnera_giant_carnivore" -> MobCategory.CREATURE;

            case "dawnera_average_water_mob",
                 "dawnera_big_water_mob" -> MobCategory.WATER_CREATURE;

            case "dawnera_small_water_mob" -> MobCategory.WATER_AMBIENT;

            case "dawnera_small_ambient_critter" -> MobCategory.AMBIENT;

            default ->
                    MobCategory.create(
                            name,
                            id,
                            max,
                            peaceful,
                            animal,
                            despawnDistance
                    );
        };
    }
}