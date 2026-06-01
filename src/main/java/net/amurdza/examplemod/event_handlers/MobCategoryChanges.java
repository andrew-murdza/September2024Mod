package net.amurdza.examplemod.event_handlers;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import dev.hybridlabs.aquatic.entity.HybridAquaticEntityTypes;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.mob_spawning.EntityTypeCategoryAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.violetmoon.quark.content.mobs.module.FoxhoundModule;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobCategoryChanges {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            setCategory(AMEntityRegistry.JERBOA.get(), MobCategory.CREATURE);

            setCategory(HybridAquaticEntityTypes.INSTANCE.getMANTA_RAY().get(), MobCategory.WATER_CREATURE);
            setCategory(HybridAquaticEntityTypes.INSTANCE.getTUNA().get(), MobCategory.WATER_CREATURE);
            setCategory(ACEntityRegistry.HULLBREAKER.get(), MobCategory.WATER_CREATURE);

            if (FoxhoundModule.foxhoundType != null) {
                setCategory(FoxhoundModule.foxhoundType, MobCategory.CREATURE);
            }
        });
    }

    private static void setCategory(EntityType<?> type, MobCategory category) {
        ((EntityTypeCategoryAccessor) type).aoemod$setCategory(category);
    }
}