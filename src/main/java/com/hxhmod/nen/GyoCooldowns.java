package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Ate que tick do mundo cada aplicacao do Gyo fica recarregando. Cada uma conta o proprio tempo. */
public record GyoCooldowns(long punch, long guard, long jump, long dash) {
	public static final GyoCooldowns NONE = new GyoCooldowns(0, 0, 0, 0);

	public static final Codec<GyoCooldowns> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("punch").forGetter(GyoCooldowns::punch),
			Codec.LONG.fieldOf("guard").forGetter(GyoCooldowns::guard),
			Codec.LONG.fieldOf("jump").forGetter(GyoCooldowns::jump),
			Codec.LONG.fieldOf("dash").forGetter(GyoCooldowns::dash)
	).apply(instance, GyoCooldowns::new));

	public static final StreamCodec<ByteBuf, GyoCooldowns> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, GyoCooldowns::punch,
			ByteBufCodecs.VAR_LONG, GyoCooldowns::guard,
			ByteBufCodecs.VAR_LONG, GyoCooldowns::jump,
			ByteBufCodecs.VAR_LONG, GyoCooldowns::dash,
			GyoCooldowns::new);

	private long until(GyoApplications.Action action) {
		return switch (action) {
			case PUNCH -> punch;
			case GUARD -> guard;
			case JUMP -> jump;
			case DASH -> dash;
		};
	}

	/** Ticks que faltam. O limite protege de um relogio menor, como ao levar o jogador para outro mundo. */
	public int remaining(GyoApplications.Action action, long now) {
		return (int) Math.clamp(until(action) - now, 0, NenRules.GYO_COOLDOWN_SLOWEST_SECONDS * 20L);
	}

	GyoCooldowns started(GyoApplications.Action action, long until) {
		return new GyoCooldowns(
				action == GyoApplications.Action.PUNCH ? until : punch,
				action == GyoApplications.Action.GUARD ? until : guard,
				action == GyoApplications.Action.JUMP ? until : jump,
				action == GyoApplications.Action.DASH ? until : dash);
	}
}
