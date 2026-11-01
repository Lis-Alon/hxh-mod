package com.hxhmod.nen;

/** Numeros de balanceamento do Nen, todos num lugar so para facilitar ajustes. */
public final class NenRules {
	// Despertar: segundos agachado e parado
	public static final int MEDITATION_SECONDS = 60;

	// Treino (em segundos com a tecnica ativa) para liberar a proxima
	public static final int ZETSU_UNLOCK_TEN_XP = 300;
	public static final int REN_UNLOCK_ZETSU_XP = 300;
	public static final int REN_MASTERY_XP = 600;

	// Nivel e aura
	public static final int XP_PER_LEVEL = 600;
	public static final int BASE_MAX_AURA = 100;
	public static final int AURA_PER_LEVEL = 20;

	// Aura por segundo
	public static final int REGEN = 2;
	public static final int ZETSU_REGEN = 6;
	public static final int REN_DRAIN = 3;

	// Videncia com o copo e a folha
	public static final int DIVINATION_USE_TICKS = 100;
	public static final int DIVINATION_AURA_COST = 30;
	public static final int DIVINATION_MIN_GAIN = 5;
	public static final int DIVINATION_MAX_GAIN = 12;
	public static final int DIVINATION_HINT_AT = 35;
	public static final int DIVINATION_REN_XP = 10;

	private NenRules() {
	}
}
