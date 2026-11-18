package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.level.block_entities.CreakingHeartBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartState;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.amurdza.examplemod.worldgen.SoulCreakingHearts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulValleyContentTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 600)
    public static void generatedPaleOakHeartSpawnsCreakingAndProducesResin(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var primary = level.getServer().overworld();
        long previousTime = primary.getDayTime();
        BlockPos root = new BlockPos(4008, 105, 4008);
        BlockPos min = root.offset(-18, -1, -18), max = root.offset(18, 19, 18);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) level.getChunk(x, z);
        }
        int filled = level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack()
                .withLevel(level).withSuppressedOutput(), "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ()
                        + " " + max.getX() + " " + max.getY() + " " + max.getZ() + " aoemod:soul_sand_valley");
        helper.assertTrue(filled > 0, "The heart lifecycle must run inside the soul valley biome");
        for (String id : new String[]{"creaking_heart", "resin_clump", "resin_block", "resin_bricks"}) {
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", id));
            helper.assertTrue(block != null && block != Blocks.AIR
                            && DimensionMaterialAvailability.isAvailable(level, block.asItem(), BiomePlacementRestrictions.NETHER)
                            && BiomePlacementRestrictions.isAllowed(level, root.above(2), block.defaultBlockState()),
                    "Natural heart/resin sources and their crafted products must pass the material placement rules: " + id);
        }
        Map<BlockPos, BlockState> previous = new HashMap<>();
        Creaking protector = null;
        try {
            for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
                BlockPos pos = cursor.immutable();
                previous.put(pos, level.getBlockState(pos));
                level.setBlock(pos, (pos.getY() == min.getY() ? Blocks.SOUL_SOIL : Blocks.AIR).defaultBlockState(), 2);
            }
            var tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("aoemod:soul_sand_valley/pale_oak"));
            helper.assertTrue(tree != null && tree.place(level, level.getChunkSource().getGenerator(), RandomSource.create(471), root),
                    "The registered soul-valley pale oak must grow on its natural floor");
            long selectedSeed = 0;
            while (RandomSource.create(selectedSeed).nextInt(8) != 0) selectedSeed++;
            helper.assertTrue(SoulCreakingHearts.place(level, RandomSource.create(selectedSeed), root),
                    "An eligible generated pale oak must receive a natural heart in a selected chunk");
            BlockPos heartPos = null;
            for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockState(cursor).getBlock() instanceof CreakingHeartBlock) {
                    helper.assertTrue(heartPos == null, "The chunk must contain at most one generated heart");
                    heartPos = cursor.immutable();
                }
            }
            helper.assertTrue(heartPos != null, "A naturally placed heart must be present in the generated trunk");
            BlockState heartState = level.getBlockState(heartPos);
            helper.assertTrue(heartState.getValue(CreakingHeartBlock.NATURAL)
                            && CreakingHeartBlock.hasRequiredLogs(heartState, level, heartPos),
                    "The native heart must recognize its custom pale oak supports");
            var heart = (CreakingHeartBlockEntity) level.getBlockEntity(heartPos);
            helper.assertTrue(heart != null, "World generation must create a ticking native heart block entity");
            primary.setDayTime(18000L);
            for (int tick = 0; tick < 30; tick++) {
                CreakingHeartBlockEntity.serverTick(level, heartPos, level.getBlockState(heartPos), heart);
            }
            helper.assertTrue(level.getBlockState(heartPos).getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE,
                    "The generated heart must wake at night underground");
            // Invoke the native spawn routine directly: the test has no real player
            // within the ordinary 32-block activation radius.
            var spawn = CreakingHeartBlockEntity.class.getDeclaredMethod("spawnProtector", ServerLevel.class, CreakingHeartBlockEntity.class);
            spawn.setAccessible(true);
            for (int attempt = 0; attempt < 20 && protector == null; attempt++) {
                protector = (Creaking) spawn.invoke(null, level, heart);
            }
            helper.assertTrue(protector != null && protector.isAlive() && level.getEntity(protector.getUUID()) == protector,
                    "Native heart spawning must actually add a living creaking to the soul valley");
            heart.setCreakingInfo(protector);
            helper.assertTrue(heart.isProtector(protector), "The spawned creaking must be bound to its generated heart");
            heart.creakingHurt();
            var resin = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:resin_clump"));
            int clumps = 0;
            for (BlockPos pos : BlockPos.betweenClosed(heartPos.offset(-4, -4, -4), heartPos.offset(4, 4, 4))) {
                if (level.getBlockState(pos).is(resin)) clumps++;
            }
            helper.assertTrue(clumps > 0, "A hurt heart-bound creaking must produce harvestable resin on custom pale oak");
            AOEMod.LOGGER.info("Soul valley lifecycle: generated heart at {}, live creaking, {} resin clumps", heartPos, clumps);
        } finally {
            if (protector != null) protector.discard();
            primary.setDayTime(previousTime);
            previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
        helper.succeed();
    }
}
