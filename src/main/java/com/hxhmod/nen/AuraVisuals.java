package com.hxhmod.nen;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lado do servidor da aura visivel: publica o estado que os clientes desenham (AuraRender) e envia as
 * explosoes das aplicacoes a cada jogador separadamente. O In esconde dos outros, e quem esta com o
 * Gyo nos olhos ve mesmo assim. Em Zetsu nao sai aura nenhuma.
 */
public final class AuraVisuals {
	private AuraVisuals() {
	}

	static void publish(ServerPlayer player, NenData data) {
		AuraShow show = AuraShow.of(data, KoStrike.charge(player, data), RyuFlow.offense(player));
		if (!show.equals(player.getAttachedOrCreate(ModAttachments.AURA_SHOW))) {
			player.setAttached(ModAttachments.AURA_SHOW, show);
		}
	}

	/** Explosao de aura de uma aplicacao, que o In tambem esconde. */
	static void burst(ServerPlayer player, NenData data, ParticleOptions particle, int count) {
		send(player, data, particle, count, 0.2);
	}

	private static void send(ServerPlayer player, NenData data, ParticleOptions particle, int count, double speed) {
		for (ServerPlayer observer : player.level().players()) {
			int shown = shownTo(observer, player, data, count);
			if (shown > 0) {
				player.level().sendParticles(observer, particle, false, false, player.getX(), player.getY() + 1, player.getZ(),
						shown, 0.35, 0.55, 0.35, speed);
			}
		}
	}

	/** Quantas particulas este observador ve: o In so deixa passar o que vazou da capacidade dele. */
	public static int shownTo(ServerPlayer observer, ServerPlayer player, NenData data, int count) {
		if (observer == player || seesWithGyo(observer, player)) {
			return count;
		}
		return (int) Math.round(count * data.inLeak());
	}

	private static boolean seesWithGyo(ServerPlayer observer, ServerPlayer player) {
		return observer.getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.GYO)
				&& observer.distanceTo(player) <= NenRules.GYO_REVEAL_RADIUS
				&& observer.hasLineOfSight(player);
	}
}
