package net.amurdza.examplemod.mixins.othermods.backport;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Matches the official modern 64x64 temperate-cow and mooshroom texture layout. */
@Mixin(CowModel.class)
public abstract class UpdatedCowTextureLayout {
    @Inject(method = "createBodyLayer", at = @At("HEAD"), cancellable = true)
    private static void aoemod$modernTextureLayout(CallbackInfoReturnable<LayerDefinition> cir) {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4F, -4F, -6F, 8F, 8F, 6F)
                .texOffs(1, 33).addBox(-3F, 1F, -7F, 6F, 3F, 1F)
                .texOffs(22, 0).addBox("right_horn", -5F, -5F, -5F, 1F, 3F, 1F)
                .texOffs(22, 0).addBox("left_horn", 4F, -5F, -5F, 1F, 3F, 1F),
                PartPose.offset(0F, 4F, -8F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 4)
                .addBox(-6F, -10F, -7F, 12F, 18F, 10F)
                .texOffs(52, 0).addBox(-2F, 2F, -8F, 4F, 6F, 1F),
                PartPose.offsetAndRotation(0F, 5F, 2F, (float) Math.PI / 2F, 0F, 0F));
        var leftLeg = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F);
        var rightLeg = CubeListBuilder.create().texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F);
        root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-4F, 12F, 7F));
        root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(4F, 12F, 7F));
        root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-4F, 12F, -5F));
        root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(4F, 12F, -5F));
        cir.setReturnValue(LayerDefinition.create(mesh, 64, 64));
    }
}
