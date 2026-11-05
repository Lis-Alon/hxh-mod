package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tudo que o mod sabe sobre o Nen de um jogador.
 * O tipo e sorteado ao despertar, mas so aparece para o jogador quando {@code revealed} for verdadeiro.
 *
 * @param xp        experiencia total de Nen (define o nivel de proficiencia)
 * @param active    tecnicas ligadas agora (varias juntas; o Zetsu so funciona sozinho)
 * @param training  segundos que cada tecnica ficou ativa
 * @param attributeXp XP de treino de cada atributo
 * @param unlocked  tecnicas desbloqueadas
 * @param upgrades  funcionalidades compradas
 * @param bonusPoints pontos extras dados por comando (somam aos pontos ganhos por nivel)
 * @param granted   tecnicas liberadas por comando, que nao custaram pontos
 * @param grantedUpgrades funcionalidades liberadas por comando, que nao custaram pontos
 * @param blocked   tecnicas bloqueadas por comando: nao sao aprendidas sozinhas ate liberar de novo
 * @param intensity intensidade escolhida para as tecnicas que podem ser reguladas
 */
public record NenData(boolean awakened, NenType type, boolean revealed, int aura, int meditation, int divination,
		Set<Technique> active, int xp, Map<Technique, Integer> training, Map<NenStat, Double> attributeXp,
		Set<Technique> unlocked, Set<NenUpgrade> upgrades, int bonusPoints, Set<Technique> granted,
		Set<NenUpgrade> grantedUpgrades, Set<Technique> blocked, Map<Technique, Integer> intensity) {
	public static final NenData DEFAULT = new NenData(false, NenType.NONE, false, 0, 0, 0, Set.of(), 0,
			Map.of(), Map.of(), Set.of(), Set.of(), 0, Set.of(), Set.of(), Set.of(), Map.of());

	// O construtor de codec aceita ate 16 campos, entao a intensidade e os pontos antigos ficam num grupo por fora
	private static final MapCodec<NenData> CORE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("awakened", false).forGetter(NenData::awakened),
			NenType.CODEC.optionalFieldOf("type", NenType.NONE).forGetter(NenData::type),
			Codec.BOOL.optionalFieldOf("revealed", false).forGetter(NenData::revealed),
			Codec.INT.optionalFieldOf("aura", 0).forGetter(NenData::aura),
			Codec.INT.optionalFieldOf("meditation", 0).forGetter(NenData::meditation),
			Codec.INT.optionalFieldOf("divination", 0).forGetter(NenData::divination),
			// Antes era uma tecnica so; mundos antigos ainda carregam
			Codec.withAlternative(Technique.CODEC.listOf().<Set<Technique>>xmap(Set::copyOf, List::copyOf),
							Technique.CODEC, technique -> technique == Technique.NONE ? Set.of() : Set.of(technique))
					.optionalFieldOf("active", Set.of()).forGetter(NenData::active),
			Codec.INT.optionalFieldOf("xp", 0).forGetter(NenData::xp),
			Codec.unboundedMap(Technique.CODEC, Codec.INT).optionalFieldOf("training", Map.of()).forGetter(NenData::training),
			Codec.unboundedMap(NenStat.CODEC, Codec.DOUBLE).optionalFieldOf("attribute_xp", Map.of()).forGetter(NenData::attributeXp),
			Technique.CODEC.listOf().<Set<Technique>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("unlocked", Set.of()).forGetter(NenData::unlocked),
			NenUpgrade.CODEC.listOf().<Set<NenUpgrade>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("upgrades", Set.of()).forGetter(NenData::upgrades),
			Codec.INT.optionalFieldOf("bonus_points", 0).forGetter(NenData::bonusPoints),
			Technique.CODEC.listOf().<Set<Technique>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("granted", Set.of()).forGetter(NenData::granted),
			NenUpgrade.CODEC.listOf().<Set<NenUpgrade>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("granted_upgrades", Set.of()).forGetter(NenData::grantedUpgrades),
			Technique.CODEC.listOf().<Set<Technique>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("blocked", Set.of()).forGetter(NenData::blocked)
	).apply(instance, (awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
			upgrades, bonusPoints, granted, grantedUpgrades, blocked) -> new NenData(awakened, type, revealed, aura, meditation,
			divination, active, xp, training, attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, Map.of())));

	public static final Codec<NenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			CORE_CODEC.forGetter(data -> data),
			Codec.unboundedMap(Technique.CODEC, Codec.INT).optionalFieldOf("intensity", Map.of()).forGetter(NenData::intensity),
			// Mundos antigos guardavam pontos distribuidos: so se le, nao se grava mais
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("stats", Map.of()).forGetter(data -> Map.of())
	).apply(instance, (data, intensity, legacyStats) -> data.withIntensity(intensity).withLegacyStats(legacyStats)));

	public static final StreamCodec<ByteBuf, NenData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	// ---- Nivel e pontos ----

	public int level() {
		return levelProgress()[0];
	}

	/** XP acumulada dentro do nivel atual (0 no nivel maximo). */
	public int xpIntoLevel() {
		return levelProgress()[1];
	}

	/** {nivel, xp dentro do nivel}, descontando a XP de cada nivel ja passado. */
	private int[] levelProgress() {
		int level = 1;
		int remaining = xp;
		while (level < NenRules.MAX_LEVEL && remaining >= NenRules.xpToNext(level)) {
			remaining -= NenRules.xpToNext(level);
			level++;
		}
		return new int[] {level, level >= NenRules.MAX_LEVEL ? 0 : remaining};
	}

	public boolean maxLevel() {
		return level() >= NenRules.MAX_LEVEL;
	}

	public int pointsSpent() {
		// Os pontos so compram funcionalidades: atributos e tecnicas vem do treino, e o que foi liberado por comando e de graca
		return upgrades.stream().filter(upgrade -> !grantedUpgrades.contains(upgrade)).mapToInt(upgrade -> upgrade.costFor(type)).sum();
	}

	public int pointsAvailable() {
		return NenRules.pointsEarned(level()) + bonusPoints - pointsSpent();
	}

	// ---- Aura e atributos ----

	/** Nivel do atributo (0 a 100), que vem so do treino dele. */
	public int attributeLevel(NenStat stat) {
		return Math.min(NenRules.MAX_ATTRIBUTE, (int) Math.sqrt(attributeXp.getOrDefault(stat, 0.0) / NenRules.ATTRIBUTE_XP_FACTOR));
	}

	/** Porcentagem do caminho ate o proximo nivel do atributo (100 no maximo). */
	public int attributeProgress(NenStat stat) {
		int level = attributeLevel(stat);
		if (level >= NenRules.MAX_ATTRIBUTE) {
			return 100;
		}
		double from = attributeXpFor(level);
		return (int) ((attributeXp.getOrDefault(stat, 0.0) - from) * 100 / (attributeXpFor(level + 1) - from));
	}

	private static double attributeXpFor(int level) {
		return level * level * NenRules.ATTRIBUTE_XP_FACTOR;
	}

	public int maxAura() {
		if (!awakened) {
			return 0;
		}
		return NenRules.BASE_MAX_AURA + NenRules.AURA_PER_LEVEL * (level() - 1)
				+ (int) (attributeLevel(NenStat.AURA_CAPACITY) * NenRules.CAPACITY_AURA);
	}

	/** Aura recuperada por segundo sem nenhuma tecnica que gaste aura (pode ser fracionada). */
	public double auraRegen() {
		return auraRegen(false);
	}

	/** @param resting parado, sem andar (o Zetsu recupera mais em repouso) */
	public double auraRegen(boolean resting) {
		double regen = NenRules.REGEN + attributeLevel(NenStat.AURA_FLOW) * NenRules.FLOW_REGEN;
		if (active.contains(Technique.TEN) && has(NenUpgrade.TEN_FLOW)) {
			regen *= 1 + tenFlowBonus();
		}
		if (active.contains(Technique.ZETSU)) {
			regen *= 1 + zetsuRegenBonus();
			if (resting) {
				regen *= has(NenUpgrade.ZETSU_REST) ? NenRules.ZETSU_DEEP_REST_MULTIPLIER : NenRules.ZETSU_REST_MULTIPLIER;
			}
		}
		return regen;
	}

	// ---- Dominio das tecnicas ----

	/** Dominio da tecnica (0 a 100), que vem do treino dela e nao do nivel geral. */
	public int mastery(Technique technique) {
		return Math.min(NenRules.MAX_MASTERY, (int) Math.sqrt(training(technique) / NenRules.MASTERY_SECONDS_FACTOR));
	}

	/** Porcentagem do caminho ate o proximo ponto de dominio (100 no maximo). */
	public int masteryProgress(Technique technique) {
		int mastery = mastery(technique);
		if (mastery >= NenRules.MAX_MASTERY) {
			return 100;
		}
		double from = mastery * mastery * NenRules.MASTERY_SECONDS_FACTOR;
		double to = (mastery + 1) * (mastery + 1) * NenRules.MASTERY_SECONDS_FACTOR;
		return (int) ((training(technique) - from) * 100 / (to - from));
	}

	/** Reducao de dano do Ten (fracao do golpe). */
	public double tenDefense() {
		return NenRules.TEN_DEFENSE_BASE + NenRules.TEN_DEFENSE_PER_MASTERY * mastery(Technique.TEN);
	}

	/** Bonus da Fluidez da Aura na recuperacao (fracao). */
	public double tenFlowBonus() {
		return NenRules.TEN_FLOW_BASE + NenRules.TEN_FLOW_PER_MASTERY * mastery(Technique.TEN);
	}

	/** Cura do Corpo Jovem por segundo, em fracao da vida maxima. */
	public double tenYouthHeal() {
		return NenRules.TEN_YOUTH_BASE + NenRules.TEN_YOUTH_PER_MASTERY * mastery(Technique.TEN);
	}

	/** Bonus do Zetsu na recuperacao de aura (fracao), antes do bonus de repouso. */
	public double zetsuRegenBonus() {
		return NenRules.ZETSU_REGEN_BASE + NenRules.ZETSU_REGEN_PER_MASTERY * mastery(Technique.ZETSU);
	}

	/** Multiplicador de quao visivel o jogador fica para os mobs em Zetsu. */
	public double zetsuVisibility() {
		return NenRules.ZETSU_VISIBILITY_BASE - NenRules.ZETSU_VISIBILITY_PER_MASTERY * mastery(Technique.ZETSU);
	}

	/** Custo de aura de uma aplicacao do Gyo, com o desconto da Transferencia Eficiente. */
	public int gyoCost() {
		double discount = has(NenUpgrade.GYO_INSTINCT)
				? NenRules.GYO_TRANSFER_BASE + NenRules.GYO_TRANSFER_PER_MASTERY * mastery(Technique.GYO) : 0;
		return (int) Math.round(NenRules.GYO_COST * (1 - discount));
	}

	/** Recarga de cada aplicacao do Gyo, em ticks: 20 s com proficiencia 0% ate 5 s com 100%. */
	public int gyoCooldownTicks() {
		double range = NenRules.GYO_COOLDOWN_SLOWEST_SECONDS - NenRules.GYO_COOLDOWN_FASTEST_SECONDS;
		double seconds = NenRules.GYO_COOLDOWN_SLOWEST_SECONDS - range * mastery(Technique.GYO) / NenRules.MAX_MASTERY;
		return (int) Math.round(seconds * 20);
	}

	/** Multiplicador da Concentracao Precisa sobre a potencia das aplicacoes. */
	private double gyoPower() {
		return 1 + (has(NenUpgrade.GYO_PRECISE)
				? NenRules.GYO_PRECISE_BASE + NenRules.GYO_PRECISE_PER_MASTERY * mastery(Technique.GYO) : 0);
	}

	/** Bonus de dano do super soco (fracao). */
	public double gyoPunch() {
		return (NenRules.GYO_PUNCH_BASE + NenRules.GYO_PUNCH_PER_MASTERY * mastery(Technique.GYO)) * gyoPower();
	}

	/** Reducao de dano da super defesa (fracao), sem chegar a invulnerabilidade. */
	public double gyoGuard() {
		return Math.min(NenRules.GYO_GUARD_MAX,
				(NenRules.GYO_GUARD_BASE + NenRules.GYO_GUARD_PER_MASTERY * mastery(Technique.GYO)) * gyoPower());
	}

	/** Impulso vertical do super pulo. */
	public double gyoJump() {
		return Math.min(NenRules.GYO_JUMP_MAX,
				(NenRules.GYO_JUMP_BASE + NenRules.GYO_JUMP_PER_MASTERY * mastery(Technique.GYO)) * gyoPower());
	}

	/** Velocidade do super dash. */
	public double gyoDash() {
		return Math.min(NenRules.GYO_DASH_MAX,
				(NenRules.GYO_DASH_BASE + NenRules.GYO_DASH_PER_MASTERY * mastery(Technique.GYO)) * gyoPower());
	}

	// ---- Intensidade ----

	public int intensityOf(Technique technique) {
		return Math.clamp(intensity.getOrDefault(technique, technique.defaultIntensity()), 1, technique.maxIntensity());
	}

	/** Passa para a proxima intensidade, voltando para a primeira depois da ultima. */
	public NenData cycledIntensity(Technique technique) {
		Map<Technique, Integer> newIntensity = new EnumMap<>(Technique.class);
		newIntensity.putAll(intensity);
		newIntensity.put(technique, intensityOf(technique) % technique.maxIntensity() + 1);
		return withIntensity(Map.copyOf(newIntensity));
	}

	private NenData withIntensity(Map<Technique, Integer> newIntensity) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				upgrades, bonusPoints, granted, grantedUpgrades, blocked, newIntensity);
	}

	private double byIntensity(Technique technique, double[] values) {
		return values[intensityOf(technique) - 1];
	}

	/** Valor de uma funcionalidade que cresce com o dominio; zero se nao foi comprada. */
	private double upgradeScale(NenUpgrade upgrade, double base, double perMastery) {
		return has(upgrade) ? base + perMastery * mastery(upgrade.technique()) : 0;
	}

	// ---- Hatsus dos galhos do hexagono ----

	/** Multiplicador dos Hatsus deste galho: a afinidade do tipo inato com ele. */
	public double hatsuPower(NenType branch) {
		return NenAffinity.power(type, branch);
	}

	/** Vitalidade Reforcada: vida maxima a mais (fracao). */
	public double hatsuVitality() {
		return has(NenUpgrade.HATSU_VITALITY)
				? (NenRules.HATSU_VITALITY_BASE + NenRules.HATSU_VITALITY_PER_MASTERY * mastery(Technique.HATSU)) * hatsuPower(NenType.ENHANCEMENT) : 0;
	}

	/** Impacto Natural: dano corpo a corpo a mais (fracao). */
	public double hatsuImpact() {
		return has(NenUpgrade.HATSU_IMPACT)
				? (NenRules.HATSU_IMPACT_BASE + NenRules.HATSU_IMPACT_PER_MASTERY * mastery(Technique.HATSU)) * hatsuPower(NenType.ENHANCEMENT) : 0;
	}

	// ---- Ren e Ken ----

	/** Bonus de dano do Ren (fracao). */
	public double renDamage() {
		return byIntensity(Technique.REN, NenRules.REN_DAMAGE)
				* (1 + upgradeScale(NenUpgrade.REN_SURGE, NenRules.REN_POWER_BASE, NenRules.REN_POWER_PER_MASTERY))
				* (1 + attributeLevel(NenStat.ATTACK_POWER) * NenRules.ATTACK_IMPACT);
	}

	/** Resistencia corporal do Ren (fracao do golpe). */
	public double renDefense() {
		return byIntensity(Technique.REN, NenRules.REN_DEFENSE);
	}

	/** Reducao de dano do Ken (fracao do golpe). */
	public double kenDefense() {
		return Math.min(NenRules.KEN_DEFENSE_MAX, byIntensity(Technique.KEN, NenRules.KEN_DEFENSE)
				+ upgradeScale(NenUpgrade.KEN_IRON, NenRules.KEN_DENSITY_BASE, NenRules.KEN_DENSITY_PER_MASTERY));
	}

	// ---- In ----

	/** Aura por segundo que as outras tecnicas estao liberando. */
	public double inReleased() {
		return active.stream().filter(technique -> technique != Technique.IN).mapToDouble(this::auraDrain).sum();
	}

	/** Quanta aura liberada o In consegue esconder. */
	public double inCapacity() {
		return (NenRules.IN_CAPACITY_BASE + NenRules.IN_CAPACITY_PER_MASTERY * mastery(Technique.IN))
				* (1 + upgradeScale(NenUpgrade.IN_SHADOW, NenRules.IN_SUSTAIN_BASE, NenRules.IN_SUSTAIN_PER_MASTERY));
	}

	/** Fracao da aura que os outros percebem: tudo sem In, e com ele so o que passa da capacidade. */
	public double inLeak() {
		if (!isActive(Technique.IN)) {
			return 1;
		}
		double released = inReleased();
		return released <= inCapacity() ? 0 : (released - inCapacity()) / released;
	}

	// ---- Ko ----

	/** Ticks para chegar na carga maxima, com a Preparacao Refinada. */
	public int koChargeTicks() {
		return Math.max(NenRules.KO_MIN_CHARGE_TICKS, (int) Math.round(NenRules.KO_CHARGE_TICKS
				* (1 - upgradeScale(NenUpgrade.KO_FOCUS, NenRules.KO_FOCUS_BASE, NenRules.KO_FOCUS_PER_MASTERY))));
	}

	/** Aura comprometida pelo golpe com esta carga (0 a 1). */
	public double koCost(double charge) {
		return NenRules.KO_COST_MIN + (NenRules.KO_COST_MAX - NenRules.KO_COST_MIN) * charge;
	}

	/** Bonus de dano do golpe com esta carga (0 a 1). */
	public double koDamage(double charge) {
		return (NenRules.KO_DAMAGE_MIN + (NenRules.KO_DAMAGE_MAX - NenRules.KO_DAMAGE_MIN) * charge)
				* (1 + NenRules.KO_DAMAGE_PER_MASTERY * mastery(Technique.KO)
				+ upgradeScale(NenUpgrade.KO_ABSOLUTE, NenRules.KO_ABSOLUTE_BASE, NenRules.KO_ABSOLUTE_PER_MASTERY))
				* (1 + attributeLevel(NenStat.ATTACK_POWER) * NenRules.ATTACK_IMPACT);
	}

	// ---- Ryu ----

	/** Ticks para a aura chegar na postura pedida, com a Transferencia Rapida. */
	public int ryuTransitionTicks() {
		return Math.max(NenRules.RYU_MIN_TRANSITION_TICKS, (int) Math.round(NenRules.RYU_TRANSITION_TICKS
				* (1 - upgradeScale(NenUpgrade.RYU_MASTER, NenRules.RYU_QUICK_BASE, NenRules.RYU_QUICK_PER_MASTERY))));
	}

	/** Aura gasta a cada redistribuicao, com o Fluxo Eficiente. */
	public int ryuShiftCost() {
		return Math.max(1, (int) Math.round(NenRules.RYU_SHIFT_COST
				* (1 - upgradeScale(NenUpgrade.RYU_FLOW, NenRules.RYU_FLOW_BASE, NenRules.RYU_FLOW_PER_MASTERY))));
	}

	// ---- Shu ----

	private double shuPower() {
		return 1 + upgradeScale(NenUpgrade.SHU_EDGE, NenRules.SHU_POWER_BASE, NenRules.SHU_POWER_PER_MASTERY);
	}

	/** Quanto a proficiencia multiplica todos os beneficios do Shu. */
	private double shuMastery() {
		return NenRules.SHU_MASTERY_LOWEST
				+ (NenRules.SHU_MASTERY_HIGHEST - NenRules.SHU_MASTERY_LOWEST) * mastery(Technique.SHU) / NenRules.MAX_MASTERY;
	}

	/** Dano a mais de qualquer ferramenta ou arma revestida. */
	public double shuToolDamage() {
		return byIntensity(Technique.SHU, NenRules.SHU_TOOL_DAMAGE) * shuMastery() * shuPower();
	}

	public double shuCommonDamage() {
		return byIntensity(Technique.SHU, NenRules.SHU_COMMON_DAMAGE) * shuMastery();
	}

	/** Velocidade de quebrar blocos a mais. */
	public double shuMiningSpeed() {
		return byIntensity(Technique.SHU, NenRules.SHU_MINING_SPEED) * shuMastery() * shuPower();
	}

	/** Velocidade de ataque a mais. */
	public double shuAttackSpeed() {
		return byIntensity(Technique.SHU, NenRules.SHU_ATTACK_SPEED) * shuMastery() * shuPower();
	}

	public double shuShieldDefense() {
		return Math.min(NenRules.SHU_SHIELD_DEFENSE_MAX, byIntensity(Technique.SHU, NenRules.SHU_SHIELD_DEFENSE) * shuMastery() * shuPower());
	}

	/** Chance de cada ponto de desgaste ser evitado. */
	public double shuWearSave() {
		return Math.min(NenRules.SHU_WEAR_SAVE_MAX, byIntensity(Technique.SHU, NenRules.SHU_WEAR_SAVE) * shuMastery()
				+ upgradeScale(NenUpgrade.SHU_EDGE, NenRules.SHU_WEAR_BASE, NenRules.SHU_WEAR_PER_MASTERY));
	}

	// ---- En ----

	/** Maior diametro (blocos) que o En alcanca agora, com a Expansao Territorial. */
	public double enMaxSize() {
		int mastery = Math.max(1, mastery(Technique.EN));
		double size = NenRules.EN_MIN_SIZE + (NenRules.EN_MAX_SIZE - NenRules.EN_MIN_SIZE) * (mastery - 1) / (NenRules.MAX_MASTERY - 1);
		return size * (1 + upgradeScale(NenUpgrade.EN_WIDE, NenRules.EN_WIDE_BASE, NenRules.EN_WIDE_PER_MASTERY))
				* (1 + attributeLevel(NenStat.PERCEPTION) * NenRules.PERCEPTION_RADIUS);
	}

	/** Diametro (blocos) da esfera do En, na intensidade escolhida. */
	public double enSize() {
		double max = enMaxSize();
		return Math.min(max, Math.max(NenRules.EN_MIN_CHOSEN_SIZE, max * intensityOf(Technique.EN) / Technique.EN.maxIntensity()));
	}

	// ---- Gasto de aura ----

	/** Aura gasta por segundo pela tecnica, com a intensidade e os descontos das funcionalidades (pode ser fracionada). */
	public double auraDrain(Technique technique) {
		return rawDrain(technique) * (1 - attributeLevel(NenStat.AURA_FLOW) * NenRules.FLOW_UPKEEP);
	}

	private double rawDrain(Technique technique) {
		return switch (technique) {
			case REN -> byIntensity(technique, NenRules.REN_DRAIN)
					* (1 - upgradeScale(NenUpgrade.REN_CONTROL, NenRules.REN_CONTROL_BASE, NenRules.REN_CONTROL_PER_MASTERY));
			case KEN -> byIntensity(technique, NenRules.KEN_DRAIN)
					* (1 - upgradeScale(NenUpgrade.KEN_ENDURANCE, NenRules.KEN_ENDURANCE_BASE, NenRules.KEN_ENDURANCE_PER_MASTERY));
			case SHU -> byIntensity(technique, NenRules.SHU_DRAIN)
					* (1 - upgradeScale(NenUpgrade.SHU_COAT, NenRules.SHU_COAT_BASE, NenRules.SHU_COAT_PER_MASTERY));
			case EN -> (NenRules.EN_DRAIN_BASE + NenRules.EN_DRAIN_PER_BLOCK * enSize() * (1 - attributeLevel(NenStat.PERCEPTION) * NenRules.PERCEPTION_UPKEEP))
					* (1 - upgradeScale(NenUpgrade.EN_CONTROL, NenRules.EN_CONTROL_BASE, NenRules.EN_CONTROL_PER_MASTERY));
			case IN -> (NenRules.IN_DRAIN_BASE + NenRules.IN_DRAIN_PER_HIDDEN * Math.min(inReleased(), inCapacity()))
					* (1 - upgradeScale(NenUpgrade.IN_CONTROL, NenRules.IN_CONTROL_BASE, NenRules.IN_CONTROL_PER_MASTERY));
			default -> technique.baseAuraDrain();
		};
	}

	public boolean isActive(Technique technique) {
		return active.contains(technique);
	}

	/** Aura gasta por segundo somando todas as tecnicas ligadas. */
	public double totalDrain() {
		return active.stream().mapToDouble(this::auraDrain).sum();
	}

	/** Gasto por segundo com a Eficiencia de Aura, que desconta uma unica vez sobre o total (pode ser fracionado). */
	public double netDrain() {
		return totalDrain() * (has(NenUpgrade.HATSU_EFFICIENCY) ? 1 - NenRules.HATSU_EFFICIENCY_DISCOUNT : 1);
	}

	public int training(Technique technique) {
		return training.getOrDefault(technique, 0);
	}

	// ---- Desbloqueios ----

	public boolean isUnlocked(Technique technique) {
		return technique == Technique.NONE || unlocked.contains(technique);
	}

	public boolean has(NenUpgrade upgrade) {
		return upgrades.contains(upgrade);
	}

	/** Tem a proficiencia pedida em todas as tecnicas base desta (os principios nao pedem nada). */
	public boolean meetsRequirements(Technique technique) {
		return technique.requirements().stream().allMatch(requirement -> mastery(requirement.technique()) >= requirement.mastery());
	}

	/** Quanto ja cumpriu dos requisitos, de 0 a 100 (media de cada um, limitado ao pedido). */
	public int requirementProgress(Technique technique) {
		List<Technique.Requirement> requirements = technique.requirements();
		if (requirements.isEmpty()) {
			return 100;
		}
		double sum = requirements.stream()
				.mapToDouble(requirement -> Math.min(1.0, mastery(requirement.technique()) / (double) requirement.mastery()))
				.sum();
		return (int) (sum * 100 / requirements.size());
	}

	/** Despertou, ainda nao tem a tecnica, nao foi bloqueada por comando e ja cumpre os requisitos. */
	public boolean canLearn(Technique technique) {
		return awakened && technique != Technique.NONE && !isUnlocked(technique) && !blocked.contains(technique)
				&& meetsRequirements(technique);
	}

	/** Motivo de nao poder comprar a funcionalidade agora, ou null se puder. */
	public UnlockProblem upgradeProblem(NenUpgrade upgrade) {
		if (has(upgrade)) {
			return UnlockProblem.ALREADY_UNLOCKED;
		}
		if (!isUnlocked(upgrade.technique())) {
			return UnlockProblem.PREREQUISITES;
		}
		if (!upgrade.availableTo(this)) {
			return UnlockProblem.TYPE;
		}
		if (upgrade.requires() != null && !has(upgrade.requires())) {
			return UnlockProblem.PREREQUISITES;
		}
		if (level() < upgrade.minLevel()) {
			return UnlockProblem.LEVEL;
		}
		if (pointsAvailable() < upgrade.costFor(type)) {
			return UnlockProblem.POINTS;
		}
		return null;
	}

	public enum UnlockProblem {
		ALREADY_UNLOCKED, LEVEL, PREREQUISITES, POINTS, TYPE
	}

	// ---- Copias modificadas ----

	public NenData awaken(NenType rolledType) {
		// Ao despertar ja vem os quatro principios: Ten, Zetsu, Ren e Hatsu
		return new NenData(true, rolledType, false, NenRules.BASE_MAX_AURA, meditation, divination, Set.of(), xp,
				training, attributeXp, Set.copyOf(Technique.BASE), upgrades, bonusPoints, granted, grantedUpgrades, Set.of(), intensity);
	}

	public NenData withMeditation(int newMeditation) {
		return new NenData(awakened, type, revealed, aura, newMeditation, divination, active, xp, training, attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData withAura(int newAura) {
		return new NenData(awakened, type, revealed, Math.clamp(newAura, 0, maxAura()), meditation, divination, active, xp,
				training, attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData withActive(Set<Technique> newActive) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, Set.copyOf(newActive), xp, training,
				attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	/** Deixa so esta tecnica ligada (NONE desliga tudo). */
	public NenData withActive(Technique only) {
		return withActive(only == Technique.NONE ? Set.of() : Set.of(only));
	}

	/**
	 * Liga ou desliga a tecnica. O Zetsu fecha os nos de aura, entao desliga todas as outras;
	 * ligar qualquer outra (que libera aura) encerra o Zetsu. O Ryu redistribui o Ken, entao um depende do outro.
	 */
	public NenData toggled(Technique technique) {
		if (active.contains(technique)) {
			Set<Technique> off = without(active, technique, Technique.class);
			return withActive(technique == Technique.KEN ? without(off, Technique.RYU, Technique.class) : off);
		}
		if (technique == Technique.ZETSU) {
			return withActive(Technique.ZETSU);
		}
		Set<Technique> on = with(without(active, Technique.ZETSU, Technique.class), technique, Technique.class);
		return withActive(technique == Technique.RYU ? with(on, Technique.KEN, Technique.class) : on);
	}

	public NenData addXp(int amount) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp + amount, training, attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	/** Coloca o treino exatamente no comeco desta proficiencia. */
	public NenData withMastery(Technique technique, int mastery) {
		Map<Technique, Integer> newTraining = new EnumMap<>(Technique.class);
		newTraining.putAll(training);
		newTraining.put(technique, (int) Math.ceil(mastery * mastery * NenRules.MASTERY_SECONDS_FACTOR));
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, Map.copyOf(newTraining),
				attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData addTraining(Technique technique, int seconds) {
		Map<Technique, Integer> newTraining = new EnumMap<>(Technique.class);
		newTraining.putAll(training);
		newTraining.merge(technique, seconds, Integer::sum);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, Map.copyOf(newTraining),
				attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData withDivination(int newDivination) {
		int clamped = Math.min(newDivination, 100);
		return new NenData(awakened, type, revealed || clamped >= 100, aura, meditation, clamped, active, xp, training,
				attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	/** Soma XP de treino no atributo, sem passar do que o nivel maximo pede. */
	public NenData addAttributeXp(NenStat stat, double amount) {
		if (amount <= 0) {
			return this;
		}
		return withAttributeXp(stat, Math.min(attributeXpFor(NenRules.MAX_ATTRIBUTE), attributeXp.getOrDefault(stat, 0.0) + amount));
	}

	/** Poe o atributo no comeco do nivel pedido. */
	public NenData withAttributeLevel(NenStat stat, int level) {
		return withAttributeXp(stat, attributeXpFor(level));
	}

	private NenData withAttributeXp(NenStat stat, double value) {
		Map<NenStat, Double> newXp = new EnumMap<>(NenStat.class);
		newXp.putAll(attributeXp);
		newXp.put(stat, value);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training,
				Map.copyOf(newXp), unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	/**
	 * Mundos de antes: cada atributo comeca no nivel equivalente aos pontos que tinha (mesma fracao do limite antigo),
	 * e esses pontos voltam a ficar livres para as funcionalidades. A velocidade de ataque agora faz parte da Agilidade.
	 */
	private NenData withLegacyStats(Map<String, Integer> legacy) {
		NenData result = this;
		for (Map.Entry<String, Integer> entry : legacy.entrySet()) {
			record Old(NenStat stat, int maxPoints) {
			}
			Old old = switch (entry.getKey()) {
				case "vitality" -> new Old(NenStat.VITALITY, 40);
				case "attack_power" -> new Old(NenStat.ATTACK_POWER, 40);
				case "attack_speed" -> new Old(NenStat.AGILITY, 25);
				case "protection" -> new Old(NenStat.PROTECTION, 40);
				case "agility" -> new Old(NenStat.AGILITY, 30);
				case "aura_capacity" -> new Old(NenStat.AURA_CAPACITY, 50);
				case "aura_flow" -> new Old(NenStat.AURA_FLOW, 20);
				default -> null;
			};
			if (old != null) {
				int level = Math.min(NenRules.MAX_ATTRIBUTE, entry.getValue() * NenRules.MAX_ATTRIBUTE / old.maxPoints());
				if (level > result.attributeLevel(old.stat())) {
					result = result.withAttributeLevel(old.stat(), level);
				}
			}
		}
		return result;
	}

	public NenData unlock(Technique technique) {
		Set<Technique> newUnlocked = EnumSet.of(technique);
		newUnlocked.addAll(unlocked);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp,
				Set.copyOf(newUnlocked), upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData buy(NenUpgrade upgrade) {
		Set<NenUpgrade> newUpgrades = EnumSet.of(upgrade);
		newUpgrades.addAll(upgrades);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				Set.copyOf(newUpgrades), bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	/** XP total no comeco do nivel informado. */
	public static int xpForLevel(int level) {
		int total = 0;
		for (int i = 1; i < Math.min(level, NenRules.MAX_LEVEL); i++) {
			total += NenRules.xpToNext(i);
		}
		return total;
	}

	public NenData withXp(int newXp) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, Math.max(newXp, 0), training,
				attributeXp, unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData withType(NenType newType) {
		return new NenData(awakened, newType, revealed, aura, meditation, divination, active, xp, training, attributeXp,
				unlocked, upgrades, bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData withBonusPoints(int newBonus) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				upgrades, newBonus, granted, grantedUpgrades, blocked, intensity);
	}

	/** Libera por comando, mesmo sem a proficiencia pedida (e tira o bloqueio). */
	public NenData grant(Technique technique) {
		if (isUnlocked(technique)) {
			return this;
		}
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp,
				with(unlocked, technique, Technique.class), upgrades, bonusPoints, with(granted, technique, Technique.class),
				grantedUpgrades, without(blocked, technique, Technique.class), intensity);
	}

	public NenData grant(NenUpgrade upgrade) {
		if (has(upgrade)) {
			return this;
		}
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				with(upgrades, upgrade, NenUpgrade.class), bonusPoints, granted,
				with(grantedUpgrades, upgrade, NenUpgrade.class), blocked, intensity);
	}

	/**
	 * Bloqueia a tecnica de novo: some junto com as funcionalidades dela e desliga se estiver ativa.
	 * Fica bloqueada (nao e aprendida sozinha pela proficiencia) ate ser liberada por comando.
	 */
	public NenData lock(Technique technique) {
		NenData result = this;
		for (NenUpgrade upgrade : NenUpgrade.of(technique)) {
			result = result.lock(upgrade);
		}
		return new NenData(awakened, type, revealed, aura, meditation, divination,
				without(active, technique, Technique.class), xp, training, attributeXp,
				without(unlocked, technique, Technique.class), result.upgrades, bonusPoints,
				without(granted, technique, Technique.class), result.grantedUpgrades, with(blocked, technique, Technique.class), intensity);
	}

	/** Tira as funcionalidades compradas, devolvendo os pontos; as liberadas por comando ficam. */
	public NenData resetUpgrades() {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				Set.copyOf(grantedUpgrades), bonusPoints, granted, grantedUpgrades, blocked, intensity);
	}

	public NenData lock(NenUpgrade upgrade) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, attributeXp, unlocked,
				without(upgrades, upgrade, NenUpgrade.class), bonusPoints, granted,
				without(grantedUpgrades, upgrade, NenUpgrade.class), blocked, intensity);
	}

	private static <E extends Enum<E>> Set<E> with(Set<E> set, E value, Class<E> type) {
		Set<E> copy = EnumSet.noneOf(type);
		copy.addAll(set);
		copy.add(value);
		return Set.copyOf(copy);
	}

	private static <E extends Enum<E>> Set<E> without(Set<E> set, E value, Class<E> type) {
		Set<E> copy = EnumSet.noneOf(type);
		copy.addAll(set);
		copy.remove(value);
		return Set.copyOf(copy);
	}
}
