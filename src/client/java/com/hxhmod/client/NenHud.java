package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.Technique;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Barra de aura no canto superior esquerdo, so aparece depois do despertar. */
public final class NenHud {
	private static final int X = 6;
	private static final int Y = 6;
	private static final int BAR_WIDTH = 82;
	private static final int BAR_HEIGHT = 5;
	private static final int UNKNOWN_TYPE_COLOR = 0xFFFFFF;

	private NenHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gui.screen() instanceof NenScreen) {
			return;
		}
		NenData data = minecraft.player.getAttached(ModAttachments.NEN);
		if (data == null || !data.awakened()) {
			return;
		}

		Component type = data.revealed() ? data.type().displayName() : Component.literal("???");
		Component line = data.active() == Technique.NONE
				? Component.translatable("nen.hxh.hud", type, data.aura(), data.maxAura())
				: Component.translatable("nen.hxh.hud_active", type, data.aura(), data.maxAura(), data.active().displayName());
		graphics.text(minecraft.font, line, X, Y, 0xFFFFFFFF, true);

		int barColor = 0xFF000000 | (data.revealed() ? data.type().barColor() : UNKNOWN_TYPE_COLOR);
		int barY = Y + minecraft.font.lineHeight + 2;
		int filled = data.maxAura() > 0 ? BAR_WIDTH * data.aura() / data.maxAura() : 0;
		graphics.fill(X - 1, barY - 1, X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xC0000000);
		graphics.fill(X, barY, X + filled, barY + BAR_HEIGHT, barColor);
	}
}
