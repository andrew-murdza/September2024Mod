package net.amurdza.examplemod.event_handlers;

import dev.hybridlabs.aquatic.item.HAItemGroups;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import dev.hybridlabs.hapi.item.HAPIItems;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Hybrid Aquatic transports its creatures in HAPI nets, rather than mob buckets. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RetainedCreatureNets {
    private RetainedCreatureNets() { }

    public static void addFilledNets(BuildCreativeModeTabContentsEvent event) {
        // Capture data remains readable in saved items, but nets are no longer
        // part of the creative selection.
    }

    public static List<ItemStack> filledNets() {
        return ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                .filter(id -> id.getNamespace().equals("hybrid_aquatic") && RetainedContent.isKeptEntity(id))
                .sorted().map(id -> {
                    ItemStack net = new ItemStack(HAPIItems.INSTANCE.getCREATURE_NET().get());
                    CompoundTag entity = new CompoundTag();
                    entity.putString("id", id.toString());
                    entity.putBoolean("PersistenceRequired", true);
                    // The native writer and native reader use these different
                    // keys; include both to preserve the captured state.
                    entity.putBoolean("FromCreatureNet", true);
                    entity.putBoolean("FromFishingNet", true);
                    entity.putInt("Hunger", BaseWaterAnimal.MAX_HUNGER);
                    entity.putInt("Moistness", 600);
                    entity.putInt("Size", 0);
                    net.getOrCreateTag().put("storedEntity", entity);
                    return net;
                }).toList();
    }
}
