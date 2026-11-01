package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Tudo que o mod sabe sobre o Nen de um jogador. */
public record NenData(NenType type, int aura, int maxAura) {
	public static final NenData DEFAULT = new NenData(NenType.NONE, 0, 0);
	public static final int STARTING_MAX_AURA = 100;

	public static final Codec<NenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			NenType.CODEC.fieldOf("type").forGetter(NenData::type),
			Codec.INT.fieldOf("aura").forGetter(NenData::aura),
			Codec.INT.fieldOf("max_aura").forGetter(NenData::maxAura)
	).apply(instance, NenData::new));

	public static final StreamCodec<ByteBuf, NenData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> NenType.values()[i], NenType::ordinal), NenData::type,
			ByteBufCodecs.VAR_INT, NenData::aura,
			ByteBufCodecs.VAR_INT, NenData::maxAura,
			NenData::new);

	public boolean awakened() {
		return type != NenType.NONE;
	}

	public NenData withAura(int newAura) {
		return new NenData(type, Math.clamp(newAura, 0, maxAura), maxAura);
	}
}
