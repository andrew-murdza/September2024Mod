package net.amurdza.examplemod.event_handlers;

import com.eightsidedsquare.angling.core.AnglingEntities;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.accessor.EntityTypeCategoryAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import teamdraco.finsandstails.registry.FTEntities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobCategoryChanges {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            setCategory(EntityType.SQUID, MobCategory.WATER_CREATURE);
            setCategory(EntityType.AXOLOTL, MobCategory.WATER_CREATURE);
            setCategory(EntityType.TURTLE, MobCategory.WATER_CREATURE);
            setCategory(FTEntities.RUBBER_BELLY_GLIDER.get(), MobCategory.WATER_AMBIENT);
            setCategory(AnglingEntities.ANOMALOCARIS.get(), MobCategory.WATER_AMBIENT);
            setCategory(AnglingEntities.SEA_SLUG.get(), MobCategory.WATER_AMBIENT);
            setCategory(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                    new net.minecraft.resources.ResourceLocation("hybrid_aquatic", "starfish")), MobCategory.WATER_AMBIENT);
            setCategory(EntityType.HOGLIN, MobCategory.CREATURE);
            setCategory(EntityType.STRIDER, MobCategory.CREATURE);
            setCategory(EntityType.MOOSHROOM, MobCategory.CREATURE);
        });
    }

    private static void setCategory(EntityType<?> type, MobCategory category) {
        ((EntityTypeCategoryAccessor) type).aoemod$setCategory(category);
    }
}
