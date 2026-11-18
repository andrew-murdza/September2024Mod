package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.BossSummonPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossSummonPositionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void indoorBossSummonsUseTheirRoomFloorAndRespectHeadroom(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(8, 20, 8));
        var queen = ModEntities.SPIDER_QUEEN.get().create(level);
        queen.moveTo(base.getX() + .5, base.getY() + 1, base.getZ() + .5, 0, 0);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            for (int dy = 0; dy <= 8; dy++) {
                level.setBlock(base.offset(dx, dy, dz), (dy == 0 || dy == 4 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
            }
        }
        for (int seed = 0; seed < 64; seed++) {
            for (var type : new EntityType<?>[]{EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.VINDICATOR}) {
                BlockPos feet = BossSummonPosition.find(level, queen, type, RandomSource.create(seed));
                helper.assertTrue(feet != null && feet.getY() == base.getY() + 1,
                        "Summons must appear inside the room, never on the higher roof");
            }
        }
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            level.setBlock(base.offset(dx, 2, dz), Blocks.STONE.defaultBlockState(), 18);
        }
        BlockPos cramped = BossSummonPosition.find(level, queen, EntityType.VINDICATOR, RandomSource.create(23));
        helper.assertTrue(cramped == null,
                "A one-block-high room must reject a full-height illager summon: " + cramped + ", room=" + base);
        helper.assertTrue(BossSummonPosition.find(level, queen, EntityType.CAVE_SPIDER, RandomSource.create(23)) != null,
                "A cave spider should still fit the same low tunnel");
        helper.succeed();
    }
}
