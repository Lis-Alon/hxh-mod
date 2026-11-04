package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/**
 * Efeitos de percepcao das tecnicas ligadas, reaplicados a cada segundo.
 * Os bonus de status (dano, defesa, velocidade...) nao passam por aqui: ficam em {@link NenBonuses}.
 */
public final class NenEffects {
	private static final int EFFECT_DURATION_TICKS = 40;

	private NenEffects() {
	}

	/**
	 * Invisibilidade (In). A visao do Gyo e o En sao so do lado do cliente: contorno visto apenas por quem usa.
	 */
	public static void applyTechniques(ServerPlayer player, NenData data) {
		if (data.isActive(Technique.IN)) {
			effect(player, MobEffects.INVISIBILITY, 0);
		}
	}

	private static void effect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
		player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier, true, false, true));
	}

	/**
	 * Versoes antigas punham os pontos de atributo como modificadores do Minecraft (vida, dano, armadura...).
	 * Agora sao status internos; isto tira o que tiver sobrado num mundo salvo antes.
	 */
	public static void removeLegacyModifiers(ServerPlayer player) {
		List<Holder<Attribute>> attributes = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
				Attributes.ATTACK_SPEED, Attributes.ARMOR, Attributes.MOVEMENT_SPEED);
		for (NenStat stat : NenStat.values()) {
			Identifier id = HxHMod.id("stat/" + stat.getSerializedName());
			for (Holder<Attribute> attribute : attributes) {
				AttributeInstance instance = player.getAttribute(attribute);
				if (instance != null && instance.getModifier(id) != null) {
					instance.removeModifier(id);
				}
			}
		}
	}
}
