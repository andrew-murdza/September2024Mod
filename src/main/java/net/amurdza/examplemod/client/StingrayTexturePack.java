package net.amurdza.examplemod.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StingrayTextureColors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

/** Derive the unspotted ray directly from the installed dependency's texture. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StingrayTexturePack extends AbstractPackResources {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ambientadditions", "textures/entity/blue_spotted_stingray.png");
    private static final byte[] METADATA = ("{\"pack\":{\"pack_format\":15,"
            + "\"description\":\"AOE unspotted stingray\"}}").getBytes(StandardCharsets.UTF_8);
    private byte[] texture;

    private StingrayTexturePack(String name) { super(name, true); }

    @SubscribeEvent
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("aoemod_unspotted_stingray", Component.literal("AOE Unspotted Stingray"),
                    true, StingrayTexturePack::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta") ? () -> new ByteArrayInputStream(METADATA) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        return type == PackType.CLIENT_RESOURCES && id.equals(TEXTURE) ? () -> new ByteArrayInputStream(texture()) : null;
    }

    private synchronized byte[] texture() throws IOException {
        if (texture != null) return texture;
        var path = ModList.get().getModFileById("ambientadditions").getFile().findResource(
                "assets", "ambientadditions", "textures", "entity", "blue_spotted_stingray.png");
        try (var input = Files.newInputStream(path); NativeImage image = NativeImage.read(input)) {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int argb = FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr),
                        FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr));
                int corrected = StingrayTextureColors.withoutSpot(x, y, argb);
                if (corrected != argb) image.setPixelRGBA(x, y, FastColor.ABGR32.color(FastColor.ARGB32.alpha(corrected),
                        FastColor.ARGB32.blue(corrected), FastColor.ARGB32.green(corrected), FastColor.ARGB32.red(corrected)));
            }
            texture = image.asByteArray();
            return texture;
        }
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type == PackType.CLIENT_RESOURCES && namespace.equals(TEXTURE.getNamespace())
                && TEXTURE.getPath().startsWith(path)) output.accept(TEXTURE, getResource(type, TEXTURE));
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(TEXTURE.getNamespace()) : Set.of();
    }

    @Override
    public void close() { texture = null; }
}
