package net.amurdza.examplemod.gametest;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalAnimalBehaviorTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void elephantCannotRearFromAiSaveDataOrAttack(GameTestHelper helper) {
        var elephant = (EntityElephant) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexsmobs:elephant")).create(helper.getLevel());
        try {
            elephant.setStanding(true);
            helper.assertFalse(elephant.isStanding(), "AI must not start the elephant's rearing pose");
            var saved = new CompoundTag();
            elephant.addAdditionalSaveData(saved);
            saved.putBoolean("Standing", true);
            elephant.readAdditionalSaveData(saved);
            helper.assertFalse(elephant.isStanding(), "Old save data must not restore rearing");
            elephant.setAnimation(EntityElephant.ANIMATION_STOMP);
            helper.assertTrue(elephant.getAnimation() == EntityElephant.ANIMATION_FLING,
                    "The rearing stomp must use the native grounded melee attack");
            elephant.setAnimation(EntityElephant.ANIMATION_EAT);
            helper.assertTrue(elephant.getAnimation() == EntityElephant.ANIMATION_EAT, "Keep unrelated elephant animations");
        } finally { elephant.discard(); }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void tallPlantsRejectSpawnBoundsOnlyForStarfish(GameTestHelper helper) {
        var level = helper.getLevel();
        var obstacle = helper.absolutePos(new BlockPos(3, 2, 3));
        var control = net.minecraft.world.entity.EntityType.COD.create(level);
        try {
            for (String id : new String[]{"starfish"}) {
                var animal = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id)).create(level);
                try {
                    animal.setPos(obstacle.getX() + .5, obstacle.getY(), obstacle.getZ() + .5);
                    for (var block : new net.minecraft.world.level.block.Block[]{Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS}) {
                        level.setBlock(obstacle, block.defaultBlockState(), 18);
                        helper.assertTrue(net.amurdza.examplemod.worldgen.SmallAquaticPlants.hasSpace(level, animal, animal.getBoundingBox())
                                        == (block == Blocks.SEAGRASS),
                                "Spawn bounds must reject tall plants and allow short seagrass for " + id);
                        helper.assertTrue(net.amurdza.examplemod.worldgen.SmallAquaticPlants.hasSpace(level, control, animal.getBoundingBox()),
                                "Unrelated fish must retain their ordinary plant spawn checks");
                    }
                } finally { animal.discard(); }
            }
        } finally { control.discard(); }
        helper.succeed();
    }
}
