package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Barra de aura no canto superior esquerdo, so aparece depois do teste de Nen. */
public final class NenHud {
	private static final int X = 6;
	private static final int Y = 6;
	private static final int BAR_WIDTH = 82;
	private static final int BAR_HEIGHT = 5;

	private NenHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		NenData data = minecraft.player.getAttached(ModAttachments.NEN);
		if (data == null || !data.awakened()) {
			return;
		}

		int barColor = 0xFF000000 | data.type().barColor();

		graphics.text(minecraft.font, Component.translatable("nen.hxh.hud", data.type().displayName(),
				data.aura(), data.maxAura()), X, Y, 0xFFFFFFFF, true);

		int barY = Y + minecraft.font.lineHeight + 2;
		int filled = data.maxAura() > 0 ? BAR_WIDTH * data.aura() / data.maxAura() : 0;
		graphics.fill(X - 1, barY - 1, X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
		graphics.fill(X, barY, X + filled, barY + BAR_HEIGHT, barColor);
	}
}
