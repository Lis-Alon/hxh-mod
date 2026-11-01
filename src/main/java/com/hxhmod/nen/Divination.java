package com.hxhmod.nen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Videncia com o copo d'agua e a folha (o teste de Nen do anime).
 * Da para treinar segurando o copo na mao ou meditando na frente do copo colocado no chao.
 */
public final class Divination {
	private static final double MAX_DISTANCE_SQR = 4.0 * 4.0;

	/** Jogadores meditando na frente de um copo no chao: posicao do copo e segundos ja concentrados. */
	private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

	private record Session(BlockPos glass, Block block, int seconds) {
	}

	private Divination() {
	}

	/** Motivo para nao poder treinar agora, ou null se estiver tudo certo. */
	public static Component problem(NenData data) {
		if (!data.awakened()) {
			return Component.translatable("nen.hxh.divination.need_awakening");
		}
		if (data.revealed()) {
			return Component.translatable("nen.hxh.already_known", data.type().displayName());
		}
		if (!data.isUnlocked(Technique.REN)) {
			return Component.translatable("nen.hxh.divination.need_ren");
		}
		if (data.aura() < NenRules.DIVINATION_AURA_COST) {
			return Component.translatable("nen.hxh.divination.need_aura", NenRules.DIVINATION_AURA_COST);
		}
		return null;
	}

	/** Termina uma sessao: gasta aura, da XP e avanca a leitura, revelando o tipo ao chegar em 100%. */
	public static void perform(ServerPlayer player) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		Component problem = problem(data);
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return;
		}

		int gain = NenRules.DIVINATION_MIN_GAIN
				+ player.getRandom().nextInt(NenRules.DIVINATION_MAX_GAIN - NenRules.DIVINATION_MIN_GAIN + 1);
		NenData result = data.withAura(data.aura() - NenRules.DIVINATION_AURA_COST)
				.addXp(NenRules.DIVINATION_XP)
				.withDivination(data.divination() + gain);
		player.setAttached(ModAttachments.NEN, result);

		if (result.revealed()) {
			player.sendSystemMessage(result.type().waterReaction());
			player.sendSystemMessage(Component.translatable("nen.hxh.revealed", result.type().displayName()));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
		} else {
			Component hint = result.divination() >= NenRules.DIVINATION_HINT_AT
					? result.type().waterHint()
					: Component.translatable("nen.hxh.divination.nothing");
			player.sendSystemMessage(hint);
			player.sendOverlayMessage(Component.translatable("nen.hxh.divination.progress", result.divination()));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.0F);
		}
	}

	/** Comeca a meditar na frente do copo colocado no chao. */
	public static void start(ServerPlayer player, BlockPos glass) {
		Component problem = problem(player.getAttachedOrCreate(ModAttachments.NEN));
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return;
		}
		SESSIONS.put(player, new Session(glass.immutable(), player.level().getBlockState(glass).getBlock(), 0));
		player.sendOverlayMessage(Component.translatable("nen.hxh.divination.focusing", 0, NenRules.DIVINATION_SECONDS));
		NenPose.update(player);
	}

	public static boolean inSession(ServerPlayer player) {
		return SESSIONS.containsKey(player);
	}

	/** Chamado uma vez por segundo. Para a sessao se o jogador andar, se afastar ou o copo sumir. */
	static void tick(ServerPlayer player, boolean still) {
		Session session = SESSIONS.get(player);
		if (session == null) {
			return;
		}
		ServerLevel level = player.level();
		boolean glassThere = level.getBlockState(session.glass()).is(session.block());
		boolean close = player.position().distanceToSqr(Vec3.atCenterOf(session.glass())) <= MAX_DISTANCE_SQR;
		if (!glassThere || !close || !still) {
			SESSIONS.remove(player);
			player.sendOverlayMessage(Component.translatable("nen.hxh.divination.interrupted"));
			return;
		}

		int seconds = session.seconds() + 1;
		level.sendParticles(ParticleTypes.SPLASH, session.glass().getX() + 0.5, session.glass().getY() + 0.45,
				session.glass().getZ() + 0.5, 6, 0.08, 0.02, 0.08, 0.0);
		if (seconds < NenRules.DIVINATION_SECONDS) {
			SESSIONS.put(player, new Session(session.glass(), session.block(), seconds));
			player.sendOverlayMessage(Component.translatable("nen.hxh.divination.focusing", seconds, NenRules.DIVINATION_SECONDS));
			return;
		}
		SESSIONS.remove(player);
		perform(player);
	}
}
