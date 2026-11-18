package net.amurdza.examplemod.mixins.othermods.farmersdelight;

import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.world.VillageStructures;

import java.util.List;

@Mixin(value = VillageStructures.class, remap = false)
public abstract class VillageFarmsWithoutOnions {
    @ModifyArg(method = "addNewVillageBuilding", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/RuleProcessor;<init>(Ljava/util/List;)V",
            remap = true), index = 0, require = 3)
    private static List<? extends ProcessorRule> aoe$removeOnionCropRules(List<? extends ProcessorRule> rules) {
        return rules.stream().filter(rule -> !rule.getOutputState().is(ModBlocks.ONION_CROP.get())).toList();
    }
}
