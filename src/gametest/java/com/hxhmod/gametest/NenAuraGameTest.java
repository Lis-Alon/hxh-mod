package com.hxhmod.gametest;

import com.hxhmod.nen.AuraShow;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.Technique;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.UnaryOperator;

/** Prints da aura em cada tecnica, ao entardecer como nas referencias, e o estado enviado aos clientes. */
public class NenAuraGameTest implements FabricClientGameTest {
	private static final int SHOW_TICKS = 40;
	private static final int SUNSET = 11600;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set " + SUNSET);
			});

			// Primeira pessoa
			shot(context, singleplayer, "aura_1_first_ten", CameraType.FIRST_PERSON,
					data -> data.withActive(Set.of(Technique.TEN)), 1);
			shot(context, singleplayer, "aura_2_first_shu", CameraType.FIRST_PERSON,
					data -> data.withActive(Set.of(Technique.TEN, Technique.SHU)), 1);

			// Terceira pessoa
			shot(context, singleplayer, "aura_3_third_ten", CameraType.THIRD_PERSON_BACK,
					data -> data.withActive(Set.of(Technique.TEN)), 1);
			shot(context, singleplayer, "aura_4_third_ren", CameraType.THIRD_PERSON_BACK,
					data -> data.withActive(Set.of(Technique.TEN, Technique.REN)), 2);
			shot(context, singleplayer, "aura_5_third_burst", CameraType.THIRD_PERSON_BACK,
					data -> data.withActive(Set.of(Technique.TEN, Technique.REN)), 3);
			shot(context, singleplayer, "aura_6_third_shu", CameraType.THIRD_PERSON_FRONT,
					data -> data.withActive(Set.of(Technique.TEN, Technique.SHU)), 1);
			shot(context, singleplayer, "aura_7_en_outside", CameraType.THIRD_PERSON_FRONT,
					data -> data.addTraining(Technique.EN, 60000).withActive(Set.of(Technique.TEN, Technique.EN)), 1);
			shot(context, singleplayer, "aura_8_en_inside", CameraType.FIRST_PERSON,
					data -> data.addTraining(Technique.EN, 60000).withActive(Set.of(Technique.TEN, Technique.EN)), 4);

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
