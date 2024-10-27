package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class ShearingMobs {
    @SubscribeEvent
    public static void shearingMobs(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            BlockPos pos=mob.getOnPos();
            if(!Helper.isOkMob(event.getLevel(),pos,mob)){
                event.setCanceled(true);
            }
            else {
                if(mob instanceof MushroomCow){
                    if(event.getItemStack().is(Items.SHEARS)){
                        event.setCanceled(true);
                    }
                }
//                else if(mob instanceof Sheep sheep&&event.getItemStack().is(Items.SHEARS)){
//                    Item wool=woolMap.get(sheep.getColor());
//                    mob.spawnAtLocation(new ItemStack(wool, Math.max(0,Config.WOOL_FROM_SHEAR-1)));
//                }
            }
        }
    }
    private static final Map<DyeColor, Item> woolMap = new HashMap<>();
    static {
        woolMap.put(DyeColor.WHITE, Items.WHITE_WOOL);
        woolMap.put(DyeColor.ORANGE, Items.ORANGE_WOOL);
        woolMap.put(DyeColor.MAGENTA, Items.MAGENTA_WOOL);
        woolMap.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL);
        woolMap.put(DyeColor.YELLOW, Items.YELLOW_WOOL);
        woolMap.put(DyeColor.LIME, Items.LIME_WOOL);
        woolMap.put(DyeColor.PINK, Items.PINK_WOOL);
        woolMap.put(DyeColor.GRAY, Items.GRAY_WOOL);
        woolMap.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL);
        woolMap.put(DyeColor.CYAN, Items.CYAN_WOOL);
        woolMap.put(DyeColor.PURPLE, Items.PURPLE_WOOL);
        woolMap.put(DyeColor.BLUE, Items.BLUE_WOOL);
        woolMap.put(DyeColor.BROWN, Items.BROWN_WOOL);
        woolMap.put(DyeColor.GREEN, Items.GREEN_WOOL);
        woolMap.put(DyeColor.RED, Items.RED_WOOL);
        woolMap.put(DyeColor.BLACK, Items.BLACK_WOOL);
    }
}
