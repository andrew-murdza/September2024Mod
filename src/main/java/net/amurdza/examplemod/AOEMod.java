package net.amurdza.examplemod;

import com.mojang.logging.LogUtils;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModPortals;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.amurdza.examplemod.registry.ModEnchantments;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.config.BlockGrowthConfig;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss1;
import net.amurdza.examplemod.mixins.accessor.BlockStateBaseAccessor;
import net.amurdza.examplemod.mixins.accessor.FireBlockAccessor;
import net.amurdza.examplemod.registry.ModDensityFunctions;
import net.amurdza.examplemod.registry.ModFeatures;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.registry.ModSurfaceRules;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AOEMod.MOD_ID)
public class AOEMod
{
    public static final String MOD_ID = "aoemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AOEMod()
    {
        net.amurdza.examplemod.registry.ModMobCategories.initialize();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModDensityFunctions.register(modEventBus);
        net.amurdza.examplemod.registry.ModBiomeSources.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        ModCreativeTabs.register(modEventBus);

        //custom items
        ModItems.register(modEventBus);
        ModEnchantments.register(modEventBus);

        //custom blocks
        ModBlocks.register(modEventBus);
        net.amurdza.examplemod.registry.RetainedCharnia.register(modEventBus);
        net.amurdza.examplemod.registry.RetainedDragonFruit.register(modEventBus);
        net.amurdza.examplemod.registry.RetainedSquidFoods.register(modEventBus);
        ModPortals.register(modEventBus);
        ModTreeDecorators.register(modEventBus);


        ModEntities.register(modEventBus);

        ModFeatures.register(modEventBus);
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us

        ModStructures.register(modEventBus);

        ModStructurePlacementTypes.STRUCTURE_PLACEMENT_TYPES.register(modEventBus);

        ModSurfaceRules.register(modEventBus);

        ModToolTiers.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(net.amurdza.examplemod.event_handlers.TropicalBiomeSync::register);
//        ModSurfaceRuleConditions.bootstrap();
        event.enqueueWork(BlockGrowthConfig::init);
        event.enqueueWork(() -> {
            BlockConfig.init();
            MobConfig.init();
            net.amurdza.examplemod.compat.VillagerRealmsLootCompatibility.register();

            FireBlockAccessor fire = (FireBlockAccessor) Blocks.FIRE;
            setNotFlammable(fire, ModBlocks.PALE_MOSS_BLOCK.get());
            setNotFlammable(fire, ModBlocks.PALE_MOSS_CARPET.get());
            setNotFlammable(fire, ModBlocks.PALE_HANGING_MOSS.get());
            setNotFlammable(fire, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", "red_shrub")));
            setNotFlammable(fire, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("nourished_nether", "soul_weeds")));
            ComposterBlock.COMPOSTABLES.put(ModBlocks.AZALEA_SAPLING.get().asItem(), 0.3F);


            BlockBehaviour.Properties props =
                    ((MobsSpawnOnGlowingMoss1) Blocks.MOSS_BLOCK).getProperties();

            ((MobsSpawnOnGlowingMoss) props).setIsValidSpawn(
                    (state, level, pos, entityType) ->
                            state.isFaceSturdy(level, pos, Direction.UP)
            );

            makeLeafLikeForMobSpawning(Blocks.RED_MUSHROOM_BLOCK);
            makeLeafLikeForMobSpawning(Blocks.BROWN_MUSHROOM_BLOCK);
            makeLeafLikeForMobSpawning(Blocks.NETHER_WART_BLOCK);
            makeLeafLikeForMobSpawning(Blocks.WARPED_WART_BLOCK);
            makeLeafLikeForMobSpawning(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("quark", "glow_shroom_block")));
        });
    }

    private static void setFlammable(FireBlockAccessor fire, Block block, int encouragement, int flammability) {
        if (block == null || block == Blocks.AIR) {
            return;
        }

        fire.aoemod$setFlammable(block, encouragement, flammability);
    }

    private static void setNotFlammable(FireBlockAccessor fire, Block block) {
        if (block == null || block == Blocks.AIR) {
            return;
        }

        setFlammable(fire, block, 0, 0);
        block.getStateDefinition().getPossibleStates().forEach(state ->
                ((BlockStateBaseAccessor) state).aoemod$setIgnitedByLava(false)
        );
    }

    private static void makeLeafLikeForMobSpawning(Block block) {
        if (block == null || block == Blocks.AIR) {
            return;
        }

        BlockBehaviour.Properties props = ((MobsSpawnOnGlowingMoss1) block).getProperties();
        ((MobsSpawnOnGlowingMoss) props).setIsValidSpawn(
                (state, level, pos, entityType) ->
                        entityType == EntityType.OCELOT || entityType == EntityType.PARROT
        );
    }
    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();

// NATURAL BLOCKS (plants, vegetation, ores-as-natural, etc.)
        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {

            event.accept(ModItems.SOUL_BEETROOT_SEEDS.get());
            event.accept(ModItems.CARROT_SEEDS.get());
            event.accept(ModItems.BEAN_SEEDS.get());
            event.accept(ModItems.POTATO_SEEDS.get());
            event.accept(ModItems.RICE_SEEDS.get());

            event.accept(ModBlocks.DESERT_GRASS.get());
            event.accept(ModBlocks.DESERT_TALL_GRASS.get());

            event.accept(ModBlocks.BASALT_QUARTZ_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_QUARTZ_ORE.get());
            event.accept(ModBlocks.BASALT_GOLD_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_GOLD_ORE.get());
            event.accept(ModBlocks.SOUL_SOIL_GOLD_ORE.get());
            event.accept(ModBlocks.SOUL_SAND_GOLD_ORE.get());
            event.accept(ModBlocks.SOUL_SOIL_QUARTZ_ORE.get());
            event.accept(ModBlocks.SOUL_SAND_QUARTZ_ORE.get());
            event.accept(ModBlocks.PALE_MOSS_BLOCK.get());
            event.accept(ModBlocks.PALE_MOSS_CARPET.get());
            event.accept(ModBlocks.PALE_HANGING_MOSS.get());
            event.accept(ModBlocks.OPEN_EYEBLOSSOM.get());
            event.accept(ModBlocks.CLOSED_EYEBLOSSOM.get());
            event.accept(ModBlocks.PALE_OAK_LOG.get());
            event.accept(ModBlocks.PALE_OAK_LEAVES.get());
            event.accept(ModBlocks.PALE_OAK_SAPLING.get());
            event.accept(ModBlocks.AZALEA_SAPLING.get());
        }

        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.PALE_OAK_LOG.get());
            event.accept(ModBlocks.STRIPPED_PALE_OAK_LOG.get());
            event.accept(ModBlocks.PALE_OAK_WOOD.get());
            event.accept(ModBlocks.STRIPPED_PALE_OAK_WOOD.get());
            event.accept(ModBlocks.PALE_OAK_PLANKS.get());
            event.accept(ModBlocks.PALE_OAK_STAIRS.get());
            event.accept(ModBlocks.PALE_OAK_SLAB.get());
            event.accept(ModBlocks.PALE_OAK_FENCE.get());
            event.accept(ModBlocks.PALE_OAK_FENCE_GATE.get());
            event.accept(ModBlocks.PALE_OAK_DOOR.get());
            event.accept(ModBlocks.PALE_OAK_TRAPDOOR.get());
            event.accept(ModBlocks.PALE_OAK_PRESSURE_PLATE.get());
            event.accept(ModBlocks.PALE_OAK_BUTTON.get());
        }

// FOOD AND DRINKS
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.SOUL_BEETROOT.get());
            event.accept(ModItems.SOUL_BEETROOT_SOUP.get());

            // pies
            event.accept(ModItems.MELON_PIE.get());
            event.accept(ModItems.GLOWBERRY_PIE.get());
            event.accept(ModItems.ORANGE_PIE.get());
            event.accept(ModItems.PEAR_PIE.get());
            event.accept(ModItems.BANANA_PIE.get());
            event.accept(ModItems.PLUM_PIE.get());
            event.accept(ModItems.CHERRY_PIE.get());

            // other
            event.accept(ModItems.BLUEBERRY_COOKIE.get());
            event.accept(ModItems.GLOWSHROOM_STEW.get());
            event.accept(ModItems.MUSHGLOOM_STEW.get());
            event.accept(ModItems.BULLET_PEPPER_SOUP.get());
            event.accept(ModItems.CARROT_SOUP.get());
            event.accept(ModItems.HONEY_BOWL.get());
        }

        // TOOLS AND UTILITIES
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BONE_AXE.get());
            event.accept(ModItems.BONE_SHOVEL.get());
            event.accept(ModItems.BONE_HOE.get());
            event.accept(ModItems.BONE_PICKAXE.get());
            event.accept(ModItems.BONE_SHEARS.get());
            event.accept(ModItems.BUCKET_END_FISH.get());
            event.accept(ModItems.BUCKET_CUBOZOA.get());
        }
        // COMBAT
        if (tab == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.BONE_AXE.get());
            event.accept(ModItems.BONE_ARROW.get());
            event.accept(ModItems.BONE_SWORD.get());
            event.accept(ModItems.BONE_SPEAR.get());
            event.accept(ModItems.BREEZE_ROD.get());
            event.accept(ModItems.MACE.get());
            event.accept(ModItems.WOODEN_SHIELD.get());
            event.accept(ModItems.COPPER_SHIELD.get());
            event.accept(ModItems.IRON_SHIELD.get());
            event.accept(ModItems.GOLDEN_SHIELD.get());
            event.accept(ModItems.DIAMOND_SHIELD.get());
            event.accept(ModItems.NETHERITE_SHIELD.get());
            event.accept(ModItems.BOSS_SHIELD.get());
        }

        // BUILDING BLOCKS (farmland fits better here than NATURAL)
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModEntities.SPIDER_QUEEN_SPAWN_EGG.get());
            event.accept(ModEntities.ILLAGER_LORD_SPAWN_EGG.get());
            event.accept(ModEntities.BOGGED_SPAWN_EGG.get());
            event.accept(ModEntities.PARCHED_SPAWN_EGG.get());
            event.accept(ModEntities.BREEZE_SPAWN_EGG.get());
            event.accept(ModEntities.CREAKING_SPAWN_EGG.get());
            event.accept(ModEntities.CAMEL_HUSK_SPAWN_EGG.get());
            event.accept(ModEntities.CAMEL_HUSK_JOCKEY_SPAWN_EGG.get());
            event.accept(ModEntities.ZOMBIE_HORSEMAN_SPAWN_EGG.get());
            event.accept(ModEntities.SKELETON_HORSEMAN_SPAWN_EGG.get());
            event.accept(ModEntities.NAUTILUS_SPAWN_EGG.get());
            event.accept(ModEntities.CUBOZOA_SPAWN_EGG.get());
            event.accept(ModEntities.END_FISH_SPAWN_EGG.get());
            event.accept(ModEntities.SEA_SERPENT_SPAWN_EGG.get());
        }
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

//    /** Takes away one level per death ({@link RestoreXpOnRespawn}) */
//    @SubscribeEvent
//    public void loseLevelOnDeath(final LivingDeathEvent event) {
//        if(event.getEntity() instanceof final ServerPlayer player) {
//            player.giveExperienceLevels(-1);
//        }
//    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_MOSS_CARPET.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_HANGING_MOSS.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.OPEN_EYEBLOSSOM.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLOSED_EYEBLOSSOM.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUE_BERRY_BUSH.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_DOOR.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_TRAPDOOR.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_LEAVES.get(), RenderType.cutoutMipped());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_SAPLING.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.AZALEA_SAPLING.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(net.amurdza.examplemod.registry.RetainedDragonFruit.WILD.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(net.amurdza.examplemod.registry.RetainedDragonFruit.BUDDING.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(net.amurdza.examplemod.registry.RetainedDragonFruit.CROP.get(), RenderType.cutout());
                ItemProperties.register(ModItems.BONE_SPEAR.get(), AOEMod.makeID("in_hand"),
                        (stack, level, entity, seed) -> entity == null ? 0.0F : 1.0F);
            });
        }
    }
    public static ResourceLocation makeID(String path){
        return new ResourceLocation(AOEMod.MOD_ID,path);
    }
}
