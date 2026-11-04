package com.hxhmod.nen;

/**
 * Status internos do personagem: o que os pontos de atributo e as tecnicas ligadas somam.
 * Tudo e aplicado pelo mod (mixins e {@link NenCombat}), sem atributos nem pocoes do Minecraft.
 *
 * @param maxHealth   vida maxima extra
 * @param flatDamage  dano extra em golpes corpo a corpo
 * @param damageBonus dano corpo a corpo a mais (0,25 = +25%)
 * @param damageTaken multiplicador do dano recebido (0,8 = recebe 80%)
 * @param attackSpeed velocidade de ataque a mais (fracao)
 * @param moveSpeed   velocidade de movimento a mais (fracao)
 */
public record NenBonuses(double maxHealth, double flatDamage, double damageBonus, double damageTaken,
		double attackSpeed, double moveSpeed) {
	public static final NenBonuses NONE = new NenBonuses(0, 0, 0, 1, 0, 0);

	public static NenBonuses of(NenData data) {
		if (data == null || !data.awakened()) {
			return NONE;
		}
		double maxHealth = points(data, NenStat.VITALITY);
		double flatDamage = points(data, NenStat.ATTACK_POWER);
		double attackSpeed = points(data, NenStat.ATTACK_SPEED);
		double moveSpeed = points(data, NenStat.AGILITY);
		double damageTaken = 1 - points(data, NenStat.PROTECTION);
		double damageBonus = data.isActive(Technique.REN) ? data.renDamage() : 0;

		// Ten, Ren e Ken sao a mesma camada de aura: vale a maior defesa, sem somar as tres
		double nenDefense = 0;
		if (data.isActive(Technique.TEN)) {
			nenDefense = Math.max(nenDefense, data.tenDefense());
		}
		if (data.isActive(Technique.REN)) {
			nenDefense = Math.max(nenDefense, data.renDefense());
		}
		if (data.isActive(Technique.KEN)) {
			nenDefense = Math.max(nenDefense, data.kenDefense());
		}
		if (data.isActive(Technique.KO)) {
			// Toda a aura no punho: o resto do corpo fica sem defesa de Nen
			nenDefense = -NenRules.KO_DAMAGE_TAKEN;
		}
		damageTaken *= 1 - nenDefense;
		return new NenBonuses(maxHealth, flatDamage, damageBonus, Math.max(damageTaken, NenRules.MIN_DAMAGE_TAKEN),
				attackSpeed, moveSpeed);
	}

	private static double points(NenData data, NenStat stat) {
		return data.statPoints(stat) * stat.perPoint();
	}
}
