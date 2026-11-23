package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.BoneMealConfig;
import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SurfaceHabitatColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyCultivationTests {
    private static final List<String> BIOMES = List.of("snowy_forest", "dappled_forest", "cold_ocean",
            "mushroom_caves", "mountain_lake", "dripstone_caves");

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void plantedBrownAndRedMushroomsBecomeGiantsInDappledForest(GameTestHelper h) {
        var level = snowy(h);
        var origin = new BlockPos(2440, 240, 2440);
        try (var fixture = new Fixture(level, origin)) {
            fixture.dappled(h);
            var player = player(level);
            for (var small : List.of(Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM)) {
                fixture.clear(6, 18, Blocks.MYCELIUM.defaultBlockState());
                h.assertTrue(GameplayDimensions.biomeIs(level, origin, "dappled_forest"),
                        "Real mushroom growth must use the authoritative dappled surface column");
                h.assertTrue(level.setBlock(origin, small.defaultBlockState(), 18)
                                && small.defaultBlockState().canSurvive(level, origin),
                        "An allowed planted mushroom must survive on mycelium");
                for (int attempt = 0; attempt < 16 && level.getBlockState(origin).is(small); attempt++) {
                    level.random.setSeed(4811L + attempt);
                    meal(h, player, origin);
                }
                h.assertTrue(level.getBlockState(origin).is(Blocks.MUSHROOM_STEM),
                        "Player bonemeal must grow a real giant " + ForgeRegistries.BLOCKS.getKey(small));
                var cap = small == Blocks.BROWN_MUSHROOM ? Blocks.BROWN_MUSHROOM_BLOCK : Blocks.RED_MUSHROOM_BLOCK;
                int stems = 0, caps = 0;
                for (var pos : BlockPos.betweenClosed(origin.offset(-6, 0, -6), origin.offset(6, 18, 6))) {
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.MUSHROOM_STEM)) stems++;
                    if (state.is(cap)) caps++;
                }
                h.assertTrue(stems >= 4 && caps >= 12, "Giant growth must commit a complete stem and cap");
            }
            fixture.clear(6, 18, Blocks.MYCELIUM.defaultBlockState());
            fixture.set(origin, Blocks.RED_MUSHROOM.defaultBlockState());
            fixture.set(origin.above(), Blocks.STONE.defaultBlockState());
            for (int attempt = 0; attempt < 8; attempt++) meal(h, player, origin);
            h.assertTrue(level.getBlockState(origin).is(Blocks.RED_MUSHROOM)
                            && level.getBlockState(origin.above()).is(Blocks.STONE),
                    "Shared habitat permission must not bypass mushroom clearance checks");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void caveWheatBeetrootAndOatsGrowWithLightAndFarmland(GameTestHelper h) {
        var level = snowy(h);
        var origin = new BlockPos(2472, -24, 2472);
        var fixture = new Fixture(level, origin);
        var farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
        fixture.clear(5, 6, farmland);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
            fixture.set(origin.offset(x, 6, z), Blocks.STONE.defaultBlockState());
        for (var torch : List.of(origin.offset(-3, 0, -3), origin.offset(3, 0, 3))) {
            fixture.set(torch.below(), Blocks.STONE.defaultBlockState());
            fixture.set(torch, Blocks.TORCH.defaultBlockState());
        }
        var dark = origin.offset(0, 0, 3);
        // Farmland is not opaque: seal the walls below it and the floor
        // underneath it so torchlight cannot enter from beneath the chamber.
        for (var pos : BlockPos.betweenClosed(dark.offset(-1, -2, -1), dark.offset(1, 2, 1))) {
            var state = pos.equals(dark) || pos.equals(dark.above()) ? Blocks.AIR.defaultBlockState()
                    : pos.equals(dark.below()) ? farmland : Blocks.STONE.defaultBlockState();
            fixture.set(pos, state);
        }
        // Lighting is asynchronous in a real server. Wait for the two actual
        // torches and the enclosed dark control before invoking random ticks.
        h.runAtTickTime(30, () -> {
            try (fixture) {
                h.assertTrue(GameplayDimensions.biomeIs(level, origin, "mushroom_caves"),
                        "Use the real -24 cave band, not a surface biome proxy");
                var oat = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("vintagedelight:oat_crop"));
                h.assertTrue(oat instanceof CropBlock, "The installed oat crop must expose its native crop behavior");
                var crops = List.of(Blocks.WHEAT, Blocks.BEETROOTS, oat);
                var positions = List.of(origin.offset(-1, 0, 0), origin.offset(1, 0, 0), origin.offset(0, 0, -2));
                var player = player(level);
                for (int i = 0; i < crops.size(); i++) {
                    var crop = (CropBlock) crops.get(i);
                    var pos = positions.get(i);
                    var young = crop.getStateForAge(0);
                    h.assertTrue(level.getRawBrightness(pos, 0) >= 9, "The real cave crop fixture needs sufficient torchlight");
                    h.assertTrue(level.setBlock(pos, young, 18) && level.getBlockState(pos).is(crop)
                                    && young.canSurvive(level, pos),
                            "The planted cave crop must remain allowed on hydrated farmland: " + crop);
                    fixture.set(pos.below(), Blocks.STONE.defaultBlockState());
                    h.assertFalse(young.canSurvive(level, pos), "Habitat sharing must not permit crops on stone");
                    fixture.set(pos.below(), farmland);
                    level.random.setSeed(9881L + i);
                    for (int tick = 0; tick < 1024 && crop.getAge(level.getBlockState(pos)) == 0; tick++)
                        level.getBlockState(pos).randomTick(level, pos, RandomSource.create(701L + tick));
                    h.assertTrue(crop.getAge(level.getBlockState(pos)) > 0,
                            "Natural random ticks must advance an allowed cave crop: " + crop);
                    fixture.set(pos, young);
                    for (int attempt = 0; attempt < 16 && crop.getAge(level.getBlockState(pos)) == 0; attempt++)
                        meal(h, player, pos);
                    h.assertTrue(crop.getAge(level.getBlockState(pos)) > 0,
                            "Player bonemeal must advance an allowed cave crop: " + crop);
                }
                var wheat = (CropBlock) Blocks.WHEAT;
                fixture.set(dark, wheat.getStateForAge(0));
                int darkLight = level.getRawBrightness(dark, 0);
                boolean survivesDarkness = wheat.defaultBlockState().canSurvive(level, dark);
                h.assertTrue(darkLight < 8 && !survivesDarkness,
                        "Cave habitat sharing must retain normal crop light requirements: light=" + darkLight
                                + ", canSurvive=" + survivesDarkness + ", canSeeSky=" + level.canSeeSky(dark));
                for (int tick = 0; tick < 32; tick++) level.getBlockState(dark).randomTick(level, dark, RandomSource.create(tick));
                h.assertTrue(wheat.getAge(level.getBlockState(dark)) == 0, "Darkness must still prevent natural crop growth");
            }
            h.succeed();
        });
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void myceliumInDappledAndGrassInCavesKeepTheirSoilPalettes(GameTestHelper h) {
        var level = snowy(h);
        for (boolean surface : new boolean[]{true, false}) {
            var origin = new BlockPos(surface ? 2552 : 2584, surface ? 240 : -24, 2552);
            try (var fixture = new Fixture(level, origin)) {
                if (surface) fixture.dappled(h);
                var soil = surface ? Blocks.MYCELIUM : Blocks.GRASS_BLOCK;
                fixture.clear(7, 9, soil.defaultBlockState());
                h.assertTrue(GameplayDimensions.biomeIs(level, origin, surface ? "dappled_forest" : "mushroom_caves"),
                        "Surface bonemeal must be exercised outside this soil's former Snowy habitat");
                level.random.setSeed(surface ? 88312 : 81123);
                meal(h, player(level), origin.below());
                Set<String> allowed = surface ? Set.of("minecraft:red_mushroom", "minecraft:brown_mushroom", "quark:glow_shroom", "twilightforest:mushgloom")
                        : Set.of("minecraft:grass", "minecraft:fern", "minecraft:large_fern");
                int plants = 0;
                for (var pos : BlockPos.betweenClosed(origin.offset(-7, 0, -7), origin.offset(7, 8, 7))) {
                    var state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    h.assertTrue(allowed.contains(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()))),
                            "Moved soil must keep its own vegetation palette: " + state);
                    plants++;
                }
                h.assertTrue(plants > 0, "Bonemeal must actually grow plants on moved Snowy soil");
            }
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sixSnowyBiomesRetainTheSharedGrowthAndBonemealScores(GameTestHelper h) throws Exception {
        var level = snowy(h);
        var pos = new BlockPos(2520, -24, 2520);
        var chunk = level.getChunkAt(pos);
        var originalBiomes = new HashMap<BlockPos, Holder<Biome>>();
        int minX = QuartPos.fromBlock(chunk.getPos().getMinBlockX()), minZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        for (int x = minX; x < minX + 4; x++) for (int z = minZ; z < minZ + 4; z++)
            for (int y = QuartPos.fromBlock(level.getMinBuildHeight()); y < QuartPos.fromBlock(level.getMaxBuildHeight()); y++)
                originalBiomes.put(new BlockPos(x, y, z), chunk.getNoiseBiome(x, y, z));
        var before = level.getBlockState(pos);
        var multiplier = BoneMeal.class.getDeclaredMethod("getDimensionMultiplier", ServerLevel.class, BlockPos.class);
        multiplier.setAccessible(true);
        var rates = BlockConfig.BLOCK_GROWTH_CHANCE_BY_DIMENSION.get(GameplayDimensions.SNOWY);
        var meals = BoneMealConfig.DIMENSION_TO_BONEMEAL_MULTIPLIERS.get(GameplayDimensions.SNOWY);
        var oat = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("vintagedelight:oat_crop"));
        h.assertTrue(rates != null && meals.plant() == 1 && meals.mushroom() == 2, "Retain the current shared Snowy scores");
        try {
            for (var name : BIOMES) {
                var key = ResourceKey.create(Registries.BIOME, AOEMod.makeID(name));
                var holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> holder, level.getChunkSource().randomState().sampler());
                h.assertTrue(level.getBiome(pos).is(key), "Rate lookup must use the actual registered fixture biome: " + name);
                for (var block : List.of(Blocks.WHEAT, Blocks.BEETROOTS, Blocks.SPRUCE_SAPLING, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.MYCELIUM, oat)) {
                    var state = block.defaultBlockState();
                    chunk.setBlockState(pos, state, false);
                    h.assertTrue(BlockConfig.blockGrowthChance(level, pos, state) == rates.getOrDefault(block, -1.0F),
                            "Do not lower or increase the shared growth score by Snowy biome: " + name + "/" + block);
                    boolean mushroom = block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM || block == Blocks.MYCELIUM;
                    h.assertTrue((float) multiplier.invoke(null, level, pos) == (mushroom ? meals.mushroom() : meals.plant()),
                            "Snowy bonemeal score must be shared across habitats: " + name + "/" + block);
                }
            }
        } finally {
            chunk.setBlockState(pos, before, false);
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> originalBiomes.get(new BlockPos(x, y, z)),
                    level.getChunkSource().randomState().sampler());
        }
        h.succeed();
    }

    private static ServerLevel snowy(GameTestHelper h) { return h.getLevel().getServer().getLevel(GameplayDimensions.SNOWY); }

    private static Player player(ServerLevel level) {
        return new Player(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "snowy-cultivation")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
    }

    private static void meal(GameTestHelper h, Player player, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        BoneMeal.onRightClickBlock(event);
        h.assertTrue(event.isCanceled() && player.getMainHandItem().getCount() == 1,
                "A player use must reach the real bonemeal handler and consume exactly one item");
    }

    private static final class Fixture implements AutoCloseable {
        private final ServerLevel level;
        private final BlockPos origin;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private SurfaceHabitatColumns column;
        private byte[] originalHabitats;

        private Fixture(ServerLevel level, BlockPos origin) { this.level = level; this.origin = origin; }

        private void dappled(GameTestHelper h) {
            column = SurfaceHabitatColumns.data(level.getChunkAt(origin));
            originalHabitats = column.aoemod$getHabitats();
            h.assertTrue(originalHabitats != null, "Dappled fixture requires persisted surface habitat columns");
            var habitats = originalHabitats.clone();
            Arrays.fill(habitats, (byte) SurfaceHabitatColumns.HABITATS.indexOf(ResourceKey.create(Registries.BIOME, AOEMod.makeID("dappled_forest"))));
            column.aoemod$setHabitats(habitats);
        }

        private void clear(int radius, int height, BlockState ground) {
            for (var pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, height, radius)))
                set(pos, pos.getY() < origin.getY() ? ground : Blocks.AIR.defaultBlockState());
        }

        private void set(BlockPos pos, BlockState state) {
            original.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlock(pos, state, 18);
        }

        @Override public void close() {
            original.forEach((pos, state) -> level.getChunkAt(pos).setBlockState(pos, state, false));
            if (column != null) column.aoemod$setHabitats(originalHabitats);
        }
    }
}
