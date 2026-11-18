package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DimensionBiomeSource;
import net.amurdza.examplemod.worldgen.DimensionTerrain;
import net.amurdza.examplemod.worldgen.NetherRampDecorationGuard;
import net.amurdza.examplemod.worldgen.TropicalLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Opt-in native-world scenes for the isolated follow-up QA world, never a player profile. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FollowupWorldgenVisualTests {
    private static final List<Map<String, Object>> VIEWS = new ArrayList<>();
    private static final List<Map<String, Object>> SCENES = new ArrayList<>();

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void exportBoundedNaturalTerrainAndExactFloraStages(GameTestHelper helper) throws Exception {
        if (!Boolean.getBoolean("aoemod.followupVisual.enabled")) {
            helper.succeed();
            return;
        }
        VIEWS.clear();
        SCENES.clear();
        var server = helper.getLevel().getServer();
        var tropical = server.getLevel(GameplayDimensions.TROPICAL);
        var snowy = server.getLevel(GameplayDimensions.SNOWY);
        var desert = server.getLevel(GameplayDimensions.DESERT);
        for (var level : List.of(tropical, snowy, desert)) {
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
        }
        int region = Integer.getInteger("aoemod.followupVisual.region", 1203);
        var layout = DimensionTerrain.tropicalLayout(tropical.getChunkSource().randomState().router().temperature());
        var island = layout.island(region | 1, 907);
        int coastX = (int) Math.round(island.x() + TropicalLayout.MANGROVE_LAND_RADIUS);
        int coastZ = (int) Math.round(island.z());
        load(tropical, coastX, coastZ, 3);
        Vec3 coast = new Vec3(coastX + .5, 69, coastZ + .5);
        above("tropical_coast_land_and_water", tropical, coast, 46, 42, 25);
        above("tropical_coast_along_shore", tropical, coast, 24, -52, 23);
        SCENES.add(Map.of("scene", "tropical_coast", "kind", "unaltered natural terrain", "x", coastX, "z", coastZ,
                "landRadius", TropicalLayout.MANGROVE_LAND_RADIUS, "reefRadius", TropicalLayout.ISLAND_RADIUS,
                "loadedRadiusChunks", 3));

        flora(helper, tropical, (int) Math.floor(island.x()), (int) Math.floor(island.z()));

        BlockPos border = snowyBoundary(snowy, region * 1024, -region * 512);
        load(snowy, border.getX(), border.getZ(), 3);
        Vec3 edge = new Vec3(border.getX() + .5, 70, border.getZ() + .5);
        above("snowy_dappled_boundary_overview", snowy, edge, 36, 48, 25);
        above("snowy_dappled_boundary_reverse", snowy, edge, -32, -38, 20);
        SCENES.add(Map.of("scene", "snowy_dappled_boundary", "kind", "unaltered natural terrain", "x", border.getX(),
                "z", border.getZ(), "loadedRadiusChunks", 3));

        var terrain = NetherRampDecorationGuard.terrain(desert.getChunkSource().randomState()).orElseThrow();
        DimensionTerrain.DesertOutlet outlet = null;
        int cycle = 17000 + Math.floorMod(region, 1000);
        for (int stripe = 0; stripe < 8; stripe++) {
            var candidate = terrain.desertOutlet(cycle, stripe);
            if (candidate.length() < 640) { outlet = candidate; break; }
        }
        if (outlet == null) throw new IllegalStateException("Bounded eight-outlet search did not find an emerged desert mouth");
        double distance = Math.max(16, outlet.length() - 26);
        int mouthX = (int) Math.round(outlet.xAt(distance)), mouthZ = (int) Math.round(outlet.zAt(distance));
        load(desert, mouthX, mouthZ, 3);
        Vec3 mouth = new Vec3(outlet.xAt(distance), DimensionTerrain.desertOutletFloor(distance) + 3, outlet.zAt(distance));
        double heading = outlet.headingAt(distance);
        above("desert_curved_mouth_overview", desert, mouth, Math.cos(heading) * 34, -Math.sin(heading) * 34, 22);
        double inside = Math.max(12, distance - 15);
        Vec3 insideFeet = new Vec3(outlet.xAt(inside), DimensionTerrain.desertOutletFloor(inside) + 1.3, outlet.zAt(inside));
        view("desert_curved_mouth_from_ramp", desert, insideFeet,
                new Vec3(outlet.xAt(inside + 12), DimensionTerrain.desertOutletFloor(inside + 12) + 2.5, outlet.zAt(inside + 12)));
        SCENES.add(Map.of("scene", "desert_curved_mouth", "kind", "unaltered natural terrain", "cycle", cycle,
                "length", outlet.length(), "x", mouthX, "z", mouthZ, "loadedRadiusChunks", 3));

        Path output = Path.of(System.getProperty("aoemod.followupVisual.output",
                "build/reports/followup-worldgen-views.json")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(output, gson.toJson(VIEWS) + "\n");
        Files.writeString(output.resolveSibling("followup-worldgen-scenes.json"), gson.toJson(SCENES) + "\n");
        server.saveEverything(true, true, true);
        AOEMod.LOGGER.info("Exported {} follow-up native-world views to {}", VIEWS.size(), output);
        helper.succeed();
    }

    private static void flora(GameTestHelper helper, ServerLevel level, int x, int z) {
        int y = 150;
        load(level, x, z, 1);
        // A separate clearly staged crop bed makes every exact texture stage reviewable.
        for (BlockPos pos : BlockPos.betweenClosed(x - 4, y - 1, z - 4, x + 28, y + 5, z + 12)) {
            level.setBlock(pos, pos.getY() == y - 1 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
        }
        var cotton = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:cotton"));
        var wild = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:wild_cotton"));
        helper.assertTrue(cotton != null && cotton != Blocks.AIR && wild != null && wild != Blocks.AIR,
                "Cotton crop and wild blocks must resolve before exporting views");
        int[] cottonAges = {0, 2, 4, 7};
        for (int stage = 0; stage <= 3; stage++) {
            BlockPos pos = new BlockPos(x + stage * 5, y, z);
            placeCrop(level, pos, property(cotton.defaultBlockState(), "age", Integer.toString(cottonAges[stage])), true, false);
            view("cotton_stage_" + stage, level, Vec3.atLowerCornerOf(pos).add(2.7, 1.0, 3.5), Vec3.atCenterOf(pos).add(0, -.1, 0));
        }
        BlockPos wildPos = new BlockPos(x + 20, y, z);
        BlockState wildState = wild.defaultBlockState();
        placeCrop(level, wildPos, wildState, false, wildState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF));
        view("wild_cotton", level, Vec3.atLowerCornerOf(wildPos).add(2.7, 1.0, 3.5), Vec3.atCenterOf(wildPos));
        for (int age = 1; age <= 4; age++) {
            BlockPos pos = new BlockPos(x + (age - 1) * 5, y, z + 7);
            BlockState state = Blocks.PITCHER_CROP.defaultBlockState().setValue(BlockStateProperties.AGE_4, age)
                    .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            placeCrop(level, pos, state, true, age >= 3);
            view("pitcher_crop_stage_" + age, level, Vec3.atLowerCornerOf(pos).add(3.0, 1.0, 4.0),
                    Vec3.atCenterOf(pos).add(0, age >= 3 ? .45 : -.1, 0));
        }
        SCENES.add(Map.of("scene", "cotton_and_pitcher_stages", "kind", "staged native blocks on Tropical rich-soil farmland/moss; random ticks disabled",
                "x", x, "y", y, "z", z, "cottonAges", List.of(0, 2, 4, 7), "pitcherAges", List.of(1, 2, 3, 4)));
    }

    private static void placeCrop(ServerLevel level, BlockPos pos, BlockState state, boolean farmland, boolean tall) {
        // Tropical's actual cultivation uses rich-soil farmland and moss. Its
        // gameplay rules reject ordinary farmland and grass blocks even in QA.
        var richFarmland = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("farmersdelight:rich_soil_farmland"));
        if (richFarmland == null || richFarmland == Blocks.AIR)
            throw new IllegalStateException("Tropical crop fixture requires installed rich-soil farmland");
        BlockState soil = farmland ? richFarmland.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7)
                : Blocks.MOSS_BLOCK.defaultBlockState();
        if (!BiomePlacementRestrictions.isAllowed(level, pos.below(), soil)
                || !BiomePlacementRestrictions.isAllowed(level, pos, state))
            throw new IllegalStateException("Crop or native soil is forbidden in the fixture's actual biome: "
                    + level.getBiome(pos).unwrapKey() + " at " + pos + ": " + state + " on " + soil);
        level.setBlock(pos.below(), soil, 18);
        if (!level.getBlockState(pos.below()).equals(soil))
            throw new IllegalStateException("Native crop soil was not placed at " + pos.below() + ": expected "
                    + soil + ", actual " + level.getBlockState(pos.below()));
        level.setBlock(pos, state, 18);
        if (tall) {
            BlockState upper = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
            level.setBlock(pos.above(), upper, 18);
            if (!level.getBlockState(pos.above()).equals(upper) || !upper.canSurvive(level, pos.above()))
                throw new IllegalStateException("Staged crop upper half failed native survival at " + pos.above());
        }
        if (!level.getBlockState(pos).equals(state) || !state.canSurvive(level, pos))
            throw new IllegalStateException("Staged crop failed to survive on native soil: " + state + " at " + pos
                    + ", actual=" + level.getBlockState(pos) + ", soil=" + level.getBlockState(pos.below())
                    + ", light=" + level.getRawBrightness(pos, 0) + ", biome=" + level.getBiome(pos).unwrapKey());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState property(BlockState state, String name, String value) {
        Property property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalArgumentException("Missing property " + name + " on " + state);
        return state.setValue(property, (Comparable) property.getValue(value).orElseThrow());
    }

    private static BlockPos snowyBoundary(ServerLevel level, int originX, int originZ) {
        var ridges = level.getChunkSource().randomState().router().ridges();
        for (int dz = -512; dz <= 512; dz += 64) {
            int z = originZ + dz;
            int previous = -1;
            for (int x = originX - 2048; x <= originX + 2048; x += 8) {
                int biome = DimensionBiomeSource.snowySurfaceIndex(ridges.compute(new DensityFunction.SinglePointContext(x, 0, z)));
                if (biome != previous && (biome == 0 && previous == 3 || biome == 3 && previous == 0))
                    return new BlockPos(x - 4, 70, z);
                previous = biome;
            }
        }
        throw new IllegalStateException("Bounded snowy contour search found no forest boundary");
    }

    private static void load(ServerLevel level, int x, int z, int radiusChunks) {
        for (int cx = (x >> 4) - radiusChunks; cx <= (x >> 4) + radiusChunks; cx++)
            for (int cz = (z >> 4) - radiusChunks; cz <= (z >> 4) + radiusChunks; cz++) level.getChunk(cx, cz);
    }

    private static void above(String label, ServerLevel level, Vec3 target, double dx, double dz, double clearance) {
        int x = (int) Math.floor(target.x + dx), z = (int) Math.floor(target.z + dz);
        double top = Math.max(target.y, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        view(label, level, new Vec3(target.x + dx, top + clearance, target.z + dz), target);
    }

    private static void view(String label, ServerLevel level, Vec3 feet, Vec3 target) {
        Vec3 direction = target.subtract(feet.add(0, 1.62, 0));
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("label", label);
        view.put("dimension", level.dimension().location().toString());
        view.put("x", feet.x); view.put("y", feet.y); view.put("z", feet.z);
        view.put("yaw", yaw); view.put("pitch", pitch);
        VIEWS.add(view);
    }
}
