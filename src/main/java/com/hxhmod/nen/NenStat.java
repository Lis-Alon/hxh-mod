package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Atributos do personagem que recebem pontos de Nen (1 ponto por clique).
 * Sao status internos do mod, calculados em {@link NenBonuses}: nao mexem nos atributos do Minecraft.
 */
public enum NenStat implements StringRepresentable {
	// +2 de vida maxima por ponto
	VITALITY("vitality", 2.0, 40),
	// +0,5 de dano corpo a corpo por ponto
	ATTACK_POWER("attack_power", 0.5, 40),
	// +1% de velocidade de ataque por ponto
	ATTACK_SPEED("attack_speed", 0.01, 25),
	// -0,75% do dano recebido por ponto
	PROTECTION("protection", 0.0075, 40),
	// +1,5% de velocidade de movimento por ponto
	AGILITY("agility", 0.015, 30),
	// +10 de aura maxima por ponto
	AURA_CAPACITY("aura_capacity", 10.0, 50),
	// +1 de aura por segundo por ponto
	AURA_FLOW("aura_flow", 1.0, 20);

	public static final Codec<NenStat> CODEC = StringRepresentable.fromEnum(NenStat::values);

	private final String name;
	private final double perPoint;
	private final int maxPoints;

	NenStat(String name, double perPoint, int maxPoints) {
		this.name = name;
		this.perPoint = perPoint;
		this.maxPoints = maxPoints;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public double perPoint() {
		return perPoint;
	}

	public int maxPoints() {
		return maxPoints;
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.stat." + name);
	}

	public Component description() {
		return Component.translatable("nen.hxh.stat." + name + ".desc");
	}
}
