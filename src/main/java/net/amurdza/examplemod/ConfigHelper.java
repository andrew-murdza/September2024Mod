package net.amurdza.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

public class ConfigHelper {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static final ForgeConfigSpec SPEC = BUILDER.build();

    //    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
    //            .comment("Whether to log the dirt block on common setup")
    //            .define("logDirtBlock", true);
    //
    //    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
    //            .comment("A magic number")
    //            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
    private static boolean validateName(final Object obj, IForgeRegistry<?> registry) {
        return obj instanceof final String itemName && registry.containsKey(new ResourceLocation(itemName));
    }

    //Read List of Minecraft Registry Objects (like Item, Block, etc)
    static <T> List<T> readListOfObjects
    (String path, IForgeRegistry<T> registry,List<String> defaultList,String comment){
        ForgeConfigSpec.ConfigValue<List<? extends String>> configList = BUILDER.comment(comment)
                .defineListAllowEmpty(path, defaultList, x -> validateName(x, registry));
        return configList.get().stream().map(name->registry.getValue(new ResourceLocation(name))).toList();
    }
    static  <T> List<T> readListOfObjects
            (String path, IForgeRegistry<T> registry, List<String> defaultList){
        return readListOfObjects(path,registry,defaultList,"no comment");
    }
    static <T> List<T> readListOfObjects
            (String path, IForgeRegistry<T> registry){
        return readListOfObjects(path,registry,new ArrayList<>());
    }

    static <T> T read(String path, T defaultValue, String comment){
        return BUILDER.comment(comment).define(path,defaultValue).get();
    }

    static <T> T read(String path, T defaultValue){
        return read(path,defaultValue,"no comment");
    }

    static <T> List<? extends T> readList(String path, List<T> defaultValue, String comment){
        return BUILDER.comment(comment).defineListAllowEmpty(path,defaultValue,x->true).get();
    }

    static <T> List<? extends T> readList(String path, List<T> defaultValue){
        return readList(path,defaultValue,"no comment");
    }

    static <T> List<? extends T> readList(String path){
        return readList(path,new ArrayList<>());
    }

    static double readChance(String path, double defaultValue, String comment){
        return BUILDER.comment(comment).defineInRange(path,defaultValue,0,1).get();
    }

    static double readChance(String path, double defaultValue){
        return readChance(path,defaultValue,"no comment");
    }
}
