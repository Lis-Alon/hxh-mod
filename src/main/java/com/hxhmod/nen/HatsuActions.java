package com.hxhmod.nen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Lanca o Hatsu equipado na tecla e cuida do que ele deixa no mundo: projeteis, mobs sob comando e marcas.
 * Dano, area e duracao ja chegam aqui multiplicados pela afinidade ({@link HatsuAbility}).
 */
public final class HatsuActions {
	private static final float SPELL_PITCH = 1.2F;
	private static final double SHOT_HIT_MARGIN = 0.3;
	private static final double CONTROL_SEARCH_RADIUS = 16;
	private static final double COMMAND_IMMUNE_HEALTH = 100;

	/** Projetil em voo: so a posicao muda a cada tick. */
	private static final class Shot {
		private final Vec3 origin;
		private final Vec3 direction;
		private final double damage;
		private final double speed;
		private final double range;
		private Vec3 position;

		private Shot(Vec3 origin, Vec3 direction, double damage, double speed, double range) {
			this.origin = origin;
			this.direction = direction;
			this.damage = damage;
			this.speed = speed;
			this.range = range;
			this.position = origin;
		}
	}

	private record Mark(UUID owner, long until) {
	}

	private static final Map<ServerPlayer, List<Shot>> SHOTS = new WeakHashMap<>();
	// Ate que tick do mundo cada jogador segura a propriedade da aura
	private static final Map<ServerPlayer, Long> PROPERTY_UNTIL = new WeakHashMap<>();
	private static final Map<ServerPlayer, Map<Mob, Long>> CONTROLLED = new WeakHashMap<>();
	private static final Map<LivingEntity, Mark> MARKS = new WeakHashMap<>();

	private HatsuActions() {
	}

	/** Passa a tecla para este galho, se o jogador tiver o Hatsu dele e o galho nao estiver bloqueado. */
	public static void equip(ServerPlayer player, NenType branch) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		Optional<HatsuAbility> ability = HatsuAbility.of(branch);
		if (ability.isEmpty() || !data.has(ability.get().upgrade()) || data.hatsuPower(branch) <= 0) {
			return;
		}
		player.setAttached(ModAttachments.HATSU_STATE, player.getAttachedOrCreate(ModAttachments.HATSU_STATE).equippedTo(branch));
		player.sendOverlayMessage(Component.translatable("nen.hxh.hatsu.equipped", ability.get().upgrade().displayName()));
	}

	public static int remaining(Player player) {
		return player.getAttachedOrCreate(ModAttachments.HATSU_STATE).remaining(player.level().getGameTime());
	}

	public static void use(ServerPlayer player) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		HatsuState state = player.getAttachedOrCreate(ModAttachments.HATSU_STATE);
		Optional<HatsuAbility> equipped = HatsuAbility.of(state.equipped());
		if (equipped.isEmpty() || !data.has(equipped.get().upgrade()) || data.hatsuPower(state.equipped()) <= 0) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.hatsu.none"));
			return;
		}
		if (data.isActive(Technique.ZETSU)) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.hatsu.zetsu"));
			return;
		}
		if (remaining(player) > 0) {
			return;
		}
		int cost = HatsuAbility.cost(data);
		if (data.aura() < cost) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.aura_depleted"));
			return;
		}
		HatsuAbility ability = equipped.get();
		player.setAttached(ModAttachments.NEN, data.withAura(data.aura() - cost).addTraining(Technique.HATSU, NenRules.HATSU_CAST_TRAINING));
		player.setAttached(ModAttachments.HATSU_STATE, state.startedUntil(player.level().getGameTime() + HatsuAbility.cooldownTicks(data)));
		switch (ability) {
			case QUAKE -> quake(player, data, ability);
			case PROPERTY -> PROPERTY_UNTIL.put(player, player.level().getGameTime() + ability.durationTicks(data));
			case ARMOR -> armor(player, data, ability);
			case MARK -> mark(player, data, ability);
			case COMMAND -> command(player, data, ability);
			case BLAST -> blast(player, data, ability);
		}
		player.sendOverlayMessage(Component.translatable("nen.hxh.hatsu.cast", ability.upgrade().displayName()));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, SPELL_PITCH);
		AuraVisuals.burst(player, data, ability == HatsuAbility.PROPERTY ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANT, 25);
	}

	// ---------------------------------------------------------------- Os seis Hatsus

	/** Golpe no chao: dano e empurrao em volta do jogador. */
	private static void quake(ServerPlayer player, NenData data, HatsuAbility ability) {
		double radius = ability.radius(data);
		ServerLevel level = player.level();
		for (LivingEntity target : targets(player, player.getBoundingBox().inflate(radius), radius)) {
			hurt(player, target, ability.damage(data));
			target.push(target.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(NenRules.HATSU_QUAKE_KNOCKBACK).add(0, NenRules.HATSU_QUAKE_LIFT, 0));
			target.needsSync = true;
		}
		level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), (int) Math.ceil(radius * 2),
				radius / 2, 0.1, radius / 2, 0);
	}

	/** Absorcao exata: a vida extra e o tempo dela seguem a afinidade. */
	private static void armor(ServerPlayer player, NenData data, HatsuAbility ability) {
		double extra = ability.damage(data);
		int amplifier = Math.max(0, (int) Math.ceil(extra / 4) - 1);
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ability.durationTicks(data), amplifier, false, true, true));
		player.setAbsorptionAmount((float) extra);
	}

	/** Todos em volta passam a brilhar e levam mais dano do jogador que marcou. */
	private static void mark(ServerPlayer player, NenData data, HatsuAbility ability) {
		double radius = ability.radius(data);
		int ticks = ability.durationTicks(data);
		long until = player.level().getGameTime() + ticks;
		for (LivingEntity target : targets(player, player.getBoundingBox().inflate(radius), radius)) {
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
			MARKS.put(target, new Mark(player.getUUID(), until));
		}
	}

	/** Os mobs em volta largam o jogador e atacam os inimigos por perto; jogadores so ficam lentos. */
	private static void command(ServerPlayer player, NenData data, HatsuAbility ability) {
		double radius = ability.radius(data);
		int ticks = ability.durationTicks(data);
		long until = player.level().getGameTime() + ticks;
		Map<Mob, Long> controlled = CONTROLLED.computeIfAbsent(player, ignored -> new WeakHashMap<>());
		for (LivingEntity target : targets(player, player.getBoundingBox().inflate(radius), radius)) {
			if (target instanceof Mob mob && mob.getMaxHealth() <= COMMAND_IMMUNE_HEALTH) {
				controlled.put(mob, until);
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
			} else if (target instanceof Player) {
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 1));
			}
		}
	}

	private static void blast(ServerPlayer player, NenData data, HatsuAbility ability) {
		Vec3 origin = player.getEyePosition();
		SHOTS.computeIfAbsent(player, ignored -> new ArrayList<>()).add(new Shot(origin, player.getLookAngle(), ability.damage(data),
				HatsuAbility.blastSpeed(data), HatsuAbility.blastRange(data)));
	}

	// ---------------------------------------------------------------- A cada tick

	static void tick(ServerPlayer player, NenData data) {
		List<Shot> shots = SHOTS.get(player);
		if (shots != null) {
			shots.removeIf(shot -> stepShot(player, data, shot));
		}
		Map<Mob, Long> controlled = CONTROLLED.get(player);
		if (controlled != null) {
			stepControlled(player, controlled);
		}
	}

	/** Anda um tick com o projetil; true quando ele acabou. */
	private static boolean stepShot(ServerPlayer player, NenData data, Shot shot) {
		ServerLevel level = player.level();
		Vec3 from = shot.position;
		Vec3 to = from.add(shot.direction.scale(shot.speed));
		HitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();

		LivingEntity hit = null;
		double closest = Double.MAX_VALUE;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1), target -> valid(player, target))) {
			Optional<Vec3> touch = target.getBoundingBox().inflate(SHOT_HIT_MARGIN).clip(from, end);
			if (touch.isPresent() && touch.get().distanceToSqr(from) < closest) {
				closest = touch.get().distanceToSqr(from);
				hit = target;
			}
		}
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 3, 0.08, 0.08, 0.08, 0);
		if (hit != null) {
			hurt(player, hit, shot.damage * HatsuAbility.blastFalloff(data, hit.position().distanceTo(shot.origin)));
			level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 12, 0.2, 0.2, 0.2, 0.1);
			return true;
		}
		shot.position = end;
		return wall.getType() != HitResult.Type.MISS || end.distanceTo(shot.origin) >= shot.range;
	}

	private static void stepControlled(ServerPlayer player, Map<Mob, Long> controlled) {
		long now = player.level().getGameTime();
		Iterator<Map.Entry<Mob, Long>> entries = controlled.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<Mob, Long> entry = entries.next();
			Mob mob = entry.getKey();
			if (!mob.isAlive() || now >= entry.getValue()) {
				mob.setTarget(null);
				entries.remove();
				continue;
			}
			mob.setTarget(nearestEnemy(mob));
		}
	}

	private static LivingEntity nearestEnemy(Mob mob) {
		return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(CONTROL_SEARCH_RADIUS),
						other -> other != mob && other.isAlive() && other instanceof Enemy)
				.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
	}

	// ---------------------------------------------------------------- Combate

	/** Dano a mais do golpe corpo a corpo com a propriedade ligada; quem apanha tambem fica lento. */
	static double propertyHit(ServerPlayer attacker, NenData data, LivingEntity victim) {
		if (!propertyActive(attacker)) {
			return 0;
		}
		victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, NenRules.HATSU_PROPERTY_SLOW_TICKS, 1));
		attacker.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
				10, 0.3, 0.4, 0.3, 0.1);
		return HatsuAbility.propertyBonus(data);
	}

	public static boolean propertyActive(ServerPlayer player) {
		return player.level().getGameTime() < PROPERTY_UNTIL.getOrDefault(player, 0L);
	}

	/** Multiplicador do dano recebido: a Adaptacao Corporal so vale com a propriedade ligada. */
	static double damageTaken(ServerPlayer player, NenData data) {
		return propertyActive(player) ? 1 - HatsuAbility.adaptation(data) : 1;
	}

	/** Dano a mais em quem este jogador marcou. */
	static double markBonus(ServerPlayer attacker, NenData data, LivingEntity victim) {
		Mark mark = MARKS.get(victim);
		if (mark == null || !mark.owner().equals(attacker.getUUID())) {
			return 0;
		}
		if (attacker.level().getGameTime() >= mark.until()) {
			MARKS.remove(victim);
			return 0;
		}
		return HatsuAbility.markBonus(data);
	}

	public static boolean controlling(ServerPlayer player, Mob mob) {
		Map<Mob, Long> controlled = CONTROLLED.get(player);
		return controlled != null && controlled.containsKey(mob);
	}

	public static boolean marked(ServerPlayer player, LivingEntity target) {
		Mark mark = MARKS.get(target);
		return mark != null && mark.owner().equals(player.getUUID());
	}

	// ---------------------------------------------------------------- Alvos

	private static List<LivingEntity> targets(ServerPlayer player, AABB box, double radius) {
		return player.level().getEntitiesOfClass(LivingEntity.class, box,
				target -> valid(player, target) && target.distanceTo(player) <= radius);
	}

	/** Nem o proprio jogador, nem aliados e bichos dele, nem quem nao pode ser atingido. */
	private static boolean valid(ServerPlayer player, LivingEntity target) {
		if (target == player || !target.isAlive() || target.isAlliedTo(player) || target instanceof ArmorStand) {
			return false;
		}
		if (target instanceof OwnableEntity owned && owned.getOwner() == player) {
			return false;
		}
		return !(target instanceof Player other) || (!other.isSpectator() && !other.isCreative() && player.canHarmPlayer(other));
	}

	private static void hurt(ServerPlayer player, LivingEntity target, double damage) {
		target.hurtServer(player.level(), player.damageSources().indirectMagic(player, player), (float) damage);
	}
}
