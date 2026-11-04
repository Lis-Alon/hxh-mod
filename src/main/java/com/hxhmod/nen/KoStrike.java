package com.hxhmod.nen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ko: enquanto a tecnica esta ligada, a aura vai carregando no punho e o resto do corpo fica sem protecao.
 * O proximo ataque libera tudo; errar gasta a mesma aura.
 */
public final class KoStrike {
	// Tick em que a carga atual comecou
	private static final Map<ServerPlayer, Integer> CHARGE_START = new WeakHashMap<>();

	private KoStrike() {
	}

	static void tick(ServerPlayer player, NenData data) {
		if (data.isActive(Technique.KO)) {
			CHARGE_START.putIfAbsent(player, player.tickCount);
		} else {
			CHARGE_START.remove(player);
		}
	}

	/** Tecla segurada liga o Ko; soltar antes de atacar desfaz a concentracao sem gastar a aura do golpe. */
	public static void hold(ServerPlayer player, boolean holding) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (holding != data.isActive(Technique.KO)) {
			NenActions.toggle(player, Technique.KO);
		}
	}

	/** Carga de 0 a 1, limitada pela aura que da para pagar. */
	public static double charge(ServerPlayer player, NenData data) {
		double elapsed = player.tickCount - CHARGE_START.getOrDefault(player, player.tickCount);
		double charge = Math.min(1, elapsed / data.koChargeTicks());
		double cost = data.koCost(charge);
		return data.aura() >= cost ? charge : charge * data.aura() / cost;
	}

	/** Bonus de dano do golpe que acertou; termina a concentracao. Zero se o Ko nao estiver ligado. */
	static double strike(ServerPlayer attacker) {
		NenData data = attacker.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.isActive(Technique.KO)) {
			return 0;
		}
		double charge = charge(attacker, data);
		release(attacker, data, charge, "nen.hxh.ko.strike");
		attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
				SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
		return data.koDamage(charge);
	}

	/** Atacou o ar: a aura comprometida se perde. */
	public static void miss(ServerPlayer player) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (data.isActive(Technique.KO)) {
			release(player, data, charge(player, data), "nen.hxh.ko.miss");
		}
	}

	private static void release(ServerPlayer player, NenData data, double charge, String messageKey) {
		NenData after = data.withAura(data.aura() - (int) Math.round(data.koCost(charge)))
				.toggled(Technique.KO)
				.addTraining(Technique.KO, NenRules.KO_APPLICATION_TRAINING);
		player.setAttached(ModAttachments.NEN, NenTraining.koReleased(player, after));
		CHARGE_START.remove(player);
		player.sendOverlayMessage(Component.translatable(messageKey));
		AuraVisuals.burst(player, data, ParticleTypes.CRIT, 25);
	}
}
