package net.amurdza.examplemod.mixins.othermods.hybridaquatic;

import dev.hybridlabs.aquatic.entity.HybridAquaticEntityTypes;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = HybridAquaticEntityTypes.class, remap = false)
public class HybridAquaticUseVanillaMobCategories {

    @ModifyArg(
            method = "registerLiving",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"
            ),
            index = 1
    )
    private MobCategory aoemod$replaceHybridAquaticCategory(MobCategory original) {
        String name = original.getName();

        return switch (name) {
            case "hybrid-aquatic:fish",
                 "hybrid-aquatic:river_fish",
                 "hybrid-aquatic:jelly",
                 "hybrid-aquatic:cephalopod",
                 "hybrid-aquatic:critter" -> MobCategory.WATER_AMBIENT;

            case "hybrid-aquatic:crustacean",
                 "hybrid-aquatic:shark",
                 "hybrid-aquatic:mammal" -> MobCategory.WATER_CREATURE;

            case "hybrid-aquatic:miniboss",
                 "hybrid-aquatic:minion" -> MobCategory.MONSTER;

            default -> original;
        };
    }
}