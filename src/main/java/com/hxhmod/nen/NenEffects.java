package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
	 * Ver no escuro e revelar invisiveis (Gyo) e invisibilidade (In).
	 * O En e so do lado do cliente: contorno nas criaturas dentro da area, visto apenas por quem usa.
	 */
	public static void applyTechniques(ServerPlayer player, NenData data) {
		if (data.isActive(Technique.GYO)) {
			effect(player, MobEffects.NIGHT_VISION, 0);
			revealInvisible(player, NenRules.GYO_REVEAL_RADIUS);
		}
		if (data.isActive(Technique.IN)) {
			effect(player, MobEffects.INVISIBILITY, 0);
		}
	}

	/** Gyo: faz brilhar quem esta invisivel por perto. */
	private static void revealInvisible(ServerPlayer player, int radius) {
		for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius), entity -> entity != player && entity.isInvisible())) {
			entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION_TICKS, 0, true, false));
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
