package net.amurdza.examplemod.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Supplies a neutral copy of the installed vanilla animation at resource reload time.
 * Its frame geometry, alpha, order and timing are unchanged; the block tint supplies the destination color.
 * No Minecraft artwork is redistributed, and the vanilla Nether portal is never overridden.
 */
public final class PortalTexturePack extends AbstractPackResources {
    private static final ResourceLocation TEXTURE = AOEMod.makeID("textures/block/wooden_portal.png");
    private static final ResourceLocation ANIMATION = AOEMod.makeID("textures/block/wooden_portal.png.mcmeta");
    private static final ResourceLocation VANILLA_TEXTURE = new ResourceLocation("minecraft", "textures/block/nether_portal.png");
    private static final ResourceLocation VANILLA_ANIMATION = new ResourceLocation("minecraft", "textures/block/nether_portal.png.mcmeta");
    private static final byte[] METADATA = ("{\"pack\":{\"pack_format\":15,"
            + "\"description\":\"AOE destination portal colors\"}}").getBytes(StandardCharsets.UTF_8);
    private byte[] neutralTexture;

    private PortalTexturePack(String name) {
        super(name, true);
    }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("aoemod_portal_colors", Component.literal("AOE Portal Colors"), true,
                    PortalTexturePack::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta")
                ? () -> new ByteArrayInputStream(METADATA) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        if (id.equals(TEXTURE)) {
            return () -> new ByteArrayInputStream(neutralTexture());
        }
        if (id.equals(ANIMATION)) {
            return () -> {
                IoSupplier<InputStream> source = Minecraft.getInstance().getVanillaPackResources()
                        .getResource(PackType.CLIENT_RESOURCES, VANILLA_ANIMATION);
                if (source == null) {
                    throw new IOException("Vanilla Nether portal animation metadata is missing");
                }
                return source.get();
            };
        }
        return null;
    }

    private synchronized byte[] neutralTexture() throws IOException {
        if (neutralTexture == null) {
            IoSupplier<InputStream> source = Minecraft.getInstance().getVanillaPackResources()
                    .getResource(PackType.CLIENT_RESOURCES, VANILLA_TEXTURE);
            if (source == null) {
                throw new IOException("Vanilla Nether portal texture is missing");
            }
            try (InputStream input = source.get(); NativeImage image = NativeImage.read(input)) {
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixelRGBA(x, y);
                        int value = Math.max(rgba & 255, Math.max((rgba >> 8) & 255, (rgba >> 16) & 255));
                        image.setPixelRGBA(x, y, (rgba & 0xFF000000) | value * 0x010101);
                    }
                }
                neutralTexture = image.asByteArray();
            }
        }
        return neutralTexture;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type == PackType.CLIENT_RESOURCES && namespace.equals(AOEMod.MOD_ID)) {
            for (ResourceLocation id : new ResourceLocation[]{TEXTURE, ANIMATION}) {
                if (id.getPath().startsWith(path)) {
                    output.accept(id, getResource(type, id));
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(AOEMod.MOD_ID) : Set.of();
    }

    @Override
    public void close() {
        neutralTexture = null;
    }
}
