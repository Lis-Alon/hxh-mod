package com.hxhmod.mixin;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import com.hxhmod.nen.NenCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Liga os status internos do Nen ao dano, a vida maxima, a velocidade e a percepcao dos mobs. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float hxh$nenDamage(float amount, ServerLevel level, DamageSource source) {
		return NenCombat.incomingDamage((LivingEntity) (Object) this, source, amount);
	}

	// Vida maxima e velocidade de movimento vem dos status internos do Nen (sem atributos do Minecraft)
	@Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
	private void hxh$nenMaxHealth(CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof Player player) {
			double bonus = NenBonuses.of(player.getAttached(ModAttachments.NEN)).maxHealth();
			if (bonus != 0) {
				cir.setReturnValue((float) (cir.getReturnValue() + bonus));
			}
		}
	}

	@Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
	private void hxh$nenSpeed(CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof Player player) {
			double bonus = NenBonuses.of(player.getAttached(ModAttachments.NEN)).moveSpeed();
			if (bonus != 0) {
				cir.setReturnValue((float) (cir.getReturnValue() * (1 + bonus)));
			}
		}
	}

	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void hxh$zetsuVisibility(ServerLevel level, Entity lookingEntity, CallbackInfoReturnable<Double> cir) {
		double multiplier = NenCombat.visibility((LivingEntity) (Object) this);
		if (multiplier != 1.0) {
			cir.setReturnValue(cir.getReturnValue() * multiplier);
		}
	}
}
