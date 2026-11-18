package net.amurdza.examplemod.event_handlers;

import dev.hybridlabs.aquatic.entity.crustacean.CoconutCrabEntity;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/** Native coconut crabs already breathe underwater; keep their wandering on the seabed. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class CoconutCrabAquatics {
    private CoconutCrabAquatics() { }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof CoconutCrabEntity crab)) return;
        if (!event.loadedFromDisk()) crab.setShellItem(ItemStack.EMPTY);
        crab.goalSelector.removeAllGoals(goal -> goal instanceof RandomStrollGoal);
        crab.goalSelector.addGoal(3, new BottomStroll(crab));
    }

    private static final class BottomStroll extends RandomStrollGoal {
        private final CoconutCrabEntity crab;

        private BottomStroll(CoconutCrabEntity crab) {
            super(crab, .4);
            this.crab = crab;
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            for (int attempt = 0; attempt < 24; attempt++) {
                BlockPos pos = crab.blockPosition().offset(crab.getRandom().nextInt(17) - 8,
                        crab.getRandom().nextInt(7) - 3, crab.getRandom().nextInt(17) - 8);
                if (crab.level().getFluidState(pos).is(FluidTags.WATER)
                        && crab.level().getBlockState(pos.below()).isFaceSturdy(crab.level(), pos.below(), Direction.UP)
                        && crab.level().noCollision(crab, crab.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(crab.position())))) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
            return null;
        }
    }
}
