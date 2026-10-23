package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DocumentedPlacementRules;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.amurdza.examplemod.util.BiomePlacementRestrictions.*;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DocumentedPlacementTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void documentOverridesAndModdedSources(GameTestHelper h) {
        expect(h, "minecraft:mud", TROPICAL | SAVANNA | SNOWY);
        expect(h, "minecraft:muddy_mangrove_roots", TROPICAL);
        expect(h, "minecraft:clay", PLAINS | SNOWY);
        expect(h, "minecraft:lava", SNOWY | DESERT | NETHER);
        expect(h, "minecraft:water", ALL & ~NETHER);
        expect(h, "minecraft:water_cauldron", ALL & ~NETHER);
        expect(h, "minecraft:ancient_debris", NETHER);
        expect(h, "minecraft:obsidian", DESERT | NETHER);
        expect(h, "minecraft:blackstone", DESERT | NETHER);
        expect(h, "minecraft:sunflower", PLAINS);
        expect(h, "farmersdelight:cabbages", TROPICAL | SAVANNA | PLAINS);
        expect(h, "floral:blue_tulip", SAVANNA);
        expect(h, "floral_flair:blue_hyacinth", TROPICAL);
        expect(h, "betterend:charnia_cyan", SNOWY);
        expect(h, "hybrid_aquatic:bull_kelp", DESERT);
        expect(h, "aoemod:pale_oak_planks", DESERT | NETHER);
        expect(h, "aoemod:pale_oak_leaves", NETHER);
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void habitatBoundariesAndChorusGrowth(GameTestHelper h) {
        ServerLevel nether = h.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        ServerLevel desert = h.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        ServerLevel snowy = h.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        BlockPos pos = new BlockPos(0, 80, 0);
        h.assertFalse(BiomePlacementRestrictions.isAllowed(nether, pos, Blocks.WATER.defaultBlockState()), "Direct water placement cannot bypass the Nether fluid rule");
        h.assertFalse(BiomePlacementRestrictions.isAllowed(nether, pos, Blocks.CRIMSON_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true)), "Waterlogged blocks cannot carry water into the AOE Nether");
        h.assertTrue(BiomePlacementRestrictions.isAllowed(snowy, pos, Blocks.LAVA.defaultBlockState()), "The snowy document allows lava");
        var crimsonAbove = boundary(nether, pos, "warped_forest", "crimson_forest");
        var warpedAbove = boundary(nether, pos, "crimson_forest", "warped_forest");
        var wrongOre = boundary(nether, pos, "soul_sand_valley", "basalt_deltas");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(crimsonAbove, pos, Blocks.NETHER_GOLD_ORE.defaultBlockState(), false), "Nether gold accepts the biome above");
        h.assertFalse(DocumentedPlacementRules.isBiomeAllowed(wrongOre, pos, Blocks.NETHER_GOLD_ORE.defaultBlockState(), false), "Nether gold rejects the wrong substrate biome");
        h.assertFalse(DocumentedPlacementRules.isBiomeAllowed(crimsonAbove, pos, Blocks.CRIMSON_STEM.defaultBlockState(), true), "Crimson trunk must not grow into warped forest");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(warpedAbove, pos, Blocks.CRIMSON_STEM.defaultBlockState(), true), "Crimson trunk uses its own biome, not the biome above");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(crimsonAbove, pos, Blocks.CRIMSON_PLANKS.defaultBlockState(), false), "Finished crimson planks may cross Nether biome boundaries");
        var paleLog = net.amurdza.examplemod.registry.ModBlocks.PALE_OAK_LOG.get().defaultBlockState();
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(crimsonAbove, pos, paleLog, false), "Players may build with pale oak outside soul sand valley");
        h.assertFalse(DocumentedPlacementRules.isBiomeAllowed(crimsonAbove, pos, paleLog, true), "A growing pale oak tree must keep its trunk inside soul sand valley");
        var nonCave = boundary(snowy, pos, "snowy_forest", "snowy_forest");
        var cave = boundary(snowy, pos, "mushroom_caves", "mushroom_caves");
        h.assertFalse(DocumentedPlacementRules.isBiomeAllowed(nonCave, pos, Blocks.BEETROOTS.defaultBlockState(), false), "Snowy beetroot requires mushroom caves");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(cave, pos, Blocks.BEETROOTS.defaultBlockState(), false), "Mushroom-cave beetroot is allowed");
        var surface = boundary(desert, pos, "desert", "desert");
        h.assertFalse(DocumentedPlacementRules.isBiomeAllowed(surface, pos, Blocks.CHORUS_FLOWER.defaultBlockState(), false), "Players plant chorus only in deep dark");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(surface, pos, Blocks.CHORUS_FLOWER.defaultBlockState(), true), "Existing chorus may grow into other desert biomes");
        h.assertTrue(DocumentedPlacementRules.isBiomeAllowed(h.getLevel(), pos, Blocks.BEETROOTS.defaultBlockState(), false), "Vanilla dimensions retain their own placement behavior");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void writeLoadedRegistryAudit(GameTestHelper h) throws Exception {
        JsonObject result = new JsonObject();
        result.add("blocks", registry(BuiltInRegistries.BLOCK));
        result.add("items", registry(BuiltInRegistries.ITEM));
        result.add("entity_types", registry(BuiltInRegistries.ENTITY_TYPE));
        result.add("worldgen/biome", registry(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME)));
        JsonArray recipes = new JsonArray();
        h.getLevel().getRecipeManager().getRecipeIds().map(ResourceLocation::toString).sorted().forEach(recipes::add);
        result.add("recipes", recipes);
        Path path = Path.of("reports", "loaded-registry-audit.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(result));
        h.succeed();
    }

    private static <T> JsonObject registry(Registry<T> registry) {
        JsonObject result = new JsonObject();
        JsonArray ids = new JsonArray();
        registry.keySet().stream().map(ResourceLocation::toString).sorted().forEach(ids::add);
        result.add("ids", ids);
        JsonObject tags = new JsonObject();
        registry.getTags().forEach(pair -> {
            JsonArray values = new JsonArray();
            pair.getSecond().stream().map(Holder::value).map(registry::getKey).map(ResourceLocation::toString).sorted().forEach(values::add);
            tags.add(pair.getFirst().location().toString(), values);
        });
        result.add("tags", tags);
        return result;
    }

    private static void expect(GameTestHelper h, String id, int expected) {
        ResourceLocation key = new ResourceLocation(id);
        h.assertTrue(BuiltInRegistries.BLOCK.containsKey(key), "Missing documented source " + id);
        Block block = BuiltInRegistries.BLOCK.get(key);
        h.assertTrue(BiomePlacementRestrictions.allowedMask(block) == expected, "Wrong document placement mask for " + id);
    }

    private static ServerLevelAccessor boundary(ServerLevel level, BlockPos pos, String here, String above) {
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> hereBiome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(here)));
        Holder<Biome> aboveBiome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(above)));
        return (ServerLevelAccessor) Proxy.newProxyInstance(DocumentedPlacementTests.class.getClassLoader(), new Class<?>[]{ServerLevelAccessor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevel" -> level;
                    case "getBiome" -> pos.above().equals(args[0]) ? aboveBiome : hereBiome;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
