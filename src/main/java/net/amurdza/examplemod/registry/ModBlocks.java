package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.*;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS= DeferredRegister.create(ForgeRegistries.BLOCKS, AOEMod.MOD_ID);

    public static final RegistryObject<Block> SOUL_BERRY_BUSH = registerBlock("soul_berry_bush", ()->new SoulBerryBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)));
    public static final RegistryObject<Block> BASALT_QUARTZ_ORE = registerBlockAndItem("basalt_quartz_ore", ()->new DropExperiencePillarBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_QUARTZ_ORE).mapColor(MapColor.COLOR_BLACK), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> BLACKSTONE_QUARTZ_ORE = registerBlockAndItem("blackstone_quartz_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.BASALT).mapColor(MapColor.COLOR_BLACK), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> BASALT_GOLD_ORE = registerBlockAndItem("basalt_gold_ore", ()->new DropExperiencePillarBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BLACK), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> BLACKSTONE_GOLD_ORE = registerBlockAndItem("blackstone_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BLACK), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> SOUL_SOIL_GOLD_ORE = registerBlockAndItem("soul_soil_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BROWN), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> SOUL_SAND_GOLD_ORE = registerBlockAndItem("soul_sand_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BROWN), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> SOUL_SOIL_QUARTZ_ORE = registerBlockAndItem("soul_soil_quartz_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BROWN), UniformInt.of(0, 1)));
    public static final RegistryObject<Block> SOUL_SAND_QUARTZ_ORE = registerBlockAndItem("soul_sand_quartz_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BROWN), UniformInt.of(0, 1)));

    public static final RegistryObject<Block> SUNFLOWER = registerBlock("sunflower", () -> new RotateableSunflower(
            BlockBehaviour.Properties.copy(Blocks.SUNFLOWER)));

    public static final RegistryObject<Block> DESERT_GRASS = registerBlockAndItem("desert_grass", ()->new DesertGrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> DESERT_TALL_GRASS = registerBlockAndItem("desert_tallgrass", ()->new DesertTallGrassBlock(BlockBehaviour.Properties.copy(Blocks.TALL_GRASS).mapColor(MapColor.SAND)));


    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }

    private static <T extends Block> RegistryObject<T> registerBlockAndItem(String name, Supplier<T> block, Function<Item.Properties, Item.Properties> func){
        RegistryObject<T> toReturn=BLOCKS.register(name, block);
        registerBlockItem(name, toReturn,func);
        return toReturn;
    }
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }
    private static <T extends Block> RegistryObject<T> registerBlockAndItem(String name, Supplier<T> block) {
        return registerBlockAndItem(name, block, t->t);
    }
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block, Function<Item.Properties, Item.Properties> func){
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), func.apply(new Item.Properties())));
    }
}
