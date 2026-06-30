package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ArchlichEntity;
import net.amurdza.examplemod.entity.CubozoaEntity;
import net.amurdza.examplemod.entity.EndFishEntity;
import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.amurdza.examplemod.entity.NetherFishEntity;
import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.amurdza.examplemod.entity.future.*;
import net.amurdza.examplemod.entity.sea_serpent.SeaSerpentEntity;
import net.amurdza.examplemod.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge 1.20.1 entity registration.
 * Register:
 *  - entity types via DeferredRegister
 *  - attributes via EntityAttributeCreationEvent
 *  - spawn placements via SpawnPlacementRegisterEvent
 *  - spawn eggs via item DeferredRegister (see below)
 */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
            EndFishEntity::new
    );

    public static final RegistryObject<EntityType<NetherFishEntity>> NETHER_FISH = registerMob(
            "nether_fish",
            MobCategory.WATER_AMBIENT,
            0.5F,
            0.5F,
            NetherFishEntity::new
    );

    public static final RegistryObject<EntityType<CubozoaEntity>> CUBOZOA = registerMob(
            "cubozoa",
            MobCategory.WATER_AMBIENT,
            0.6F,
            1.0F,
            CubozoaEntity::new
    );

    public static final RegistryObject<EntityType<ArchlichEntity>> ARCHLICH = registerMob(
            "archlich",
            MobCategory.MONSTER,
            0.6F,
            1.99F,
            ArchlichEntity::new
    );

    public static final RegistryObject<EntityType<SpiderQueenEntity>> SPIDER_QUEEN = registerMob(
            "spider_queen",
            MobCategory.MONSTER,
            2.3F,
            1.25F,
            SpiderQueenEntity::new
    );

    public static final RegistryObject<EntityType<IllagerLordEntity>> ILLAGER_LORD = registerMob(
            "illager_lord",
            MobCategory.MONSTER,
            0.7F,
            2.25F,
            IllagerLordEntity::new
    );

    public static final RegistryObject<EntityType<BoggedEntity>> BOGGED = registerMob(
            "bogged", MobCategory.MONSTER, 0.6F, 1.99F, BoggedEntity::new
    );

    public static final RegistryObject<EntityType<ParchedEntity>> PARCHED = registerMob(
            "parched", MobCategory.MONSTER, 0.6F, 1.99F, ParchedEntity::new
    );

    public static final RegistryObject<EntityType<BreezeEntity>> BREEZE = registerMob(
            "breeze", MobCategory.MONSTER, 0.6F, 1.77F, BreezeEntity::new
    );

    public static final RegistryObject<EntityType<CreakingEntity>> CREAKING = registerMob(
            "creaking", MobCategory.MONSTER, 0.9F, 2.7F, CreakingEntity::new
    );

    public static final RegistryObject<EntityType<CamelHuskEntity>> CAMEL_HUSK = registerMob(
            "camel_husk", MobCategory.MONSTER, 1.7F, 2.375F, CamelHuskEntity::new
    );

    public static final RegistryObject<EntityType<CamelHuskJockeyEntity>> CAMEL_HUSK_JOCKEY = registerMob(
            "camel_husk_jockey", MobCategory.MONSTER, 1.7F, 2.375F, CamelHuskJockeyEntity::new
    );

    public static final RegistryObject<EntityType<ZombieHorsemanEntity>> ZOMBIE_HORSEMAN = registerMob(
            "zombie_horseman", MobCategory.MONSTER, 1.3965F, 1.6F, ZombieHorsemanEntity::new
    );

    public static final RegistryObject<EntityType<SkeletonHorsemanEntity>> SKELETON_HORSEMAN = registerMob(
            "skeleton_horseman", MobCategory.MONSTER, 1.3965F, 1.6F, SkeletonHorsemanEntity::new
    );

    public static final RegistryObject<EntityType<NautilusEntity>> NAUTILUS = registerMob(
            "nautilus", MobCategory.WATER_CREATURE, 0.875F, 0.95F, NautilusEntity::new
    );

    public static final RegistryObject<EntityType<BreezeWindChargeEntity>> BREEZE_WIND_CHARGE =
            ENTITY_TYPES.register("breeze_wind_charge", () ->
                    EntityType.Builder.<BreezeWindChargeEntity>of(BreezeWindChargeEntity::new, MobCategory.MISC)
                            .sized(0.3125F, 0.3125F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build(AOEMod.MOD_ID + ":breeze_wind_charge"));

    public static final RegistryObject<EntityType<SeaSerpentEntity>> SEA_SERPENT =
            ENTITY_TYPES.register("sea_serpent", () ->
                    EntityType.Builder.of(SeaSerpentEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.1F, 1.1F)
                            .clientTrackingRange(10)
                            .build(AOEMod.MOD_ID + ":sea_serpent"));

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

    public static final RegistryObject<ForgeSpawnEggItem> NETHER_FISH_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_nether_fish",
                    () -> new ForgeSpawnEggItem(
                            NETHER_FISH,
                            ColorUtil.color(166, 57, 29),
                            ColorUtil.color(255, 169, 54),
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

    public static final RegistryObject<ForgeSpawnEggItem> SEA_SERPENT_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_sea_serpent",
                    () -> new ForgeSpawnEggItem(
                            SEA_SERPENT,
                            ColorUtil.color(151, 77, 181),
                            ColorUtil.color(93, 176, 238),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    public static final RegistryObject<ForgeSpawnEggItem> ARCHLICH_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_archlich",
                    () -> new ForgeSpawnEggItem(
                            ARCHLICH,
                            ColorUtil.color(29, 38, 59),
                            ColorUtil.color(117, 210, 232),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    public static final RegistryObject<ForgeSpawnEggItem> SPIDER_QUEEN_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_spider_queen",
                    () -> new ForgeSpawnEggItem(
                            SPIDER_QUEEN,
                            ColorUtil.color(34, 24, 42),
                            ColorUtil.color(142, 38, 157),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    public static final RegistryObject<ForgeSpawnEggItem> ILLAGER_LORD_SPAWN_EGG =
            ModItems.ITEMS.register("spawn_egg_illager_lord",
                    () -> new ForgeSpawnEggItem(
                            ILLAGER_LORD,
                            ColorUtil.color(72, 86, 82),
                            ColorUtil.color(178, 178, 178),
                            new net.minecraft.world.item.Item.Properties()
                    )
            );

    public static final RegistryObject<ForgeSpawnEggItem> BOGGED_SPAWN_EGG = spawnEgg(
            "bogged", BOGGED, 0x566A42, 0x8F9B75
    );
    public static final RegistryObject<ForgeSpawnEggItem> PARCHED_SPAWN_EGG = spawnEgg(
            "parched", PARCHED, 0xC9A56B, 0x6F5239
    );
    public static final RegistryObject<ForgeSpawnEggItem> BREEZE_SPAWN_EGG = spawnEgg(
            "breeze", BREEZE, 0x8CB8C5, 0xDDEEF1
    );
    public static final RegistryObject<ForgeSpawnEggItem> CREAKING_SPAWN_EGG = spawnEgg(
            "creaking", CREAKING, 0x4A3525, 0xE09A3E
    );
    public static final RegistryObject<ForgeSpawnEggItem> CAMEL_HUSK_SPAWN_EGG = spawnEgg(
            "camel_husk", CAMEL_HUSK, 0x75614A, 0xB8A073
    );
    public static final RegistryObject<ForgeSpawnEggItem> CAMEL_HUSK_JOCKEY_SPAWN_EGG = spawnEgg(
            "camel_husk_jockey", CAMEL_HUSK_JOCKEY, 0x66513C, 0x9E8967
    );
    public static final RegistryObject<ForgeSpawnEggItem> ZOMBIE_HORSEMAN_SPAWN_EGG = spawnEgg(
            "zombie_horseman", ZOMBIE_HORSEMAN, 0x315234, 0x78906C
    );
    public static final RegistryObject<ForgeSpawnEggItem> SKELETON_HORSEMAN_SPAWN_EGG = spawnEgg(
            "skeleton_horseman", SKELETON_HORSEMAN, 0xC7C7C7, 0x4C4C4C
    );
    public static final RegistryObject<ForgeSpawnEggItem> NAUTILUS_SPAWN_EGG = spawnEgg(
            "nautilus", NAUTILUS, 0xD69B62, 0x6E4B38
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
    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        // Config gating: attribute registration is harmless even if you later disable spawns/eggs.
        event.put(END_FISH.get(), EndFishEntity.createMobAttributes().build());
        event.put(NETHER_FISH.get(), NetherFishEntity.createMobAttributes().build());
        event.put(CUBOZOA.get(), CubozoaEntity.createMobAttributes().build());
        event.put(ModEntities.SEA_SERPENT.get(), SeaSerpentEntity.createAttributes().build());
        event.put(ARCHLICH.get(), ArchlichEntity.createAttributes().build());
        event.put(SPIDER_QUEEN.get(), SpiderQueenEntity.createAttributes().build());
        event.put(ILLAGER_LORD.get(), IllagerLordEntity.createAttributes().build());
        event.put(BOGGED.get(), BoggedEntity.createAttributes().build());
        event.put(PARCHED.get(), ParchedEntity.createAttributes().build());
        event.put(BREEZE.get(), BreezeEntity.createAttributes().build());
        event.put(CREAKING.get(), CreakingEntity.createAttributes().build());
        event.put(CAMEL_HUSK.get(), CamelHuskEntity.createAttributes().build());
        event.put(CAMEL_HUSK_JOCKEY.get(), CamelHuskEntity.createAttributes().build());
        event.put(ZOMBIE_HORSEMAN.get(), net.minecraft.world.entity.animal.horse.ZombieHorse.createAttributes().build());
        event.put(SKELETON_HORSEMAN.get(), net.minecraft.world.entity.animal.horse.SkeletonHorse.createAttributes().build());
        event.put(NAUTILUS.get(), NautilusEntity.createAttributes().build());
    }

    @SubscribeEvent
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
                NETHER_FISH.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                ModEntities::checkLavaFishSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                CUBOZOA.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                ModEntities::checkLavaFishSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                SEA_SERPENT.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING,
                TropicalFish::checkSurfaceWaterAnimalSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                BOGGED.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                PARCHED.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                BREEZE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                CREAKING.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                NAUTILUS.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR
        );
    }

    // ---------------
    // Helper methods
    // ---------------
    private static boolean checkLavaFishSpawnRules(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getFluidState(pos).is(FluidTags.LAVA)
                && level.getFluidState(pos.above()).is(FluidTags.LAVA);
    }

    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(
            String name,
            MobCategory category,
            float width,
            float height,
            EntityType.EntityFactory<T> factory
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
            EntityType.EntityFactory<T> factory
    ) {
        return registerEntity(name, category, width, height, factory);
    }

    private static <T extends Mob> RegistryObject<ForgeSpawnEggItem> spawnEgg(
            String name,
            RegistryObject<EntityType<T>> type,
            int primaryColor,
            int secondaryColor
    ) {
        return ModItems.ITEMS.register(
                "spawn_egg_" + name,
                () -> new ForgeSpawnEggItem(type, primaryColor, secondaryColor, new net.minecraft.world.item.Item.Properties())
        );
    }
}
