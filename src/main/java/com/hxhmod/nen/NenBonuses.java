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
 * @param moveSpeed   velocidade de movimento a mais (fracao; negativa deixa mais lento)
 * @param miningSpeed velocidade de quebrar blocos a mais (fracao)
 */
public record NenBonuses(double maxHealth, double flatDamage, double damageBonus, double damageTaken,
		double attackSpeed, double moveSpeed, double miningSpeed) {
	public static final NenBonuses NONE = new NenBonuses(0, 0, 0, 1, 0, 0, 0);

	public static NenBonuses of(NenData data) {
		if (data == null || !data.awakened()) {
			return NONE;
		}
		double maxHealth = points(data, NenStat.VITALITY);
		double flatDamage = points(data, NenStat.ATTACK_POWER);
		double attackSpeed = points(data, NenStat.ATTACK_SPEED);
		double moveSpeed = points(data, NenStat.AGILITY);
		double damageTaken = 1 - points(data, NenStat.PROTECTION);
		double damageBonus = 0;
		double miningSpeed = 0;

		for (Technique technique : data.active()) {
			switch (technique) {
				case TEN -> damageTaken *= 1 - data.tenDefense();
				case KEN -> damageTaken *= 1 - (data.has(NenUpgrade.KEN_IRON) ? NenRules.KEN_IRON_DEFENSE : NenRules.KEN_DEFENSE);
				case REN -> damageBonus += data.has(NenUpgrade.REN_SURGE) ? NenRules.REN_SURGE_DAMAGE : NenRules.REN_DAMAGE;
				case IN -> damageBonus += data.has(NenUpgrade.IN_SHADOW) ? NenRules.IN_SHADOW_DAMAGE : NenRules.IN_DAMAGE;
				case KO -> {
					damageBonus += data.has(NenUpgrade.KO_ABSOLUTE) ? NenRules.KO_ABSOLUTE_DAMAGE : NenRules.KO_DAMAGE;
					damageTaken *= 1 + NenRules.KO_DAMAGE_TAKEN;
					moveSpeed -= NenRules.KO_SLOWNESS;
				}
				case RYU -> {
					boolean master = data.has(NenUpgrade.RYU_MASTER);
					damageBonus += master ? NenRules.RYU_MASTER_DAMAGE : NenRules.RYU_DAMAGE;
					damageTaken *= 1 - (master ? NenRules.RYU_MASTER_DEFENSE : NenRules.RYU_DEFENSE);
					moveSpeed += NenRules.RYU_SPEED;
				}
				case SHU -> {
					miningSpeed += NenRules.SHU_MINING;
					if (data.has(NenUpgrade.SHU_EDGE)) {
						damageBonus += NenRules.SHU_EDGE_DAMAGE;
					}
				}
				default -> {
				}
			}
		}
		return new NenBonuses(maxHealth, flatDamage, damageBonus, Math.max(damageTaken, NenRules.MIN_DAMAGE_TAKEN),
				attackSpeed, moveSpeed, miningSpeed);
	}

	private static double points(NenData data, NenStat stat) {
		return data.statPoints(stat) * stat.perPoint();
	}
}
