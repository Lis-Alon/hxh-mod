package com.hxhmod.nen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shu: a aura reveste o objeto segurado e o beneficio depende do que ele e.
 * Guardar, soltar ou trocar o objeto desfaz o revestimento.
 */
public final class ShuCoat {
	public enum Kind {
		NONE, WEAPON, TOOL, SHIELD, COMMON
	}

	/** Objeto revestido e o slot da hotbar em que ele estava. */
	private record Coated(ItemStack stack, int slot) {
	}

	private static final Map<ServerPlayer, Coated> COATED = new WeakHashMap<>();

	private ShuCoat() {
	}

	public static Kind kind(ItemStack stack) {
		if (stack.isEmpty()) {
			return Kind.NONE;
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) {
			return Kind.WEAPON;
		}
		if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
			return Kind.TOOL;
		}
		return stack.is(Items.SHIELD) ? Kind.SHIELD : Kind.COMMON;
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
		ItemStack held = held(player);
		int slot = player.getInventory().getSelectedSlot();
		Coated coated = COATED.get(player);
		if (held.isEmpty()) {
			end(player, data, "nen.hxh.shu.empty");
		} else if (coated == null) {
			COATED.put(player, new Coated(held.copy(), slot));
		} else if (coated.slot != slot || !ItemStack.isSameItem(coated.stack, held)) {
			end(player, data, "nen.hxh.shu.faded");
		}
	}

	private static void end(ServerPlayer player, NenData data, String messageKey) {
		COATED.remove(player);
		player.setAttached(ModAttachments.NEN, data.toggled(Technique.SHU));
		player.sendOverlayMessage(Component.translatable(messageKey));
	}

	/** Dano a mais do golpe com o objeto revestido. */
	static double damageBonus(ServerPlayer player, NenData data) {
		if (!data.isActive(Technique.SHU)) {
			return 0;
		}
		return switch (kind(player.getMainHandItem())) {
			case WEAPON -> data.shuWeaponDamage();
			case COMMON -> data.shuCommonDamage();
			default -> 0;
		};
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
		ItemStack stack = player.getMainHandItem();
		return kind(stack) == Kind.TOOL || stack.is(ItemTags.AXES) ? data.shuToolSpeed() : 0;
	}

	/** Desgaste do objeto revestido: cada ponto tem chance de ser evitado. */
	public static int reduceWear(ServerPlayer player, ItemStack stack, int amount) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (amount <= 0 || !data.isActive(Technique.SHU) || stack != held(player)) {
			return amount;
		}
		int kept = 0;
		for (int i = 0; i < amount; i++) {
			if (player.getRandom().nextDouble() >= data.shuWearSave()) {
				kept++;
			}
		}
		return kept;
	}
}
