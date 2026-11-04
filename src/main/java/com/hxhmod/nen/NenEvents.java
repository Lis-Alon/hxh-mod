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
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickTechniques(player, server.getTickCount());
			}
			if (server.getTickCount() % TICKS_PER_SECOND != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});

		NenTraining.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> NenCommand.register(dispatcher));
	}

	/** O que precisa andar a cada tick: objeto do Shu, carga do Ko, postura do Ryu e aura visivel. */
	private static void tickTechniques(ServerPlayer player, int tick) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.awakened()) {
			AuraVisuals.publish(player, data);
			return;
		}
		ShuCoat.tick(player, data);
		KoStrike.tick(player, data);
		RyuFlow.tick(player, data);
		AuraVisuals.publish(player, player.getAttachedOrCreate(ModAttachments.NEN));
	}

	private static void tickPlayer(ServerPlayer player) {
		NenData before = player.getAttachedOrCreate(ModAttachments.NEN);
		Vec3 lastPosition = LAST_POSITIONS.put(player, player.position());
		boolean still = lastPosition != null && lastPosition.distanceToSqr(player.position()) < STILL_DISTANCE_SQR;
		NenData after = before.awakened() ? tickTraining(player, before, still) : tickMeditation(player, before, still);
		after = learnTechniques(player, after);
		after = NenTraining.second(player, before, after);
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
		// O Ryu redistribui a aura do Ken
		if (!active.contains(Technique.KEN)) {
			active.remove(Technique.RYU);
		}
		NenData result = data.withActive(active);

		// A aura volta sempre e as tecnicas ligadas gastam juntas
		double regen = result.auraRegen(still);
		double balance = regen - result.netDrain() + AURA_REMAINDER.getOrDefault(player, 0.0);
		int whole = (int) Math.floor(balance);
		if (result.aura() + whole < 0) {
			// Sem aura para manter: desliga o que gasta e fica so com o que nao gasta (Ten)
			boolean intense = active.contains(Technique.REN) || active.contains(Technique.KO);
			active.removeIf(technique -> result.auraDrain(technique) > 0);
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			AURA_REMAINDER.put(player, 0.0);
			NenData drained = result.withActive(active).withAura(result.aura() + (int) regen);
			return intense ? NenTraining.exhausted(player, drained) : drained;
		}
		AURA_REMAINDER.put(player, balance - whole);
		NenData updated = result.withAura(result.aura() + whole);

		if (!active.isEmpty()) {
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
			double recovery = 1 + data.attributeLevel(NenStat.VITALITY) * NenRules.VITALITY_RECOVERY;
			player.heal((float) (player.getMaxHealth() * fraction * recovery));
		}
	}

	/**
	 * Segundo extra de dominio quando a tecnica e usada do jeito certo:
	 * Ten e Ken aguentando golpes, Ren, Shu e Ryu acertando golpes, Zetsu descansando parado.
	 */
	private static int pertinentBonus(ServerPlayer player, Technique active, boolean still) {
		return switch (active) {
			case TEN, KEN -> NenCombat.hurtRecently(player) ? 1 : 0;
			case REN, RYU -> NenCombat.attackedRecently(player) ? 1 : 0;
			case SHU -> NenCombat.attackedRecently(player) || ShuCoat.usedRecently(player) ? 1 : 0;
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
