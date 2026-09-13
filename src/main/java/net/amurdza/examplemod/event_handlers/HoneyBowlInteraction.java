package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HoneyBowlInteraction {
    @SubscribeEvent
    public static void fillHoneyBowl(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldStack = event.getItemStack();
        if (!heldStack.is(Items.BOWL)) {
            return;
        }

        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if ((!state.is(Blocks.BEEHIVE) && !state.is(Blocks.BEE_NEST))
                || state.getValue(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.MAX_HONEY_LEVELS) {
            return;
        }

        Player player = event.getEntity();
        if (!level.isClientSide) {
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(
                    heldStack,
                    player,
                    new ItemStack(ModItems.HONEY_BOWL.get())
            ));
            player.awardStat(Stats.ITEM_USED.get(Items.BOWL));
            level.setBlock(event.getPos(), state.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);
            level.playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, event.getPos());
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }

    private HoneyBowlInteraction() {}
}
