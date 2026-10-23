package net.amurdza.examplemod.gametest;

import com.google.common.collect.Multimap;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.future.FutureMobCompatibility;
import net.amurdza.examplemod.event_handlers.BoneMeal;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Exercises the loaded dependency content through Minecraft APIs, without linking its implementation classes. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FutureContentGameTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    private FutureContentGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void requestedContentIsRegistered(GameTestHelper helper) {
        for (String path : new String[]{"sulfur", "cinnabar", "potent_sulfur", "red_shrub", "wildflowers",
                "copper_chest", "oak_shelf", "copper_bars", "copper_lantern", "copper_chain",
                "polished_tuff", "tuff_bricks", "golden_dandelion"}) {
            ResourceLocation id = new ResourceLocation("minecraft", path);
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Required block is missing: " + id);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Required block item is missing: " + id);
        }
        for (String path : new String[]{"wolf_armor", "armadillo_scute", "wooden_spear", "stone_spear",
                "copper_spear", "iron_spear", "golden_spear", "diamond_spear", "netherite_spear"}) {
            item(helper, "minecraft:" + path);
        }
        for (String color : new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
            item(helper, "minecraft:" + color + "_bundle");
        }
        for (String id : new String[]{"minecraft:armadillo", "minecraft:copper_golem", "minecraft:sulfur_cube",
                "minecraft:camel_husk", "minecraft:parched", "aoemod:bogged"}) {
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(new ResourceLocation(id)),
                    "Required entity is missing: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bundlesCraftWithEitherHideAndOneString(GameTestHelper helper) {
        for (Item hide : new Item[]{Items.LEATHER, Items.RABBIT_HIDE}) {
            var menu = new AbstractContainerMenu(null, -1) {
                @Override
                public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

                @Override
                public boolean stillValid(Player player) { return true; }
            };
            var grid = new TransientCraftingContainer(menu, 2, 2);
            grid.setItem(0, new ItemStack(Items.STRING));
            grid.setItem(2, new ItemStack(hide));
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "A string above " + hide + " must match a loaded bundle recipe");
            ItemStack result = recipe.orElseThrow().assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(Items.BUNDLE) && result.getCount() == 1,
                    "The two-ingredient bundle recipe must produce one bundle for " + hide);
            grid.setItem(0, ItemStack.EMPTY);
            helper.assertFalse(recipe.orElseThrow().matches(grid, helper.getLevel()), "Bundle crafting must require string");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void redShrubsUseAoeCrimsonRootSpreading(GameTestHelper helper) throws ReflectiveOperationException {
        var shrub = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", "red_shrub"));
        helper.assertTrue(shrub != Blocks.AIR, "Red shrub must be registered");
        var eligible = BoneMeal.class.getDeclaredMethod("isEligibleBonemealTarget", ServerLevel.class, BlockPos.class, BlockState.class);
        eligible.setAccessible(true);
        var flowerLike = BoneMeal.class.getDeclaredMethod("isFlowerLike", Block.class);
        flowerLike.setAccessible(true);
        for (Block block : new Block[]{Blocks.CRIMSON_ROOTS, shrub}) {
            helper.assertTrue((boolean) eligible.invoke(null, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), block.defaultBlockState()),
                    "AOE bonemeal must intercept " + block + " for the same root-spreading behavior");
            helper.assertTrue((boolean) flowerLike.invoke(null, block),
                    "AOE must route " + block + " to its existing flower/root spread implementation");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void copperGolemsStillConstructInPlains(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        helper.assertTrue(plains != null, "The plains dimension must be loaded");
        BlockPos testOrigin = helper.absolutePos(BlockPos.ZERO);
        BlockPos copperPos = new BlockPos(testOrigin.getX(), plains.getMaxBuildHeight() - 8, testOrigin.getZ());
        BlockPos pumpkinPos = copperPos.above();
        boolean alreadyForced = plains.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(copperPos.getX() >> 4, copperPos.getZ() >> 4));
        plains.setChunkForced(copperPos.getX() >> 4, copperPos.getZ() >> 4, true);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(plains.isPositionEntityTicking(copperPos),
                "Copper golem test chunk must become entity-ticking before spatial queries"))
                .thenExecute(() -> verifyCopperGolemConstruction(helper, plains, copperPos, pumpkinPos, alreadyForced)).thenSucceed();
    }

    private static void verifyCopperGolemConstruction(GameTestHelper helper, ServerLevel plains, BlockPos copperPos,
                                                       BlockPos pumpkinPos, boolean alreadyForced) {
        helper.assertTrue(plains.isEmptyBlock(copperPos) && plains.isEmptyBlock(pumpkinPos),
                "Copper golem test needs two empty blocks near the plains build ceiling");
        var golemType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft", "copper_golem"));
        AABB area = new AABB(copperPos).inflate(4);
        Set<UUID> originalEntities = new HashSet<>();
        plains.getEntities((Entity) null, area, entity -> entity.getType() == golemType)
                .forEach(entity -> originalEntities.add(entity.getUUID()));
        try {
            helper.assertTrue(plains.setBlock(copperPos, Blocks.COPPER_BLOCK.defaultBlockState(), 3),
                    "Plains must allow the copper base");
            helper.assertTrue(plains.setBlock(pumpkinPos, Blocks.CARVED_PUMPKIN.defaultBlockState(), 3),
                    "Plains must allow the carved pumpkin head");
            helper.assertTrue(plains.getEntities((Entity) null, area,
                    entity -> entity.getType() == golemType && !originalEntities.contains(entity.getUUID())).size() == 1,
                    "Placing a carved pumpkin over copper must spawn a copper golem; remaining blocks: "
                            + plains.getBlockState(copperPos) + " / " + plains.getBlockState(pumpkinPos));
            helper.assertTrue(plains.getBlockState(copperPos).is(
                            BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", "copper_chest"))),
                    "Copper golem construction must leave its copper chest");
            helper.assertTrue(plains.isEmptyBlock(pumpkinPos), "Copper golem construction must consume the pumpkin");
        } finally {
            plains.getEntities((Entity) null, area,
                    entity -> entity.getType() == golemType && !originalEntities.contains(entity.getUUID()))
                    .forEach(Entity::discard);
            plains.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), 3);
            plains.setBlock(copperPos, Blocks.AIR.defaultBlockState(), 3);
            if (!alreadyForced) plains.setChunkForced(copperPos.getX() >> 4, copperPos.getZ() >> 4, false);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void zombieNautilusCannotJoinTheWorld(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("minecraft", "zombie_nautilus");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "Excluded backport entity should exist for this integration test");
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(id).create(helper.getLevel());
        helper.assertTrue(entity != null, "Zombie nautilus must be constructible before testing its spawn rejection");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        try {
            helper.assertFalse(helper.getLevel().addFreshEntity(entity), "EntityJoinLevelEvent must reject zombie nautilus spawns");
            helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == null,
                    "A rejected zombie nautilus must not remain in the level");
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void directBlockWritesRespectDimensionRestrictions(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        helper.assertTrue(plains != null, "The plains dimension must be loaded");
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos customPos = new BlockPos(origin.getX(), plains.getMaxBuildHeight() - 8, origin.getZ());
        plains.getChunk(customPos);
        helper.assertTrue(plains.isEmptyBlock(customPos), "Placement test needs an empty plains ceiling position");
        try {
            helper.assertFalse(plains.setBlock(customPos, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3),
                    "Direct setBlock must reject snowy-only spruce planks in plains");
            helper.assertTrue(plains.isEmptyBlock(customPos), "A forbidden write must leave the original air intact");
            helper.assertFalse(plains.setBlock(customPos, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 3),
                    "The technical twisting-vine body must share its Nether-only tip restriction");
            helper.assertFalse(plains.setBlock(customPos, Blocks.POTTED_SPRUCE_SAPLING.defaultBlockState(), 3),
                    "A flower pot must not bypass its spruce content's dimension restriction");
            helper.assertTrue(plains.setBlock(customPos, Blocks.OAK_PLANKS.defaultBlockState(), 3),
                    "Direct setBlock must allow plains oak planks");
            helper.assertFalse(plains.setBlock(customPos, Blocks.LAPIS_BLOCK.defaultBlockState(), 3),
                    "Direct setBlock must reject snowy-only lapis in plains");
            helper.assertTrue(plains.getBlockState(customPos).is(Blocks.OAK_PLANKS),
                    "A rejected replacement must preserve the existing allowed block");
            helper.assertTrue(helper.getLevel().setBlock(origin, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3),
                    "Vanilla Overworld must still allow spruce planks");
            helper.assertTrue(helper.getLevel().setBlock(origin, Blocks.LAPIS_BLOCK.defaultBlockState(), 3),
                    "Vanilla Overworld must still allow lapis blocks");
        } finally {
            plains.setBlock(customPos, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void normalAndLapisGolemsRespectDimensions(GameTestHelper helper) {
        ServerLevel snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        helper.assertTrue(snowy != null, "The snowy dimension must be loaded");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 1));
        for (ServerLevel level : new ServerLevel[]{helper.getLevel(), snowy}) {
            boolean custom = level == snowy;
            BlockPos head = custom ? new BlockPos(origin.getX(), level.getMaxBuildHeight() - 8, origin.getZ()) : origin.above(2);
            level.getChunk(head);
            List<BlockPos> ironBody = List.of(head.below(), head.below(2), head.below().west(), head.below().east());
            assertGolemPattern(helper, level, head, ironBody, Blocks.IRON_BLOCK, Blocks.CARVED_PUMPKIN,
                    EntityType.IRON_GOLEM, !custom, false);
            assertGolemPattern(helper, level, head, ironBody, Blocks.IRON_BLOCK, Blocks.LAPIS_BLOCK,
                    EntityType.IRON_GOLEM, custom, true);
            assertGolemPattern(helper, level, head, List.of(head.below(), head.below(2)), Blocks.SNOW_BLOCK,
                    Blocks.CARVED_PUMPKIN, EntityType.SNOW_GOLEM, !custom, false);
        }
        // Plains permits both the iron frame and pumpkin, so this exercises the golem guard
        // independently of snowy's earlier pumpkin-placement rejection.
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        helper.assertTrue(plains != null, "The plains dimension must be loaded");
        BlockPos head = new BlockPos(origin.getX(), plains.getMaxBuildHeight() - 8, origin.getZ());
        plains.getChunk(head);
        assertGolemPattern(helper, plains, head, List.of(head.below(), head.below(2), head.below().west(), head.below().east()),
                Blocks.IRON_BLOCK, Blocks.CARVED_PUMPKIN, EntityType.IRON_GOLEM, false, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void armadilloAgeLockSurvivesCustomAging(GameTestHelper helper) throws ReflectiveOperationException {
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft", "armadillo"))
                .create(helper.getLevel());
        helper.assertTrue(entity instanceof AgeableMob, "The loaded armadillo must be an ageable mob");
        AgeableMob armadillo = (AgeableMob) entity;
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        armadillo.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        armadillo.setAge(-1000);
        var setAgeLocked = armadillo.getClass().getMethod("tiny_takeover_backport$setAgeLocked", boolean.class);
        setAgeLocked.invoke(armadillo, true);
        helper.assertTrue(FutureMobCompatibility.isAgeLocked(armadillo), "The optional age-lock bridge must read armadillo state");
        armadillo.ageUp(10, true);
        armadillo.aiStep();
        helper.assertTrue(armadillo.getAge() == -1000, "Custom food and natural aging must both honor the armadillo age lock");
        setAgeLocked.invoke(armadillo, false);
        armadillo.ageUp(10, true);
        helper.assertTrue(armadillo.getAge() > -1000, "Unlocking an armadillo must restore food-based growth");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void netheriteEquipmentUsesCopperIronMidpoints(GameTestHelper helper) {
        for (String kind : new String[]{"sword", "shovel", "pickaxe", "axe", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
            Item copper = item(helper, "aoemod:copper_" + kind);
            Item iron = item(helper, "minecraft:iron_" + kind);
            Item netherite = item(helper, "minecraft:netherite_" + kind);
            assertMidpointDurability(helper, copper, iron, netherite, kind);
            helper.assertTrue(netherite.getEnchantmentValue() == Math.round((copper.getEnchantmentValue() + iron.getEnchantmentValue()) / 2.0F),
                    "Netherite " + kind + " enchantability must be the rounded copper/iron midpoint");
            EquipmentSlot slot = netherite instanceof ArmorItem armor ? armor.getEquipmentSlot() : EquipmentSlot.MAINHAND;
            var copperAttributes = copper.getDefaultAttributeModifiers(slot);
            var ironAttributes = iron.getDefaultAttributeModifiers(slot);
            var netheriteAttributes = netherite.getDefaultAttributeModifiers(slot);
            for (Attribute attribute : new Attribute[]{Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED,
                    Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE}) {
                for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
                    double expected = (amount(copperAttributes, attribute, operation) + amount(ironAttributes, attribute, operation)) / 2.0D;
                    helper.assertTrue(Math.abs(amount(netheriteAttributes, attribute, operation) - expected) < 0.00001D,
                            "Netherite " + kind + " has the wrong copper/iron midpoint for " + attribute.getDescriptionId() + "/" + operation);
                }
            }
        }
        Item copperPickaxe = item(helper, "aoemod:copper_pickaxe");
        float expectedSpeed = (copperPickaxe.getDefaultInstance().getDestroySpeed(Blocks.STONE.defaultBlockState())
                + Items.IRON_PICKAXE.getDefaultInstance().getDestroySpeed(Blocks.STONE.defaultBlockState())) / 2.0F;
        helper.assertTrue(Items.NETHERITE_PICKAXE.getDefaultInstance().getDestroySpeed(Blocks.STONE.defaultBlockState()) == expectedSpeed,
                "Netherite mining speed must be the copper/iron midpoint");
        assertMidpointDurability(helper, item(helper, "aoemod:copper_shield"), item(helper, "aoemod:iron_shield"),
                item(helper, "aoemod:netherite_shield"), "shield");
        helper.succeed();
    }

    private static void assertMidpointDurability(GameTestHelper helper, Item copper, Item iron, Item netherite, String kind) {
        int expected = Math.round((copper.getDefaultInstance().getMaxDamage() + iron.getDefaultInstance().getMaxDamage()) / 2.0F);
        helper.assertTrue(netherite.getDefaultInstance().getMaxDamage() == expected,
                "Netherite " + kind + " durability must be the rounded copper/iron midpoint");
    }

    private static double amount(Multimap<Attribute, AttributeModifier> attributes, Attribute attribute, AttributeModifier.Operation operation) {
        return attributes.get(attribute).stream().filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static void assertGolemPattern(GameTestHelper helper, ServerLevel level, BlockPos head, List<BlockPos> body,
                                           Block material, Block headBlock, EntityType<?> type, boolean shouldSpawn, boolean postPlacement) {
        helper.assertTrue(level.isEmptyBlock(head) && body.stream().allMatch(level::isEmptyBlock),
                "Golem pattern needs empty test positions in " + level.dimension().location());
        AABB area = new AABB(head).inflate(4);
        Set<UUID> original = new HashSet<>();
        level.getEntities((Entity) null, area, entity -> entity.getType() == type)
                .forEach(entity -> original.add(entity.getUUID()));
        try {
            for (BlockPos pos : body) {
                helper.assertTrue(level.setBlock(pos, material.defaultBlockState(), 3), "Golem body material must be placeable");
            }
            level.setBlock(head, headBlock.defaultBlockState(), 3);
            boolean headPresent = level.getBlockState(head).is(headBlock);
            if (postPlacement) {
                helper.assertTrue(headPresent, "The lapis head must exist before posting its placement event in " + level.dimension().location());
                BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, head);
                var event = new BlockEvent.EntityPlaceEvent(snapshot, material.defaultBlockState(), FakePlayerFactory.getMinecraft(level));
                helper.assertTrue(event.getLevel() == level && event.getPlacedBlock().is(headBlock),
                        "Placement event must identify the tested dimension and actual lapis head");
                MinecraftForge.EVENT_BUS.post(event);
                helper.assertFalse(event.isCanceled(), "A permitted lapis placement must not be canceled by another handler");
            }
            int spawned = level.getEntities((Entity) null, area,
                    entity -> entity.getType() == type && !original.contains(entity.getUUID())).size();
            helper.assertTrue(spawned == (shouldSpawn ? 1 : 0),
                    headBlock + " over " + material + " in " + level.dimension().location() + " spawned " + spawned
                            + " golems; remaining head=" + level.getBlockState(head) + ", body=" + level.getBlockState(head.below()));
            helper.assertTrue(shouldSpawn || !headPresent ? level.isEmptyBlock(head) : level.getBlockState(head).is(headBlock),
                    "Construction/placement left an unexpected head block for " + headBlock + " in " + level.dimension().location());
        } finally {
            level.getEntities((Entity) null, area, entity -> entity.getType() == type && !original.contains(entity.getUUID()))
                    .forEach(Entity::discard);
            level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
            body.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3));
        }
    }

    private static Item item(GameTestHelper helper, String name) {
        ResourceLocation id = new ResourceLocation(name);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Required item is missing: " + id);
        return BuiltInRegistries.ITEM.get(id);
    }
}
