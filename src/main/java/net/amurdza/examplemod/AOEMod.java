package net.amurdza.examplemod;

import com.mojang.logging.LogUtils;
import net.amurdza.examplemod.config.BlockConfig;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.config.BlockGrowthConfig;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss;
import net.amurdza.examplemod.mixins.accessor.MobsSpawnOnGlowingMoss1;
import net.amurdza.examplemod.registry.ModDensityFunctions;
import net.amurdza.examplemod.registry.ModFeatures;
import net.amurdza.examplemod.registry.ModTreeDecorators;
import net.amurdza.examplemod.registry.ModStructurePlacementTypes;
import net.amurdza.examplemod.registry.ModStructures;
import net.amurdza.examplemod.registry.ModSurfaceRules;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
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


            BlockBehaviour.Properties props =
                    ((MobsSpawnOnGlowingMoss1) Blocks.MOSS_BLOCK).getProperties();

            ((MobsSpawnOnGlowingMoss) props).setIsValidSpawn(
                    (state, level, pos, entityType) ->
                            state.isFaceSturdy(level, pos, Direction.UP)
            );
        });
    }
    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();

// NATURAL BLOCKS (plants, vegetation, ores-as-natural, etc.)
        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {

            event.accept(ModItems.SOUL_BERRIES.get());

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
        }

// FOOD AND DRINKS
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.SOUL_BERRIES.get());

            //Raw Meat
            event.accept(ModItems.RAW_FOX.get());
            event.accept(ModItems.RAW_SQUIRREL.get());
            event.accept(ModItems.RAW_MOOSE.get());
            event.accept(ModItems.RAW_WARPED_TOAD.get());

            // raw seafood
            event.accept(ModItems.RAW_SQUID.get());
            event.accept(ModItems.RAW_GLOW_SQUID.get());
            event.accept(ModItems.RAW_ARROW_SQUID.get());
            event.accept(ModItems.END_FISH.get());
            event.accept(ModItems.CUBOZOA.get());

            //Cooked Meat
            event.accept(ModItems.COOKED_FOX.get());
            event.accept(ModItems.COOKED_SQUIRREL.get());
            event.accept(ModItems.COOKED_MOOSE.get());
            event.accept(ModItems.COOKED_WARPED_TOAD.get());

            // cooked seafood
            event.accept(ModItems.COOKED_PUFFERFISH.get());
            event.accept(ModItems.COOKED_TROPICAL_FISH.get());
            event.accept(ModItems.COOKED_SQUID.get());
            event.accept(ModItems.COOKED_END_FISH.get());
            event.accept(ModItems.COOKED_CUBOZOA.get());
            event.accept(ModItems.COOKED_ARROW_SQUID.get());

            // pies
            event.accept(ModItems.MELON_PIE.get());
            event.accept(ModItems.GLOW_BERRY_PIE.get());
            event.accept(ModItems.PEAR_PIE.get());
            event.accept(ModItems.BANANA_PIE.get());
            event.accept(ModItems.PLUM_PIE.get());
            event.accept(ModItems.CHERRY_PIE.get());

            // other
            event.accept(ModItems.ORANGE_JUICE.get());
            event.accept(ModItems.SOUL_BERRY_COOKIE.get());
            event.accept(ModItems.CHORUS_BREAD.get());
            event.accept(ModItems.NETHER_FUNGUS_STEW.get());

            // fruit
            event.accept(ModItems.PEAR.get());
            event.accept(ModItems.PLUM.get());
            event.accept(ModItems.ORANGE.get());
            event.accept(ModItems.CHERRIES.get());
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
        }

        // BUILDING BLOCKS (farmland fits better here than NATURAL)
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
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

        }
    }
    public static ResourceLocation makeID(String path){
        return new ResourceLocation(AOEMod.MOD_ID,path);
    }
}
