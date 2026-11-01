package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum Technique implements StringRepresentable {
	NONE("none"),
	TEN("ten"),
	ZETSU("zetsu"),
	REN("ren");

	public static final Codec<Technique> CODEC = StringRepresentable.fromEnum(Technique::values);
	public static final StreamCodec<ByteBuf, Technique> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> values()[i], Technique::ordinal);
	public static final List<Technique> TRAINABLE = List.of(TEN, ZETSU, REN);

	private final String name;

	Technique(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.technique." + name);
	}

	public boolean isUnlocked(NenData data) {
		return switch (this) {
			case NONE -> true;
			case TEN -> data.awakened();
			case ZETSU -> data.awakened() && data.tenXp() >= NenRules.ZETSU_UNLOCK_TEN_XP;
			case REN -> data.awakened() && data.zetsuXp() >= NenRules.REN_UNLOCK_ZETSU_XP;
		};
	}

	/** Quanto treino esta tecnica precisa para o proximo marco (liberar a seguinte ou dominar). */
	public int milestoneXp() {
		return switch (this) {
			case NONE -> 0;
			case TEN -> NenRules.ZETSU_UNLOCK_TEN_XP;
			case ZETSU -> NenRules.REN_UNLOCK_ZETSU_XP;
			case REN -> NenRules.REN_MASTERY_XP;
		};
	}
}
