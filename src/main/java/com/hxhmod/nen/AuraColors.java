package com.hxhmod.nen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * As duas cores da aura que o jogador escolheu, em RGB. A principal fica nas faixas externas, no En e
 * no Shu; a secundaria fica no miolo das chamas.
 */
public record AuraColors(int primary, int secondary) {
	/** Canal mais forte que uma cor precisa ter: a aura soma luz na cena, entao cor escura nao apareceria. */
	private static final int MIN_PEAK = 90;

	public static final AuraColors DEFAULT = Preset.CLASSIC.colors();

	public static final Codec<AuraColors> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("primary").forGetter(AuraColors::primary),
			Codec.INT.fieldOf("secondary").forGetter(AuraColors::secondary)
	).apply(instance, AuraColors::new));

	public static final StreamCodec<ByteBuf, AuraColors> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, AuraColors::primary,
			ByteBufCodecs.INT, AuraColors::secondary,
			AuraColors::new);

	/** Cores prontas do painel. */
	public enum Preset {
		CLASSIC("classic", 0x38B4FF, 0xFFA41E),
		FIRE("fire", 0xFF4A1C, 0xFFB52E),
		POISON("poison", 0x58E02C, 0xC6FF3A),
		SAKURA("sakura", 0xFF5FB0, 0xFFD0E4),
		THUNDER("thunder", 0x7C5CFF, 0x38E0FF),
		BLOOD("blood", 0xE0122E, 0x8A0CFF),
		ICE("ice", 0xA8E8FF, 0xFFFFFF),
		GOLD("gold", 0xFFC21E, 0xFFF0A0),
		SHADOW("shadow", 0x7A2CFF, 0xE0124C),
		JADE("jade", 0x12D08A, 0x4CF0E0),
		AURORA("aurora", 0x2CF0A0, 0xC04CFF),
		SOLAR("solar", 0xFF8A1E, 0xFFF04C);

		private final String id;
		private final AuraColors colors;

		Preset(String id, int primary, int secondary) {
			this.id = id;
			this.colors = new AuraColors(primary, secondary);
		}

		public String id() {
			return id;
		}

		public AuraColors colors() {
			return colors;
		}
	}

	/** O servidor passa todo pedido por aqui: tira lixo do canal alfa e clareia cor escura demais. */
	public AuraColors sanitized() {
		return new AuraColors(visible(primary), visible(secondary));
	}

	private static int visible(int color) {
		int red = color >> 16 & 0xFF;
		int green = color >> 8 & 0xFF;
		int blue = color & 0xFF;
		int peak = Math.max(red, Math.max(green, blue));
		if (peak >= MIN_PEAK) {
			return color & 0xFFFFFF;
		}
		if (peak == 0) {
			return MIN_PEAK << 16 | MIN_PEAK << 8 | MIN_PEAK;
		}
		return red * MIN_PEAK / peak << 16 | green * MIN_PEAK / peak << 8 | blue * MIN_PEAK / peak;
	}
}
