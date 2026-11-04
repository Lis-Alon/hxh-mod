package com.hxhmod.client.mixin;

import com.hxhmod.client.EnSense;
import com.hxhmod.client.GyoControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Contorno de quem o En ou o Gyo detecta, visto so por quem usa, e botao direito com o Gyo preparado. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public LocalPlayer player;

	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void hxh$enOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (EnSense.senses(entity) || GyoControl.sees(entity)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void hxh$gyoGuard(CallbackInfo ci) {
		if (GyoControl.onUseItem(player)) {
			ci.cancel();
		}
	}
}
