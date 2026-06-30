package net.amurdza.examplemod;

import com.mojang.logging.LogUtils;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.config.BlockGrowthConfig;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss1;
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
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModDensityFunctions.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        ModCreativeTabs.register(modEventBus);

        //custom items
        ModItems.register(modEventBus);

        //custom blocks
        ModBlocks.register(modEventBus);
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
//        ModSurfaceRuleConditions.bootstrap();
        event.enqueueWork(BlockGrowthConfig::init);
        event.enqueueWork(() -> {
            BlockConfig.init();
            MobConfig.init();

            FireBlockAccessor fire = (FireBlockAccessor) Blocks.FIRE;
            fire.aoemod$setFlammable(ModBlocks.PALE_MOSS_BLOCK.get(), 30, 100);
            fire.aoemod$setFlammable(ModBlocks.PALE_MOSS_CARPET.get(), 60, 100);
            fire.aoemod$setFlammable(ModBlocks.PALE_HANGING_MOSS.get(), 60, 100);


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

            event.accept(ModItems.SOUL_BERRIES.get());
            event.accept(ModItems.ASHEN_WHEAT.get());
            event.accept(ModItems.ASHEN_WHEAT_SEEDS.get());

            event.accept(ModItems.CARROT_SEEDS.get());
            event.accept(ModItems.POTATO_SEEDS.get());
            event.accept(ModItems.ONION_SEEDS.get());
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
        }

        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.COPPER_CHAIN.get());
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
            event.accept(ModItems.SOUL_BERRIES.get());

            // raw seafood
            event.accept(ModItems.END_FISH.get());
            event.accept(ModItems.NETHER_FISH.get());

            // cooked seafood
            event.accept(ModItems.COOKED_PUFFERFISH.get());
            event.accept(ModItems.COOKED_TROPICAL_FISH.get());
            event.accept(ModItems.COOKED_GLOW_SQUID.get());
            event.accept(ModItems.COOKED_END_FISH.get());

            // pies
            event.accept(ModItems.MELON_PIE.get());
            event.accept(ModItems.GLOW_BERRY_PIE.get());
            event.accept(ModItems.MANGO_PIE.get());
            event.accept(ModItems.PEAR_PIE.get());
            event.accept(ModItems.BANANA_PIE.get());
            event.accept(ModItems.PLUM_PIE.get());
            event.accept(ModItems.CHERRY_PIE.get());

            // other
            event.accept(ModItems.SOUL_BERRY_COOKIE.get());
            event.accept(ModItems.BLUE_BERRY_COOKIE.get());
            event.accept(ModItems.SEARING_COD_ROLL.get());
            event.accept(ModItems.NETHER_FISH_KELP_ROLL.get());
            event.accept(ModItems.ASHEN_BREAD.get());
        }

        // TOOLS AND UTILITIES
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BONE_AXE.get());
            event.accept(ModItems.BONE_SHOVEL.get());
            event.accept(ModItems.BONE_HOE.get());
            event.accept(ModItems.BONE_PICKAXE.get());
            event.accept(ModItems.BONE_SHEARS.get());
            event.accept(ModItems.COPPER_AXE.get());
            event.accept(ModItems.COPPER_SHOVEL.get());
            event.accept(ModItems.COPPER_HOE.get());
            event.accept(ModItems.COPPER_PICKAXE.get());
            event.accept(ModItems.BUCKET_END_FISH.get());
            event.accept(ModItems.BUCKET_CUBOZOA.get());
        }
        // COMBAT
        if (tab == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.BONE_AXE.get());
            event.accept(ModItems.BONE_ARROW.get());
            event.accept(ModItems.BONE_SWORD.get());
            event.accept(ModItems.COPPER_SWORD.get());
            event.accept(ModItems.COPPER_AXE.get());
            event.accept(ModItems.COPPER_HELMET.get());
            event.accept(ModItems.COPPER_CHESTPLATE.get());
            event.accept(ModItems.COPPER_LEGGINGS.get());
            event.accept(ModItems.COPPER_BOOTS.get());
            event.accept(ModItems.WOODEN_SPEAR.get());
            event.accept(ModItems.STONE_SPEAR.get());
            event.accept(ModItems.COPPER_SPEAR.get());
            event.accept(ModItems.IRON_SPEAR.get());
            event.accept(ModItems.GOLDEN_SPEAR.get());
            event.accept(ModItems.DIAMOND_SPEAR.get());
            event.accept(ModItems.NETHERITE_SPEAR.get());
        }

        // BUILDING BLOCKS (farmland fits better here than NATURAL)
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModEntities.ARCHLICH_SPAWN_EGG.get());
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
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUE_BERRY_BUSH.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.ASHEN_WHEAT_CROP.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_DOOR.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_TRAPDOOR.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_LEAVES.get(), RenderType.cutoutMipped());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.PALE_OAK_SAPLING.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPPER_CHAIN.get(), RenderType.cutout());
                for (var spear : new net.minecraft.world.item.Item[]{
                        ModItems.WOODEN_SPEAR.get(), ModItems.STONE_SPEAR.get(), ModItems.COPPER_SPEAR.get(),
                        ModItems.IRON_SPEAR.get(), ModItems.GOLDEN_SPEAR.get(), ModItems.DIAMOND_SPEAR.get(),
                        ModItems.NETHERITE_SPEAR.get()}) {
                    ItemProperties.register(spear, AOEMod.makeID("in_hand"),
                            (stack, level, entity, seed) -> entity == null ? 0.0F : 1.0F);
                }
            });
        }
    }
    public static ResourceLocation makeID(String path){
        return new ResourceLocation(AOEMod.MOD_ID,path);
    }
}
