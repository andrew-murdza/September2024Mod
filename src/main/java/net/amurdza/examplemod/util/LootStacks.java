package net.amurdza.examplemod.util;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class LootStacks {
    private LootStacks() { }

    public static void fillPartialStacks(Container container, List<ItemStack> generated) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var target = container.getItem(slot);
            if (target.isEmpty()) continue;
            int limit = Math.min(target.getMaxStackSize(), container.getMaxStackSize());
            for (var source : generated) {
                if (target.getCount() >= limit) break;
                if (source.isEmpty() || !ItemStack.isSameItemSameTags(target, source)) continue;
                int transfer = Math.min(limit - target.getCount(), source.getCount());
                target.grow(transfer);
                source.shrink(transfer);
                changed = true;
            }
        }
        if (changed) container.setChanged();
    }

    /** Keep different tags separate and respect each item's own stack limit. */
    public static void merge(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack target = stacks.get(i);
            if (target.isEmpty()) continue;
            for (int j = i + 1; j < stacks.size() && target.getCount() < target.getMaxStackSize(); j++) {
                ItemStack source = stacks.get(j);
                if (source.isEmpty() || !ItemStack.isSameItemSameTags(target, source)) continue;
                int transfer = Math.min(target.getMaxStackSize() - target.getCount(), source.getCount());
                target.grow(transfer);
                source.shrink(transfer);
            }
        }
        stacks.removeIf(ItemStack::isEmpty);
    }
}
