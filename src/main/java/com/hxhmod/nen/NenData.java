package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tudo que o mod sabe sobre o Nen de um jogador.
 * O tipo e sorteado ao despertar, mas so aparece para o jogador quando {@code revealed} for verdadeiro.
 */
public record NenData(boolean awakened, NenType type, boolean revealed, int aura, int meditation,
		int tenXp, int zetsuXp, int renXp, int divination, Technique active) {
	public static final NenData DEFAULT = new NenData(false, NenType.NONE, false, 0, 0, 0, 0, 0, 0, Technique.NONE);

	public static final Codec<NenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("awakened", false).forGetter(NenData::awakened),
			NenType.CODEC.optionalFieldOf("type", NenType.NONE).forGetter(NenData::type),
			Codec.BOOL.optionalFieldOf("revealed", false).forGetter(NenData::revealed),
			Codec.INT.optionalFieldOf("aura", 0).forGetter(NenData::aura),
			Codec.INT.optionalFieldOf("meditation", 0).forGetter(NenData::meditation),
			Codec.INT.optionalFieldOf("ten_xp", 0).forGetter(NenData::tenXp),
			Codec.INT.optionalFieldOf("zetsu_xp", 0).forGetter(NenData::zetsuXp),
			Codec.INT.optionalFieldOf("ren_xp", 0).forGetter(NenData::renXp),
			Codec.INT.optionalFieldOf("divination", 0).forGetter(NenData::divination),
			Technique.CODEC.optionalFieldOf("active", Technique.NONE).forGetter(NenData::active)
	).apply(instance, NenData::new));

	public static final StreamCodec<ByteBuf, NenData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public int totalXp() {
		return tenXp + zetsuXp + renXp;
	}

	public int level() {
		return 1 + totalXp() / NenRules.XP_PER_LEVEL;
	}

	public int maxAura() {
		return awakened ? NenRules.BASE_MAX_AURA + NenRules.AURA_PER_LEVEL * (level() - 1) : 0;
	}

	public int xp(Technique technique) {
		return switch (technique) {
			case NONE -> 0;
			case TEN -> tenXp;
			case ZETSU -> zetsuXp;
			case REN -> renXp;
		};
	}

	public NenData awaken(NenType rolledType) {
		return new NenData(true, rolledType, false, NenRules.BASE_MAX_AURA, meditation, tenXp, zetsuXp, renXp, divination, Technique.NONE);
	}

	public NenData withMeditation(int newMeditation) {
		return new NenData(awakened, type, revealed, aura, newMeditation, tenXp, zetsuXp, renXp, divination, active);
	}

	public NenData withAura(int newAura) {
		return new NenData(awakened, type, revealed, Math.clamp(newAura, 0, maxAura()), meditation, tenXp, zetsuXp, renXp, divination, active);
	}

	public NenData withActive(Technique newActive) {
		return new NenData(awakened, type, revealed, aura, meditation, tenXp, zetsuXp, renXp, divination, newActive);
	}

	public NenData addXp(Technique technique, int amount) {
		return new NenData(awakened, type, revealed, aura, meditation,
				tenXp + (technique == Technique.TEN ? amount : 0),
				zetsuXp + (technique == Technique.ZETSU ? amount : 0),
				renXp + (technique == Technique.REN ? amount : 0),
				divination, active);
	}

	public NenData withDivination(int newDivination) {
		int clamped = Math.min(newDivination, 100);
		return new NenData(awakened, type, revealed || clamped >= 100, aura, meditation, tenXp, zetsuXp, renXp, clamped, active);
	}
}
