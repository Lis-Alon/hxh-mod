package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Limpeza dos modificadores do Minecraft de versoes antigas: os status agora ficam em {@link NenBonuses}. */
public final class NenEffects {
	private NenEffects() {
	}

	/**
	 * Versoes antigas punham os pontos de atributo como modificadores do Minecraft (vida, dano, armadura...).
	 * Agora sao status internos; isto tira o que tiver sobrado num mundo salvo antes.
	 */
	public static void removeLegacyModifiers(ServerPlayer player) {
		List<Holder<Attribute>> attributes = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
				Attributes.ATTACK_SPEED, Attributes.ARMOR, Attributes.MOVEMENT_SPEED);
		for (String stat : List.of("vitality", "attack_power", "attack_speed", "protection", "agility", "aura_capacity", "aura_flow")) {
			Identifier id = HxHMod.id("stat/" + stat);
			for (Holder<Attribute> attribute : attributes) {
				AttributeInstance instance = player.getAttribute(attribute);
				if (instance != null && instance.getModifier(id) != null) {
					instance.removeModifier(id);
				}
			}
		}
	}
}
