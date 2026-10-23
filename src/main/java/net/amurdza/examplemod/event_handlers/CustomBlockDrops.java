package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomBlockDrops {
    private static final ResourceLocation SOUL_WEEDS = new ResourceLocation("nourished_nether", "soul_weeds");

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
        if (dropEntries.isEmpty()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();

        if (shouldLetVanillaShearsHandle(state, tool)) {
            return;
        }

        event.setCanceled(true);

        if (player.isCreative()) {
            level.destroyBlock(pos, false, player);
            return;
        }

        level.levelEvent(player, 2001, pos, Block.getId(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);

        int xp = state.getExpDrop(level, level.random, pos, fortuneLevel(tool), silkTouchLevel(tool));
        event.setExpToDrop(xp);

        for (ItemStack drop : configuredDrops(level, pos, state, tool, level.random)) dropStack(level, pos, drop);

        if (xp > 0) {
            state.getBlock().popExperience(level, pos, xp);
        }
    }

    private static boolean hasConfiguredCustomDrops(Block block) {
        return BlockConfig.BLOCK_INFO_BY_BLOCK.containsKey(block);
    }

    /** A reusable base loot roll for workers; null delegates to the original table, empty is a valid zero roll. */
    public static List<ItemStack> configuredDrops(ServerLevel level, BlockPos pos, BlockState state,
                                                 ItemStack tool, RandomSource random) {
        List<BlockConfig.BlockLootEntry> entries = BlockConfig.blockDropEntries(level, pos, state);
        if (entries.isEmpty() || shouldLetVanillaShearsHandle(state, tool)) return null;
        List<ItemStack> drops = new ArrayList<>();
        if (isShears(tool) && (state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(GlimmeringWealdModule.glow_shroom_block) || state.is(GlimmeringWealdModule.glow_shroom_stem))) {
            drops.add(new ItemStack(state.getBlock()));
        }
        List<WeightedDrop> seeds = new ArrayList<>();
        for (BlockConfig.BlockLootEntry entry : entries) {
            if (entry.item() == null) continue;
            float amount = BlockConfig.blockDropAmount(state, entry);
            if (entry.affectedByFortune()) amount = applyFortune(amount, tool);
            if (entry.groupedSeedRoll()) {
                if (amount > 0) seeds.add(new WeightedDrop(entry.item(), amount));
            } else if (amount > 0) {
                int count = Helper.computeIncrements(random, amount);
                if (count > 0) drops.add(new ItemStack(entry.item(), count));
            }
        }
        float weight = 0;
        for (WeightedDrop seed : seeds) weight += seed.weight();
        int rolls = weight > 0 ? Helper.computeIncrements(random, weight) : 0;
        Map<Item, Integer> selected = new LinkedHashMap<>();
        for (int roll = 0; roll < rolls; roll++) {
            Item item = selectWeightedItem(random, seeds, weight);
            if (item != null) selected.merge(item, 1, Integer::sum);
        }
        selected.forEach((item, count) -> drops.add(new ItemStack(item, count)));
        return drops;
    }

    public static boolean usesConfiguredDrops(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        return !BlockConfig.blockDropEntries(level, pos, state).isEmpty() && !shouldLetVanillaShearsHandle(state, tool);
    }

    private static boolean shouldLetVanillaShearsHandle(BlockState state, ItemStack tool) {
        return isShears(tool)
                && (state.is(Blocks.FERN)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN)
                || state.is(ModBlocks.DESERT_GRASS.get())
                || state.is(ModBlocks.DESERT_TALL_GRASS.get())
                || SOUL_WEEDS.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock())));
    }

    private static boolean isShears(ItemStack tool) {
        return tool.is(Tags.Items.SHEARS) || tool.getItem() instanceof ShearsItem;
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
