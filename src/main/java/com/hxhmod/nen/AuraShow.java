package com.hxhmod.nen;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que os outros precisam saber para desenhar a aura de um jogador. Vai para todos por perto,
 * ao contrario do NenData, que so o proprio jogador recebe. Cada tecnica aparece onde ela age.
 *
 * @param level      aura no corpo: 0 nenhuma, 1 suave (Ten, Ren basico), 2 chamas (Ren, Ken), 3 chamas altas (Ren maximo)
 * @param mastery    dominio de 0 a 100 da tecnica que forma a aura do corpo (ou a do punho, no Ko)
 * @param koCharge   carga do Ko de 0 a 1, ou negativa com o Ko desligado
 * @param gyoMastery dominio do Gyo, ou negativo com o Gyo desligado
 * @param shuMastery dominio do Shu, ou negativo com o Shu desligado
 * @param enRadius   raio da esfera do En em blocos; 0 com o En desligado
 * @param enMastery  dominio do En
 * @param offense    fracao da aura no ataque, de 0 a 1; o Ryu tira de um lado e poe no outro
 * @param leak       fracao da aura que quem nao usa Gyo percebe (vem do In)
 */
public record AuraShow(int level, int mastery, float koCharge, int gyoMastery, int shuMastery, float enRadius, int enMastery,
		float offense, float leak) {
	public static final int SOFT = 1;
	public static final int FLAMES = 2;
	public static final int BURST = 3;
	private static final int OFF = -1;
	private static final float BALANCED = 0.5F;
	/** Passos dos valores continuos: mudancas pequenas nao precisam ir pela rede a cada tick. */
	private static final float STEPS = 20;

	public static final AuraShow NONE = new AuraShow(0, 0, OFF, OFF, OFF, 0, 0, BALANCED, 1);

	public static final StreamCodec<ByteBuf, AuraShow> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AuraShow::level,
			ByteBufCodecs.VAR_INT, AuraShow::mastery,
			ByteBufCodecs.FLOAT, AuraShow::koCharge,
			ByteBufCodecs.INT, AuraShow::gyoMastery,
			ByteBufCodecs.INT, AuraShow::shuMastery,
			ByteBufCodecs.FLOAT, AuraShow::enRadius,
			ByteBufCodecs.VAR_INT, AuraShow::enMastery,
			ByteBufCodecs.FLOAT, AuraShow::offense,
			ByteBufCodecs.FLOAT, AuraShow::leak,
			AuraShow::new);

	/** Estado para este jogador. A carga do Ko e a parte ofensiva do Ryu vivem fora do NenData. */
	static AuraShow of(NenData data, double koCharge, double offense) {
		if (!data.awakened() || data.active().isEmpty() || data.isActive(Technique.ZETSU)) {
			return NONE;
		}
		boolean ko = data.isActive(Technique.KO);
		return new AuraShow(level(data), bodyMastery(data),
				ko ? stepped(koCharge) : OFF,
				data.isActive(Technique.GYO) ? data.mastery(Technique.GYO) : OFF,
				data.isActive(Technique.SHU) ? data.mastery(Technique.SHU) : OFF,
				data.isActive(Technique.EN) ? (float) (data.enSize() / 2) : 0,
				data.mastery(Technique.EN),
				data.isActive(Technique.RYU) ? stepped(offense) : BALANCED,
				stepped(data.inLeak()));
	}

	private static float stepped(double value) {
		return Math.round(value * STEPS) / STEPS;
	}

	/** So Ten, Ren e Ken reforcam o corpo; o Ko tira a aura de todo o resto e leva para o punho. */
	private static int level(NenData data) {
		if (data.isActive(Technique.KO)) {
			return 0;
		}
		if (data.isActive(Technique.REN)) {
			int intensity = data.intensityOf(Technique.REN);
			return intensity >= Technique.REN.maxIntensity() ? BURST : intensity > 1 ? FLAMES : SOFT;
		}
		if (data.isActive(Technique.KEN)) {
			return FLAMES;
		}
		return data.isActive(Technique.TEN) ? SOFT : 0;
	}

	private static int bodyMastery(NenData data) {
		Technique source = data.isActive(Technique.KO) ? Technique.KO
				: data.isActive(Technique.REN) ? Technique.REN : data.isActive(Technique.KEN) ? Technique.KEN : Technique.TEN;
		return data.mastery(source);
	}

	public boolean ko() {
		return koCharge >= 0;
	}

	public boolean gyo() {
		return gyoMastery >= 0;
	}

	public boolean shu() {
		return shuMastery >= 0;
	}

	public boolean en() {
		return enRadius > 0;
	}

	/** Tem alguma coisa para desenhar. */
	public boolean any() {
		return level > 0 || ko() || gyo() || shu() || en();
	}
}
