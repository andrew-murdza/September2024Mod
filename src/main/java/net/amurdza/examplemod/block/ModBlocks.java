package net.amurdza.examplemod.block;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.mcreator.nourishednether.init.NourishedNetherModBlocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vectorwing.farmersdelight.common.block.WildCropBlock;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS= DeferredRegister.create(ForgeRegistries.BLOCKS, AOEMod.MOD_ID);
    public static final RegistryObject<Block> GRAPE_VINE=registerBlock("grape_vine", GrapeVineNew::new);
    public static final RegistryObject<Block> JUNGLE_SWEET_BERRY_BUSH=registerBlock("jungle_sweet_berry_bush", ()->new JungleSweetBerries(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)));
    public static final RegistryObject<Block> JUNGLE_GLOW_LICHEN=registerBlockAndItem("jungle_glow_lichen", ()->new GlowLichenBlock(BlockBehaviour.Properties.copy(Blocks.GLOW_LICHEN).lightLevel(GlowLichenBlock.emission(15))));
    public static final RegistryObject<Block> ASHEN_WHEAT = registerBlock("ashen_wheat", ()->new NetherCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
    public static final RegistryObject<Block> WARPED_CARROTS = registerBlock("warped_carrots", ()->new NetherCarrotsBlock(BlockBehaviour.Properties.copy(Blocks.CARROTS)));
    public static final RegistryObject<Block> CRIMSON_POTATOES = registerBlock("crimson_potatoes", ()->new NetherPotatoBlock(BlockBehaviour.Properties.copy(Blocks.POTATOES)));
    public static final RegistryObject<Block> SOUL_BEETS = registerBlock("soul_beets", ()->new NetherBeetRootBlock(BlockBehaviour.Properties.copy(Blocks.BEETROOTS)));
    public static final RegistryObject<Block> SOUL_BERRY_BUSH = registerBlock("soul_berry_bush", ()->new SoulBerryBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)));
    public static final RegistryObject<Block> NETHER_CANE = registerBlockAndItem("nether_cane", ()->new NetherSugarCane(BlockBehaviour.Properties.copy(Blocks.SUGAR_CANE)));
    public static final RegistryObject<Block> BASALT_QUARTZ_ORE = registerBlockAndItem("basalt_quartz_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_QUARTZ_ORE).mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> BLACKSTONE_QUARTZ_ORE = registerBlockAndItem("blackstone_quartz_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_QUARTZ_ORE).mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> BASALT_GOLD_ORE = registerBlockAndItem("basalt_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> BLACKSTONE_GOLD_ORE = registerBlockAndItem("blackstone_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> SOUL_SOIL_GOLD_ORE = registerBlockAndItem("soul_soil_gold_ore", ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE).mapColor(MapColor.COLOR_BROWN)));
    public static final RegistryObject<Block> DESERT_GRASS = registerBlockAndItem("desert_grass", ()->new DesertGrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> DESERT_TALL_GRASS = registerBlockAndItem("desert_tallgrass", ()->new DesertTallGrassBlock(BlockBehaviour.Properties.copy(Blocks.TALL_GRASS).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> NETHER_FARMLAND = registerBlockAndItem("nether_farmland", ()->new NetherFarmlandBlock(BlockBehaviour.Properties.copy(Blocks.FARMLAND)));
    public static final RegistryObject<Block> WILD_CRYO_ROOTS = registerBlockAndItem("wild_cryo_roots", ()->new WildCropBlock(MobEffects.SLOW_FALLING,6,BlockBehaviour.Properties.copy(Blocks.TALL_GRASS)));
    public static final RegistryObject<Block> WILD_WARPED_CARROTS = registerBlockAndItem("wild_warped_carrots", ()->new WildWarpedCarrots(MobEffects.FIRE_RESISTANCE,6,BlockBehaviour.Properties.copy(Blocks.WARPED_ROOTS)));
    public static final RegistryObject<Block> WILD_CRIMSON_POTATOES = registerBlockAndItem("wild_crimson_potatoes", ()->new WildCrimsonPotatoes(MobEffects.FIRE_RESISTANCE,6,BlockBehaviour.Properties.copy(Blocks.CRIMSON_ROOTS)));
    public static final RegistryObject<Block> WILD_SOUL_BEETS = registerBlockAndItem("wild_soul_beets", ()->new WildSoulBeets(MobEffects.FIRE_RESISTANCE,6,BlockBehaviour.Properties.copy(NourishedNetherModBlocks.SOUL_WEEDS.get())));
    public static final RegistryObject<Block> WILD_BEANS = registerBlockAndItem("wild_beans",
            () -> new WildCropBlock(MobEffects.SATURATION, 6, BlockBehaviour.Properties.copy(Blocks.TALL_GRASS)));



    public static final RegistryObject<Block> CAVE_VINES=registerBlock("cave_vines", ()->new CaveVinesHeadNew(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES).lightLevel(CaveVines.emission(15))));
    public static final RegistryObject<Block> CAVE_VINES_PLANT=registerBlock("cave_vines_plant", ()->new CaveVinesPlantNew(BlockBehaviour.Properties.copy(Blocks.CAVE_VINES_PLANT).lightLevel(CaveVines.emission(15)).randomTicks()));
    public static final RegistryObject<Block> SEA_PICKLE=registerBlockAndItem("sea_pickle",()->new SeaPickleNew(BlockBehaviour.Properties.copy(Blocks.SEA_PICKLE).lightLevel((p_152680_) -> SeaPickleBlock.isDead(p_152680_) ? 0 : 3 + 3 * p_152680_.getValue(SeaPickleBlock.PICKLES))));
    public static final RegistryObject<Block> LUSHROOM = registerBlockAndItem("lush_mushroom",()->new MushroomBlock(BlockBehaviour.Properties.copy(Blocks.RED_MUSHROOM).mapColor(MapColor.COLOR_GREEN), ModFeatures.HUGE_LUSHROOM));
    public static final RegistryObject<Block> LUSHROOM_BLOCK = registerBlockAndItem("lush_mushroom_block", () -> new HugeMushroomBlock(BlockBehaviour.Properties.copy(Blocks.RED_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_GREEN)));
    public static final RegistryObject<Block> CHERRY_VINE = registerBlockAndItem("cherry_vine", () -> new VineBlock(BlockBehaviour.Properties.copy(Blocks.VINE).mapColor(MapColor.COLOR_PINK)));
    public static final RegistryObject<Block> GLOW_SHROOM= registerBlockAndItem("glow_shroom",()->new GlowShroomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).lightLevel(p->10).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY), ModFeatures.HUGE_GLOW_SHROOM));
    public static final RegistryObject<Block> SUNFLOWER = registerBlockAndItem("sunflower", () -> new Sunflower(BlockBehaviour.Properties.copy(Blocks.SUNFLOWER)));

//    public static final RegistryObject<Block> LAVENDER=registerFlower("lavender",MobEffects.NIGHT_VISION,7);
//    public static final RegistryObject<Block> WILDFLOWER=registerFlower("wildflower",MobEffects.DAMAGE_RESISTANCE,7);

//    public static RegistryObject<Block> registerFlower(String name, MobEffect effect, int duration){
//        return registerBlock(name, ()->new FlowerBlock(effect, duration, BlockBehaviour.Properties.of()
//                .mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
//    }

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
