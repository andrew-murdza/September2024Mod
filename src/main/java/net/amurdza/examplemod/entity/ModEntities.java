package net.amurdza.examplemod.entity;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.util.ColorUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge 1.20.1 entity registration.
 *
 * Register:
 *  - entity types via DeferredRegister
 *  - attributes via EntityAttributeCreationEvent
 *  - spawn placements via SpawnPlacementRegisterEvent
 *  - spawn eggs via item DeferredRegister (see below)
 */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AOEMod.MOD_ID);

    // ---------
    // Entities
    // ---------

    public static final RegistryObject<EntityType<EndFishEntity>> END_FISH = registerMob(
            "end_fish",
            MobCategory.WATER_AMBIENT,
            0.5F,
            0.5F,
            EndFishEntity::new,
            true
    );

    public static final RegistryObject<EntityType<CubozoaEntity>> CUBOZOA = registerMob(
            "cubozoa",
            MobCategory.WATER_AMBIENT,
            0.6F,
            1.0F,
            CubozoaEntity::new,
            true
    );

    // -------------
    // Spawn Eggs
    // -------------
    // Forge eggs are ITEMS, so they should be registered in your ModItems DeferredRegister.
    //
    // If you want them “config gated”, you can still register them and make them unobtainable,
    // OR you can always register but hide via creative tab/event, OR you can register a dummy.
    //
    // This example registers them normally, but you can conditionally *expose* them elsewhere.

    public static final RegistryObject<ForgeSpawnEggItem> END_FISH_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_end_fish",
                    () -> new ForgeSpawnEggItem(
                            END_FISH,
                            ColorUtil.color(3, 50, 76),
                            ColorUtil.color(120, 206, 255),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    public static final RegistryObject<ForgeSpawnEggItem> CUBOZOA_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_cubozoa",
                    () -> new ForgeSpawnEggItem(
                            CUBOZOA,
                            ColorUtil.color(151, 77, 181),
                            ColorUtil.color(93, 176, 238),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    // ---------------------
    // Public init/register
    // ---------------------

    /**
     * Call from your mod constructor: MobEntities.register(modEventBus);
     */
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);

        // ModItems.ITEMS.register(modEventBus);  // do this in your ModItems class, not necessarily here
        // and register your event handlers below (either here or in your main mod class).
    }

    // -------------------
    // Event registrations
    // -------------------

    /**
     * Call this from your mod setup by registering it to the MOD event bus,
     * e.g. modEventBus.addListener(MobEntities::onEntityAttributes);
     */
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        // Config gating: attribute registration is harmless even if you later disable spawns/eggs.
        event.put(END_FISH.get(), EndFishEntity.createMobAttributes().build());
        event.put(CUBOZOA.get(), CubozoaEntity.createMobAttributes().build());
    }

    /**
     * Call this from your mod setup by registering it to the MOD event bus,
     * e.g. modEventBus.addListener(MobEntities::onSpawnPlacements);
     */
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // If you want Fabric-like “config disables entity entirely”, the closest Forge pattern is:
        // - always register EntityType
        // - if disabled in config, DON'T register spawn placements / biome spawns / eggs.
        event.register(
                END_FISH.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING,
                TropicalFish::checkSurfaceWaterAnimalSpawnRules, // implement like vanilla fish, or use a shared predicate
                SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                CUBOZOA.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING,
                TropicalFish::checkSurfaceWaterAnimalSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
    }

    // ---------------
    // Helper methods
    // ---------------
    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(
            String name,
            MobCategory category,
            float width,
            float height,
            EntityType.EntityFactory<T> factory,
            boolean fixedSize
    ) {
        return ENTITY_TYPES.register(name, () ->
                EntityType.Builder
                        .of(factory, category)
                        .sized(width, height) // Forge/vanilla builder uses “sized”; it’s effectively fixed.
                        // If you truly need scalable vs fixed behavior, that’s primarily about hitbox updates.
                        // Most mods just use sized().
                        .build(new ResourceLocation(AOEMod.MOD_ID, name).toString())
        );
    }

    private static <T extends Mob> RegistryObject<EntityType<T>> registerMob(
            String name,
            MobCategory category,
            float width,
            float height,
            EntityType.EntityFactory<T> factory,
            boolean fixedSize
    ) {
        return registerEntity(name, category, width, height, factory, fixedSize);
    }
}
