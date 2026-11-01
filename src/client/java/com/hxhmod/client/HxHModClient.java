package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;

public class HxHModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HxHMod.id("controls"));

	// Tecla padrao N; pode ser trocada em Opcoes > Controles > Hunter x Hunter
	public static final KeyMapping OPEN_NEN_PANEL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.hxh.nen_panel", InputConstants.KEY_N, CATEGORY));

	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(HxHMod.id("nen_aura"), NenHud::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_NEN_PANEL.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					client.gui.setScreen(new NenScreen());
				}
			}
		});
	}
}
