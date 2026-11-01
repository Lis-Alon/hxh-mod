package com.hxhmod.client;

import com.hxhmod.HxHMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class HxHModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(HxHMod.id("nen_aura"), NenHud::render);
	}
}
