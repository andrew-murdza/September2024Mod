package net.amurdza.examplemod.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One seed shared by every chunk and chest in the same camp. */
public final class OrcCampStock {
    public static final String STOCK_TAG = "aoemod_camp_stock";

    private record Animal(String id, String raw, String cooked) { }
    private static final Animal[][] STOCK = {
            {new Animal("alexsmobs:bison", "wildernature:bison_meat", "wildernature:cooked_bison_meat"),
                    new Animal("minecraft:sheep", "minecraft:mutton", "minecraft:cooked_mutton")},
            {new Animal("alexsmobs:gazelle", "wildernature:venison", "wildernature:cooked_venison"),
                    new Animal("biodiverse:oryx", "wildernature:venison", "wildernature:cooked_venison")},
            {new Animal("alexsmobs:emu", "wildernature:cassowary_meat", "wildernature:cooked_cassowary_meat"),
                    new Animal("alexsmobs:kangaroo", "alexsmobs:kangaroo_meat", "alexsmobs:cooked_kangaroo_meat")},
            {new Animal("twilightforest:boar", "minecraft:porkchop", "minecraft:cooked_porkchop"),
                    new Animal("biodiverse:wildebeest", "minecraft:beef", "minecraft:cooked_beef")}
    };

    private OrcCampStock() { }

    public static int selection(long seed, BlockPos origin) {
        return RandomSource.create(seed ^ Mth.getSeed(origin)).nextInt(256);
    }

    public static String animal(int selection, int herd) {
        return STOCK[herd][selection >> herd & 1].id();
    }

    public static BlockState matureCrop(int selection, int strip) {
        // Each irrigated field grows wheat on one bank and a second crop on the other.
        int field = strip / 2;
        String crop = "minecraft:wheat";
        if (strip == 1 || strip == 2) {
            crop = field == 0 ? ((selection & 16) == 0 ? "minecraft:carrots" : "minecraft:beetroots")
                    : ((selection & 32) == 0 ? "farmersdelight:cabbages" : "minecraft:potatoes");
        }
        var plant = (CropBlock) ForgeRegistries.BLOCKS.getValue(new ResourceLocation(crop));
        return plant.getStateForAge(plant.getMaxAge());
    }

    public static void addProvisions(int selection, RandomSource random, List<ItemStack> loot) {
        var available = new ArrayList<String>();
        for (int herd = 0; herd < STOCK.length; herd++) {
            var animal = STOCK[herd][selection >> herd & 1];
            Collections.addAll(available, animal.raw, animal.cooked);
        }
        if ((selection & 4) == 0) Collections.addAll(available, "alexsmobs:emu_feather", "alexsmobs:emu_egg", "alexsmobs:boiled_emu_egg");
        if ((selection & 16) == 0) Collections.addAll(available, "minecraft:carrot", "aoemod:carrot_soup");
        else Collections.addAll(available, "minecraft:beetroot", "minecraft:beetroot_seeds", "minecraft:beetroot_soup");
        if ((selection & 32) == 0) Collections.addAll(available, "farmersdelight:cabbage", "farmersdelight:cabbage_seeds", "farmersdelight:cabbage_rolls");
        else Collections.addAll(available, "minecraft:potato", "minecraft:baked_potato");
        sampleProvisions(available, random, loot);
    }

    public static void addLegacyProvisions(RandomSource random, List<ItemStack> loot) {
        // Unopened chests predating stock selection belonged to sheep/boar camps
        // with wheat and carrots. Their saved inventories have no selection tag.
        sampleProvisions(new ArrayList<>(List.of("minecraft:mutton", "minecraft:cooked_mutton", "minecraft:porkchop",
                "minecraft:cooked_porkchop", "minecraft:carrot", "aoemod:carrot_soup")), random, loot);
    }

    private static void sampleProvisions(List<String> available, RandomSource random, List<ItemStack> loot) {
        int rolls = 3 + random.nextInt(4);
        for (int i = 0; i < rolls; i++) {
            String id = available.remove(random.nextInt(available.size()));
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (item != null) loot.add(new ItemStack(item, Math.min(item.getMaxStackSize(), 2 + random.nextInt(5))));
        }
    }
}
