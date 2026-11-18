package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.event_handlers.BreedingItems;
import net.amurdza.examplemod.registry.DimensionFoods;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.TropicalUmbrellaVariants;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PackTrimFaunaFoodsTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void removedAoEItemsAreAbsentAndRetainedFoodsRemainCookable(GameTestHelper helper) {
        for (String name : new String[]{"bone_bucket", "bone_water_bucket", "bone_lava_bucket", "nether_fish",
                "bucket_nether_fish", "spawn_egg_nether_fish", "soul_berry_cookie", "seared_cod_roll", "nether_fish_roll"}) {
            helper.assertFalse(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(AOEMod.MOD_ID, name)),
                    "Removed AoE item must be absent from the registry: " + name);
        }
        helper.assertFalse(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(AOEMod.MOD_ID, "nether_fish")),
                "Removed Nether Fish entity must be absent from the registry");
        helper.assertTrue(ModItems.BOSS_AXE.isPresent() && ModItems.MACE.isPresent() && ModItems.BREEZE_ROD.isPresent(),
                "The boss axe and custom features without a native replacement must stay registered");
        for (String[] pair : new String[][]{
                {"wildernature:turkey_meat", "wildernature:cooked_turkey_meat"},
                {"wildernature:cassowary_meat", "wildernature:cooked_cassowary_meat"},
                {"wildernature:bison_meat", "wildernature:cooked_bison_meat"},
                {"wildernature:venison", "wildernature:cooked_venison"},
                {"wildernature:pelican_meat", "wildernature:cooked_pelican_meat"},
                {"biodiverse:raw_venison", "biodiverse:cooked_venison"},
                {"alexsmobs:kangaroo_meat", "alexsmobs:cooked_kangaroo_meat"},
                {"alexsmobs:emu_egg", "alexsmobs:boiled_emu_egg"},
                {"alexscaves:tripodfish", "alexscaves:cooked_tripodfish"},
                {"finsandtails:raw_golden_river_ray_wing", "finsandtails:golden_river_ray_wing_fillet"},
                {"ecologics:crab_meat", "aoemod:cooked_crab_meat"},
                {"alexsmobs:lost_tentacle", "aoemod:cooked_lost_tentacle"},
                {"culturaldelights:squid", "culturaldelights:cooked_squid"},
                {"culturaldelights:glow_squid", "culturaldelights:cooked_squid"},
                {"outer_end:stalker_meat", "outer_end:cooked_stalker_meat"},
                {"aoemod:raw_umbrella_octopus", "aoemod:cooked_umbrella_octopus"}}) {
            var raw = ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair[0]));
            var cooked = ForgeRegistries.ITEMS.getValue(new ResourceLocation(pair[1]));
            helper.assertTrue(raw != null && raw != Items.AIR && cooked != null && cooked != Items.AIR && cooked.isEdible(),
                    "Retained food pair must resolve to edible registered items: " + pair[0]);
            var input = new SimpleContainer(new ItemStack(raw));
            for (var type : java.util.List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
                helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(type).stream()
                                .anyMatch(recipe -> recipe.matches(input, helper.getLevel())
                                        && recipe.getResultItem(helper.getLevel().registryAccess()).is(cooked)),
                        "Retained food must remain cookable after namespace filtering: " + pair[0] + " / " + type);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void retiredModsAreAbsentAndRequestedLivestockReplacementsAreWired(GameTestHelper helper) {
        for (String mod : new String[]{"born_in_chaos_v1", "untamedwilds", "naturalist",
                "bountiful_critters", "dropthemeat", "dtmeat", "ferdinandsflowers", "culturaldelights"}) {
            helper.assertFalse(ModList.get().isLoaded(mod), "Retired mod must not be loaded: " + mod);
        }
        helper.assertTrue(DocumentedFauna.forBiome("snowy_forest").stream()
                        .anyMatch(species -> species.id().toString().equals("biodiverse:reindeer"))
                        && DocumentedFauna.forBiome("snowy_forest").stream()
                        .noneMatch(species -> species.id().toString().equals("environmental:reindeer")),
                "Snowy forest must use the retained Biodiverse reindeer");
        helper.assertTrue(ModList.get().isLoaded("outer_end")
                        && DocumentedFauna.forBiome("deep_dark").stream()
                        .anyMatch(species -> species.id().toString().equals("outer_end:stalker")),
                "The restored Stalker must have its exclusive deep-dark fauna source");
        helper.assertTrue(DocumentedFauna.forBiome("warped_forest").stream()
                        .anyMatch(species -> species.id().toString().equals("minecraft:strider"))
                        && DocumentedFauna.forBiome("warped_forest").stream()
                        .noneMatch(species -> species.id().toString().equals("alexsmobs:warped_toad")),
                "Striders must replace warped toads in the warped forest");
        for (String biome : new String[]{"desert", "mesa", "deep_dark"}) {
            helper.assertTrue(DocumentedFauna.forBiome(biome).stream().noneMatch(species ->
                            species.id().toString().equals("alexsmobs:jerboa")
                                    || species.id().getNamespace().equals("outer_end") && !species.id().getPath().equals("stalker")),
                    "Removed desert livestock must not remain in the natural roster");
        }
        var cassowary = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("biodiverse:cassowary"));
        var turkey = ForgeRegistries.ITEMS.getValue(new ResourceLocation("wildernature:turkey_meat"));
        var cooked = ForgeRegistries.ITEMS.getValue(new ResourceLocation("wildernature:cooked_turkey_meat"));
        helper.assertTrue(MobConfig.MOB_INFO_BY_MOB.get(cassowary).drops().stream()
                        .anyMatch(row -> row.item() == turkey && row.cookedItem() == cooked && row.tropical() == 2),
                "Cassowary must use retained turkey meat and its cooked counterpart");
        var arthropleuraRaw = ForgeRegistries.ITEMS.getValue(AOEMod.makeID("raw_arthropleura"));
        var arthropleuraCooked = ForgeRegistries.ITEMS.getValue(AOEMod.makeID("cooked_arthropleura"));
        helper.assertTrue(MobConfig.MOB_INFO_BY_MOB.get(ModEntities.ARTHROPLEURA.get()).drops().stream()
                        .anyMatch(row -> row.item() == arthropleuraRaw && row.cookedItem() == arthropleuraCooked && row.tropical() == 3),
                "Tropical arthropleura must drop their own meat");
        var capybara = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("biodiverse:capybara"));
        helper.assertTrue(MobConfig.MOB_INFO_BY_MOB.get(capybara).drops().stream()
                        .anyMatch(row -> row.item() == Items.BEEF && row.cookedItem() == Items.COOKED_BEEF && row.tropical() > 0),
                "Tropical capybaras must provide beef");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void netherLivestockUseTheirRequestedFoodsAndStridersRemainWarm(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var strider = EntityType.STRIDER.create(nether);
        strider.setPos(8, 80, 8);
        helper.assertTrue(strider.isFood(new ItemStack(Items.WARPED_FUNGUS))
                        && !strider.isFood(new ItemStack(Items.NETHER_WART)),
                "The native strider feeding path must use warped fungus exclusively");
        strider.setSuffocating(true);
        helper.assertFalse(strider.isSuffocating(), "Cold state and its movement penalty must stay disabled");
        feedBabyAndAdult(helper, strider, Items.WARPED_FUNGUS);
        helper.succeed();
    }

    private static void feedBabyAndAdult(GameTestHelper helper, Animal animal, net.minecraft.world.item.Item food) {
        var player = helper.makeMockSurvivalPlayer();
        animal.setAge(-24000);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(food, 2));
        var babyUse = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, animal);
        BreedingItems.extraOrCancelBreeding(babyUse);
        helper.assertTrue(babyUse.isCanceled() && animal.getAge() > -24000 && player.getMainHandItem().getCount() == 1,
                "Requested food must mature the baby and consume one item: " + animal.getType());
        animal.setAge(0);
        var adultUse = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, animal);
        BreedingItems.extraOrCancelBreeding(adultUse);
        helper.assertTrue(adultUse.isCanceled() && animal.isInLove() && player.getMainHandItem().isEmpty(),
                "Requested food must breed the adult and consume one item: " + animal.getType());
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalUmbrellaFilterMatchesTheInstalledUuidPaletteWithoutMutatingIdentity(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:umbrella_octopus"));
        var octopus = type.create(tropical);
        var control = type.create(helper.getLevel());
        int excluded = 0;
        for (long seed = 0; seed < 96; seed++) {
            UUID uuid = new UUID(125, seed);
            octopus.setUUID(uuid);
            control.setUUID(uuid);
            boolean brown = Math.floorMod(seed, 5) == 1;
            helper.assertTrue(TropicalUmbrellaVariants.excludedNaturalVariant(octopus) == brown
                            && !TropicalUmbrellaVariants.excludedNaturalVariant(control) && octopus.getUUID().equals(uuid),
                    "Only the Tropical brown palette member may be excluded, with persistent identity intact");
            helper.assertFalse(TropicalUmbrellaVariants.color(octopus).equals("brown"),
                    "Existing tropical individuals must also stop displaying the excluded color");
            if (brown) excluded++;
        }
        helper.assertTrue(excluded > 0 && excluded < 96, "Palette coverage must include kept and removed colors");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void desertFiddlersKeepOnlyRedNaturalSpawnsAndWitherSkeletonsCarryGold(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var crab = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:fiddler_crab")).create(desert);
        var lake = AquaticPlantGenerationTests.fixture(helper, "desert_lake", new java.util.HashMap<>(), 4, desert);
        var cave = AquaticPlantGenerationTests.fixture(helper, "deep_dark", new java.util.HashMap<>(), 4, desert);
        int red = 0;
        for (long seed = 0; seed < 48; seed++) {
            crab.setUUID(new UUID(271, seed));
            boolean isRed = kotlin.random.RandomKt.Random(seed).nextInt(3) == 2;
            helper.assertTrue(net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns.excludedNaturalVariant(lake, crab) != isRed
                            && !net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns.excludedNaturalVariant(cave, crab),
                    "Only the red UUID palette entry may spawn in the desert lake");
            if (isRed) red++;
        }
        helper.assertTrue(red > 0 && red < 48, "The sample must exercise both kept and excluded crabs");
        var skeleton = EntityType.WITHER_SKELETON.create(desert);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        net.amurdza.examplemod.event_handlers.MobTierBalancing.onMobJoin(new net.minecraftforge.event.entity.EntityJoinLevelEvent(skeleton, desert));
        helper.assertTrue(skeleton.getMainHandItem().is(Items.GOLDEN_SWORD), "New and previously iron-equipped skeletons must receive gold");
        helper.succeed();
    }
}
