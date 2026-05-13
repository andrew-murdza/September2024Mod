package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.legacy.blue_skies.registries.SkiesEntityTypes;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.mixins.mob_spawning.EntityTypeCategoryAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.violetmoon.quark.content.mobs.module.FoxhoundModule;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobCategoryChanges {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            setCategory(AMEntityRegistry.JERBOA.get(), MobCategory.CREATURE);
            setCategory(SkiesEntityTypes.EMBERBACK, MobCategory.CREATURE);
            setCategory(IafEntityRegistry.AMPHITHERE.get(), MobCategory.CREATURE);
            setCategory(IafEntityRegistry.SIREN.get(), MobCategory.CREATURE);
            setCategory(IafEntityRegistry.CYCLOPS.get(), MobCategory.CREATURE);
            setCategory(IafEntityRegistry.STYMPHALIAN_BIRD.get(), MobCategory.CREATURE);
            setCategory(AMEntityRegistry.JERBOA.get(), MobCategory.CREATURE);

            // Move to water_creature
            setCategory("hybrid-aquatic:manta_ray", MobCategory.WATER_CREATURE);
            setCategory("hybrid-aquatic:tuna", MobCategory.WATER_CREATURE);
            setCategory("hybrid-aquatic:dragonfish", MobCategory.WATER_CREATURE);
            setCategory("unusualfishmod:prawn", MobCategory.WATER_CREATURE);
            setCategory("alexscaves:hullbreaker", MobCategory.WATER_CREATURE);

            // Move to water_ambient
            setCategory("unusualfishmod:wizard_jelly", MobCategory.WATER_AMBIENT);
            setCategory("ambientadditions:blue_spotted_stingray", MobCategory.WATER_AMBIENT);
            setCategory("finsandtails:golden_river_ray", MobCategory.WATER_AMBIENT);
            setCategory("finsandtails:rubber_belly_glider", MobCategory.WATER_AMBIENT);
            setCategory("alexsmobs:triops", MobCategory.WATER_AMBIENT);
            setCategory("ambientadditions:shame_faced_crab", MobCategory.WATER_AMBIENT);

            // Foxhound is already built as CREATURE in Quark's EntityType.
            // This line is harmless, but it also guarantees EntityType#getCategory() is CREATURE.
            if (FoxhoundModule.foxhoundType != null) {
                setCategory(FoxhoundModule.foxhoundType, MobCategory.CREATURE);
            }
        });
    }

    private static void setCategory(EntityType<?> type, MobCategory category) {
        ((EntityTypeCategoryAccessor) type).aoemod$setCategory(category);
    }
    private static void setCategory(String id, MobCategory category) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));

        if (type != null) {
            ((EntityTypeCategoryAccessor) type).aoemod$setCategory(category);
        }
    }
}