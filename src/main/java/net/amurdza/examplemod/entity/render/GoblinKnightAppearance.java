package net.amurdza.examplemod.entity.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.core.Direction;
import twilightforest.client.JappaPackReloadListener;
import twilightforest.client.model.entity.LowerGoblinKnightModel;
import twilightforest.client.model.entity.UpperGoblinKnightModel;
import twilightforest.client.model.entity.newmodels.NewLowerGoblinKnightModel;
import twilightforest.client.model.entity.newmodels.NewUpperGoblinKnightModel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Local appearance changes derived from untouched bundled meshes, without patching native model classes. */
public final class GoblinKnightAppearance {
    public final boolean jappa = JappaPackReloadListener.INSTANCE.uncachedJappaPackCheck().getAsBoolean();
    public final ModelPart upper;
    public final ModelPart lower;
    public final ModelPart breastplate;
    public final ModelPart tunic;

    public GoblinKnightAppearance() {
        var nativeUpper = (jappa ? NewUpperGoblinKnightModel.create() : UpperGoblinKnightModel.create()).bakeRoot();
        var nativeLower = (jappa ? NewLowerGoblinKnightModel.create() : LowerGoblinKnightModel.create()).bakeRoot();
        var originalPlate = jappa ? nativeUpper.getChild("body").getChild("breastplate") : nativeUpper.getChild("breastplate");
        breastplate = cleanRear(originalPlate, 13, 12, 0);
        tunic = cleanRear(nativeLower.getChild("tunic"), 12, 9, 19);
        var upperParts = nativeRootParts(nativeUpper);
        if (jappa) {
            upperParts.put("body", withChildren(nativeUpper.getChild("body"), Map.of("breastplate", breastplate)));
        } else upperParts.put("breastplate", breastplate);
        upper = withChildren(nativeUpper, upperParts);
        var lowerParts = nativeRootParts(nativeLower);
        lowerParts.put("tunic", tunic);
        lower = withChildren(nativeLower, lowerParts);
        // One person's native helmet, arms, armored torso/waist and walking legs.
        // Only duplicate anatomy is hidden; no native retained cuboid is resized.
        lower.getChild("head").visible = false;
        lower.getChild("hat").visible = false;
        lower.getChild("right_arm").visible = false;
        lower.getChild("left_arm").visible = false;
        upper.getChild("right_leg").visible = false;
        upper.getChild("left_leg").visible = false;
    }

    private static Map<String, ModelPart> nativeRootParts(ModelPart root) {
        var parts = new LinkedHashMap<String, ModelPart>();
        for (String name : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"})
            parts.put(name, root.getChild(name));
        return parts;
    }

    private static ModelPart withChildren(ModelPart original, Map<String, ModelPart> children) {
        var cubes = new ArrayList<ModelPart.Cube>();
        original.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> {
            if (path.isEmpty()) cubes.add(cube);
        });
        var result = new ModelPart(cubes, children);
        result.copyFrom(original);
        result.setInitialPose(original.getInitialPose());
        return result;
    }

    private static ModelPart cleanRear(ModelPart original, int width, int height, int textureY) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("armor", CubeListBuilder.create().texOffs(64, textureY)
                .addBox(-width / 2F, 0, -3, width, height, 6, EnumSet.complementOf(EnumSet.of(Direction.SOUTH)))
                .texOffs(70 - width, textureY + 6)
                .addBox(-width / 2F, 0, 3, width, height, 0, Set.of(Direction.SOUTH)), PartPose.ZERO);
        var result = LayerDefinition.create(mesh, 128, 64).bakeRoot().getChild("armor");
        result.copyFrom(original);
        result.setInitialPose(original.getInitialPose());
        return result;
    }
}
