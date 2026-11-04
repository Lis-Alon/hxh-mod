package com.hxhmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Terceira pessoa: faixas de aura presas aos bracos do modelo, acompanhando a animacao. */
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
		PlayerModel model = getParentModel();
		pose.pushPose();
		model.root().translateAndRotate(pose);
		pose.pushPose();
		model.rightArm.translateAndRotate(pose);
		AuraRender.arm(pose, collector, view, state.ageInTicks, 1);
		pose.popPose();
		model.leftArm.translateAndRotate(pose);
		AuraRender.arm(pose, collector, view, state.ageInTicks, -1);
		pose.popPose();
	}
}
