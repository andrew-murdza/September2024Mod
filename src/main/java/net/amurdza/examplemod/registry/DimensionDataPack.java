package net.amurdza.examplemod.registry;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.resource.PathPackResources;

/** Existing worlds can retain the old order of mod data packs after dependencies are added. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DimensionDataPack {
    public static final String ID = "aoemod_dimension_rules";

    private DimensionDataPack() { }

    @SubscribeEvent
    public static void add(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        var root = ModList.get().getModFileById(AOEMod.MOD_ID).getFile().findResource("pack.mcmeta").getParent();
        event.addRepositorySource(consumer -> {
            Pack.ResourcesSupplier resources = name -> new PathPackResources(name, true, root);
            Pack.Info metadata = Pack.readPackInfo(ID, resources);
            if (metadata != null) consumer.accept(Pack.create(ID, Component.literal("AOE Dimension Rules"),
                    true, resources, metadata, PackType.SERVER_DATA, Pack.Position.TOP, false, PackSource.BUILT_IN));
        });
    }
}
