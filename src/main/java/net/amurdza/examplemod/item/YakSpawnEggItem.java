package net.amurdza.examplemod.item;

import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Dedicated yak egg; old variant tags are upgraded when the saved item is used. */
public final class YakSpawnEggItem extends ForgeSpawnEggItem {
    public YakSpawnEggItem(Properties properties) { super(ModEntities.YAK, 0x6D4B35, 0xD4C9AA, properties); }
    public static void prepareStack(ItemStack stack) {
        var entity = stack.getOrCreateTagElement("EntityTag");
        entity.putString("id", "aoemod:yak");
        entity.remove("variant");
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        prepareStack(context.getItemInHand());
        InteractionResult result = super.useOn(context);
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (result.consumesAction() && !level.isClientSide
                && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
            var saved = spawner.getSpawner().save(new net.minecraft.nbt.CompoundTag());
            // Vanilla changes the next mob only. An existing weighted list could
            // otherwise select the old mob again after the first yak spawns.
            saved.remove("SpawnPotentials");
            spawner.getSpawner().load(level, pos, saved);
            spawner.setChanged();
            var state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        return result;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        prepareStack(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }
}
