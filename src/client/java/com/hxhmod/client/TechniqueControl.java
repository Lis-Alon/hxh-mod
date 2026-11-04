package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.RyuFlow;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

/** Lado do cliente do Ko (segurar a tecla, carga e golpe no ar) e do Ryu (postura pelo ataque e pela defesa). */
public final class TechniqueControl {
	// Depois de um golpe, a aura fica no ataque por um tempo para a sequencia aproveitar
	private static final int ATTACK_STANCE_TICKS = 30;

	private static boolean koKeyDown;
	private static int koTicks;
	private static int attackTicks;
	private static RyuFlow.Stance stance = RyuFlow.Stance.BALANCED;

	private TechniqueControl() {
	}

	static void tick(Minecraft client, KeyMapping koKey) {
		LocalPlayer player = client.player;
		NenData data = player == null ? null : player.getAttached(ModAttachments.NEN);
		if (data == null) {
			koKeyDown = false;
			koTicks = 0;
			stance = RyuFlow.Stance.BALANCED;
			return;
		}
		boolean down = koKey.isDown() && client.gui.screen() == null;
		if (down != koKeyDown && ClientPlayNetworking.canSend(NenPayloads.KoHold.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.KoHold(down));
		}
		koKeyDown = down;
		koTicks = data.isActive(Technique.KO) ? koTicks + 1 : 0;

		if (attackTicks > 0) {
			attackTicks--;
		}
		RyuFlow.Stance wanted = RyuFlow.Stance.BALANCED;
		if (data.isActive(Technique.RYU) && client.gui.screen() == null) {
			if (client.options.keyUse.isDown()) {
				wanted = RyuFlow.Stance.DEFEND;
			} else if (client.options.keyAttack.isDown() || attackTicks > 0) {
				wanted = RyuFlow.Stance.ATTACK;
			}
		}
		if (wanted != stance && ClientPlayNetworking.canSend(NenPayloads.SetRyuStance.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.SetRyuStance(wanted));
			stance = wanted;
		}
	}

	/** Clique de ataque: alimenta a postura do Ryu e, no ar, gasta o Ko. Minerar nao conta como erro. */
	public static void onStartAttack(Minecraft client) {
		attackTicks = ATTACK_STANCE_TICKS;
		LocalPlayer player = client.player;
		NenData data = player == null ? null : player.getAttached(ModAttachments.NEN);
		if (data != null && data.isActive(Technique.KO) && client.missTime <= 0 && client.hitResult != null
				&& client.hitResult.getType() == HitResult.Type.MISS
				&& ClientPlayNetworking.canSend(NenPayloads.KoMiss.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.KoMiss());
		}
	}

	/** Carga do Ko mostrada na tela, de 0 a 1. */
	static float koCharge(NenData data) {
		return data.isActive(Technique.KO) ? Math.min(1F, koTicks / (float) data.koChargeTicks()) : 0F;
	}

	static RyuFlow.Stance stance() {
		return stance;
	}
}
