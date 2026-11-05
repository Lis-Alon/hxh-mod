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
	HATSU_EFFICIENCY("hatsu_efficiency", Technique.HATSU, 3, 15),
	// Aba Hatsu: um Hatsu ativo e duas passivas em cada galho do hexagono, comprados em ordem
	HATSU_QUAKE("hatsu_quake", NenType.ENHANCEMENT, null, 6, 20),
	HATSU_VITALITY("hatsu_vitality", NenType.ENHANCEMENT, HATSU_QUAKE, 4, 30),
	HATSU_IMPACT("hatsu_impact", NenType.ENHANCEMENT, HATSU_VITALITY, 4, 40),
	HATSU_PROPERTY("hatsu_property", NenType.TRANSMUTATION, null, 6, 20),
	HATSU_PERSISTENT("hatsu_persistent", NenType.TRANSMUTATION, HATSU_PROPERTY, 4, 30),
	HATSU_ADAPTATION("hatsu_adaptation", NenType.TRANSMUTATION, HATSU_PERSISTENT, 4, 40),
	HATSU_ARMOR("hatsu_armor", NenType.CONJURATION, null, 6, 20),
	HATSU_STRUCTURE("hatsu_structure", NenType.CONJURATION, HATSU_ARMOR, 4, 30),
	HATSU_MANIFEST("hatsu_manifest", NenType.CONJURATION, HATSU_STRUCTURE, 4, 40),
	HATSU_MARK("hatsu_mark", NenType.SPECIALIZATION, null, 6, 20),
	HATSU_READING("hatsu_reading", NenType.SPECIALIZATION, HATSU_MARK, 4, 30),
	HATSU_MEMORY("hatsu_memory", NenType.SPECIALIZATION, HATSU_READING, 4, 40),
	HATSU_COMMAND("hatsu_command", NenType.MANIPULATION, null, 6, 20),
	HATSU_BOND("hatsu_bond", NenType.MANIPULATION, HATSU_COMMAND, 4, 30),
	HATSU_ORDER("hatsu_order", NenType.MANIPULATION, HATSU_BOND, 4, 40),
	HATSU_BLAST("hatsu_blast", NenType.EMISSION, null, 6, 20),
	HATSU_COHESION("hatsu_cohesion", NenType.EMISSION, HATSU_BLAST, 4, 30),
	HATSU_SWIFT("hatsu_swift", NenType.EMISSION, HATSU_COHESION, 4, 40);

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
	private final NenType branch;
	private final NenUpgrade requires;

	NenUpgrade(String name, Technique technique, int cost, int minLevel) {
		this.branch = NenType.NONE;
		this.requires = null;
		this.name = name;
		this.technique = technique;
		this.cost = cost;
		this.minLevel = minLevel;
	}

	NenUpgrade(String name, NenType branch, NenUpgrade requires, int cost, int minLevel) {
		this.branch = branch;
		this.requires = requires;
		this.name = name;
		this.technique = Technique.HATSU;
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

	/** Os nos de um galho da aba Hatsu, do centro para a ponta. */
	public static List<NenUpgrade> ofBranch(NenType branch) {
		return Arrays.stream(values()).filter(upgrade -> upgrade.branch == branch).toList();
	}

	/** Nao e da aba Hatsu, ou o tipo ja foi revelado e tem alguma afinidade com o galho. */
	public boolean availableTo(NenData data) {
		return branch == NenType.NONE || (data.revealed() && !NenAffinity.blocked(data.type(), branch));
	}

	public Technique technique() {
		return technique;
	}

	/** Galho do hexagono; NONE para as funcionalidades da arvore de tecnicas. */
	public NenType branch() {
		return branch;
	}

	/** O no anterior do galho, ou null. */
	public NenUpgrade requires() {
		return requires;
	}

	/** Preco para quem nasceu com este tipo: quanto menor a afinidade com o galho, mais caro. */
	public int costFor(NenType innate) {
		return branch == NenType.NONE ? cost : NenAffinity.cost(cost, NenAffinity.percent(innate, branch));
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
