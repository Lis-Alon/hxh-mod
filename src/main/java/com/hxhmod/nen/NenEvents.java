package com.hxhmod.nen;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class NenEvents {
	private static final int REGEN_INTERVAL_TICKS = 20;
	private static final int REGEN_AMOUNT = 2;

	private NenEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % REGEN_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
				if (data.awakened() && data.aura() < data.maxAura()) {
					player.setAttached(ModAttachments.NEN, data.withAura(data.aura() + REGEN_AMOUNT));
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				Commands.literal("nen")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
							if (!data.awakened()) {
								context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.not_awakened"), false);
							} else {
								context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.status",
										data.type().displayName(), data.aura(), data.maxAura()), false);
							}
							return 1;
						})
						// /nen reset: apaga o tipo de Nen para refazer o teste (so operadores)
						.then(Commands.literal("reset")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> {
									ServerPlayer player = context.getSource().getPlayerOrException();
									player.setAttached(ModAttachments.NEN, NenData.DEFAULT);
									context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.reset"), false);
									return 1;
								}))));
	}
}
