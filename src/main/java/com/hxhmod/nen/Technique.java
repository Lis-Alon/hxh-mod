package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * As tecnicas de Nen, na ordem das abas do painel: os quatro principios (Ten, Zetsu, Ren e Hatsu)
 * e as aplicacoes avancadas de cada um. Os principios vem ao despertar; as avancadas sao aprendidas
 * quando a proficiencia nas tecnicas base chega no minimo pedido.
 */
public enum Technique implements StringRepresentable {
	NONE("none", 0, 0),
	TEN("ten", 1, 0),
	KEN("ken", 3, 0),
	ZETSU("zetsu", 1, 0),
	IN("in", 3, 0),
	REN("ren", 2, 0),
	GYO("gyo", 2, 1),
	KO("ko", 4, 3),
	RYU("ryu", 5, 2),
	SHU("shu", 2, 0),
	EN("en", 3, 0),
	HATSU("hatsu", 2, 2);

	public static final Codec<Technique> CODEC = StringRepresentable.fromEnum(Technique::values);
	public static final StreamCodec<ByteBuf, Technique> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> values()[i], Technique::ordinal);
	/** Ordem das abas no painel. */
	public static final List<Technique> TABS = List.of(TEN, KEN, ZETSU, IN, REN, GYO, KO, RYU, SHU, EN, HATSU);
	/** Os quatro principios, aprendidos ao despertar. */
	public static final Set<Technique> BASE = EnumSet.of(TEN, ZETSU, REN, HATSU);

	/** Proficiencia minima (0 a 100) numa tecnica base. */
	public record Requirement(Technique technique, int mastery) {
	}

	private final String name;
	private final int xpPerSecond;
	private final int auraDrain;

	Technique(String name, int xpPerSecond, int auraDrain) {
		this.name = name;
		this.xpPerSecond = xpPerSecond;
		this.auraDrain = auraDrain;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public int xpPerSecond() {
		return xpPerSecond;
	}

	/** Quantas intensidades a tecnica tem (1 = nao da para regular). No En, cada passo e 1/4 do tamanho maximo. */
	public int maxIntensity() {
		return switch (this) {
			case REN, KEN, SHU -> 3;
			case EN -> 4;
			default -> 1;
		};
	}

	/** Intensidade de quem nunca escolheu: media, e o En inteiro. */
	public int defaultIntensity() {
		return this == EN ? 4 : Math.min(2, maxIntensity());
	}

	/** Aura gasta por segundo das tecnicas de gasto fixo; as outras calculam em {@link NenData#auraDrain}. */
	public int baseAuraDrain() {
		return auraDrain;
	}

	public boolean activatable() {
		return this != NONE;
	}

	/** Proficiencia minima nas tecnicas base para aprender esta (vazio nos principios). */
	public List<Requirement> requirements() {
		return switch (this) {
			case NONE, TEN, ZETSU, REN, HATSU -> List.of();
			case GYO -> List.of(new Requirement(TEN, 20));
			case IN -> List.of(new Requirement(ZETSU, 50));
			case EN -> List.of(new Requirement(TEN, 40), new Requirement(REN, 40));
			case SHU -> List.of(new Requirement(TEN, 50));
			case KEN -> List.of(new Requirement(TEN, 60), new Requirement(REN, 60));
			case KO -> List.of(new Requirement(GYO, 70), new Requirement(TEN, 70), new Requirement(ZETSU, 70),
					new Requirement(REN, 70), new Requirement(HATSU, 70));
			case RYU -> List.of(new Requirement(GYO, 40), new Requirement(KEN, 40));
		};
	}

	/** As tecnicas base desta, na ordem dos requisitos. */
	public List<Technique> prerequisites() {
		return requirements().stream().map(Requirement::technique).toList();
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
		String key = "nen.hxh.technique." + name + ".effect";
		// O texto do Gyo mostra as teclas que quem joga escolheu
		return this == GYO ? Component.translatable(key, GyoApplications.keys()) : Component.translatable(key);
	}
}
