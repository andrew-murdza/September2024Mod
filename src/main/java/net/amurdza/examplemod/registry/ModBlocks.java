package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.*;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
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
    public static final RegistryObject<Block> BLUE_BERRY_BUSH = registerBlock("blue_berry_bush", () -> new BlueBerryBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)));
    public static final RegistryObject<Block> JUNGLE_SWEET_BERRY_BUSH = registerBlock("jungle_sweet_berry_bush", () -> new SweetBerryBushBlock(BlockBehaviour.Properties.copy(Blocks.SWEET_BERRY_BUSH)));
    public static final RegistryObject<Block> ASHEN_WHEAT_CROP = registerBlock("ashen_wheat", () -> new AshenWheatCropBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));
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

    public static final RegistryObject<Block> PALE_MOSS_BLOCK = registerBlockAndItem("pale_moss_block",
            () -> new PaleMossBlock(BlockBehaviour.Properties.copy(Blocks.MOSS_BLOCK)));
    public static final RegistryObject<Block> PALE_MOSS_CARPET = registerBlockAndItem("pale_moss_carpet",
            () -> new CarpetBlock(BlockBehaviour.Properties.copy(Blocks.MOSS_CARPET)));
    public static final RegistryObject<Block> PALE_HANGING_MOSS = registerBlockAndItem("pale_hanging_moss",
            () -> new PaleHangingMossBlock(BlockBehaviour.Properties.copy(Blocks.VINE).sound(SoundType.MOSS_CARPET)));
    public static final RegistryObject<Block> OPEN_EYEBLOSSOM = registerBlockAndItem("open_eyeblossom",
            () -> new EyeblossomBlock(true, BlockBehaviour.Properties.copy(Blocks.POPPY).randomTicks().lightLevel(state -> 3)));
    public static final RegistryObject<Block> CLOSED_EYEBLOSSOM = registerBlockAndItem("closed_eyeblossom",
            () -> new EyeblossomBlock(false, BlockBehaviour.Properties.copy(Blocks.POPPY).randomTicks()));

    public static final RegistryObject<RotatedPillarBlock> STRIPPED_PALE_OAK_LOG = registerBlockAndItem("stripped_pale_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_WARPED_STEM)));
    public static final RegistryObject<RotatedPillarBlock> PALE_OAK_LOG = registerBlockAndItem("pale_oak_log",
            () -> new StrippableRotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_STEM), STRIPPED_PALE_OAK_LOG));
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_PALE_OAK_WOOD = registerBlockAndItem("stripped_pale_oak_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_WARPED_HYPHAE)));
    public static final RegistryObject<RotatedPillarBlock> PALE_OAK_WOOD = registerBlockAndItem("pale_oak_wood",
            () -> new StrippableRotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_HYPHAE), STRIPPED_PALE_OAK_WOOD));
    public static final RegistryObject<Block> PALE_OAK_PLANKS = registerBlockAndItem("pale_oak_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.WARPED_PLANKS)));
    public static final RegistryObject<StairBlock> PALE_OAK_STAIRS = registerBlockAndItem("pale_oak_stairs",
            () -> new StairBlock(PALE_OAK_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.WARPED_STAIRS)));
    public static final RegistryObject<SlabBlock> PALE_OAK_SLAB = registerBlockAndItem("pale_oak_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_SLAB)));
    public static final RegistryObject<FenceBlock> PALE_OAK_FENCE = registerBlockAndItem("pale_oak_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_FENCE)));
    public static final RegistryObject<FenceGateBlock> PALE_OAK_FENCE_GATE = registerBlockAndItem("pale_oak_fence_gate",
            () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_FENCE_GATE), WoodType.OAK));
    public static final RegistryObject<DoorBlock> PALE_OAK_DOOR = registerBlockAndItem("pale_oak_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_DOOR), BlockSetType.OAK));
    public static final RegistryObject<TrapDoorBlock> PALE_OAK_TRAPDOOR = registerBlockAndItem("pale_oak_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_TRAPDOOR), BlockSetType.OAK));
    public static final RegistryObject<PressurePlateBlock> PALE_OAK_PRESSURE_PLATE = registerBlockAndItem("pale_oak_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(Blocks.WARPED_PRESSURE_PLATE), BlockSetType.OAK));
    public static final RegistryObject<ButtonBlock> PALE_OAK_BUTTON = registerBlockAndItem("pale_oak_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.WARPED_BUTTON), BlockSetType.OAK, 30, true));
    public static final RegistryObject<LeavesBlock> PALE_OAK_LEAVES = registerBlockAndItem("pale_oak_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final RegistryObject<PaleOakSaplingBlock> PALE_OAK_SAPLING = registerBlockAndItem("pale_oak_sapling",
            () -> new PaleOakSaplingBlock(BlockBehaviour.Properties.copy(Blocks.CRIMSON_FUNGUS).randomTicks()));

    public static final RegistryObject<ChainBlock> COPPER_CHAIN = registerBlockAndItem("copper_chain",
            () -> new ChainBlock(BlockBehaviour.Properties.copy(Blocks.CHAIN).mapColor(MapColor.COLOR_ORANGE)));


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
