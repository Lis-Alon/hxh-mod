package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * En: a aura espalhada numa area quadrada em volta do jogador detecta toda criatura la dentro,
 * inclusive as invisiveis. O tamanho vem da proficiencia no En (5x5 ate 20x20).
 */
public final class EnSense {
	private EnSense() {
	}

	public static boolean senses(Entity entity) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || entity == player || !(entity instanceof LivingEntity)) {
			return false;
		}
		NenData data = player.getAttached(ModAttachments.NEN);
		if (data == null || !data.isActive(Technique.EN)) {
			return false;
		}
		double half = data.enSize() / 2;
		return Math.abs(entity.getX() - player.getX()) <= half
				&& Math.abs(entity.getZ() - player.getZ()) <= half
				&& Math.abs(entity.getY() - player.getY()) <= half;
	}
}
