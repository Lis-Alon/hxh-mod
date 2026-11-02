package com.hxhmod.nen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Regras de Nen que mexem no combate: defesa do Ten, vulnerabilidade e furtividade do Zetsu.
 * Chamado pelos mixins de {@code LivingEntity}.
 */
public final class NenCombat {
	// Ultimo tick em que o jogador levou dano (treino pertinente do Ten)
	private static final Map<ServerPlayer, Integer> LAST_HURT = new WeakHashMap<>();
	// Ate que tick o jogador em Zetsu fica exposto por ter atacado
	private static final Map<ServerPlayer, Integer> EXPOSED_UNTIL = new WeakHashMap<>();

	private NenCombat() {
	}

	/**
	 * Ajusta o dano antes da armadura com os status internos: forca de quem bate (corpo a corpo)
	 * e defesa de quem apanha (Protecao, Ten, Ken...). Em Zetsu nao ha defesa de Nen.
	 */
	public static float incomingDamage(LivingEntity victim, DamageSource source, float amount) {
		if (amount <= 0) {
			return amount;
		}
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
			NenData attackerData = attacker.getAttachedOrCreate(ModAttachments.NEN);
			if (attackerData.isActive(Technique.ZETSU)) {
				// Atacar denuncia a posicao
				EXPOSED_UNTIL.put(attacker, attacker.tickCount + NenRules.ZETSU_EXPOSED_SECONDS * 20);
			}
			if (source.getDirectEntity() == attacker) {
				NenBonuses bonuses = NenBonuses.of(attackerData);
				amount = (float) ((amount + bonuses.flatDamage()) * (1 + bonuses.damageBonus()));
			}
		}
		if (!(victim instanceof ServerPlayer player)) {
			return amount;
		}
		LAST_HURT.put(player, player.tickCount);
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		amount = (float) (amount * NenBonuses.of(data).damageTaken());
		if (data.isActive(Technique.ZETSU) && releasingAura(source)) {
			amount = (float) (amount * NenRules.ZETSU_NEN_DAMAGE_MULTIPLIER);
		}
		return amount;
	}

	/** O golpe vem de alguem com aura liberada (qualquer tecnica alem de Ten e Zetsu). */
	private static boolean releasingAura(DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer attacker)) {
			return false;
		}
		return attacker.getAttachedOrCreate(ModAttachments.NEN).active().stream()
				.anyMatch(technique -> technique != Technique.TEN && technique != Technique.ZETSU);
	}

	/** Multiplica a chance dos mobs perceberem o jogador. Em Zetsu ele continua visivel, so mais discreto. */
	public static double visibility(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return 1.0;
		}
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.isActive(Technique.ZETSU) || exposed(player)) {
			return 1.0;
		}
		double visibility = data.zetsuVisibility();
		if (player.isShiftKeyDown() && data.has(NenUpgrade.ZETSU_STEP)) {
			visibility *= NenRules.ZETSU_SILENT_STEP_VISIBILITY;
		}
		return visibility;
	}

	public static boolean exposed(ServerPlayer player) {
		return player.tickCount < EXPOSED_UNTIL.getOrDefault(player, 0);
	}

	/** Levou dano no ultimo segundo. */
	static boolean hurtRecently(ServerPlayer player) {
		return player.tickCount - LAST_HURT.getOrDefault(player, -1000) <= 20;
	}
}
