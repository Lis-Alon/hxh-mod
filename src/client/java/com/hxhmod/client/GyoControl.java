package com.hxhmod.client;

import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lado do cliente do Gyo ligado: cada aplicacao tem a propria tecla, que da para trocar nos Controles.
 * Tambem mostra o contorno de quem esta oculto, visto so por quem usa.
 */
public final class GyoControl {
	// Espera a resposta do servidor antes de mandar a mesma acao de novo
	private static final int RESEND_TICKS = 10;

	private static final Map<GyoApplications.Action, KeyMapping> KEYS = new EnumMap<>(GyoApplications.Action.class);
	private static final Map<GyoApplications.Action, Integer> WAITING = new EnumMap<>(GyoApplications.Action.class);

	private GyoControl() {
	}

	static void registerKeys(KeyMapping.Category category) {
		register(GyoApplications.Action.PUNCH, InputConstants.KEY_GRAVE, category);
		register(GyoApplications.Action.GUARD, InputConstants.KEY_X, category);
		register(GyoApplications.Action.JUMP, InputConstants.KEY_C, category);
		register(GyoApplications.Action.DASH, InputConstants.KEY_LALT, category);
	}

	private static void register(GyoApplications.Action action, int defaultKey, KeyMapping.Category category) {
		KEYS.put(action, KeyMappingHelper.registerKeyMapping(new KeyMapping(action.keyName(), defaultKey, category)));
	}

	public static KeyMapping key(GyoApplications.Action action) {
		return KEYS.get(action);
	}

	private static boolean prepared(LocalPlayer player) {
		NenData data = player.getAttached(ModAttachments.NEN);
		return data != null && data.isActive(Technique.GYO);
	}

	static void tick(Minecraft client) {
		WAITING.replaceAll((action, ticks) -> Math.max(0, ticks - 1));
		KEYS.forEach((action, key) -> {
			boolean pressed = false;
			while (key.consumeClick()) {
				pressed = true;
			}
			if (pressed && client.player != null && client.gui.screen() == null) {
				press(client.player, action);
			}
		});
	}

	private static void press(LocalPlayer player, GyoApplications.Action action) {
		if (!prepared(player)) {
			NenData data = player.getAttached(ModAttachments.NEN);
			// Antes de aprender o Gyo a tecla nao faz nada: pode ter sido sem querer
			if (data != null && data.isUnlocked(Technique.GYO)) {
				player.sendOverlayMessage(Component.translatable("nen.hxh.gyo.needs_gyo"));
			}
			return;
		}
		if (WAITING.getOrDefault(action, 0) > 0 || GyoApplications.remaining(player, action) > 0
				|| (action == GyoApplications.Action.JUMP && !player.onGround())) {
			return;
		}
		Vec3 direction = action == GyoApplications.Action.DASH ? moveDirection(player) : Vec3.ZERO;
		if (ClientPlayNetworking.canSend(NenPayloads.GyoApplication.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.GyoApplication(action, (float) direction.x, (float) direction.z));
			WAITING.put(action, RESEND_TICKS);
		}
	}

	/** Para onde as teclas de andar apontam, ou nada se estiver parado (o servidor usa a camera). */
	private static Vec3 moveDirection(LocalPlayer player) {
		Vec2 move = player.input.getMoveVector();
		double yaw = Math.toRadians(player.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new Vec3(move.x * cos - move.y * sin, 0, move.y * cos + move.x * sin);
	}

	/** Visao de aura: quem esta oculto, perto e na linha de visao de quem prepara o Gyo. */
	public static boolean sees(Entity entity) {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && entity != player && entity instanceof LivingEntity && entity.isInvisible()
				&& prepared(player) && player.distanceTo(entity) <= NenRules.GYO_REVEAL_RADIUS
				&& player.hasLineOfSight(entity);
	}
}
