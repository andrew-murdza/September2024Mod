package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariants;
import com.blackgear.vanillabackport.common.level.items.spear.SpearItem;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.future.FutureContentPolicy;
import net.amurdza.examplemod.item.ModToolTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FutureBackportTests {
    private FutureBackportTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void armoredPawsWolfHealthAndFeeding(GameTestHelper helper) {
        var wolf = EntityType.WOLF.create(helper.getLevel());
        var player = helper.makeMockSurvivalPlayer();
        wolf.tame(player);
        helper.assertTrue(wolf.getMaxHealth() == 40F, "Tamed wolves must have the Armored Paws health pool");
        wolf.setHealth(10F);
        ItemStack food = new ItemStack(Items.BEEF);
        float expectedHealing = food.getFoodProperties(wolf).getNutrition() * 2F;
        player.setItemInHand(InteractionHand.MAIN_HAND, food);
        wolf.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(wolf.getHealth() == 10F + expectedHealing, "Feeding a wolf must restore twice the food nutrition");
        var attacker = EntityType.PIG.create(helper.getLevel());
        wolf.invulnerableTime = 0;
        float previousHealth = wolf.getHealth();
        wolf.hurt(helper.getLevel().damageSources().mobAttack(attacker), 6F);
        helper.assertTrue(wolf.getHealth() == previousHealth - 6F, "Mob damage must no longer be halved before reaching wolf armor/health");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void completeSpearsMigrateAndKeepPackProgression(GameTestHelper helper) {
        ItemStack legacy = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("aoemod", "iron_spear")));
        legacy.setHoverName(Component.literal("Legacy spear"));
        ItemStack migrated = FutureContentPolicy.canonicalStack(legacy);
        helper.assertTrue(migrated.getItem() instanceof SpearItem, "Old nonbone spears must use the complete backport implementation");
        helper.assertTrue(migrated.getHoverName().getString().equals("Legacy spear"), "Migration must retain custom NBT");
        var netherite = (SpearItem) BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", "netherite_spear"));
        helper.assertTrue(netherite.getTier() == ModToolTiers.REBALANCED_NETHERITE, "Netherite spear must use the copper/iron midpoint tier");
        helper.assertTrue(netherite.getAttackDamage() == ModToolTiers.REBALANCED_NETHERITE.getAttackDamageBonus(), "Spear attack damage must use the midpoint, including its constructor-cached value");
        helper.assertTrue(!FutureContentPolicy.isExcludedItem(new ResourceLocation("aoemod", "bone_spear")), "Bone spear must stay available");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void redShrubUsesCrimsonRootRules(GameTestHelper helper) {
        var shrub = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", "red_shrub"));
        helper.assertTrue(shrub != Blocks.AIR, "Red shrub must be registered");
        var shrubState = shrub.defaultBlockState();
        var roots = Blocks.CRIMSON_ROOTS.defaultBlockState();
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos plant = helper.absolutePos(support.above());
        helper.setBlock(support, Blocks.CRIMSON_NYLIUM);
        helper.assertTrue(shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must survive on crimson nylium");
        helper.setBlock(support, Blocks.GRASS_BLOCK);
        helper.assertTrue(!shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must reject grass like crimson roots");
        helper.assertTrue(!((BonemealableBlock) shrub).isValidBonemealTarget(helper.getLevel(), plant, shrub.defaultBlockState(), false), "Red shrubs must not spread by direct bonemeal");
        helper.assertTrue(shrubState.getSoundType() == roots.getSoundType(), "Red shrubs must use crimson-root sounds");
        helper.assertTrue(shrubState.ignitedByLava() == roots.ignitedByLava(), "Red shrubs must inherit crimson-root lava ignition behavior");
        helper.assertTrue(shrubState.canBeReplaced() == roots.canBeReplaced(), "Red shrubs must inherit crimson-root replaceability");
        helper.assertTrue(shrubState.getShape(helper.getLevel(), plant).bounds().equals(roots.getShape(helper.getLevel(), plant).bounds()), "Red shrubs must use the crimson-root shape and position offset");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void requestedVariantsAndExclusionsApply(GameTestHelper helper) {
        var pig = EntityType.PIG.create(helper.getLevel());
        VariantDataHolder.<PigVariant>getHolder(pig).orElseThrow().setVariantData(PigVariants.REGISTRIES.get(PigVariants.WARM));
        helper.assertTrue(pig.getName().getString().equals(Component.translatable("entity.aoemod.boar").getString()), "Warm pigs must be named Boar");
        helper.assertTrue(!VanillaBackport.COMMON_CONFIG.hasNautilus.get(), "The full backport nautilus must remain disabled");
        helper.assertTrue(!VanillaBackport.COMMON_CONFIG.hasDriedGhasts.get(), "Dried ghasts must remain disabled");
        helper.assertTrue(FutureContentPolicy.isExcludedItem(new ResourceLocation("minecraft", "happy_ghast_spawn_egg")), "Happy ghasts must be hidden from normal acquisition");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation("minecraft", "bundle")).isPresent(), "The requested bundle recipe must survive filtering");
        helper.succeed();
    }
}
