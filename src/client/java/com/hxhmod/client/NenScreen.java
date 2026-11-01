package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.ToggleTechniquePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/** Painel do Nen (tecla N): tipo, nivel, aura, treino das tecnicas e videncia. */
public class NenScreen extends Screen {
	private static final int PANEL_WIDTH = 270;
	private static final int PANEL_HEIGHT = 226;
	private static final int PADDING = 12;
	private static final int ROW_TOP = 82;
	private static final int ROW_HEIGHT = 28;
	private static final int BAR_HEIGHT = 5;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int GOLD = 0xFFFFD45A;
	private static final int AURA_COLOR = 0xFF6FD3FF;
	private static final int TRAINING_COLOR = 0xFF7BE37B;
	private static final int DIVINATION_COLOR = 0xFF4A86E8;

	private final Map<Technique, Button> techniqueButtons = new EnumMap<>(Technique.class);
	private int left;
	private int top;

	public NenScreen() {
		super(Component.translatable("screen.hxh.nen.title"));
	}

	@Override
	protected void init() {
		left = (width - PANEL_WIDTH) / 2;
		top = (height - PANEL_HEIGHT) / 2;

		techniqueButtons.clear();
		int y = top + ROW_TOP;
		for (Technique technique : Technique.TRAINABLE) {
			Button button = Button.builder(Component.empty(),
							b -> ClientPlayNetworking.send(new ToggleTechniquePayload(technique)))
					.bounds(left + PANEL_WIDTH - PADDING - 64, y, 64, 18)
					.build();
			techniqueButtons.put(technique, addRenderableWidget(button));
			y += ROW_HEIGHT;
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 50, top + PANEL_HEIGHT - 28, 100, 20)
				.build());
	}

	private NenData data() {
		if (minecraft.player == null) {
			return NenData.DEFAULT;
		}
		NenData data = minecraft.player.getAttached(ModAttachments.NEN);
		return data != null ? data : NenData.DEFAULT;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101018);
		graphics.fill(left, top, left + PANEL_WIDTH, top + 1, GOLD);
		graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, GOLD);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		NenData data = data();
		updateButtons(data);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(font, title, width / 2, top + 8, GOLD);
		if (data.awakened()) {
			renderAwakened(graphics, data);
		} else {
			renderDormant(graphics, data);
		}
	}

	private void updateButtons(NenData data) {
		for (Map.Entry<Technique, Button> entry : techniqueButtons.entrySet()) {
			Technique technique = entry.getKey();
			Button button = entry.getValue();
			boolean unlocked = technique.isUnlocked(data);
			button.visible = data.awakened();
			button.active = unlocked;
			button.setMessage(Component.translatable(!unlocked ? "screen.hxh.nen.locked"
					: data.active() == technique ? "screen.hxh.nen.deactivate" : "screen.hxh.nen.activate"));
		}
	}

	private void renderDormant(GuiGraphicsExtractor graphics, NenData data) {
		int x = left + PADDING;
		int y = top + 32;
		graphics.text(font, Component.translatable("screen.hxh.nen.dormant"), x, y, WHITE);
		graphics.text(font, Component.translatable("screen.hxh.nen.dormant_hint"), x, y + 14, GRAY);
		graphics.text(font, Component.translatable("screen.hxh.nen.meditation",
				data.meditation(), NenRules.MEDITATION_SECONDS), x, y + 36, WHITE);
		bar(graphics, x, y + 48, PANEL_WIDTH - 2 * PADDING, data.meditation(), NenRules.MEDITATION_SECONDS, AURA_COLOR);
	}

	private void renderAwakened(GuiGraphicsExtractor graphics, NenData data) {
		int x = left + PADDING;
		int innerWidth = PANEL_WIDTH - 2 * PADDING;
		int y = top + 26;

		Component type = data.revealed() ? data.type().displayName() : Component.literal("???");
		graphics.text(font, Component.translatable("screen.hxh.nen.type", type), x, y, WHITE);
		Component level = Component.translatable("screen.hxh.nen.level", data.level());
		graphics.text(font, level, left + PANEL_WIDTH - PADDING - font.width(level), y, GOLD);

		y += 14;
		graphics.text(font, Component.translatable("screen.hxh.nen.aura", data.aura(), data.maxAura()), x, y, WHITE);
		bar(graphics, x, y + 11, innerWidth, data.aura(), data.maxAura(), AURA_COLOR);

		graphics.text(font, Component.translatable("screen.hxh.nen.techniques"), x, top + ROW_TOP - 14, GOLD);
		int rowY = top + ROW_TOP;
		int rowWidth = innerWidth - 72;
		for (Technique technique : Technique.TRAINABLE) {
			renderTechniqueRow(graphics, data, technique, x, rowY, rowWidth);
			rowY += ROW_HEIGHT;
		}

		renderDivination(graphics, data, x, rowY + 2, innerWidth);
	}

	private void renderTechniqueRow(GuiGraphicsExtractor graphics, NenData data, Technique technique, int x, int y, int width) {
		boolean unlocked = technique.isUnlocked(data);
		boolean active = data.active() == technique;
		Component name = technique.displayName();
		graphics.text(font, active ? Component.translatable("screen.hxh.nen.active_name", name) : name,
				x, y, active ? GOLD : unlocked ? WHITE : GRAY);

		if (!unlocked) {
			graphics.text(font, unlockRequirement(technique), x, y + 11, GRAY);
			return;
		}
		int xp = data.xp(technique);
		Component trained = Component.translatable("screen.hxh.nen.trained", xp, technique.milestoneXp());
		graphics.text(font, trained, x + width - font.width(trained), y, GRAY);
		bar(graphics, x, y + 12, width, xp, technique.milestoneXp(), TRAINING_COLOR);
	}

	private static Component unlockRequirement(Technique technique) {
		return switch (technique) {
			case ZETSU -> Component.translatable("screen.hxh.nen.requires",
					NenRules.ZETSU_UNLOCK_TEN_XP, Technique.TEN.displayName());
			case REN -> Component.translatable("screen.hxh.nen.requires",
					NenRules.REN_UNLOCK_ZETSU_XP, Technique.ZETSU.displayName());
			default -> Component.empty();
		};
	}

	private void renderDivination(GuiGraphicsExtractor graphics, NenData data, int x, int y, int width) {
		graphics.text(font, Component.translatable("screen.hxh.nen.divination", data.divination()), x, y, GOLD);
		if (!Technique.REN.isUnlocked(data)) {
			graphics.text(font, Component.translatable("screen.hxh.nen.divination_locked"), x, y + 12, GRAY);
			return;
		}
		bar(graphics, x, y + 12, width, data.divination(), 100, DIVINATION_COLOR);
	}

	private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int value, int max, int color) {
		int filled = max > 0 ? width * Math.min(value, max) / max : 0;
		graphics.fill(x - 1, y - 1, x + width + 1, y + BAR_HEIGHT + 1, 0xFF000000);
		graphics.fill(x, y, x + width, y + BAR_HEIGHT, 0xFF303038);
		graphics.fill(x, y, x + filled, y + BAR_HEIGHT, color);
	}

	@Override
	public boolean isPauseScreen() {
		// O treino continua rodando com o painel aberto
		return false;
	}
}
