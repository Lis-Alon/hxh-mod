package com.hxhmod.nen;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ryu: redistribui a aura do Ken entre ataque e defesa. A parte ofensiva anda ate a postura pedida ao longo de
 * alguns ticks, entao trocar tarde deixa uma janela vulneravel. Reforcar um lado sempre tira do outro.
 */
public final class RyuFlow {
	/** Para onde o jogador esta mandando a aura. */
	public enum Stance {
		BALANCED(0.5), ATTACK(0.7), DEFEND(0.3);

		public static final StreamCodec<ByteBuf, Stance> STREAM_CODEC =
				ByteBufCodecs.VAR_INT.map(i -> values()[i], Stance::ordinal);

		private final double offense;

		Stance(double offense) {
			this.offense = offense;
		}
	}

	private static final Map<ServerPlayer, Stance> STANCE = new WeakHashMap<>();
	private static final Map<ServerPlayer, Double> OFFENSE = new WeakHashMap<>();

	private RyuFlow() {
	}

	static void tick(ServerPlayer player, NenData data) {
		if (!data.isActive(Technique.RYU)) {
			STANCE.remove(player);
			OFFENSE.remove(player);
			return;
		}
		double target = STANCE.getOrDefault(player, Stance.BALANCED).offense;
		double current = offense(player);
		double step = (Stance.ATTACK.offense - Stance.BALANCED.offense) / data.ryuTransitionTicks();
		OFFENSE.put(player, current < target ? Math.min(target, current + step) : Math.max(target, current - step));
	}

	/** Pede outra postura; mandar a aura para um lado custa aura, voltar ao equilibrio nao. */
	public static void setStance(ServerPlayer player, Stance stance) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.isActive(Technique.RYU) || stance == STANCE.getOrDefault(player, Stance.BALANCED)) {
			return;
		}
		if (stance != Stance.BALANCED) {
			if (data.aura() < data.ryuShiftCost()) {
				return;
			}
			player.setAttached(ModAttachments.NEN, data.withAura(data.aura() - data.ryuShiftCost()));
		}
		STANCE.put(player, stance);
	}

	static double offense(ServerPlayer player) {
		return OFFENSE.getOrDefault(player, Stance.BALANCED.offense);
	}

	/** Dano a mais (ou a menos) conforme a aura no ataque agora. */
	static double damageBonus(ServerPlayer player) {
		return (offense(player) - Stance.BALANCED.offense) * NenRules.RYU_SWING;
	}

	/** Multiplicador do dano recebido: menos aura na defesa, mais dano. */
	static double damageTaken(ServerPlayer player) {
		return 1 + (offense(player) - Stance.BALANCED.offense) * NenRules.RYU_SWING;
	}
}
