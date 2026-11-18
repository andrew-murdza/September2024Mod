package net.amurdza.examplemod.mixins.recipes;

import com.google.common.collect.ImmutableMap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.future.FutureContentPolicy;
import net.amurdza.examplemod.util.RetainedContent;
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
    @Unique
    private final java.util.Set<ResourceLocation> aoemod$retainedNativeRecipes = new java.util.HashSet<>();

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
        aoemod$retainedNativeRecipes.clear();
        object.forEach((id, recipe) -> {
            if (aoemod$hasSelectedNativeResult(recipe)) aoemod$retainedNativeRecipes.add(id);
        });
        object.keySet().removeIf(id -> !aoemod$isAllowedRecipeNamespace(id) && !aoemod$retainedNativeRecipes.contains(id));
        object.values().forEach(FutureContentPolicy::normalizeRecipe);
        object.values().removeIf(RemoveOtherModRecipes::aoemod$requiresRemovedKitchenEquipment);
        object.values().removeIf(RemoveOtherModRecipes::aoemod$hasExcludedResult);
        object.values().removeIf(RemoveOtherModRecipes::aoemod$usesRemovedIngredient);
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

                if (aoemod$isAllowedRecipeNamespace(id) || aoemod$retainedNativeRecipes.contains(id)) {
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
    private static boolean aoemod$requiresRemovedKitchenEquipment(JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        JsonElement type = recipe.getAsJsonObject().get("type");
        return type != null && type.isJsonPrimitive()
                && (type.getAsString().equals("farmersdelight:cutting")
                || type.getAsString().equals("farmersdelight:cooking"));
    }

    @Unique
    private static boolean aoemod$hasSelectedNativeResult(JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        var object = recipe.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type == null || !type.isJsonPrimitive()) return false;
        ResourceLocation serializer = ResourceLocation.tryParse(type.getAsString());
        if (serializer == null || !net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS.containsKey(serializer)) return false;
        return aoemod$selectedResult(object.get("result"));
    }

    @Unique
    private static boolean aoemod$selectedResult(JsonElement result) {
        if (result == null) return false;
        if (result.isJsonArray()) {
            for (JsonElement child : result.getAsJsonArray()) if (aoemod$selectedResult(child)) return true;
            return false;
        }
        if (result.isJsonObject()) result = result.getAsJsonObject().get("item");
        if (result == null || !result.isJsonPrimitive() || !result.getAsJsonPrimitive().isString()) return false;
        ResourceLocation id = ResourceLocation.tryParse(result.getAsString());
        if (id == null || id.getNamespace().equals("minecraft")
                || !net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id)) return false;
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
        // Restore the building recipes needed for catalog parity without
        // reintroducing native alternatives to the pack's tuned food recipes.
        return item instanceof net.minecraft.world.item.BlockItem
                && RetainedContent.isSelectedItem(new net.minecraft.world.item.ItemStack(item));
    }

    @Unique
    private static boolean aoemod$hasExcludedResult(JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        JsonElement result = recipe.getAsJsonObject().get("result");
        if (result == null) return false;
        return aoemod$excludedResult(result);
    }

    @Unique
    private static boolean aoemod$excludedResult(JsonElement result) {
        if (result.isJsonArray()) {
            for (JsonElement element : result.getAsJsonArray()) if (aoemod$excludedResult(element)) return true;
            return false;
        }
        if (result.isJsonObject()) result = result.getAsJsonObject().get("item");
        if (result == null || !result.isJsonPrimitive() || !result.getAsJsonPrimitive().isString()) return false;
        ResourceLocation id = ResourceLocation.tryParse(result.getAsString());
        return id != null && (FutureContentPolicy.isExcludedItem(id) || RetainedContent.isExplicitlyRemovedItem(id));
    }

    @Unique
    private static boolean aoemod$usesRemovedIngredient(JsonElement element) {
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (aoemod$usesRemovedIngredient(entry.getValue())) return true;
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (aoemod$usesRemovedIngredient(child)) return true;
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            return RetainedContent.isRemovedOrcItem(id) || RetainedContent.isRemovedOuterEndItem(id)
                    || RetainedContent.isRemovedLavaFishFood(id) || RetainedContent.isRemovedBygBlock(id);
        }
        return false;
    }

    @Unique
    private static boolean aoemod$isAllowedRecipeNamespace(ResourceLocation id) {
        String namespace = id.getNamespace();
        return namespace.equals("minecraft") || namespace.equals(AOEMod.MOD_ID) || namespace.equals("quark")
                || namespace.equals("vanillabackport") || namespace.equals("tiny_takeover_backport")
                || namespace.equals("orcz")
                || namespace.equals("hearthandharvest") && id.getPath().equals("string_from_cotton");
    }
}
