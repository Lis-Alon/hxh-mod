package com.hxhmod.nen;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * O que os outros precisam saber para desenhar a aura de um jogador. Vai para todos por perto,
 * ao contrario do NenData, que so o proprio jogador recebe.
 *
 * @param level    0 sem aura, 1 suave (Ten, Ren basico), 2 chamas, 3 explosao (Ren maximo, Ko)
 * @param shu      a aura reveste o objeto segurado
 * @param enRadius raio da esfera do En em blocos; 0 com o En desligado
 * @param leak     fracao da aura que quem nao usa Gyo percebe (vem do In)
 */
public record AuraShow(int level, boolean shu, float enRadius, float leak) {
	public static final int SOFT = 1;
	public static final int FLAMES = 2;
	public static final int BURST = 3;

	public static final AuraShow NONE = new AuraShow(0, false, 0, 1);

	public static final StreamCodec<ByteBuf, AuraShow> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AuraShow::level,
			ByteBufCodecs.BOOL, AuraShow::shu,
			ByteBufCodecs.FLOAT, AuraShow::enRadius,
			ByteBufCodecs.FLOAT, AuraShow::leak,
			AuraShow::new);

	public static AuraShow of(NenData data) {
		if (!data.awakened() || data.active().isEmpty() || data.isActive(Technique.ZETSU)) {
			return NONE;
		}
		float enRadius = data.isActive(Technique.EN) ? (float) (data.enSize() / 2) : 0;
		// Em passos: a mudanca de vazamento nao precisa ser enviada a cada tick
		float leak = Math.round(data.inLeak() * 20) / 20F;
		return new AuraShow(level(data), data.isActive(Technique.SHU), enRadius, leak);
	}

	private static int level(NenData data) {
		if (data.isActive(Technique.KO)) {
			return BURST;
		}
		if (data.isActive(Technique.REN)) {
			int intensity = data.intensityOf(Technique.REN);
			return intensity >= Technique.REN.maxIntensity() ? BURST : intensity > 1 ? FLAMES : SOFT;
		}
		return data.isActive(Technique.KEN) ? FLAMES : SOFT;
	}
}
