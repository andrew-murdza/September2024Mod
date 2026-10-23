package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;

import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherHoglinTests {
    private NetherHoglinTests() { }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void warpedConversionPreservesImmunityAndNoAi(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var hoglin = EntityType.HOGLIN.create(nether);
        hoglin.setPos(8.5, 40, 8.5);
        helper.assertTrue(GameplayDimensions.biomeIs(nether, hoglin.blockPosition(), "crimson_forest"), "Expected crimson test position");
        helper.assertTrue(!hoglin.isConverting(), "Custom crimson forest must remain safe for hoglins");
        hoglin.setPos(8.5, 72, 8.5);
        helper.assertTrue(GameplayDimensions.biomeIs(nether, hoglin.blockPosition(), "warped_forest"), "Expected warped test position");
        helper.assertTrue(hoglin.isConverting(), "Custom warped forest must start vanilla hoglin conversion");
        hoglin.setImmuneToZombification(true);
        helper.assertTrue(!hoglin.isConverting(), "Warped conversion ignored explicit immunity");
        hoglin.setImmuneToZombification(false);
        hoglin.setNoAi(true);
        helper.assertTrue(!hoglin.isConverting(), "Warped conversion ignored NoAI");
        hoglin.setNoAi(false);
        helper.assertTrue(hoglin.isConverting(), "Removing immunity and NoAI did not restore conversion");
        var nativeNether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(!EntityType.HOGLIN.create(nativeNether).isConverting()
                        && EntityType.HOGLIN.create(helper.getLevel()).isConverting(),
                "Custom warped conversion changed native Nether or Overworld behavior");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void hoglinsCannotLeaveThroughVanillaOrSeamPortals(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var hoglin = EntityType.HOGLIN.create(nether);
        hoglin.setPos(8.5, 40, 8.5);
        helper.assertTrue(!ForgeHooks.onTravelToDimension(hoglin, desert.dimension())
                        && hoglin.changeDimension(desert) == null && !hoglin.isRemoved(),
                "A custom Nether hoglin escaped through ordinary dimension travel");
        var seam = VerticalConnectingPortal.createConnectingPortal(nether, VerticalConnectingPortal.ConnectorType.ceil,
                desert, 1, false, 0, nether.getMinBuildHeight(), nether.getMaxBuildHeight(),
                desert.getMinBuildHeight(), desert.getMaxBuildHeight());
        helper.assertTrue(!seam.canTeleportEntity(hoglin), "The actual Immersive Portals seam bypassed hoglin containment");
        helper.assertTrue(ForgeHooks.onTravelToDimension(hoglin, nether.dimension())
                        && seam.canTeleportEntity(EntityType.PIG.create(nether)),
                "Hoglin containment blocked local travel or an unrelated animal");
        var nativeHoglin = EntityType.HOGLIN.create(helper.getLevel().getServer().getLevel(Level.NETHER));
        helper.assertTrue(ForgeHooks.onTravelToDimension(nativeHoglin, helper.getLevel().dimension()),
                "Hoglin containment leaked into the vanilla Nether");
        BlockPos destination = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(hoglin.teleportTo(helper.getLevel(), destination.getX() + .5, destination.getY(),
                        destination.getZ() + .5, Set.of(), 0, 0),
                "Explicit administrative relocation should remain available");
        var moved = helper.getLevel().getEntity(hoglin.getUUID());
        helper.assertTrue(moved != null, "Administrative relocation did not preserve the hoglin");
        moved.discard();
        helper.succeed();
    }
}
