package com.hxhmod.nen;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** O que o servidor faz quando o painel pede para ligar uma tecnica ou comprar uma funcionalidade. */
public final class NenActions {
	private NenActions() {
	}

	/**
	 * Liga a tecnica pedida, ou desliga se ela ja estiver ativa.
	 * Varias ficam ligadas juntas; o Zetsu desliga as outras e e desligado por qualquer outra.
	 */
	public static void toggle(ServerPlayer player, Technique technique) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!technique.activatable() || !data.isUnlocked(technique)) {
			return;
		}
		NenData next = data.toggled(technique);
		if (next.isActive(technique) && data.aura() < next.totalDrain()) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return;
		}
		player.setAttached(ModAttachments.NEN, next);
		if (next.isActive(technique)) {
			NenTraining.switched(player, technique);
		}
		if (technique == Technique.GYO && next.isActive(technique)) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.gyo.prepared", GyoApplications.keys()));
		}
	}

	/** Passa para a proxima intensidade da tecnica (no En, o tamanho da area). */
	public static void cycleIntensity(ServerPlayer player, Technique technique) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (technique.maxIntensity() <= 1 || !data.isUnlocked(technique)) {
			return;
		}
		NenData next = data.cycledIntensity(technique);
		player.setAttached(ModAttachments.NEN, next);
		player.sendOverlayMessage(technique == Technique.EN
				? Component.translatable("nen.hxh.en.size", Math.round(next.enSize()))
				: Component.translatable("nen.hxh.intensity", technique.displayName(), next.intensityOf(technique), technique.maxIntensity()));
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

	/** Botao Resetar da arvore: devolve os pontos das funcionalidades compradas. */
	public static void resetUpgrades(ServerPlayer player) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		player.setAttached(ModAttachments.NEN, data.resetUpgrades());
		player.sendSystemMessage(Component.translatable("nen.hxh.upgrades_reset"));
	}

	private static void celebrate(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
	}
}
