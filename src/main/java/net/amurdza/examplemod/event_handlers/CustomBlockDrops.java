package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomBlockDrops {

    @SubscribeEvent
    public static void addConfiguredDrops(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (!hasConfiguredCustomDrops(block)) {
            return;
        }

        List<BlockConfig.BlockLootEntry> dropEntries = BlockConfig.blockDropEntries(level, pos, state);

        /*
         * Configured block, but no matching biome entries:
         * replace vanilla with no drops.
         */
        event.setCanceled(true);

        if (player.isCreative()) {
            level.destroyBlock(pos, false, player);
            return;
        }

        ItemStack tool = player.getMainHandItem();

        level.levelEvent(player, 2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);

        int xp = state.getExpDrop(level, level.random, pos, fortuneLevel(tool), silkTouchLevel(tool));
        event.setExpToDrop(xp);

        List<WeightedDrop> groupedSeedDrops = new ArrayList<>();

        for (BlockConfig.BlockLootEntry entry : dropEntries) {
            Item item = entry.item();

            if (item == null) {
                continue;
            }

            float multiplier = BlockConfig.blockDropAmount(state, entry);

            if (entry.affectedByFortune()) {
                multiplier = applyFortune(multiplier, tool);
            }

            if (entry.groupedSeedRoll()) {
                if (multiplier > 0.0F) {
                    groupedSeedDrops.add(new WeightedDrop(item, multiplier));
                }
                continue;
            }

            dropItem(level, pos, item, multiplier);
        }

        dropGroupedSeeds(level, pos, groupedSeedDrops);

        if (xp > 0) {
            state.getBlock().popExperience(level, pos, xp);
        }
    }

    private static boolean hasConfiguredCustomDrops(Block block) {
        if (BlockConfig.BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG.isEmpty()) {
            return false;
        }

        for (Map<Item, Map<Block, List<BlockConfig.BlockLootEntry>>> byItem
                : BlockConfig.BLOCK_LOOT_ENTRIES_BY_BLOCK_BY_ITEM_BY_TAG.values()) {
            for (Map<Block, List<BlockConfig.BlockLootEntry>> byBlock : byItem.values()) {
                List<BlockConfig.BlockLootEntry> entries = byBlock.get(block);

                if (entries != null && !entries.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static float applyFortune(float amount, ItemStack tool) {
        int fortune = fortuneLevel(tool);

        if (fortune <= 0) {
            return amount;
        }
        if (fortune == 1) {
            return amount * 4 / 3;
        }
        if (fortune == 2) {
            return amount * 7 / 4;
        }
        return amount * 11 / 5;
    }

    private static int fortuneLevel(ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
    }

    private static int silkTouchLevel(ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool);
    }

    private static void dropItem(ServerLevel level, BlockPos pos, Item item, float multiplier) {
        if (item == null || multiplier <= 0.0F) {
            return;
        }

        int amount = Helper.computeIncrements(level.random, multiplier);

        if (amount <= 0) {
            return;
        }

        dropStack(level, pos, new ItemStack(item, amount));
    }

    private static void dropGroupedSeeds(ServerLevel level, BlockPos pos, List<WeightedDrop> seedDrops) {
        if (seedDrops.isEmpty()) {
            return;
        }

        float totalWeight = 0.0F;

        for (WeightedDrop seedDrop : seedDrops) {
            if (seedDrop.weight() > 0.0F) {
                totalWeight += seedDrop.weight();
            }
        }

        if (totalWeight <= 0.0F) {
            return;
        }

        int amount = Helper.computeIncrements(level.random, totalWeight);

        if (amount <= 0) {
            return;
        }

        Map<Item, Integer> selectedCounts = new LinkedHashMap<>();

        for (int i = 0; i < amount; i++) {
            Item selected = selectWeightedItem(level.random, seedDrops, totalWeight);

            if (selected != null) {
                selectedCounts.merge(selected, 1, Integer::sum);
            }
        }

        for (Map.Entry<Item, Integer> entry : selectedCounts.entrySet()) {
            dropStack(level, pos, new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    private static Item selectWeightedItem(RandomSource random, List<WeightedDrop> seedDrops, float totalWeight) {
        float roll = random.nextFloat() * totalWeight;
        float running = 0.0F;

        for (WeightedDrop seedDrop : seedDrops) {
            if (seedDrop.weight() <= 0.0F) {
                continue;
            }

            running += seedDrop.weight();

            if (roll < running) {
                return seedDrop.item();
            }
        }

        return seedDrops.get(seedDrops.size() - 1).item();
    }

    private static void dropStack(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack
        );

        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    private record WeightedDrop(Item item, float weight) {}
}