package net.amurdza.examplemod;

import com.mojang.logging.LogUtils;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.entity.ModEntities;
import net.amurdza.examplemod.event_handlers.BlockGrowthConfig;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.mixins.mob_spawning.MobsSpawnOnGlowingMoss;
import net.amurdza.examplemod.mixins.mob_spawning.MobsSpawnOnGlowingMoss1;
import net.amurdza.examplemod.util.ConfigHelper;
import net.amurdza.examplemod.worldgen.blockstates.ModBlockStateProviderTypes;
import net.amurdza.examplemod.worldgen.density_function.AOEDensityFunctions;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.amurdza.examplemod.worldgen.feature.ModTreeDecorators;
import net.amurdza.examplemod.worldgen.structure.ModStructurePlacementTypes;
import net.amurdza.examplemod.worldgen.structure.ModStructures;
import net.amurdza.examplemod.worldgen.surface_rule.ModSurfaceRules;
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
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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

        AOEDensityFunctions.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        //custom items
        ModItems.register(modEventBus);

        //custom blocks
        ModBlocks.register(modEventBus);
        ModTreeDecorators.register(modEventBus); // <-- add this


        ModEntities.register(modEventBus);

        ModFeatures.register(modEventBus);
        ModBlockStateProviderTypes.register(modEventBus);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHelper.SPEC);

        ModStructures.register(modEventBus);

        ModStructurePlacementTypes.STRUCTURE_PLACEMENT_TYPES.register(modEventBus);

        ModSurfaceRules.register(modEventBus);

        ModToolTiers.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
//        ModSurfaceRuleConditions.bootstrap();
        event.enqueueWork(Config::rebuildBiomeMultiplierCache);
        event.enqueueWork(BlockGrowthConfig::init);
        event.enqueueWork(() -> {

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
            event.accept(ModBlocks.DESERT_GRASS.get());
            event.accept(ModBlocks.DESERT_TALL_GRASS.get());

            event.accept(ModBlocks.BASALT_QUARTZ_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_QUARTZ_ORE.get());
            event.accept(ModBlocks.BASALT_GOLD_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_GOLD_ORE.get());
            event.accept(ModBlocks.SOUL_SOIL_GOLD_ORE.get());
        }

        // FOOD AND DRINKS
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.SOUL_BERRIES.get());

            // fish & seafood (raw)
            event.accept(ModItems.END_FISH.get());
            event.accept(ModItems.CUBOZOA.get());
            event.accept(ModItems.NETHER_CRAB_CLAW.get());

            // cooked seafood
            event.accept(ModItems.COOKED_PUFFERFISH.get());
            event.accept(ModItems.COOKED_END_FISH.get());
            event.accept(ModItems.COOKED_CUBOZOA.get());
            event.accept(ModItems.COOKED_NETHER_CRAB_CLAW.get());
            event.accept(ModItems.COOKED_TUNA.get());

            //Other
            event.accept(ModItems.SOUL_BERRY_COOKIE.get());
            event.accept(ModItems.GLOW_BERRY_JUICE.get());
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
    public void onServerStarting(ServerStartingEvent event)
    {

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
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
    public static ResourceLocation makeID(String path){
        return new ResourceLocation(AOEMod.MOD_ID,path);
    }
}
