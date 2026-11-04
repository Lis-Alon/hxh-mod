package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;

import java.util.EnumMap;
import java.util.Map;

public class HxHModClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HxHMod.id("controls"));

	// Tecla padrao N; pode ser trocada em Opcoes > Controles > Hunter x Hunter
	public static final KeyMapping OPEN_NEN_PANEL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.hxh.nen_panel", InputConstants.KEY_N, CATEGORY));

	// Uma tecla para ligar/desligar cada tecnica; todas podem ser trocadas em Controles
	private static final Map<Technique, KeyMapping> TECHNIQUE_KEYS = new EnumMap<>(Technique.class);

	static {
		technique(Technique.TEN, InputConstants.KEY_Z);
		technique(Technique.ZETSU, InputConstants.KEY_V);
		technique(Technique.REN, InputConstants.KEY_R);
		technique(Technique.KEN, InputConstants.KEY_K);
		technique(Technique.GYO, InputConstants.KEY_G);
		technique(Technique.IN, InputConstants.KEY_I);
		technique(Technique.KO, InputConstants.KEY_B);
		technique(Technique.RYU, InputConstants.KEY_Y);
		technique(Technique.SHU, InputConstants.KEY_H);
		technique(Technique.EN, InputConstants.KEY_J);
		technique(Technique.HATSU, InputConstants.KEY_U);
	}

	private static void technique(Technique technique, int defaultKey) {
		TECHNIQUE_KEYS.put(technique, KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.hxh.technique." + technique.getSerializedName(), defaultKey, CATEGORY)));
	}

	@Override
	public void onInitializeClient() {
		HudElementRegistry.addLast(HxHMod.id("nen_aura"), NenHud::render);

		// No inicio do tick: depois dele o pulo normal ja tirou o jogador do chao
		ClientTickEvents.START_CLIENT_TICK.register(GyoControl::tick);
		ClientTickEvents.START_CLIENT_TICK.register(client -> TechniqueControl.tick(client, TECHNIQUE_KEYS.get(Technique.KO)));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_NEN_PANEL.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					client.gui.setScreen(new NenScreen());
				}
			}
			TECHNIQUE_KEYS.forEach((technique, key) -> {
				while (key.consumeClick()) {
					// O Ko funciona segurando a tecla (TechniqueControl); agachado, a tecla regula a intensidade
					if (client.player == null || technique == Technique.KO || !ClientPlayNetworking.canSend(NenPayloads.ToggleTechnique.TYPE)) {
						continue;
					}
					if (client.options.keyShift.isDown() && technique.maxIntensity() > 1) {
						ClientPlayNetworking.send(new NenPayloads.CycleIntensity(technique));
					} else {
						ClientPlayNetworking.send(new NenPayloads.ToggleTechnique(technique));
					}
				}
			});
		});
	}
}
