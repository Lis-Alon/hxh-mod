package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Funcionalidades extras de cada tecnica, compradas com pontos de Nen. */
public enum NenUpgrade implements StringRepresentable {
	TEN_FLOW("ten_flow", Technique.TEN, 2, 5),
	TEN_YOUTH("ten_youth", Technique.TEN, 3, 15),
	KEN_ENDURANCE("ken_endurance", Technique.KEN, 3, 35),
	KEN_IRON("ken_iron", Technique.KEN, 5, 50),
	ZETSU_SUPPRESS("zetsu_suppress", Technique.ZETSU, 2, 10),
	ZETSU_REST("zetsu_rest", Technique.ZETSU, 2, 8),
	ZETSU_STEP("zetsu_step", Technique.ZETSU, 2, 14),
	IN_CONTROL("in_control", Technique.IN, 3, 30),
	IN_SHADOW("in_shadow", Technique.IN, 4, 45),
	REN_CONTROL("ren_control", Technique.REN, 2, 12),
	REN_SURGE("ren_surge", Technique.REN, 3, 20),
	GYO_PRECISE("gyo_precise", Technique.GYO, 3, 25),
	GYO_INSTINCT("gyo_instinct", Technique.GYO, 3, 30),
	KO_ABSOLUTE("ko_absolute", Technique.KO, 5, 60),
	KO_FOCUS("ko_focus", Technique.KO, 4, 50),
	RYU_FLOW("ryu_flow", Technique.RYU, 5, 60),
	RYU_MASTER("ryu_master", Technique.RYU, 6, 80),
	SHU_COAT("shu_coat", Technique.SHU, 2, 20),
	SHU_EDGE("shu_edge", Technique.SHU, 3, 25),
	EN_WIDE("en_wide", Technique.EN, 3, 35),
	EN_CONTROL("en_control", Technique.EN, 3, 45),
	HATSU_EFFICIENCY("hatsu_efficiency", Technique.HATSU, 3, 15);

	// Nomes antigos: "ten_calm" virou Fluidez da Aura e "en_vast" virou Controle Territorial;
	// mundos salvos antes continuam carregando
	public static final Codec<NenUpgrade> CODEC = Codec.STRING.comapFlatMap(
			name -> byName(name.equals("ten_calm") ? "ten_flow" : name.equals("en_vast") ? "en_control" : name)
					.map(DataResult::success)
					.orElseGet(() -> DataResult.error(() -> "Funcionalidade de Nen desconhecida: " + name)),
			NenUpgrade::getSerializedName);
	public static final StreamCodec<ByteBuf, NenUpgrade> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(i -> values()[i], NenUpgrade::ordinal);

	private final String name;
	private final Technique technique;
	private final int cost;
	private final int minLevel;

	NenUpgrade(String name, Technique technique, int cost, int minLevel) {
		this.name = name;
		this.technique = technique;
		this.cost = cost;
		this.minLevel = minLevel;
	}

	public static Optional<NenUpgrade> byName(String name) {
		return Arrays.stream(values()).filter(upgrade -> upgrade.name.equals(name)).findFirst();
	}

	public static List<NenUpgrade> of(Technique technique) {
		return Arrays.stream(values()).filter(upgrade -> upgrade.technique == technique).toList();
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public Technique technique() {
		return technique;
	}

	public int cost() {
		return cost;
	}

	public int minLevel() {
		return minLevel;
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.upgrade." + name);
	}

	public Component description() {
		return Component.translatable("nen.hxh.upgrade." + name + ".desc");
	}
}
