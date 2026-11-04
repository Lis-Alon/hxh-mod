package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.Technique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * En: a aura espalhada numa esfera em volta do jogador detecta toda criatura la dentro, tambem acima,
 * abaixo e atras de paredes, inclusive as invisiveis. O diametro vem da proficiencia no En (5 ate 20 blocos).
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
		return player.distanceTo(entity) <= data.enSize() / 2;
	}
}
