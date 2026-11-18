package net.amurdza.examplemod.client;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

/** Place selected crop artwork, models and names above merged dependency resources. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CropVisualOverridesPack extends AbstractPackResources {
    private static final Set<ResourceLocation> RESOURCES = resources();
    private static final byte[] METADATA = ("{\"pack\":{\"pack_format\":15,"
            + "\"description\":\"AOE crop visuals and names\"}}").getBytes(StandardCharsets.UTF_8);

    private CropVisualOverridesPack(String name) { super(name, true); }

    private static Set<ResourceLocation> resources() {
        Set<ResourceLocation> resources = new LinkedHashSet<>();
        for (String part : new String[]{"bottom", "middle", "top"}) {
            for (int stage = 0; stage <= 5; stage++) {
                resources.add(new ResourceLocation("hearthandharvest", "textures/block/corn_" + part + "_stage_" + stage + ".png"));
            }
        }
        for (int stage = 0; stage <= 3; stage++) {
            resources.add(new ResourceLocation("hearthandharvest", "textures/block/cotton_stage" + stage + ".png"));
        }
        resources.add(new ResourceLocation("hearthandharvest", "textures/block/wild_cotton.png"));
        for (int stage = 1; stage <= 4; stage++) {
            resources.add(new ResourceLocation("minecraft", "textures/block/pitcher_crop_bottom_stage_" + stage + ".png"));
        }
        resources.add(new ResourceLocation("minecraft", "textures/block/pitcher_crop_top_stage_3.png"));
        resources.add(new ResourceLocation("mynethersdelight", "blockstates/powdery_cane.json"));
        resources.add(new ResourceLocation("legumedelight", "lang/en_us.json"));
        resources.add(new ResourceLocation("fruitsdelight", "lang/en_us.json"));
        resources.add(new ResourceLocation("ambientadditions", "lang/en_us.json"));
        return Set.copyOf(resources);
    }

    @SubscribeEvent
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("aoemod_crop_visuals", Component.literal("AOE Crop Visuals"),
                    true, CropVisualOverridesPack::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta") ? () -> new ByteArrayInputStream(METADATA) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES || !RESOURCES.contains(id)) return null;
        // Read the owning mod file without resolving the dependency's same-named resource again.
        return () -> Files.newInputStream(ModList.get().getModFileById(AOEMod.MOD_ID).getFile()
                .findResource("assets", id.getNamespace(), id.getPath()));
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        for (ResourceLocation id : RESOURCES) {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path)) output.accept(id, getResource(type, id));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES
                ? Set.of("minecraft", "hearthandharvest", "mynethersdelight", "legumedelight", "fruitsdelight", "ambientadditions") : Set.of();
    }

    @Override
    public void close() { }
}
