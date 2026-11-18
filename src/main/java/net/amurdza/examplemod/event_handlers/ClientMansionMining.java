package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.MansionProtectionSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

/** Stop the hand swing as well as block prediction, while retaining one explanation per held attack. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, value = Dist.CLIENT)
public final class ClientMansionMining {
    private static ClientLevel feedbackLevel;
    private static boolean explainedPress;

    private ClientMansionMining() { }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null || client.player.isCreative()
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(MansionProtectionSnapshot.protects(client.level, hit.getBlockPos())
                || net.amurdza.examplemod.worldgen.structure.SavannaCampProtectionSnapshot.protects(client.level, hit.getBlockPos()))) return;
        event.setSwingHand(false);
        event.setCanceled(true);
        client.gameMode.stopDestroyBlock();
        if (feedbackLevel != client.level || !explainedPress) {
            client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    hit.getBlockPos(), hit.getDirection()));
            feedbackLevel = client.level;
            explainedPress = true;
        }
    }

    @SubscribeEvent
    public static void release(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (client.level != feedbackLevel || client.player == null || !client.options.keyAttack.isDown()) {
            explainedPress = false;
            feedbackLevel = client.level;
        }
    }
}
