package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AOEMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> AOE_MOD = CREATIVE_MODE_TABS.register("aoe_mod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aoemod"))
                    .icon(() -> new ItemStack(ModBlocks.PALE_OAK_SAPLING.get()))
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().stream()
                                    .forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
