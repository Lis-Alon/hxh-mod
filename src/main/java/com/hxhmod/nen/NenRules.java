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

	// Bonus internos das tecnicas (porcentagens), no lugar dos efeitos de pocao do Minecraft
	public static final double KEN_DEFENSE = 0.30;
	public static final double KEN_IRON_DEFENSE = 0.45;
	public static final double REN_DAMAGE = 0.25;
	public static final double REN_SURGE_DAMAGE = 0.45;
	public static final double IN_DAMAGE = 0.15;
	public static final double IN_SHADOW_DAMAGE = 0.30;
	// Ko: toda a aura num ponto; o resto do corpo fica sem protecao
	public static final double KO_DAMAGE = 0.80;
	public static final double KO_ABSOLUTE_DAMAGE = 1.10;
	public static final double KO_DAMAGE_TAKEN = 0.50;
	public static final double KO_SLOWNESS = 0.15;
	public static final double RYU_DAMAGE = 0.35;
	public static final double RYU_DEFENSE = 0.25;
	public static final double RYU_MASTER_DAMAGE = 0.50;
	public static final double RYU_MASTER_DEFENSE = 0.35;
	public static final double RYU_SPEED = 0.10;
	public static final double SHU_MINING = 0.60;
	public static final double SHU_EDGE_DAMAGE = 0.15;
	// Somando tudo, o dano recebido nunca cai abaixo desta fracao
	public static final double MIN_DAMAGE_TAKEN = 0.20;

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
	// Supressao Refinada: menos vazamento de aura em Zetsu
	public static final double ZETSU_SUPPRESSION_VISIBILITY = 0.85;
	// Eficiencia de Aura (Hatsu): desconto fixo no gasto de todas as tecnicas
	public static final double HATSU_EFFICIENCY_DISCOUNT = 0.05;
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

	// Raio em que o Gyo revela quem esta invisivel (blocos)
	public static final int GYO_REVEAL_RADIUS = 16;
	// Aplicacoes do Gyo: custo de aura por uso, treino extra (segundos) e potencia conforme a proficiencia
	public static final int GYO_COST = 10;
	public static final int GYO_APPLICATION_TRAINING = 20;
	public static final double GYO_PUNCH_BASE = 0.50;
	public static final double GYO_PUNCH_PER_MASTERY = 0.005;
	public static final double GYO_GUARD_BASE = 0.40;
	public static final double GYO_GUARD_PER_MASTERY = 0.002;
	public static final double GYO_GUARD_MAX = 0.80;
	public static final int GYO_GUARD_TICKS = 30;
	public static final double GYO_GUARD_OFFENSE_LOSS = 0.25;
	public static final double GYO_JUMP_BASE = 0.75;
	public static final double GYO_JUMP_PER_MASTERY = 0.0035;
	public static final double GYO_JUMP_MAX = 1.40;
	public static final int GYO_SAFE_LANDING_TICKS = 100;
	public static final double GYO_DASH_BASE = 1.40;
	public static final double GYO_DASH_PER_MASTERY = 0.008;
	public static final double GYO_DASH_MAX = 3.00;
	public static final double GYO_DASH_LIFT = 0.15;
	// O resto do corpo fica com menos aura logo depois do soco, do pulo ou do dash
	public static final double GYO_EXPOSED_DAMAGE = 0.25;
	public static final int GYO_EXPOSED_TICKS = 20;
	// Concentracao Precisa: multiplica a potencia das aplicacoes
	public static final double GYO_PRECISE_BASE = 0.25;
	public static final double GYO_PRECISE_PER_MASTERY = 0.0025;
	// Transferencia Eficiente: desconto no custo das aplicacoes
	public static final double GYO_TRANSFER_BASE = 0.25;
	public static final double GYO_TRANSFER_PER_MASTERY = 0.0025;
	// En: area quadrada de deteccao, de 5x5 (1% de proficiencia) ate 20x20 (100%)
	public static final double EN_MIN_SIZE = 5;
	public static final double EN_MAX_SIZE = 20;
	// Expansao Territorial aumenta a area
	public static final double EN_WIDE_MULTIPLIER = 1.5;

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
