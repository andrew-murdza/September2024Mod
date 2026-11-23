package net.amurdza.examplemod.gametest;

import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.TropicalUmbrellaVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmallAquaticRevisionTests {
    private static final List<String> SMALL = List.of("starfish");

    @GameTest(template = "plant_gameplay_empty")
    public static void everyStarfishStyleKeepsMaximumAcrossMetadataWorldSavesAndCaptureData(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", "starfish"));
        helper.assertTrue(type.getCategory() == MobCategory.WATER_AMBIENT, "Starfish must retain their ambient population category");
        for (var level : List.of(helper.getLevel(), tropical)) for (Type variant : Type.values()) {
            var animal = (StarfishEntity) type.create(level);
            try {
                animal.setVariant(variant);
                animal.setStarfishColor(0xE9A86F);
                animal.setOverlayColor(0xAD552C);
                helper.assertTrue(AquaticMobSizes.usesMaximumSize(animal), "Missing maximum-size rule for " + variant);
                for (int size : new int[]{-100, -5, 0, 5, 100}) {
                    animal.setSize(size);
                    DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(animal, level, true));
                    helper.assertTrue(animal.getSize() == 5, "Size writes or saved-world joins must not shrink " + variant);
                    animal.getEntityData().set(BaseWaterAnimal.Companion.getSIZE(), size);
                    helper.assertTrue(animal.getSize() == 5, "Stale client metadata must still display maximum " + variant);
                    helper.assertTrue(Math.abs(BaseWaterAnimal.Companion.getScaleAdjustment(animal, .05F) - 1.25F) < .0001,
                            "Native rendering must use the maximum scale for " + variant);
                    CompoundTag saved = new CompoundTag();
                    animal.saveWithoutId(saved);
                    helper.assertTrue(saved.getInt("Size") == 5, "Saving must persist maximum size for " + variant);
                    saved.putInt("Size", size);
                    saved.putBoolean("FromFishingNet", true);
                    var rescued = (StarfishEntity) type.create(level);
                    try {
                        // Native creature-net release loads its storedEntity compound using this exact path.
                        rescued.load(saved);
                        DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(rescued, level, true));
                        var dimensions = rescued.getDimensions(rescued.getPose());
                        helper.assertTrue(rescued.getSize() == 5 && rescued.getVariant() == variant
                                        && rescued.getStarfishColor() == 0xE9A86F && rescued.getOverlayColor() == 0xAD552C,
                                "Capture/save restoration must maximize size while preserving style and colors: " + variant);
                        helper.assertTrue(Math.abs(rescued.getBbWidth() - dimensions.width) < .0001
                                        && Math.abs(rescued.getBbHeight() - dimensions.height) < .0001,
                                "Restored collision bounds must match the retained native style: " + variant);
                        helper.assertTrue(rescued.getEntityData().get(BaseWaterAnimal.Companion.getSIZE()) == 5,
                                "Restored metadata must synchronize the maximum size: " + variant);
                    } finally { rescued.discard(); }
                }
            } finally { animal.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyStarfishTreatTallSeagrassAndKelpAsSolid(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos obstacle = helper.absolutePos(new BlockPos(3, 2, 3));
        var control = EntityType.COD.create(level);
        for (String id : List.of("alexsmobs:lobster", "hybrid_aquatic:flower_crab", "hybrid_aquatic:lightfoot_crab")) {
            var unrelated = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)).create(level);
            helper.assertFalse(net.amurdza.examplemod.worldgen.SmallAquaticPlants.affected(unrelated),
                    "Starfish masking and collision must not apply to lobsters or removed crabs: " + id);
            unrelated.discard();
        }
        for (String id : SMALL) {
            var animal = (BaseWaterAnimal) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id)).create(level);
            try {
                for (var block : List.of(Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT,
                        Blocks.SEA_PICKLE, Blocks.TUBE_CORAL, Blocks.BLUE_ORCHID, Blocks.WATER)) {
                    var state = block.defaultBlockState();
                    boolean blocked = block == Blocks.TALL_SEAGRASS || block == Blocks.KELP || block == Blocks.KELP_PLANT;
                    var nativeShape = state.getCollisionShape(level, obstacle, CollisionContext.empty());
                    var expectedShape = blocked ? Shapes.block() : nativeShape;
                    helper.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(level, obstacle, CollisionContext.of(animal)),
                                    expectedShape, BooleanOp.NOT_SAME),
                            "Wrong plant collision for " + id + "/" + block);
                    helper.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(level, obstacle, CollisionContext.of(control)),
                                    nativeShape, BooleanOp.NOT_SAME),
                            "Plant collision leaked to unrelated fish: " + block);
                }
                for (var block : List.of(Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT)) {
                    for (int y = -1; y <= 2; y++) level.setBlock(obstacle.above(y), block.defaultBlockState(), 18);
                    for (var movement : List.of(new Vec3(2, 0, 0), new Vec3(2, 0, .2), new Vec3(2, -.1, 0))) {
                        animal.setPos(obstacle.getX() - 1, obstacle.getY(), obstacle.getZ() + .5);
                        animal.move(MoverType.SELF, movement);
                        helper.assertTrue(animal.getBoundingBox().maxX <= obstacle.getX() + .0001,
                                "Real entity movement entered tall aquatic plant: " + id + "/" + block + "/" + movement);
                    }
                    control.setPos(obstacle.getX() - 1, obstacle.getY(), obstacle.getZ() + .5);
                    control.move(MoverType.SELF, new Vec3(2, 0, 0));
                    helper.assertTrue(control.getX() > obstacle.getX(), "Other fish must cross tall aquatic plants");
                }
                for (int y = -1; y <= 2; y++) level.setBlock(obstacle.above(y), Blocks.WATER.defaultBlockState(), 18);
                level.setBlock(obstacle, Blocks.SEAGRASS.defaultBlockState(), 18);
                animal.setPos(obstacle.getX() - 1, obstacle.getY(), obstacle.getZ() + .5);
                animal.move(MoverType.SELF, new Vec3(2, 0, 0));
                helper.assertTrue(animal.getX() > obstacle.getX(), "Short seagrass must remain passable: " + id);
                helper.assertTrue(level.getBlockState(obstacle).is(Blocks.SEAGRASS), "Crossing short grass removed the plant block");
            } finally { animal.discard(); }
        }
        control.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void picturedBrownUmbrellaIsAbsentWithoutChangingEntityIdentity(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:umbrella_octopus"));
        var octopus = type.create(tropical);
        var control = type.create(helper.getLevel());
        try {
            for (int value = -20; value <= 20; value++) {
                UUID uuid = new UUID(71, value);
                octopus.setUUID(uuid);
                control.setUUID(uuid);
                helper.assertFalse(TropicalUmbrellaVariants.color(octopus).equals("brown"), "Tropical renderer still selects the pictured color");
                helper.assertTrue(TropicalUmbrellaVariants.excludedNaturalVariant(octopus)
                                == TropicalUmbrellaVariants.color(control).equals("brown"),
                        "Natural spawn filter and render palette disagree");
                helper.assertTrue(octopus.getUUID().equals(uuid), "Removing a color must never mutate persistent identity");
            }
        } finally { octopus.discard(); control.discard(); }
        helper.succeed();
    }
}
