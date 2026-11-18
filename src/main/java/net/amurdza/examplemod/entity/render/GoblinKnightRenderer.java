package net.amurdza.examplemod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.client.model.entity.LowerGoblinKnightModel;
import twilightforest.client.model.entity.UpperGoblinKnightModel;
import twilightforest.client.model.entity.newmodels.NewLowerGoblinKnightModel;
import twilightforest.client.model.entity.newmodels.NewUpperGoblinKnightModel;
import twilightforest.client.renderer.entity.TFBipedRenderer;
import twilightforest.client.renderer.entity.UpperGoblinKnightRenderer;
import twilightforest.client.renderer.entity.newmodels.NewUpperGoblinKnightRenderer;
import twilightforest.entity.monster.LowerGoblinKnight;
import twilightforest.entity.monster.UpperGoblinKnight;
import twilightforest.init.TFEntities;

import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

/** One real saved entity, rendered through the original two complete native living-render passes. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoblinKnightRenderer extends EntityRenderer<UpperGoblinKnight> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("twilightforest", "textures/model/doublegoblin.png");
    public final GoblinKnightAppearance appearance = new GoblinKnightAppearance();
    public final LivingEntityRenderer<LowerGoblinKnight, ? extends HumanoidModel<LowerGoblinKnight>> lowerRenderer;
    public final LivingEntityRenderer<UpperGoblinKnight, ? extends HumanoidModel<UpperGoblinKnight>> upperRenderer;
    // Values must also be weak: each proxy references its ClientLevel, which can
    // reference the real entity key. This avoids retaining unloaded worlds while
    // still allowing portal renderers to draw several live dimensions.
    private final Map<UpperGoblinKnight, WeakReference<VisualPair>> visuals = new WeakHashMap<>();

    public record VisualPair(LowerGoblinKnight lower, UpperGoblinKnight upper) { }

    public GoblinKnightRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .625F;
        if (appearance.jappa) {
            lowerRenderer = new TFBipedRenderer<>(context, new NewLowerGoblinKnightModel(appearance.lower), .625F, "doublegoblin.png");
            upperRenderer = new NewUpperGoblinKnightRenderer(context, new NewUpperGoblinKnightModel(appearance.upper), .625F);
        } else {
            lowerRenderer = new TFBipedRenderer<>(context, new LowerGoblinKnightModel(appearance.lower), .625F, "doublegoblin.png");
            upperRenderer = new UpperGoblinKnightRenderer(context, new UpperGoblinKnightModel(appearance.upper), .625F);
        }
    }

    public VisualPair visualPair(UpperGoblinKnight source) {
        var reference = visuals.get(source);
        var pair = reference == null ? null : reference.get();
        if (pair == null) {
            // Constructor-only visual objects. Never finalize, tick, add to a
            // level, serialize, target, damage or award loot for these proxies.
            var lower = new LowerGoblinKnight(TFEntities.LOWER_GOBLIN_KNIGHT.get(), source.level());
            var upper = new UpperGoblinKnight(TFEntities.UPPER_GOBLIN_KNIGHT.get(), source.level());
            upper.startRiding(lower, true);
            pair = new VisualPair(lower, upper);
            visuals.put(source, new WeakReference<>(pair));
        }
        copyState(source, pair.lower(), 0);
        copyState(source, pair.upper(), pair.lower().getPassengersRidingOffset());
        pair.upper().yHeadRot = source.yBodyRot;
        pair.upper().yHeadRotO = source.yBodyRotO;
        pair.upper().heavySpearTimer = source.heavySpearTimer;
        return pair;
    }

    private static void copyState(UpperGoblinKnight source, LivingEntity target, double yOffset) {
        // Native team visibility uses the scoreboard UUID identity. These are
        // untracked objects, so matching the real UUID cannot create world IDs.
        if (!target.getUUID().equals(source.getUUID())) target.setUUID(source.getUUID());
        target.setPos(source.getX(), source.getY() + yOffset, source.getZ());
        target.xo = source.xo; target.yo = source.yo + yOffset; target.zo = source.zo;
        target.setYRot(source.getYRot()); target.yRotO = source.yRotO;
        target.setXRot(source.getXRot()); target.xRotO = source.xRotO;
        target.yBodyRot = source.yBodyRot; target.yBodyRotO = source.yBodyRotO;
        target.yHeadRot = source.yHeadRot; target.yHeadRotO = source.yHeadRotO;
        target.tickCount = source.tickCount;
        target.attackAnim = source.attackAnim; target.oAttackAnim = source.oAttackAnim;
        target.hurtTime = source.deathTime > 0 ? Math.max(1, source.hurtTime) : source.hurtTime;
        // Death roll is applied once to the complete body below, around one pivot.
        target.deathTime = 0;
        target.setHealth(source.getHealth());
        target.setInvisible(source.isInvisible());
        target.setGlowingTag(source.isCurrentlyGlowing());
        target.setPose(source.getPose());
        target.setOnGround(source.onGround());
        target.setDeltaMovement(source.getDeltaMovement());
        copyWalk(source.walkAnimation, target.walkAnimation);
    }

    private static void copyWalk(WalkAnimationState source, WalkAnimationState target) {
        // Public native methods can copy phase and both interpolation endpoints
        // exactly without tick-dependent accumulation or private-field access.
        target.update(source.position() - target.position() - source.speed(), 1);
        target.setSpeed(source.speed(0));
        target.update(source.speed(), 1);
    }

    @Override public void render(UpperGoblinKnight source, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light) {
        var pair = visualPair(source);
        pose.pushPose();
        if (source.deathTime > 0) {
            float roll = Math.min(1, Mth.sqrt(((source.deathTime + partial - 1) / 20F) * 1.6F)) * 90;
            float heading = 180 - Mth.rotLerp(partial, source.yBodyRotO, source.yBodyRot);
            pose.mulPose(Axis.YP.rotationDegrees(heading));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.mulPose(Axis.YP.rotationDegrees(-heading));
        }
        // Direct delegation retains native state setup, adult/rider handling,
        // shield animation and spear-specific rotation. Dispatcher shadows/fire
        // and nameplates belong only to the real outer entity.
        lowerRenderer.render(pair.lower(), yaw, partial, pose, buffers, light);
        pose.pushPose();
        pose.translate(0, pair.lower().getPassengersRidingOffset(), 0);
        upperRenderer.render(pair.upper(), yaw, partial, pose, buffers, light);
        pose.popPose();
        pose.popPose();
        super.render(source, yaw, partial, pose, buffers, light);
    }

    @Override protected boolean shouldShowName(UpperGoblinKnight entity) {
        return entity.hasCustomName() && (entity.shouldShowName() || entity == entityRenderDispatcher.crosshairPickEntity);
    }
    @Override public ResourceLocation getTextureLocation(UpperGoblinKnight entity) { return TEXTURE; }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TFEntities.UPPER_GOBLIN_KNIGHT.get(), GoblinKnightRenderer::new);
    }
}
