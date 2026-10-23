package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.portal.DesertNetherStack;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertNetherStackTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void stackIsUnscaledPersistentAndIdempotent(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var desert = server.getLevel(GameplayDimensions.DESERT);
        var nether = server.getLevel(GameplayDimensions.NETHER);
        var upper = GlobalPortalStorage.get(desert);
        var lower = GlobalPortalStorage.get(nether);
        var vanilla = GlobalPortalStorage.get(server.getLevel(Level.NETHER));
        var preservedUpper = List.copyOf(upper.data);
        var preservedLower = List.copyOf(lower.data);
        var preservedVanilla = List.copyOf(vanilla.data);
        helper.assertTrue(DesertNetherStack.ensureConnected(server), "The custom dimensions must acquire both seamless connections");
        int upperCount = upper.data.size(), lowerCount = lower.data.size();
        helper.assertTrue(DesertNetherStack.ensureConnected(server), "Reconciliation must accept an existing stack");
        helper.assertTrue(upper.data.size() == upperCount && lower.data.size() == lowerCount,
                "Repeated reconciliation must not duplicate global portals");
        helper.assertTrue(upper.data.containsAll(preservedUpper) && lower.data.containsAll(preservedLower)
                        && vanilla.data.equals(preservedVanilla), "Existing portals and the vanilla Nether must remain unchanged");
        var floor = upper.data.stream().filter(p -> DesertNetherStack.matches(p, desert, nether, true)).findFirst().orElseThrow();
        var ceiling = lower.data.stream().filter(p -> DesertNetherStack.matches(p, nether, desert, false)).findFirst().orElseThrow();
        helper.assertTrue(floor.getOriginPos().y == -32 && floor.getDestPos().y == 128,
                "The desert floor at -32 must meet the Nether ceiling at 128");
        Vec3 sample = new Vec3(37, -32, -51);
        helper.assertTrue(floor.transformPoint(sample).distanceToSqr(new Vec3(37, 128, -51)) < 0.000001,
                "Crossing the seam must retain exact X/Z coordinates at 1:1 scale");
        helper.assertTrue(ceiling.transformPoint(new Vec3(37, 128, -51)).distanceToSqr(sample) < 0.000001,
                "The reverse seam must undo the transform");
        var loaded = new GlobalPortalStorage(desert);
        loaded.fromNbt(upper.save(new CompoundTag()));
        helper.assertTrue(loaded.data.stream().anyMatch(p -> DesertNetherStack.matches(p, desert, nether, true)
                        && p.getTags().contains(DesertNetherStack.PORTAL_TAG)),
                "The seam geometry and ownership tag must survive the actual global-portal NBT round trip");
        helper.succeed();
    }
}
