package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.mixins.accessor.ChestLootTableAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.amurdza.examplemod.util.LootStacks;
import net.amurdza.examplemod.worldgen.structure.OrcCampStock;
import net.amurdza.examplemod.worldgen.structure.OrcVillageLoot;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.Tags;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.List;

@Mixin(LootTable.class)
public abstract class CompactChestLoot {
    @Shadow private void shuffleAndSplitItems(ObjectArrayList<ItemStack> stacks, int available, RandomSource random) {
        throw new AssertionError();
    }

    @Shadow private List<Integer> getAvailableSlots(Container container, RandomSource random) { throw new AssertionError(); }

    @Redirect(method = "fill", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getAvailableSlots(Lnet/minecraft/world/Container;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<Integer> aoemod$orderChestSlots(LootTable table, Container container, RandomSource random,
                                                Container filled, LootParams params, long seed) {
        // The top half can overflow into this half. Roll it before taking the free-slot snapshot.
        if (container instanceof ChestBlockEntity chest && chest.getBlockState().getValue(ChestBlock.TYPE) == ChestType.LEFT)
            aoemod$openPartner(chest, params);
        List<Integer> slots = getAvailableSlots(container, random);
        if (aoemod$isChest(container)) slots.sort(Comparator.reverseOrder());
        return slots;
    }

    @Unique
    private static boolean aoemod$isChest(Container container) {
        return container instanceof ChestBlockEntity || container instanceof MinecartChest || container instanceof ChestBoat
                || container instanceof CompoundContainer || container instanceof BlockEntity block && block.getBlockState().is(Tags.Blocks.CHESTS);
    }

    @Redirect(method = "fill", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;shuffleAndSplitItems(Lit/unimi/dsi/fastutil/objects/ObjectArrayList;ILnet/minecraft/util/RandomSource;)V"))
    private void aoemod$groupChestStacks(LootTable table, ObjectArrayList<ItemStack> items, int available,
                                        RandomSource random, Container container, LootParams params, long seed) {
        if (!aoemod$isChest(container)) {
            shuffleAndSplitItems(items, available, random);
            return;
        }
        if (SavannaOrcCampProcessor.LOOT_TABLE.equals(table.getLootTableId()) && container instanceof ChestBlockEntity chest) {
            var data = chest.getPersistentData();
            if (data.contains(OrcVillageLoot.INDEX)) {
                items.clear();
                items.addAll(OrcVillageLoot.share(table, params, data));
            } else if (data.contains(OrcCampStock.STOCK_TAG)) OrcCampStock.addProvisions(data.getInt(OrcCampStock.STOCK_TAG), random, items);
            else OrcCampStock.addLegacyProvisions(random, items);
        }
        LootStacks.fillPartialStacks(container, items);
        // A double chest generates each half with its own seed and player luck.
        // Roll the top half first even when a hopper accesses the bottom half directly.
        ChestBlockEntity partner = container instanceof ChestBlockEntity chest ? aoemod$openPartner(chest, params) : null;
        if (partner != null) LootStacks.fillPartialStacks(partner, items);
        LootStacks.merge(items);
        int reserve = partner != null && partner.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT ? 0 : available;
        if (partner != null && items.size() > reserve) {
            for (int slot = 0; slot < partner.getContainerSize() && items.size() > reserve; slot++) {
                if (partner.getItem(slot).isEmpty()) partner.setItem(slot, items.remove(items.size() - 1));
            }
        }
        net.minecraft.Util.shuffle(items, random);
    }

    @Unique
    private static ChestBlockEntity aoemod$openPartner(ChestBlockEntity chest, LootParams params) {
        var level = chest.getLevel();
        var state = chest.getBlockState();
        if (level == null || !(state.getBlock() instanceof ChestBlock block) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
        var pos = chest.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ChestBlockEntity partner)) return null;
        if (((ChestLootTableAccess) partner).aoemod$pendingLootTable() != null) {
            if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) return null;
            var opener = params.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
            partner.unpackLootTable(opener instanceof net.minecraft.world.entity.player.Player player ? player : null);
        }
        return ChestBlock.getContainer(block, state, level, chest.getBlockPos(), true) instanceof CompoundContainer pair
                && pair.contains(partner) ? partner : null;
    }
}
