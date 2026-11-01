package com.hxhmod.nen;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class NenEvents {
	private static final int TICKS_PER_SECOND = 20;
	private static final double STILL_DISTANCE_SQR = 0.01;

	// Posicao de cada jogador no ultimo segundo, para saber se ele ficou parado meditando
	private static final Map<ServerPlayer, Vec3> LAST_POSITIONS = new WeakHashMap<>();

	private NenEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % TICKS_PER_SECOND != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> NenCommand.register(dispatcher));
	}

	private static void tickPlayer(ServerPlayer player) {
		NenData before = player.getAttachedOrCreate(ModAttachments.NEN);
		Vec3 lastPosition = LAST_POSITIONS.put(player, player.position());
		boolean still = lastPosition != null && lastPosition.distanceToSqr(player.position()) < STILL_DISTANCE_SQR;
		NenData after = before.awakened() ? tickTraining(player, before) : tickMeditation(player, before, still);
		if (!after.equals(before)) {
			player.setAttached(ModAttachments.NEN, after);
			announceLevelUp(player, before, after);
		}
		Divination.tick(player, still);
		NenPose.update(player);
		NenEffects.applyStats(player, after);
	}

	private static NenData tickMeditation(ServerPlayer player, NenData data, boolean still) {
		// Medita agachado ou sentado com o painel aberto, parado no chao
		boolean meditating = (player.isShiftKeyDown() || NenPose.panelOpen(player)) && player.onGround() && still;
		NenPose.setAwakening(player, meditating);
		if (!meditating) {
			return data;
		}
		int meditation = data.meditation() + 1;
		if (meditation < NenRules.MEDITATION_SECONDS) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.meditating", meditation, NenRules.MEDITATION_SECONDS));
			return data.withMeditation(meditation);
		}
		NenPose.setAwakening(player, false);
		player.sendSystemMessage(Component.translatable("nen.hxh.awakened"));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
		return data.withMeditation(meditation).awaken(NenType.roll(player.getRandom()));
	}

	private static NenData tickTraining(ServerPlayer player, NenData data) {
		Technique active = data.isUnlocked(data.active()) && data.active().activatable() ? data.active() : Technique.NONE;
		NenData result = data.withActive(active);

		int drain = result.auraDrain(active);
		if (drain > 0) {
			if (result.aura() < drain) {
				player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
				return result.withActive(Technique.NONE);
			}
			result = result.withAura(result.aura() - drain);
		} else {
			result = result.withAura(result.aura() + result.auraRegen());
		}

		if (active != Technique.NONE) {
			NenEffects.applyTechnique(player, result);
			result = result.addTraining(active, 1).addXp(active.xpPerSecond());
		}
		return result;
	}

	static void announceLevelUp(ServerPlayer player, NenData before, NenData after) {
		if (after.level() > before.level()) {
			int gained = NenRules.pointsEarned(after.level()) - NenRules.pointsEarned(before.level());
			player.sendSystemMessage(Component.translatable("nen.hxh.level_up", after.level(), gained));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}
}
