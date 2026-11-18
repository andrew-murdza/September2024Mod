package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.MonsterEcology;
import net.amurdza.examplemod.future.FutureContentPolicy;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.SoulCaveClock;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import twilightforest.entity.ai.goal.RedcapLightTNTGoal;
import twilightforest.entity.ai.goal.RedcapPlantTNTGoal;
import twilightforest.entity.monster.Redcap;
import twilightforest.entity.monster.UpperGoblinKnight;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void selectedOrczContentAndOverworldGenerationLoad(GameTestHelper helper) throws java.io.IOException {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        var registry = access.registryOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS);
        var biomes = access.registryOrThrow(Registries.BIOME);
        var plains = biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        var savanna = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod:savanna")));
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access);
        var identities = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<net.minecraftforge.common.world.BiomeModifier, Boolean>());
        for (String name : List.of("blue_orcshroom", "camp", "camp_the_other", "fakeleaf", "faketree",
                "orcagaric", "orcamanita", "savagegoblin", "savageorc", "savageorciron")) {
            var id = new ResourceLocation("orcz", name + "_biome_modifier");
            var loaded = registry.get(id);
            if (loaded instanceof net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier feature) {
                helper.assertTrue(feature.biomes().contains(plains) && !feature.biomes().contains(savanna)
                                && feature.features().size() > 0,
                        "Original Orcz features must generate in the Overworld, outside AOE Savanna: " + id);
                helper.assertTrue(plains.value().getGenerationSettings().features().stream().flatMap(set -> set.stream())
                                .anyMatch(value -> value.is(new ResourceLocation("orcz", name))),
                        "Loaded Overworld generation must contain the original Orcz feature: " + id);
            } else {
                helper.assertTrue(loaded instanceof net.minecraftforge.common.world.ForgeBiomeModifiers.AddSpawnsBiomeModifier,
                        "Original Orcz spawn modifier must load: " + id);
                var spawns = (net.minecraftforge.common.world.ForgeBiomeModifiers.AddSpawnsBiomeModifier) loaded;
                helper.assertTrue(spawns.biomes().contains(plains) && !spawns.biomes().contains(savanna)
                                && !spawns.spawners().isEmpty(),
                        "Native Orcz natural spawns must be enabled in Overworld biomes: " + id);
            }
            helper.assertTrue(identities.add(loaded), "Modifiers need independent registry values: " + id);
            var file = new ResourceLocation("orcz", "forge/biome_modifier/" + id.getPath() + ".json");
            try (var reader = server.getResourceManager().getResource(file).orElseThrow().openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader);
                for (int reload = 0; reload < 2; reload++) {
                    var decoded = net.minecraftforge.common.world.BiomeModifier.DIRECT_CODEC.parse(ops, json).result()
                            .orElseThrow(() -> new AssertionError("Cannot reload Orcz modifier " + id));
                    helper.assertTrue(identities.add(decoded), "A registry reload reused a biome modifier object: " + id);
                }
            }
        }
        int orcEggs = 0;
        Set<String> selectedOrcs = Set.of("orcz:orcsavagemace", "orcz:orcshield");
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            if (!entry.getKey().location().getNamespace().equals("orcz")
                    || !(entry.getValue() instanceof net.minecraft.world.item.SpawnEggItem)) continue;
            orcEggs++;
            var egg = (net.minecraft.world.item.SpawnEggItem) entry.getValue();
            boolean selected = selectedOrcs.contains(ForgeRegistries.ENTITY_TYPES.getKey(egg.getType(null)).toString());
            helper.assertTrue(net.amurdza.examplemod.util.RetainedContent.isKeptItem(new ItemStack(egg)) == selected,
                    "Only the chosen native Orcz spawn eggs may be available: " + entry.getKey().location());
        }
        helper.assertTrue(orcEggs == 26, "All 26 native Orcz eggs must remain installed");
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (entry.getKey().location().getNamespace().equals("orcz")) {
                helper.assertTrue(net.amurdza.examplemod.util.RetainedContent.isKeptEntity(entry.getValue())
                                == selectedOrcs.contains(entry.getKey().location().toString()),
                        "Only the final selected Orcz entities may be available: " + entry.getKey().location());
            }
        }
        var recipes = server.getResourceManager().listResources("recipes", id -> id.getNamespace().equals("orcz")
                && id.getPath().endsWith(".json"));
        helper.assertTrue(!recipes.isEmpty(), "Orcz source recipes must be available");
        for (var file : recipes.keySet()) {
            var recipe = new ResourceLocation("orcz", file.getPath().substring("recipes/".length(), file.getPath().length() - 5));
            helper.assertTrue(server.getRecipeManager().byKey(recipe).isPresent(), "Original Orcz recipe must remain enabled: " + recipe);
        }
        helper.assertTrue(!net.minecraftforge.fml.ModList.get().isLoaded("iceandfire")
                        && !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation("aoemod:archlich")),
                "Ice and Fire and its unused Archlich integration must be removed");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void orcBossHealthSurvivesReloadWithoutHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("orcz:bigorcboss"));
        var ordinary = (net.minecraft.world.entity.Mob) type.create(level);
        float originalMaximum = ordinary.getMaxHealth();
        net.amurdza.examplemod.event_handlers.OrcBossEncounter.joined(new EntityJoinLevelEvent(ordinary, level));
        helper.assertTrue(ordinary.getMaxHealth() == originalMaximum, "Unmarked command/egg orcs retain their source-mod balance");
        var boss = (net.minecraft.world.entity.Mob) type.create(level);
        net.amurdza.examplemod.util.StructureSpawnedMobData.mark(boss, boss.blockPosition());
        net.amurdza.examplemod.event_handlers.OrcBossEncounter.joined(new EntityJoinLevelEvent(boss, level));
        helper.assertTrue(boss.getMaxHealth() == 200.0F && boss.getHealth() == 200.0F, "The camp boss must start at 200 HP");
        boss.setHealth(53.0F);
        var saved = boss.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        net.amurdza.examplemod.event_handlers.OrcBossEncounter.left(new net.minecraftforge.event.entity.EntityLeaveLevelEvent(boss, level));
        var restored = (net.minecraft.world.entity.Mob) type.create(level);
        restored.load(saved);
        net.amurdza.examplemod.event_handlers.OrcBossEncounter.joined(new EntityJoinLevelEvent(restored, level));
        helper.assertTrue(restored.getMaxHealth() == 200.0F && restored.getHealth() == 53.0F,
                "Reloading a camp boss must retain its health and must not heal it");
        net.amurdza.examplemod.event_handlers.OrcBossEncounter.left(new net.minecraftforge.event.entity.EntityLeaveLevelEvent(restored, level));
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sandOnlyDesertFaunaAndPowderCaneClearance(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var position = new net.minecraft.core.BlockPos(0, 2, 0);
        var states = new java.util.HashMap<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
        java.util.function.Function<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> state =
                pos -> states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        var level = (net.minecraft.world.level.ServerLevelAccessor) java.lang.reflect.Proxy.newProxyInstance(
                MobRevisionTests.class.getClassLoader(), new Class<?>[]{net.minecraft.world.level.ServerLevelAccessor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> desert;
                    case "getBlockState" -> state.apply((net.minecraft.core.BlockPos) args[0]);
                    case "getFluidState" -> state.apply((net.minecraft.core.BlockPos) args[0]).getFluidState();
                    default -> method.invoke(desert, args);
                });
        for (String id : List.of("minecraft:camel", "minecraft:rabbit")) {
            var species = net.amurdza.examplemod.worldgen.DocumentedFauna.forBiome("desert").stream()
                    .filter(value -> value.id().toString().equals(id)).findFirst().orElseThrow();
            for (var support : List.of(Blocks.TERRACOTTA, Blocks.RED_SAND, Blocks.GRASS_BLOCK)) {
                states.put(position.below(), support.defaultBlockState());
                helper.assertFalse(net.amurdza.examplemod.worldgen.DocumentedFauna.hasSpace(level, position, species),
                        id + " must reject " + support);
            }
            states.put(position.below(), Blocks.SAND.defaultBlockState());
            helper.assertTrue(net.amurdza.examplemod.worldgen.DocumentedFauna.hasSpace(level, position, species), id + " must accept sand");
        }
        var powder = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight:powdery_cane"));
        states.put(position.above(), powder.defaultBlockState());
        helper.assertFalse(net.amurdza.examplemod.worldgen.SpawnCollisionSafety.hasNoPowderCane(level,
                        new net.minecraft.world.phys.AABB(0, 2, 0, 1, 4, 1)),
                "Spawn bounds must reject a powder cane stalk even above the feet");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void exclusiveMonsterRostersAndQuietCaves(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String name : List.of("rainforest", "mangrove_swamp", "cherry_grove", "coral_reef", "savanna", "plains",
                "snowy_forest", "dappled_forest", "mushroom_caves", "desert", "mesa", "deep_dark",
                "soul_sand_valley", "warped_forest", "crimson_forest", "basalt_deltas")) {
            var biome = biomes.getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", name)));
            Set<String> actual = biome.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                    .map(entry -> String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entry.type))).collect(Collectors.toSet());
            Set<String> expected = Set.copyOf(DocumentedMonsters.forBiome(name));
            helper.assertTrue(actual.equals(expected), name + " monster roster differs: " + actual + " expected " + expected);
        }
        for (String id : List.of("aoemod:spider_queen", "aoemod:illager_lord", "twilightforest:alpha_yeti", "orcz:bigorcboss")) {
            helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(id)), "Required boss must be installed: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void knightArmorAndRedcapTnt(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        UpperGoblinKnight knight = (UpperGoblinKnight) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("twilightforest:upper_goblin_knight")).create(level);
        MonsterEcology.joined(new EntityJoinLevelEvent(knight, level));
        helper.assertTrue(knight.getArmorValue() == 20, "Retain the native armored knight");
        var breakArmor = UpperGoblinKnight.class.getDeclaredMethod("breakArmor");
        breakArmor.setAccessible(true);
        breakArmor.invoke(knight);
        helper.assertTrue(knight.hasArmor() && knight.getArmorValue() == 20, "Back hits must no longer break the armor");
        helper.assertFalse(knight.isPassenger() || knight.isVehicle(), "Dappled knights must be single entities");
        var breakShield = UpperGoblinKnight.class.getDeclaredMethod("breakShield");
        breakShield.setAccessible(true);
        breakShield.invoke(knight);
        helper.assertTrue(knight.hasShield(), "The shield must survive rear hits and dismounting");
        var saved = new net.minecraft.nbt.CompoundTag();
        knight.addAdditionalSaveData(saved);
        saved.putBoolean("hasArmor", false);
        saved.putBoolean("hasShield", false);
        knight.readAdditionalSaveData(saved);
        helper.assertTrue(knight.hasArmor() && knight.getArmorValue() == 20 && knight.hasShield(),
                "Previously stripped knights must recover native armor and shield on load");
        Redcap redcap = (Redcap) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("twilightforest:redcap")).create(level);
        MonsterEcology.joined(new EntityJoinLevelEvent(redcap, level));
        helper.assertFalse(redcap.goalSelector.getAvailableGoals().stream().anyMatch(goal ->
                goal.getGoal() instanceof RedcapLightTNTGoal || goal.getGoal() instanceof RedcapPlantTNTGoal),
                "Redcaps must neither plant nor light TNT");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void drownedAreUnarmedAndWitherRespectsOtherMobs(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var drowned = EntityType.DROWNED.create(tropical);
        drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        drowned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        MonsterEcology.joined(new EntityJoinLevelEvent(drowned, tropical));
        for (EquipmentSlot slot : EquipmentSlot.values()) helper.assertTrue(drowned.getItemBySlot(slot).isEmpty(), "Drowned equipment must be empty");
        helper.assertFalse(drowned.canPickUpLoot(), "Unarmed drowned must not rearm themselves");
        var level = helper.getLevel();
        WitherBoss wither = EntityType.WITHER.create(level);
        helper.assertFalse(wither.canAttack(EntityType.COW.create(level)), "Withers must not target livestock");
        helper.assertFalse(wither.canAttack(EntityType.MAGMA_CUBE.create(level)), "Withers must not target their support mobs");
        helper.assertFalse(wither.canAttack(EntityType.BLAZE.create(level)), "Withers must not target other cave monsters");
        helper.assertTrue(wither.canAttack(EntityType.VILLAGER.create(level)), "Villagers remain valid wither targets");
        helper.assertTrue(wither.canAttack(EntityType.PIGLIN.create(level)), "Piglins remain valid wither targets");
        helper.assertFalse(WitherBoss.canDestroy(Blocks.OBSIDIAN.defaultBlockState()), "Wither lair obsidian must be immune");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeCreakingHeartAndUndergroundNight(GameTestHelper helper) {
        var heart = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:creaking_heart"));
        helper.assertTrue(heart instanceof CreakingHeartBlock,
                "Native creaking heart behavior must be available");
        var heartPos = new net.minecraft.core.BlockPos(2, 3, 2);
        var log = net.amurdza.examplemod.registry.ModBlocks.PALE_OAK_LOG.get();
        helper.setBlock(heartPos.below(), log);
        helper.setBlock(heartPos, heart);
        helper.setBlock(heartPos.above(), log);
        // Activation checks the two aligned trunk logs. The similarly named
        // isSurroundedByLogs checks all six neighbors only for ambient sound.
        helper.assertTrue(CreakingHeartBlock.hasRequiredLogs(heart.defaultBlockState(), helper.getLevel(), helper.absolutePos(heartPos)),
                "Native hearts must recognize the custom soul-valley pale oak trunks");
        for (String id : List.of("creaking_heart", "creaking_spawn_egg", "resin_clump", "resin_block")) {
            helper.assertFalse(FutureContentPolicy.isExcludedItem(new ResourceLocation("minecraft", id)), "Requested heart/resin item must be enabled: " + id);
        }
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        // Custom dimensions use DerivedLevelData, whose time setters are no-ops.
        // Change the primary clock they actually read, then restore it in finally.
        var primary = helper.getLevel().getServer().overworld();
        long previous = primary.getDayTime();
        try {
            primary.setDayTime(6000L);
            helper.assertTrue(level.getDayTime() == 6000L, "The cave must inherit the primary noon clock");
            helper.assertFalse(SoulCaveClock.isNight(level), "Underground noon is day");
            primary.setDayTime(18000L);
            helper.assertTrue(level.getDayTime() == 18000L, "The cave must inherit the primary midnight clock");
            helper.assertTrue(SoulCaveClock.isNight(level), "Underground midnight is night");
            helper.assertTrue(com.blackgear.vanillabackport.core.util.WorldUtilities.EnvironmentUtils.isNaturalNight(level),
                    "Native heart activation must use the cave's day clock");
        } finally {
            primary.setDayTime(previous);
        }
        helper.succeed();
    }
}
