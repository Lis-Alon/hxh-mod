package com.hxhmod.nen;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shu: a aura reveste o objeto segurado. Qualquer ferramenta ou arma melhora em tudo (dano, velocidade de quebrar,
 * velocidade de ataque e durabilidade), de metade ate o dobro dos valores, conforme a proficiencia.
 * O revestimento acompanha o que estiver na mao: trocar de objeto nao desliga o Shu, e de mao vazia ele so nao faz efeito.
 */
public final class ShuCoat {
	public enum Kind {
		NONE, TOOL, SHIELD, COMMON
	}

	// Quem ja segurou um objeto desde que ligou o Shu; ligar sem nada na mao nao pega
	private static final Set<ServerPlayer> COATED = Collections.newSetFromMap(new WeakHashMap<>());
	// Ultimo tick em que o objeto revestido gastou durabilidade (treino pertinente de quem minera ou atira)
	private static final Map<ServerPlayer, Integer> LAST_USED = new WeakHashMap<>();

	private ShuCoat() {
	}

	public static Kind kind(ItemStack stack) {
		if (stack.isEmpty()) {
			return Kind.NONE;
		}
		if (stack.is(Items.SHIELD)) {
			return Kind.SHIELD;
		}
		// Pelo que o objeto faz, nao por uma lista: tambem vale para tesoura, arco, vara de pesca e ferramentas de outros mods
		return stack.isDamageableItem() || stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON) ? Kind.TOOL : Kind.COMMON;
	}

	/** O objeto da mao principal; com ela vazia, o escudo da outra mao. */
	public static ItemStack held(Player player) {
		ItemStack main = player.getMainHandItem();
		return main.isEmpty() && player.getOffhandItem().is(Items.SHIELD) ? player.getOffhandItem() : main;
	}

	static void tick(ServerPlayer player, NenData data) {
		if (!data.isActive(Technique.SHU)) {
			COATED.remove(player);
			return;
		}
		if (!held(player).isEmpty()) {
			COATED.add(player);
		} else if (!COATED.contains(player)) {
			player.setAttached(ModAttachments.NEN, data.toggled(Technique.SHU));
			player.sendOverlayMessage(Component.translatable("nen.hxh.shu.empty"));
		}
	}

	/** Dano a mais do golpe com o objeto revestido. */
	static double damageBonus(ServerPlayer player, NenData data) {
		if (!data.isActive(Technique.SHU)) {
			return 0;
		}
		return switch (kind(player.getMainHandItem())) {
			case TOOL -> data.shuToolDamage();
			case COMMON -> data.shuCommonDamage();
			default -> 0;
		};
	}

	/** Dano a mais da flecha disparada com arco ou besta revestidos. */
	static double rangedBonus(ServerPlayer player, NenData data) {
		return data.isActive(Technique.SHU) && player.getMainHandItem().getItem() instanceof ProjectileWeaponItem ? data.shuToolDamage() : 0;
	}

	/** Multiplicador do dano recebido segurando um escudo revestido (o bloqueio normal continua igual). */
	static double damageTaken(ServerPlayer player, NenData data) {
		return data.isActive(Technique.SHU) && kind(held(player)) == Kind.SHIELD ? 1 - data.shuShieldDefense() : 1;
	}

	/** Velocidade de quebrar blocos a mais; roda nos dois lados, para o cliente prever a quebra igual ao servidor. */
	public static double miningBonus(Player player) {
		NenData data = player.getAttached(ModAttachments.NEN);
		if (data == null || !data.isActive(Technique.SHU)) {
			return 0;
		}
		return kind(player.getMainHandItem()) == Kind.TOOL ? data.shuMiningSpeed() : 0;
	}

	/** Velocidade de ataque a mais; tambem roda nos dois lados, para a barra de recarga do golpe bater com o servidor. */
	public static double attackSpeedBonus(Player player) {
		NenData data = player.getAttached(ModAttachments.NEN);
		if (data == null || !data.isActive(Technique.SHU)) {
			return 0;
		}
		return kind(player.getMainHandItem()) == Kind.TOOL ? data.shuAttackSpeed() : 0;
	}

	/** Usou o objeto revestido no ultimo segundo. */
	static boolean usedRecently(ServerPlayer player) {
		return player.tickCount - LAST_USED.getOrDefault(player, -1000) <= 20;
	}

	/** Desgaste do objeto revestido: cada ponto tem chance de ser evitado. */
	public static int reduceWear(ServerPlayer player, ItemStack stack, int amount) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (amount <= 0 || !data.isActive(Technique.SHU) || stack != held(player)) {
			return amount;
		}
		LAST_USED.put(player, player.tickCount);
		int kept = 0;
		for (int i = 0; i < amount; i++) {
			if (player.getRandom().nextDouble() >= data.shuWearSave()) {
				kept++;
			}
		}
		return kept;
	}
}
