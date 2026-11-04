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
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
import com.hxhmod.nen.NenCombat;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenStat;
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

			// Arvore de tecnicas: janela pequena e Full HD, mouse em cima do Ken e compra de uma sub-habilidade pelo clique
			input.pressKey(InputConstants.KEY_TAB);
			context.waitTicks(5);
			context.takeScreenshot("nen_5b_tree_small");
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

			// Gyo preparado: a proxima acao vira a aplicacao e termina a preparacao
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
				check(!data.isActive(Technique.GYO), "o pulo deveria gastar a preparacao do Gyo");
				check(player.getY() - start[0] > 1.5, "o super pulo deveria subir mais que um pulo normal, subiu " + (player.getY() - start[0]));
				start[1] = player.getHealth();
			});
			context.waitTicks(60);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getHealth() >= start[1], "a queda do super pulo nao deveria machucar");
				player.setAttached(ModAttachments.NEN, player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.GYO).withAura(100));
				start[0] = player.getX();
				start[2] = player.getZ();
			});
			context.waitTicks(5);
			input.holdKeyFor(InputConstants.KEY_LCONTROL, 2);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(!data.isActive(Technique.GYO), "o Ctrl deveria gastar a preparacao do Gyo");
				double moved = Math.hypot(player.getX() - start[0], player.getZ() - start[2]);
				check(moved > 2, "o super dash deveria andar mais de 2 blocos, andou " + moved);
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.GYO).withAura(100));
			});
			context.waitTicks(40);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(player.getAttachedOrCreate(ModAttachments.NEN).isActive(Technique.GYO), "esperar nao deveria gastar a preparacao");
			});
			input.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				check(!data.isActive(Technique.GYO) && GyoApplications.guarding(player), "o botao direito deveria virar super defesa");
				float guarded = NenCombat.incomingDamage(player, player.damageSources().generic(), 10.0F);
				check(guarded < 10.0F * NenBonuses.of(data).damageTaken(), "a super defesa deveria reduzir o dano, veio " + guarded);

				// Super soco: so quando o golpe acerta, e soma com os outros bonus
				player.setAttached(ModAttachments.NEN, data.withActive(Technique.GYO).withAura(100));
				NenData prepared = player.getAttachedOrCreate(ModAttachments.NEN);
				Pig target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				double normal = (10.0F + NenBonuses.of(prepared).flatDamage()) * (1 + NenBonuses.of(prepared).damageBonus());
				float punched = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				NenData after = player.getAttachedOrCreate(ModAttachments.NEN);
				check(punched > normal && !after.isActive(Technique.GYO) && after.aura() == 100 - prepared.gyoCost(),
						"o golpe com Gyo deveria ser super soco e gastar a aura, veio " + punched);
				player.setAttached(ModAttachments.NEN, after.withActive(Technique.NONE));
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
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Set.of(Technique.RYU, Technique.KEN)).withAura(100);
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

				// Shu: o beneficio depende do objeto e trocar desfaz
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN).withActive(Technique.SHU).withAura(100);
				player.setAttached(ModAttachments.NEN, data);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
				player.setHealth(player.getMaxHealth());
				check(ShuCoat.kind(player.getMainHandItem()) == ShuCoat.Kind.TOOL, "picareta deveria ser ferramenta");
				check(ShuCoat.miningBonus(player) > 0, "picareta revestida deveria quebrar mais rapido");
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
				check(ShuCoat.kind(new ItemStack(Items.SHIELD)) == ShuCoat.Kind.SHIELD && ShuCoat.kind(new ItemStack(Items.IRON_SWORD)) == ShuCoat.Kind.WEAPON
						&& ShuCoat.kind(new ItemStack(Items.STICK)) == ShuCoat.Kind.COMMON, "os objetos deveriam cair no tipo certo");
				Pig target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				float withTool = NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				check(NenCombat.incomingDamage(target, player.damageSources().playerAttack(player), 10.0F) > withTool,
						"a espada revestida deveria causar mais dano que a picareta");
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

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
