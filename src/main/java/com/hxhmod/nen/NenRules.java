package com.hxhmod.nen;

/** Numeros de balanceamento do Nen, todos num lugar so para facilitar ajustes. */
public final class NenRules {
	// Despertar: segundos agachado e parado
	public static final int MEDITATION_SECONDS = 60;

	// Proficiencia de Nen: nivel 1 a 100
	public static final int MAX_LEVEL = 100;
	public static final int XP_FIRST_LEVEL = 60;
	public static final int XP_GROWTH_PER_LEVEL = 20;

	// Pontos ganhos a cada nivel, e bonus a cada 10 niveis
	public static final int POINTS_PER_LEVEL = 2;
	public static final int MILESTONE_EVERY = 10;
	public static final int MILESTONE_BONUS_POINTS = 3;

	// Aura
	public static final int BASE_MAX_AURA = 100;
	public static final int AURA_PER_LEVEL = 5;
	public static final int REGEN = 2;
	public static final int ZETSU_REGEN_MULTIPLIER = 3;

	// Videncia com o copo e a folha
	public static final int DIVINATION_USE_TICKS = 100;
	public static final int DIVINATION_AURA_COST = 30;
	public static final int DIVINATION_MIN_GAIN = 5;
	public static final int DIVINATION_MAX_GAIN = 12;
	public static final int DIVINATION_HINT_AT = 35;
	public static final int DIVINATION_XP = 20;

	// Raio do En (blocos)
	public static final int EN_RADIUS = 16;
	public static final int EN_WIDE_RADIUS = 32;
	public static final int EN_VAST_RADIUS = 50;

	private NenRules() {
	}

	/** XP necessaria para sair do nivel informado e chegar no proximo. */
	public static int xpToNext(int level) {
		return XP_FIRST_LEVEL + XP_GROWTH_PER_LEVEL * (level - 1);
	}

	public static int pointsEarned(int level) {
		return (level - 1) * POINTS_PER_LEVEL + (level / MILESTONE_EVERY) * MILESTONE_BONUS_POINTS;
	}
}
