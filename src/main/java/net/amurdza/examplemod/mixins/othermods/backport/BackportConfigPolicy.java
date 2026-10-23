package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.level.recipes.RecipeStrategy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/** Enforces the user's exclusions even if an old dependency config is present. */
@Pseudo
@Mixin(targets = "com.blackgear.platform.core.util.config.forge.ForgeConfigValue", remap = false)
public abstract class BackportConfigPolicy {
    @Unique
    private static final Set<String> AOE_DISABLED_OPTIONS = Set.of(
            "Copper Age.enable_dye_palettes", "Copper Age.enable_modern_dye_textures",
            "The Garden Awakens.pale_garden", "The Garden Awakens.creaking",
            "The Garden Awakens.resin", "The Garden Awakens.resin_loot",
            "The Garden Awakens.pale_trades", "Chase the Skies.dried_ghasts",
            "Spring to Life.spring_trades",
            "Chase the Skies.tears_music_disc", "Chase the Skies.enable_locator_bar",
            "Hot as Lava.lava_chicken_music_disc", "Mounts of Mayhem.has_nautilus",
            "Mounts of Mayhem.has_nautilus_armor_loot", "Chaos Cubed.has_sulfur_caves",
            "Chaos Cubed.has_sulfur_springs", "Wilderness Bound.has_dappled_forest");

    @Shadow public abstract List<String> getPath();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void aoe$enforceBackportExclusions(CallbackInfoReturnable<Object> cir) {
        String path = String.join(".", getPath());
        if (AOE_DISABLED_OPTIONS.contains(path)) cir.setReturnValue(false);
        if (path.equals("Mounts of Mayhem.zombie_nautilus_spawn_chance")) cir.setReturnValue(0.0D);
        if (path.equals("Bundles of Bravery.bundle_recipe")
                || path.equals("The Garden Awakens.lead_recipe")
                || path.equals("The Garden Awakens.saddle_recipe")
                || path.equals("Spring to Life.lodestone_recipe")) {
            cir.setReturnValue(RecipeStrategy.IGNORE);
        }
    }
}
