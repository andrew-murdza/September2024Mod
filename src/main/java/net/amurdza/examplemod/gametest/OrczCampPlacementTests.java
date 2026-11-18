package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.worldgen.OrczCampResidents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrczCampPlacementTests {
    private OrczCampPlacementTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void bothCampCommandsPlaceCompleteCampsAtRequestedHeight(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(Level.OVERWORLD);
        var marker = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("orcz", "camp_structure_generator"));
        var palisade = ModBlocks.ACACIA_PALISADE.get();
        for (int index = 0; index < 2; index++) {
            String feature = index == 0 ? "camp" : "camp_the_other";
            BlockPos pos = helper.absolutePos(new BlockPos(0, 0, 0)).offset(index * 64, 100, 64);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) level.getChunk((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z);
            }
            int result = level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack().withLevel(level)
                            .withPosition(Vec3.atLowerCornerOf(pos)).withSuppressedOutput(),
                    "place feature orcz:" + feature + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            helper.assertTrue(result == 1, "Manual Orcz feature placement must succeed without a rarity roll: " + feature);
            helper.assertFalse(level.getBlockState(pos.offset(1, 1, 1)).is(marker),
                    "The camp generator must expand immediately instead of leaving a marker: " + feature);
            int walls = 0;
            int chests = 0;
            for (BlockPos check : BlockPos.betweenClosed(pos.offset(-16, 0, -20), pos.offset(17, 17, 21))) {
                var state = level.getBlockState(check);
                if (state.is(palisade)) walls++;
                if (state.is(Blocks.CHEST)) chests++;
                helper.assertTrue(!new ResourceLocation("orcz", "orcsymbolred")
                        .equals(ForgeRegistries.BLOCKS.getKey(state.getBlock())), "Native camps must omit the red Orcz symbol");
            }
            helper.assertTrue(walls > 100 && chests > 0,
                    "The command must build a full camp, with walls and loot, at its requested height: " + feature);
            AABB bounds = new AABB(pos.offset(-16, 0, -20), pos.offset(18, 20, 23));
            var residents = level.getEntitiesOfClass(Mob.class, bounds,
                    mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("orcz"));
            for (String selected : new String[]{"orcshield", "orcsavagemace"}) {
                helper.assertTrue(residents.stream().anyMatch(mob -> ForgeRegistries.ENTITY_TYPES
                                .getKey(mob.getType()).getPath().equals(selected)),
                        "Each native camp must retain the chosen " + selected + ": " + feature);
            }
            helper.assertTrue(residents.stream().allMatch(mob -> {
                String id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getPath();
                return id.equals("orcshield") || id.equals("orcsavagemace") || id.equals("bigorcboss");
            }), "Native camps must contain only the two selected guards and their boss: " + feature);
            OrczCampResidents.ensureSelectedResidents(level, pos.offset(1, 1, 1));
            level.neighborChanged(pos.offset(1, 1, 1), marker, pos.offset(1, 1, 1));
            helper.assertTrue(level.getEntitiesOfClass(Mob.class, bounds,
                            mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("orcz"))
                            .size() == residents.size(),
                    "Repeating camp population and marker updates must not duplicate residents: " + feature);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalCampPlacementRetainsItsRarityRoll(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(Level.OVERWORLD);
        helper.assertTrue(RandomSource.create(0L).nextInt(1_000_000) + 1 > 8_000,
                "The regression seed must fail Orcz's native rarity roll");
        for (String name : new String[]{"camp", "camp_the_other"}) {
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation("orcz", name));
            helper.assertTrue(feature != null, "Missing native camp feature: " + name);
            helper.assertFalse(feature.place(level, level.getChunkSource().getGenerator(),
                            RandomSource.create(0L), helper.absolutePos(BlockPos.ZERO)),
                    "Non-command camp placement must retain native world-generation rules: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 400)
    public static void occupiedGuardSiteSearchesTheRemainingCampWithoutDuplicates(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(Level.OVERWORLD);
        BlockPos pos = helper.absolutePos(new BlockPos(128, 100, 192));
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
            level.getChunk((pos.getX() >> 4) + cx, (pos.getZ() >> 4) + cz);
        }
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level)
                        .withPosition(Vec3.atLowerCornerOf(pos)).withSuppressedOutput(),
                "place feature orcz:camp " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        AABB bounds = new AABB(pos.offset(-16, 0, -20), pos.offset(18, 20, 23));
        level.getEntitiesOfClass(Mob.class, bounds, mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("orcz"))
                .forEach(net.minecraft.world.entity.Entity::discard);
        BlockPos preferred = pos.offset(1, 3, 1);
        for (BlockPos blocked : BlockPos.betweenClosed(preferred.offset(-3, 0, -3), preferred.offset(3, 3, 3))) {
            level.setBlock(blocked, Blocks.STONE.defaultBlockState(), 2);
        }
        OrczCampResidents.ensureSelectedResidents(level, pos.offset(1, 1, 1));
        var mace = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("orcz", "orcsavagemace"));
        var guards = level.getEntitiesOfClass(Mob.class, bounds, mob -> mob.getType() == mace);
        helper.assertTrue(guards.size() == 1 && level.noCollision(guards.get(0)),
                "A blocked preferred courtyard must still receive one unobstructed mace guard");
        helper.assertTrue(Math.max(Math.abs(guards.get(0).blockPosition().getX() - preferred.getX()),
                        Math.abs(guards.get(0).blockPosition().getZ() - preferred.getZ())) > 3,
                "The fixture must exercise a supported position beyond the old three-block search radius");
        OrczCampResidents.ensureSelectedResidents(level, pos.offset(1, 1, 1));
        helper.assertTrue(level.getEntitiesOfClass(Mob.class, bounds, mob -> mob.getType() == mace).size() == 1,
                "Repeated fallback must not duplicate a relocated guard");
        level.getEntitiesOfClass(Mob.class, bounds, mob -> ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("orcz"))
                .forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }
}
