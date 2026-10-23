package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.entity.SwordfishEntity;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionLivestockTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void swordfishCanSpawnDeepAndCookThroughAllThreeMethods(GameTestHelper helper) {
        var level = helper.getLevel();
        var fish = ModEntities.SWORDFISH.get().create(level);
        helper.assertTrue(fish != null && fish.getMaxHealth() == 12
                        && fish.getNavigation() instanceof WaterBoundPathNavigation && fish.getTarget() == null,
                "Swordfish must have aquatic navigation, registered health and no unprovoked target");
        BlockPos water = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int dy = -1; dy <= 1; dy++) level.setBlockAndUpdate(water.above(dy), Blocks.WATER.defaultBlockState());
        helper.assertTrue(SwordfishEntity.checkSpawnRules(ModEntities.SWORDFISH.get(), level,
                        MobSpawnType.NATURAL, water, level.random),
                "Swordfish spawning must accept deep water independently of vanilla sea-level limits");
        level.setBlockAndUpdate(water.below(), Blocks.STONE.defaultBlockState());
        helper.assertFalse(SwordfishEntity.checkSpawnRules(ModEntities.SWORDFISH.get(), level,
                        MobSpawnType.NATURAL, water, level.random),
                "Swordfish spawning must reject a pool too shallow for its body");
        var input = new SimpleContainer(ModItems.RAW_SWORDFISH.get().getDefaultInstance());
        var manager = level.getRecipeManager();
        helper.assertTrue(manager.getRecipeFor(RecipeType.SMELTING, input, level).orElseThrow()
                        .getResultItem(level.registryAccess()).is(ModItems.COOKED_SWORDFISH.get())
                        && manager.getRecipeFor(RecipeType.SMOKING, input, level).orElseThrow()
                        .getResultItem(level.registryAccess()).is(ModItems.COOKED_SWORDFISH.get())
                        && manager.getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).orElseThrow()
                        .getResultItem(level.registryAccess()).is(ModItems.COOKED_SWORDFISH.get()),
                "All three actual recipe types must cook swordfish steak");
        helper.assertTrue(ModItems.RAW_SWORDFISH.get().getDefaultInstance().is(ModTags.Items.fish)
                        && ModItems.RAW_SWORDFISH.get().isEdible() && ModItems.COOKED_SWORDFISH.get().isEdible(),
                "The raw fish must enter existing fish recipes/feed and both steaks must be edible");
        helper.assertFalse(MobConfig.mobDropEntries(fish).isEmpty(), "Swordfish must have its regional food economy row");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void arthropleuraBreedsAsHerbivorousGrazingLivestock(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var parent = ModEntities.ARTHROPLEURA.get().create(tropical);
        var mate = ModEntities.ARTHROPLEURA.get().create(tropical);
        helper.assertTrue(parent != null && parent.getMaxHealth() == 24 && parent.getMobType() == MobType.ARTHROPOD,
                "Arthropleura must have registered arthropod attributes");
        helper.assertTrue(parent.isFood(new ItemStack(Items.OAK_LEAVES))
                        && parent.isFood(new ItemStack(Items.FERN))
                        && parent.isFood(new ItemStack(Items.BROWN_MUSHROOM))
                        && !parent.isFood(ModItems.RAW_SWORDFISH.get().getDefaultInstance()),
                "Arthropleura must accept plant forage and reject meat");
        var baby = parent.getBreedOffspring(tropical, mate);
        helper.assertTrue(baby != null && baby.getType() == parent.getType(),
                "Breeding must create actual Arthropleura offspring");
        baby.setAge(-24000);
        baby.setHealth(2);
        baby.ate();
        helper.assertTrue(baby.getAge() > -24000 && baby.getHealth() > 2,
                "Grazing in its native tropical habitat must nourish and mature young Arthropleura; age="
                        + baby.getAge() + ", health=" + baby.getHealth());
        helper.assertFalse(MobConfig.mobDropEntries(parent).isEmpty(), "Arthropleura must have its regional livestock economy row");
        helper.succeed();
    }
}
