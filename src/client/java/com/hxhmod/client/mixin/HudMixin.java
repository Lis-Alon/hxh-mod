package com.hxhmod.client.mixin;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A barra de coracoes mostra a vida extra da Vitalidade, que e um status interno do Nen. */
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyExpressionValue(method = "extractPlayerHealth", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double hxh$nenMaxHealth(double maxHealth) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return maxHealth;
		}
		return maxHealth + NenBonuses.of(minecraft.player.getAttached(ModAttachments.NEN)).maxHealth();
	}
}
