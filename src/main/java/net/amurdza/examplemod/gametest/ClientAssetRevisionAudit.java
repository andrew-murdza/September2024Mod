package net.amurdza.examplemod.gametest;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.duke.orcz.client.model.ModelOrc_big_savage_2sword;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.snow.client.SnowClient;
import twilightforest.client.model.entity.UpperGoblinKnightModel;
import twilightforest.client.model.entity.newmodels.NewUpperGoblinKnightModel;
import twilightforest.init.TFEntities;
import twilightforest.entity.monster.UpperGoblinKnight;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Opt-in client audit: checks the actual reloaded renderers, atlases and Snow!RealMagic bindings. */
public final class ClientAssetRevisionAudit {
    private static final String OUTPUT = System.getProperty("aoemod.assetAudit.report");
    private static final Map<String, ResourceLocation> ORC_TEXTURES = new LinkedHashMap<>();
    private static boolean completed;

    private ClientAssetRevisionAudit() { }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Renderers {
        @SubscribeEvent
        public static void loaded(EntityRenderersEvent.AddLayers event) {
            if (OUTPUT == null) return;
            for (String id : List.of("orcsavagemace", "orcshield", "bigorcboss")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("orcz", id));
                var renderer = event.getEntityRenderer(type);
                ORC_TEXTURES.put(id, renderer == null ? null : renderer.getTextureLocation(null));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            var minecraft = Minecraft.getInstance();
            if (OUTPUT == null || completed || event.phase != TickEvent.Phase.END
                    || ORC_TEXTURES.size() != 3 || minecraft.getOverlay() != null) return;
            if (Boolean.getBoolean("aoemod.assetAudit.world")
                    && (minecraft.level == null || minecraft.player == null
                    || !minecraft.gameRenderer.getMainCamera().isInitialized())) return;
            completed = true;
            Map<String, Object> report = new LinkedHashMap<>();
            List<String> checks = new ArrayList<>();
            try {
                for (var entry : ORC_TEXTURES.entrySet()) {
                    String file = switch (entry.getKey()) {
                        case "orcshield" -> "orc_savage_shield";
                        case "bigorcboss" -> "orc_savage_big_2sword";
                        default -> "orc_savage";
                    };
                    var expected = new ResourceLocation("aoemod", "textures/entity/orcz/" + file + ".png");
                    require(expected.equals(entry.getValue()), "Wrong live renderer texture: " + entry);
                    var resource = minecraft.getResourceManager().getResourceOrThrow(expected);
                    byte[] bytes;
                    try (var stream = resource.open()) { bytes = stream.readAllBytes(); }
                    try (var image = NativeImage.read(new java.io.ByteArrayInputStream(bytes))) {
                        require(image.getWidth() == 256 && image.getHeight() == 256, "Orc atlas dimensions changed");
                        int bone = 0;
                        boolean boss = entry.getKey().equals("bigorcboss");
                        for (int y = boss ? 0 : 45; y < (boss ? 256 : 75); y++) for (int x = boss ? 0 : 88; x < (boss ? 256 : 132); x++) {
                            if (boss && !(x >= 160 && x < 224 && y >= 160 && y < 196
                                    || x >= 38 && x < 86 && y >= 90 && y < 118)) continue;
                            int pixel = image.getPixelRGBA(x, y);
                            if (FastColor.ABGR32.alpha(pixel) != 0 && FastColor.ABGR32.red(pixel) >= 176
                                    && FastColor.ABGR32.green(pixel) >= 161 && FastColor.ABGR32.blue(pixel) >= 124) bone++;
                        }
                        require(bone >= (boss ? 1151 : entry.getKey().equals("orcshield") ? 272 : 512), "Live tool atlas is still dark: " + file);
                    }
                    checks.add(expected + " pack=" + resource.sourcePackId() + " sha256="
                            + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
                }
                auditBossSwordUvs(checks);
                if (Boolean.getBoolean("aoemod.assetAudit.world")) {
                    auditGoblinShield(minecraft, checks);
                    November15AppearanceAudit.audit(minecraft, checks);
                }
                java.util.Set<Integer> berryPalette = new java.util.HashSet<>();
                try (var stream = minecraft.getResourceManager().getResourceOrThrow(
                        new ResourceLocation("fruitsdelight", "textures/item/blueberry.png")).open(); var fruit = NativeImage.read(stream)) {
                    for (int y = 0; y < fruit.getHeight(); y++) for (int x = 0; x < fruit.getWidth(); x++) {
                        int p = fruit.getPixelRGBA(x, y);
                        if (FastColor.ABGR32.alpha(p) > 0 && FastColor.ABGR32.blue(p) > FastColor.ABGR32.red(p)
                                && FastColor.ABGR32.blue(p) > FastColor.ABGR32.green(p)) berryPalette.add(p);
                    }
                }
                require(berryPalette.size() == 5, "Unexpected native Fruits Delight berry palette");
                var models = minecraft.getModelManager();
                for (int stage = 0; stage < 4; stage++) {
                    var normal = new ResourceLocation("aoemod", "block/blue_berry_bush_stage" + stage);
                    var snowy = new ResourceLocation("aoemod", "block/snowy_blue_berry_bush_stage" + stage);
                    var state = ModBlocks.BLUE_BERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, stage);
                    var baked = minecraft.getBlockRenderer().getBlockModel(state);
                    require(baked != models.getMissingModel() && baked.getParticleIcon().contents().name().equals(normal),
                            "Normal blueberry stage uses the wrong atlas sprite: " + stage);
                    var mapping = SnowClient.snowVariantMapping.get(normal);
                    require(mapping != null && snowy.equals(mapping.model), "Snow replacement mapping is missing for stage " + stage);
                    var snowModel = models.getModel(snowy);
                    require(snowModel != models.getMissingModel() && snowModel.getParticleIcon().contents().name().equals(snowy),
                            "Snow blueberry model was not baked with its own texture: " + stage);
                    for (var sprite : List.of(normal, snowy)) {
                        var texture = new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
                        var source = new ResourceLocation(sprite.equals(snowy) ? "snowrealmagic" : "minecraft",
                                "textures/block/" + (sprite.equals(snowy) ? "snowy_" : "") + "sweet_berry_bush_stage" + stage + ".png");
                        try (var stream = minecraft.getResourceManager().getResourceOrThrow(texture).open(); var image = NativeImage.read(stream);
                             var sourceStream = minecraft.getResourceManager().getResourceOrThrow(source).open(); var original = NativeImage.read(sourceStream)) {
                            require(image.getWidth() == original.getWidth() && image.getHeight() == original.getHeight(), "Berry atlas dimensions changed");
                            int berries = 0;
                            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                                int pixel = image.getPixelRGBA(x, y);
                                int before = original.getPixelRGBA(x, y);
                                int r = FastColor.ABGR32.red(before), g = FastColor.ABGR32.green(before), b = FastColor.ABGR32.blue(before);
                                boolean berry = FastColor.ABGR32.alpha(before) > 0 && r > g * 1.5 && r > b * 1.5;
                                if (berry) {
                                    require(berryPalette.contains(pixel), "Blueberry pixel differs from Fruits Delight fruit: " + sprite);
                                    berries++;
                                } else require(pixel == before, "Non-berry leaf, snow or alpha pixel changed: " + sprite);
                            }
                            require(stage < 2 ? berries == 0 : berries > 0, "Incorrect berry color/stage: " + sprite);
                        }
                    }
                    checks.add("Baked normal + snowlogged blueberry stage " + stage);
                }
                report.put("status", "passed");
            } catch (Throwable failure) {
                report.put("status", "failed");
                report.put("failure", failure.toString());
                AOEMod.LOGGER.error("Client asset audit failed", failure);
            }
            report.put("checks", checks);
            try {
                Path path = Path.of(OUTPUT).toAbsolutePath();
                Files.createDirectories(path.getParent());
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
                AOEMod.LOGGER.info("Client asset audit: {}", report);
            } catch (Exception failure) { AOEMod.LOGGER.error("Cannot write client asset audit", failure); }
            if (Boolean.getBoolean("aoemod.assetAudit.exit")) minecraft.stop();
        }
    }

    static void auditGoblinShield(Minecraft minecraft, List<String> checks) {
        var knight = TFEntities.UPPER_GOBLIN_KNIGHT.get().create(minecraft.level);
        require(knight != null, "Cannot construct Goblin Knight for the model audit");
        knight.setHasShield(true);
        for (HumanoidModel<UpperGoblinKnight> model : List.of(
                new UpperGoblinKnightModel(UpperGoblinKnightModel.create().bakeRoot()),
                new NewUpperGoblinKnightModel(NewUpperGoblinKnightModel.create().bakeRoot()))) {
            var shield = model.leftArm.getChild("shield");
            for (float frame : new float[]{0, 1.5F, 4.5F, 20}) {
                model.setupAnim(knight, frame, .6F, frame, 25, -10);
                require(shield.visible && shield.x == 0 && shield.y == 12 && shield.z == 0,
                        "The original Twilight Forest shield must remain on its native arm socket");
            }
        }
        checks.add("Both Twilight Forest models retain the original shield socket and visible shield.");
        require(minecraft.getEntityRenderDispatcher().getRenderer(knight)
                        instanceof net.amurdza.examplemod.entity.render.GoblinKnightRenderer,
                "The merged knight must delegate to complete native living renderers");
        var renderer = (net.amurdza.examplemod.entity.render.GoblinKnightRenderer) minecraft.getEntityRenderDispatcher().getRenderer(knight);
        var appearance = renderer.appearance;
        boolean jappa = twilightforest.client.JappaPackReloadListener.INSTANCE.uncachedJappaPackCheck().getAsBoolean();
        require(appearance.jappa == jappa && (jappa
                        ? renderer.upperRenderer instanceof twilightforest.client.renderer.entity.newmodels.NewUpperGoblinKnightRenderer
                        : renderer.upperRenderer instanceof twilightforest.client.renderer.entity.UpperGoblinKnightRenderer),
                "The full native upper renderer and model family must match the active texture pack");
        String actualName = knight.getDisplayName().getString();
        float actualHeight = knight.getBbHeight();
        checks.add("Live Goblin Knight identity: name=" + actualName + ", height=" + actualHeight
                + ", nativeTranslation=" + net.minecraft.network.chat.Component.translatable("entity.twilightforest.upper_goblin_knight").getString());
        require(actualName.equals("Goblin Knight"), "Complete knight display name must be Goblin Knight; actual=" + actualName);
        require(actualHeight >= 2.6, "Complete knight collision height must encompass the body; actual=" + actualHeight);
        knight.moveTo(0, 0, 0, 0, 0);
        knight.yBodyRot = knight.yBodyRotO = knight.yHeadRot = knight.yHeadRotO = 0;
        knight.tickCount = 27;
        var idleVertices = new UvBounds();
        renderer.render(knight, 0, 0, new PoseStack(), type -> idleVertices, 15728880);
        var pair = renderer.visualPair(knight);
        require(!renderer.upperRenderer.getModel().young && !renderer.lowerRenderer.getModel().young
                        && renderer.upperRenderer.getModel().riding && !renderer.lowerRenderer.getModel().riding
                        && idleVertices.maxY > 2.3F,
                "Actual native render passes must initialize adult/rider state and emit the full raised helmet");
        require(pair.upper().getVehicle() == pair.lower() && pair.lower().getPassengers().size() == 1
                        && Math.abs(pair.upper().getY() - pair.lower().getY() - 1) < .0001
                        && minecraft.level.getEntity(pair.lower().getId()) == null && minecraft.level.getEntity(pair.upper().getId()) == null
                        && !pair.lower().hasCustomName() && !pair.upper().hasCustomName()
                        && pair.lower().getUUID().equals(knight.getUUID()) && pair.upper().getUUID().equals(knight.getUUID())
                        && pair.lower().getScoreboardName().equals(knight.getScoreboardName())
                        && pair.upper().getScoreboardName().equals(knight.getScoreboardName())
                        && !knight.isPassenger() && !knight.isVehicle(),
                "Untracked visual proxies must preserve the real scoreboard identity; the real mob remains one entity/nameplate");
        require(!appearance.lower.getChild("head").visible && !appearance.lower.getChild("right_arm").visible
                        && !appearance.upper.getChild("right_leg").visible && appearance.upper.getChild("hat").visible
                        && appearance.upper.getChild("right_arm").visible && appearance.upper.getChild("left_arm").visible,
                "The complete native helmet, weapon/shield arms and walking legs must remain; only duplicate anatomy is hidden");
        knight.deathTime = 20;
        var deathVertices = new UvBounds();
        renderer.render(knight, 0, 0, new PoseStack(), type -> deathVertices, 15728880);
        require(deathVertices.vertices == idleVertices.vertices
                        && Math.abs(deathVertices.minX - idleVertices.minY) < .002
                        && Math.abs(deathVertices.maxX - idleVertices.maxY) < .002
                        && Math.abs(deathVertices.minY + idleVertices.maxX) < .002
                        && Math.abs(deathVertices.maxY + idleVertices.minX) < .002,
                "Death must rotate the complete native geometry around one shared pivot without separating the two visual passes");
        knight.deathTime = 0;
        knight.walkAnimation.update(.7F, 1);
        knight.hurtTime = 3;
        knight.heavySpearTimer = 30;
        pair = renderer.visualPair(knight);
        require(pair.upper().hurtTime == 3 && pair.lower().hurtTime == 3 && pair.upper().heavySpearTimer == 30,
                "Both visual passes must share damage state while the native upper retains its spear animation");
        for (float partial : new float[]{0, .5F, 1}) require(
                Math.abs(pair.lower().walkAnimation.position(partial) - knight.walkAnimation.position(partial)) < .0001
                        && Math.abs(pair.lower().walkAnimation.speed(partial) - knight.walkAnimation.speed(partial)) < .0001,
                "Native walking interpolation must copy exactly without advancing with render frames");
        try (var stream = minecraft.getResourceManager().getResourceOrThrow(renderer.getTextureLocation(knight)).open();
             var texture = NativeImage.read(stream)) {
            var coverage = new QuadTextureCoverage(texture);
            appearance.lower.getChild("right_leg").render(new PoseStack(), coverage, 0, 0);
            appearance.lower.getChild("left_leg").render(new PoseStack(), coverage, 0, 0);
            require(coverage.faces == 12 && coverage.opaqueFaces == 12,
                    "Every native boot/leg face must sample an opaque live atlas pixel");
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot audit the live knight atlas", failure); }
        for (var part : List.of(appearance.breastplate, appearance.tunic)) {
            var backUv = new UvBounds();
            part.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (index == 1) cube.compile(pose, backUv, 0, 0, 1, 1, 1, 1);
            });
            boolean plate = part == appearance.breastplate;
            require(backUv.vertices == 4 && backUv.minU == 70F / 128
                            && backUv.maxU == (plate ? 83F : 82F) / 128
                            && backUv.minV == (plate ? 6F : 25F) / 64
                            && backUv.maxV == (plate ? 18F : 34F) / 64,
                    "Both rear armor faces must reuse intact native front material");
        }
        var yak = net.amurdza.examplemod.registry.ModEntities.YAK.get().create(minecraft.level);
        require(minecraft.getEntityRenderDispatcher().getRenderer(yak)
                        instanceof net.amurdza.examplemod.entity.render.YakRenderer,
                "The dedicated horse-based yak must retain its shaggy cold-cow model");
        var yakRenderer = (net.amurdza.examplemod.entity.render.YakRenderer) minecraft.getEntityRenderDispatcher().getRenderer(yak);
        var saddleVertices = new UvBounds();
        net.minecraft.client.renderer.MultiBufferSource saddleBuffer = type -> saddleVertices;
        yakRenderer.saddleLayer().render(new PoseStack(), saddleBuffer, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(saddleVertices.vertices == 0, "An unsaddled yak must not show tack");
        // Horse inventories change equipment on the server; clients receive the
        // native flag byte. Exercise that same data-packet path here. Taming
        // identifies the native horse flag accessor without reflection or an ID.
        yak.getEntityData().packDirty();
        yak.setTamed(true);
        var flags = yak.getEntityData().packDirty();
        require(flags != null && flags.size() == 1
                        && flags.get(0).serializer() == net.minecraft.network.syncher.EntityDataSerializers.BYTE
                        && flags.get(0).value().equals((byte) 2), "Native horse taming must identify its flag byte");
        yak.getEntityData().assignValues(List.of(new net.minecraft.network.syncher.SynchedEntityData.DataValue<>(
                flags.get(0).id(), net.minecraft.network.syncher.EntityDataSerializers.BYTE, (byte) 6)));
        require(yak.isSaddled(), "The live yak must receive its native synchronized saddle flag");
        yakRenderer.saddleLayer().render(new PoseStack(), saddleBuffer, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(saddleVertices.vertices == 24 && saddleVertices.minY < 1.5F / 16
                        && saddleVertices.maxX > 6.5F / 16
                        && saddleVertices.minU >= 26F / 64 && saddleVertices.maxU <= 1
                        && saddleVertices.minV >= 0 && saddleVertices.maxV <= 18F / 64,
                "The visible saddle must keep native donkey UVs and sit outside the yak's fur");
        require(minecraft.getResourceManager().getResource(net.amurdza.examplemod.entity.render.YakSaddleLayer.TEXTURE).isPresent(),
                "The native donkey saddle texture must be available in the live resource pack");
        yak.getEntityData().assignValues(flags);
        yakRenderer.saddleLayer().render(new PoseStack(), saddleBuffer, 15728880, yak, 0, 0, 0, 0, 0, 0);
        require(saddleVertices.vertices == 24, "Removing the saddle must hide its rendered mesh immediately");
        checks.add("Goblin Knight: restored native " + (jappa ? "Jappa" : "classic")
                + " renderer delegates retain adult helmet/arms, rider pose, opaque legs and a shared death pivot; one real saved entity.");
        checks.add("Native yak saddle appears only when equipped.");
        knight.discard();
        yak.discard();
    }

    private static void auditBossSwordUvs(List<String> checks) {
        var root = ModelOrc_big_savage_2sword.createBodyLayer().bakeRoot();
        int[] relocated = {0}, unchanged = {0};
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            var uv = new UvBounds();
            cube.compile(pose, uv, 0, 0, 1, 1, 1, 1);
            boolean sword = path.equals("/RightArm") && (index == 2 || index == 3)
                    || path.equals("/LeftArm/RightArm_r1") && index == 0;
            if (sword) {
                require(uv.minU >= 80F / 128 && uv.maxU <= 112F / 128
                                && uv.minV >= 80F / 128 && uv.maxV <= 98F / 128,
                        "Boss sword still samples the shared native head UV: " + path + "/" + index);
                relocated[0]++;
            } else {
                require(uv.maxV < 80F / 128, "A non-sword boss cuboid moved into the copied UV island: " + path + "/" + index);
                unchanged[0]++;
            }
        });
        require(relocated[0] == 3 && unchanged[0] == 29, "Boss model cube inventory changed");
        checks.add("Boss model retains all 32 native cuboids; only three shared sword UVs relocate to (80,80)");
    }

    private static final class UvBounds implements VertexConsumer {
        private int vertices;
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        private float minU = Float.POSITIVE_INFINITY, minV = Float.POSITIVE_INFINITY;
        private float maxU = Float.NEGATIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
        @Override public VertexConsumer uv(float u, float v) {
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, v); maxV = Math.max(maxV, v);
            return this;
        }
        @Override public VertexConsumer vertex(double x, double y, double z) {
            vertices++;
            minX = Math.min(minX, (float) x); minY = Math.min(minY, (float) y);
            maxX = Math.max(maxX, (float) x); maxY = Math.max(maxY, (float) y);
            return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }

    private static final class QuadTextureCoverage implements VertexConsumer {
        private final NativeImage texture;
        private int vertices, faces, opaqueFaces;
        private float u, v;
        private QuadTextureCoverage(NativeImage texture) { this.texture = texture; }
        @Override public VertexConsumer uv(float u, float v) { this.u += u; this.v += v; return this; }
        @Override public void endVertex() {
            if (++vertices % 4 != 0) return;
            int x = Math.min(texture.getWidth() - 1, Math.max(0, (int) (u * texture.getWidth() / 4)));
            int y = Math.min(texture.getHeight() - 1, Math.max(0, (int) (v * texture.getHeight() / 4)));
            faces++;
            if (FastColor.ABGR32.alpha(texture.getPixelRGBA(x, y)) == 255) opaqueFaces++;
            u = v = 0;
        }
        @Override public VertexConsumer vertex(double x, double y, double z) { return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
