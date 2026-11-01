package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Efeitos das tecnicas ativas e bonus dos atributos, reaplicados a cada segundo. */
public final class NenEffects {
	private static final int EFFECT_DURATION_TICKS = 40;

	private NenEffects() {
	}

	public static void applyTechnique(ServerPlayer player, NenData data) {
		switch (data.active()) {
			// Ten e Zetsu nao usam efeitos de pocao: a defesa, a cura e a furtividade ficam em NenCombat e NenEvents
			case TEN, ZETSU -> {
			}
			case KEN -> effect(player, MobEffects.RESISTANCE, data.has(NenUpgrade.KEN_IRON) ? 2 : 1);
			case IN -> {
				effect(player, MobEffects.INVISIBILITY, 0);
				effect(player, MobEffects.STRENGTH, data.has(NenUpgrade.IN_SHADOW) ? 1 : 0);
			}
			case REN -> effect(player, MobEffects.STRENGTH, data.has(NenUpgrade.REN_SURGE) ? 1 : 0);
			case GYO -> {
				effect(player, MobEffects.NIGHT_VISION, 0);
				revealInvisible(player, NenRules.EN_RADIUS);
			}
			case KO -> {
				effect(player, MobEffects.STRENGTH, 2);
				effect(player, MobEffects.SLOWNESS, 0);
			}
			case RYU -> {
				int level = data.has(NenUpgrade.RYU_MASTER) ? 2 : 1;
				effect(player, MobEffects.STRENGTH, level);
				effect(player, MobEffects.RESISTANCE, level);
				effect(player, MobEffects.SPEED, 0);
			}
			case SHU -> {
				effect(player, MobEffects.HASTE, 1);
				if (data.has(NenUpgrade.SHU_EDGE)) {
					effect(player, MobEffects.STRENGTH, 0);
				}
			}
			case EN -> {
				int radius = data.has(NenUpgrade.EN_VAST) ? NenRules.EN_VAST_RADIUS
						: data.has(NenUpgrade.EN_WIDE) ? NenRules.EN_WIDE_RADIUS : NenRules.EN_RADIUS;
				for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
						player.getBoundingBox().inflate(radius), entity -> entity != player)) {
					entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION_TICKS, 0, true, false));
				}
			}
			case NONE, HATSU -> {
			}
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

	/** Bonus dos pontos de atributo, como modificadores temporarios (reaplicados apos morrer ou relogar). */
	public static void applyStats(ServerPlayer player, NenData data) {
		for (NenStat stat : NenStat.values()) {
			if (stat.attribute() == null) {
				continue;
			}
			AttributeInstance instance = player.getAttribute(stat.attribute());
			if (instance == null) {
				continue;
			}
			Identifier id = HxHMod.id("stat/" + stat.getSerializedName());
			double amount = data.statPoints(stat) * stat.perPoint();
			AttributeModifier current = instance.getModifier(id);
			if (amount == 0) {
				if (current != null) {
					instance.removeModifier(id);
				}
			} else if (current == null || current.amount() != amount) {
				instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
			}
		}
	}
}
