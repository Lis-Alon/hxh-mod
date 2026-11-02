package com.hxhmod.client.mixin;

import com.hxhmod.client.EnSense;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Contorno nas criaturas dentro do En, visto so por quem usa (mesmo se estiverem invisiveis). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void hxh$enOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (EnSense.senses(entity)) {
			cir.setReturnValue(true);
		}
	}
}
