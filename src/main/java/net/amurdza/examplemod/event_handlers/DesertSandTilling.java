package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class DesertSandTilling {
    private DesertSandTilling() { }

    @SubscribeEvent
    public static void tillSand(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.HOE_TILL || !event.getState().is(Blocks.SAND)
                || !GameplayDimensions.is(event.getLevel(), GameplayDimensions.DESERT)
                || event.getContext().getClickedFace() == Direction.DOWN
                || !event.getLevel().isEmptyBlock(event.getPos().above())) return;
        var farmland = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("biomeswevegone", "sandy_farmland"));
        if (farmland != null && farmland != Blocks.AIR) event.setFinalState(farmland.defaultBlockState());
    }
}
