package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RemoveBlackBackground {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event){
        for(Supplier<Block> block:new Supplier[]{ModBlocks.CAVE_VINES, ModBlocks.CAVE_VINES_PLANT,ModBlocks.GRAPE_VINE,ModBlocks.BLUE_BERRY_BUSH}){
            setRenderLayer(block, RenderType.cutout());
        }
    }
    private static void setRenderLayer(Supplier<? extends Block> supplier, RenderType... types) {
        ItemBlockRenderTypes.setRenderLayer(supplier.get(), t -> {
            for(RenderType allowed : types)
                if(t==allowed)
                    return true;
            return false;
        });
    }
}
