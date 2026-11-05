package com.hxhmod.nen;

/** Numeros de balanceamento do Nen, todos num lugar so para facilitar ajustes. */
public final class NenRules {
	// Despertar: segundos agachado e parado
	public static final int MEDITATION_SECONDS = 60;

	// Proficiencia de Nen: nivel 1 a 100
	// Atributos treinados: nivel = raiz(XP / fator), como a proficiencia das tecnicas. O nivel 100 pede 50 000 XP.
	public static final int MAX_ATTRIBUTE = 100;
	public static final double ATTRIBUTE_XP_FACTOR = 5;
	// O que cada nivel de atributo da
	public static final double VITALITY_HEALTH = 0.8;
	public static final double VITALITY_RECOVERY = 0.01;
	public static final double CAPACITY_AURA = 5;
	public static final double FLOW_REGEN = 0.2;
	public static final double FLOW_UPKEEP = 0.003;
	public static final double ATTACK_FLAT = 0.1;
	public static final double ATTACK_PERCENT = 0.01;
	public static final double ATTACK_IMPACT = 0.005;
	public static final double PROTECTION_DEFENSE = 0.0075;
	public static final double AGILITY_MOVE = 0.0045;
	public static final double AGILITY_ATTACK = 0.0025;
	public static final double PERCEPTION_RADIUS = 0.01;
	public static final double PERCEPTION_UPKEEP = 0.004;

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

	// Ren: por intensidade (1 a 3), dano a mais, resistencia corporal e aura por segundo
	public static final double[] REN_DAMAGE = {0.15, 0.25, 0.40};
	public static final double[] REN_DEFENSE = {0.03, 0.05, 0.08};
	public static final double[] REN_DRAIN = {2, 3, 5};
	// Controle de Liberacao: desconto no gasto. Potencia Ofensiva: multiplica o bonus de dano
	public static final double REN_CONTROL_BASE = 0.15;
	public static final double REN_CONTROL_PER_MASTERY = 0.002;
	public static final double REN_POWER_BASE = 0.20;
	public static final double REN_POWER_PER_MASTERY = 0.003;
	// Ken: por intensidade (1 a 3), reducao de dano e aura por segundo
	public static final double[] KEN_DEFENSE = {0.20, 0.30, 0.40};
	public static final double[] KEN_DRAIN = {2, 3, 5};
	// Fortificacao Duradoura: desconto no gasto. Densidade Protetora: reducao a mais, com teto
	public static final double KEN_ENDURANCE_BASE = 0.20;
	public static final double KEN_ENDURANCE_PER_MASTERY = 0.002;
	public static final double KEN_DENSITY_BASE = 0.05;
	public static final double KEN_DENSITY_PER_MASTERY = 0.001;
	public static final double KEN_DEFENSE_MAX = 0.55;
	// In: gasto base mais o quanto esconde; capacidade em aura por segundo liberada
	public static final double IN_DRAIN_BASE = 1;
	public static final double IN_DRAIN_PER_HIDDEN = 0.5;
	public static final double IN_CONTROL_BASE = 0.20;
	public static final double IN_CONTROL_PER_MASTERY = 0.002;
	public static final double IN_CAPACITY_BASE = 3;
	public static final double IN_CAPACITY_PER_MASTERY = 0.07;
	public static final double IN_SUSTAIN_BASE = 0.25;
	public static final double IN_SUSTAIN_PER_MASTERY = 0.0025;
	// Ko: tempo de carga, aura do golpe e dano conforme a carga (0 a 1). O resto do corpo fica sem defesa de Nen
	public static final int KO_CHARGE_TICKS = 60;
	public static final int KO_MIN_CHARGE_TICKS = 20;
	public static final double KO_COST_MIN = 10;
	public static final double KO_COST_MAX = 40;
	public static final double KO_DAMAGE_MIN = 1.0;
	public static final double KO_DAMAGE_MAX = 3.0;
	public static final double KO_DAMAGE_PER_MASTERY = 0.002;
	public static final double KO_DAMAGE_TAKEN = 0.50;
	public static final int KO_APPLICATION_TRAINING = 30;
	// Concentracao Absoluta: multiplica o dano. Preparacao Refinada: encurta a carga
	public static final double KO_ABSOLUTE_BASE = 0.25;
	public static final double KO_ABSOLUTE_PER_MASTERY = 0.0025;
	public static final double KO_FOCUS_BASE = 0.20;
	public static final double KO_FOCUS_PER_MASTERY = 0.003;
	// Ryu: 30% a 70% da aura no ataque vira -30% a +30% de dano, e o inverso na defesa
	public static final double RYU_SWING = 1.5;
	public static final int RYU_TRANSITION_TICKS = 6;
	public static final int RYU_MIN_TRANSITION_TICKS = 2;
	public static final double RYU_SHIFT_COST = 3;
	// Transferencia Rapida: encurta a troca. Fluxo Eficiente: desconto na troca
	public static final double RYU_QUICK_BASE = 0.25;
	public static final double RYU_QUICK_PER_MASTERY = 0.004;
	public static final double RYU_FLOW_BASE = 0.25;
	public static final double RYU_FLOW_PER_MASTERY = 0.0025;
	// Shu: por intensidade (1 a 3), conforme o objeto
	public static final double[] SHU_DRAIN = {1, 2, 3};
	public static final double[] SHU_TOOL_DAMAGE = {0.15, 0.25, 0.40};
	public static final double[] SHU_COMMON_DAMAGE = {0.08, 0.12, 0.16};
	public static final double[] SHU_MINING_SPEED = {0.30, 0.60, 1.00};
	public static final double[] SHU_ATTACK_SPEED = {0.05, 0.10, 0.15};
	// Os valores do Shu vao de metade (proficiencia 0%) ao dobro (100%)
	public static final double SHU_MASTERY_LOWEST = 0.5;
	public static final double SHU_MASTERY_HIGHEST = 2.0;
	public static final double[] SHU_SHIELD_DEFENSE = {0.10, 0.15, 0.20};
	public static final double[] SHU_WEAR_SAVE = {0.30, 0.40, 0.50};
	public static final double SHU_WEAR_SAVE_MAX = 0.85;
	public static final double SHU_SHIELD_DEFENSE_MAX = 0.40;
	// Revestimento Eficiente: desconto no gasto. Reforco Estrutural: multiplica o beneficio e poupa o desgaste
	public static final double SHU_COAT_BASE = 0.20;
	public static final double SHU_COAT_PER_MASTERY = 0.002;
	public static final double SHU_POWER_BASE = 0.20;
	public static final double SHU_POWER_PER_MASTERY = 0.003;
	public static final double SHU_WEAR_BASE = 0.10;
	public static final double SHU_WEAR_PER_MASTERY = 0.002;
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
	// Hatsus da aba Hatsu: gasto de aura, recarga (de 20 s a 8 s pelo dominio) e o treino de cada uso
	public static final int HATSU_CAST_COST = 30;
	public static final double HATSU_COOLDOWN_SLOWEST = 20;
	public static final double HATSU_COOLDOWN_FASTEST = 8;
	public static final int HATSU_CAST_TRAINING = 30;
	// As passivas de cada galho aumentam em 50% a area, a duracao ou a resistencia do Hatsu ativo
	public static final double HATSU_BOOST = 1.5;
	public static final double HATSU_QUAKE_KNOCKBACK = 0.8;
	public static final double HATSU_QUAKE_LIFT = 0.35;
	public static final double HATSU_PROPERTY_DAMAGE_MIN = 0.25;
	public static final double HATSU_PROPERTY_DAMAGE_MAX = 0.5;
	public static final int HATSU_PROPERTY_SLOW_TICKS = 40;
	public static final double HATSU_ADAPTATION_REDUCTION = 0.2;
	public static final double HATSU_MARK_DAMAGE_MIN = 0.15;
	public static final double HATSU_MARK_DAMAGE_MAX = 0.3;
	// Projetil: blocos por tick e alcance; depois de 8 blocos perde ate metade do dano
	public static final double HATSU_BLAST_SPEED = 1.2;
	public static final double HATSU_BLAST_SWIFT = 1.6;
	public static final double HATSU_BLAST_RANGE = 40;
	public static final double HATSU_BLAST_FULL_RANGE = 8;
	public static final double HATSU_BLAST_FALLOFF = 0.5;
	public static final double HATSU_BLAST_COHESION = 0.25;
	// Vitalidade Reforcada e Impacto Natural, conforme o dominio do Hatsu: vida maxima e dano corpo a corpo a mais
	public static final double HATSU_VITALITY_BASE = 0.10;
	public static final double HATSU_VITALITY_PER_MASTERY = 0.002;
	public static final double HATSU_IMPACT_BASE = 0.10;
	public static final double HATSU_IMPACT_PER_MASTERY = 0.002;
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
	public static final int GYO_PUNCH_READY_TICKS = 100;
	public static final int GYO_APPLICATION_TRAINING = 20;
	public static final int GYO_COOLDOWN_SLOWEST_SECONDS = 20;
	public static final int GYO_COOLDOWN_FASTEST_SECONDS = 5;
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
	// En: esfera de deteccao com diametro de 5 (1% de proficiencia) ate 20 blocos (100%)
	public static final double EN_MIN_SIZE = 5;
	public static final double EN_MAX_SIZE = 20;
	// O jogador escolhe uma area menor para gastar menos: cada passo e 1/4 do diametro maximo
	public static final double EN_MIN_CHOSEN_SIZE = 3;
	public static final double EN_DRAIN_BASE = 1;
	public static final double EN_DRAIN_PER_BLOCK = 0.1;
	// Expansao Territorial aumenta a area. Controle Territorial: desconto no gasto
	public static final double EN_WIDE_BASE = 0.25;
	public static final double EN_WIDE_PER_MASTERY = 0.0025;
	public static final double EN_CONTROL_BASE = 0.20;
	public static final double EN_CONTROL_PER_MASTERY = 0.002;

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
