package com.hxhmod.mixin;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Velocidade de ataque e de quebrar blocos vindas dos status internos do Nen. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void hxh$nenAttackSpeed(CallbackInfoReturnable<Float> cir) {
		double bonus = NenBonuses.of(((Player) (Object) this).getAttached(ModAttachments.NEN)).attackSpeed();
		if (bonus > 0) {
			cir.setReturnValue((float) (cir.getReturnValue() / (1 + bonus)));
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void hxh$nenMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		double bonus = NenBonuses.of(((Player) (Object) this).getAttached(ModAttachments.NEN)).miningSpeed();
		if (bonus > 0) {
			cir.setReturnValue((float) (cir.getReturnValue() * (1 + bonus)));
		}
	}
}
