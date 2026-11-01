package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Atributos do personagem que recebem pontos de Nen (1 ponto por clique). */
public enum NenStat implements StringRepresentable {
	VITALITY("vitality", Attributes.MAX_HEALTH, 2.0, 40),
	ATTACK_POWER("attack_power", Attributes.ATTACK_DAMAGE, 0.5, 40),
	ATTACK_SPEED("attack_speed", Attributes.ATTACK_SPEED, 0.04, 25),
	PROTECTION("protection", Attributes.ARMOR, 0.5, 40),
	AGILITY("agility", Attributes.MOVEMENT_SPEED, 0.0015, 30),
	AURA_CAPACITY("aura_capacity", null, 10.0, 50),
	AURA_FLOW("aura_flow", null, 1.0, 20);

	public static final Codec<NenStat> CODEC = StringRepresentable.fromEnum(NenStat::values);

	private final String name;
	private final Holder<Attribute> attribute;
	private final double perPoint;
	private final int maxPoints;

	NenStat(String name, Holder<Attribute> attribute, double perPoint, int maxPoints) {
		this.name = name;
		this.attribute = attribute;
		this.perPoint = perPoint;
		this.maxPoints = maxPoints;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** Atributo vanilla que este status altera, ou null para os de aura (calculados pelo mod). */
	public Holder<Attribute> attribute() {
		return attribute;
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
