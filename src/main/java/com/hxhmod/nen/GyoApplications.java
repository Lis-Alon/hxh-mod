package com.hxhmod.nen;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Aplicacoes do Gyo: com o Gyo ligado, cada tecla leva a aura dos olhos para uma parte do corpo: super soco,
 * super defesa, super pulo e super dash. O Gyo continua ligado, e cada aplicacao entra na propria recarga.
 * A tecla do soco deixa o proximo golpe pronto; quem aplica o bonus e {@link NenCombat}, quando o golpe acerta.
 */
public final class GyoApplications {
	public enum Action {
		GUARD, JUMP, DASH, PUNCH;

		public static final StreamCodec<ByteBuf, Action> STREAM_CODEC =
				ByteBufCodecs.VAR_INT.map(i -> values()[i], Action::ordinal);

		public String keyName() {
			return "key.hxh.gyo." + name().toLowerCase(Locale.ROOT);
		}
	}

	/** Os nomes das teclas atuais de quem le, na ordem soco, defesa, pulo e dash, para usar nos textos. */
	public static Object[] keys() {
		return new Object[] {Component.keybind(Action.PUNCH.keyName()), Component.keybind(Action.GUARD.keyName()),
				Component.keybind(Action.JUMP.keyName()), Component.keybind(Action.DASH.keyName())};
	}

	// Ate que tick dura a super defesa, a exposicao depois de concentrar a aura e a queda sem dano do super pulo
	private static final Map<ServerPlayer, Integer> GUARD_UNTIL = new WeakHashMap<>();
	private static final Map<ServerPlayer, Integer> EXPOSED_UNTIL = new WeakHashMap<>();
	private static final Map<ServerPlayer, Integer> SAFE_LANDING_UNTIL = new WeakHashMap<>();
	// Ate que tick o proximo golpe vira super soco, depois de apertar a tecla
	private static final Map<ServerPlayer, Integer> PUNCH_READY_UNTIL = new WeakHashMap<>();

	private GyoApplications() {
	}

	public static void use(ServerPlayer player, Action action, float directionX, float directionZ) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.isActive(Technique.GYO) || remaining(player, action) > 0 || (action == Action.JUMP && !player.onGround())) {
			return;
		}
		if (data.aura() < data.gyoCost()) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return;
		}
		// O soco so gasta quando acerta: errar nao custa nada
		if (action == Action.PUNCH) {
			PUNCH_READY_UNTIL.put(player, player.tickCount + NenRules.GYO_PUNCH_READY_TICKS);
			player.sendOverlayMessage(Component.translatable("nen.hxh.gyo.punch_ready", NenRules.GYO_PUNCH_READY_TICKS / 20));
			return;
		}
		consume(player, data, action);
		switch (action) {
			case GUARD -> {
				GUARD_UNTIL.put(player, player.tickCount + NenRules.GYO_GUARD_TICKS);
				feedback(player, "nen.hxh.gyo.guard");
			}
			case JUMP -> {
				expose(player);
				SAFE_LANDING_UNTIL.put(player, player.tickCount + NenRules.GYO_SAFE_LANDING_TICKS);
				Vec3 motion = player.getDeltaMovement();
				launch(player, new Vec3(motion.x, data.gyoJump(), motion.z));
				feedback(player, "nen.hxh.gyo.jump");
				NenTraining.gyoMove(player);
			}
			case DASH -> {
				expose(player);
				Vec3 direction = new Vec3(directionX, 0, directionZ);
				if (direction.lengthSqr() < 1.0E-4) {
					direction = player.getLookAngle().multiply(1, 0, 1);
				}
				launch(player, direction.normalize().scale(data.gyoDash()).add(0, NenRules.GYO_DASH_LIFT, 0));
				feedback(player, "nen.hxh.gyo.dash");
				NenTraining.gyoMove(player);
			}
			case PUNCH -> {
			}
		}
	}

	/** Bonus de dano do super soco. Zero se a tecla nao foi apertada antes, o Gyo estiver desligado ou faltar aura. */
	static double punch(ServerPlayer attacker) {
		NenData data = attacker.getAttachedOrCreate(ModAttachments.NEN);
		Integer readyUntil = PUNCH_READY_UNTIL.get(attacker);
		if (readyUntil == null || attacker.tickCount >= readyUntil || !data.isActive(Technique.GYO)
				|| remaining(attacker, Action.PUNCH) > 0 || data.aura() < data.gyoCost()) {
			return 0;
		}
		PUNCH_READY_UNTIL.remove(attacker);
		consume(attacker, data, Action.PUNCH);
		expose(attacker);
		feedback(attacker, "nen.hxh.gyo.punch");
		return data.gyoPunch();
	}

	/** Multiplicador do dano causado: na super defesa a aura esta na guarda, nao no golpe. */
	static double damageDealt(ServerPlayer player) {
		return guarding(player) ? 1 - NenRules.GYO_GUARD_OFFENSE_LOSS : 1;
	}

	/** Multiplicador do dano recebido: menor na guarda, maior logo depois de concentrar a aura em outro lugar. */
	static double damageTaken(ServerPlayer player) {
		if (guarding(player)) {
			return 1 - player.getAttachedOrCreate(ModAttachments.NEN).gyoGuard();
		}
		return player.tickCount < EXPOSED_UNTIL.getOrDefault(player, 0) ? 1 + NenRules.GYO_EXPOSED_DAMAGE : 1;
	}

	public static boolean guarding(ServerPlayer player) {
		return player.tickCount < GUARD_UNTIL.getOrDefault(player, 0);
	}

	/** A queda do super pulo nao machuca. */
	public static boolean consumeSafeLanding(ServerPlayer player) {
		Integer until = SAFE_LANDING_UNTIL.remove(player);
		return until != null && player.tickCount < until;
	}

	/** Ticks de recarga que faltam para esta aplicacao. */
	public static int remaining(Player player, Action action) {
		return player.getAttachedOrElse(ModAttachments.GYO_COOLDOWNS, GyoCooldowns.NONE).remaining(action, player.level().getGameTime());
	}

	private static void consume(ServerPlayer player, NenData data, Action action) {
		NenData after = data.withAura(data.aura() - data.gyoCost())
				.addTraining(Technique.GYO, NenRules.GYO_APPLICATION_TRAINING);
		player.setAttached(ModAttachments.NEN, after);
		long until = player.level().getGameTime() + data.gyoCooldownTicks();
		player.setAttached(ModAttachments.GYO_COOLDOWNS, player.getAttachedOrCreate(ModAttachments.GYO_COOLDOWNS).started(action, until));
	}

	private static void expose(ServerPlayer player) {
		EXPOSED_UNTIL.put(player, player.tickCount + NenRules.GYO_EXPOSED_TICKS);
	}

	/** O movimento do jogador e do cliente: o servidor precisa mandar a nova velocidade. */
	private static void launch(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	private static void feedback(ServerPlayer player, String messageKey) {
		player.sendOverlayMessage(Component.translatable(messageKey));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.2F);
		AuraVisuals.burst(player, player.getAttachedOrCreate(ModAttachments.NEN), ParticleTypes.CRIT, 12);
	}
}
