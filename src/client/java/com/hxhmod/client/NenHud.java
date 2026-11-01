package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Barras de aura e de XP de Nen no canto superior esquerdo, so aparecem depois do despertar. */
public final class NenHud {
	private static final int X = 6;
	private static final int Y = 6;
	private static final int BAR_WIDTH = 82;
	private static final int BAR_HEIGHT = 5;
	private static final int XP_BAR_HEIGHT = 2;
	private static final int UNKNOWN_TYPE_COLOR = 0xFFFFFF;
	private static final int XP_COLOR = 0xFFD8B46A;

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
				? Component.translatable("nen.hxh.hud", data.level(), type, data.aura(), data.maxAura())
				: Component.translatable("nen.hxh.hud_active", data.level(), type, data.aura(), data.maxAura(),
						data.active().displayName());
		graphics.text(minecraft.font, line, X, Y, 0xFFFFFFFF, true);

		int barColor = 0xFF000000 | (data.revealed() ? data.type().barColor() : UNKNOWN_TYPE_COLOR);
		int barY = Y + minecraft.font.lineHeight + 2;
		int filled = data.maxAura() > 0 ? BAR_WIDTH * data.aura() / data.maxAura() : 0;
		graphics.fill(X - 1, barY - 1, X + BAR_WIDTH + 1, barY + BAR_HEIGHT + XP_BAR_HEIGHT + 2, 0xC0000000);
		graphics.fill(X, barY, X + filled, barY + BAR_HEIGHT, barColor);

		int xpY = barY + BAR_HEIGHT + 1;
		int xpFilled = data.maxLevel() ? BAR_WIDTH : BAR_WIDTH * data.xpIntoLevel() / NenRules.xpToNext(data.level());
		graphics.fill(X, xpY, X + xpFilled, xpY + XP_BAR_HEIGHT, XP_COLOR);
	}
}
