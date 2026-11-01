package com.hxhmod.gametest;

import com.hxhmod.client.NenScreen;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.Technique;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;

/** Abre o painel do Nen em varios estados e tira prints de cada aba, para conferir o visual. */
public class NenPanelGameTest implements FabricClientGameTest {
	private static final int TEST_XP = 2640;
	private static final int RELOAD_TICKS = 100;

	@Override
	public void runTest(ClientGameTestContext context) {
		// Prints em portugues, que tem os textos mais compridos
		context.runOnClient(client -> {
			client.options.languageCode = "pt_br";
			client.getLanguageManager().setSelected("pt_br");
			client.reloadResourcePacks();
		});
		context.waitTicks(RELOAD_TICKS);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();

			context.setScreen(NenScreen::new);
			context.waitTicks(5);
			context.takeScreenshot("nen_1_dormant");

			// Desperta o Nen e adianta o treino para ter pontos e algumas tecnicas liberadas
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = NenData.DEFAULT.awaken(NenType.ENHANCEMENT)
						.addXp(TEST_XP)
						.unlock(Technique.ZETSU)
						.unlock(Technique.REN)
						.addTraining(Technique.TEN, 754)
						.addTraining(Technique.REN, 121)
						.withActive(Technique.TEN);
				player.setAttached(ModAttachments.NEN, data.withAura(80));
			});
			context.waitTicks(30);

			TestInput input = context.getInput();
			context.setScreen(NenScreen::new);
			context.waitTicks(5);
			input.pressKey(InputConstants.KEY_RIGHT);
			input.pressKey(InputConstants.KEY_RIGHT);
			input.pressKey(InputConstants.KEY_DOWN);
			input.pressKey(InputConstants.KEY_RIGHT);
			context.waitTicks(2);
			context.takeScreenshot("nen_2_level");

			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(2);
			context.takeScreenshot("nen_3_techniques_ten");

			for (int i = 0; i < 4; i++) {
				input.pressKey(InputConstants.KEY_DOWN);
			}
			context.waitTicks(2);
			context.takeScreenshot("nen_4_techniques_ren");

			input.pressKey(InputConstants.KEY_DOWN);
			context.waitTicks(2);
			context.takeScreenshot("nen_5_techniques_gyo");

			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(2);
			context.takeScreenshot("nen_6_status");

			context.setScreen(() -> null);
			context.waitTicks(5);
			context.takeScreenshot("nen_7_hud");
		}
	}
}
