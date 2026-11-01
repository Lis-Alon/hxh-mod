package com.hxhmod.client;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.NenStat;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Painel do Nen (tecla N), com tres abas:
 * Nivel (proficiencia e pontos de atributo), Tecnicas (uma aba por tecnica) e Status (registro do treino).
 * Tudo e desenhado a mao; os botoes sao recriados a cada quadro e conferidos no clique.
 */
public class NenScreen extends Screen {
	private static final int MAX_WIDTH = 440;
	private static final int MAX_HEIGHT = 256;
	private static final int MARGIN = 10;
	private static final int HEADER_HEIGHT = 30;
	private static final int ROW_HEIGHT = 16;
	private static final int BUTTON_HEIGHT = 16;
	private static final int TECH_ROW_HEIGHT = 16;
	private static final int TECH_LIST_WIDTH = 112;
	private static final int SHIFT_STEP = 5;

	private static final int BACKDROP = 0x99000000;
	private static final int PANEL_TOP = 0xF4101219;
	private static final int PANEL_BOTTOM = 0xF407080D;
	private static final int GOLD = 0xFFD8B46A;
	private static final int GOLD_DIM = 0xFF6E5A35;
	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF8E8A80;
	private static final int LOCKED = 0xFF5E5C58;
	private static final int GOOD = 0xFF8FD46A;
	private static final int BAD = 0xFFD46A6A;
	private static final int ROW_TOP = 0xFF1C2232;
	private static final int ROW_BOTTOM = 0xFF131726;
	private static final int ROW_SELECTED_TOP = 0xFF6B5B3C;
	private static final int ROW_SELECTED_BOTTOM = 0xFF3A3326;
	private static final int BAR_BACK = 0xFF262A36;
	private static final int XP_COLOR = 0xFFD8B46A;

	private enum Page {
		LEVEL("screen.hxh.nen.page.level"),
		TECHNIQUES("screen.hxh.nen.page.techniques"),
		STATUS("screen.hxh.nen.page.status");

		private final String key;

		Page(String key) {
			this.key = key;
		}

		Component title() {
			return Component.translatable(key);
		}
	}

	/** Area clicavel desenhada neste quadro. */
	private record Clickable(int x, int y, int width, int height, boolean enabled, Runnable action) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private final Map<NenStat, Integer> pending = new EnumMap<>(NenStat.class);
	private final List<Clickable> clickables = new ArrayList<>();
	private Page page = Page.LEVEL;
	private int selectedStat;
	private int selectedTechnique;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;

	public NenScreen() {
		super(Component.translatable("screen.hxh.nen.title"));
	}

	@Override
	protected void init() {
		panelWidth = Math.min(MAX_WIDTH, width - 8);
		panelHeight = Math.min(MAX_HEIGHT, height - 8);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
	}

	private NenData data() {
		if (minecraft.player == null) {
			return NenData.DEFAULT;
		}
		NenData data = minecraft.player.getAttached(ModAttachments.NEN);
		return data != null ? data : NenData.DEFAULT;
	}

	@Override
	public boolean isPauseScreen() {
		// O treino continua rodando com o painel aberto
		return false;
	}

	// ======================================================================
	// Desenho
	// ======================================================================

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(0, 0, width, height, BACKDROP);
		graphics.fillGradient(left, top, left + panelWidth, top + panelHeight, PANEL_TOP, PANEL_BOTTOM);
		graphics.fill(left, top, left + panelWidth, top + 1, GOLD_DIM);
		graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, GOLD_DIM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		clickables.clear();
		NenData data = data();

		renderHeader(graphics, mouseX, mouseY);
		if (!data.awakened()) {
			renderDormant(graphics, data);
			return;
		}
		switch (page) {
			case LEVEL -> renderLevelPage(graphics, data, mouseX, mouseY);
			case TECHNIQUES -> renderTechniquesPage(graphics, data, mouseX, mouseY);
			case STATUS -> renderStatusPage(graphics, data);
		}
	}

	private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Icone e titulo grande, como na referencia
		int iconX = left + MARGIN;
		int iconY = top + 6;
		graphics.fill(iconX, iconY, iconX + 18, iconY + 18, GOLD_DIM);
		graphics.fill(iconX + 1, iconY + 1, iconX + 17, iconY + 17, 0xFF2A1E2E);
		graphics.centeredText(font, Component.literal("念"), iconX + 9, iconY + 5, GOLD);

		graphics.pose().pushMatrix();
		graphics.pose().translate(iconX + 26, iconY + 3);
		graphics.pose().scale(1.5F, 1.5F);
		graphics.text(font, page.title().copy().withStyle(ChatFormatting.BOLD), 0, 0, TEXT, false);
		graphics.pose().popMatrix();

		// Abas no canto direito
		int x = left + panelWidth - MARGIN - 10;
		addClickable(x - 2, top + 8, 14, 14, true, this::onClose);
		graphics.text(font, Component.literal("×"), x + 2, top + 11, hovered(x - 2, top + 8, 14, 14, mouseX, mouseY) ? GOLD : TEXT, false);
		for (int i = Page.values().length - 1; i >= 0; i--) {
			Page tab = Page.values()[i];
			Component label = tab.title();
			int labelWidth = font.width(label);
			x -= labelWidth + 16;
			boolean current = tab == page;
			boolean hover = hovered(x - 4, top + 6, labelWidth + 8, 18, mouseX, mouseY);
			graphics.text(font, label, x, top + 11, current || hover ? GOLD : TEXT, false);
			if (current) {
				graphics.fill(x, top + 22, x + labelWidth, top + 24, GOLD);
			}
			addClickable(x - 4, top + 6, labelWidth + 8, 18, true, () -> page = tab);
		}

		graphics.fill(left + MARGIN, top + HEADER_HEIGHT, left + panelWidth - MARGIN, top + HEADER_HEIGHT + 1, GOLD_DIM);
	}

	private void renderDormant(GuiGraphicsExtractor graphics, NenData data) {
		int x = left + MARGIN + 6;
		int y = top + HEADER_HEIGHT + 20;
		int width = panelWidth - 2 * MARGIN - 12;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.dormant"), left + MARGIN, y - 10, width);
		y = wrapped(graphics, Component.translatable("screen.hxh.nen.dormant_hint"), x, y + 6, width, DIM, 4);
		graphics.text(font, Component.translatable("screen.hxh.nen.meditation", data.meditation(), NenRules.MEDITATION_SECONDS),
				x, y + 8, TEXT, false);
		bar(graphics, x, y + 20, width, 4, data.meditation(), NenRules.MEDITATION_SECONDS, GOLD);
	}

	// ---------------------------------------------------------------- Nivel

	private void renderLevelPage(GuiGraphicsExtractor graphics, NenData data, int mouseX, int mouseY) {
		int columnWidth = (panelWidth - 3 * MARGIN) * 52 / 100;
		int x = left + MARGIN;
		int y = top + HEADER_HEIGHT + 8;

		// Nivel, pontos e progresso para o proximo nivel
		int available = data.pointsAvailable() - pendingTotal();
		Component level = Component.translatable("screen.hxh.nen.level", data.level());
		graphics.text(font, level, x + 4, y, TEXT, false);
		graphics.text(font, Component.translatable("screen.hxh.nen.points", available), x + 16 + font.width(level), y,
				available > 0 ? GOLD : TEXT, false);
		Component progress = data.maxLevel()
				? Component.translatable("screen.hxh.nen.max_level")
				: Component.translatable("screen.hxh.nen.xp_progress", data.xpIntoLevel(), NenRules.xpToNext(data.level()),
						percent(data.xpIntoLevel(), NenRules.xpToNext(data.level())));
		graphics.text(font, progress, x + columnWidth - font.width(progress), y, DIM, false);
		bar(graphics, x + 4, y + 12, columnWidth - 4, 3, data.maxLevel() ? 1 : data.xpIntoLevel(),
				data.maxLevel() ? 1 : NenRules.xpToNext(data.level()), XP_COLOR);

		// Lista de atributos
		y += 22;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.attributes"), x, y, columnWidth);
		y += 14;
		NenStat[] stats = NenStat.values();
		for (int i = 0; i < stats.length; i++) {
			renderStatRow(graphics, data, stats[i], i, x, y + i * ROW_HEIGHT, columnWidth, mouseX, mouseY);
		}

		// Confirmar / Resetar, logo abaixo da lista sem encostar no rodape
		int buttonsY = Math.min(y + stats.length * ROW_HEIGHT + 6, top + panelHeight - 22 - BUTTON_HEIGHT);
		int confirmWidth = columnWidth * 62 / 100;
		boolean hasPending = pendingTotal() > 0;
		button(graphics, Component.translatable("screen.hxh.nen.confirm"), x, buttonsY, confirmWidth, BUTTON_HEIGHT,
				hasPending, mouseX, mouseY, this::confirmStats);
		button(graphics, Component.translatable("screen.hxh.nen.reset"), x + confirmWidth + 6, buttonsY,
				columnWidth - confirmWidth - 6, BUTTON_HEIGHT, hasPending, mouseX, mouseY, pending::clear);

		renderBaseStats(graphics, data, left + 2 * MARGIN + columnWidth, top + HEADER_HEIGHT + 8,
				panelWidth - 3 * MARGIN - columnWidth);
		footer(graphics, Component.translatable("screen.hxh.nen.hint.level"));
	}

	private void renderStatRow(GuiGraphicsExtractor graphics, NenData data, NenStat stat, int index, int x, int y,
			int width, int mouseX, int mouseY) {
		boolean selected = index == selectedStat;
		row(graphics, x, y, width, ROW_HEIGHT - 2, selected);
		addClickable(x, y, width, ROW_HEIGHT - 2, true, () -> selectedStat = index);

		int current = data.statPoints(stat);
		int next = current + pending.getOrDefault(stat, 0);
		graphics.text(font, stat.displayName(), x + 8, y + 4, TEXT, false);

		// Valores colados na direita, deixando espaco para nomes compridos
		int nextX = x + width - 22;
		Component currentText = Component.literal(current + " →");
		graphics.text(font, currentText, nextX - 34 - font.width(currentText), y + 4, TEXT, false);
		Component nextText = Component.literal(next >= stat.maxPoints() ? next + "★" : String.valueOf(next));
		graphics.centeredText(font, nextText, nextX, y + 4, next > current ? GOOD : TEXT);

		if (selected) {
			arrow(graphics, "‹", nextX - 22, y, mouseX, mouseY, () -> adjust(stat, -1));
			arrow(graphics, "›", nextX + 12, y, mouseX, mouseY, () -> adjust(stat, 1));
		}
	}

	private void renderBaseStats(GuiGraphicsExtractor graphics, NenData data, int x, int y, int width) {
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.base_stats"), x, y, width);
		y += 14;
		y = statLine(graphics, "screen.hxh.nen.stat.health", 20, NenStat.VITALITY, data, x, y, width, "");
		y = statLine(graphics, "screen.hxh.nen.stat.damage", 1, NenStat.ATTACK_POWER, data, x, y, width, "");
		y = statLine(graphics, "screen.hxh.nen.stat.attack_speed", 4, NenStat.ATTACK_SPEED, data, x, y, width, "");
		y = statLine(graphics, "screen.hxh.nen.stat.armor", 0, NenStat.PROTECTION, data, x, y, width, "");
		y = statLine(graphics, "screen.hxh.nen.stat.speed", 100, NenStat.AGILITY, data, x, y, width, "%");

		y += 6;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.aura_stats"), x, y, width);
		y += 14;
		int capacityBonus = (int) (pending.getOrDefault(NenStat.AURA_CAPACITY, 0) * NenStat.AURA_CAPACITY.perPoint());
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.max_aura"),
				String.valueOf(data.maxAura()), String.valueOf(data.maxAura() + capacityBonus), x, y, width);
		int regen = data.withActive(Technique.NONE).auraRegen();
		int flowBonus = (int) (pending.getOrDefault(NenStat.AURA_FLOW, 0) * NenStat.AURA_FLOW.perPoint());
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.aura_regen"),
				regen + "/s", (regen + flowBonus) + "/s", x, y, width);

		// Explicacao do atributo selecionado
		NenStat stat = NenStat.values()[selectedStat];
		y += 8;
		graphics.text(font, stat.displayName().copy().withColor(GOLD), x, y, GOLD, false);
		wrapped(graphics, stat.description(), x, y + 11, width, DIM, 4);
	}

	/** Linha "nome  atual → novo" de um status base que soma o bonus do atributo. */
	private int statLine(GuiGraphicsExtractor graphics, String key, double base, NenStat stat, NenData data,
			int x, int y, int width, String suffix) {
		double perPoint = stat == NenStat.AGILITY ? stat.perPoint() * 1000 : stat.perPoint();
		double current = base + data.statPoints(stat) * perPoint;
		double next = current + pending.getOrDefault(stat, 0) * perPoint;
		return valueLine(graphics, Component.translatable(key), number(current) + suffix, number(next) + suffix, x, y, width);
	}

	private int valueLine(GuiGraphicsExtractor graphics, Component name, String current, String next, int x, int y, int width) {
		row(graphics, x, y, width, ROW_HEIGHT - 3, false);
		graphics.text(font, name, x + 6, y + 3, TEXT, false);
		Component values = Component.literal(current + " → ").append(
				Component.literal(next).withColor(next.equals(current) ? TEXT : GOOD));
		graphics.text(font, values, x + width - 6 - font.width(values), y + 3, TEXT, false);
		return y + ROW_HEIGHT - 1;
	}

	// ------------------------------------------------------------- Tecnicas

	private void renderTechniquesPage(GuiGraphicsExtractor graphics, NenData data, int mouseX, int mouseY) {
		int x = left + MARGIN;
		int y = top + HEADER_HEIGHT + 8;

		List<Technique> tabs = Technique.TABS;
		for (int i = 0; i < tabs.size(); i++) {
			Technique technique = tabs.get(i);
			int rowY = y + i * TECH_ROW_HEIGHT;
			boolean selected = i == selectedTechnique;
			row(graphics, x, rowY, TECH_LIST_WIDTH, TECH_ROW_HEIGHT - 2, selected);
			int index = i;
			addClickable(x, rowY, TECH_LIST_WIDTH, TECH_ROW_HEIGHT - 2, true, () -> selectedTechnique = index);

			boolean unlocked = data.isUnlocked(technique);
			boolean active = data.active() == technique;
			// Aplicacoes avancadas ficam recuadas, embaixo do principio
			int indent = isAdvanced(technique) ? 14 : 6;
			graphics.text(font, technique.displayName(), x + indent, rowY + 3, active ? GOLD : unlocked ? TEXT : LOCKED, false);
			Component mark = active ? Component.literal("●") : unlocked ? Component.literal("✔")
					: data.unlockProblem(technique) == null ? Component.literal("!") : Component.literal("✖");
			int markColor = active ? GOLD : unlocked ? GOOD : data.unlockProblem(technique) == null ? GOLD : LOCKED;
			graphics.text(font, mark, x + TECH_LIST_WIDTH - 12, rowY + 3, markColor, false);
		}

		int detailX = x + TECH_LIST_WIDTH + MARGIN;
		renderTechniqueDetails(graphics, data, tabs.get(selectedTechnique), detailX, y,
				left + panelWidth - MARGIN - detailX, mouseX, mouseY);
		footer(graphics, Component.translatable("screen.hxh.nen.hint.techniques"));
	}

	private static boolean isAdvanced(Technique technique) {
		return switch (technique) {
			case TEN, ZETSU, REN, HATSU -> false;
			default -> true;
		};
	}

	private void renderTechniqueDetails(GuiGraphicsExtractor graphics, NenData data, Technique technique,
			int x, int y, int width, int mouseX, int mouseY) {
		boolean unlocked = data.isUnlocked(technique);

		// Nome grande + significado
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(1.5F, 1.5F);
		graphics.text(font, technique.displayName(), 0, 0, GOLD, false);
		graphics.pose().popMatrix();
		int nameWidth = (int) (font.width(technique.displayName()) * 1.5F);
		graphics.text(font, Component.literal("— ").append(technique.meaning()), x + nameWidth + 6, y + 3, DIM, false);
		graphics.text(font, technique.origin(), x, y + 15, DIM, false);

		y = wrapped(graphics, technique.lore(), x, y + 28, width, TEXT, 3);
		y = wrapped(graphics, Component.translatable("screen.hxh.nen.effect", technique.effect()), x, y + 3, width, GOOD, 3);

		// Estado: requisitos para desbloquear, ou treino e botao de ativar
		y += 5;
		if (!unlocked) {
			NenData.UnlockProblem problem = data.unlockProblem(technique);
			Component label = Component.translatable("screen.hxh.nen.unlock", technique.unlockCost());
			int buttonWidth = buttonWidth(label);
			wrapped(graphics, requirements(data, technique), x, y + 1, width - buttonWidth - 6, TEXT, 2);
			button(graphics, label, x + width - buttonWidth, y, buttonWidth, 16, problem == null, mouseX, mouseY,
					() -> ClientPlayNetworking.send(new NenPayloads.UnlockTechnique(technique)));
			y += 24;
		} else {
			Component info = technique == Technique.HATSU
					? hatsuInfo(data)
					: Component.translatable("screen.hxh.nen.training_info", duration(data.training(technique)),
							data.auraDrain(technique) > 0
									? Component.translatable("screen.hxh.nen.drain", data.auraDrain(technique))
									: Component.translatable("screen.hxh.nen.no_drain"));
			Component label = Component.translatable(data.active() == technique
					? "screen.hxh.nen.deactivate" : "screen.hxh.nen.activate");
			int buttonWidth = technique.activatable() ? buttonWidth(label) : 0;
			wrapped(graphics, info, x, y + 1, width - buttonWidth - 6, TEXT, 2);
			if (technique.activatable()) {
				button(graphics, label, x + width - buttonWidth, y, buttonWidth, 16, true, mouseX, mouseY,
						() -> ClientPlayNetworking.send(new NenPayloads.ToggleTechnique(technique)));
			}
			y += 24;
		}

		// Funcionalidades
		List<NenUpgrade> upgrades = NenUpgrade.of(technique);
		if (upgrades.isEmpty()) {
			return;
		}
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.features"), x, y, width);
		y += 13;
		for (NenUpgrade upgrade : upgrades) {
			y = renderUpgrade(graphics, data, upgrade, x, y, width, mouseX, mouseY);
		}
	}

	private int renderUpgrade(GuiGraphicsExtractor graphics, NenData data, NenUpgrade upgrade, int x, int y, int width,
			int mouseX, int mouseY) {
		boolean owned = data.has(upgrade);
		NenData.UnlockProblem problem = data.upgradeProblem(upgrade);
		Component label = Component.translatable("screen.hxh.nen.unlock", upgrade.cost());
		int buttonWidth = buttonWidth(label);
		graphics.text(font, upgrade.displayName(), x, y, owned ? GOOD : TEXT, false);
		Component requirement = Component.translatable("screen.hxh.nen.upgrade_requirement", upgrade.minLevel(), upgrade.cost())
				.withColor(data.level() >= upgrade.minLevel() ? DIM : BAD);
		graphics.text(font, requirement, x + font.width(upgrade.displayName()) + 6, y, DIM, false);
		if (owned) {
			graphics.text(font, Component.translatable("screen.hxh.nen.owned"), x + width - buttonWidth + 8, y + 2, GOOD, false);
		} else {
			button(graphics, label, x + width - buttonWidth, y - 2, buttonWidth, 14, problem == null, mouseX, mouseY,
					() -> ClientPlayNetworking.send(new NenPayloads.BuyUpgrade(upgrade)));
		}
		return wrapped(graphics, upgrade.description(), x, y + 13, width, DIM, 2) + 3;
	}

	/** "Requisitos: Nivel 20 · Ten, Ren · 4 pontos", com o que falta em vermelho. */
	private Component requirements(NenData data, Technique technique) {
		MutableComponent text = Component.translatable("screen.hxh.nen.requirements").copy();
		text.append(Component.translatable("screen.hxh.nen.level", technique.minLevel())
				.withColor(data.level() >= technique.minLevel() ? GOOD : BAD));
		for (Technique prerequisite : technique.prerequisites()) {
			text.append(Component.literal(" · "));
			text.append(prerequisite.displayName().copy().withColor(data.isUnlocked(prerequisite) ? GOOD : BAD));
		}
		if (technique == Technique.HATSU) {
			text.append(Component.literal(" · "));
			text.append(Component.translatable("screen.hxh.nen.requires_type").withColor(data.revealed() ? GOOD : BAD));
		}
		text.append(Component.literal(" · "));
		text.append(Component.translatable("screen.hxh.nen.cost", technique.unlockCost())
				.withColor(data.pointsAvailable() >= technique.unlockCost() ? GOOD : BAD));
		return text;
	}

	private Component hatsuInfo(NenData data) {
		return Component.translatable("screen.hxh.nen.hatsu_info", data.type().displayName());
	}

	// --------------------------------------------------------------- Status

	private void renderStatusPage(GuiGraphicsExtractor graphics, NenData data) {
		int columnWidth = (panelWidth - 3 * MARGIN) / 2;
		int x = left + MARGIN;
		int y = top + HEADER_HEIGHT + 8;

		sectionTitle(graphics, Component.translatable("screen.hxh.nen.records"), x, y, columnWidth);
		y += 14;
		Component type = data.revealed() ? data.type().displayName() : Component.literal("???");
		y = infoLine(graphics, "screen.hxh.nen.info.type", type, x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.divination", Component.literal(data.divination() + "%"), x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.level", Component.literal(data.level() + " / " + NenRules.MAX_LEVEL), x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.total_xp", Component.literal(String.valueOf(data.xp())), x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.points",
				Component.literal(data.pointsAvailable() + " / " + NenRules.pointsEarned(data.level())), x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.aura", Component.literal(data.aura() + " / " + data.maxAura()), x, y, columnWidth);
		y = infoLine(graphics, "screen.hxh.nen.info.active",
				data.active() == Technique.NONE ? Component.translatable("nen.hxh.technique.none") : data.active().displayName(),
				x, y, columnWidth);
		int unlockedCount = (int) Technique.TABS.stream().filter(data::isUnlocked).count();
		infoLine(graphics, "screen.hxh.nen.info.techniques", Component.literal(unlockedCount + " / " + Technique.TABS.size()),
				x, y, columnWidth);

		int rightX = x + columnWidth + MARGIN;
		int rightY = top + HEADER_HEIGHT + 8;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.training"), rightX, rightY, columnWidth);
		rightY += 14;
		List<Technique> trained = Technique.TABS.stream().filter(data::isUnlocked).filter(Technique::activatable)
				.collect(Collectors.toList());
		for (Technique technique : trained) {
			rightY = infoLine(graphics, technique.displayName(), Component.literal(duration(data.training(technique))),
					rightX, rightY, columnWidth);
		}
		footer(graphics, Component.translatable("screen.hxh.nen.hint.status"));
	}

	private int infoLine(GuiGraphicsExtractor graphics, String key, Component value, int x, int y, int width) {
		return infoLine(graphics, Component.translatable(key), value, x, y, width);
	}

	private int infoLine(GuiGraphicsExtractor graphics, Component name, Component value, int x, int y, int width) {
		row(graphics, x, y, width, ROW_HEIGHT - 3, false);
		graphics.text(font, name, x + 6, y + 3, TEXT, false);
		graphics.text(font, value, x + width - 6 - font.width(value), y + 3, TEXT, false);
		return y + ROW_HEIGHT - 1;
	}

	// ------------------------------------------------------------- Pecas

	private void sectionTitle(GuiGraphicsExtractor graphics, Component title, int x, int y, int width) {
		graphics.text(font, Component.literal("✦ ").append(title.copy().withStyle(ChatFormatting.BOLD)), x, y, GOLD, false);
		graphics.fill(x, y + 10, x + width, y + 11, GOLD_DIM);
	}

	private void row(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected) {
		graphics.fillGradient(x, y, x + width, y + height,
				selected ? ROW_SELECTED_TOP : ROW_TOP, selected ? ROW_SELECTED_BOTTOM : ROW_BOTTOM);
		if (selected) {
			graphics.fill(x, y, x + 2, y + height, GOLD);
		}
	}

	private void button(GuiGraphicsExtractor graphics, Component label, int x, int y, int width, int height,
			boolean enabled, int mouseX, int mouseY, Runnable action) {
		boolean hover = enabled && hovered(x, y, width, height, mouseX, mouseY);
		int border = !enabled ? LOCKED : hover ? GOLD : TEXT;
		graphics.fill(x, y, x + width, y + height, border);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xFF2A2418 : 0xFF0B0C11);
		graphics.centeredText(font, label, x + width / 2, y + (height - 8) / 2, enabled ? (hover ? GOLD : TEXT) : LOCKED);
		addClickable(x, y, width, height, enabled, action);
	}

	/** Largura do botao para caber o texto, com um minimo para os botoes ficarem alinhados. */
	private int buttonWidth(Component label) {
		return Math.max(64, font.width(label) + 12);
	}

	private void arrow(GuiGraphicsExtractor graphics, String symbol, int x, int y, int mouseX, int mouseY, Runnable action) {
		boolean hover = hovered(x - 4, y, 14, ROW_HEIGHT - 2, mouseX, mouseY);
		graphics.text(font, Component.literal(symbol), x, y + 4, hover ? GOLD : DIM, false);
		addClickable(x - 4, y, 14, ROW_HEIGHT - 2, true, action);
	}

	private void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int value, int max, int color) {
		int filled = max > 0 ? width * Math.min(value, max) / max : 0;
		graphics.fill(x, y, x + width, y + height, BAR_BACK);
		graphics.fill(x, y, x + filled, y + height, color);
	}

	/** Texto com quebra de linha; devolve o y logo abaixo da ultima linha. */
	private int wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color, int maxLines) {
		List<FormattedCharSequence> lines = font.split(text, width);
		for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
			graphics.text(font, lines.get(i), x, y, color, false);
			y += font.lineHeight + 1;
		}
		return y;
	}

	private void footer(GuiGraphicsExtractor graphics, Component hint) {
		int y = top + panelHeight - 13;
		graphics.fill(left + MARGIN, y - 4, left + panelWidth - MARGIN, y - 3, GOLD_DIM);
		graphics.text(font, hint, left + MARGIN, y, DIM, false);
	}

	private void addClickable(int x, int y, int width, int height, boolean enabled, Runnable action) {
		clickables.add(new Clickable(x, y, width, height, enabled, action));
	}

	private static boolean hovered(int x, int y, int width, int height, int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static String number(double value) {
		if (value == Math.rint(value)) {
			return String.valueOf((long) value);
		}
		return String.format("%.2f", value).replaceAll("0+$", "");
	}

	private static int percent(int value, int max) {
		return max > 0 ? value * 100 / max : 100;
	}

	private static String duration(int seconds) {
		if (seconds >= 3600) {
			return String.format("%dh %02dm", seconds / 3600, seconds % 3600 / 60);
		}
		return String.format("%dm %02ds", seconds / 60, seconds % 60);
	}

	// ======================================================================
	// Pontos de atributo
	// ======================================================================

	private int pendingTotal() {
		return pending.values().stream().mapToInt(Integer::intValue).sum();
	}

	private void adjust(NenStat stat, int delta) {
		NenData data = data();
		int current = pending.getOrDefault(stat, 0);
		int room = stat.maxPoints() - data.statPoints(stat) - current;
		int free = data.pointsAvailable() - pendingTotal();
		int change = delta > 0 ? Math.min(delta, Math.min(room, free)) : Math.max(delta, -current);
		if (change != 0) {
			pending.put(stat, current + change);
		}
	}

	private void confirmStats() {
		Map<NenStat, Integer> points = new EnumMap<>(NenStat.class);
		pending.forEach((stat, amount) -> {
			if (amount > 0) {
				points.put(stat, amount);
			}
		});
		if (!points.isEmpty()) {
			ClientPlayNetworking.send(new NenPayloads.AllocateStats(Map.copyOf(points)));
		}
		pending.clear();
	}

	// ======================================================================
	// Entrada
	// ======================================================================

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			// Percorre de tras para frente: o que foi desenhado por ultimo fica por cima
			for (int i = clickables.size() - 1; i >= 0; i--) {
				Clickable clickable = clickables.get(i);
				if (clickable.contains(event.x(), event.y())) {
					if (clickable.enabled()) {
						clickable.action().run();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (HxHModClient.OPEN_NEN_PANEL.matches(event)) {
			onClose();
			return true;
		}
		int key = event.key();
		if (key == InputConstants.KEY_TAB) {
			int step = event.hasShiftDown() ? Page.values().length - 1 : 1;
			page = Page.values()[(page.ordinal() + step) % Page.values().length];
			return true;
		}
		if (page == Page.LEVEL && handleLevelKey(key, event.hasShiftDown())) {
			return true;
		}
		if (page == Page.TECHNIQUES && handleTechniqueKey(key)) {
			return true;
		}
		return super.keyPressed(event);
	}

	private boolean handleLevelKey(int key, boolean shift) {
		int count = NenStat.values().length;
		NenStat stat = NenStat.values()[selectedStat];
		switch (key) {
			case InputConstants.KEY_UP -> selectedStat = (selectedStat + count - 1) % count;
			case InputConstants.KEY_DOWN -> selectedStat = (selectedStat + 1) % count;
			case InputConstants.KEY_LEFT -> adjust(stat, shift ? -SHIFT_STEP : -1);
			case InputConstants.KEY_RIGHT -> adjust(stat, shift ? SHIFT_STEP : 1);
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> confirmStats();
			default -> {
				return false;
			}
		}
		return true;
	}

	private boolean handleTechniqueKey(int key) {
		int count = Technique.TABS.size();
		Technique technique = Technique.TABS.get(selectedTechnique);
		switch (key) {
			case InputConstants.KEY_UP -> selectedTechnique = (selectedTechnique + count - 1) % count;
			case InputConstants.KEY_DOWN -> selectedTechnique = (selectedTechnique + 1) % count;
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				NenData data = data();
				if (!data.isUnlocked(technique)) {
					ClientPlayNetworking.send(new NenPayloads.UnlockTechnique(technique));
				} else if (technique.activatable()) {
					ClientPlayNetworking.send(new NenPayloads.ToggleTechnique(technique));
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}
}
