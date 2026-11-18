package net.amurdza.examplemod.gametest;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Exercise Forge's real registry import/sync code without resetting the running test world's registries. */
@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CharniaRegistryMigrationTests {
    private static final ResourceLocation AIR = new ResourceLocation("minecraft", "air");
    private static final ResourceLocation LIGHT_BLUE = new ResourceLocation("betterend", "charnia_light_blue");
    private static final ResourceLocation CYAN = new ResourceLocation("betterend", "charnia_cyan");
    private static final ResourceLocation OLD_BASKET = new ResourceLocation("farmersdelight", "basket");
    private static final ResourceLocation BASKET = new ResourceLocation("farmersdelight", "bamboo_basket");

    private CharniaRegistryMigrationTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void oldCharniaSnapshotCanLeaveAndCreateAnotherWorld(GameTestHelper helper) throws ReflectiveOperationException {
        for (ResourceLocation name : new ResourceLocation[]{Registries.BLOCK.location(), Registries.ITEM.location()}) {
            ForgeRegistry<Object> frozen = currentRegistry(name);
            ForgeRegistry.Snapshot saved = frozen.makeSnapshot();
            saved.ids.removeInt(CYAN);
            saved.aliases.put(CYAN, LIGHT_BLUE);
            saved.aliases.put(OLD_BASKET, BASKET);
            // Match the old world's actual level.dat: cyan has an alias but no numeric entry.
            saved = ForgeRegistry.Snapshot.read(saved.write());

            ForgeRegistry<Object> staged = detached(name);
            saved.aliases.forEach(staged::addAlias);
            staged.loadIds(saved.ids, saved.overrides, new Object2IntLinkedOpenHashMap<>(),
                    new HashMap<>(), frozen, name);
            helper.assertFalse(staged.containsKey(CYAN),
                    "The retired alias must not make Forge skip the real cyan registration: " + name);
            helper.assertTrue(staged.getRaw(LIGHT_BLUE) == frozen.getRaw(LIGHT_BLUE),
                    "Old light-blue blocks/items must retain their original saved identity");
            helper.assertTrue(staged.getRaw(OLD_BASKET) == frozen.getRaw(BASKET),
                    "The unrelated Farmer's Delight basket alias must remain usable");

            // This is Forge's loadFrozenDataToStagingRegistry missing-entry selection.
            var added = new Object2IntLinkedOpenHashMap<ResourceLocation>();
            frozen.getKeys().stream().filter(key -> !staged.containsKey(key))
                    .forEach(key -> added.put(key, frozen.getID(key)));
            staged.loadIds(added, frozen.makeSnapshot().overrides, new Object2IntLinkedOpenHashMap<>(),
                    new HashMap<>(), frozen, name);

            ForgeRegistry<Object> active = detached(name);
            for (int transition = 0; transition < 3; transition++) {
                sync(active, staged); // Join the old saved world.
                assertCurrentIdentities(helper, active, frozen);
                sync(active, frozen); // Leave it; previously threw after clearing the block palette.
                assertCurrentIdentities(helper, active, frozen);
                sync(active, frozen); // Start another world in the same process.
                assertCurrentIdentities(helper, active, frozen);
                helper.assertTrue(active.getRaw(OLD_BASKET) == frozen.getRaw(BASKET),
                        "Unrelated saved aliases must survive world transitions");
            }
            helper.assertFalse(active.makeSnapshot().aliases.containsKey(CYAN),
                    "The next save must stop persisting the retired cyan remap");
        }
        helper.assertTrue(Block.BLOCK_STATE_REGISTRY.getId(Blocks.AIR.defaultBlockState()) >= 0,
                "Registry migration testing must leave the running world's air palette intact");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void registrySyncAlsoRemovesStaleCharniaAliases(GameTestHelper helper) throws ReflectiveOperationException {
        for (ResourceLocation name : new ResourceLocation[]{Registries.BLOCK.location(), Registries.ITEM.location()}) {
            for (boolean staleSource : new boolean[]{false, true}) {
                ForgeRegistry<Object> frozen = currentRegistry(name);
                ForgeRegistry<Object> active = detached(name);
                // Deliberately bypass addAlias to model stale maps already held by either sync side.
                aliasMap(staleSource ? frozen : active).put(CYAN, LIGHT_BLUE);
                aliasMap(staleSource ? frozen : active).put(OLD_BASKET, BASKET);
                sync(active, frozen);
                assertCurrentIdentities(helper, active, frozen);
                helper.assertFalse(active.makeSnapshot().aliases.containsKey(CYAN),
                        "Directly merged stale aliases must be removed before Forge rebuilds numeric IDs");
                helper.assertTrue(active.getRaw(OLD_BASKET) == frozen.getRaw(BASKET),
                        "Sync cleanup must preserve unrelated aliases in either source or destination");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void charniaMigrationOnlyTargetsItsRetiredBlockAndItemAlias(GameTestHelper helper) throws ReflectiveOperationException {
        ForgeRegistry<Object> entities = detached(Registries.ENTITY_TYPE.location());
        entities.addAlias(CYAN, LIGHT_BLUE);
        helper.assertTrue(LIGHT_BLUE.equals(entities.makeSnapshot().aliases.get(CYAN)),
                "The Charnia migration must not alter other registry types");
        for (ResourceLocation name : new ResourceLocation[]{Registries.BLOCK.location(), Registries.ITEM.location()}) {
            ForgeRegistry<Object> registry = detached(name);
            registry.addAlias(CYAN, BASKET);
            helper.assertTrue(BASKET.equals(registry.makeSnapshot().aliases.get(CYAN)),
                    "Only the known historical cyan-to-light-blue pair may be removed");
            registry.addAlias(OLD_BASKET, BASKET);
            helper.assertTrue(BASKET.equals(registry.makeSnapshot().aliases.get(OLD_BASKET)),
                    "Unrelated remaps must not be discarded");
        }
        helper.succeed();
    }

    private static void assertCurrentIdentities(GameTestHelper helper, ForgeRegistry<Object> active,
                                                ForgeRegistry<Object> frozen) {
        for (ResourceLocation key : new ResourceLocation[]{AIR, LIGHT_BLUE, CYAN, BASKET}) {
            helper.assertTrue(active.getRaw(key) == frozen.getRaw(key)
                            && active.getID(key) == frozen.getID(key),
                    "World transitions must retain each plant, air and unrelated registration at its own ID: " + key);
        }
        helper.assertTrue(active.getID(CYAN) != active.getID(LIGHT_BLUE),
                "Restored cyan and light blue must never share a numeric registry ID");
    }

    private static ForgeRegistry<Object> currentRegistry(ResourceLocation name) throws ReflectiveOperationException {
        ForgeRegistry<Object> registry = detached(name);
        registry.register(0, AIR, new Object());
        registry.register(6237, LIGHT_BLUE, new Object());
        registry.register(6238, CYAN, new Object());
        registry.register(6250, BASKET, new Object());
        return registry;
    }

    @SuppressWarnings("unchecked")
    private static ForgeRegistry<Object> detached(ResourceLocation name) throws ReflectiveOperationException {
        Constructor<?> constructor = ForgeRegistry.class.getDeclaredConstructor(
                RegistryManager.class, ResourceLocation.class, RegistryBuilder.class);
        constructor.setAccessible(true);
        // No wrappers/callbacks: these are genuine Forge registries with no global state or world membership.
        return (ForgeRegistry<Object>) constructor.newInstance(new RegistryManager("AOE_CHARNIA_TEST"), name,
                new RegistryBuilder<>().setName(name));
    }

    private static void sync(ForgeRegistry<Object> target, ForgeRegistry<Object> source) throws ReflectiveOperationException {
        Method sync = ForgeRegistry.class.getDeclaredMethod("sync", ResourceLocation.class, ForgeRegistry.class);
        sync.setAccessible(true);
        sync.invoke(target, target.getRegistryName(), source);
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, ResourceLocation> aliasMap(ForgeRegistry<Object> registry) throws ReflectiveOperationException {
        Field aliases = ForgeRegistry.class.getDeclaredField("aliases");
        aliases.setAccessible(true);
        return (Map<ResourceLocation, ResourceLocation>) aliases.get(registry);
    }
}
