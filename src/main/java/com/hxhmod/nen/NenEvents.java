package com.hxhmod.nen;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class NenEvents {
	private static final int TICKS_PER_SECOND = 20;
	private static final double STILL_DISTANCE_SQR = 0.01;

	// Posicao de cada jogador no ultimo segundo, para saber se ele ficou parado meditando
	private static final Map<ServerPlayer, Vec3> LAST_POSITIONS = new WeakHashMap<>();
	// Parte fracionada da aura recuperada, para os bonus em porcentagem nao se perderem no arredondamento
	private static final Map<ServerPlayer, Double> AURA_REMAINDER = new WeakHashMap<>();

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
		NenData after = before.awakened() ? tickTraining(player, before, still) : tickMeditation(player, before, still);
		after = learnTechniques(player, after);
		if (!after.equals(before)) {
			player.setAttached(ModAttachments.NEN, after);
			announceLevelUp(player, before, after);
		}
		Divination.tick(player, still);
		NenPose.update(player);
		NenEffects.removeLegacyModifiers(player);
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

	private static NenData tickTraining(ServerPlayer player, NenData data, boolean still) {
		// So o que esta liberado e pode ser ligado; o Zetsu nunca fica junto com outra tecnica
		Set<Technique> active = EnumSet.noneOf(Technique.class);
		data.active().stream().filter(technique -> data.isUnlocked(technique) && technique.activatable()).forEach(active::add);
		if (active.contains(Technique.ZETSU) && active.size() > 1) {
			active.retainAll(Set.of(Technique.ZETSU));
		}
		NenData result = data.withActive(active);

		// A aura volta sempre e as tecnicas ligadas gastam juntas
		double regen = result.auraRegen(still) + AURA_REMAINDER.getOrDefault(player, 0.0);
		int whole = (int) regen;
		AURA_REMAINDER.put(player, regen - whole);
		int drain = result.totalDrain();
		if (result.aura() + whole < drain) {
			// Sem aura para manter: desliga o que gasta e fica so com o que nao gasta (Ten)
			active.removeIf(technique -> result.auraDrain(technique) > 0);
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return result.withActive(active).withAura(result.aura() + whole);
		}
		NenData updated = result.withAura(result.aura() + whole - drain);

		if (!active.isEmpty()) {
			NenEffects.applyTechniques(player, updated);
			heal(player, updated, still);
			for (Technique technique : active) {
				updated = updated.addTraining(technique, 1 + pertinentBonus(player, technique, still))
						.addXp(technique.xpPerSecond());
			}
		}
		return updated;
	}

	/**
	 * Aprende sozinho o que ja pode: os principios que faltarem (mundos de antes) e as tecnicas avancadas
	 * cuja proficiencia nas tecnicas base chegou no minimo pedido.
	 */
	private static NenData learnTechniques(ServerPlayer player, NenData data) {
		NenData result = data;
		for (Technique technique : Technique.TABS) {
			if (result.canLearn(technique)) {
				result = result.unlock(technique);
				if (!Technique.BASE.contains(technique)) {
					player.sendSystemMessage(Component.translatable("nen.hxh.learned", technique.displayName()));
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.4F);
				}
			}
		}
		return result;
	}

	/** Cura feita pelo proprio mod (sem o efeito de Regeneracao), que continua durante o combate. */
	private static void heal(ServerPlayer player, NenData data, boolean still) {
		double fraction = 0;
		if (data.isActive(Technique.TEN) && data.has(NenUpgrade.TEN_YOUTH)) {
			fraction = data.tenYouthHeal();
		} else if (data.isActive(Technique.ZETSU) && still && data.has(NenUpgrade.ZETSU_REST)) {
			fraction = NenRules.ZETSU_DEEP_REST_HEAL;
		}
		if (fraction > 0 && player.getHealth() < player.getMaxHealth()) {
			player.heal((float) (player.getMaxHealth() * fraction));
		}
	}

	/**
	 * Segundo extra de dominio quando a tecnica e usada do jeito certo:
	 * Ten aguentando golpes, Zetsu descansando parado.
	 */
	private static int pertinentBonus(ServerPlayer player, Technique active, boolean still) {
		return switch (active) {
			case TEN -> NenCombat.hurtRecently(player) ? 1 : 0;
			case ZETSU -> still ? 1 : 0;
			default -> 0;
		};
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
