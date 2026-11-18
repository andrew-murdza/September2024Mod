package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ObtainableContent;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrczCatalogTests {
    private static final Set<String> RETAINED_ITEMS = Set.of("orcsavagemace_spawn_egg", "orcshield_spawn_egg", "bigorcboss_spawn_egg");

    @GameTest(template = "plant_gameplay_empty")
    public static void unusedOrcItemsCannotReturnThroughRecipesSourcesOrRegistryCatalogs(GameTestHelper helper) {
        var level = helper.getLevel();
        ObtainableContent.rebuild(level.getRecipeManager(), level.registryAccess());
        Set<String> catalog = CreativeCatalog.items("orcz")
                .map(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()))
                .filter(id -> id.getNamespace().equals("orcz"))
                .map(ResourceLocation::getPath).collect(Collectors.toSet());
        helper.assertTrue(catalog.equals(RETAINED_ITEMS),
                "Orcz discovery must expose only the three selected eggs: " + catalog);
        int retired = 0;
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("orcz")) continue;
            boolean kept = RETAINED_ITEMS.contains(id.getPath());
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(entry.getValue())) == kept
                            && RetainedContent.isExplicitlyRemovedItem(id) != kept,
                    "Direct registered-item discovery must agree with the selected catalog: " + id);
            if (kept) continue;
            retired++;
            helper.assertFalse(ObtainableContent.contains(entry.getValue()), "A removed item reappeared through recipes: " + id);
            if (entry.getValue() instanceof BlockItem block) {
                helper.assertTrue(DocumentedPlacementRules.sourceMask(block.getBlock()) == null
                                && BiomePlacementRestrictions.allowedMask(block.getBlock()) == 0,
                        "Unused Orcz blocks must no longer be allowed dimension sources: " + id);
            }
        }
        helper.assertTrue(retired > 50, "Unused Orcz IDs must remain registered for saved-world compatibility");
        for (var recipe : level.getRecipeManager().getRecipes()) {
            var result = ForgeRegistries.ITEMS.getKey(recipe.getResultItem(level.registryAccess()).getItem());
            helper.assertFalse(RetainedContent.isRemovedOrcItem(result), "Removed Orcz recipe result remains: " + recipe.getId());
            for (var ingredient : recipe.getIngredients()) {
                helper.assertTrue(ingredient.isEmpty() || Arrays.stream(ingredient.getItems()).anyMatch(stack ->
                                !RetainedContent.isRemovedOrcItem(ForgeRegistries.ITEMS.getKey(stack.getItem()))),
                        "A recipe still requires an unused Orcz item: " + recipe.getId());
            }
        }
        for (String recipe : Set.of("orcsymbolrecipe", "orcsymbolbluerecipe",
                "orc_wood_planks_recipe", "orc_wood_slab_recipe", "scrap_2block", "block_2scrap", "ironscraprecipe")) {
            helper.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("orcz", recipe)).isEmpty(),
                    "A retired camp material recipe remains: " + recipe);
        }
        for (String recipe : Set.of("orcteethrecipe", "orceyerecipe", "squigclawrecipe", "squigeyerecipe",
                "squigeyerep", "pallisaderes", "pallisadedarkres", "junglechestres", "orc_wood_stairs_recipe", "orcsymbolredrecipe")) {
            helper.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("orcz", recipe)).isEmpty(),
                    "An unused item or legacy-item conversion recipe remains: " + recipe);
        }
        var campLoot = level.getServer().getLootData().getLootTable(new ResourceLocation("orcz", "chests/camp"));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        for (int seed = 0; seed < 128; seed++) for (var stack : campLoot.getRandomItems(params, seed)) {
            helper.assertFalse(RetainedContent.isRemovedOrcItem(ForgeRegistries.ITEMS.getKey(stack.getItem())),
                    "Native camp loot must not reintroduce retired Orcz decorations");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void twoSelectedOrcsAndTheirCampBossRemainAvailableAndSpawnable(GameTestHelper helper) {
        Set<String> selected = Set.of("orcz:orcsavagemace", "orcz:orcshield", "orcz:bigorcboss");
        Set<String> natural = Set.of("orcz:orcsavagemace", "orcz:orcshield");
        var eggs = CreativeCatalog.items("orcz").filter(stack -> stack.getItem() instanceof SpawnEggItem).toList();
        var types = eggs.stream().map(stack -> ((SpawnEggItem) stack.getItem()).getType(stack.getTag())).collect(Collectors.toSet());
        int mobs = 0;
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (!entry.getKey().location().getNamespace().equals("orcz")) continue;
            boolean retained = selected.contains(entry.getKey().location().toString());
            helper.assertTrue(RetainedContent.isKeptEntity(entry.getValue()) == retained,
                    "Only the two chosen guards and restored boss may remain available: " + entry.getKey().location());
            var entity = entry.getValue().create(helper.getLevel());
            if (entity instanceof Mob) {
                mobs++;
                helper.assertTrue(types.contains(entry.getValue()) == retained,
                        "Creative egg availability must match the final roster: " + entry.getKey().location());
                for (boolean loaded : new boolean[]{false, true}) {
                    var event = new EntityJoinLevelEvent(entity, helper.getLevel(), loaded);
                    RetainedMobSpawns.joined(event);
                    helper.assertTrue(event.isCanceled() != retained, "Spawn/load filtering disagrees with selection: " + entry.getKey().location());
                }
            }
            if (entity != null) entity.discard();
        }
        helper.assertTrue(mobs == 32 && eggs.size() == 3, "The installed source registry must expose the two guard eggs and boss egg");
        helper.assertTrue(DocumentedMonsters.forBiome("savanna").stream().filter(id -> id.startsWith("orcz:"))
                        .collect(Collectors.toSet()).equals(natural),
                "Only the two guards spawn naturally; the boss belongs to its camp");
        var savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA)
                .registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("aoemod:savanna"));
        Set<String> loaded = Arrays.stream(MobCategory.values()).flatMap(category -> savanna.getMobSettings().getMobs(category).unwrap().stream())
                .map(entry -> ForgeRegistries.ENTITY_TYPES.getKey(entry.type).toString()).filter(id -> id.startsWith("orcz:"))
                .collect(Collectors.toSet());
        helper.assertTrue(loaded.equals(natural), "Source-mod spawn additions must not leak into the loaded Savanna roster");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void walkingGoblinHasItsOwnVisibleSpawnEgg(GameTestHelper helper) {
        var egg = ForgeRegistries.ITEMS.getValue(new ResourceLocation("aoemod", "spawn_egg_goblin_knight"));
        var upper = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("twilightforest", "upper_goblin_knight"));
        helper.assertTrue(egg instanceof SpawnEggItem spawnEgg && spawnEgg.getType(null) == upper,
                "The restored egg must directly create the walking upper Goblin Knight");
        helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(egg))
                        && CreativeCatalog.items("twilightforest").anyMatch(stack -> stack.is(egg)),
                "The standalone Goblin Knight egg must remain discoverable in the Twilight Forest catalog");
        helper.succeed();
    }
}
