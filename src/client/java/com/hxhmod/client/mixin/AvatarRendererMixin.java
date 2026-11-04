package com.hxhmod.client.mixin;

import com.hxhmod.client.AuraRender;
import com.hxhmod.client.MeditationPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hxhmod.nen.ModAttachments;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copia "esta meditando" do jogador para o estado de desenho. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void hxh$markMeditating(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		boolean meditating = entity.getAttachedOrElse(ModAttachments.MEDITATING, false)
				&& !state.isPassenger && !state.isFallFlying && !state.isVisuallySwimming;
		state.setData(MeditationPose.MEDITATING, meditating);
		if (meditating) {
			// Meditar agachado nao deve puxar a pose de agachar
			state.isCrouching = false;
		}
		AuraRender.extract(entity, state);
	}

	// Primeira pessoa: a aura nos bracos segue a mesma pose do braco
	@Inject(method = "renderRightHand", at = @At("HEAD"))
	private void hxh$auraOnRightHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
		AuraRender.firstPersonArm(pose, collector, 1);
	}

	@Inject(method = "renderLeftHand", at = @At("HEAD"))
	private void hxh$auraOnLeftHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
		AuraRender.firstPersonArm(pose, collector, -1);
	}
}
