package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.SavannaHorsemanSpawns;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedMonsters;
import net.amurdza.examplemod.worldgen.SavannaBands;
import net.amurdza.examplemod.worldgen.structure.SavannaCampPlacement;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class November15SavannaTests {
    private November15SavannaTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void restoredBiomeModsProvideTheirOriginalRegistrations(GameTestHelper helper) {
        for (String mod : List.of("environmental", "biomesoplenty", "biomeswevegone")) {
            helper.assertTrue(ModList.get().isLoaded(mod), "Restored mod must load: " + mod);
            helper.assertTrue(ForgeRegistries.BLOCKS.getKeys().stream().anyMatch(id -> id.getNamespace().equals(mod))
                    && ForgeRegistries.ITEMS.getKeys().stream().anyMatch(id -> id.getNamespace().equals(mod)),
                    "Restored dependency must register its original blocks and items: " + mod);
        }
        for (String id : List.of("environmental:blue_delphinium", "environmental:purple_delphinium",
                "biomesoplenty:pink_daffodil", "biomeswevegone:japanese_orchid", "biomeswevegone:sandy_farmland")) {
            var key = new ResourceLocation(id);
            helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(key)
                    && net.amurdza.examplemod.util.RetainedContent.isKeptItem(new net.minecraft.world.item.ItemStack(
                    ForgeRegistries.ITEMS.getValue(key))), "Restored gameplay object must remain available: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void squareCampsChooseHighDryGroundForTheirEntireApron(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var random = level.getChunkSource().randomState();
        var generator = level.getChunkSource().getGenerator();
        var generatorState = level.getChunkSource().getGeneratorState();
        var placement = new SavannaCampPlacement();
        var land = SavannaCampPlacement.terrainLand(random);
        var bands = new SavannaBands(land);
        int extent = SavannaOrcVillage.CENTER + SavannaOrcVillage.MARGIN;
        for (int column = -4; column <= 4; column++) for (int row = -3; row <= 3; row++) {
            BlockPos center = SavannaCampPlacement.findCenter(land, column, row);
            helper.assertTrue(Math.floorDiv(center.getX(), 1152) == column && Math.floorDiv(center.getZ(), SavannaCampPlacement.Z_SPACING) == row,
                    "Each camp must stay in its placement cell, including negative coordinates");
            helper.assertTrue((center.getX() & 15) == 8 && (center.getZ() & 15) == 8,
                    "The camp must be centered on its owning chunk");
            var owner = new net.minecraft.world.level.ChunkPos(center);
            helper.assertTrue(placement.isStructureChunk(generatorState, owner.x, owner.z)
                            && placement.locateCandidates(generatorState, center, 0).equals(List.of(owner)),
                    "Natural placement and locate must agree on the actual seeded terrain divide");
            helper.assertTrue(bands.shoreDistance(center.getX(), center.getZ()) > 150,
                    "Center must sit near the high divide");
            for (int dx = -extent; dx <= extent; dx += 4) for (int dz : new int[]{-extent, extent}) {
                helper.assertTrue(bands.shoreDistance(center.getX() + dx, center.getZ() + dz) > 25,
                        "Entire square apron needs a dry buffer beyond its outermost edge: " + center);
            }
            // Independently query the native generator: optimizing the climate
            // temperature field used to satisfy all band-only checks above.
            for (int dx : new int[]{-extent, 0, extent}) for (int dz : new int[]{-extent, 0, extent}) {
                int x = center.getX() + dx, z = center.getZ() + dz;
                int top = generator.getBaseHeight(x, z,
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, random) - 1;
                int expected = (int) Math.ceil(bands.height(x, z) + .5) - 1;
                helper.assertTrue(top >= generator.getSeaLevel() && Math.abs(top - expected) <= 1,
                        "Camp warp must match the actual interpolated terrain, not climate noise, at " + x + "," + z);
            }
            helper.assertTrue(Math.abs(((center.getX() + extent) >> 4) - (center.getX() >> 4)) <= 8
                            && Math.abs(((center.getX() - extent) >> 4) - (center.getX() >> 4)) <= 8,
                    "Every apron chunk must lie within native structure-reference search distance");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cropPondMudReachesBedrockWithoutReplacingIt(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var states = new HashMap<BlockPos, BlockState>();
        BlockPos origin = new BlockPos(30000, 100, 30000);
        BlockPos column = origin.offset(SavannaOrcVillage.CENTER + 46, 0, SavannaOrcVillage.CENTER + 46);
        int bottom = level.getMinBuildHeight();
        states.put(column.atY(bottom), Blocks.BEDROCK.defaultBlockState());
        var clip = new BoundingBox(column.getX(), bottom, column.getZ(), column.getX(), 255, column.getZ());
        WorldGenLevel world = (WorldGenLevel) Proxy.newProxyInstance(November15SavannaTests.class.getClassLoader(),
                new Class[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlockState": return states.getOrDefault(args[0], Blocks.AIR.defaultBlockState());
                        case "getFluidState": return states.getOrDefault(args[0], Blocks.AIR.defaultBlockState()).getFluidState();
                        case "setBlock": {
                            var pos = ((BlockPos) args[0]).immutable();
                            helper.assertTrue(clip.isInside(pos), "Mud filling must respect its chunk clip");
                            states.put(pos, (BlockState) args[1]); return true;
                        }
                    }
                    try { return method.invoke(level, args); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                });
        SavannaOrcVillage.placeGround(world, clip, origin, SavannaOrcVillage.planGround(102, (x, z) -> 70));
        helper.assertTrue(states.get(column.atY(bottom)).is(Blocks.BEDROCK), "Do not replace bedrock");
        for (int y = bottom + 1; y <= 99; y++) helper.assertTrue(states.get(column.atY(y)).is(Blocks.MUD),
                "Pond substrate must be mud all the way to bedrock atY=" + y);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void horsemenUseNormalHostileLifetimesWithoutBonusLightningTraps(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        for (String id : DocumentedMonsters.forBiome("savanna")) helper.assertTrue(DocumentedMonsters.weight(id) == 10,
                "All six ordinary Savanna monsters retain equal selection weight");
        var trap = EntityType.SKELETON_HORSE.create(level);
        trap.setTrap(true);
        var event = new EntityJoinLevelEvent(trap, level, false);
        SavannaHorsemanSpawns.join(event);
        helper.assertTrue(event.isCanceled(), "Weather must not inject four persistent bonus archers");
        for (var type : List.of(ModEntities.SKELETON_HORSEMAN.get(), ModEntities.ZOMBIE_HORSEMAN.get())) {
            var horse = type.create(level);
            helper.assertTrue(horse.removeWhenFarAway(20000), "Wild horsemen must despawn like other monsters");
            horse.setOwnerUUID(UUID.randomUUID());
            helper.assertFalse(horse.removeWhenFarAway(20000), "Player-owned mounts must be preserved");
        }
        BlockPos pos = new BlockPos(31000, 220, 31000);
        var box = new AABB(pos).inflate(2);
        var horse = ModEntities.SKELETON_HORSEMAN.get().create(level);
        horse.moveTo(pos, 0, 0);
        horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        var lightning = new LightningProbe();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(lightning);
        try {
            SavannaHorsemanSpawns.join(new EntityJoinLevelEvent(horse, level, false));
            var weather = level.getServer().overworld();
            boolean storm = weather.isThundering() && weather.getLevelData().isRaining()
                    && weather.getLevelData().isThundering();
            helper.assertTrue(lightning.bolts.size() == (storm ? 1 : 0),
                    "A natural skeleton horseman retains its lightning effect only during a shared thunderstorm");
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(lightning);
            lightning.bolts.forEach(bolt -> bolt.discard());
        }
        horse.getPassengers().forEach(entity -> entity.discard());
        horse.discard();
        helper.succeed();
    }


    public static final class LightningProbe {
        final java.util.List<LightningBolt> bolts = new java.util.ArrayList<>();

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void joined(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof LightningBolt bolt) bolts.add(bolt);
        }
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void actualSavannaLightningDoesNotIgniteBlocks(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos pos = new BlockPos(31016, 220, 31016);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3)))
            level.setBlock(cell, (cell.getY() < pos.getY() ? Blocks.OAK_PLANKS : Blocks.AIR).defaultBlockState(), 18);
        for (int i = 0; i < 12; i++) {
            var bolt = EntityType.LIGHTNING_BOLT.create(level);
            bolt.moveTo(pos, 0, 0);
            bolt.tick();
            helper.assertTrue(bolt.getBlocksSetOnFire() == 0, "Lightning must never light the Savanna");
            bolt.discard();
        }
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 1, 3)))
            helper.assertTrue(level.getBlockState(cell).isAir(), "Lightning left fire on the test platform");
        helper.succeed();
    }
}
