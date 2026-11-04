package com.hxhmod.gametest;

import com.hxhmod.client.NenScreen;
import com.hxhmod.nen.AuraColors;
import com.hxhmod.nen.AuraShow;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.RyuFlow;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.Technique;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Prints da aura em cada tecnica, ao entardecer como nas referencias, e o estado enviado aos clientes. */
public class NenAuraGameTest implements FabricClientGameTest {
	private static final int SHOW_TICKS = 40;
	private static final int SUNSET = 12150;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + SUNSET);
			});

			// Ten: uma visao de cada faixa de dominio
			shot(context, singleplayer, "aura_1_first_ten", CameraType.FIRST_PERSON,
					data -> mastered(data, Technique.TEN, 60).withActive(Set.of(Technique.TEN)), 1);
			shot(context, singleplayer, "aura_2a_ten_low", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.TEN, 10).withActive(Set.of(Technique.TEN)), 1);
			expect(singleplayer, "Ten com dominio 10 mostra aura suave", show -> show.level() == AuraShow.SOFT && show.mastery() == 10);
			shot(context, singleplayer, "aura_2b_ten_mid", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.TEN, 50).withActive(Set.of(Technique.TEN)), 1);
			shot(context, singleplayer, "aura_2c_ten_high", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.TEN, 100).withActive(Set.of(Technique.TEN)), 1);

			// Ren: do basico ao maximo
			shot(context, singleplayer, "aura_3a_ren_low", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.REN, 10).withActive(Set.of(Technique.TEN, Technique.REN)), 3);
			shot(context, singleplayer, "aura_3b_ren_medium", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.REN, 50).withActive(Set.of(Technique.TEN, Technique.REN)), 2);
			shot(context, singleplayer, "aura_3c_ren_max", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.REN, 100).withActive(Set.of(Technique.TEN, Technique.REN)), 3);
			expect(singleplayer, "Ren maximo mostra as chamas altas com o dominio do Ren", show -> show.level() == AuraShow.BURST && show.mastery() == 100);

			// Shu: reveste so o objeto
			shot(context, singleplayer, "aura_4a_shu_first", CameraType.FIRST_PERSON,
					data -> mastered(data, Technique.SHU, 60).withActive(Set.of(Technique.SHU)), 1);
			expect(singleplayer, "Shu sozinho fica so no objeto", show -> show.level() == 0 && show.shuMastery() == 60);
			shot(context, singleplayer, "aura_4b_shu_third", CameraType.THIRD_PERSON_FRONT,
					data -> mastered(data, Technique.SHU, 60).withActive(Set.of(Technique.SHU)), 1);

			shot(context, singleplayer, "aura_4c_shu_back", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.SHU, 100).withActive(Set.of(Technique.SHU)), 1);

			// Gyo: chamas nas bordas e o que estava invisivel aparece
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
						"summon pig " + player.getX() + " " + player.getY() + " " + (player.getZ() + 6)
								+ " {NoAI:1b,active_effects:[{id:\"minecraft:invisibility\",duration:2000,show_particles:0b}]}");
			});
			shot(context, singleplayer, "aura_5a_gyo_first", CameraType.FIRST_PERSON,
					data -> mastered(data, Technique.GYO, 80).withActive(Set.of(Technique.GYO)), 1);
			shot(context, singleplayer, "aura_5b_gyo_third", CameraType.THIRD_PERSON_FRONT,
					data -> mastered(data, Technique.GYO, 80).withActive(Set.of(Technique.GYO)), 1);
			expect(singleplayer, "Gyo sozinho nao poe aura no corpo", show -> show.level() == 0 && show.gyoMastery() == 80);
			singleplayer.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(
					server.createCommandSourceStack().withSuppressedOutput(), "kill @e[type=pig]"));

			// Ko: tudo no punho, crescendo com a carga
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});
			shot(context, singleplayer, "aura_6a_ko_third", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.KO, 60).withActive(Set.of(Technique.KO)), 1);
			expect(singleplayer, "Ko leva toda a aura para o punho", show -> show.level() == 0 && show.ko());
			shot(context, singleplayer, "aura_6b_ko_first", CameraType.FIRST_PERSON,
					data -> mastered(data, Technique.KO, 60).withActive(Set.of(Technique.KO)), 1);
			singleplayer.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD)));

			// Ryu: a mesma aura com a densidade indo para os bracos e o objeto
			shot(context, singleplayer, "aura_7a_ryu_balanced", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.KEN, 60).withActive(Set.of(Technique.TEN, Technique.REN, Technique.KEN, Technique.RYU)), 2);

			ryuStance(singleplayer, RyuFlow.Stance.ATTACK);
			shot(context, singleplayer, "aura_7b_ryu_attack", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.KEN, 60).withActive(Set.of(Technique.TEN, Technique.REN, Technique.KEN, Technique.RYU)), 2);
			expect(singleplayer, "Ryu em ataque manda mais aura para os bracos", show -> show.offense() > 0.5F);
			ryuStance(singleplayer, RyuFlow.Stance.DEFEND);
			shot(context, singleplayer, "aura_7c_ryu_defend", CameraType.THIRD_PERSON_BACK,
					data -> mastered(data, Technique.KEN, 60).withActive(Set.of(Technique.TEN, Technique.REN, Technique.KEN, Technique.RYU)), 2);
			expect(singleplayer, "Ryu em defesa manda mais aura para o corpo", show -> show.offense() < 0.5F);
			ryuStance(singleplayer, RyuFlow.Stance.BALANCED);

			// En: da esfera tremendo a esfera firme
			shot(context, singleplayer, "aura_8a_en_low", CameraType.THIRD_PERSON_FRONT,
					data -> mastered(data, Technique.EN, 100).withActive(Set.of(Technique.TEN, Technique.EN)), 1);
			shot(context, singleplayer, "aura_8b_en_inside", CameraType.FIRST_PERSON,
					data -> mastered(data, Technique.EN, 100).withActive(Set.of(Technique.TEN, Technique.EN)), 4);

			// Aba Aura: clicar numa cor pronta chega ao servidor e muda a aura no mundo
			shot(context, singleplayer, "aura_9_page_setup", CameraType.THIRD_PERSON_BACK,
					data -> data.withActive(Set.of(Technique.TEN, Technique.REN)), 3);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.setScreen(NenScreen::new);
			context.waitTicks(5);
			TestInput input = context.getInput();
			for (int i = 0; i < 3; i++) {
				input.pressKey(InputConstants.KEY_TAB);
				context.waitTicks(2);
			}
			context.waitTicks(20);
			context.takeScreenshot("aura_a_page");
			int fire = AuraColors.Preset.FIRE.ordinal();
			int[] center = context.computeOnClient(client -> ((NenScreen) client.gui.screen()).auraPresetCenter(fire));
			int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
			input.setCursorPos(center[0] * scale, center[1] * scale);
			context.waitTicks(2);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(15);
			context.takeScreenshot("aura_b_page_fire");
			singleplayer.getServer().runOnServer(server -> {
				AuraColors saved = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.AURA_COLORS);
				if (!saved.equals(AuraColors.Preset.FIRE.colors())) {
					throw new AssertionError("o clique na cor pronta deveria mudar a cor no servidor, ficou " + saved);
				}
				AuraColors dark = new AuraColors(0xFF000000, 0x100000).sanitized();
				if (dark.primary() >>> 24 != 0 || (dark.secondary() >> 16 & 0xFF) < 90) {
					throw new AssertionError("cor escura deveria ser clareada, ficou " + dark);
				}
			});

			// Arrastar a matiz: so chega ao servidor ao soltar, e a cor principal vira verde
			int[] from = context.computeOnClient(client -> ((NenScreen) client.gui.screen()).auraHuePoint(0.1F));
			int[] to = context.computeOnClient(client -> ((NenScreen) client.gui.screen()).auraHuePoint(0.33F));
			input.setCursorPos(from[0] * scale, from[1] * scale);
			context.waitTicks(2);
			input.holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(2);
			input.setCursorPos(to[0] * scale, to[1] * scale);
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				AuraColors during = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.AURA_COLORS);
				if (!during.equals(AuraColors.Preset.FIRE.colors())) {
					throw new AssertionError("durante o arraste a cor nao deveria ir ao servidor");
				}
			});
			input.releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			context.takeScreenshot("aura_b2_page_slider");
			singleplayer.getServer().runOnServer(server -> {
				AuraColors after = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.AURA_COLORS);
				int color = after.primary();
				if ((color >> 8 & 0xFF) <= (color >> 16 & 0xFF) || after.secondary() != AuraColors.Preset.FIRE.colors().secondary()) {
					throw new AssertionError("arrastar a matiz deveria deixar a principal verde e manter a secundaria, ficou " + after);
				}
			});
			input.setCursorPos(center[0] * scale, center[1] * scale);
			context.waitTicks(2);
			input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(5);

			input.resizeWindow(1920, 1080);
			context.waitTicks(10);
			context.takeScreenshot("aura_c_page_1080");
			input.resizeWindow(854, 480);
			context.waitTicks(5);
			context.setScreen(() -> null);
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(30);
			context.takeScreenshot("aura_d_fire_world");
			shot(context, singleplayer, "aura_e_fire_en", CameraType.THIRD_PERSON_FRONT,
					data -> mastered(data, Technique.EN, 100).withActive(Set.of(Technique.TEN, Technique.EN)), 1);

			// Zetsu nao mostra nada
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(ModAttachments.NEN, aware().grant(Technique.ZETSU).withActive(Technique.ZETSU));
			});
			context.waitTicks(3);
			singleplayer.getServer().runOnServer(server -> {
				AuraShow show = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.AURA_SHOW);
				if (show.level() != 0) {
					throw new AssertionError("Zetsu nao deveria mostrar aura, nivel " + show.level());
				}
			});
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	/** O que os outros jogadores recebem sobre a aura de quem esta no teste. */
	private static void expect(TestSingleplayerContext singleplayer, String rule, Predicate<AuraShow> check) {
		singleplayer.getServer().runOnServer(server -> {
			AuraShow show = server.getPlayerList().getPlayers().getFirst().getAttachedOrCreate(ModAttachments.AURA_SHOW);
			if (!check.test(show)) {
				throw new AssertionError(rule + " deveria valer, mas chegou " + show);
			}
		});
	}

	private static void ryuStance(TestSingleplayerContext singleplayer, RyuFlow.Stance stance) {
		singleplayer.getServer().runOnServer(server -> RyuFlow.setStance(server.getPlayerList().getPlayers().getFirst(), stance));
	}

	/** Treina a tecnica ate este dominio. */
	private static NenData mastered(NenData data, Technique technique, int mastery) {
		return data.addTraining(technique, (int) (mastery * mastery * NenRules.MASTERY_SECONDS_FACTOR) + 1);
	}

	private static NenData aware() {
		NenData data = NenData.DEFAULT.awaken(NenType.ENHANCEMENT);
		for (Technique technique : Technique.values()) {
			data = data.grant(technique);
		}
		return data;
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name, CameraType camera,
			UnaryOperator<NenData> setup, int intensity) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			NenData data = setup.apply(aware()).withAura(100000);
			// Nivel pedido para a tecnica que tem intensidade neste print
			Technique tuned = data.isActive(Technique.EN) ? Technique.EN : Technique.REN;
			while (data.intensityOf(tuned) != Math.min(intensity, tuned.maxIntensity())) {
				data = data.cycledIntensity(tuned);
			}
			player.setAttached(ModAttachments.NEN, data);
			player.setYRot(0);
			player.setXRot(0);
		});
		context.runOnClient(client -> client.options.setCameraType(camera));
		context.waitTicks(SHOW_TICKS);
		context.takeScreenshot(name);
	}
}
