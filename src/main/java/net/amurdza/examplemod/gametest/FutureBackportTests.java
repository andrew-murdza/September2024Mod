package net.amurdza.examplemod.gametest;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariants;
import com.blackgear.vanillabackport.common.level.items.spear.SpearItem;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.future.FutureContentPolicy;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.registry.ModItems;
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
        for (String name : new String[]{"wooden_spear", "stone_spear", "copper_spear", "iron_spear",
                "golden_spear", "diamond_spear", "netherite_spear", "copper_sword", "copper_shovel",
                "copper_pickaxe", "copper_axe", "copper_hoe", "copper_helmet", "copper_chestplate",
                "copper_leggings", "copper_boots", "copper_chain"}) {
            ResourceLocation oldId = new ResourceLocation(AOEMod.MOD_ID, name);
            ResourceLocation nativeId = new ResourceLocation("minecraft", name);
            helper.assertFalse(BuiltInRegistries.ITEM.containsKey(oldId), "Duplicate AoE registration must be absent: " + name);
            helper.assertTrue(FutureContentPolicy.canonicalItem(oldId).equals(nativeId)
                            && BuiltInRegistries.ITEM.containsKey(nativeId) && !FutureContentPolicy.isExcludedItem(nativeId),
                    "Old IDs must migrate to their available native replacement: " + name);
        }
        helper.assertFalse(BuiltInRegistries.BLOCK.containsKey(new ResourceLocation(AOEMod.MOD_ID, "copper_chain")),
                "Duplicate copper chain block must be absent too");
        ItemStack migrated = new ItemStack(ModItems.IRON_SPEAR.get());
        migrated.setHoverName(Component.literal("Legacy spear"));
        helper.assertTrue(migrated.getItem() instanceof SpearItem, "All equipment references must use the complete native spear");
        helper.assertTrue(FutureContentPolicy.canonicalStack(migrated) == migrated
                        && migrated.getHoverName().getString().equals("Legacy spear"),
                "Native replacements must preserve their existing stack and NBT");
        var netherite = (SpearItem) BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", "netherite_spear"));
        helper.assertTrue(netherite.getTier() == ModToolTiers.REBALANCED_NETHERITE, "Netherite spear must use the copper/iron midpoint tier");
        helper.assertTrue(netherite.getAttackDamage() == ModToolTiers.REBALANCED_NETHERITE.getAttackDamageBonus(), "Spear attack damage must use the midpoint, including its constructor-cached value");
        helper.assertTrue(!FutureContentPolicy.isExcludedItem(new ResourceLocation("aoemod", "bone_spear")), "Bone spear must stay available");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void redShrubRequiresGrassAndDoesNotBurn(GameTestHelper helper) {
        var shrub = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", "red_shrub"));
        helper.assertTrue(shrub != Blocks.AIR, "Red shrub must be registered");
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos plant = helper.absolutePos(support.above());
        helper.setBlock(support, Blocks.GRASS_BLOCK);
        helper.assertTrue(shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must survive on grass");
        helper.setBlock(support, Blocks.CRIMSON_NYLIUM);
        helper.assertFalse(shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must no longer survive on crimson nylium");
        helper.setBlock(support, Blocks.DIRT);
        helper.assertFalse(shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must not survive on dirt");
        helper.setBlock(support, Blocks.WARPED_NYLIUM);
        helper.assertFalse(shrub.defaultBlockState().canSurvive(helper.getLevel(), plant), "Red shrubs must not survive on warped nylium");
        for (var side : net.minecraft.core.Direction.values()) {
            helper.assertTrue(shrub.defaultBlockState().getFlammability(helper.getLevel(), plant, side) == 0
                            && shrub.defaultBlockState().getFireSpreadSpeed(helper.getLevel(), plant, side) == 0,
                    "Red shrubs must neither burn nor spread fire on any face");
        }
        helper.assertFalse(shrub.defaultBlockState().ignitedByLava(), "Red shrubs must not be ignited by lava");
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
