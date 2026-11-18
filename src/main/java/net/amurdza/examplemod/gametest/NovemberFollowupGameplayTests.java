package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.BossDeathChests;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.event_handlers.ItemsPlaceModBlocks;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.CreativeCatalog;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.snow.block.entity.SnowBlockEntity;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.CompoundContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NovemberFollowupGameplayTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void plantingBlueberriesPreservesSnowAndConsumesOneFruit(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var player = h.makeMockSurvivalPlayer();
        Item blueberry = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight:blueberry"));
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        // Click the snow itself, as a player does when aiming at the ground.
        level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blueberry, 2));
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        ItemsPlaceModBlocks.sunflowerPlace1(event);
        h.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction(), "Planting must succeed");
        h.assertTrue(level.getBlockEntity(pos) instanceof SnowBlockEntity, "Planting must preserve the snow container");
        var snow = (SnowBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(snow.getContainedState().is(ModBlocks.BLUE_BERRY_BUSH.get())
                        && level.getBlockState(pos).getValue(SnowLayerBlock.LAYERS) == 1,
                "The planted blue bush must retain the original layer count");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "Planting consumes exactly one fruit");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void actualBossDeathsPlaceOneNormalChestIncludingAlphaYeti(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        for (String name : new String[]{"aoemod:spider_queen", "aoemod:illager_lord", "twilightforest:alpha_yeti",
                "orcz:bigorcboss", "minecraft:wither"}) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            var boss = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(name)).create(level);
            boss.setNoAi(true);
            boss.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            level.addFreshEntity(boss);
            boss.die(level.damageSources().generic());
            h.assertTrue(level.getBlockState(pos).is(Blocks.CHEST), "Actual death failed to create normal chest: " + name);
            var chest = (ChestBlockEntity) level.getBlockEntity(pos);
            h.assertFalse(chest.isEmpty(), "Boss chest is empty: " + name);
            if (name.equals("aoemod:illager_lord")) {
                boolean shield = false;
                for (int i = 0; i < chest.getContainerSize(); i++) shield |= chest.getItem(i).is(ModItems.BOSS_SHIELD.get());
                h.assertTrue(shield, "The Lord's intact shield must move into its reward chest");
            }
            if (name.equals("twilightforest:alpha_yeti")) {
                Set<Item> allowed = Set.of(Items.DIAMOND, Items.EMERALD, Items.AMETHYST_SHARD,
                        Items.WHITE_WOOL, Items.BONE);
                for (int i = 0; i < chest.getContainerSize(); i++) h.assertTrue(chest.getItem(i).isEmpty()
                                || allowed.contains(chest.getItem(i).getItem()), "Alpha Yeti yielded a forbidden mountain reward");
            }
            var repeated = new LivingDropsEvent(boss, level.damageSources().generic(), new ArrayList<>(), 0, true);
            BossDeathChests.handle(repeated);
            h.assertTrue(repeated.getDrops().isEmpty(), "Repeated death handling duplicated a reward chest");
            boss.discard();
            chest.clearContent();
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void airborneBossKeepsChestContentsAndMobLootRule(GameTestHelper h) {
        var level = h.getLevel();
        var boss = (LivingEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("aoemod:illager_lord")).create(level);
        boss.moveTo(h.absolutePos(BlockPos.ZERO).getX(), level.getMaxBuildHeight() + 30, h.absolutePos(BlockPos.ZERO).getZ());
        var drops = new ArrayList<ItemEntity>();
        drops.add(new ItemEntity(level, boss.getX(), boss.getY(), boss.getZ(), new ItemStack(ModItems.BOSS_SHIELD.get())));
        var event = new LivingDropsEvent(boss, level.damageSources().generic(), drops, 0, true);
        BossDeathChests.handle(event);
        h.assertTrue(event.getDrops().size() == 1, "An unsupported death needs one portable chest");
        var chest = event.getDrops().iterator().next().getItem();
        h.assertTrue(chest.is(Items.CHEST) && chest.getTagElement("BlockEntityTag") != null, "Portable chest lost its inventory");
        var items = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(chest.getTagElement("BlockEntityTag"), items);
        h.assertTrue(items.stream().anyMatch(stack -> stack.is(ModItems.BOSS_SHIELD.get()))
                        && items.stream().anyMatch(stack -> stack.is(Items.IRON_INGOT)), "Portable chest lost shield or iron");
        boolean old = level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        try {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, level.getServer());
            var other = (LivingEntity) boss.getType().create(level);
            var disabled = new LivingDropsEvent(other, level.damageSources().generic(), new ArrayList<>(), 0, true);
            BossDeathChests.handle(disabled);
            h.assertTrue(disabled.getDrops().isEmpty(), "doMobLoot=false must suppress boss rewards");
        } finally { level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(old, level.getServer()); }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void doubleChestsCompactAcrossHalvesAndFullChestsFillPartialStacks(GameTestHelper h) {
        var level = h.getLevel();
        var leftPos = h.absolutePos(new BlockPos(3, 2, 3));
        var state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,
                ChestType.LEFT);
        var rightPos = leftPos.relative(ChestBlock.getConnectedDirection(state));
        level.setBlock(leftPos, state, 18);
        level.setBlock(rightPos, state.setValue(ChestBlock.TYPE,
                ChestType.RIGHT), 18);
        var left = (ChestBlockEntity) level.getBlockEntity(leftPos);
        var right = (ChestBlockEntity) level.getBlockEntity(rightPos);
        var table = new ResourceLocation("aoemod:chests/bosses/orc_boss");
        for (boolean openLeftFirst : new boolean[]{true, false}) {
            left.setLootTable(table, 164);
            right.setLootTable(table, 927);
            var first = openLeftFirst ? left : right;
            var second = openLeftFirst ? right : left;
            first.unpackLootTable(null);
            second.unpackLootTable(null);
            var both = ChestBlock.getContainer((ChestBlock) Blocks.CHEST,
                    level.getBlockState(leftPos), level, leftPos, true);
            h.assertTrue(both instanceof CompoundContainer, "Open the actual double-chest inventory");
            boolean reachedEmpty = false;
            for (int slot = 0; slot < both.getContainerSize(); slot++) {
                if (both.getItem(slot).isEmpty()) reachedEmpty = true;
                else h.assertFalse(reachedEmpty, "Loot must fill consecutive slots starting at the top left");
            }
            for (Item item : List.of(Items.BONE, Items.LEATHER)) {
                int total = 0, partial = 0;
                for (int slot = 0; slot < both.getContainerSize(); slot++) {
                    var stack = both.getItem(slot);
                    if (!stack.is(item)) continue;
                    total += stack.getCount();
                    if (stack.getCount() < item.getMaxStackSize()) partial++;
                }
                h.assertTrue(total >= 80 && total <= 128 && partial <= 1,
                        "Both independent chest rolls must produce full stacks and at most one partial stack of " + item);
            }
            left.clearContent();
            right.clearContent();
        }

        left.clearContent();
        right.clearContent();
        for (int slot = 0; slot < left.getContainerSize(); slot++) {
            left.setItem(slot, new ItemStack(Items.STONE, 64));
            right.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        left.setItem(0, new ItemStack(Items.BONE, 60));
        right.setItem(1, new ItemStack(Items.BONE, 58));
        var tagged = new ItemStack(Items.BONE, 11);
        tagged.setHoverName(Component.literal("Named bone"));
        left.setItem(1, tagged);
        var bones = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(Items.BONE))
                        .apply(SetItemCountFunction.setCount(
                                ConstantValue.exactly(9)))).build();
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN,
                        Vec3.atCenterOf(leftPos))
                .create(LootContextParamSets.CHEST);
        bones.fill(left, params, 109);
        h.assertTrue(left.getItem(0).getCount() == 64 && right.getItem(1).getCount() == 63 && left.getItem(1).getCount() == 11,
                "A full double chest must use compatible partial stacks on both sides and preserve item tags");
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void fixedYetiGoblinAndRedcapDropsMatchCountsAndOdds(GameTestHelper h) {
        var level = h.getLevel();
        for (String name : new String[]{"yeti", "upper_goblin_knight", "blockchain_goblin", "redcap"}) {
            var mob = (LivingEntity) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("twilightforest", name)).create(level);
            mob.getRandom().setSeed(731551L);
            int bones = 0, iron = 0, gold = 0, coal = 0, copper = 0;
            for (int trial = 0; trial < 2048; trial++) {
                var event = new LivingDropsEvent(mob, level.damageSources().generic(), new ArrayList<>(), trial % 4, true);
                CustomMobDrops.addConfiguredDrops(event);
                int b = count(event, Items.BONE), i = count(event, Items.IRON_INGOT);
                bones += b; iron += i; gold += count(event, Items.GOLD_INGOT);
                coal += count(event, Items.COAL); copper += count(event, Items.COPPER_INGOT);
                switch (name) {
                    case "yeti" -> h.assertTrue(b == 2 && count(event, Items.WHITE_WOOL) == 2 && event.getDrops().size() == 2, "Yeti exact loot changed");
                    case "upper_goblin_knight" -> h.assertTrue(b == 1 && i == 2 && event.getDrops().size() == 2, "Knight exact loot changed");
                    case "blockchain_goblin" -> h.assertTrue(b <= 1 && i <= 1 && event.getDrops().size() == b + i, "Goblin odds must yield at most one of each");
                    case "redcap" -> h.assertTrue(i == 1 && event.getDrops().size() == 2
                            && count(event, Items.GOLD_INGOT) + count(event, Items.COPPER_INGOT) + count(event, Items.COAL) / 2 == 1,
                            "Redcap must provide exactly one weighted bonus");
                }
            }
            if (name.equals("blockchain_goblin")) h.assertTrue(Math.abs(bones - 512) < 100 && Math.abs(iron - 1024) < 100, "Goblin drop rates changed");
            if (name.equals("redcap")) h.assertTrue(Math.abs(gold - 512) < 100 && Math.abs(copper - 512) < 100 && Math.abs(coal - 2048) < 200, "Redcap bonus weights changed");
        }
        h.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void restoredStalkerAndKnightHaveOnlyIntendedCatalogItems(GameTestHelper h) {
        Set<String> allowed = Set.of("stalker_meat", "cooked_stalker_meat", "stalker_spawn_egg");
        int kept = 0;
        for (var entry : ForgeRegistries.ITEMS.getEntries()) if (entry.getKey().location().getNamespace().equals("outer_end")) {
            boolean retained = RetainedContent.isKeptItem(new ItemStack(entry.getValue()));
            h.assertTrue(retained == allowed.contains(entry.getKey().location().getPath()), "Unexpected Outer End catalog policy: " + entry.getKey());
            if (retained) kept++;
        }
        h.assertTrue(kept == 3, "Stalker foods and its egg must all be present");
        var resources = h.getLevel().getServer().getResourceManager();
        h.assertTrue(resources.getResource(new ResourceLocation("outer_end:loot_tables/entities/stalker.json")).isPresent(),
                "The retained Stalker must keep its native meat loot");
        h.assertTrue(resources.getResource(new ResourceLocation("outer_end:loot_tables/blocks/azure_vertical_slab.json")).isEmpty()
                        && resources.getResource(new ResourceLocation("outer_end:advancements/end/find_catacombs.json")).isEmpty(),
                "Unused Outer End loot/advancements must not load stale unregistered items");
        var egg = new ItemStack(ModItems.GOBLIN_KNIGHT_SPAWN_EGG.get());
        h.assertTrue(RetainedContent.isKeptItem(egg) && "twilightforest".equals(CreativeCatalog.owner(egg)), "Knight egg must appear in the Twilight Forest catalog");
        h.assertTrue(ForgeRegistries.ENTITY_TYPES.getKey(((net.minecraft.world.item.SpawnEggItem) egg.getItem()).getType(null))
                .toString().equals("twilightforest:upper_goblin_knight"), "Knight egg must spawn the actual upper knight");
        for (var recipe : h.getLevel().getRecipeManager().getRecipes()) {
            var output = ForgeRegistries.ITEMS.getKey(recipe.getResultItem(h.getLevel().registryAccess()).getItem());
            h.assertFalse(RetainedContent.isRemovedOuterEndItem(output), "Unused Outer End recipe survived: " + recipe.getId());
        }
        for (String cooking : new String[]{"smelting", "smoking", "campfire_cooking"}) h.assertTrue(h.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("aoemod:food/cooked_stalker_meat_from_" + cooking)).isPresent(), "Stalker cooking missing: " + cooking);
        h.succeed();
    }

    private static int count(LivingDropsEvent event, Item item) {
        return event.getDrops().stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }
}
