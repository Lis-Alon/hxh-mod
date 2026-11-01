package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum NenType implements StringRepresentable {
	NONE("none", ChatFormatting.GRAY, 0xAAAAAA),
	ENHANCEMENT("enhancement", ChatFormatting.RED, 0xFF5555),
	TRANSMUTATION("transmutation", ChatFormatting.LIGHT_PURPLE, 0xFF55FF),
	EMISSION("emission", ChatFormatting.YELLOW, 0xFFFF55),
	CONJURATION("conjuration", ChatFormatting.AQUA, 0x55FFFF),
	MANIPULATION("manipulation", ChatFormatting.GREEN, 0x55FF55),
	SPECIALIZATION("specialization", ChatFormatting.DARK_PURPLE, 0xAA00AA);

	public static final Codec<NenType> CODEC = StringRepresentable.fromEnum(NenType::values);

	private final String name;
	private final ChatFormatting color;
	private final int barColor;

	NenType(String name, ChatFormatting color, int barColor) {
		this.name = name;
		this.color = color;
		this.barColor = barColor;
	}

	/** Cor RGB da barra de aura no HUD. */
	public int barColor() {
		return barColor;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public ChatFormatting color() {
		return color;
	}

	public Component displayName() {
		return Component.translatable("nen.hxh.type." + name).withStyle(color);
	}

	/** O que acontece com o copo de agua quando a videncia termina. */
	public Component waterReaction() {
		return Component.translatable("nen.hxh.reaction." + name);
	}

	/** Um sinal fraco da reacao, mostrado no meio do treino com a folha. */
	public Component waterHint() {
		return Component.translatable("nen.hxh.hint." + name);
	}

	private static final int SPECIALIZATION_CHANCE = 20; // 1 em 20
	private static final NenType[] COMMON_TYPES = {ENHANCEMENT, TRANSMUTATION, EMISSION, CONJURATION, MANIPULATION};

	public static NenType roll(RandomSource random) {
		if (random.nextInt(SPECIALIZATION_CHANCE) == 0) {
			return SPECIALIZATION;
		}
		return COMMON_TYPES[random.nextInt(COMMON_TYPES.length)];
	}
}
