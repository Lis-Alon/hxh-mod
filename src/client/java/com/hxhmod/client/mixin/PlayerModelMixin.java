package com.hxhmod.client.mixin;

import com.hxhmod.client.AuraRender;
import com.hxhmod.client.MeditationPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Depois da animacao normal, troca a pose pela de meditacao quando marcado. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void hxh$meditate(AvatarRenderState state, CallbackInfo ci) {
		if (Boolean.TRUE.equals(state.getData(MeditationPose.MEDITATING))) {
			MeditationPose.apply((PlayerModel) (Object) this);
			return;
		}
		AuraRender.poseArms((PlayerModel) (Object) this, state);
	}
}
