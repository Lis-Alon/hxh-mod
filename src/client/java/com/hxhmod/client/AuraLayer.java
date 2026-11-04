package com.hxhmod.client;

import com.hxhmod.nen.AuraShow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;

/** Terceira pessoa: o que fica preso ao modelo e acompanha a animacao (bracos, punho do Ko e cabeca do Gyo). */
public class AuraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public AuraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		AuraRender.View view = state.getData(AuraRender.VIEW);
		if (view == null) {
			return;
		}
		if (view.preview()) {
			AuraRender.previewBody(pose, collector, view, state.ageInTicks);
		}
		AuraShow show = view.show();
		PlayerModel model = getParentModel();
		pose.pushPose();
		model.root().translateAndRotate(pose);
		arm(pose, collector, view, state, model, HumanoidArm.RIGHT);
		arm(pose, collector, view, state, model, HumanoidArm.LEFT);
		if (show.gyo()) {
			pose.pushPose();
			model.head.translateAndRotate(pose);
			AuraRender.head(pose, collector, view, state.ageInTicks);
			pose.popPose();
		}
		pose.popPose();
	}

	private static void arm(PoseStack pose, SubmitNodeCollector collector, AuraRender.View view, AvatarRenderState state, PlayerModel model, HumanoidArm arm) {
		pose.pushPose();
		(arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm).translateAndRotate(pose);
		AuraRender.arm(pose, collector, view, state.ageInTicks, arm == HumanoidArm.RIGHT ? 1 : -1, arm == state.mainArm, false);
		pose.popPose();
	}
}
