package com.hxhmod.nen;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Treino organico dos atributos: em vez de receber pontos, cada um ganha XP quando o jogador faz o que o treina.
 * 1 XP vale mais ou menos 1 segundo de treino simples. O que acontece a cada segundo vem de {@link #second};
 * o resto vem dos eventos de combate, das tecnicas e das interacoes com o mundo.
 * <p>
 * O multiplicador sobe em combate e cai se o jogador ficar parado no mesmo lugar sem mexer em nada. Meditar e
 * descansar sao treinos parados de proposito, entao a XP de recuperar aura e vida nao perde por isso.
 */
public final class NenTraining {
	private static final int NEVER = -1_000_000;

	// Vitalidade: segundos de Ten e vida recuperada
	private static final double TEN_XP = 1;
	private static final double HEALED_XP = 3;
	// Capacidade: encher a barra inteira em Zetsu (vale a fracao, para uma barra grande nao render mais) e esgotar a aura
	private static final double ZETSU_REFILL_XP = 25;
	private static final double EXHAUSTED_XP = 60;
	private static final int EXHAUSTED_COOLDOWN_TICKS = 60 * 20;
	private static final int EXHAUSTED_AURA = 2;
	// Fluxo: aura correndo no combate, que rende ate o dobro sem interrupcao, Fluidez da Aura e trocas rapidas de estado
	private static final double FLOW_XP = 1;
	private static final double FLOW_STREAK_XP = 1;
	private static final int FLOW_STREAK_SECONDS = 60;
	private static final double TEN_FLOW_XP = 0.25;
	private static final double SWITCH_XP = 3;
	private static final int SWITCH_WINDOW_TICKS = 3 * 20;
	private static final int SWITCH_GAP_TICKS = 2 * 20;
	// Ataque: por ponto de dano reforcado pelo Nen, com um teto por golpe
	private static final double HIT_XP = 0.6;
	private static final double HIT_MAX_XP = 30;
	private static final double KO_HIT_SCALE = 1.5;
	// Protecao: por ponto de dano absorvido ou bloqueado, com um teto por golpe
	private static final double ABSORBED_XP = 4;
	private static final double BLOCKED_XP = 2;
	private static final double DEFENSE_MAX_XP = 30;
	// Agilidade: super pulo e super dash, e movimento, golpes e trocas de postura com o Ryu
	private static final double GYO_MOVE_XP = 8;
	private static final double RYU_MOVE_XP = 0.75;
	private static final double RYU_MOVE_DISTANCE_SQR = 3 * 3;
	private static final double RYU_HIT_XP = 1.5;
	private static final double RYU_STANCE_XP = 2;
	private static final int RYU_STANCE_GAP_TICKS = 2 * 20;
	// Percepcao: segundos de En, criaturas rastreadas e alvos ocultos achados com En ou Gyo
	private static final double EN_XP = 1;
	private static final double EN_TRACKED_XP = 0.1;
	private static final double EN_TRACKED_MAX_XP = 1;
	private static final double HIDDEN_XP = 1.5;
	private static final int HIDDEN_MAX = 3;

	// Combate: 1,5x ao comecar, 2x depois de manter a luta ou contra outro jogador
	private static final int COMBAT_TICKS = 8 * 20;
	private static final int COMBAT_RAMP_TICKS = 30 * 20;
	private static final double COMBAT_MIN = 1.5;
	private static final double COMBAT_MAX = 2.0;
	// Parado: sem perda no primeiro minuto, depois cai ate 20% em mais 3 minutos
	private static final double IDLE_DISTANCE_SQR = 2 * 2;
	private static final int IDLE_GRACE_SECONDS = 60;
	private static final int IDLE_RAMP_SECONDS = 180;
	private static final double IDLE_MIN = 0.2;

	/** O que so importa enquanto o jogador esta no mundo; a XP em si fica em {@link NenData}. */
	private static final class Session {
		Vec3 anchor;
		Vec3 lastPosition;
		int idleSeconds;
		float lastHealth = -1;
		int lastCombat = NEVER;
		int lastPlayerCombat = NEVER;
		int combatStart;
		int flowSeconds;
		Technique lastState;
		int lastSwitch = NEVER;
		int lastSwitchXp = NEVER;
		int lastExhausted = NEVER;
		int lastStanceXp = NEVER;
	}

	private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

	private NenTraining() {
	}

	/** Mexer em blocos e criaturas conta como interagir, mesmo sem sair do lugar. */
	static void register() {
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			interacted(player);
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			interacted(player);
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			interacted(player);
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> interacted(player));
	}

	private static void interacted(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			session(serverPlayer).idleSeconds = 0;
		}
	}

	private static Session session(ServerPlayer player) {
		return SESSIONS.computeIfAbsent(player, unused -> new Session());
	}

	// ---- Multiplicador ----

	/** Multiplicador de agora: maior em combate, menor parado no mesmo lugar. */
	public static double multiplier(ServerPlayer player) {
		Session session = session(player);
		int now = player.tickCount;
		if (now - session.lastPlayerCombat <= COMBAT_TICKS) {
			return COMBAT_MAX;
		}
		if (now - session.lastCombat <= COMBAT_TICKS) {
			return combatFactor(now - session.combatStart);
		}
		return idleFactor(session.idleSeconds);
	}

	/** De 1,5x a 2x conforme a luta continua. */
	public static double combatFactor(int combatTicks) {
		return Mth.lerp(Mth.clamp((double) combatTicks / COMBAT_RAMP_TICKS, 0, 1), COMBAT_MIN, COMBAT_MAX);
	}

	/** De 1x a 0,2x conforme o tempo no mesmo lugar sem interagir. */
	public static double idleFactor(int idleSeconds) {
		double ramp = (double) (idleSeconds - IDLE_GRACE_SECONDS) / IDLE_RAMP_SECONDS;
		return Mth.lerp(Mth.clamp(ramp, 0, 1), 1, IDLE_MIN);
	}

	/** Para meditar e descansar: o combate ainda ajuda, mas ficar parado nao atrapalha. */
	private static double restMultiplier(ServerPlayer player) {
		return Math.max(1, multiplier(player));
	}

	// ---- A cada segundo ----

	/**
	 * Treino que corre com o tempo.
	 *
	 * @param before como o jogador estava antes deste segundo, para medir a aura recuperada
	 * @param after  como ele ficou depois de pagar as tecnicas
	 */
	static NenData second(ServerPlayer player, NenData before, NenData after) {
		Session session = session(player);
		trackPlace(session, player);
		float health = player.getHealth();
		float healed = session.lastHealth < 0 ? 0 : Math.max(0, health - session.lastHealth);
		session.lastHealth = health;
		Vec3 lastPosition = session.lastPosition;
		session.lastPosition = player.position();
		if (!after.awakened()) {
			return after;
		}

		double multiplier = multiplier(player);
		double rest = restMultiplier(player);
		NenData data = after;

		// Vitalidade
		if (data.isActive(Technique.TEN)) {
			data = gain(player, data, NenStat.VITALITY, TEN_XP * multiplier);
		}
		data = gain(player, data, NenStat.VITALITY, healed * HEALED_XP * rest);

		// Capacidade de Aura
		if (data.isActive(Technique.ZETSU) && data.maxAura() > 0) {
			double refilled = (double) Math.max(0, after.aura() - before.aura()) / data.maxAura();
			data = gain(player, data, NenStat.AURA_CAPACITY, refilled * ZETSU_REFILL_XP * rest);
		}

		// Fluxo de Aura: parar de sustentar a aura ou sair da luta zera a sequencia
		boolean fighting = player.tickCount - session.lastCombat <= COMBAT_TICKS;
		boolean flowing = fighting && !data.active().isEmpty() && !data.isActive(Technique.ZETSU) && data.aura() > 0;
		session.flowSeconds = flowing ? session.flowSeconds + 1 : 0;
		if (flowing) {
			double streak = Math.min(1, (double) session.flowSeconds / FLOW_STREAK_SECONDS);
			data = gain(player, data, NenStat.AURA_FLOW, (FLOW_XP + FLOW_STREAK_XP * streak) * multiplier);
		}
		if (data.isActive(Technique.TEN) && data.has(NenUpgrade.TEN_FLOW)) {
			data = gain(player, data, NenStat.AURA_FLOW, TEN_FLOW_XP * multiplier);
		}

		// Agilidade
		if (data.isActive(Technique.RYU) && lastPosition != null
				&& lastPosition.distanceToSqr(player.position()) >= RYU_MOVE_DISTANCE_SQR) {
			data = gain(player, data, NenStat.AGILITY, RYU_MOVE_XP * multiplier);
		}

		return perception(player, data, multiplier);
	}

	private static void trackPlace(Session session, ServerPlayer player) {
		if (session.anchor == null || session.anchor.distanceToSqr(player.position()) > IDLE_DISTANCE_SQR) {
			session.anchor = player.position();
			session.idleSeconds = 0;
		} else {
			session.idleSeconds++;
		}
	}

	private static NenData perception(ServerPlayer player, NenData data, double multiplier) {
		boolean en = data.isActive(Technique.EN);
		boolean gyo = data.isActive(Technique.GYO);
		if (!en && !gyo) {
			return data;
		}
		double enRadius = en ? data.enSize() / 2 : 0;
		double gyoRadius = gyo ? NenRules.GYO_REVEAL_RADIUS : 0;
		double radius = Math.max(enRadius, gyoRadius);
		List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand));
		int tracked = 0;
		int hidden = 0;
		for (LivingEntity entity : nearby) {
			double distance = player.distanceTo(entity);
			boolean sensed = distance <= enRadius;
			if (sensed) {
				tracked++;
			}
			if (hidden(entity) && (sensed || (distance <= gyoRadius && player.hasLineOfSight(entity)))) {
				hidden++;
			}
		}
		double xp = Math.min(hidden, HIDDEN_MAX) * HIDDEN_XP;
		if (en) {
			xp += EN_XP + Math.min(EN_TRACKED_MAX_XP, tracked * EN_TRACKED_XP);
		}
		return gain(player, data, NenStat.PERCEPTION, xp * multiplier);
	}

	/** Invisivel, ou jogador escondendo a aura com Zetsu ou In. */
	private static boolean hidden(LivingEntity entity) {
		if (entity.isInvisible()) {
			return true;
		}
		if (entity instanceof ServerPlayer other) {
			NenData data = other.getAttachedOrCreate(ModAttachments.NEN);
			return data.isActive(Technique.ZETSU) || data.isActive(Technique.IN);
		}
		return false;
	}

	// ---- Eventos ----

	/** Bateu ou apanhou de uma criatura: conta como combate para os dois lados. */
	static void combat(ServerPlayer player, LivingEntity opponent) {
		if (opponent instanceof ArmorStand) {
			return;
		}
		Session session = session(player);
		int now = player.tickCount;
		if (now - session.lastCombat > COMBAT_TICKS) {
			session.combatStart = now;
		}
		session.lastCombat = now;
		if (opponent instanceof Player) {
			session.lastPlayerCombat = now;
		}
		session.idleSeconds = 0;
	}

	/**
	 * Golpe que acertou.
	 *
	 * @param damage       dano ja com os bonus
	 * @param strengthened o Nen reforcou o golpe (Ren, Ko, Shu ou super soco)
	 * @param ko           foi um impacto de Ko
	 */
	static void landedHit(ServerPlayer attacker, LivingEntity victim, float damage, boolean strengthened, boolean ko) {
		if (victim instanceof ArmorStand || victim.getInvulnerableTime() > 10) {
			return;
		}
		double multiplier = multiplier(attacker);
		NenData data = attacker.getAttachedOrCreate(ModAttachments.NEN);
		if (strengthened) {
			// O que passou da vida do alvo nao conta, para um Ko em cima de uma galinha nao valer como um num chefe
			double dealt = Math.min(damage, victim.getHealth());
			double xp = Math.min(HIT_MAX_XP, dealt * HIT_XP * (ko ? KO_HIT_SCALE : 1));
			data = gain(attacker, data, NenStat.ATTACK_POWER, xp * multiplier);
		}
		if (data.isActive(Technique.RYU)) {
			data = gain(attacker, data, NenStat.AGILITY, RYU_HIT_XP * multiplier);
		}
		attacker.setAttached(ModAttachments.NEN, data);
	}

	/** Dano que o Ten, o Ken ou a super defesa seguraram. */
	static void absorbed(ServerPlayer player, NenData data, float amount) {
		if (amount <= 0 || player.getInvulnerableTime() > 10 || player.getAbilities().invulnerable) {
			return;
		}
		if (data.isActive(Technique.TEN) || data.isActive(Technique.KEN) || GyoApplications.guarding(player)) {
			award(player, NenStat.PROTECTION, Math.min(DEFENSE_MAX_XP, amount * ABSORBED_XP) * multiplier(player));
		}
	}

	/** Dano que um escudo segurou com alguma tecnica de Nen ligada. */
	public static void blocked(ServerPlayer player, float amount) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (amount > 0 && !data.active().isEmpty() && !data.isActive(Technique.ZETSU)) {
			award(player, NenStat.PROTECTION, Math.min(DEFENSE_MAX_XP, amount * BLOCKED_XP) * multiplier(player));
		}
	}

	/** Ligou uma tecnica: trocar logo em seguida entre Zetsu, Ten e Ren treina o Fluxo. */
	static void switched(ServerPlayer player, Technique technique) {
		if (technique != Technique.ZETSU && technique != Technique.TEN && technique != Technique.REN) {
			return;
		}
		Session session = session(player);
		int now = player.tickCount;
		boolean fast = technique != session.lastState && now - session.lastSwitch <= SWITCH_WINDOW_TICKS;
		if (fast && now - session.lastSwitchXp >= SWITCH_GAP_TICKS) {
			session.lastSwitchXp = now;
			award(player, NenStat.AURA_FLOW, SWITCH_XP * multiplier(player));
		}
		session.lastState = technique;
		session.lastSwitch = now;
	}

	/** Ko liberado: se levou quase toda a aura junto, conta como esgotar. */
	static NenData koReleased(ServerPlayer player, NenData after) {
		return after.aura() <= EXHAUSTED_AURA ? exhausted(player, after) : after;
	}

	/** Esvaziou a aura com Ren ou Ko: no maximo uma vez por minuto, porque a barra precisa ter enchido de novo. */
	static NenData exhausted(ServerPlayer player, NenData data) {
		Session session = session(player);
		if (player.tickCount - session.lastExhausted < EXHAUSTED_COOLDOWN_TICKS) {
			return data;
		}
		session.lastExhausted = player.tickCount;
		return gain(player, data, NenStat.AURA_CAPACITY, EXHAUSTED_XP * restMultiplier(player));
	}

	/** Super pulo ou super dash. */
	static void gyoMove(ServerPlayer player) {
		award(player, NenStat.AGILITY, GYO_MOVE_XP * multiplier(player));
	}

	/** Mandou a aura para outro lado com o Ryu. */
	static void stanceChanged(ServerPlayer player) {
		Session session = session(player);
		if (player.tickCount - session.lastStanceXp >= RYU_STANCE_GAP_TICKS) {
			session.lastStanceXp = player.tickCount;
			award(player, NenStat.AGILITY, RYU_STANCE_XP * multiplier(player));
		}
	}

	// ---- XP ----

	private static void award(ServerPlayer player, NenStat stat, double xp) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (data.awakened()) {
			player.setAttached(ModAttachments.NEN, gain(player, data, stat, xp));
		}
	}

	private static NenData gain(ServerPlayer player, NenData data, NenStat stat, double xp) {
		NenData after = data.addAttributeXp(stat, xp);
		if (after.attributeLevel(stat) > data.attributeLevel(stat)) {
			player.sendOverlayMessage(stat.trainedMessage());
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.3F);
		}
		return after;
	}
}
