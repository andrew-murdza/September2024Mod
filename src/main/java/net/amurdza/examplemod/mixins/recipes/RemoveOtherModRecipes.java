package net.amurdza.examplemod.mixins.recipes;

import com.google.common.collect.ImmutableMap;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RemoveOtherModRecipes {

    @Shadow
    @Mutable
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Shadow
    @Mutable
    private Map<ResourceLocation, Recipe<?>> byName;

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL")
    )
    private void aoemod$removeNonAllowedRecipes(
            Map<ResourceLocation, ?> object,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        ImmutableMap.Builder<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<ResourceLocation, Recipe<?>> byNameBuilder = ImmutableMap.builder();

        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> typeEntry : this.recipes.entrySet()) {
            ImmutableMap.Builder<ResourceLocation, Recipe<?>> recipesOfTypeBuilder = ImmutableMap.builder();

            for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : typeEntry.getValue().entrySet()) {
                ResourceLocation id = recipeEntry.getKey();
                Recipe<?> recipe = recipeEntry.getValue();

                if (aoemod$isAllowedRecipeNamespace(id)) {
                    recipesOfTypeBuilder.put(id, recipe);
                    byNameBuilder.put(id, recipe);
                }
            }

            recipesBuilder.put(typeEntry.getKey(), recipesOfTypeBuilder.build());
        }

        this.recipes = recipesBuilder.build();
        this.byName = byNameBuilder.build();

        AOEMod.LOGGER.info("Removed all recipes outside the minecraft, aoemod, and quark namespaces.");
    }

    @Unique
    private static boolean aoemod$isAllowedRecipeNamespace(ResourceLocation id) {
        String namespace = id.getNamespace();
        return namespace.equals("minecraft") || namespace.equals(AOEMod.MOD_ID) || namespace.equals("quark");
    }
}
