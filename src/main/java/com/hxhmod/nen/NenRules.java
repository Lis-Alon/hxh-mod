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

	// Dominio de cada tecnica (0 a 100), separado do nivel geral de Nen.
	// Dominio = raiz(segundos de treino / fator): 10 min ~ 10, 1 h ~ 24, 4 h ~ 49, 16,7 h = 100
	public static final int MAX_MASTERY = 100;
	public static final double MASTERY_SECONDS_FACTOR = 6.0;

	// Ten: reducao de dano enquanto ativo (porcentagem do golpe), cresce com o dominio
	public static final double TEN_DEFENSE_BASE = 0.10;
	public static final double TEN_DEFENSE_PER_MASTERY = 0.0015;
	// Fluidez da Aura: bonus na recuperacao de aura com o Ten ativo
	public static final double TEN_FLOW_BASE = 0.10;
	public static final double TEN_FLOW_PER_MASTERY = 0.004;
	// Corpo Jovem: cura por segundo, em fracao da vida maxima, com o Ten ativo (nao para ao levar dano)
	public static final double TEN_YOUTH_BASE = 0.005;
	public static final double TEN_YOUTH_PER_MASTERY = 0.00015;

	// Zetsu: bonus na recuperacao de aura, maior parado
	public static final double ZETSU_REGEN_BASE = 0.50;
	public static final double ZETSU_REGEN_PER_MASTERY = 0.01;
	public static final double ZETSU_REST_MULTIPLIER = 1.5;
	public static final double ZETSU_DEEP_REST_MULTIPLIER = 2.0;
	// Descanso Profundo: cura parado em Zetsu (fracao da vida maxima por segundo)
	public static final double ZETSU_DEEP_REST_HEAL = 0.005;
	// Quao visivel o jogador fica para os mobs (1 = normal). Agachar multiplica de novo.
	public static final double ZETSU_VISIBILITY_BASE = 0.60;
	public static final double ZETSU_VISIBILITY_PER_MASTERY = 0.0025;
	public static final double ZETSU_SILENT_STEP_VISIBILITY = 0.6;
	// Segundos que um ataque denuncia a posicao de quem esta em Zetsu
	public static final int ZETSU_EXPOSED_SECONDS = 5;
	// Sem a defesa de Nen: golpes de quem esta com aura liberada causam mais dano
	public static final double ZETSU_NEN_DAMAGE_MULTIPLIER = 1.5;

	// Videncia com o copo e a folha
	public static final int DIVINATION_USE_TICKS = 100;
	public static final int DIVINATION_AURA_COST = 30;
	public static final int DIVINATION_MIN_GAIN = 5;
	public static final int DIVINATION_MAX_GAIN = 12;
	public static final int DIVINATION_HINT_AT = 35;
	public static final int DIVINATION_XP = 20;
	// Segundos meditando na frente do copo colocado no chao
	public static final int DIVINATION_SECONDS = 5;

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
