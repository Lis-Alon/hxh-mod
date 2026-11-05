package com.hxhmod.nen;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Afinidade entre os tipos de Nen: quanto cada tipo inato aproveita de cada galho do hexagono.
 * A porcentagem define o preco e o poder dos Hatsus; zero bloqueia o galho.
 */
public final class NenAffinity {
	/** Os galhos no sentido horario, a partir do topo do hexagono. */
	public static final List<NenType> HEXAGON = List.of(NenType.ENHANCEMENT, NenType.TRANSMUTATION, NenType.CONJURATION,
			NenType.SPECIALIZATION, NenType.MANIPULATION, NenType.EMISSION);

	private static final Map<NenType, Map<NenType, Integer>> TABLE = new EnumMap<>(NenType.class);

	static {
		row(NenType.ENHANCEMENT, 100, 80, 60, 0, 60, 80);
		row(NenType.TRANSMUTATION, 80, 100, 80, 0, 40, 60);
		row(NenType.CONJURATION, 60, 80, 100, 1, 60, 40);
		row(NenType.SPECIALIZATION, 40, 60, 80, 100, 80, 60);
		row(NenType.MANIPULATION, 60, 40, 60, 1, 100, 80);
		row(NenType.EMISSION, 80, 60, 40, 0, 80, 100);
	}

	private NenAffinity() {
	}

	/** Uma linha da tabela, com os valores na ordem de {@link #HEXAGON}. */
	private static void row(NenType innate, int... percents) {
		Map<NenType, Integer> row = new EnumMap<>(NenType.class);
		for (int i = 0; i < percents.length; i++) {
			row.put(HEXAGON.get(i), percents[i]);
		}
		TABLE.put(innate, row);
	}

	/** Afinidade de 0 a 100 de quem nasceu com este tipo; sem tipo, ou fora do hexagono, e zero. */
	public static int percent(NenType innate, NenType branch) {
		Map<NenType, Integer> row = TABLE.get(innate);
		return row == null ? 0 : row.getOrDefault(branch, 0);
	}

	/** Multiplicador do dano, da duracao e da area de um Hatsu deste galho. */
	public static double power(NenType innate, NenType branch) {
		return percent(innate, branch) / 100.0;
	}

	/** Afinidade zero: os nos nao podem ser comprados e o galho fica todo escuro. */
	public static boolean blocked(NenType innate, NenType branch) {
		return percent(innate, branch) == 0;
	}

	/** Preco inversamente proporcional a afinidade. Sem afinidade nao ha preco: vale o preco base. */
	public static int cost(int baseCost, int percent) {
		return percent <= 0 ? baseCost : (int) Math.ceil(baseCost * 100.0 / percent);
	}
}
