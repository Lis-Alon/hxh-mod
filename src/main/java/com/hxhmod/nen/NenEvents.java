package com.hxhmod.nen;

import com.hxhmod.network.ToggleTechniquePayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class NenEvents {
	private static final int TICKS_PER_SECOND = 20;
	private static final int EFFECT_DURATION_TICKS = 40;
	private static final double STILL_DISTANCE_SQR = 0.01;

	// Posicao de cada jogador no ultimo segundo, para saber se ele ficou parado meditando
	private static final Map<ServerPlayer, Vec3> LAST_POSITIONS = new WeakHashMap<>();

	private NenEvents() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ToggleTechniquePayload.TYPE, ToggleTechniquePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ToggleTechniquePayload.TYPE,
				(payload, context) -> toggle(context.player(), payload.technique()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % TICKS_PER_SECOND != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});

		registerCommand();
	}

	private static void tickPlayer(ServerPlayer player) {
		NenData before = player.getAttachedOrCreate(ModAttachments.NEN);
		Vec3 lastPosition = LAST_POSITIONS.put(player, player.position());
		NenData after = before.awakened() ? tickTraining(player, before) : tickMeditation(player, before, lastPosition);
		if (!after.equals(before)) {
			player.setAttached(ModAttachments.NEN, after);
			announceProgress(player, before, after);
		}
	}

	private static NenData tickMeditation(ServerPlayer player, NenData data, Vec3 lastPosition) {
		boolean still = lastPosition != null && lastPosition.distanceToSqr(player.position()) < STILL_DISTANCE_SQR;
		if (!player.isShiftKeyDown() || !player.onGround() || !still) {
			return data;
		}
		int meditation = data.meditation() + 1;
		if (meditation < NenRules.MEDITATION_SECONDS) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.meditating", meditation, NenRules.MEDITATION_SECONDS));
			return data.withMeditation(meditation);
		}
		player.sendSystemMessage(Component.translatable("nen.hxh.awakened"));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
		return data.withMeditation(meditation).awaken(NenType.roll(player.getRandom()));
	}

	private static NenData tickTraining(ServerPlayer player, NenData data) {
		Technique active = data.active().isUnlocked(data) ? data.active() : Technique.NONE;
		NenData result = data.withActive(active);

		switch (active) {
			case NONE, TEN -> result = result.withAura(result.aura() + NenRules.REGEN);
			case ZETSU -> result = result.withAura(result.aura() + NenRules.ZETSU_REGEN);
			case REN -> {
				if (result.aura() <= NenRules.REN_DRAIN) {
					player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
					return result.withAura(0).withActive(Technique.NONE);
				}
				result = result.withAura(result.aura() - NenRules.REN_DRAIN);
			}
		}

		if (active != Technique.NONE) {
			applyEffect(player, active);
			result = result.addXp(active, 1);
		}
		return result;
	}

	private static void applyEffect(ServerPlayer player, Technique technique) {
		Holder<MobEffect> effect = switch (technique) {
			case TEN -> MobEffects.RESISTANCE;
			case ZETSU -> MobEffects.INVISIBILITY;
			case REN -> MobEffects.STRENGTH;
			case NONE -> null;
		};
		if (effect != null) {
			player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, 0, true, false, true));
		}
	}

	private static void announceProgress(ServerPlayer player, NenData before, NenData after) {
		if (after.level() > before.level()) {
			player.sendSystemMessage(Component.translatable("nen.hxh.level_up", after.level(), after.maxAura()));
		}
		for (Technique technique : Technique.TRAINABLE) {
			if (technique != Technique.TEN && technique.isUnlocked(after) && !technique.isUnlocked(before)) {
				player.sendSystemMessage(Component.translatable("nen.hxh.unlocked", technique.displayName()));
			}
		}
	}

	/** Liga a tecnica pedida, ou desliga se ela ja estiver ativa. */
	public static void toggle(ServerPlayer player, Technique technique) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!technique.isUnlocked(data)) {
			return;
		}
		Technique next = data.active() == technique ? Technique.NONE : technique;
		if (next == Technique.REN && data.aura() <= NenRules.REN_DRAIN) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return;
		}
		player.setAttached(ModAttachments.NEN, data.withActive(next));
	}

	private static void registerCommand() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				Commands.literal("nen")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
							if (!data.awakened()) {
								context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.not_awakened"), false);
							} else {
								Component type = data.revealed() ? data.type().displayName() : Component.literal("???");
								context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.status",
										type, data.level(), data.aura(), data.maxAura()), false);
							}
							return 1;
						})
						// /nen reset: apaga todo o Nen para recomecar do zero (so operadores)
						.then(Commands.literal("reset")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> {
									ServerPlayer player = context.getSource().getPlayerOrException();
									player.setAttached(ModAttachments.NEN, NenData.DEFAULT);
									context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.reset"), false);
									return 1;
								}))));
	}
}
