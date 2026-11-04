package com.hxhmod.client.mixin;

import com.hxhmod.client.AuraRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Com a aura ligada os dois bracos aparecem em primeira pessoa, mesmo segurando um objeto. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Shadow
	protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			float inverseArmHeight, float attackValue, HumanoidArm arm, PlayerRenderState playerState);

	@Unique
	private boolean hxh$raised;

	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	private void hxh$showArm(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
			InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		var avatar = playerState.avatarRenderState;
		boolean main = hand == InteractionHand.MAIN_HAND;
		AuraRender.coatHeld(main, itemStack);
		// Ko: o braco principal vai a frente, com o punho em direcao ao centro da tela
		hxh$raised = main && AuraRender.ownKo();
		if (hxh$raised) {
			int raisedSide = avatar == null || avatar.mainArm == HumanoidArm.RIGHT ? 1 : -1;
			poseStack.pushPose();
			poseStack.translate(raisedSide * -0.16F, 0.24F, -0.1F);
		}
		if (avatar == null || state.isScoping || avatar.isInvisible || (main && itemStack.isEmpty())
				|| !AuraRender.showsBareArm(itemStack, avatar.isUsingItem && avatar.useItemHand == hand)) {
			return;
		}
		HumanoidArm arm = main ? avatar.mainArm : avatar.mainArm.getOpposite();
		poseStack.pushPose();
		// O braco sai do canto da tela em direcao ao centro, para aparecer inteiro acima da hotbar
		int side = arm == HumanoidArm.RIGHT ? 1 : -1;
		float pivotX = side * 0.75F;
		poseStack.translate(pivotX, -0.6F, -0.7F);
		poseStack.rotateDegrees(Axis.YP, side * (main ? 12 : 34));
		poseStack.rotateDegrees(Axis.XP, main ? 6 : 10);
		poseStack.translate(-pivotX, 0.6F + (main ? 0.06F : 0.1F), 0.7F);
		renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attack, arm, playerState);
		poseStack.popPose();
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void hxh$endCoat(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
			InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		AuraRender.endCoat();
		if (hxh$raised) {
			poseStack.popPose();
			hxh$raised = false;
		}
	}
}
