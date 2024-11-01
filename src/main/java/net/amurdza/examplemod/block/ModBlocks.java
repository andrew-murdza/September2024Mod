package net.amurdza.examplemod.block;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.ModItems;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS= DeferredRegister.create(ForgeRegistries.BLOCKS, AOEMod.MOD_ID);
    public static final RegistryObject<Block> GRAPE_VINE=registerBlock("grapes", GrapeVineNew::new,t->t.food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3F).build()));
    public static final RegistryObject<Block> BLUE_BERRY_BUSH=registerBlock("blue_berry_bush", ()->new BlueBerryBush(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> LUSH_FRUIT=registerBlock("lush_fruit_stem", ()->new LushFruitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> CAVE_VINES=registerBlock("cave_vines", ()->new CaveVinesHeadNew(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().lightLevel(CaveVines.emission(15)).instabreak().sound(SoundType.CAVE_VINES).pushReaction(PushReaction.DESTROY).noOcclusion()));
    public static final RegistryObject<Block> CAVE_VINES_PLANT=registerBlock("cave_vines_plant", ()->new CaveVinesPlantNew(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().lightLevel(CaveVines.emission(15)).instabreak().sound(SoundType.CAVE_VINES).pushReaction(PushReaction.DESTROY).noOcclusion()));
    public static final RegistryObject<Block> SEA_PICKLE=registerBlock("sea_pickles",()->new SeaPickleNew(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).lightLevel((p_152680_) -> SeaPickleBlock.isDead(p_152680_) ? 0 : 3 + 3 * p_152680_.getValue(SeaPickleBlock.PICKLES)).sound(SoundType.SLIME_BLOCK).noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, Function<Item.Properties, Item.Properties> func){
        RegistryObject<T> toReturn=BLOCKS.register(name, block);
        registerBlockItem(name, toReturn,func);
        return toReturn;
    }
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        return registerBlock(name, block, t->t);
    }
    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block, Function<Item.Properties, Item.Properties> func){
        return ModItems.ITEMS.register(name, ()-> new BlockItem(block.get(), func.apply(new Item.Properties())));
    }
}
