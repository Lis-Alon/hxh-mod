package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Qual galho esta equipado na tecla do Hatsu e ate que tick do mundo ele fica recarregando. */
public record HatsuState(NenType equipped, long readyAt) {
	public static final HatsuState NONE = new HatsuState(NenType.NONE, 0);

	public static final Codec<HatsuState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			NenType.CODEC.fieldOf("equipped").forGetter(HatsuState::equipped),
			Codec.LONG.fieldOf("ready_at").forGetter(HatsuState::readyAt)
	).apply(instance, HatsuState::new));

	public static final StreamCodec<ByteBuf, HatsuState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> NenType.values()[i], NenType::ordinal), HatsuState::equipped,
			ByteBufCodecs.VAR_LONG, HatsuState::readyAt,
			HatsuState::new);

	/** Ticks que faltam. O limite protege de um relogio menor, como ao levar o jogador para outro mundo. */
	public int remaining(long now) {
		return (int) Math.clamp(readyAt - now, 0, NenRules.HATSU_COOLDOWN_SLOWEST * 20L);
	}

	HatsuState equippedTo(NenType branch) {
		return new HatsuState(branch, readyAt);
	}

	HatsuState startedUntil(long tick) {
		return new HatsuState(equipped, tick);
	}
}
