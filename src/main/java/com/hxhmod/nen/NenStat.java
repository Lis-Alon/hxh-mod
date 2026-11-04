package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Atributos do personagem. Nao recebem pontos: cada um tem a propria XP e sobe sozinho com o treino
 * certo ({@link NenTraining}), de 0 a {@link NenRules#MAX_ATTRIBUTE}. O que cada nivel da esta em {@link NenRules}.
 * Sao status internos do mod: nao mexem nos atributos do Minecraft.
 */
public enum NenStat implements StringRepresentable {
	VITALITY("vitality"),
	AURA_CAPACITY("aura_capacity"),
	AURA_FLOW("aura_flow"),
	ATTACK_POWER("attack_power"),
	PROTECTION("protection"),
	AGILITY("agility"),
	PERCEPTION("perception");

	public static final Codec<NenStat> CODEC = StringRepresentable.fromEnum(NenStat::values);

	private final String name;

	NenStat(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.stat." + name);
	}

	public Component description() {
		return Component.translatable("nen.hxh.stat." + name + ".desc");
	}

	/** Aviso de quando o treino sobe o nivel; a frase e inteira por causa do genero ("Sua Vitalidade", "Seu Fluxo"). */
	public Component trainedMessage() {
		return Component.translatable("nen.hxh.stat." + name + ".up");
	}
}
