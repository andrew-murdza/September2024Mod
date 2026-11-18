package net.amurdza.examplemod.gametest;

import alabaster.hearthandharvest.common.block.CornStalkBlock;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalMobAndMovementTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void matureCornAllowsCollisionAndLandPathsThroughEverySection(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("hearthandharvest:corn_stalk"));
        helper.assertTrue(block instanceof CornStalkBlock, "The real corn block must be loaded");
        var cow = EntityType.COW.create(level);
        helper.assertTrue(cow != null, "Could not create a movement candidate");
        for (var section : CornStalkBlock.CornSection.values()) {
            for (int age = 0; age <= 5; age++) {
                var state = block.defaultBlockState().setValue(CornStalkBlock.SECTION, section)
                        .setValue(CornStalkBlock.AGE, age).setValue(CornStalkBlock.PASSABLE, false);
                helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(cow)).isEmpty(),
                        "Corn must not block entities: " + section + " age " + age);
                helper.assertTrue(state.isPathfindable(level, pos, PathComputationType.LAND),
                        "Corn must not block a land navigation route: " + section + " age " + age);
                helper.assertFalse(state.getShape(level, pos).isEmpty(),
                        "Passable corn must retain its selectable outline for harvesting");
            }
        }
        level.setBlock(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        var sections = new CornStalkBlock.CornSection[]{CornStalkBlock.CornSection.BOTTOM,
                CornStalkBlock.CornSection.MIDDLE, CornStalkBlock.CornSection.TOP};
        // The source block removes unsupported mature sections during shape
        // updates. Install a complete native stalk before testing movement.
        for (int i = 0; i < sections.length; i++) {
            level.setBlock(pos.above(i), block.defaultBlockState().setValue(CornStalkBlock.AGE, 5)
                    .setValue(CornStalkBlock.SECTION, sections[i]),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        }
        cow.setPos(pos.getX() - 0.75, pos.getY(), pos.getZ() + 0.5);
        double before = cow.getX();
        cow.move(MoverType.SELF, new Vec3(1.5, 0, 0));
        helper.assertTrue(Math.abs(cow.getX() - before - 1.5) < 0.001,
                "An entity must physically move through a mature corn column");
        helper.assertTrue(level.getBlockState(pos).is(block), "Walking through corn must retain the crop");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void pickerelweedFormsDoNotApplyAStuckMovementMultiplier(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        int[] stuckCalls = {0};
        var cow = new Cow(EntityType.COW, level) {
            @Override
            public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
                stuckCalls[0]++;
                super.makeStuckInBlock(state, multiplier);
            }
        };
        for (String name : new String[]{"pickerelweed", "tall_pickerelweed", "pickerelweed_block"}) {
            var plant = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("upgrade_aquatic", name));
            helper.assertTrue(plant != null && plant != Blocks.AIR, "Missing pickerelweed form " + name);
            for (var state : plant.getStateDefinition().getPossibleStates()) {
                state.entityInside(level, pos, cow);
                helper.assertTrue(stuckCalls[0] == 0, "Pickerelweed must preserve movement speed: " + state);
            }
        }
        Blocks.COBWEB.defaultBlockState().entityInside(level, pos, cow);
        helper.assertTrue(stuckCalls[0] > 0, "The fixture must still observe genuine slowdown from other blocks");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void variableAquaticSpawnsUseTheirMaximumAndPersistIt(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (String id : new String[]{"hybrid_aquatic:octopus", "hybrid_aquatic:umbrella_octopus", "hybrid_aquatic:starfish"}) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            var aquatic = (dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal) type.create(tropical);
            int maximum = ((net.amurdza.examplemod.mixins.othermods.hybridaquatic.MaximumAquaticSize) aquatic).aoemod$maximumSize();
            if (net.amurdza.examplemod.util.AquaticMobSizes.usesNativeSize(aquatic)) maximum = 0;
            var difficulty = tropical.getCurrentDifficultyAt(BlockPos.ZERO);
            DocumentedNaturalSpawns.finalizeSpawn(new MobSpawnEvent.FinalizeSpawn(aquatic, tropical, 0, 62, 0,
                    difficulty, MobSpawnType.NATURAL, null, null, null));
            aquatic.finalizeSpawn(tropical, difficulty, MobSpawnType.NATURAL, null, null);
            aquatic.setSize(0);
            DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(aquatic, tropical));
            helper.assertTrue(aquatic.getSize() == maximum, "Spawn completion must choose the native maximum: " + id);
            CompoundTag saved = new CompoundTag();
            aquatic.addAdditionalSaveData(saved);
            var reloaded = (dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal) type.create(tropical);
            reloaded.readAdditionalSaveData(saved);
            helper.assertTrue(reloaded.getSize() == maximum, "Maximum size must survive save/reload: " + id);
        }
        helper.succeed();
    }
}
