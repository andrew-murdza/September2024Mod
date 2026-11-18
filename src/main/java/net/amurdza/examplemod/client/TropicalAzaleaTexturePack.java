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
import net.minecraft.util.FastColor;
import net.minecraftforge.event.AddPackFindersEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Pixel-preserving palette correction for the two tropical azalea models, leaving flowers untinted. */
public final class TropicalAzaleaTexturePack extends AbstractPackResources {
    private static final byte[] METADATA = ("{\"pack\":{\"pack_format\":15,"
            + "\"description\":\"AOE tropical azalea foliage colors\"}}").getBytes(StandardCharsets.UTF_8);
    private static final Set<String> LEAVES = Set.of("azalea_leaves", "flowering_azalea_leaves");
    private final Map<String, byte[]> textures = new HashMap<>();

    private TropicalAzaleaTexturePack(String name) { super(name, true); }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("aoemod_tropical_azalea_colors", Component.literal("AOE Tropical Azalea Colors"),
                    true, TropicalAzaleaTexturePack::new, PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) consumer.accept(pack);
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta") ? () -> new ByteArrayInputStream(METADATA) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES || !id.getNamespace().equals(AOEMod.MOD_ID)) return null;
        for (String leaf : LEAVES) {
            if (id.getPath().equals("textures/block/tropical_" + leaf + ".png")) {
                return () -> new ByteArrayInputStream(texture(leaf));
            }
        }
        return null;
    }

    private synchronized byte[] texture(String leaf) throws IOException {
        if (textures.containsKey(leaf)) return textures.get(leaf);
        var vanilla = Minecraft.getInstance().getVanillaPackResources();
        var source = vanilla.getResource(PackType.CLIENT_RESOURCES,
                new ResourceLocation("minecraft", "textures/block/" + leaf + ".png"));
        var colormap = vanilla.getResource(PackType.CLIENT_RESOURCES,
                new ResourceLocation("minecraft", "textures/colormap/foliage.png"));
        if (source == null || colormap == null) throw new IOException("Missing installed azalea foliage resources");
        try (var input = source.get(); var colors = colormap.get();
             NativeImage image = NativeImage.read(input); NativeImage foliage = NativeImage.read(colors)) {
            // Temperature=1 and humidity=1 select this same pixel for the rainforest.
            int target = foliage.getPixelRGBA(0, 0);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int r = FastColor.ABGR32.red(pixel), g = FastColor.ABGR32.green(pixel), b = FastColor.ABGR32.blue(pixel);
                if (FastColor.ABGR32.alpha(pixel) == 0 || g < r || g < b) continue;
                int brightness = Math.min(255, Math.round(Math.max(r, Math.max(g, b)) * 1.5f));
                // NativeImage uses ABGR, whereas biome colors use RGB. Named channel
                // accessors avoid red/blue packing mistakes and protect pink flowers.
                image.setPixelRGBA(x, y, FastColor.ABGR32.color(FastColor.ABGR32.alpha(pixel),
                        brightness * FastColor.ABGR32.blue(target) / 255,
                        brightness * FastColor.ABGR32.green(target) / 255,
                        brightness * FastColor.ABGR32.red(target) / 255));
            }
            byte[] result = image.asByteArray();
            textures.put(leaf, result);
            return result;
        }
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals(AOEMod.MOD_ID)) return;
        for (String leaf : LEAVES) {
            ResourceLocation id = AOEMod.makeID("textures/block/tropical_" + leaf + ".png");
            if (id.getPath().startsWith(path)) output.accept(id, getResource(type, id));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) { return type == PackType.CLIENT_RESOURCES ? Set.of(AOEMod.MOD_ID) : Set.of(); }

    @Override
    public void close() { textures.clear(); }
}
