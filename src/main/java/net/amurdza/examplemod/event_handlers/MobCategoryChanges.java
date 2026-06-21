package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.accessor.EntityTypeCategoryAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobCategoryChanges {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> setCategory(EntityType.HOGLIN, MobCategory.CREATURE));
    }

    private static void setCategory(EntityType<?> type, MobCategory category) {
        ((EntityTypeCategoryAccessor) type).aoemod$setCategory(category);
    }
}