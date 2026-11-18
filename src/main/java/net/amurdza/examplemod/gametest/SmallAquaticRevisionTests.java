package net.amurdza.examplemod.gametest;

import dev.hybridlabs.aquatic.entity.crustacean.CoconutCrabEntity;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.CoconutCrabAquatics;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmallAquaticRevisionTests {
    private static final List<String> SMALL = List.of("flower_crab", "lightfoot_crab", "starfish");

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeBodySizeSurvivesJoinMetadataAndSaveReload(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (var level : List.of(helper.getLevel(), tropical)) for (String id : SMALL) {
            var animal = (BaseWaterAnimal) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id)).create(level);
            helper.assertTrue(AquaticMobSizes.usesNativeSize(animal), "Missing native-size exemption: " + id);
            try {
                for (int size : new int[]{-5, 0, 5}) {
                    animal.setSize(size);
                    DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(animal, level, true));
                    helper.assertTrue(animal.getSize() == size, "Join processing overwrote native size for " + id);
                    animal.getEntityData().set(BaseWaterAnimal.Companion.getSIZE(), size);
                    helper.assertTrue(animal.getSize() == size, "Client metadata is still forced large for " + id);
                    CompoundTag saved = new CompoundTag();
                    animal.addAdditionalSaveData(saved);
                    animal.readAdditionalSaveData(saved);
                    animal.refreshDimensions();
                    var dimensions = animal.getDimensions(animal.getPose());
                    helper.assertTrue(animal.getSize() == size && Math.abs(animal.getBbWidth() - dimensions.width) < .0001
                                    && Math.abs(animal.getBbHeight() - dimensions.height) < .0001,
                            "Native saved scale and collision bounds disagree for " + id);
                }
            } finally { animal.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void onlyTallSeagrassAndKelpBlockTheThreeSmallBottomDwellers(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos obstacle = helper.absolutePos(new BlockPos(3, 2, 3));
        var control = EntityType.COD.create(level);
        for (String id : SMALL) {
            var animal = (BaseWaterAnimal) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id)).create(level);
            try {
                for (var block : List.of(Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT,
                        Blocks.SEA_PICKLE, Blocks.TUBE_CORAL, Blocks.BLUE_ORCHID, Blocks.WATER)) {
                    var state = block.defaultBlockState();
                    boolean blocked = block == Blocks.TALL_SEAGRASS || block == Blocks.KELP || block == Blocks.KELP_PLANT;
                    helper.assertTrue(!state.getCollisionShape(level, obstacle, CollisionContext.of(animal)).isEmpty() == blocked,
                            "Wrong plant collision for " + id + "/" + block);
                    helper.assertTrue(state.getCollisionShape(level, obstacle, CollisionContext.of(control)).isEmpty(),
                            "Plant collision leaked to unrelated fish: " + block);
                }
                for (int y = -1; y <= 2; y++) level.setBlock(obstacle.above(y), Blocks.KELP_PLANT.defaultBlockState(), 18);
                animal.setPos(obstacle.getX() - 1, obstacle.getY(), obstacle.getZ() + .5);
                animal.move(MoverType.SELF, new Vec3(2, 0, 0));
                helper.assertTrue(animal.getBoundingBox().maxX <= obstacle.getX() + .0001,
                        "Real entity movement entered the kelp column: " + id);
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
    public static void coconutFreshSpawnsHaveNoShellAndStayHealthyUnderwater(GameTestHelper helper) {
        var level = helper.getLevel();
        var crab = (CoconutCrabEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:coconut_crab")).create(level);
        BlockPos tank = helper.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos pos : BlockPos.betweenClosed(tank.offset(-1, -1, -1), tank.offset(1, 2, 1)))
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 18);
        crab.moveTo(tank.getX() + .5, tank.getY(), tank.getZ() + .5);
        crab.setShellItem(new ItemStack(Items.TURTLE_HELMET));
        CoconutCrabAquatics.joined(new EntityJoinLevelEvent(crab, level, false));
        helper.assertTrue(crab.getShellItem().isEmpty(), "Fresh coconut crabs must spawn without a carried shell");
        float health = crab.getHealth();
        for (int tick = 0; tick < 360; tick++) crab.baseTick();
        helper.assertTrue(crab.isInWater() && crab.getHealth() == health && crab.getAirSupply() == crab.getMaxAirSupply(),
                "The coconut crab must remain an underwater animal");
        crab.setShellItem(new ItemStack(Items.TURTLE_HELMET));
        CoconutCrabAquatics.joined(new EntityJoinLevelEvent(crab, level, true));
        helper.assertFalse(crab.getShellItem().isEmpty(), "Loading an existing owned crab must not erase its carried item");
        crab.discard();
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
