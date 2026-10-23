package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.portal.PortalDestination;
import net.amurdza.examplemod.portal.PortalNetwork;
import net.amurdza.examplemod.portal.WoodenPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModPortals {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AOEMod.MOD_ID);
    private static final Map<PortalDestination, RegistryObject<WoodenPortalBlock>> PORTALS = new EnumMap<>(PortalDestination.class);

    static {
        for (PortalDestination destination : PortalDestination.values()) {
            PORTALS.put(destination, BLOCKS.register(destination.id() + "_portal", () -> new WoodenPortalBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().noOcclusion()
                            .strength(-1.0F).sound(SoundType.GLASS).lightLevel(state -> 11).noLootTable()
                            .pushReaction(PushReaction.BLOCK), destination)));
        }
    }

    private ModPortals() { }

    public static RegistryObject<WoodenPortalBlock> block(PortalDestination destination) {
        return PORTALS.get(destination);
    }

    public static Block[] blocks() {
        return PORTALS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(PortalNetwork::register));
    }
}
