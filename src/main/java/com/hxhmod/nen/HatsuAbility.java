package com.hxhmod.nen;

import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.Optional;

/**
 * O Hatsu ativo de cada galho do hexagono. Os numeros vao do dominio 0 ao 100 do Hatsu, sempre
 * multiplicados pela afinidade do tipo inato com o galho; as sub-habilidades do galho somam por cima.
 */
public enum HatsuAbility {
	QUAKE(NenType.ENHANCEMENT, NenUpgrade.HATSU_QUAKE, 8, 20, 4, 6, 0, 0, null, null, null),
	PROPERTY(NenType.TRANSMUTATION, NenUpgrade.HATSU_PROPERTY, 0, 0, 0, 0, 10, 20, null, null, NenUpgrade.HATSU_PERSISTENT),
	ARMOR(NenType.CONJURATION, NenUpgrade.HATSU_ARMOR, 8, 24, 0, 0, 20, 40, NenUpgrade.HATSU_STRUCTURE, null, NenUpgrade.HATSU_MANIFEST),
	MARK(NenType.SPECIALIZATION, NenUpgrade.HATSU_MARK, 0, 0, 10, 20, 15, 30, null, NenUpgrade.HATSU_READING, NenUpgrade.HATSU_MEMORY),
	COMMAND(NenType.MANIPULATION, NenUpgrade.HATSU_COMMAND, 0, 0, 6, 10, 8, 16, null, NenUpgrade.HATSU_BOND, NenUpgrade.HATSU_ORDER),
	BLAST(NenType.EMISSION, NenUpgrade.HATSU_BLAST, 10, 24, 0, 0, 0, 0, null, null, null);

	private final NenType branch;
	private final NenUpgrade upgrade;
	private final double damageMin;
	private final double damageMax;
	private final double radiusMin;
	private final double radiusMax;
	private final double secondsMin;
	private final double secondsMax;
	private final NenUpgrade damageBoost;
	private final NenUpgrade radiusBoost;
	private final NenUpgrade durationBoost;

	HatsuAbility(NenType branch, NenUpgrade upgrade, double damageMin, double damageMax, double radiusMin, double radiusMax,
			double secondsMin, double secondsMax, NenUpgrade damageBoost, NenUpgrade radiusBoost, NenUpgrade durationBoost) {
		this.branch = branch;
		this.upgrade = upgrade;
		this.damageMin = damageMin;
		this.damageMax = damageMax;
		this.radiusMin = radiusMin;
		this.radiusMax = radiusMax;
		this.secondsMin = secondsMin;
		this.secondsMax = secondsMax;
		this.damageBoost = damageBoost;
		this.radiusBoost = radiusBoost;
		this.durationBoost = durationBoost;
	}

	public static Optional<HatsuAbility> of(NenType branch) {
		return Arrays.stream(values()).filter(ability -> ability.branch == branch).findFirst();
	}

	public static Optional<HatsuAbility> of(NenUpgrade upgrade) {
		return Arrays.stream(values()).filter(ability -> ability.upgrade == upgrade).findFirst();
	}

	public NenType branch() {
		return branch;
	}

	public NenUpgrade upgrade() {
		return upgrade;
	}

	/** Dano final (na Armadura, a vida extra). */
	public double damage(NenData data) {
		return stat(data, damageMin, damageMax, damageBoost);
	}

	/** Raio final em blocos. */
	public double radius(NenData data) {
		return stat(data, radiusMin, radiusMax, radiusBoost);
	}

	/** Duracao final em ticks. */
	public int durationTicks(NenData data) {
		return (int) Math.round(stat(data, secondsMin, secondsMax, durationBoost) * 20);
	}

	private double stat(NenData data, double min, double max, NenUpgrade boost) {
		double value = Mth.lerp(data.mastery(Technique.HATSU) / 100.0, min, max) * data.hatsuPower(branch);
		return boost != null && data.has(boost) ? value * NenRules.HATSU_BOOST : value;
	}

	/** Dano a mais nos golpes com a propriedade ligada (fracao). */
	public static double propertyBonus(NenData data) {
		return Mth.lerp(data.mastery(Technique.HATSU) / 100.0, NenRules.HATSU_PROPERTY_DAMAGE_MIN, NenRules.HATSU_PROPERTY_DAMAGE_MAX)
				* data.hatsuPower(NenType.TRANSMUTATION);
	}

	/** Dano a mais em quem esta marcado (fracao). */
	public static double markBonus(NenData data) {
		return Mth.lerp(data.mastery(Technique.HATSU) / 100.0, NenRules.HATSU_MARK_DAMAGE_MIN, NenRules.HATSU_MARK_DAMAGE_MAX)
				* data.hatsuPower(NenType.SPECIALIZATION);
	}

	/** Fracao do dano que a Adaptacao Corporal corta enquanto a propriedade estiver ligada. */
	public static double adaptation(NenData data) {
		return data.has(NenUpgrade.HATSU_ADAPTATION) ? NenRules.HATSU_ADAPTATION_REDUCTION * data.hatsuPower(NenType.TRANSMUTATION) : 0;
	}

	/** Blocos por tick do projetil. */
	public static double blastSpeed(NenData data) {
		return data.has(NenUpgrade.HATSU_SWIFT) ? NenRules.HATSU_BLAST_SPEED * NenRules.HATSU_BLAST_SWIFT : NenRules.HATSU_BLAST_SPEED;
	}

	/** Alcance do projetil em blocos. */
	public static double blastRange(NenData data) {
		return NenRules.HATSU_BLAST_RANGE * data.hatsuPower(NenType.EMISSION);
	}

	/** Parte do dano que resta depois de voar esta distancia: a aura perde forca longe do corpo. */
	public static double blastFalloff(NenData data, double distance) {
		double range = blastRange(data);
		if (range <= NenRules.HATSU_BLAST_FULL_RANGE) {
			return 1;
		}
		double lost = NenRules.HATSU_BLAST_FALLOFF * Math.clamp((distance - NenRules.HATSU_BLAST_FULL_RANGE) / (range - NenRules.HATSU_BLAST_FULL_RANGE), 0, 1);
		return 1 - (data.has(NenUpgrade.HATSU_COHESION) ? lost * NenRules.HATSU_BLAST_COHESION : lost);
	}

	public static int cost(NenData data) {
		return (int) Math.round(NenRules.HATSU_CAST_COST * (data.has(NenUpgrade.HATSU_EFFICIENCY) ? 1 - NenRules.HATSU_EFFICIENCY_DISCOUNT : 1));
	}

	public static int cooldownTicks(NenData data) {
		return (int) Math.round(Mth.lerp(data.mastery(Technique.HATSU) / 100.0, NenRules.HATSU_COOLDOWN_SLOWEST, NenRules.HATSU_COOLDOWN_FASTEST) * 20);
	}
}
