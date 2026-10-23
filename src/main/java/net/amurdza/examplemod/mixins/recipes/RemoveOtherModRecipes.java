package net.amurdza.examplemod.mixins.recipes;

import com.google.common.collect.ImmutableMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.future.FutureContentPolicy;
import com.google.gson.JsonElement;
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

@Mixin(value = RecipeManager.class, priority = 900)
public abstract class RemoveOtherModRecipes {

    @Shadow
    @Mutable
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Shadow
    @Mutable
    private Map<ResourceLocation, Recipe<?>> byName;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void aoe$canonicalBackportItems(Map<ResourceLocation, JsonElement> object,
                                          ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        // Apply the existing namespace policy before decoding absent optional serializers.
        object.keySet().removeIf(id -> !aoemod$isAllowedRecipeNamespace(id));
        object.values().forEach(FutureContentPolicy::normalizeRecipe);
        object.values().removeIf(RemoveOtherModRecipes::aoemod$hasExcludedResult);
    }

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

        AOEMod.LOGGER.info("Applied AOE recipe namespace and backport-content policy.");
    }

    @Unique
    private static boolean aoemod$hasExcludedResult(JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        JsonElement result = recipe.getAsJsonObject().get("result");
        if (result == null) return false;
        if (result.isJsonObject()) result = result.getAsJsonObject().get("item");
        if (result == null || !result.isJsonPrimitive() || !result.getAsJsonPrimitive().isString()) return false;
        ResourceLocation id = ResourceLocation.tryParse(result.getAsString());
        return id != null && FutureContentPolicy.isExcludedItem(id);
    }

    @Unique
    private static boolean aoemod$isAllowedRecipeNamespace(ResourceLocation id) {
        String namespace = id.getNamespace();
        return namespace.equals("minecraft") || namespace.equals(AOEMod.MOD_ID) || namespace.equals("quark")
                || namespace.equals("vanillabackport") || namespace.equals("tiny_takeover_backport");
    }
}
