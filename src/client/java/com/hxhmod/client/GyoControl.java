package com.hxhmod.client;

import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Lado do cliente do Gyo ligado: manda as acoes (pular, Ctrl ou botao direito) que nao estao recarregando
 * para o servidor e mostra o contorno de quem esta oculto, visto so por quem usa.
 */
public final class GyoControl {
	// Espera a resposta do servidor antes de mandar outra acao
	private static final int RESEND_TICKS = 10;

	private static boolean wasJumping;
	private static boolean wasSprinting;
	private static int waiting;

	private GyoControl() {
	}

	private static boolean prepared(LocalPlayer player) {
		NenData data = player.getAttached(ModAttachments.NEN);
		return data != null && data.isActive(Technique.GYO);
	}

	static void tick(Minecraft client) {
		boolean jumping = client.options.keyJump.isDown();
		boolean sprinting = client.options.keySprint.isDown();
		LocalPlayer player = client.player;
		if (waiting > 0) {
			waiting--;
		}
		if (player != null && prepared(player) && client.gui.screen() == null && waiting == 0) {
			if (jumping && !wasJumping && player.onGround() && GyoApplications.remaining(player, GyoApplications.Action.JUMP) == 0) {
				send(GyoApplications.Action.JUMP, Vec3.ZERO);
			} else if (sprinting && !wasSprinting && GyoApplications.remaining(player, GyoApplications.Action.DASH) == 0) {
				send(GyoApplications.Action.DASH, moveDirection(player));
			}
		}
		wasJumping = jumping;
		wasSprinting = sprinting;
	}

	/** Botao direito com a super defesa pronta vira defesa em vez de usar o item. Devolve se tratou o clique. */
	public static boolean onUseItem(LocalPlayer player) {
		if (player == null || !prepared(player)) {
			return false;
		}
		if (waiting == 0) {
			// Recarregando, o botao direito volta a usar itens e blocos: o Gyo agora fica ligado por muito tempo
			if (GyoApplications.remaining(player, GyoApplications.Action.GUARD) > 0) {
				return false;
			}
			send(GyoApplications.Action.GUARD, Vec3.ZERO);
		}
		return true;
	}

	private static void send(GyoApplications.Action action, Vec3 direction) {
		if (ClientPlayNetworking.canSend(NenPayloads.GyoApplication.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.GyoApplication(action, (float) direction.x, (float) direction.z));
			waiting = RESEND_TICKS;
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
