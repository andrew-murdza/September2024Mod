package net.amurdza.examplemod;

import com.mojang.logging.LogUtils;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.entity.ModEntities;
import net.amurdza.examplemod.event_handlers.BlockGrowthConfig;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.item.ModToolTiers;
import net.amurdza.examplemod.util.ConfigHelper;
import net.amurdza.examplemod.worldgen.AOEDensityFunctions;
import net.amurdza.examplemod.worldgen.feature.ModFeatures;
import net.amurdza.examplemod.worldgen.feature.ModTreeDecorators;
import net.amurdza.examplemod.worldgen.structure.ModStructures;
import net.amurdza.examplemod.worldgen.surface_rule.ModSurfaceRules;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import terrablender.api.SurfaceRuleManager;

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
        modEventBus.addListener(ModEntities::onSpawnPlacements);

        //custom items
        ModItems.register(modEventBus);

        //custom blocks
        ModBlocks.register(modEventBus);
        ModTreeDecorators.register(modEventBus); // <-- add this


        ModEntities.register(modEventBus);

        ModFeatures.register(modEventBus);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHelper.SPEC);

        ModStructures.register(modEventBus);

        ModSurfaceRules.register(modEventBus);

        ModToolTiers.register();
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.rebuildBiomeMultiplierCache();
        }
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.rebuildBiomeMultiplierCache();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
//        ModSurfaceRuleConditions.bootstrap();
        event.enqueueWork(Config::rebuildBiomeMultiplierCache);
        event.enqueueWork(BlockGrowthConfig::init);
        event.enqueueWork(() -> {
            SurfaceRuleManager.setDefaultSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, ModSurfaceRules.makeRules());
            ComposterBlock.COMPOSTABLES.put(ModBlocks.SUNFLOWER.get().asItem(), 0.65F);
//            SurfaceRuleManager.addToDefaultSurfaceRulesAtStage(SurfaceRuleManager.RuleCategory.OVERWORLD,
//                    SurfaceRuleManager.RuleStage.AFTER_BEDROCK,100000,ModSurfaceRules.makeRules());
            //.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, MOD_ID, ModSurfaceRules.makeRules());
        });
    }
    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();

        // NATURAL BLOCKS (plants, vegetation, ores-as-natural, etc.)
        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            // existing
            event.accept(ModItems.GRAPES.get());
            event.accept(ModItems.GLOW_BERRIES.get());

            event.accept(ModBlocks.SEA_PICKLE.get());
            event.accept(ModBlocks.LUSHROOM.get());
            event.accept(ModBlocks.LUSHROOM_BLOCK.get());
            event.accept(ModBlocks.CHERRY_VINE.get());
            event.accept(ModItems.ASHEN_WHEAT_SEEDS.get());
            event.accept(ModItems.SOUL_BERRIES.get());
            event.accept(ModItems.SOUL_BEET_SEEDS.get());
            event.accept(ModItems.BEAN_SEEDS.get());

            // remaining registerBlockAndItem blocks (from your list)
            event.accept(ModBlocks.JUNGLE_GLOW_LICHEN.get());
            event.accept(ModBlocks.NETHER_CANE.get());

            event.accept(ModBlocks.DESERT_GRASS.get());
            event.accept(ModBlocks.DESERT_TALL_GRASS.get());

            event.accept(ModBlocks.WILD_CRYO_ROOTS.get());
            event.accept(ModBlocks.WILD_WARPED_CARROTS.get());
            event.accept(ModBlocks.WILD_CRIMSON_POTATOES.get());
            event.accept(ModBlocks.WILD_SOUL_BEETS.get());
            event.accept(ModBlocks.WILD_BEANS.get());

            event.accept(ModBlocks.GLOW_SHROOM.get());
            event.accept(ModBlocks.SUNFLOWER.get());

            // ores: vanilla puts ores under NATURAL_BLOCKS
            event.accept(ModBlocks.BASALT_QUARTZ_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_QUARTZ_ORE.get());
            event.accept(ModBlocks.BASALT_GOLD_ORE.get());
            event.accept(ModBlocks.BLACKSTONE_GOLD_ORE.get());
            event.accept(ModBlocks.SOUL_SOIL_GOLD_ORE.get());
        }

        // FOOD AND DRINKS
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            // existing
            event.accept(ModItems.GLOW_BERRIES.get());
            event.accept(ModItems.GRAPES.get());
            event.accept(ModItems.WARPED_CARROT.get());
            event.accept(ModItems.CRIMSON_POTATO.get());
            event.accept(ModItems.JUNGLE_SWEET_BERRIES.get());
            event.accept(ModItems.BAKED_CRIMSON_POTATO.get());
            event.accept(ModItems.SOUL_BERRIES.get());
            event.accept(ModItems.SOUL_BEET.get());
            event.accept(ModItems.SOUL_BEET_SOUP.get());
            event.accept(ModItems.CRYO_ROOT_SOUP.get());

            // fish & seafood (raw)
            event.accept(ModItems.END_FISH.get());
            event.accept(ModItems.CUBOZOA.get());
            event.accept(ModItems.CUTTLEFISH.get());
            event.accept(ModItems.FIREWORK_JELLYFISH.get());
            event.accept(ModItems.WIZARD_JELLYFISH.get());
            event.accept(ModItems.ISOPOD.get());
            event.accept(ModItems.SHRIMP.get());
            event.accept(ModItems.SQUAT_LOBSTER.get());
            event.accept(ModItems.VAMPIRE_SQUID.get());
            event.accept(ModItems.CRAB_LEGS.get());
            event.accept(ModItems.NETHER_CRAB_CLAW.get());
            event.accept(ModItems.ARROW_SQUID.get());
            event.accept(ModItems.WARPED_ARROW_SQUID.get());
            event.accept(ModItems.FRILLED_SHARK.get());
            event.accept(ModItems.HOUND_SHARK.get());
            event.accept(ModItems.TRIOPS.get());
            event.accept(ModItems.DEVILS_HOLE_PUPFUSH.get());

            // cooked seafood
            event.accept(ModItems.COOKED_END_FISH.get());
            event.accept(ModItems.COOKED_CUBOZOA.get());
            event.accept(ModItems.COOKED_CUTTLEFISH.get());
            event.accept(ModItems.COOKED_FIREWORK_JELLYFISH.get());
            event.accept(ModItems.COOKED_WIZARD_JELLYFISH.get());
            event.accept(ModItems.COOKED_ISOPOD.get());
            event.accept(ModItems.COOKED_SHRIMP.get());
            event.accept(ModItems.COOKED_SQUAT_LOBSTER.get());
            event.accept(ModItems.COOKED_VAMPIRE_SQUID.get());
            event.accept(ModItems.COOKED_CRAB_LEGS.get());
            event.accept(ModItems.COOKED_NETHER_CRAB_CLAW.get());
            event.accept(ModItems.COOKED_ARROW_SQUID.get());
            event.accept(ModItems.COOKED_FRILLED_SHARK.get());
            event.accept(ModItems.COOKED_HOUND_SHARK.get());
            event.accept(ModItems.COOKED_TRIOPS.get());
            event.accept(ModItems.COOKED_DEVILS_HOLE_PUPFUSH.get());
            event.accept(ModItems.COOKED_KOI.get());
            event.accept(ModItems.COOKED_LOST_TENTACLE.get());
            event.accept(ModItems.COOKED_DRAGONFISH.get());
            event.accept(ModItems.COOKED_TUNA.get());

            // other food
            event.accept(ModItems.BEANS.get());
            event.accept(ModItems.BEAN_SOUP.get());
            event.accept(ModItems.ASHEN_BREAD.get());
        }

        // INGREDIENTS
        if (tab == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ASHEN_WHEAT.get());
//            event.accept(ModItems.ASHEN_WHEAT_SEEDS.get());
//            event.accept(ModItems.SOUL_BEET_SEEDS.get());
//            event.accept(ModItems.BEAN_SEEDS.get());
        }


        // TOOLS AND UTILITIES
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BONE_AXE.get());
            event.accept(ModItems.BONE_SHOVEL.get());
            event.accept(ModItems.BONE_HOE.get());
            event.accept(ModItems.BONE_PICKAXE.get());
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
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.NETHER_FARMLAND.get());
        }
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModEntities.CUBOZOA_SPAWN_EGG.get());
            event.accept(ModEntities.END_FISH_SPAWN_EGG.get());
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
        //Add once in 1.21
//        @SubscribeEvent
//        private static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
//            event.register(EXAMPLE_DATA);
//        }
    }
    public static ResourceLocation makeID(String path){
        return new ResourceLocation(AOEMod.MOD_ID,path);
    }
}
