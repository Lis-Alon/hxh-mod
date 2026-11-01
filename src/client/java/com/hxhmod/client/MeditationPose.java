package com.hxhmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Pose sentada de pernas cruzadas (como na meditacao), usada com o painel do Nen aberto,
 * ao meditar para despertar e ao se concentrar no copo d'agua.
 */
public final class MeditationPose {
	/** Marcado no estado de desenho do jogador quando ele esta meditando. */
	public static final RenderStateDataKey<Boolean> MEDITATING = RenderStateDataKey.create(() -> "hxh:meditating");

	// Quanto o corpo desce (em pixels do modelo) para as pernas ficarem no chao
	private static final float SIT_DROP = 11.0F;

	private MeditationPose() {
	}

	public static void apply(PlayerModel model) {
		model.head.y += SIT_DROP;
		model.body.y += SIT_DROP;
		model.rightArm.y += SIT_DROP;
		model.leftArm.y += SIT_DROP;

		// Pernas para a frente, abertas e cruzadas na altura dos pes
		model.rightLeg.y += SIT_DROP - 1.0F;
		model.leftLeg.y += SIT_DROP - 1.0F;
		model.rightLeg.xRot = -1.45F;
		model.leftLeg.xRot = -1.45F;
		model.rightLeg.yRot = -0.65F;
		model.leftLeg.yRot = 0.65F;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.zRot = 0.0F;

		// Bracos descansando sobre os joelhos
		model.rightArm.xRot = -0.55F;
		model.leftArm.xRot = -0.55F;
		model.rightArm.yRot = 0.0F;
		model.leftArm.yRot = 0.0F;
		model.rightArm.zRot = 0.35F;
		model.leftArm.zRot = -0.35F;

		// Tronco reto e cabeca levemente baixa
		model.body.xRot = 0.0F;
		model.head.xRot = 0.15F;
	}
}
