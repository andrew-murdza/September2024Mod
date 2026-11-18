package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.block.CharniaBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Preserve saved block IDs while retiring the full source mod. */
public final class RetainedCharnia {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "betterend");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "betterend");
    public static final RegistryObject<Block> LIGHT_BLUE = plant("charnia_light_blue");
    public static final RegistryObject<Block> CYAN = plant("charnia_cyan");

    private RetainedCharnia() { }

    private static RegistryObject<Block> plant(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new CharniaBlock(BlockBehaviour.Properties.copy(Blocks.SEAGRASS)));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
