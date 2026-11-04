package com.hxhmod.client;

import com.hxhmod.nen.AuraColors;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Aba Aura: cores prontas, ajuste fino de cada uma das duas cores e o personagem mostrando o resultado.
 * Enquanto uma barra e arrastada so a previa muda; o servidor recebe a cor ao soltar o mouse.
 */
final class NenAuraPage {
	private static final int GAP = 8;
	private static final int PRESET_WIDTH = 80;
	private static final int PRESET_HEIGHT = 15;
	private static final int PRESET_COLUMNS = 2;
	private static final int SWATCH = 6;
	private static final int BAR_HEIGHT = 7;
	private static final int SLIDER_STEP = 21;
	private static final int PREVIEW_SIZE = 38;
	private static final float MIN_BRIGHTNESS = 0.35F;
	private static final float MAX_HUE = 0.999F;
	private static final int HUE = 0;
	private static final int SATURATION = 1;
	private static final int BRIGHTNESS = 2;
	private static final int NOT_DRAGGING = -1;
	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF8E8A80;
	private static final int HANDLE = 0xFFFFFFFF;
	private static final int HANDLE_SHADOW = 0xFF101014;

	private final NenScreen screen;
	/** Cores na tela, que podem estar a frente do servidor durante o arraste. */
	private AuraColors colors;
	private boolean editingSecondary;
	/** Matiz, saturacao e brilho da cor em edicao: a matiz nao se perde quando a saturacao chega a zero. */
	private final float[] hsv = new float[3];
	private int dragging = NOT_DRAGGING;
	private int barX;
	private int barWidth;
	private int originX;
	private int originY;

	NenAuraPage(NenScreen screen) {
		this.screen = screen;
	}

	void render(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		if (colors == null) {
			load();
		}
		originX = x;
		originY = y;
		renderPresets(graphics, font, x, y, mouseX, mouseY);

		int presetsWidth = PRESET_COLUMNS * PRESET_WIDTH + PRESET_COLUMNS - 1;
		int customX = x + presetsWidth + GAP;
		int previewWidth = NenScreen.PREVIEW_WIDTH;
		renderCustom(graphics, font, customX, y, width - presetsWidth - GAP * 2 - previewWidth, mouseX, mouseY);
		renderPreview(graphics, x + width - previewWidth, y, previewWidth, height, mouseX, mouseY);
		screen.footer(graphics, Component.translatable("screen.hxh.nen.hint.aura"));
	}

	private void load() {
		LocalPlayer player = Minecraft.getInstance().player;
		colors = player == null ? AuraColors.DEFAULT : player.getAttachedOrElse(ModAttachments.AURA_COLORS, AuraColors.DEFAULT);
		loadSliders();
	}

	private void renderPresets(GuiGraphicsExtractor graphics, Font font, int x, int y, int mouseX, int mouseY) {
		screen.sectionTitle(graphics, Component.translatable("screen.hxh.nen.aura_presets"), x, y, PRESET_COLUMNS * PRESET_WIDTH + PRESET_COLUMNS - 1);
		AuraColors.Preset[] presets = AuraColors.Preset.values();
		for (int i = 0; i < presets.length; i++) {
			AuraColors.Preset preset = presets[i];
			int rowX = presetX(x, i);
			int rowY = presetY(y, i);
			boolean hover = NenScreen.hovered(rowX, rowY, PRESET_WIDTH, PRESET_HEIGHT - 1, mouseX, mouseY);
			screen.row(graphics, rowX, rowY, PRESET_WIDTH, PRESET_HEIGHT - 1, preset.colors().equals(colors), hover);
			swatch(graphics, rowX + 4, rowY + 4, preset.colors().primary());
			swatch(graphics, rowX + 4 + SWATCH, rowY + 4, preset.colors().secondary());
			Component name = screen.panelFont(Component.translatable("screen.hxh.nen.aura.preset." + preset.id()));
			graphics.text(font, name, rowX + 7 + SWATCH * 2, rowY + 3, TEXT, false);
			screen.addClickable(rowX, rowY, PRESET_WIDTH, PRESET_HEIGHT - 1, true, () -> apply(preset.colors()));
		}
	}

	private static int presetX(int x, int index) {
		return x + index % PRESET_COLUMNS * (PRESET_WIDTH + 1);
	}

	private static int presetY(int y, int index) {
		return y + 15 + index / PRESET_COLUMNS * PRESET_HEIGHT;
	}

	/** Centro de uma cor pronta na tela, para os testes clicarem nela. */
	int[] presetCenter(int index) {
		return new int[] {presetX(originX, index) + PRESET_WIDTH / 2, presetY(originY, index) + PRESET_HEIGHT / 2};
	}

	/** Ponto na barra de matiz, para os testes arrastarem. */
	int[] huePoint(float fraction) {
		return new int[] {barX + Math.round(fraction * (barWidth - 1)), originY + 15 + PRESET_HEIGHT * 2 + 5 + 10 + BAR_HEIGHT / 2};
	}

	private void renderCustom(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
		screen.sectionTitle(graphics, Component.translatable("screen.hxh.nen.aura_custom"), x, y, width);
		int rowY = y + 15;
		slot(graphics, font, "screen.hxh.nen.aura.primary", false, colors.primary(), x, rowY, width, mouseX, mouseY);
		slot(graphics, font, "screen.hxh.nen.aura.secondary", true, colors.secondary(), x, rowY + PRESET_HEIGHT, width, mouseX, mouseY);

		barX = x + 2;
		barWidth = width - 4;
		int sliderY = rowY + PRESET_HEIGHT * 2 + 5;
		slider(graphics, font, HUE, "screen.hxh.nen.aura.hue", sliderY);
		slider(graphics, font, SATURATION, "screen.hxh.nen.aura.saturation", sliderY + SLIDER_STEP);
		slider(graphics, font, BRIGHTNESS, "screen.hxh.nen.aura.brightness", sliderY + SLIDER_STEP * 2);
	}

	private void slot(GuiGraphicsExtractor graphics, Font font, String key, boolean secondary, int color, int x, int y, int width, int mouseX, int mouseY) {
		boolean hover = NenScreen.hovered(x, y, width, PRESET_HEIGHT - 1, mouseX, mouseY);
		screen.row(graphics, x, y, width, PRESET_HEIGHT - 1, editingSecondary == secondary, hover);
		graphics.fill(x + 4, y + 3, x + 12, y + 11, HANDLE_SHADOW);
		graphics.fill(x + 5, y + 4, x + 11, y + 10, 0xFF000000 | color);
		graphics.text(font, screen.panelFont(Component.translatable(key)), x + 16, y + 3, TEXT, false);
		screen.addClickable(x, y, width, PRESET_HEIGHT - 1, true, () -> {
			editingSecondary = secondary;
			loadSliders();
		});
	}

	private void slider(GuiGraphicsExtractor graphics, Font font, int channel, String key, int y) {
		graphics.text(font, screen.panelFont(Component.translatable(key)), barX, y, DIM, false);
		int barY = y + 10;
		graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + BAR_HEIGHT + 1, HANDLE_SHADOW);
		for (int column = 0; column < barWidth; column++) {
			float position = barWidth > 1 ? (float) column / (barWidth - 1) : 0;
			graphics.fill(barX + column, barY, barX + column + 1, barY + BAR_HEIGHT, 0xFF000000 | barColor(channel, position));
		}
		int handle = barX + Math.round(position(channel) * (barWidth - 1));
		graphics.fill(handle - 2, barY - 2, handle + 3, barY + BAR_HEIGHT + 2, HANDLE_SHADOW);
		graphics.fill(handle - 1, barY - 1, handle + 2, barY + BAR_HEIGHT + 1, HANDLE);
		screen.addPressable(barX - 2, barY - 2, barWidth + 4, BAR_HEIGHT + 4, mouseX -> {
			dragging = channel;
			drag(mouseX);
		});
	}

	private int barColor(int channel, float position) {
		return switch (channel) {
			case HUE -> Mth.hsvToRgb(Math.min(position, MAX_HUE), 1, 1);
			case SATURATION -> Mth.hsvToRgb(hsv[HUE], position, Math.max(hsv[BRIGHTNESS], 0.6F));
			default -> Mth.hsvToRgb(hsv[HUE], hsv[SATURATION], Mth.lerp(position, MIN_BRIGHTNESS, 1));
		};
	}

	private float position(int channel) {
		return channel == BRIGHTNESS ? (hsv[BRIGHTNESS] - MIN_BRIGHTNESS) / (1 - MIN_BRIGHTNESS) : hsv[channel];
	}

	private void renderPreview(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		AuraRender.beginPreview(colors);
		// Menor que nas outras abas: as chamas passam bastante da altura do corpo
		InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + width, y + height, PREVIEW_SIZE, 0.0625F, mouseX, mouseY, player);
		AuraRender.endPreview();
	}

	private static void swatch(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | color);
	}

	// ---- Edicao ----

	boolean drag(double mouseX) {
		if (dragging == NOT_DRAGGING || barWidth <= 1) {
			return false;
		}
		float position = Mth.clamp((float) (mouseX - barX) / (barWidth - 1), 0, 1);
		// A matiz 1 da volta ao comeco da barra: a ponta direita para logo antes
		hsv[dragging] = dragging == BRIGHTNESS ? Mth.lerp(position, MIN_BRIGHTNESS, 1) : dragging == HUE ? Math.min(position, MAX_HUE) : position;
		int color = Mth.hsvToRgb(hsv[HUE], hsv[SATURATION], hsv[BRIGHTNESS]) & 0xFFFFFF;
		colors = editingSecondary ? new AuraColors(colors.primary(), color) : new AuraColors(color, colors.secondary());
		return true;
	}

	boolean release() {
		if (dragging == NOT_DRAGGING) {
			return false;
		}
		dragging = NOT_DRAGGING;
		send();
		return true;
	}

	private void apply(AuraColors chosen) {
		colors = chosen;
		loadSliders();
		send();
	}

	private void send() {
		if (ClientPlayNetworking.canSend(NenPayloads.SetAuraColors.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.SetAuraColors(colors.primary(), colors.secondary()));
		}
	}

	private void loadSliders() {
		int color = editingSecondary ? colors.secondary() : colors.primary();
		float red = (color >> 16 & 0xFF) / 255F;
		float green = (color >> 8 & 0xFF) / 255F;
		float blue = (color & 0xFF) / 255F;
		float max = Math.max(red, Math.max(green, blue));
		float min = Math.min(red, Math.min(green, blue));
		float delta = max - min;
		// Sem saturacao qualquer matiz da a mesma cor: fica a que ja estava na barra
		if (delta > 0) {
			float hue = max == red ? (green - blue) / delta : max == green ? (blue - red) / delta + 2 : (red - green) / delta + 4;
			hsv[HUE] = (hue / 6 + 1) % 1;
		}
		hsv[SATURATION] = max > 0 ? delta / max : 0;
		hsv[BRIGHTNESS] = Mth.clamp(max, MIN_BRIGHTNESS, 1);
	}
}
