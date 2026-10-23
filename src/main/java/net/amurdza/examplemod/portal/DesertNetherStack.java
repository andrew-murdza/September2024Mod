package net.amurdza.examplemod.portal;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.List;

/** The custom desert floor and custom Nether ceiling share a persistent, unscaled seam. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class DesertNetherStack {
    public static final String PORTAL_TAG = "aoemod:desert_nether_stack";

    private DesertNetherStack() { }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        ensureConnected(event.getServer());
    }

    /** Reconcile only this mod's two portals. Unrelated global portals and stack presets are retained. */
    public static boolean ensureConnected(MinecraftServer server) {
        ServerLevel desert = server.getLevel(GameplayDimensions.DESERT);
        ServerLevel nether = server.getLevel(GameplayDimensions.NETHER);
        if (desert == null || nether == null) {
            AOEMod.LOGGER.error("Cannot connect desert/Nether stack: a custom dimension is unavailable");
            return false;
        }
        if (hasConflictingSeam(desert, nether, true) || hasConflictingSeam(nether, desert, false)) {
            AOEMod.LOGGER.error("Cannot install AoE desert/Nether seam: an unrelated global portal already occupies it; preserved the existing portal");
            return false;
        }
        ensureSide(desert, nether, true);
        ensureSide(nether, desert, false);
        return true;
    }

    private static boolean hasConflictingSeam(ServerLevel from, ServerLevel to, boolean floor) {
        double y = floor ? from.getMinBuildHeight() : from.getMaxBuildHeight();
        return GlobalPortalStorage.get(from).data.stream().anyMatch(portal ->
                portal instanceof VerticalConnectingPortal && !portal.getTags().contains(PORTAL_TAG)
                        && (portal.getNormal().y > 0) == floor && Math.abs(portal.getOriginPos().y - y) < 0.001
                        && !matches(portal, from, to, floor));
    }

    private static void ensureSide(ServerLevel from, ServerLevel to, boolean floor) {
        GlobalPortalStorage storage = GlobalPortalStorage.get(from);
        Portal retained = storage.data.stream().filter(portal -> matches(portal, from, to, floor)).findFirst().orElse(null);
        for (Portal existing : List.copyOf(storage.data)) {
            if (existing != retained && existing.getTags().contains(PORTAL_TAG)) storage.removePortal(existing);
        }
        if (retained != null) return;
        VerticalConnectingPortal portal = VerticalConnectingPortal.createConnectingPortal(from,
                floor ? VerticalConnectingPortal.ConnectorType.floor : VerticalConnectingPortal.ConnectorType.ceil,
                to, 1.0, false, 0,
                from.getMinBuildHeight(), from.getMaxBuildHeight(), to.getMinBuildHeight(), to.getMaxBuildHeight());
        // Match Immersive Portals' own stack rendering for a ceiling connection.
        portal.fuseView = !floor;
        portal.addTag(PORTAL_TAG);
        storage.addPortal(portal);
    }

    public static boolean matches(Portal portal, ServerLevel from, ServerLevel to, boolean floor) {
        return portal instanceof VerticalConnectingPortal && portal.level() == from
                && to.dimension().equals(portal.dimensionTo) && Math.abs(portal.scaling - 1.0) < 0.000001
                && DQuaternion.isClose(portal.getRotationD(), DQuaternion.identity, 0.000001)
                && (portal.getNormal().y > 0) == floor
                && portal.getOriginPos().distanceToSqr(new Vec3(0, floor ? from.getMinBuildHeight() : from.getMaxBuildHeight(), 0)) < 0.000001
                && portal.getDestPos().distanceToSqr(new Vec3(0, floor ? to.getMaxBuildHeight() : to.getMinBuildHeight(), 0)) < 0.000001;
    }
}
