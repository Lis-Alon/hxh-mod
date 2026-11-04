package com.hxhmod.client;

/**
 * O rastro da barra de vida: quando o jogador apanha, a barra cai na hora e o rastro fica onde ela estava,
 * espera um pouco e esvazia devagar. Um golpe novo durante a espera recomeca a contagem, como num combo.
 */
public final class DamageTrail {
	public static final float HOLD_SECONDS = 0.45F;
	public static final float DRAIN_PER_SECOND = 0.4F;

	private float trail = -1.0F;
	private float last = -1.0F;
	private float hold;

	/** Atualiza com a fracao de vida atual (0 a 1) e devolve ate onde o rastro vai. */
	public float update(float current, float seconds) {
		if (trail < 0.0F || current >= trail) {
			trail = current;
			hold = 0.0F;
		} else {
			if (current < last) {
				hold = HOLD_SECONDS;
			}
			if (hold > 0.0F) {
				hold -= seconds;
			} else {
				trail = Math.max(current, trail - DRAIN_PER_SECOND * seconds);
			}
		}
		last = current;
		return trail;
	}
}
