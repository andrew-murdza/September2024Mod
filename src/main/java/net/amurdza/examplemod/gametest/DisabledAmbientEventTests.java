package net.amurdza.examplemod.gametest;

import alabaster.hearthandharvest.common.entity.ManureDropHelper;
import alabaster.hearthandharvest.common.event.ManureEvents;
import alabaster.hearthandharvest.common.registry.HHModAttachments;
import alabaster.hearthandharvest.common.registry.HHModItems;
import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DisabledAmbientEventTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void manureFeedingGrazingSavedTimersAndDirectDropsAreDisabled(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var cow = EntityType.COW.create(level);
        var sheep = EntityType.SHEEP.create(level);
        cow.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sheep.moveTo(cow.getX(), cow.getY(), cow.getZ());
        var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        var sounds = new SoundAudit(level);
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            helper.assertTrue(!ManureDropHelper.canPoop(cow) && !ManureDropHelper.canPoop(sheep),
                    "Adult cows and sheep must be excluded from periodic manure production");
            ManureEvents.onPlayerFeedAnimal(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow));
            sheep.ate();
            helper.assertTrue(HHModAttachments.MANURE_POOP_TIMER.get(cow) == 0
                            && HHModAttachments.MANURE_POOP_TIMER.get(sheep) == 0,
                    "Feeding and sheep grazing must not schedule manure drops");
            HHModAttachments.MANURE_POOP_TIMER.set(cow, 1);
            // Exercise the due saved timer and many periodic 20-tick opportunities.
            for (int i = 0; i < 200; i++) {
                cow.tickCount = i * 20;
                ManureEvents.onEntityTick(new LivingEvent.LivingTickEvent(cow));
            }
            ManureDropHelper.dropPoop(cow);
            ManureDropHelper.dropPoop(sheep);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                            .noneMatch(item -> item.getItem().is(HHModItems.MANURE.get())),
                    "No manure may be created by periodic, saved-timer, or direct production paths");
            helper.assertTrue(sounds.played.isEmpty(), "Disabled manure production must also suppress its sound");
            ItemEntity ordinary = cow.spawnAtLocation(Items.WHEAT);
            helper.assertTrue(ordinary != null && ordinary.getItem().is(Items.WHEAT) && ordinary.isAddedToWorld(),
                    "Ordinary item drops must still work");
            ordinary.discard();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }
        helper.succeed();
    }

    public static final class SoundAudit {
        private final Level level;
        private final List<SoundEvent> played = new ArrayList<>();

        SoundAudit(Level level) { this.level = level; }

        @SubscribeEvent
        public void sound(PlayLevelSoundEvent event) {
            if (event.getLevel() == level && event.getSound() != null) played.add(event.getSound().value());
        }
    }

}
