package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.enchantment.MaceEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, AOEMod.MOD_ID);

    public static final RegistryObject<Enchantment> DENSITY = ENCHANTMENTS.register(
            "density",
            () -> new MaceEnchantment(Enchantment.Rarity.COMMON, 5, 5, 8)
    );

    public static final RegistryObject<Enchantment> BREACH = ENCHANTMENTS.register(
            "breach",
            () -> new MaceEnchantment(Enchantment.Rarity.RARE, 4, 15, 9)
    );

    public static final RegistryObject<Enchantment> WIND_BURST = ENCHANTMENTS.register(
            "wind_burst",
            () -> new MaceEnchantment(Enchantment.Rarity.VERY_RARE, 3, 15, 9)
    );

    private ModEnchantments() {
    }

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
