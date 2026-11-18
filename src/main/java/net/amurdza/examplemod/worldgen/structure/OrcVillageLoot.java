package net.amurdza.examplemod.worldgen.structure;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.amurdza.examplemod.util.LootStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Reproducible shares of the former eight-chest budget, independent of opening order and player luck. */
public final class OrcVillageLoot {
    public static final String INDEX = "aoemod_village_chest";
    public static final String SEED = "aoemod_village_seed";
    public static final String ORIGIN = "aoemod_village_origin";
    public static final String COUNT = "aoemod_village_chest_count";
    public static final int CHESTS = 36, PREVIOUS_CHESTS = 8;

    private OrcVillageLoot() { }

    public static ObjectArrayList<ItemStack> budget(LootTable table, LootParams params, CompoundTag data) {
        var stable = new LootParams.Builder(params.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.of(data.getLong(ORIGIN))))
                .withLuck(0).create(LootContextParamSets.CHEST);
        var result = new ObjectArrayList<ItemStack>();
        var random = RandomSource.create(data.getLong(SEED));
        for (int i = 0; i < PREVIOUS_CHESTS; i++) {
            result.addAll(table.getRandomItems(stable, random.nextLong()));
            OrcCampStock.addProvisions(data.getInt(OrcCampStock.STOCK_TAG), random, result);
        }
        return result;
    }

    public static ObjectArrayList<ItemStack> share(LootTable table, LootParams params, CompoundTag data) {
        int index = data.getInt(INDEX);
        // Archived camps have no count marker and retain their original shares.
        int chests = data.contains(COUNT) ? data.getInt(COUNT) : CHESTS;
        var result = new ObjectArrayList<ItemStack>();
        if (chests <= 0 || index < 0 || index >= chests) return result;
        int cursor = 0;
        for (ItemStack stack : budget(table, params, data)) {
            int count = 0;
            for (int i = 0; i < stack.getCount(); i++) if (cursor++ % chests == index) count++;
            if (count > 0) result.add(stack.copyWithCount(count));
        }
        LootStacks.merge(result);
        return result;
    }
}
