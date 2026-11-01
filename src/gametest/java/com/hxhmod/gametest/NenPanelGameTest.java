package com.hxhmod.gametest;

import com.hxhmod.block.ModBlocks;
import com.hxhmod.client.NenScreen;
import com.hxhmod.item.ModItems;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenCombat;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenPose;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.Technique;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Abre o painel do Nen em varios estados e tira prints de cada aba, para conferir o visual. */
public class NenPanelGameTest implements FabricClientGameTest {
	private static final int TEST_XP = 2640;
	private static final int RELOAD_TICKS = 100;
	// Mesmas medidas do NenScreen, para achar a primeira linha de atributo com o mouse
	private static final int PANEL_WIDTH = 440;
	private static final int PANEL_HEIGHT = 256;
	private static final int FIRST_STAT_ROW_Y = 30 + 8 + 22 + 14;

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
			context.waitTicks(25);
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

			// Mouse: rodinha em cima da Vitalidade poe 3 pontos, botao direito tira 1
			double[] cursor = context.computeOnClient(client -> {
				int scale = client.getWindow().getGuiScale();
				int guiWidth = client.getWindow().getGuiScaledWidth();
				int guiHeight = client.getWindow().getGuiScaledHeight();
				int left = (guiWidth - Math.min(PANEL_WIDTH, guiWidth - 8)) / 2;
				int top = (guiHeight - Math.min(PANEL_HEIGHT, guiHeight - 8)) / 2;
				return new double[] {(left + 60) * scale, (top + FIRST_STAT_ROW_Y + 6) * scale};
			});
			input.setCursorPos(cursor[0], cursor[1]);
			context.waitTicks(2);
			for (int i = 0; i < 3; i++) {
				input.scroll(1);
			}
			input.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(2);
			context.takeScreenshot("nen_2_level");

			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(2);
			context.takeScreenshot("nen_3_techniques_ten");

			input.pressKey(InputConstants.KEY_DOWN);
			input.pressKey(InputConstants.KEY_DOWN);
			context.waitTicks(2);
			context.takeScreenshot("nen_3b_techniques_zetsu");
			// Rola os detalhes com a rodinha em cima deles
			double[] details = context.computeOnClient(client -> {
				int scale = client.getWindow().getGuiScale();
				return new double[] {client.getWindow().getGuiScaledWidth() * 0.7 * scale,
						client.getWindow().getGuiScaledHeight() * 0.6 * scale};
			});
			input.setCursorPos(details[0], details[1]);
			for (int i = 0; i < 6; i++) {
				input.scroll(-1);
			}
			context.waitTicks(2);
			context.takeScreenshot("nen_3c_techniques_zetsu_scrolled");
			input.pressKey(InputConstants.KEY_DOWN);
			input.pressKey(InputConstants.KEY_DOWN);
			context.waitTicks(2);
			context.takeScreenshot("nen_4_techniques_ren");

			input.pressKey(InputConstants.KEY_DOWN);
			context.waitTicks(2);
			context.takeScreenshot("nen_5_techniques_gyo");

			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(10);
			context.takeScreenshot("nen_6_status");

			context.setScreen(() -> null);
			context.waitTicks(5);
			context.takeScreenshot("nen_7_hud");

			// Pose de meditacao vista de frente, com o copo colocado no chao
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos glass = player.blockPosition().relative(player.getDirection().getClockWise(), 1);
				player.level().setBlockAndUpdate(glass, ModBlocks.WATER_GLASS.defaultBlockState());
				NenPose.setPanelOpen(player, true);
			});
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			});
			context.waitTicks(20);
			context.takeScreenshot("nen_8_meditation_pose");

			// O copo de perto, em primeira pessoa olhando para baixo
			singleplayer.getServer().runOnServer(server ->
					NenPose.setPanelOpen(server.getPlayerList().getPlayers().getFirst(), false));
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				client.player.setXRot(60.0F);
			});
			context.waitTicks(20);
			context.takeScreenshot("nen_9_water_glass");

			// Ten reduz o dano; Zetsu deixa o jogador mais discreto e sem defesa contra golpes com aura
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.TEN));
				float reduced = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(Math.abs(reduced - 10.0F * (1 - data.tenDefense())) < 0.001F && reduced < 10.0F,
						"o Ten deveria reduzir o dano, veio " + reduced);
				player.setAttached(ModAttachments.NEN, data.unlock(Technique.ZETSU).withActive(Technique.ZETSU));
				check(NenCombat.visibility(player) < 1.0, "em Zetsu o jogador deveria ficar mais discreto");
				check(NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F) == 10.0F,
						"em Zetsu golpes sem aura causam o dano normal");
				player.setAttached(ModAttachments.NEN, data);
			});

			// Comandos de liberar e bloquear
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CommandSourceStack source = server.createCommandSourceStack().withEntity(player);
				int pointsBefore = player.getAttachedOrCreate(ModAttachments.NEN).pointsAvailable();
				server.getCommands().performPrefixedCommand(source, "nen unlock ryu");
				NenData unlocked = player.getAttachedOrCreate(ModAttachments.NEN);
				check(unlocked.isUnlocked(Technique.RYU) && unlocked.isUnlocked(Technique.KO) && unlocked.isUnlocked(Technique.GYO),
						"/nen unlock ryu deveria liberar Ryu e o que vem antes");
				check(unlocked.pointsAvailable() == pointsBefore, "liberar por comando nao deveria gastar pontos");
				server.getCommands().performPrefixedCommand(source, "nen lock ren");
				check(!player.getAttachedOrCreate(ModAttachments.NEN).isUnlocked(Technique.REN), "/nen lock ren deveria bloquear");
				server.getCommands().performPrefixedCommand(source, "nen lock all");
				NenData locked = player.getAttachedOrCreate(ModAttachments.NEN);
				check(Technique.TABS.stream().noneMatch(locked::isUnlocked) && locked.upgrades().isEmpty(),
						"/nen lock all deveria bloquear tudo");
				server.getCommands().performPrefixedCommand(source, "nen unlock all");
				check(Technique.TABS.stream().allMatch(player.getAttachedOrCreate(ModAttachments.NEN)::isUnlocked),
						"/nen unlock all deveria liberar tudo");
				server.getCommands().performPrefixedCommand(source, "nen level 30");
				check(player.getAttachedOrCreate(ModAttachments.NEN).level() == 30, "/nen level 30 deveria ir para o nivel 30");
			});

			// Aba do mod no criativo
			singleplayer.getServer().runOnServer(server ->
					server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
			context.waitTicks(10);
			context.runOnClient(client -> {
				CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(client.player,
						client.level.enabledFeatures(), false);
				client.gui.setScreen(screen);
				((FabricCreativeModeInventoryScreen) screen).setSelectedTab(ModItems.TAB);
			});
			context.waitTicks(10);
			context.takeScreenshot("nen_10_creative_tab");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
