package net.amurdza.examplemod.gametest;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.RetiredBclibWorldgen;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetiredBclibWorldgenTests {
    private static final long SAVED_SEED = -7157108048730296141L;

    private RetiredBclibWorldgenTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void realWorldLoaderDecodesRetiredNetherAndEndWithoutEditingInput(GameTestHelper helper) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        CompoundTag current = currentSettings(helper, ops);
        helper.assertTrue(WorldGenSettings.CODEC.parse(ops, current).result().isPresent(),
                "The current dimension set must be a valid codec baseline");
        for (boolean wrapped : new boolean[]{false, true}) {
            CompoundTag old = current.copy();
            for (String dimension : new String[]{"minecraft:the_nether", "minecraft:the_end"}) {
                boolean end = dimension.endsWith("the_end");
                CompoundTag generator = old.getCompound("dimensions").getCompound(dimension).getCompound("generator");
                generator.putString("type", "bclib:betterx");
                generator.put("biome_source", savedSource(end, wrapped && end));
            }
            CompoundTag untouched = old.copy();
            helper.assertTrue(WorldGenSettings.CODEC.parse(ops, old).error().isPresent(),
                    "This representative old save must actually require the removed codecs");
            CompoundTag levelData = new CompoundTag();
            levelData.put("WorldGenSettings", old);
            DataResult<WorldGenSettings> loaded = readThroughActualLoader(new Dynamic<>(ops, levelData));
            helper.assertTrue(loaded.error().isEmpty(), "The actual world-loading hook must repair retired codecs: " + loaded.error());
            WorldGenSettings world = loaded.result().orElseThrow();
            helper.assertTrue(world.options().seed() == SAVED_SEED && world.options().generateStructures(),
                    "Load migration must retain the original world seed and generation options");
            for (var key : java.util.List.of(LevelStem.NETHER, LevelStem.END)) {
                helper.assertTrue(world.dimensions().get(key).orElseThrow().generator() instanceof NoiseBasedChunkGenerator,
                        "The two retired generators must decode as ordinary vanilla noise generators");
            }
            helper.assertTrue(old.equals(untouched) && levelData.getCompound("WorldGenSettings").equals(untouched),
                    "World loading must not mutate the input level.dat tree");
            CompoundTag migrated = (CompoundTag) RetiredBclibWorldgen.migrate(new Dynamic<Tag>(ops, old)).getValue();
            assertPreserved(helper, untouched, migrated);
            helper.assertTrue(RetiredBclibWorldgen.migrate(new Dynamic<Tag>(ops, migrated)).getValue().equals(migrated),
                    "Migration must be idempotent before the next normal save");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void migrationPreservesVanillaForeignCodecsAndBlueprintMetadata(GameTestHelper helper) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        CompoundTag vanilla = currentSettings(helper, ops);
        helper.assertTrue(RetiredBclibWorldgen.migrate(new Dynamic<Tag>(ops, vanilla)).getValue().equals(vanilla),
                "A new world with vanilla codecs must remain unchanged");
        CompoundTag scoped = vanilla.copy();
        CompoundTag end = scoped.getCompound("dimensions").getCompound("minecraft:the_end").getCompound("generator");
        end.put("biome_source", savedSource(true, true));
        // A valid generator with a retired nested source must also be repairable.
        CompoundTag wrapper = end.getCompound("biome_source");
        wrapper.putString("custom_note", "preserve wrapper metadata");
        CompoundTag foreignDimension = scoped.getCompound("dimensions").getCompound("minecraft:the_end").copy();
        foreignDimension.getCompound("generator").putString("type", "bclib:betterx");
        scoped.getCompound("dimensions").put("example:unrelated_dimension", foreignDimension.copy());
        CompoundTag repaired = (CompoundTag) RetiredBclibWorldgen.migrate(new Dynamic<Tag>(ops, scoped)).getValue();
        var repairedWrapper = repaired.getCompound("dimensions").getCompound("minecraft:the_end")
                .getCompound("generator").getCompound("biome_source");
        helper.assertTrue(repairedWrapper.getString("type").equals("blueprint:modded")
                        && repairedWrapper.getString("custom_note").equals("preserve wrapper metadata")
                        && repairedWrapper.getCompound("original_biome_source").getString("type").equals("minecraft:the_end")
                        && repairedWrapper.getCompound("original_biome_source").getLong("seed") == SAVED_SEED,
                "Only the retired nested source may change; Blueprint and the saved seed must survive");
        helper.assertTrue(repaired.getCompound("dimensions").getCompound("example:unrelated_dimension").equals(foreignDimension),
                "Even a matching codec in a foreign dimension is outside this migration's scope");
        end.putString("type", "example:custom_generator");
        end.getCompound("biome_source").putString("type", "example:custom_wrapper");
        helper.assertTrue(RetiredBclibWorldgen.migrate(new Dynamic<Tag>(ops, scoped)).getValue().equals(scoped),
                "Unknown generators and wrappers must not be rewritten speculatively");
        helper.succeed();
    }

    private static void assertPreserved(GameTestHelper helper, CompoundTag before, CompoundTag after) {
        for (String name : before.getAllKeys()) {
            if (!name.equals("dimensions")) helper.assertTrue(before.get(name).equals(after.get(name)),
                    "Preserve world option " + name);
        }
        var beforeDimensions = before.getCompound("dimensions");
        var afterDimensions = after.getCompound("dimensions");
        for (String name : beforeDimensions.getAllKeys()) {
            if (!name.equals("minecraft:the_end") && !name.equals("minecraft:the_nether")) {
                helper.assertTrue(beforeDimensions.get(name).equals(afterDimensions.get(name)),
                        "Preserve the entire existing AoE/other dimension entry: " + name);
            } else {
                helper.assertTrue(beforeDimensions.getCompound(name).getCompound("generator").get("settings")
                                .equals(afterDimensions.getCompound(name).getCompound("generator").get("settings")),
                        "Preserve the saved noise settings: " + name);
            }
        }
    }

    private static CompoundTag currentSettings(GameTestHelper helper, RegistryOps<Tag> ops) {
        CompoundTag settings = new CompoundTag(), dimensions = new CompoundTag();
        settings.putLong("seed", SAVED_SEED);
        settings.putBoolean("generate_features", true);
        settings.putBoolean("bonus_chest", false);
        for (var level : helper.getLevel().getServer().getAllLevels()) {
            var stem = new LevelStem(level.dimensionTypeRegistration(), level.getChunkSource().getGenerator());
            dimensions.put(level.dimension().location().toString(), LevelStem.CODEC.encodeStart(ops, stem).result().orElseThrow());
        }
        settings.put("dimensions", dimensions);
        return settings;
    }

    /** These keys and values match the audited pre-removal1.20.1 level.dat metadata. */
    private static CompoundTag savedSource(boolean end, boolean wrapped) {
        CompoundTag source = new CompoundTag(), config = new CompoundTag();
        source.putString("type", end ? "bclib:end_biome_source" : "bclib:nether_biome_source");
        source.putLong("seed", SAVED_SEED);
        config.putString("map_type", "hex");
        config.putString("generator_version", "vanilla");
        if (end) {
            config.putInt("inner_void_radius_squared", 1048576);
            for (String area : new String[]{"barrens", "land", "center", "void"}) config.putInt(area + "_biomes_size", 256);
            config.putBoolean("with_void_biomes", true);
        }
        source.put("config", config);
        if (!wrapped) return source;
        CompoundTag wrapper = new CompoundTag();
        wrapper.putString("type", "blueprint:modded");
        wrapper.put("original_biome_source", source);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private static DataResult<WorldGenSettings> readThroughActualLoader(Dynamic<Tag> levelData) {
        try {
            // Match the unique signature so this assertion also works with runtime
            // method remapping; it executes the real transformed loading method.
            var method = Arrays.stream(LevelStorageSource.class.getDeclaredMethods())
                    .filter(candidate -> Arrays.equals(candidate.getParameterTypes(),
                            new Class<?>[]{Dynamic.class, DataFixer.class, int.class}))
                    .findFirst().orElseThrow();
            method.setAccessible(true);
            return (DataResult<WorldGenSettings>) method.invoke(null, levelData, DataFixers.getDataFixer(),
                    SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("World loading failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot exercise the actual world-loading path", failure);
        }
    }
}
