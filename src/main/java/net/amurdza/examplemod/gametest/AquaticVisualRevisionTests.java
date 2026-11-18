package net.amurdza.examplemod.gametest;

import codyhuh.ambientadditions.common.entities.BlueSpottedStingray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.StingrayTextureColors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticVisualRevisionTests {
    private AquaticVisualRevisionTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void stingraySpotsDisappearWithoutChangingOtherTexelsOrTransparency(GameTestHelper helper) throws Exception {
        var path = ModList.get().getModFileById("ambientadditions").getFile().findResource(
                "assets", "ambientadditions", "textures", "entity", "blue_spotted_stingray.png");
        int changed = 0, eyes = 0;
        try (var input = Files.newInputStream(path)) {
            var image = ImageIO.read(input);
            helper.assertTrue(image != null && image.getWidth() == 64 && image.getHeight() == 64,
                    "The palette repair must use the installed native stingray atlas");
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int before = image.getRGB(x, y), after = StingrayTextureColors.withoutSpot(x, y, before);
                helper.assertFalse(StingrayTextureColors.isSpot(after), "Every blue spot and its colored rim must disappear");
                helper.assertTrue((before >>> 24) == (after >>> 24), "The native ray silhouette cannot change");
                if (StingrayTextureColors.isSpot(before)) changed++;
                else helper.assertTrue(before == after, "Non-spot shading, eyes and underside must remain byte-for-byte intact");
                if (before == 0xFF23211A) eyes++;
            }
        }
        helper.assertTrue(changed == 46 && eyes == 4, "Repair all 23 spot pixels and 23 rim pixels while preserving all eye pixels");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void bullKelpRestoresTheCompleteNativeModelAndAnimation(GameTestHelper helper) throws Exception {
        var nativeFile = ModList.get().getModFileById("hybrid_aquatic").getFile();
        var override = ModList.get().getModFileById(AOEMod.MOD_ID).getFile().findResource(
                "assets", "hybrid_aquatic", "models", "block", "bull_kelp.json");
        var original = nativeFile.findResource("assets", "hybrid_aquatic", "models", "block", "bull_kelp.json");
        helper.assertTrue(java.util.Arrays.equals(Files.readAllBytes(override), Files.readAllBytes(original)),
                "Bull kelp must restore the exact native cross model, including the original top texels");
        var nativeAtlas = nativeFile.findResource("assets", "hybrid_aquatic", "textures", "block", "bull_kelp.png");
        try (var stream = Files.newInputStream(nativeAtlas)) {
            var image = ImageIO.read(stream);
            helper.assertTrue(image.getWidth() == 16 && image.getHeight() == 320, "Preserve all twenty native animation frames");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyFiddlerVariantHasWarmUndersideAndRedPurpleShellsMatchLegs(GameTestHelper helper) throws Exception {
        var originalMod = ModList.get().getModFileById("hybrid_aquatic").getFile();
        var ours = ModList.get().getModFileById(AOEMod.MOD_ID).getFile();
        var legPalette = java.util.Set.of(0xFFFF9547, 0xFFFFA65C, 0xFFFFB570, 0xFFFFCD94, 0xFFFFE2B7);
        for (String variant : java.util.List.of("blue", "purple", "red")) {
            String[] path = {"assets", "hybrid_aquatic", "textures", "entity", "crustacean", "fiddler_crab", "fiddler_crab_" + variant + ".png"};
            String[] repairedPath = path.clone();
            repairedPath[1] = AOEMod.MOD_ID;
            try (var nativeInput = Files.newInputStream(originalMod.findResource(path));
                 var changedInput = Files.newInputStream(ours.findResource(repairedPath))) {
                var before = ImageIO.read(nativeInput); var after = ImageIO.read(changedInput);
                helper.assertTrue(after.getWidth() == 16 && after.getHeight() == 16, "Preserve native atlas dimensions");
                int changed = 0;
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    int old = before.getRGB(x, y), color = after.getRGB(x, y);
                    helper.assertTrue((old >>> 24) == (color >>> 24), "Preserve every native transparent texel");
                    if (old != color) { changed++; helper.assertTrue(legPalette.contains(color), "Every recolored shell texel must use an existing leg color"); }
                    if (x >= 10 && x < 14 && y >= 5 && y < 8) helper.assertTrue(old == color, "Keep the exact native leg UVs");
                }
                helper.assertTrue(changed == (variant.equals("blue") ? 25 : 62), "Repair every underside and both reddish shell variants");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void nativeStingrayCanNavigateUpAndDownThroughOpenWater(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 7; y++) helper.setBlock(new BlockPos(x, y, z),
                    x == 0 || x == 8 || z == 0 || z == 8 ? Blocks.GLASS : Blocks.WATER);
        }
        var ray = (BlueSpottedStingray) ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation("ambientadditions", "blue_spotted_stingray")).create(helper.getLevel());
        helper.assertTrue(ray.getNavigation() instanceof WaterBoundPathNavigation
                        && ray.getMoveControl() instanceof SmoothSwimmingMoveControl,
                "The native stingray must use water pathfinding and a swimming movement controller");
        helper.assertTrue(ForgeRegistries.ITEMS.getKey(ray.getBucketItemStack().getItem())
                        .equals(new ResourceLocation("ambientadditions", "blue_spotted_stingray_bucket")),
                "Keep the same native stingray entity and bucket");
        // Remove random goal arbitration only in the fixture; exercise the real
        // navigation, movement controller, fish travel, water and block collision.
        ray.goalSelector.removeAllGoals(goal -> true);
        ray.targetSelector.removeAllGoals(goal -> true);
        BlockPos start = helper.absolutePos(new BlockPos(2, 1, 4));
        ray.moveTo(start.getX() + .5, start.getY() + .05, start.getZ() + .5, 0, 0);
        ray.baseTick();
        double floorY = ray.getY();
        BlockPos upper = helper.absolutePos(new BlockPos(5, 5, 4));
        try {
            helper.assertTrue(ray.isInWater(), "The native stingray fixture must be submerged");
            helper.assertTrue(ray.getNavigation().moveTo(upper.getX() + .5, upper.getY() + .5, upper.getZ() + .5, 1),
                    "Water navigation must find an elevated destination without a ground walkway");
            for (int tick = 0; tick < 240; tick++) ray.tick();
            double upperY = ray.getY();
            helper.assertTrue(upperY > floorY + 2, "The stingray must swim away from the bottom, actual rise=" + (upperY - floorY));
            ray.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(ray.getNavigation().moveTo(start.getX() + .5, floorY + .5, start.getZ() + .5, 1),
                    "Water navigation must also support descending");
            for (int tick = 0; tick < 240; tick++) ray.tick();
            helper.assertTrue(ray.getY() < upperY - 1.5, "The stingray must descend again rather than floating permanently upward");
        } finally {
            ray.discard();
        }
        helper.succeed();
    }
}
