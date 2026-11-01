package com.hxhmod.nen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

/** O que o servidor faz quando o painel pede para ligar, desbloquear ou distribuir pontos. */
public final class NenActions {
	private NenActions() {
	}

	/** Liga a tecnica pedida, ou desliga se ela ja estiver ativa. */
	public static void toggle(ServerPlayer player, Technique technique) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!technique.activatable() || !data.isUnlocked(technique)) {
			return;
		}
		Technique next = data.active() == technique ? Technique.NONE : technique;
		if (next != Technique.NONE && data.aura() < data.auraDrain(next)) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return;
		}
		player.setAttached(ModAttachments.NEN, data.withActive(next));
	}

	public static void unlock(ServerPlayer player, Technique technique) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (data.unlockProblem(technique) != null) {
			return;
		}
		player.setAttached(ModAttachments.NEN, data.unlock(technique));
		player.sendSystemMessage(Component.translatable("nen.hxh.unlocked", technique.displayName()));
		celebrate(player);
	}

	public static void buy(ServerPlayer player, NenUpgrade upgrade) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (data.upgradeProblem(upgrade) != null) {
			return;
		}
		player.setAttached(ModAttachments.NEN, data.buy(upgrade));
		player.sendSystemMessage(Component.translatable("nen.hxh.upgrade_bought", upgrade.displayName()));
		celebrate(player);
	}

	/** Aplica os pontos so se o pedido inteiro for valido (pontos suficientes e sem passar do limite). */
	public static void allocate(ServerPlayer player, Map<NenStat, Integer> points) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		int total = 0;
		for (Map.Entry<NenStat, Integer> entry : points.entrySet()) {
			int amount = entry.getValue();
			if (amount < 0 || data.statPoints(entry.getKey()) + amount > entry.getKey().maxPoints()) {
				return;
			}
			total += amount;
		}
		if (!data.awakened() || total == 0 || total > data.pointsAvailable()) {
			return;
		}
		player.setAttached(ModAttachments.NEN, data.addStats(points));
		NenEffects.applyStats(player, player.getAttachedOrCreate(ModAttachments.NEN));
		celebrate(player);
	}

	private static void celebrate(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
	}
}
