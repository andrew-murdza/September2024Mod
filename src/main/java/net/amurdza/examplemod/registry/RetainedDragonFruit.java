package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.block.DragonFruitCropBlock;
import net.amurdza.examplemod.block.BuddingDragonFruitBlock;
import net.amurdza.examplemod.block.WildDragonFruitBlock;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The original dragon-fruit IDs and farming chain, without the retired dependency's other content. */
public final class RetainedDragonFruit {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "collectorsreap");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "collectorsreap");
    public static final RegistryObject<Block> WILD = BLOCKS.register("dragon_bush",
            () -> new WildDragonFruitBlock(BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH)));
    public static final RegistryObject<BuddingDragonFruitBlock> BUDDING = BLOCKS.register("budding_pink_dragon_fruits",
            () -> new BuddingDragonFruitBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
    public static final RegistryObject<DragonFruitCropBlock> CROP = BLOCKS.register("pink_dragon_fruits",
            () -> new DragonFruitCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
    public static final RegistryObject<Item> FRUIT = ITEMS.register("pink_dragon_fruit",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(.2F).build())));
    public static final RegistryObject<Item> SEEDS = ITEMS.register("dragon_fruit_seeds",
            () -> new ItemNameBlockItem(BUDDING.get(), new Item.Properties()));
    public static final RegistryObject<Item> WILD_ITEM = ITEMS.register("dragon_bush",
            () -> new BlockItem(WILD.get(), new Item.Properties()));

    private RetainedDragonFruit() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
