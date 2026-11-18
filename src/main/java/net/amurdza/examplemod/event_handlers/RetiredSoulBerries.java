package net.amurdza.examplemod.event_handlers;

import com.soytutta.mynethersdelight.common.registry.MNDItems;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Existing saves lose the retired bushes and keep their food as bullet peppers. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RetiredSoulBerries {
    private RetiredSoulBerries() { }

    @SubscribeEvent
    public static void migrateRemovedCrop(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(Registries.BLOCK, AOEMod.MOD_ID)) {
            if (mapping.getKey().getPath().equals("soul_berry_bush")) mapping.remap(Blocks.AIR);
        }
        for (var mapping : event.getMappings(Registries.ITEM, AOEMod.MOD_ID)) {
            if (mapping.getKey().getPath().equals("soul_berries")) mapping.remap(MNDItems.BULLET_PEPPER.get());
        }
    }
}
