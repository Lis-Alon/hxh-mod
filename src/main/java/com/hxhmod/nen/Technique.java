package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * As tecnicas de Nen, na ordem das abas do painel: os quatro principios (Ten, Zetsu, Ren, Hatsu)
 * e as aplicacoes avancadas de cada um.
 */
public enum Technique implements StringRepresentable {
	NONE("none", 0, 0, 0, 0),
	TEN("ten", 0, 1, 1, 0),
	KEN("ken", 4, 20, 3, 3),
	ZETSU("zetsu", 1, 2, 1, 0),
	IN("in", 3, 25, 3, 2),
	REN("ren", 2, 5, 2, 3),
	GYO("gyo", 2, 10, 2, 1),
	KO("ko", 4, 30, 4, 5),
	RYU("ryu", 5, 40, 5, 4),
	SHU("shu", 2, 15, 2, 1),
	EN("en", 3, 18, 3, 2),
	HATSU("hatsu", 5, 20, 0, 0);

	public static final Codec<Technique> CODEC = StringRepresentable.fromEnum(Technique::values);
	public static final StreamCodec<ByteBuf, Technique> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> values()[i], Technique::ordinal);
	/** Ordem das abas no painel. */
	public static final List<Technique> TABS = List.of(TEN, KEN, ZETSU, IN, REN, GYO, KO, RYU, SHU, EN, HATSU);

	private final String name;
	private final int unlockCost;
	private final int minLevel;
	private final int xpPerSecond;
	private final int auraDrain;

	Technique(String name, int unlockCost, int minLevel, int xpPerSecond, int auraDrain) {
		this.name = name;
		this.unlockCost = unlockCost;
		this.minLevel = minLevel;
		this.xpPerSecond = xpPerSecond;
		this.auraDrain = auraDrain;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int unlockCost() {
		return unlockCost;
	}

	public int minLevel() {
		return minLevel;
	}

	public int xpPerSecond() {
		return xpPerSecond;
	}

	/** Aura gasta por segundo antes dos descontos das funcionalidades. */
	public int baseAuraDrain() {
		return auraDrain;
	}

	/** O Hatsu e a habilidade unica do tipo: ainda nao se ativa pelo painel. */
	public boolean activatable() {
		return this != NONE && this != HATSU;
	}

	/** Tecnicas que precisam estar desbloqueadas antes desta. */
	public List<Technique> prerequisites() {
		return switch (this) {
			case NONE, TEN -> List.of();
			case ZETSU, REN -> List.of(TEN);
			case KEN, EN -> List.of(TEN, REN);
			case IN -> List.of(ZETSU, REN);
			case GYO, SHU -> List.of(REN);
			case KO -> List.of(GYO, ZETSU);
			case RYU -> List.of(KO, KEN);
			case HATSU -> List.of(REN);
		};
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.technique." + name);
	}

	/** Nome em portugues da tecnica (ex.: "Envoltura"). */
	public Component meaning() {
		return Component.translatable("nen.hxh.technique." + name + ".meaning");
	}

	/** De onde a tecnica vem: principio fundamental ou aplicacao avancada de qual principio. */
	public Component origin() {
		return Component.translatable("nen.hxh.technique." + name + ".origin");
	}

	/** O que a tecnica e no anime. */
	public Component lore() {
		return Component.translatable("nen.hxh.technique." + name + ".lore");
	}

	/** O que a tecnica faz no jogo. */
	public Component effect() {
		return Component.translatable("nen.hxh.technique." + name + ".effect");
	}
}
