package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.function.Supplier;

/** A creative menu may inspect rewards without granting inventory access to automation. */
public final class CreativeRewardChestContainer implements Container {
    private record Access(Container container, Player player) { }
    private static final ThreadLocal<Access> ACCESS = new ThreadLocal<>();
    private final Container delegate;
    private final Player owner;
    private boolean closed;

    private CreativeRewardChestContainer(Container delegate, Player owner) {
        this.delegate = delegate;
        this.owner = owner;
    }

    public static Container wrap(Container container, Player player) {
        return player.isCreative() && (container instanceof ChestBlockEntity || container instanceof CompoundContainer)
                ? new CreativeRewardChestContainer(container, player) : container;
    }

    /** Chest lid/open-count code must still see the underlying single or double chest. */
    public static Container unwrap(Container container) {
        return container instanceof CreativeRewardChestContainer wrapper ? wrapper.delegate : container;
    }

    public static boolean mayRead(BlockEntity chest) {
        var access = ACCESS.get();
        return matches(access, chest) && (access.player() == null || access.player().isCreative());
    }

    public static boolean mayRemove(BlockEntity chest) {
        var access = ACCESS.get();
        return matches(access, chest) && access.player() != null && access.player().isCreative();
    }

    private static boolean matches(Access access, BlockEntity chest) {
        return access != null && chest instanceof Container inventory
                && (access.container() == inventory || access.container() instanceof CompoundContainer pair && pair.contains(inventory));
    }

    /** Filling needs real stack/slot reads, but this scope never authorizes extraction. */
    public static void filling(Container container, Runnable fill) {
        Container inventory = container;
        if (container instanceof ChestBlockEntity chest && chest.getLevel() != null
                && chest.getBlockState().getBlock() instanceof ChestBlock block
                && chest.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && chest.getLevel().hasChunkAt(chest.getBlockPos().relative(ChestBlock.getConnectedDirection(chest.getBlockState())))) {
            var pair = ChestBlock.getContainer(block, chest.getBlockState(), chest.getLevel(), chest.getBlockPos(), true);
            if (pair != null) inventory = pair;
        }
        scoped(new Access(inventory, null), () -> { fill.run(); return null; });
    }

    private static <T> T scoped(Access access, Supplier<T> operation) {
        var previous = ACCESS.get();
        ACCESS.set(access);
        try { return operation.get(); }
        finally {
            if (previous == null) ACCESS.remove();
            else ACCESS.set(previous);
        }
    }

    private boolean active() { return !closed && owner.isCreative(); }
    private <T> T read(Supplier<T> operation, T denied) {
        return active() ? scoped(new Access(delegate, owner), operation) : denied;
    }
    private void write(Runnable operation) {
        if (active()) scoped(new Access(delegate, owner), () -> { operation.run(); return null; });
    }

    @Override public int getContainerSize() { return delegate.getContainerSize(); }
    @Override public int getMaxStackSize() { return delegate.getMaxStackSize(); }
    @Override public boolean isEmpty() { return read(delegate::isEmpty, true); }
    @Override public ItemStack getItem(int slot) { return read(() -> delegate.getItem(slot), ItemStack.EMPTY); }
    @Override public ItemStack removeItem(int slot, int count) { return read(() -> delegate.removeItem(slot, count), ItemStack.EMPTY); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return read(() -> delegate.removeItemNoUpdate(slot), ItemStack.EMPTY); }
    @Override public void setItem(int slot, ItemStack stack) { write(() -> delegate.setItem(slot, stack)); }
    @Override public void setChanged() { write(delegate::setChanged); }
    @Override public void clearContent() { write(delegate::clearContent); }
    @Override public boolean stillValid(Player player) { return player == owner && active() && delegate.stillValid(player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return active() && delegate.canPlaceItem(slot, stack); }
    @Override public boolean canTakeItem(Container target, int slot, ItemStack stack) { return active() && delegate.canTakeItem(target, slot, stack); }
    @Override public void startOpen(Player player) { if (player == owner && active()) delegate.startOpen(player); }
    @Override public void stopOpen(Player player) {
        if (player == owner && !closed) {
            closed = true;
            delegate.stopOpen(player);
        }
    }
}
