package net.amurdza.examplemod.compat;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/** Optional worker-loot API. AoE can load independently of VillagerRealms. */
public final class VillagerRealmsLootCompatibility {
    private static boolean registered;

    private VillagerRealmsLootCompatibility() { }

    @SuppressWarnings("unchecked")
    public static void register() {
        if (registered || !ModList.get().isLoaded("villager_realms")) return;
        try {
            Class<?> raw = Class.forName("com.villagerrealms.loot.WorkerBlockLootEvent");
            Class<Event> eventType = (Class<Event>) raw.asSubclass(Event.class);
            Method level = raw.getMethod("getLevel");
            Method position = raw.getMethod("getPosition");
            Method state = raw.getMethod("getState");
            Method tool = raw.getMethod("getTool");
            Method setBaseRoll = raw.getMethod("setBaseRoll", Supplier.class);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, eventType, event -> {
                try {
                    ServerLevel world = (ServerLevel) level.invoke(event);
                    BlockPos pos = (BlockPos) position.invoke(event);
                    BlockState block = (BlockState) state.invoke(event);
                    ItemStack held = ((ItemStack) tool.invoke(event)).copy();
                    if (!CustomBlockDrops.usesConfiguredDrops(world, pos, block, held)) return;
                    Supplier<List<ItemStack>> rolls = () -> CustomBlockDrops.configuredDrops(world, pos, block, held, world.random);
                    setBaseRoll.invoke(event, rolls);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException("VillagerRealms worker loot API failed", failure);
                }
            });
            registered = true;
            AOEMod.LOGGER.info("AoE regional harvest yields connected to VillagerRealms worker loot");
        } catch (ClassNotFoundException | NoSuchMethodException failure) {
            AOEMod.LOGGER.warn("VillagerRealms lacks the worker loot API; install the paired updated build for regional worker yields", failure);
        }
    }
}
