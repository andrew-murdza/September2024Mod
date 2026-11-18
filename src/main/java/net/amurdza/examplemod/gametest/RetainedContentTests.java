package net.amurdza.examplemod.gametest;

import com.google.gson.JsonParser;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetainedContentTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void selectionUsesRealRegistryNames(GameTestHelper h) throws Exception {
        try (var stream = RetainedContent.class.getResourceAsStream("/aoemod-retained-content.json")) {
            var document = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> missing = new ArrayList<>();
            document.getAsJsonObject("items").entrySet().forEach(group -> group.getValue().getAsJsonArray().forEach(value -> {
                String path = value.getAsString();
                if (!path.contains("*")) {
                    var id = new ResourceLocation(group.getKey(), path);
                    if (!BuiltInRegistries.ITEM.containsKey(id) && !BuiltInRegistries.BLOCK.containsKey(id)) missing.add(id.toString());
                }
            }));
            h.assertTrue(missing.isEmpty(), "Retained selection contains unknown registry IDs: " + missing);
        }
        List<String> kept = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (RetainedContent.isKeptItem(new ItemStack(item))) kept.add(BuiltInRegistries.ITEM.getKey(item).toString());
        });
        kept.sort(String::compareTo);
        Files.createDirectories(Path.of("build/reports"));
        Files.write(Path.of("build/reports/retained-items.txt"), kept);
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void menusRetainRequestedEggsBucketsAndHideUnselectedContent(GameTestHelper h) {
        for (String id : List.of("pineapple_delight:pineapple", "vintagedelight:peanut", "wildernature:turkey_meat",
                "hybrid_aquatic:tuna", "alexscaves:tripodfish_bucket", "finsandtails:golden_river_ray_bucket",
                "ambientadditions:blue_spotted_stingray_bucket", "upgrade_aquatic:squid_bucket",
                "upgrade_aquatic:glow_squid_bucket", "biodiverse:raw_venison", "quark:paper_wall", "quark:chute", "aoemod:plum")) {
            var key = new ResourceLocation(id);
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(key), "Missing retained item " + id);
            h.assertTrue(RetainedContent.isKeptItem(new ItemStack(BuiltInRegistries.ITEM.get(key))), "Hidden retained item " + id);
        }
        // Chutes use retained planks and sticks, so the recipe-derived catalog keeps them.
        for (String id : List.of("pineapple_delight:pineapple_juice", "environmental:plum",
                "wildernature:bison_spawn_egg", "hybrid_aquatic:trout_spawn_egg")) {
            h.assertFalse(RetainedContent.isKeptItem(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)))),
                    "Unselected content must be absent from creative menus: " + id);
        }
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof SpawnEggItem egg) {
                ItemStack stack = new ItemStack(egg);
                var id = BuiltInRegistries.ITEM.getKey(item);
                if (!net.amurdza.examplemod.future.FutureContentPolicy.isExcludedItem(id))
                    h.assertTrue(RetainedContent.isKeptItem(stack) == (RetainedContent.isKeptEntity(egg.getType(null)) && !RetainedContent.isExplicitlyRemovedItem(id)),
                            "Spawn egg must follow its actual entity: " + id);
            }
        });
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void rejectedTapirsRiverHogsAndDucksAreHiddenAndCannotSpawn(GameTestHelper h) {
        var level = h.getLevel();
        var parameters = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        for (String name : List.of("ambientadditions:red_river_hog")) {
            var id = new ResourceLocation(name);
            h.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(id) && !RetainedContent.isKeptEntity(id),
                    "Rejected animal must be filtered from the source registry: " + id);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            var eggId = new ResourceLocation(id.getNamespace(), id.getPath() + "_spawn_egg");
            var item = ForgeRegistries.ITEMS.getValue(eggId);
            h.assertTrue(item instanceof SpawnEggItem && ((SpawnEggItem) item).getType(null) == type
                            && !RetainedContent.isKeptItem(new ItemStack(item)),
                    "Rejected animal must not have a usable spawn egg: " + eggId);
            var catalog = ModCreativeTabs.catalogs().get(id.getNamespace()).get();
            catalog.buildContents(parameters);
            h.assertTrue(catalog.getDisplayItems().stream().noneMatch(stack -> stack.is(item)),
                    "Rejected animal's spawn egg must not appear in its source creative menu: " + eggId);
            var entity = type.spawn(level, pos, MobSpawnType.SPAWN_EGG);
            h.assertTrue(entity == null || entity.isRemoved() || level.getEntity(entity.getUUID()) == null,
                    "The retained-spawn filter must reject the removed animal: " + id);
            if (entity != null) entity.discard();
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void requestedDimensionMasksOverrideOldSources(GameTestHelper h) {
        expect(h, "pineapple_delight:pineapple_wild_crop", 1);
        expect(h, "vintagedelight:wild_peanuts", 16);
        expect(h, "ecologics:prickly_pear", 16);
        expect(h, "ecologics:azalea_planks", 1);
        expect(h, "hearthandharvest:corn_stalk", 3);
        expect(h, "biomeswevegone:sandy_farmland", 16);
        expect(h, "biomeswevegone:japanese_orchid", 1);
        expect(h, "biomesoplenty:pink_daffodil", 1);
        expect(h, "farmersdelight:onions", 0);
        expect(h, "nourished_nether:smooth_basalt_stairs", 56);
        expect(h, "nourished_nether:soul_quartz_stairs", 48);
        expect(h, "nourished_nether:netherrack_slab", 32);
        expect(h, "netherdepthsupgrade:lava_glass", 56);
        expect(h, "netherdepthsupgrade:warped_kelp", 32);
        expect(h, "alexscaves:bioluminescent_torch", 8);
        expect(h, "betterend:charnia_light_blue", 8);
        expect(h, "quark:paper_wall", 15);
        expect(h, "quark:gold_bars", 60);
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nullablePlacementOverridesPreserveUnrelatedBlockFallbacks(GameTestHelper h) {
        h.assertTrue(RetainedContent.placementMask(null) == null, "Missing registry IDs must use the existing placement policy");
        for (String namespace : RetainedContent.namespaces()) {
            Integer mask = RetainedContent.placementMask(new ResourceLocation(namespace, "aoemod_no_explicit_override"));
            if (namespace.equals("orcz") || namespace.equals("outer_end")) {
                h.assertTrue(java.util.Objects.equals(mask, 0), "Unused content from a pruned dependency must remain excluded: " + namespace);
            } else h.assertTrue(mask == null,
                    "An unrelated block must fall through without unboxing a null override: " + namespace);
        }
        // Exercise the loaded dependency registry, including Hearth and Harvest's unrelated blocks.
        // Startup consults this same optional policy for every registered block.
        BuiltInRegistries.BLOCK.keySet().forEach(RetainedContent::placementMask);
        h.assertTrue(java.util.Objects.equals(RetainedContent.placementMask(new ResourceLocation("hearthandharvest:corn_stalk")), 3),
                "Corn must retain the Tropical and Savanna override");
        for (String path : List.of("cotton", "wild_cotton", "cotton_bale")) {
            h.assertTrue(java.util.Objects.equals(RetainedContent.placementMask(new ResourceLocation("hearthandharvest", path)), 1),
                    "Cotton must retain its Tropical override: " + path);
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sandyFarmlandCanOnlyBeCreatedByHoeingUncoveredDesertSand(GameTestHelper h) {
        var desert = h.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var plains = h.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        BlockPos pos = new BlockPos(8, 230, 8);
        for (var level : List.of(desert, plains)) {
            var player = FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            Items.IRON_HOE.useOn(context);
            String result = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            h.assertTrue(result.equals(level == desert ? "biomeswevegone:sandy_farmland" : "minecraft:sand"),
                    "Sand hoe result in " + level.dimension().location() + ": " + result);
            level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
            Items.IRON_HOE.useOn(context);
            h.assertTrue(level.getBlockState(pos).is(Blocks.SAND), "Covered sand must not be tilled");
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void dedicatedTabsContainOnlyTheirRetainedItems(GameTestHelper h) {
        var parameters = new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        for (var holder : List.of(ModCreativeTabs.UPGRADE_AQUATIC, ModCreativeTabs.ENVIRONMENTAL, ModCreativeTabs.QUARK)) {
            var tab = holder.get();
            tab.buildContents(parameters);
            h.assertTrue(!tab.getDisplayItems().isEmpty(), "Curated tab must not be empty: " + holder.getId());
            tab.getDisplayItems().forEach(stack -> h.assertTrue(RetainedContent.isKeptItem(stack), "Unexpected creative item " + stack));
        }
        var tomes = ModCreativeTabs.QUARK.get().getDisplayItems().stream()
                .filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("quark:ancient_tome")).toList();
        h.assertTrue(!tomes.isEmpty() && tomes.stream().allMatch(stack ->
                        org.violetmoon.quark.content.tools.module.AncientTomesModule.getTomeEnchantment(stack) != null),
                "The Quark tab must expose usable enchanted tome variants, not empty base items");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void removedModsAreAbsent(GameTestHelper h) {
        for (String namespace : List.of("born_in_chaos_v1", "untamedwilds", "naturalist", "bountiful_critters",
                "dtmeat", "dropthemeat", "ferdinandsflowers")) {
            h.assertFalse(ModList.get().isLoaded(namespace), "Removed mod remains loaded: " + namespace);
            h.assertFalse(BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals(namespace)),
                    "Removed mod still registers items: " + namespace);
        }
        h.succeed();
    }

    private static void expect(GameTestHelper h, String name, int mask) {
        var id = new ResourceLocation(name);
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Missing requested block: " + id);
        var block = BuiltInRegistries.BLOCK.get(id);
        h.assertTrue(BiomePlacementRestrictions.allowedMask(block) == mask, "Wrong dimensions for " + id);
        for (var key : List.of(GameplayDimensions.TROPICAL, GameplayDimensions.SAVANNA, GameplayDimensions.PLAINS,
                GameplayDimensions.SNOWY, GameplayDimensions.DESERT, GameplayDimensions.NETHER)) {
            var level = h.getLevel().getServer().getLevel(key);
            int dimension = BiomePlacementRestrictions.dimensionMask(level);
            if ((dimension & mask) == 0) h.assertFalse(BiomePlacementRestrictions.isAllowed(level, new BlockPos(0, 180, 0), block.defaultBlockState()),
                    id + " must not be placeable in " + key.location());
        }
    }
}
