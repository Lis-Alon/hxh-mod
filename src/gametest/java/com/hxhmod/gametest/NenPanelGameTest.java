package com.hxhmod.gametest;

import com.hxhmod.block.ModBlocks;
import com.hxhmod.client.EnSense;
import com.hxhmod.client.GyoControl;
import com.hxhmod.client.NenScreen;
import com.hxhmod.client.NenTreePage;
import com.hxhmod.item.ModItems;
import com.hxhmod.nen.AuraVisuals;
import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.KoStrike;
import com.hxhmod.nen.ShuCoat;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import com.hxhmod.nen.NenCombat;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenStat;
import com.hxhmod.nen.NenTraining;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.NenActions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.zombie.Zombie;
import com.hxhmod.nen.NenPose;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.NenUpgrade;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Abre o painel do Nen em varios estados e tira prints de cada aba, para conferir o visual. */
public class NenPanelGameTest implements FabricClientGameTest {
	private static final int TEST_XP = 2640;
	private static final int RELOAD_TICKS = 100;
	// Mesmas medidas do NenScreen, para achar a primeira linha de atributo com o mouse
	private final Zombie[] hiddenTarget = new Zombie[1];

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
			// Atributos vindos do treino: a pagina so mostra, nao tem mais onde por pontos
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.withAttributeLevel(NenStat.VITALITY, 40).addAttributeXp(NenStat.VITALITY, 333)
						.withAttributeLevel(NenStat.AURA_FLOW, 12).addAttributeXp(NenStat.AURA_FLOW, 40)
						.withAttributeLevel(NenStat.ATTACK_POWER, 100).withAttributeLevel(NenStat.PERCEPTION, 7));
			});
			context.waitTicks(3);
			input.pressKey(InputConstants.KEY_DOWN);

			// Mouse: a rodinha troca o atributo selecionado
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
			input.scroll(-1);
			context.waitTicks(2);
			// Na menor janela so cabem 3 linhas embaixo da lista
			context.runOnClient(client -> {
				for (NenStat stat : NenStat.values()) {
					check(client.font.split(stat.description(), 226).size() <= 3, "a descricao de " + stat.getSerializedName() + " nao cabe em 3 linhas");
				}
			});
			context.takeScreenshot("nen_2_level");
			// O ceu da pagina de Nivel se mexe sozinho
			context.waitTicks(100);
			context.takeScreenshot("nen_2b_level_sky");
			input.resizeWindow(1366, 768);
			context.waitTicks(5);
			context.takeScreenshot("nen_2c_level_768");
			input.resizeWindow(1920, 1080);
			context.waitTicks(5);
			context.takeScreenshot("nen_2d_level_1080");
			input.resizeWindow(854, 480);
			context.waitTicks(5);

			// Tecnicas nao tem aba: abre pelo botao da arvore
			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(5);
			pointTree(context, input, 480, 20);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(2);
			context.takeScreenshot("nen_3_techniques_ten");
			input.resizeWindow(1366, 768);
			context.waitTicks(5);
			context.takeScreenshot("nen_3a_techniques_768");
			input.resizeWindow(1920, 1080);
			context.waitTicks(5);
			context.takeScreenshot("nen_3a2_techniques_1080");
			input.resizeWindow(854, 480);
			context.waitTicks(5);

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

			// Arvore de tecnicas: janela pequena e Full HD, mouse em cima do Ken e compra de uma sub-habilidade pelo clique
			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(5);
			context.takeScreenshot("nen_5b_tree_small");
			input.resizeWindow(1366, 768);
			context.waitTicks(5);
			context.takeScreenshot("nen_5b2_tree_768");
			input.resizeWindow(1920, 1080);
			context.waitTicks(5);
			hoverTree(context, input, Technique.KEN, null);
			context.takeScreenshot("nen_5c_tree_ken");
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).withActive(Set.of(Technique.TEN, Technique.REN)));
			});
			hoverTree(context, input, null, NenUpgrade.TEN_FLOW);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> check(server.getPlayerList().getPlayers().getFirst()
					.getAttachedOrCreate(ModAttachments.NEN).has(NenUpgrade.TEN_FLOW), "clicar na Fluidez da Aura deveria comprar"));
			// Mostrar nomes e Resetar (o primeiro clique so pede confirmacao)
			pointTree(context, input, 50, 22);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(2);
			context.takeScreenshot("nen_5d_tree_bought");
			pointTree(context, input, 36, 287);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(5);
			singleplayer.getServer().runOnServer(server -> check(server.getPlayerList().getPlayers().getFirst()
					.getAttachedOrCreate(ModAttachments.NEN).has(NenUpgrade.TEN_FLOW), "o primeiro clique no Resetar so deveria pedir confirmacao"));
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> check(!server.getPlayerList().getPlayers().getFirst()
					.getAttachedOrCreate(ModAttachments.NEN).has(NenUpgrade.TEN_FLOW), "o segundo clique no Resetar deveria devolver a compra"));
			input.resizeWindow(854, 480);
			context.waitTicks(5);

			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(10);
			context.takeScreenshot("nen_6_status");
			input.resizeWindow(1920, 1080);
			context.waitTicks(5);
			context.takeScreenshot("nen_6b_status_1080");
			input.resizeWindow(854, 480);
			context.waitTicks(5);

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
				NenData strong = data.withAttributeLevel(NenStat.VITALITY, 25).withAttributeLevel(NenStat.PROTECTION, 100);
				player.setAttached(ModAttachments.NEN, strong);
				check(player.getMaxHealth() == 40.0F, "Vitalidade 25 deveria dar 40 de vida maxima, veio " + player.getMaxHealth());
				check(player.getAttributeValue(Attributes.ARMOR) == 0.0, "Protecao nao deveria mexer na armadura do Minecraft");
				check(player.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "Vitalidade nao deveria mexer no atributo de vida");
				float unprotected = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(unprotected == 10.0F, "Protecao sem Ten nem Ken nao deveria reduzir o dano, veio " + unprotected);
				checkAttributes(data);
				checkTrainingEvents(player, data);
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
			// Vitalidade 25 = +10 coracoes de vida, que a barra precisa mostrar
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.withAttributeLevel(NenStat.VITALITY, 25));
				player.setHealth(player.getMaxHealth());
			});
			context.waitTicks(25);
			context.takeScreenshot("nen_7b_hud_ten_ren");

			// Treino correndo de verdade: a Vitalidade esta a meio XP de subir, e o Ten ligado completa
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.TEN).withAura(data.maxAura())
						.withAttributeLevel(NenStat.VITALITY, 30).addAttributeXp(NenStat.VITALITY, 304.5));
			});
			context.waitTicks(25);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(data.attributeLevel(NenStat.VITALITY) == 31, "o Ten ligado deveria subir a Vitalidade para 31, ficou " + data.attributeLevel(NenStat.VITALITY));
			});
			context.takeScreenshot("nen_7c_trained_message");

			// Esgotar a aura com o Ren treina a capacidade
			double[] capacityBefore = new double[1];
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				capacityBefore[0] = data.attributeXp().getOrDefault(NenStat.AURA_CAPACITY, 0.0);
				// Ren no maximo e sem Fluxo treinado: o gasto passa da recuperacao
				NenData ren = data.withAttributeLevel(NenStat.AURA_FLOW, 0).withActive(Technique.REN).withAura(0);
				while (ren.intensityOf(Technique.REN) < Technique.REN.maxIntensity()) {
					ren = ren.cycledIntensity(Technique.REN);
				}
				player.setAttached(ModAttachments.NEN, ren);
			});
			context.waitTicks(25);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(!data.isActive(Technique.REN), "sem aura o Ren deveria desligar");
				double gained = data.attributeXp().getOrDefault(NenStat.AURA_CAPACITY, 0.0) - capacityBefore[0];
				check(gained >= 60, "esgotar a aura com Ren deveria render XP de Capacidade, rendeu " + gained);
			});

			// Recuperar aura em Zetsu treina a capacidade; ja com a barra cheia nao rende
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				capacityBefore[0] = data.attributeXp().getOrDefault(NenStat.AURA_CAPACITY, 0.0);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.ZETSU).withAura(0));
			});
			context.waitTicks(25);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.attributeXp().getOrDefault(NenStat.AURA_CAPACITY, 0.0) > capacityBefore[0], "recuperar aura em Zetsu deveria render XP de Capacidade");
				// En aberto com uma criatura invisivel no raio: rende mais que o En sozinho
				capacityBefore[0] = data.attributeXp().getOrDefault(NenStat.PERCEPTION, 0.0);
				player.setAttached(ModAttachments.NEN, data.unlock(Technique.EN).withActive(Technique.EN).withAura(data.maxAura()));
				Zombie zombie = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.TRIGGERED);
				zombie.setPos(player.position());
				zombie.setNoAi(true);
				zombie.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200));
				player.level().addFreshEntity(zombie);
				hiddenTarget[0] = zombie;
			});
			context.waitTicks(25);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				double gained = data.attributeXp().getOrDefault(NenStat.PERCEPTION, 0.0) - capacityBefore[0];
				// O jogador do teste esta parado ha um tempo, entao vale o multiplicador de agora
				check(gained >= 2.4 * NenTraining.multiplier(player), "achar um alvo invisivel com o En deveria render mais XP de Percepcao, rendeu " + gained);
				hiddenTarget[0].discard();
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.TEN));
			});
			context.waitTicks(3);

			// Comandos de liberar e bloquear
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				CommandSourceStack source = server.createCommandSourceStack().withEntity(player);
				int pointsBefore = player.getAttachedOrCreate(ModAttachments.NEN).pointsAvailable();
				server.getCommands().performPrefixedCommand(source, "nen attribute aura_flow 50");
				check(player.getAttachedOrCreate(ModAttachments.NEN).attributeLevel(NenStat.AURA_FLOW) == 50, "/nen attribute deveria definir o nivel");
				server.getCommands().performPrefixedCommand(source, "nen attribute all 3");
				check(player.getAttachedOrCreate(ModAttachments.NEN).attributeLevel(NenStat.PERCEPTION) == 3, "/nen attribute all deveria definir todos");
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

			// Gyo ligado: cada acao vira a aplicacao, entra na propria recarga e o Gyo continua ligado
			double[] start = new double[3];
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.GYO).withAura(100));
				start[0] = player.getY();
			});
			context.waitTicks(5);
			input.holdKeyFor(InputConstants.KEY_SPACE, 2);
			context.waitTicks(6);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.isActive(Technique.GYO), "o super pulo nao deveria desligar o Gyo");
				check(GyoApplications.remaining(player, GyoApplications.Action.JUMP) > 0, "o super pulo deveria entrar em recarga");
				check(GyoApplications.remaining(player, GyoApplications.Action.DASH) == 0, "cada aplicacao deveria ter a propria recarga");
				check(player.getY() - start[0] > 1.5, "o super pulo deveria subir mais que um pulo normal, subiu " + (player.getY() - start[0]));
				start[1] = player.getHealth();
			});
			context.waitTicks(60);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getHealth() >= start[1], "a queda do super pulo nao deveria machucar");
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).withAura(100));
				start[0] = player.getY();
			});
			context.waitTicks(1);

			// Durante a recarga, pular e so pular
			input.holdKeyFor(InputConstants.KEY_SPACE, 2);
			context.waitTicks(6);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(player.getY() - start[0] < 1.5, "pular na recarga deveria ser um pulo normal, subiu " + (player.getY() - start[0]));
				check(data.aura() >= 100 - 2, "pular na recarga nao deveria gastar a aura da aplicacao, ficou " + data.aura());
			});
			context.waitTicks(30);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).withAura(100));
				start[0] = player.getX();
				start[2] = player.getZ();
			});
			context.waitTicks(5);
			input.holdKeyFor(InputConstants.KEY_LCONTROL, 2);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.isActive(Technique.GYO), "o super dash nao deveria desligar o Gyo");
				check(GyoApplications.remaining(player, GyoApplications.Action.DASH) > 0, "o super dash deveria entrar em recarga");
				double moved = Math.hypot(player.getX() - start[0], player.getZ() - start[2]);
				check(moved > 2, "o super dash deveria andar mais de 2 blocos, andou " + moved);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.GYO).withAura(100));
			});
			context.waitTicks(40);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.GYO), "esperar nao deveria desligar o Gyo");
			});
			input.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.isActive(Technique.GYO) && GyoApplications.guarding(player), "o botao direito deveria virar super defesa com o Gyo ligado");
				check(GyoApplications.remaining(player, GyoApplications.Action.GUARD) > 0, "a super defesa deveria entrar em recarga");
				float guarded = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(guarded < 10.0F * NenBonuses.of(data).damageTaken(), "a super defesa deveria reduzir o dano, veio " + guarded);

				// Super soco: so quando o golpe acerta, e soma com os outros bonus
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.GYO).withAura(100));
				NenData prepared = player.getAttachedOrCreate(ModAttachments.NEN);
				Pig target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				double normal = (10.0F + NenBonuses.of(prepared).flatDamage()) * (1 + NenBonuses.of(prepared).damageBonus());
				float punched = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				NenData after = player.getAttachedOrCreate(ModAttachments.NEN);
				check(punched > normal && after.isActive(Technique.GYO) && after.aura() == 100 - prepared.gyoCost(),
						"o golpe com Gyo deveria ser super soco, gastar a aura e manter o Gyo, veio " + punched);
				float again = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				NenData recharging = player.getAttachedOrCreate(ModAttachments.NEN);
				check(again < punched && recharging.aura() == after.aura(), "o soco na recarga deveria ser comum e nao gastar aura, veio " + again);
			});
			context.waitTicks(10);

			context.takeScreenshot("gyo_cooldowns");

			// Na recarga, o botao direito volta a usar itens
			context.runOnClient(client -> check(!GyoControl.onUseItem(client.player), "na recarga o botao direito deveria usar o item"));

			// A recarga vai de 20 s com 0% a 5 s com 100%
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.withMastery(Technique.GYO, 0).gyoCooldownTicks() == 400, "a recarga com 0% deveria ser 20 s");
				check(data.withMastery(Technique.GYO, 50).gyoCooldownTicks() == 250, "a recarga com 50% deveria ser 12,5 s");
				check(data.withMastery(Technique.GYO, 100).gyoCooldownTicks() == 100, "a recarga com 100% deveria ser 5 s");
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.NONE));
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

			// Visao do Gyo: so o oculto, so para quem usa, e sem efeitos do jogo
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).grant(Technique.GYO).withActive(Technique.GYO).withAura(100));
			});
			context.waitTicks(10);
			boolean[] seen = context.computeOnClient(client -> {
				boolean hidden = false;
				boolean visible = false;
				for (Entity entity : client.level.entitiesForRendering()) {
					if (entity instanceof Pig pig) {
						if (pig.isInvisible()) {
							hidden = GyoControl.sees(pig);
						} else {
							visible = GyoControl.sees(pig);
						}
					}
				}
				return new boolean[] {hidden, visible};
			});
			check(seen[0] && !seen[1], "o Gyo deveria ver o porco invisivel e nao destacar o visivel");
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(!player.hasEffect(MobEffects.NIGHT_VISION), "o Gyo nao deveria dar visao noturna");
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.EN));
			});

			// Ren e Ken por intensidade; Ten, Ren e Ken nao somam e o Ko suspende a defesa
			singleplayer.getServer().runOnServer(server -> {
				NenData base = NenData.DEFAULT.awaken(NenType.ENHANCEMENT);
				NenData ren1 = base.cycledIntensity(Technique.REN).cycledIntensity(Technique.REN);
				NenData ren3 = ren1.cycledIntensity(Technique.REN).cycledIntensity(Technique.REN);
				check(ren1.intensityOf(Technique.REN) == 1 && ren3.intensityOf(Technique.REN) == 3, "a intensidade deveria dar a volta");
				check(ren3.renDamage() > ren1.renDamage() && ren3.auraDrain(Technique.REN) > ren1.auraDrain(Technique.REN),
						"mais intensidade deveria dar mais dano e gastar mais");
				check(NenData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
						NenData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, ren3).getOrThrow()).getOrThrow().intensityOf(Technique.REN) == 3,
						"a intensidade deveria ficar salva");
				NenData guards = base.withActive(Set.of(Technique.TEN, Technique.REN, Technique.KEN));
				check(Math.abs(NenBonuses.of(guards).damageTaken() - (1 - guards.kenDefense())) < 0.001,
						"Ten, Ren e Ken deveriam valer a maior defesa, sem somar");
				check(NenBonuses.of(base.withActive(Set.of(Technique.TEN, Technique.KEN, Technique.KO))).damageTaken() > 1,
						"o Ko deveria suspender a defesa de Nen");
				check(base.grant(NenUpgrade.KEN_ENDURANCE).auraDrain(Technique.KEN) < base.auraDrain(Technique.KEN),
						"Fortificacao Duradoura deveria reduzir o gasto");

				// In esconde ate a capacidade; passou disso, vaza
				NenData hidden = base.withActive(Set.of(Technique.IN, Technique.GYO));
				check(hidden.inLeak() == 0, "o In deveria esconder pouca aura por inteiro");
				NenData intense = base.cycledIntensity(Technique.REN).withActive(Set.of(Technique.IN, Technique.REN, Technique.KEN, Technique.EN));
				check(intense.inLeak() > 0 && intense.inLeak() < 1, "liberar demais deveria vazar parte da aura");
				check(hidden.auraDrain(Technique.IN) > base.withActive(Technique.IN).auraDrain(Technique.IN),
						"esconder mais aura deveria gastar mais");
				check(NenBonuses.of(base.withActive(Technique.IN)).damageBonus() == 0, "o In nao deveria dar dano");

				// En escolhe uma area menor, que gasta menos
				NenData wide = base.addTraining(Technique.EN, 60000);
				NenData small = wide.cycledIntensity(Technique.EN);
				check(small.enSize() == 5.0 && wide.enSize() == 20.0, "o primeiro passo do En deveria ser 1/4 do tamanho");
				check(small.auraDrain(Technique.EN) < wide.auraDrain(Technique.EN), "area menor deveria gastar menos");

				// Ryu liga com o Ken e cai junto
				NenData ryu = base.toggled(Technique.RYU);
				check(ryu.isActive(Technique.KEN), "o Ryu deveria ligar o Ken");
				check(!ryu.toggled(Technique.KEN).isActive(Technique.RYU), "desligar o Ken deveria desligar o Ryu");

				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(ModAttachments.NEN, base.grant(NenUpgrade.ZETSU_STEP).withActive(Technique.ZETSU));
				check(NenCombat.silentSteps(player), "Passos Silenciosos deveria silenciar em Zetsu");
				check(AuraVisuals.shownTo(player, player, hidden, 10) == 10, "voce sempre ve a propria aura");

				// Ko pela tecla
				player.setAttached(ModAttachments.NEN, base.grant(Technique.KO).grant(Technique.GYO).grant(Technique.RYU)
						.grant(Technique.KEN).grant(Technique.SHU).withAura(100));
			});
			context.waitTicks(5);
			input.holdKey(InputConstants.KEY_B);
			context.waitTicks(30);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.isActive(Technique.KO), "segurar a tecla deveria carregar o Ko");
				double charge = KoStrike.charge(player, data);
				check(charge > 0.3 && charge < 1, "a carga deveria crescer com o tempo, veio " + charge);
				Pig target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				float hit = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				NenData after = player.getAttachedOrCreate(ModAttachments.NEN);
				check(hit > 20.0F && !after.isActive(Technique.KO) && after.aura() < data.aura(),
						"o golpe do Ko deveria causar mais de +100% e gastar a aura, veio " + hit);
			});
			input.releaseKey(InputConstants.KEY_B);
			context.waitTicks(5);
			input.holdKey(InputConstants.KEY_B);
			context.waitTicks(5);
			input.releaseKey(InputConstants.KEY_B);
			context.waitTicks(5);
			singleplayer.getServer().runOnServer(server -> {
				check(!server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.KO),
						"soltar a tecla deveria cancelar o Ko");
			});
			// Ko no ar: olhando para cima o clique nao acerta nada e a aura se perde
			float[] pitch = new float[1];
			context.runOnClient(client -> {
				pitch[0] = client.player.getXRot();
				client.player.setXRot(-80.0F);
			});
			int[] auraBefore = new int[1];
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withAura(100);
				player.setAttached(ModAttachments.NEN, data);
				auraBefore[0] = data.aura();
			});
			input.holdKey(InputConstants.KEY_B);
			context.waitTicks(10);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(3);
			input.releaseKey(InputConstants.KEY_B);
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(!data.isActive(Technique.KO) && data.aura() < auraBefore[0] - 5, "errar o Ko deveria gastar a aura, ficou " + data.aura());
			});
			context.runOnClient(client -> client.player.setXRot(pitch[0]));

			// Agachado, a tecla regula a intensidade em vez de ligar
			input.holdKey(InputConstants.KEY_LSHIFT);
			context.waitTicks(2);
			input.pressKey(InputConstants.KEY_R);
			context.waitTicks(3);
			input.releaseKey(InputConstants.KEY_LSHIFT);
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				NenData data = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN);
				check(data.intensityOf(Technique.REN) == 3 && !data.isActive(Technique.REN),
						"a tecla agachado deveria trocar a intensidade sem ligar o Ren");
			});

			// Ryu: a postura do jogador muda dano e defesa depois da transicao
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Set.of(Technique.RYU, Technique.KEN)).withAura(100)
						// No teto a Protecao nao sobe com os golpes do teste, que comparam o dano exato
						.withAttributeLevel(NenStat.PROTECTION, 100);
				player.setAttached(ModAttachments.NEN, data);
				start[0] = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
			});
			context.waitTicks(3);
			input.holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(15);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				float defended = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(defended < start[0], "segurar o botao direito no Ryu deveria reduzir o dano, veio " + defended + " contra " + start[0]);
				check(player.getAttachedOrCreate(ModAttachments.NEN).aura() < 100 || player.getAttachedOrCreate(ModAttachments.NEN).maxAura() > 100,
						"a redistribuicao deveria custar aura");
			});
			input.releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(15);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				float balanced = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(Math.abs(balanced - start[0]) < 0.01, "soltar deveria voltar ao equilibrio do Ken");

				// Shu: toda ferramenta melhora em tudo, conforme a proficiencia, e trocar desfaz
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.SHU).withAura(100).withMastery(Technique.SHU, 100);
				player.setAttached(ModAttachments.NEN, data);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
				player.setHealth(player.getMaxHealth());
				check(ShuCoat.kind(player.getMainHandItem()) == ShuCoat.Kind.TOOL, "picareta deveria ser ferramenta");
				check(ShuCoat.miningBonus(player) > 0, "picareta revestida deveria quebrar mais rapido");
				check(ShuCoat.attackSpeedBonus(player) > 0, "picareta revestida deveria atacar mais rapido");
				NenData novice = data.withMastery(Technique.SHU, 0);
				check(Math.abs(data.shuToolDamage() - 4 * novice.shuToolDamage()) < 1.0E-6
						&& Math.abs(data.shuMiningSpeed() - 4 * novice.shuMiningSpeed()) < 1.0E-6
						&& Math.abs(data.shuAttackSpeed() - 4 * novice.shuAttackSpeed()) < 1.0E-6
						&& data.shuWearSave() > novice.shuWearSave(),
						"a proficiencia deveria levar os beneficios do Shu de metade ao dobro");
				for (var tool : List.of(Items.DIAMOND_PICKAXE, Items.IRON_SHOVEL, Items.WOODEN_HOE, Items.IRON_AXE, Items.IRON_SWORD,
						Items.SHEARS, Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.MACE, Items.FISHING_ROD, Items.FLINT_AND_STEEL, Items.BRUSH)) {
					check(ShuCoat.kind(new ItemStack(tool)) == ShuCoat.Kind.TOOL, tool + " deveria receber todos os beneficios do Shu");
				}
				ItemStack worn = player.getMainHandItem();
				for (int i = 0; i < 200; i++) {
					worn.hurtAndBreak(1, player.level(), player, item -> {
					});
				}
				check(worn.getDamageValue() < 170 && worn.getDamageValue() > 0, "o Shu deveria poupar o desgaste, gastou " + worn.getDamageValue());
			});
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.SHU), "segurar o mesmo objeto deveria manter o Shu");
				check(ShuCoat.kind(new ItemStack(Items.SHIELD)) == ShuCoat.Kind.SHIELD && ShuCoat.kind(new ItemStack(Items.STICK)) == ShuCoat.Kind.COMMON,
						"os objetos deveriam cair no tipo certo");
				Pig target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				NenData coated = player.getAttachedOrCreate(ModAttachments.NEN);
				float withTool = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				player.setAttached(ModAttachments.NEN, coated.withActive(Technique.NONE));
				float bare = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				check(withTool > bare, "a picareta revestida deveria causar mais dano, veio " + withTool + " contra " + bare);

				// A flecha do arco revestido tambem
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				player.setAttached(ModAttachments.NEN, coated);
				Arrow arrow = EntityTypes.ARROW.create(player.level(), EntitySpawnReason.COMMAND);
				float shot = NenCombat.incomingDamage(target, player.damageSources().arrow(arrow, player), 10.0F);
				check(Math.abs(shot - 10.0F * (1 + coated.shuToolDamage())) < 0.01, "a flecha do arco revestido deveria ganhar o bonus, veio " + shot);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			});
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(!player.getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.SHU), "trocar de objeto deveria desfazer o Shu");
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.SHU);
				player.setAttached(ModAttachments.NEN, data);
			});
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				check(!server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.SHU),
						"o Shu nao deveria ficar ligado sem objeto");
			});

			// Passivas do Reforco: so para quem revelou o tipo; compra pela arvore
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				NenData hidden = NenData.DEFAULT.awaken(NenType.ENHANCEMENT).withXp(NenData.xpForLevel(30));
				check(hidden.upgradeProblem(NenUpgrade.HATSU_IMPACT) == NenData.UnlockProblem.TYPE, "o tipo escondido nao deveria comprar a passiva");
				NenData revealed = hidden.withDivination(100);
				check(revealed.upgradeProblem(NenUpgrade.HATSU_IMPACT) == null, "o Reforco revelado deveria poder comprar");
				check(revealed.withType(NenType.EMISSION).upgradeProblem(NenUpgrade.HATSU_IMPACT) == NenData.UnlockProblem.TYPE,
						"outro tipo nao deveria comprar a passiva do Reforco");
				NenData other = revealed.withType(NenType.EMISSION).grant(NenUpgrade.HATSU_IMPACT).grant(NenUpgrade.HATSU_VITALITY);
				check(other.hatsuImpact() == 0 && other.hatsuVitality() == 0, "liberada por comando em outro tipo, a passiva nao deveria valer");
				player.setAttached(ModAttachments.NEN, revealed);
				start[0] = player.getMaxHealth();
			});
			input.resizeWindow(1920, 1080);
			context.setScreen(NenScreen::new);
			context.waitTicks(5);
			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(5);
			hoverTree(context, input, null, NenUpgrade.HATSU_IMPACT);
			context.takeScreenshot("nen_12_tree_enhancement");
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(data.has(NenUpgrade.HATSU_IMPACT), "clicar em Impacto Natural deveria comprar");
				check(Math.abs(NenBonuses.of(data).damageBonus() - data.hatsuImpact()) < 0.001 && data.hatsuImpact() >= 0.10,
						"Impacto Natural deveria dar dano sem o Hatsu ligado");
				NenData vital = data.grant(NenUpgrade.HATSU_VITALITY);
				player.setAttached(ModAttachments.NEN, vital);
				check(Math.abs(player.getMaxHealth() - start[0] * (1 + vital.hatsuVitality())) < 0.01,
						"Vitalidade Reforcada deveria aumentar a vida maxima, veio " + player.getMaxHealth());
				player.setAttached(ModAttachments.NEN, data);
			});
			context.setScreen(() -> null);
			input.resizeWindow(854, 480);
			context.waitTicks(5);

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

	/** Poe o mouse num ponto da arvore, com a tela no tamanho atual. */
	private static void pointTree(ClientGameTestContext context, TestInput input, int x, int y) {
		double[] point = context.computeOnClient(client -> {
			int scale = client.getWindow().getGuiScale();
			float[] position = NenTreePage.screenPoint(x, y, 4 + 2, 4 + 30 + 3, client.getWindow().getGuiScaledWidth() - 8 - 4,
					client.getWindow().getGuiScaledHeight() - 8 - 30 - 6);
			return new double[] {position[0] * scale, position[1] * scale};
		});
		input.setCursorPos(point[0], point[1]);
		context.waitTicks(2);
	}

	/** Poe o mouse em cima de um no da arvore, com a tela no tamanho atual. */
	private static void hoverTree(ClientGameTestContext context, TestInput input, Technique technique, NenUpgrade upgrade) {
		double[] point = context.computeOnClient(client -> {
			int scale = client.getWindow().getGuiScale();
			int guiWidth = client.getWindow().getGuiScaledWidth();
			int guiHeight = client.getWindow().getGuiScaledHeight();
			int areaLeft = 4 + 2;
			int areaTop = 4 + 30 + 3;
			int areaWidth = guiWidth - 8 - 4;
			int areaHeight = guiHeight - 8 - 30 - 6;
			float[] node = technique != null ? NenTreePage.screenPoint(technique, areaLeft, areaTop, areaWidth, areaHeight)
					: NenTreePage.screenPoint(upgrade, areaLeft, areaTop, areaWidth, areaHeight);
			return new double[] {node[0] * scale, node[1] * scale};
		});
		input.setCursorPos(point[0], point[1]);
		context.waitTicks(2);
	}

	/** Nivel, gravacao e o que cada nivel de atributo da. */
	private static void checkAttributes(NenData base) {
		NenData data = base.withActive(Technique.NONE);
		for (NenStat stat : NenStat.values()) {
			data = data.withAttributeLevel(stat, 0);
		}
		NenData zero = data;

		// Curva: nivel = raiz(XP / 5), com teto no 100
		NenData forty = zero.withAttributeLevel(NenStat.VITALITY, 40);
		check(forty.attributeLevel(NenStat.VITALITY) == 40 && forty.attributeProgress(NenStat.VITALITY) == 0, "nivel 40 deveria comecar com 0%");
		check(forty.addAttributeXp(NenStat.VITALITY, 332.1).attributeProgress(NenStat.VITALITY) == 82, "405 XP separam o 40 do 41: 332 deveria dar 82%");
		check(forty.addAttributeXp(NenStat.VITALITY, 405).attributeLevel(NenStat.VITALITY) == 41, "405 XP deveriam subir para o 41");
		NenData maxed = zero.withAttributeLevel(NenStat.VITALITY, 100).addAttributeXp(NenStat.VITALITY, 1.0E9);
		check(maxed.attributeLevel(NenStat.VITALITY) == 100 && maxed.attributeProgress(NenStat.VITALITY) == 100, "o atributo deveria parar no 100");

		// Mundo antigo: pontos viram nivel na mesma fracao do limite e voltam a ficar livres; desconhecidos sao ignorados
		NenData legacy = NenData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
				"{\"awakened\":true,\"stats\":{\"vitality\":20,\"attack_speed\":25,\"agility\":3,\"nada\":9}}")).getOrThrow();
		check(legacy.attributeLevel(NenStat.VITALITY) == 50, "20 de 40 pontos deveriam virar Vitalidade 50, veio " + legacy.attributeLevel(NenStat.VITALITY));
		check(legacy.attributeLevel(NenStat.AGILITY) == 100, "a velocidade de ataque antiga deveria contar na Agilidade");
		check(legacy.pointsSpent() == 0, "os pontos antigos deveriam voltar a ficar livres");
		JsonObject saved = NenData.CODEC.encodeStart(JsonOps.INSTANCE, legacy).getOrThrow().getAsJsonObject();
		check(!saved.has("stats") && saved.has("attribute_xp"), "deveria gravar so a XP dos atributos: " + saved);
		check(NenData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().attributeLevel(NenStat.VITALITY) == 50, "a XP dos atributos deveria voltar do save");

		// O que o nivel 100 da
		check(zero.withAttributeLevel(NenStat.AURA_CAPACITY, 100).maxAura() == zero.maxAura() + 500, "Capacidade 100 deveria dar +500 de aura");
		NenData flow = zero.withAttributeLevel(NenStat.AURA_FLOW, 100);
		check(close(flow.auraRegen(), zero.auraRegen() + 20), "Fluxo 100 deveria recuperar +20/s, veio " + flow.auraRegen());
		check(close(flow.auraDrain(Technique.REN), zero.auraDrain(Technique.REN) * 0.7), "Fluxo 100 deveria gastar 30% menos");
		NenBonuses attack = NenBonuses.of(zero.withAttributeLevel(NenStat.ATTACK_POWER, 100));
		check(close(attack.flatDamage(), 10) && close(attack.damageBonus(), NenBonuses.of(zero).damageBonus() + 1), "Ataque 100 deveria dar +10 e +100%");
		check(close(zero.withAttributeLevel(NenStat.ATTACK_POWER, 100).renDamage(), zero.renDamage() * 1.5), "Ataque 100 deveria dar +50% de impacto no Ren");
		check(close(zero.withAttributeLevel(NenStat.ATTACK_POWER, 100).koDamage(1), zero.koDamage(1) * 1.5), "Ataque 100 deveria dar +50% de impacto no Ko");
		NenBonuses agility = NenBonuses.of(zero.withAttributeLevel(NenStat.AGILITY, 100));
		check(close(agility.moveSpeed(), 0.45) && close(agility.attackSpeed(), 0.25), "Agilidade 100 deveria dar +45% de movimento e +25% de ataque");
		NenData perceptive = zero.withAttributeLevel(NenStat.PERCEPTION, 100);
		check(close(perceptive.enMaxSize(), zero.enMaxSize() * 2), "Percepcao 100 deveria dobrar o alcance do En");
		double perBlockBefore = (zero.auraDrain(Technique.EN) - NenRules.EN_DRAIN_BASE) / zero.enSize();
		double perBlockAfter = (perceptive.auraDrain(Technique.EN) - NenRules.EN_DRAIN_BASE) / perceptive.enSize();
		check(close(perBlockAfter, perBlockBefore * 0.6), "Percepcao 100 deveria gastar 40% menos por bloco");
		NenData ten = zero.withActive(Technique.TEN);
		check(close(NenBonuses.of(ten.withAttributeLevel(NenStat.PROTECTION, 100)).damageTaken(), 1 - ten.tenDefense() * 1.75), "Protecao 100 deveria reforcar a defesa do Ten");

		// Multiplicadores do treino
		check(close(NenTraining.idleFactor(0), 1) && close(NenTraining.idleFactor(60), 1), "o primeiro minuto parado nao deveria perder XP");
		check(close(NenTraining.idleFactor(240), 0.2) && close(NenTraining.idleFactor(5000), 0.2), "parado por muito tempo deveria render 20%");
		check(close(NenTraining.combatFactor(0), 1.5) && close(NenTraining.combatFactor(600), 2), "o combate deveria render de 1,5x a 2x");
	}

	/** Golpes, dano absorvido e troca de estados pelo caminho de verdade. */
	private static void checkTrainingEvents(ServerPlayer player, NenData start) {
		// Longe do teto, onde a XP para de subir
		NenData base = start.withAttributeLevel(NenStat.ATTACK_POWER, 10);
		Zombie target = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.TRIGGERED);
		DamageSource hit = player.damageSources().playerAttack(player);

		player.setAttached(ModAttachments.NEN, base.withActive(Technique.NONE));
		NenCombat.incomingDamage(target, hit, 5.0F);
		check(attributeXp(player, NenStat.ATTACK_POWER) == base.attributeXp().getOrDefault(NenStat.ATTACK_POWER, 0.0), "golpe sem Nen nao deveria treinar o Ataque");
		check(NenTraining.multiplier(player) >= 1.5, "acertar um golpe deveria contar como combate, veio " + NenTraining.multiplier(player));

		player.setAttached(ModAttachments.NEN, base.withActive(Technique.REN));
		double attackBefore = attributeXp(player, NenStat.ATTACK_POWER);
		NenCombat.incomingDamage(target, hit, 5.0F);
		check(attributeXp(player, NenStat.ATTACK_POWER) > attackBefore, "golpe com Ren deveria treinar o Ataque");

		player.setAttached(ModAttachments.NEN, base.withActive(Technique.TEN));
		double defenseBefore = attributeXp(player, NenStat.PROTECTION);
		NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
		check(attributeXp(player, NenStat.PROTECTION) > defenseBefore, "dano absorvido pelo Ten deveria treinar a Protecao");

		player.setAttached(ModAttachments.NEN, base.withActive(Technique.ZETSU));
		defenseBefore = attributeXp(player, NenStat.PROTECTION);
		NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
		check(attributeXp(player, NenStat.PROTECTION) == defenseBefore, "em Zetsu nao ha defesa de Nen para treinar");

		player.setAttached(ModAttachments.NEN, base.withActive(Technique.NONE).withAura(base.maxAura()));
		double flowBefore = attributeXp(player, NenStat.AURA_FLOW);
		NenActions.toggle(player, Technique.TEN);
		check(attributeXp(player, NenStat.AURA_FLOW) == flowBefore, "a primeira tecnica ligada ainda nao e uma troca");
		NenActions.toggle(player, Technique.REN);
		check(attributeXp(player, NenStat.AURA_FLOW) > flowBefore, "trocar rapido do Ten para o Ren deveria treinar o Fluxo");
	}

	private static double attributeXp(ServerPlayer player, NenStat stat) {
		return player.getAttachedOrCreate(ModAttachments.NEN).attributeXp().getOrDefault(stat, 0.0);
	}

	private static boolean close(double a, double b) {
		return Math.abs(a - b) < 1.0E-6;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
