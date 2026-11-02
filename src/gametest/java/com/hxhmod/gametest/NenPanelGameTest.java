package com.hxhmod.gametest;

import com.hxhmod.block.ModBlocks;
import com.hxhmod.client.EnSense;
import com.hxhmod.client.NenScreen;
import com.hxhmod.item.ModItems;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenCombat;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenStat;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.Set;

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

			// Desperta o Nen e adianta o treino: Ten com 20% de proficiencia ja ensina o Gyo
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = NenData.DEFAULT.awaken(NenType.ENHANCEMENT)
						.addXp(TEST_XP)
						.addTraining(Technique.TEN, 2420)
						.addTraining(Technique.REN, 1500)
						.addTraining(Technique.ZETSU, 9000)
						.withActive(Technique.TEN);
				check(Technique.BASE.stream().allMatch(data::isUnlocked) && !data.isUnlocked(Technique.GYO),
						"ao despertar deveria vir so Ten, Zetsu, Ren e Hatsu");
				player.setAttached(ModAttachments.NEN, data.withAura(80));
			});
			context.waitTicks(30);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(data.mastery(Technique.TEN) >= 20 && data.isUnlocked(Technique.GYO),
						"Ten com 20% deveria ensinar o Gyo, Ten ficou em " + data.mastery(Technique.TEN));
				check(!data.isUnlocked(Technique.SHU) && !data.isUnlocked(Technique.IN) && !data.isUnlocked(Technique.KEN),
						"Shu, In e Ken ainda nao deveriam ter sido aprendidos");
				check(data.pointsSpent() == 0, "aprender tecnicas nao deveria gastar pontos");
			});

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

			// Arvore de tecnicas, com o mouse em cima do Ken para mostrar os requisitos no rodape
			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(2);
			context.takeScreenshot("nen_5b_tree");
			double[] ken = context.computeOnClient(client -> {
				int scale = client.getWindow().getGuiScale();
				int guiWidth = client.getWindow().getGuiScaledWidth();
				int guiHeight = client.getWindow().getGuiScaledHeight();
				int panelWidth = Math.min(PANEL_WIDTH, guiWidth - 8);
				int panelHeight = Math.min(PANEL_HEIGHT, guiHeight - 8);
				int left = (guiWidth - panelWidth) / 2;
				int top = (guiHeight - panelHeight) / 2;
				int areaTop = top + 30 + 10;
				int areaHeight = top + panelHeight - 24 - areaTop;
				float treeScale = Math.min(1.0F, Math.min((panelWidth - 20) / 416.0F, areaHeight / 174.0F));
				float originX = left + (panelWidth - 416 * treeScale) / 2;
				float originY = areaTop + Math.max(0, (areaHeight - 174 * treeScale) / 2);
				return new double[] {(originX + 208 * treeScale) * scale, (originY + 87 * treeScale) * scale};
			});
			input.setCursorPos(ken[0], ken[1]);
			context.waitTicks(2);
			context.takeScreenshot("nen_5c_tree_ken");

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
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.ZETSU));
				check(NenCombat.visibility(player) < 1.0, "em Zetsu o jogador deveria ficar mais discreto");
				check(NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F) == 10.0F,
						"em Zetsu golpes sem aura causam o dano normal");
				player.setAttached(ModAttachments.NEN, data);
			});

			// Varias tecnicas juntas; o Zetsu so funciona sozinho
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.NONE);
				NenData tenRen = data.toggled(Technique.TEN).toggled(Technique.REN);
				check(tenRen.isActive(Technique.TEN) && tenRen.isActive(Technique.REN), "Ten e Ren deveriam ficar ligados juntos");
				NenData zetsu = tenRen.toggled(Technique.ZETSU);
				check(zetsu.active().equals(Set.of(Technique.ZETSU)), "o Zetsu deveria desligar as outras");
				NenData renAgain = zetsu.toggled(Technique.REN);
				check(renAgain.active().equals(Set.of(Technique.REN)), "ligar o Ren deveria encerrar o Zetsu");

				// Status internos: nada de modificador nos atributos do Minecraft
				NenData strong = data.addStats(Map.of(NenStat.VITALITY, 5, NenStat.PROTECTION, 20));
				player.setAttached(ModAttachments.NEN, strong);
				check(player.getMaxHealth() == 30.0F, "Vitalidade 5 deveria dar 30 de vida maxima, veio " + player.getMaxHealth());
				check(player.getAttributeValue(Attributes.ARMOR) == 0.0, "Protecao nao deveria mexer na armadura do Minecraft");
				check(player.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "Vitalidade nao deveria mexer no atributo de vida");
				float protectedDamage = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(Math.abs(protectedDamage - 8.5F) < 0.001F, "Protecao 20 deveria tirar 15% do dano, veio " + protectedDamage);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.TEN).withAura(80));
			});
			context.waitTicks(5);

			// Teclas de atalho: Z liga/desliga o Ten, R o Ren
			input.pressKey(InputConstants.KEY_Z);
			input.pressKey(InputConstants.KEY_R);
			context.waitTicks(5);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(!data.isActive(Technique.TEN) && data.isActive(Technique.REN),
						"Z deveria desligar o Ten e R ligar o Ren, ficou " + data.active());
			});
			input.pressKey(InputConstants.KEY_Z);
			// Vitalidade 10 = +10 coracoes de vida, que a barra precisa mostrar
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.addStats(Map.of(NenStat.VITALITY, 10)));
				player.setHealth(player.getMaxHealth());
			});
			context.waitTicks(25);
			context.takeScreenshot("nen_7b_hud_ten_ren");

			// Comandos de liberar e bloquear
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CommandSourceStack source = server.createCommandSourceStack().withEntity(player);
				int pointsBefore = player.getAttachedOrCreate(ModAttachments.NEN).pointsAvailable();
				server.getCommands().performPrefixedCommand(source, "nen unlock ryu");
				NenData unlocked = player.getAttachedOrCreate(ModAttachments.NEN);
				check(unlocked.isUnlocked(Technique.RYU) && unlocked.isUnlocked(Technique.KEN) && unlocked.isUnlocked(Technique.GYO),
						"/nen unlock ryu deveria liberar Ryu e as tecnicas base dele");
				check(unlocked.pointsAvailable() == pointsBefore, "liberar por comando nao deveria gastar pontos");
				server.getCommands().performPrefixedCommand(source, "nen lock ren");
				check(!player.getAttachedOrCreate(ModAttachments.NEN).isUnlocked(Technique.REN), "/nen lock ren deveria bloquear");
			});
			// Bloqueada por comando nao volta sozinha, mesmo sendo um principio
			context.waitTicks(30);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CommandSourceStack source = server.createCommandSourceStack().withEntity(player);
				check(!player.getAttachedOrCreate(ModAttachments.NEN).isUnlocked(Technique.REN), "o Ren bloqueado nao deveria voltar sozinho");
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

			// En: area de 5x5 com 1% de proficiencia ate 20x20 com 100%; um porco invisivel ao lado aparece com contorno
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				NenData fresh = data.lock(Technique.EN).grant(Technique.EN);
				check(Math.abs(NenData.DEFAULT.enSize() - 5.0) < 0.001, "En com 1% deveria cobrir 5x5");
				check(Math.abs(NenData.DEFAULT.addTraining(Technique.EN, 60000).enSize() - 20.0) < 0.001,
						"En com 100% deveria cobrir 20x20");
				player.setAttached(ModAttachments.NEN, NenData.DEFAULT.awaken(NenType.ENHANCEMENT).grant(Technique.EN)
						.addTraining(Technique.EN, 6).withActive(Technique.EN).withAura(100));
				Pig pig = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				Vec3 front = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
				pig.setPos(front.x, player.getY(), front.z);
				pig.setNoAi(true);
				pig.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 2000, 0, false, false));
				player.level().addFreshEntity(pig);
				Pig far = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				Vec3 farPos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(8));
				far.setPos(farPos.x, player.getY(), farPos.z);
				far.setNoAi(true);
				player.level().addFreshEntity(far);
				check(fresh.isUnlocked(Technique.EN), "liberar o En por comando deveria tirar o bloqueio");
			});
			context.runOnClient(client -> client.player.setXRot(15.0F));
			context.waitTicks(20);
			boolean[] sensed = context.computeOnClient(client -> {
				boolean near = false;
				boolean far = false;
				for (Entity entity : client.level.entitiesForRendering()) {
					if (entity instanceof Pig pig) {
						boolean close = pig.distanceTo(client.player) < 4;
						if (close) {
							near = EnSense.senses(pig);
						} else {
							far = EnSense.senses(pig);
						}
					}
				}
				return new boolean[] {near, far};
			});
			check(sensed[0] && !sensed[1], "o En de 5x5 deveria sentir o porco a 2 blocos e nao o de 8");
			context.takeScreenshot("nen_11_en_invisible_pig");

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
