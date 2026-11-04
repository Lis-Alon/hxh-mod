package com.hxhmod.mixin;

import com.hxhmod.nen.ShuCoat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O Shu poupa o desgaste do objeto revestido. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "processDurabilityChange", at = @At("RETURN"), cancellable = true)
	private void hxh$shuWear(int amount, ServerLevel level, ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
		if (player != null) {
			cir.setReturnValue(ShuCoat.reduceWear(player, (ItemStack) (Object) this, cir.getReturnValue()));
		}
	}
}
