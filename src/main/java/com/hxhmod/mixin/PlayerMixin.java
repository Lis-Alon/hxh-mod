package com.hxhmod.mixin;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import com.hxhmod.nen.NenCombat;
import com.hxhmod.nen.ShuCoat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Velocidade de ataque e de quebrar blocos vindas dos status internos do Nen. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void hxh$nenAttackSpeed(CallbackInfoReturnable<Float> cir) {
		Player player = (Player) (Object) this;
		double bonus = NenBonuses.of(player.getAttached(ModAttachments.NEN)).attackSpeed() + ShuCoat.attackSpeedBonus(player);
		if (bonus > 0) {
			cir.setReturnValue((float) (cir.getReturnValue() / (1 + bonus)));
		}
	}

	// Passos Silenciosos: em Zetsu os passos nao fazem som
	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void hxh$silentSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (NenCombat.silentSteps((Player) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void hxh$nenMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		double bonus = ShuCoat.miningBonus((Player) (Object) this);
		if (bonus > 0) {
			cir.setReturnValue((float) (cir.getReturnValue() * (1 + bonus)));
		}
	}
}
