package com.hxhmod.client.mixin;

import com.hxhmod.client.AuraRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Com a aura ligada os dois bracos aparecem em primeira pessoa, mesmo segurando um objeto. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Shadow
	protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			float inverseArmHeight, float attackValue, HumanoidArm arm, PlayerRenderState playerState);

	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	private void hxh$showArm(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
			InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		var avatar = playerState.avatarRenderState;
		boolean main = hand == InteractionHand.MAIN_HAND;
		AuraRender.coatHeld(main, itemStack);
		if (avatar == null || state.isScoping || avatar.isInvisible || (main && itemStack.isEmpty())
				|| !AuraRender.showsBareArm(itemStack, avatar.isUsingItem && avatar.useItemHand == hand)) {
			return;
		}
		HumanoidArm arm = main ? avatar.mainArm : avatar.mainArm.getOpposite();
		poseStack.pushPose();
		// Um pouco mais alto e para dentro que o normal, para o braco inteiro aparecer acima da hotbar
		int side = arm == HumanoidArm.RIGHT ? 1 : -1;
		poseStack.translate(side * -0.06F, 0.17F, 0);
		renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attack, arm, playerState);
		poseStack.popPose();
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void hxh$endCoat(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
			InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		AuraRender.endCoat();
	}
}
