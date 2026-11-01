package com.hxhmod.nen;

import com.mojang.serialization.Codec;
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
 * @param training  segundos que cada tecnica ficou ativa
 * @param stats     pontos gastos em cada atributo
 * @param unlocked  tecnicas desbloqueadas
 * @param upgrades  funcionalidades compradas
 */
public record NenData(boolean awakened, NenType type, boolean revealed, int aura, int meditation, int divination,
		Technique active, int xp, Map<Technique, Integer> training, Map<NenStat, Integer> stats,
		Set<Technique> unlocked, Set<NenUpgrade> upgrades) {
	public static final NenData DEFAULT = new NenData(false, NenType.NONE, false, 0, 0, 0, Technique.NONE, 0,
			Map.of(), Map.of(), Set.of(), Set.of());

	public static final Codec<NenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("awakened", false).forGetter(NenData::awakened),
			NenType.CODEC.optionalFieldOf("type", NenType.NONE).forGetter(NenData::type),
			Codec.BOOL.optionalFieldOf("revealed", false).forGetter(NenData::revealed),
			Codec.INT.optionalFieldOf("aura", 0).forGetter(NenData::aura),
			Codec.INT.optionalFieldOf("meditation", 0).forGetter(NenData::meditation),
			Codec.INT.optionalFieldOf("divination", 0).forGetter(NenData::divination),
			Technique.CODEC.optionalFieldOf("active", Technique.NONE).forGetter(NenData::active),
			Codec.INT.optionalFieldOf("xp", 0).forGetter(NenData::xp),
			Codec.unboundedMap(Technique.CODEC, Codec.INT).optionalFieldOf("training", Map.of()).forGetter(NenData::training),
			Codec.unboundedMap(NenStat.CODEC, Codec.INT).optionalFieldOf("stats", Map.of()).forGetter(NenData::stats),
			Technique.CODEC.listOf().<Set<Technique>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("unlocked", Set.of()).forGetter(NenData::unlocked),
			NenUpgrade.CODEC.listOf().<Set<NenUpgrade>>xmap(Set::copyOf, List::copyOf)
					.optionalFieldOf("upgrades", Set.of()).forGetter(NenData::upgrades)
	).apply(instance, NenData::new));

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
		int spent = stats.values().stream().mapToInt(Integer::intValue).sum();
		spent += unlocked.stream().mapToInt(Technique::unlockCost).sum();
		spent += upgrades.stream().mapToInt(NenUpgrade::cost).sum();
		return spent;
	}

	public int pointsAvailable() {
		return NenRules.pointsEarned(level()) - pointsSpent();
	}

	// ---- Aura e atributos ----

	public int statPoints(NenStat stat) {
		return stats.getOrDefault(stat, 0);
	}

	public int maxAura() {
		if (!awakened) {
			return 0;
		}
		return NenRules.BASE_MAX_AURA + NenRules.AURA_PER_LEVEL * (level() - 1)
				+ (int) (statPoints(NenStat.AURA_CAPACITY) * NenStat.AURA_CAPACITY.perPoint());
	}

	/** Aura recuperada por segundo sem nenhuma tecnica que gaste aura. */
	public int auraRegen() {
		int regen = NenRules.REGEN + (int) (statPoints(NenStat.AURA_FLOW) * NenStat.AURA_FLOW.perPoint());
		if (active == Technique.TEN && has(NenUpgrade.TEN_CALM)) {
			regen += 1;
		}
		if (active == Technique.ZETSU) {
			regen *= NenRules.ZETSU_REGEN_MULTIPLIER;
		}
		return regen;
	}

	/** Aura gasta por segundo pela tecnica, ja com os descontos das funcionalidades. */
	public int auraDrain(Technique technique) {
		int drain = technique.baseAuraDrain();
		if (technique == Technique.KEN && has(NenUpgrade.KEN_ENDURANCE)) {
			drain -= 1;
		}
		if (technique == Technique.REN && has(NenUpgrade.REN_CONTROL)) {
			drain -= 1;
		}
		if (technique == Technique.GYO && has(NenUpgrade.GYO_INSTINCT)) {
			drain = 0;
		}
		if (technique == Technique.KO && has(NenUpgrade.KO_FOCUS)) {
			drain -= 2;
		}
		if (technique == Technique.RYU && has(NenUpgrade.RYU_FLOW)) {
			drain -= 1;
		}
		return Math.max(drain, 0);
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

	/** Motivo de nao poder desbloquear a tecnica agora, ou null se puder. */
	public UnlockProblem unlockProblem(Technique technique) {
		if (!awakened) {
			return UnlockProblem.NOT_AWAKENED;
		}
		if (isUnlocked(technique)) {
			return UnlockProblem.ALREADY_UNLOCKED;
		}
		if (level() < technique.minLevel()) {
			return UnlockProblem.LEVEL;
		}
		if (!technique.prerequisites().stream().allMatch(this::isUnlocked)) {
			return UnlockProblem.PREREQUISITES;
		}
		if (technique == Technique.HATSU && !revealed) {
			return UnlockProblem.TYPE_UNKNOWN;
		}
		if (pointsAvailable() < technique.unlockCost()) {
			return UnlockProblem.POINTS;
		}
		return null;
	}

	/** Motivo de nao poder comprar a funcionalidade agora, ou null se puder. */
	public UnlockProblem upgradeProblem(NenUpgrade upgrade) {
		if (has(upgrade)) {
			return UnlockProblem.ALREADY_UNLOCKED;
		}
		if (!isUnlocked(upgrade.technique())) {
			return UnlockProblem.PREREQUISITES;
		}
		if (level() < upgrade.minLevel()) {
			return UnlockProblem.LEVEL;
		}
		if (pointsAvailable() < upgrade.cost()) {
			return UnlockProblem.POINTS;
		}
		return null;
	}

	public enum UnlockProblem {
		NOT_AWAKENED, ALREADY_UNLOCKED, LEVEL, PREREQUISITES, TYPE_UNKNOWN, POINTS
	}

	// ---- Copias modificadas ----

	public NenData awaken(NenType rolledType) {
		return new NenData(true, rolledType, false, NenRules.BASE_MAX_AURA, meditation, divination, Technique.NONE, xp,
				training, stats, Set.of(Technique.TEN), upgrades);
	}

	public NenData withMeditation(int newMeditation) {
		return new NenData(awakened, type, revealed, aura, newMeditation, divination, active, xp, training, stats, unlocked, upgrades);
	}

	public NenData withAura(int newAura) {
		return new NenData(awakened, type, revealed, Math.clamp(newAura, 0, maxAura()), meditation, divination, active, xp,
				training, stats, unlocked, upgrades);
	}

	public NenData withActive(Technique newActive) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, newActive, xp, training, stats, unlocked, upgrades);
	}

	public NenData addXp(int amount) {
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp + amount, training, stats, unlocked, upgrades);
	}

	public NenData addTraining(Technique technique, int seconds) {
		Map<Technique, Integer> newTraining = new EnumMap<>(Technique.class);
		newTraining.putAll(training);
		newTraining.merge(technique, seconds, Integer::sum);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, Map.copyOf(newTraining),
				stats, unlocked, upgrades);
	}

	public NenData withDivination(int newDivination) {
		int clamped = Math.min(newDivination, 100);
		return new NenData(awakened, type, revealed || clamped >= 100, aura, meditation, clamped, active, xp, training,
				stats, unlocked, upgrades);
	}

	public NenData addStats(Map<NenStat, Integer> added) {
		Map<NenStat, Integer> newStats = new EnumMap<>(NenStat.class);
		newStats.putAll(stats);
		added.forEach((stat, amount) -> newStats.merge(stat, amount, Integer::sum));
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training,
				Map.copyOf(newStats), unlocked, upgrades);
	}

	public NenData unlock(Technique technique) {
		Set<Technique> newUnlocked = EnumSet.of(technique);
		newUnlocked.addAll(unlocked);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, stats,
				Set.copyOf(newUnlocked), upgrades);
	}

	public NenData buy(NenUpgrade upgrade) {
		Set<NenUpgrade> newUpgrades = EnumSet.of(upgrade);
		newUpgrades.addAll(upgrades);
		return new NenData(awakened, type, revealed, aura, meditation, divination, active, xp, training, stats, unlocked,
				Set.copyOf(newUpgrades));
	}
}
