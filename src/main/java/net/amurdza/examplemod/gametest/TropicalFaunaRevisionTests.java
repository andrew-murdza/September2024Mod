package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.worldgen.TropicalIslandPlan;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.amurdza.examplemod.worldgen.TropicalMoundFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalFaunaRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void tropicalAquaticPalettesAndCategoriesMatchAcrossBiomes(GameTestHelper helper) {
        Set<String> reef = new HashSet<>();
        for (var species : DocumentedFauna.forBiome("coral_reef")) {
            helper.assertTrue(species.type() != null, "Missing requested installed entity " + species.id());
            helper.assertTrue(species.habitat() != DocumentedFauna.Habitat.LAND, "Reef fauna must spawn in water");
            reef.add(species.id().toString());
        }
        helper.assertTrue(reef.equals(Set.of("minecraft:tropical_fish", "minecraft:pufferfish", "angling:anomalocaris",
                        "minecraft:axolotl", "hybrid_aquatic:umbrella_octopus",
                        "hybrid_aquatic:coconut_crab", "minecraft:turtle", "hybrid_aquatic:lightfoot_crab",
                        "hybrid_aquatic:starfish", "aoemod:nautilus", "minecraft:dolphin", "angling:sea_slug")),
                "Reef must contain exactly the twelve requested aquatic species");
        for (String biome : List.of("cherry_grove", "mangrove_swamp", "rainforest")) {
            Set<String> expected = new HashSet<>(reef);
            expected.remove("aoemod:nautilus");
            if (!biome.equals("mangrove_swamp")) expected.clear();
            Set<String> actual = new HashSet<>();
            DocumentedFauna.forBiome(biome).stream().filter(s -> s.habitat() != DocumentedFauna.Habitat.LAND)
                    .forEach(s -> actual.add(s.id().toString()));
            helper.assertTrue(expected.equals(actual), "Incorrect aquatic palette for " + biome);
        }
        helper.assertTrue(reef.contains("aoemod:nautilus"), "Nautilus must remain available in the coral reef");
        for (String biome : List.of("snowy_forest", "mushroom_caves")) helper.assertTrue(
                DocumentedFauna.forBiome(biome).stream().noneMatch(species -> species.id().toString().equals("aoemod:nautilus")),
                "The explicit snowy roster must exclude nautiluses in " + biome);
        for (String biomeName : List.of("coral_reef", "mangrove_swamp", "rainforest", "cherry_grove")) {
            var biome = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .get(new ResourceLocation("aoemod", biomeName));
            Set<String> expected = new HashSet<>();
            DocumentedFauna.forBiome(biomeName).forEach(species -> expected.add(species.id().toString()));
            expected.addAll(net.amurdza.examplemod.worldgen.DocumentedMonsters.forBiome(biomeName));
            Set<String> actual = new HashSet<>();
            for (MobCategory category : MobCategory.values()) biome.getMobSettings().getMobs(category).unwrap()
                    .forEach(entry -> actual.add(ForgeRegistries.ENTITY_TYPES.getKey(entry.type).toString()));
            helper.assertTrue(actual.equals(expected), "Final biome spawn table leaked or omitted species in " + biomeName + ": " + actual);
        }
        helper.assertTrue(EntityType.AXOLOTL.getCategory() == MobCategory.WATER_CREATURE
                && EntityType.TURTLE.getCategory() == MobCategory.WATER_CREATURE, "Axolotls and turtles must use aquatic mob caps");
        for (var species : DocumentedFauna.forBiome("mangrove_swamp")) helper.assertFalse(
                Set.of("biodiverse:capybara", "minecraft:frog", "wildernature:flamingo").contains(species.id().toString()),
                "Mound animals must not re-enter random natural spawning");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void eightSeededShoreGroupsEachReceiveTwoAdults(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (int seed = 0; seed < 8; seed++) {
            var layout = new TropicalLayout(seed);
            var plan = TropicalIslandPlan.forIsland(layout, layout.island(0, 0));
            var counts = new HashMap<String, Integer>();
            List<Entity> spawned = new ArrayList<>();
            WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(TropicalFaunaRevisionTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getLevel" -> tropical;
                        case "getHeight" -> args == null || args.length == 0 ? tropical.getHeight() : 65;
                        case "getBiome" -> tropical.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                                        new ResourceLocation("aoemod:mangrove_swamp")));
                        case "getFluidState" -> Blocks.AIR.defaultBlockState().getFluidState();
                        case "getBlockState" -> ((BlockPos) args[0]).getY() == 64 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        case "noCollision" -> true;
                        case "addFreshEntityWithPassengers" -> { spawned.add((Entity) args[0]); yield null; }
                        default -> method.invoke(tropical, args);
                    });
            for (var chunk : plan.mounds()) {
                String species = TropicalMoundFauna.species(layout, chunk.x(), chunk.z());
                if (species == null) continue;
                counts.merge(species, 1, Integer::sum);
                int before = spawned.size();
                helper.assertTrue(TropicalMoundFauna.place(new FeaturePlaceContext<>(Optional.empty(), world,
                        tropical.getChunkSource().getGenerator(), RandomSource.create(seed),
                        new BlockPos(chunk.x() * 16, 0, chunk.z() * 16), NoneFeatureConfiguration.INSTANCE), layout),
                        "Selected shore must place its group");
                helper.assertTrue(spawned.size() == before + 2, "Each selected shore needs two actual entities");
            }
            helper.assertTrue(counts.size() == 2 && counts.values().stream().allMatch(n -> n == 4)
                    && spawned.size() == 16 && !counts.containsKey("wildernature:flamingo"),
                    "Island requires four shore groups each for capybaras and frogs, with no flamingos");
            helper.assertTrue(counts.getOrDefault("minecraft:frog", 0) == 4,
                    "Every island must reserve four distinct frog shore groups");
            for (Entity entity : spawned) {
                helper.assertFalse(((net.minecraft.world.entity.AgeableMob) entity).isBaby(), "Mound groups must be adults");
                helper.assertTrue(TropicalMoundFauna.isMoundAnimal(entity),
                        "Island animals must retain their one-time generation marker");
                long origin = entity.getPersistentData().getLong("aoemod:mound_fauna_chunk");
                var saved = new net.minecraft.nbt.CompoundTag();
                entity.saveWithoutId(saved);
                var restored = entity.getType().create(tropical);
                restored.load(saved);
                helper.assertTrue(TropicalMoundFauna.isMoundAnimal(restored)
                                && TropicalMoundFauna.belongsToMound(restored, origin),
                        "Mound provenance must survive the actual entity NBT round trip");
                helper.assertTrue(((net.minecraft.world.entity.Mob) restored).isPersistenceRequired(),
                        "Saving and loading must retain the one-time mound animals");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void moundAnimalsCannotReplenishThroughNaturalSpawning(GameTestHelper helper) {
        BlockPos feet = new BlockPos(8, 101, 8);
        for (String biome : List.of("mangrove_swamp", "rainforest", "cherry_grove", "coral_reef")) {
            var world = AquaticPlantGenerationTests.fixture(helper, biome, new HashMap<>(), 0);
            for (String id : List.of("biodiverse:capybara", "minecraft:frog", "wildernature:flamingo")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                    var placement = new MobSpawnEvent.SpawnPlacementCheck(type, world, reason, feet,
                            RandomSource.create(42), true);
                    placement.setResult(Event.Result.ALLOW);
                    DocumentedNaturalSpawns.placement(placement);
                    helper.assertTrue(placement.getResult() == Event.Result.DENY,
                            id + " must not spawn outside its generated mound in " + biome);
                    var mob = (net.minecraft.world.entity.Mob) type.create(world.getLevel());
                    mob.setPos(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                    var position = new MobSpawnEvent.PositionCheck(mob, world, reason, null);
                    position.setResult(Event.Result.ALLOW);
                    DocumentedNaturalSpawns.position(position);
                    helper.assertTrue(position.getResult() == Event.Result.DENY,
                            id + " must remain excluded even if another mod skips placement checks");
                }
                var egg = new MobSpawnEvent.SpawnPlacementCheck(type, world, MobSpawnType.SPAWN_EGG, feet,
                        RandomSource.create(42), true);
                DocumentedNaturalSpawns.placement(egg);
                helper.assertTrue(egg.getResult() == Event.Result.DEFAULT,
                        "One-time natural generation must preserve deliberate spawn eggs");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalFrogsAreGreenAndCassowariesCannotRest(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var frog = EntityType.FROG.create(level);
        frog.setVariant(FrogVariant.WARM);
        frog.getPersistentData().putBoolean("aoemod:documented_natural_spawn", true);
        DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(frog, level, true));
        helper.assertTrue(frog.getVariant() == FrogVariant.COLD, "Generated frogs cannot use orange or white variants");
        frog.setVariant(FrogVariant.TEMPERATE);
        DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(frog, level));
        helper.assertTrue(frog.getVariant() == FrogVariant.COLD, "Tadpole conversions and spawn eggs also need the green variant");
        var cassowary = (net.mcreator.biodiverse.entity.CassowaryEntity) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("biodiverse:cassowary")).create(level);
        cassowary.setResting(true);
        DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(cassowary, level));
        helper.assertFalse(cassowary.isResting(), "Loaded cassowaries must stand up");
        helper.assertFalse(cassowary.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof net.mcreator.biodiverse.ai.goal.RestingGoal), "Cassowaries cannot start resting again");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void capybaraTickDoesNotApplySerenityToNearbyAnimals(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        var capybara = (net.mcreator.biodiverse.entity.CapybaraEntity) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("biodiverse:capybara")).create(level);
        var cow = EntityType.COW.create(level);
        capybara.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        cow.moveTo(pos.getX() + 2, pos.getY(), pos.getZ() + .5, 0, 0);
        capybara.setNoAi(true);
        cow.setNoAi(true);
        level.addFreshEntity(capybara);
        level.addFreshEntity(cow);
        var serenity = net.mcreator.biodiverse.init.BiodiverseModMobEffects.SERENITY.get();
        try {
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(serenity, 60));
            helper.assertTrue(cow.hasEffect(serenity), "Fixture must accept an explicit serenity effect");
            cow.removeEffect(serenity);
            for (int tick = 0; tick < 81; tick++) capybara.baseTick();
            helper.assertFalse(cow.hasEffect(serenity), "Capybara aura must never add serenity");
        } finally {
            capybara.discard();
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void turtlesUseNearbyMossBanksWithoutReturningToBirthplace(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos pos = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 51000, 220, 51000);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) for (int y = -1; y <= 3; y++) {
            level.setBlock(pos.offset(x, y, z), (y == -1 ? Blocks.MOSS_BLOCK : Blocks.AIR).defaultBlockState(), 18);
        }
        Turtle turtle = EntityType.TURTLE.create(level);
        turtle.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        turtle.setHomePos(pos.offset(1000, 0, 1000));
        CompoundTag state = new CompoundTag();
        turtle.addAdditionalSaveData(state);
        state.putBoolean("HasEgg", true);
        turtle.readAdditionalSaveData(state);
        var lay = turtle.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal())
                .filter(g -> g.getClass().getSimpleName().equals("TurtleLayEggGoal")).findFirst().orElseThrow();
        var home = turtle.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal())
                .filter(g -> g.getClass().getSimpleName().equals("TurtleGoHomeGoal")).findFirst().orElseThrow();
        helper.assertFalse(home.canUse(), "Pregnant turtles must not start a birthplace journey");
        helper.assertTrue(lay.canUse(), "A nearby tropical moss bank must be available regardless of home position");
        helper.assertTrue(TurtleEggBlock.onSand(level, pos), "Eggs laid on tropical moss must also be able to hatch");
        helper.succeed();
    }
}
