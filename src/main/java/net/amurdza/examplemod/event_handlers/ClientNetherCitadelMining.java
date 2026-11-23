package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.MiningPressFeedback;
import net.amurdza.examplemod.worldgen.structure.NetherCitadelProtectionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cancel the protected mining swing while asking the server for one current explanation per attack press. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientNetherCitadelMining {
    private static final MiningPressFeedback PRESS = new MiningPressFeedback();
    private static Level feedbackLevel;

    private ClientNetherCitadelMining() { }

    public static boolean shouldExplainBlocked(Level level) {
        if (feedbackLevel != level) { PRESS.release(); feedbackLevel = level; }
        return PRESS.shouldExplainBlocked();
    }

    public static void recordAllowedCropHit(Level level, BlockPos pos) {
        if (feedbackLevel != level) { PRESS.release(); feedbackLevel = level; }
        if (!(level.getBlockState(pos).getBlock() instanceof ChestBlock)
                && NetherCitadelProtectionSnapshot.rewardsUnlocked(level, pos)) PRESS.harvested();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null || client.player.isCreative()
                || !GameplayDimensions.is(client.level, GameplayDimensions.NETHER)
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (!NetherCitadelProtectionSnapshot.protects(client.level, hit.getBlockPos())) {
            recordAllowedCropHit(client.level, hit.getBlockPos());
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        client.gameMode.stopDestroyBlock();
        if (shouldExplainBlocked(client.level)) client.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, hit.getBlockPos(), hit.getDirection()));
    }

    @SubscribeEvent
    public static void release(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (client.level != feedbackLevel || client.player == null || !client.options.keyAttack.isDown()) {
            PRESS.release();
            feedbackLevel = client.level;
        }
    }
}
