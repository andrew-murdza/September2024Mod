package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;
import static net.amurdza.examplemod.Config.BLACKLISTED_USE_ITEMS;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class CancelPlaceBlackListed {
    @SubscribeEvent
    public static void cancelPlace(PlayerInteractEvent.RightClickBlock event){
        final LevelAccessor level = event.getLevel();
        final BlockPos pos = event.getPos();
        if(Helper.isSpecialBiome(level,pos)){
            final Item item=event.getItemStack().getItem();
            if(!item.equals(Items.AIR)&&BLACKLISTED_USE_ITEMS.contains(item)){
                String message="You cannot place "+item+" in starter biomes";//+SPECIAL_BIOME;
                Helper.sendMessage(event.getEntity(),message);
                event.setCanceled(true);
            }
        }
    }

}
