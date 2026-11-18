package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalIslandFaunaTests {
    private static final Map<String, Set<String>> HABITATS = Map.of(
            "rainforest", Set.of("alexsmobs:gorilla", "biodiverse:cassowary", "alexsmobs:elephant",
                    "minecraft:parrot", "minecraft:ocelot", "aoemod:arthropleura"),
            "cherry_grove", Set.of("minecraft:rabbit", "minecraft:panda", "minecraft:fox", "minecraft:cat"));

    @GameTest(template = "plant_gameplay_empty")
    public static void jungleAndCherryAnimalsSpawnNormallyOnlyInTheirHabitats(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        BlockPos feet = new BlockPos(8, 101, 8);
        for (String biome : List.of("rainforest", "cherry_grove", "mangrove_swamp", "coral_reef")) {
            var holder = tropical.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation("aoemod", biome)));
            var generated = new java.util.ArrayList<Mob>();
            WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(TropicalIslandFaunaTests.class.getClassLoader(),
                    new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getLevel" -> tropical;
                        case "getBiome" -> holder;
                        case "getHeight" -> args == null || args.length == 0 ? tropical.getHeight() : 101;
                        case "addFreshEntityWithPassengers" -> { generated.add((Mob) args[0]); yield null; }
                        case "getBlockState" -> ((BlockPos) args[0]).getY() == 100
                                ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        case "getFluidState" -> Blocks.AIR.defaultBlockState().getFluidState();
                        case "noCollision" -> true;
                        default -> method.invoke(tropical, args);
                    });
            if (HABITATS.containsKey(biome)) {
                helper.assertTrue(holder.value().getMobSettings().getCreatureProbability() > 0,
                        "Chunk-generation animal probability must be enabled in " + biome);
                for (int seed = 0; seed < 128; seed++) {
                    net.minecraft.world.level.NaturalSpawner.spawnMobsForChunkGeneration(world, holder,
                            new net.minecraft.world.level.ChunkPos(0, 0), RandomSource.create(TropicalLayout.mix(seed)));
                }
                helper.assertTrue(!generated.isEmpty(), "Vanilla chunk-generation spawning must produce animals in " + biome);
                for (Mob mob : generated) helper.assertTrue(HABITATS.get(biome).contains(
                                ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString())
                                && !mob.getTags().contains("aoemod:island_fauna") && !mob.isPersistenceRequired(),
                        "Native spawning must use habitat animals without fixed group tags or forced persistence");
            }
            for (var habitat : HABITATS.entrySet()) for (String id : habitat.getValue()) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
                boolean expected = habitat.getKey().equals(biome);
                for (MobSpawnType reason : List.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION)) {
                    var placement = new MobSpawnEvent.SpawnPlacementCheck(type, world, reason, feet, RandomSource.create(42), true);
                    DocumentedNaturalSpawns.placement(placement);
                    helper.assertTrue(placement.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                            id + " spawn placement differs from its habitat: " + biome);
                    var mob = (Mob) type.create(tropical);
                    mob.setPos(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                    var position = new MobSpawnEvent.PositionCheck(mob, world, reason, null);
                    DocumentedNaturalSpawns.position(position);
                    helper.assertTrue(position.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                            id + " position differs from its habitat: " + biome);
                    helper.assertFalse(mob.isPersistenceRequired(), "Normal animal spawning must not force persistence");
                }
                boolean listed = false;
                for (MobCategory category : MobCategory.values()) listed |= holder.value().getMobSettings().getMobs(category)
                        .unwrap().stream().anyMatch(entry -> entry.type == type);
                helper.assertTrue(listed == expected, "Loaded biome spawn table differs for " + id + " in " + biome);
            }
        }
        helper.assertTrue(DocumentedFauna.forBiome("savanna").stream().anyMatch(s -> s.id().toString().equals("minecraft:rabbit")),
                "Tropical changes must preserve other dimensions' wildlife");
        helper.succeed();
    }
}
