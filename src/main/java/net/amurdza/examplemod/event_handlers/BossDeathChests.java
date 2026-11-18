package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A normal chest at each authored boss's death, without overwriting terrain or inventories. */
public final class BossDeathChests {
    private static final String AWARDED = "aoemod_boss_chest_awarded";
    private static final Map<String, String> TABLES = Map.of(
            "aoemod:spider_queen", "spider_queen", "aoemod:illager_lord", "illager_lord",
            "twilightforest:alpha_yeti", "alpha_yeti", "orcz:bigorcboss", "orc_boss",
            "minecraft:wither", "wither");

    private BossDeathChests() { }

    public static ResourceLocation table(LivingEntity entity) {
        String name = TABLES.get(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())));
        return name == null ? null : new ResourceLocation("aoemod", "chests/bosses/" + name);
    }

    public static boolean handle(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        ResourceLocation table = table(entity);
        if (table == null) return false;
        // Preserve the intact Illager Lord shield that its native death hook
        // already generated, but put it inside the same reward chest.
        List<ItemStack> rewards = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) if (drop.getItem().is(ModItems.BOSS_SHIELD.get()))
            rewards.add(drop.getItem().copy());
        event.getDrops().clear();
        if (!(entity.level() instanceof ServerLevel level) || entity.getPersistentData().getBoolean(AWARDED)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return true;
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, entity.position())
                .create(LootContextParamSets.CHEST);
        rewards.addAll(level.getServer().getLootData().getLootTable(table).getRandomItems(params));
        net.amurdza.examplemod.util.LootStacks.merge(rewards);
        BlockPos position = findChestPosition(level, entity.blockPosition());
        if (position != null && level.setBlock(position, Blocks.CHEST.defaultBlockState(), 3)
                && level.getBlockEntity(position) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < rewards.size(); i++) {
                if (i < chest.getContainerSize()) chest.setItem(i, rewards.get(i));
                else event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), rewards.get(i)));
            }
            chest.setChanged();
        } else {
            // A death in midair/inside a filled room still awards the contents.
            // BlockItem restores this regular chest's inventory when placed.
            ItemStack chest = new ItemStack(Items.CHEST);
            CompoundTag contents = new CompoundTag();
            var items = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
            for (int i = 0; i < rewards.size(); i++) {
                if (i < items.size()) items.set(i, rewards.get(i));
                else event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), rewards.get(i)));
            }
            net.minecraft.world.ContainerHelper.saveAllItems(contents, items);
            chest.addTagElement("BlockEntityTag", contents);
            event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), chest));
        }
        entity.getPersistentData().putBoolean(AWARDED, true);
        return true;
    }

    private static BlockPos findChestPosition(ServerLevel level, BlockPos origin) {
        for (int radius = 0; radius <= 4; radius++) for (int offset = 0; offset <= 8; offset++) {
            for (int dy : offset == 0 ? new int[]{0} : new int[]{-offset, offset}) {
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    var state = level.getBlockState(pos);
                    if ((state.isAir() || state.is(Blocks.SNOW)) && state.getFluidState().isEmpty()
                            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return pos;
                }
            }
        }
        return null;
    }
}
