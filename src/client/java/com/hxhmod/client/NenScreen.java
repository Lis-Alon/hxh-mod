package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenBonuses;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

/**
 * Painel do Nen (tecla N), com quatro abas: Nivel (proficiencia e pontos de atributo),
 * Tecnicas (uma aba por tecnica), Arvore (como as tecnicas se ligam e a proficiencia de cada uma)
 * e Status (registro do treino).
 * Tudo e desenhado a mao; os botoes sao recriados a cada quadro e conferidos no clique.
 * Mouse: clique seleciona e aperta botoes, botao direito tira ponto, a rodinha ajusta ou troca a selecao.
 * Enquanto o painel esta aberto o personagem fica sentado meditando.
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
	// Barrinha de progresso de cada atributo
	private static final int ATTRIBUTE_ROW_STEP = 15;
	private static final int ATTRIBUTE_SEGMENTS = 10;
	private static final int ATTRIBUTE_SEGMENT_STEP = 4;
	static final int PREVIEW_WIDTH = 84;
	private static final int DETAIL_SCROLL_STEP = 12;
	private static final int GOLD = 0xFFD8B46A;
	private static final int GOLD_DIM = 0xFF6E5A35;
	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF8E8A80;
	private static final int LOCKED = 0xFF5E5C58;
	private static final int GOOD = 0xFF8FD46A;
	private static final int BAD = 0xFFD46A6A;
	private static final int BAR_BACK = 0xFF262A36;
	private static final int XP_COLOR = 0xFFD8B46A;
	private static final FontDescription SERIF = new FontDescription.Resource(HxHMod.id("serif"));

	private enum Page {
		LEVEL("screen.hxh.nen.page.level"),
		TECHNIQUES("screen.hxh.nen.page.techniques"),
		TREE("screen.hxh.nen.page.tree"),
		STATUS("screen.hxh.nen.page.status"),
		AURA("screen.hxh.nen.page.aura");

		private final String key;

		Page(String key) {
			this.key = key;
		}

		Component title() {
			return Component.translatable(key);
		}
	}

	/** Area clicavel desenhada neste quadro; {@code rightAction} roda com o botao direito (pode ser null). */
	private record Clickable(int x, int y, int width, int height, boolean enabled, Runnable action, Runnable rightAction) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private final List<Clickable> clickables = new ArrayList<>();
	/** Tecnicas nao tem aba: abrem pelo botao da arvore e pelos nos dela. */
	private static final List<Page> TABS = List.of(Page.LEVEL, Page.TREE, Page.STATUS, Page.AURA);

	private final NenTreePage tree = new NenTreePage(this);
	private final NenAuraPage aura = new NenAuraPage(this);
	private Page page = Page.LEVEL;
	private int selectedStat;
	private int selectedTechnique;
	// Linha embaixo do mouse neste quadro (-1 se nenhuma), usada pela rodinha
	private int hoveredTechnique = -1;
	// Rolagem dos detalhes da tecnica e area visivel (o que fica fora nao recebe clique)
	private int detailScroll;
	private int maxDetailScroll;
	private int scrolledTechnique = -1;
	private int detailsLeft = Integer.MAX_VALUE;
	private int clipTop = Integer.MIN_VALUE;
	private int clipBottom = Integer.MAX_VALUE;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	/** Posicao do clique em andamento. */
	private double pressX;

	public NenScreen() {
		super(Component.translatable("screen.hxh.nen.title"));
	}

	@Override
	protected void init() {
		layoutPanel();
		sendPanelOpen(true);
	}

	/** A arvore usa a tela toda; as outras abas ficam num painel menor no centro. */
	private void layoutPanel() {
		panelWidth = page == Page.TREE ? width - 8 : Math.min(MAX_WIDTH, width - 8);
		panelHeight = page == Page.TREE ? height - 8 : Math.min(MAX_HEIGHT, height - 8);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
	}

	private void setPage(Page newPage) {
		page = newPage;
		tree.disarm();
		layoutPanel();
	}

	/** Abre a tecnica na aba Tecnicas (clique na arvore). */
	void openTechnique(Technique technique) {
		selectedTechnique = Technique.TABS.indexOf(technique);
		setPage(Page.TECHNIQUES);
	}

	@Override
	public void removed() {
		// Fechou com uma barra ainda presa: a cor na tela e a que vale
		aura.release();
		sendPanelOpen(false);
		super.removed();
	}

	/** Avisa o servidor para sentar (ou levantar) o personagem. */
	private static void sendPanelOpen(boolean open) {
		if (ClientPlayNetworking.canSend(NenPayloads.PanelOpen.TYPE)) {
			ClientPlayNetworking.send(new NenPayloads.PanelOpen(open));
		}
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

	void openTechniques() {
		setPage(Page.TECHNIQUES);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		StarrySky.render(graphics, width, height, minecraft.getWindow().getGuiScale(), Util.getMillis());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		clickables.clear();
		hoveredTechnique = -1;
		NenData data = data();

		renderHeader(graphics, mouseX, mouseY);
		if (!data.awakened()) {
			renderDormant(graphics, data, mouseX, mouseY);
			return;
		}
		switch (page) {
			case LEVEL -> renderLevelPage(graphics, data, mouseX, mouseY);
			case TECHNIQUES -> renderTechniquesPage(graphics, data, mouseX, mouseY);
			case TREE -> tree.render(graphics, font, data, left + 2, top + HEADER_HEIGHT + 3, panelWidth - 4,
					panelHeight - HEADER_HEIGHT - 6, mouseX, mouseY);
			case STATUS -> renderStatusPage(graphics, data, mouseX, mouseY);
			case AURA -> aura.render(graphics, font, left + MARGIN, top + HEADER_HEIGHT + 8, panelWidth - 2 * MARGIN,
					panelHeight - HEADER_HEIGHT - 30, mouseX, mouseY);
		}
	}

	private void renderHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Icone e titulo grande, como na referencia
		int iconX = left + MARGIN;
		int iconY = top + 6;
		NenArt.shared(graphics, "emblem", iconX, iconY);
		if (!NenArt.text(graphics, "title_" + page.name().toLowerCase(Locale.ROOT), iconX + 26, top + 4)) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(iconX + 26, iconY + 3);
			graphics.pose().scale(1.5F, 1.5F);
			graphics.text(font, serif(page.title()), 0, 0, TEXT, false);
			graphics.pose().popMatrix();
		}

		// Abas no canto direito
		int x = left + panelWidth - MARGIN - 10;
		addClickable(x - 2, top + 8, 14, 14, true, this::onClose);
		NenArt.shared(graphics, hovered(x - 2, top + 8, 14, 14, mouseX, mouseY) ? "close_on" : "close", x + 1, top + 10);
		for (int i = TABS.size() - 1; i >= 0; i--) {
			Page tab = TABS.get(i);
			Component label = serif(tab.title());
			String tabKey = "tab_" + tab.name().toLowerCase(Locale.ROOT);
			int labelWidth = NenArt.width(tabKey, font.width(label));
			x -= labelWidth + 16;
			boolean current = tab == page || (tab == Page.TREE && page == Page.TECHNIQUES);
			boolean hover = hovered(x - 4, top + 6, labelWidth + 8, 18, mouseX, mouseY);
			if (!NenArt.text(graphics, tabKey + (current || hover ? "_on" : ""), x, top + 8)) {
				graphics.text(font, label, x, top + 11, current || hover ? GOLD : TEXT, false);
			}
			if (current) {
				graphics.fill(x, top + 22, x + labelWidth, top + 23, GOLD);
			}
			addClickable(x - 4, top + 6, labelWidth + 8, 18, true, () -> setPage(tab));
		}

		graphics.fill(left + MARGIN, top + HEADER_HEIGHT, left + panelWidth - MARGIN, top + HEADER_HEIGHT + 1, GOLD_DIM);
	}

	private void renderDormant(GuiGraphicsExtractor graphics, NenData data, int mouseX, int mouseY) {
		int x = left + MARGIN + 6;
		int y = top + HEADER_HEIGHT + 20;
		int width = panelWidth - 3 * MARGIN - 12 - PREVIEW_WIDTH;
		renderPreview(graphics, left + panelWidth - MARGIN - PREVIEW_WIDTH, top + HEADER_HEIGHT + 8, PREVIEW_WIDTH,
				panelHeight - HEADER_HEIGHT - 16, mouseX, mouseY);
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.dormant"), left + MARGIN, y - 10, width);
		y = wrapped(graphics, Component.translatable("screen.hxh.nen.dormant_hint"), x, y + 6, width, DIM, 4);
		graphics.text(font, Component.translatable("screen.hxh.nen.meditation", data.meditation(), NenRules.MEDITATION_SECONDS),
				x, y + 8, TEXT, false);
		bar(graphics, x, y + 20, width, 4, data.meditation(), NenRules.MEDITATION_SECONDS, GOLD);
	}

	// ---------------------------------------------------------------- Nivel

	private void renderLevelPage(GuiGraphicsExtractor graphics, NenData data, int mouseX, int mouseY) {
		int columnWidth = (panelWidth - 3 * MARGIN) * 60 / 100;
		int x = left + MARGIN;
		int y = top + HEADER_HEIGHT + 8;

		// Nivel, pontos (para as funcionalidades) e progresso para o proximo nivel
		int available = data.pointsAvailable();
		Component level = panelFont(Component.translatable("screen.hxh.nen.level", data.level()));
		graphics.text(font, level, x + 4, y, TEXT, false);
		graphics.text(font, panelFont(Component.translatable("screen.hxh.nen.points", available)), x + 16 + font.width(level), y,
				available > 0 ? GOLD : TEXT, false);
		Component progress = panelFont(data.maxLevel()
				? Component.translatable("screen.hxh.nen.max_level")
				: Component.translatable("screen.hxh.nen.xp_progress", data.xpIntoLevel(), NenRules.xpToNext(data.level()),
						percent(data.xpIntoLevel(), NenRules.xpToNext(data.level()))));
		graphics.text(font, progress, x + columnWidth - font.width(progress), y, DIM, false);
		bar(graphics, x + 4, y + 12, columnWidth - 4, 3, data.maxLevel() ? 1 : data.xpIntoLevel(),
				data.maxLevel() ? 1 : NenRules.xpToNext(data.level()), XP_COLOR);

		// Atributos: sobem sozinhos com o treino
		y += 22;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.attributes"), x, y, columnWidth);
		y += 14;
		NenStat[] stats = NenStat.values();
		for (int i = 0; i < stats.length; i++) {
			renderStatRow(graphics, data, stats[i], i, x, y + i * ATTRIBUTE_ROW_STEP, columnWidth, mouseX, mouseY);
		}
		wrapped(graphics, panelFont(stats[selectedStat].description()), x + 4, y + stats.length * ATTRIBUTE_ROW_STEP + 2, columnWidth - 8, DIM, 3);

		renderBaseStats(graphics, data, left + 2 * MARGIN + columnWidth, top + HEADER_HEIGHT + 8,
				panelWidth - 3 * MARGIN - columnWidth);
		footer(graphics, Component.translatable("screen.hxh.nen.hint.level"));
	}

	/** "Vitalidade   Niv. 40 ▮▮▮▮▮▮▮▮▯▯ 82%". */
	private void renderStatRow(GuiGraphicsExtractor graphics, NenData data, NenStat stat, int index, int x, int y,
			int width, int mouseX, int mouseY) {
		boolean selected = index == selectedStat;
		boolean hover = hovered(x, y, width, ATTRIBUTE_ROW_STEP - 2, mouseX, mouseY);
		row(graphics, x, y, width, ATTRIBUTE_ROW_STEP - 2, selected, hover);
		addClickable(x, y, width, ATTRIBUTE_ROW_STEP - 2, true, () -> selectedStat = index);
		graphics.text(font, panelFont(stat.displayName()), x + 8, y + 3, TEXT, false);

		int level = data.attributeLevel(stat);
		int progress = data.attributeProgress(stat);
		boolean maxed = level >= NenRules.MAX_ATTRIBUTE;
		Component percent = Component.literal(progress + "%");
		int percentRight = x + width - 6;
		graphics.text(font, percent, percentRight - font.width(percent), y + 3, DIM, false);

		int barWidth = ATTRIBUTE_SEGMENTS * ATTRIBUTE_SEGMENT_STEP - 1;
		int barX = percentRight - font.width("100%") - 4 - barWidth;
		for (int i = 0; i < ATTRIBUTE_SEGMENTS; i++) {
			boolean filled = progress >= (i + 1) * 100 / ATTRIBUTE_SEGMENTS;
			int segmentX = barX + i * ATTRIBUTE_SEGMENT_STEP;
			graphics.fill(segmentX, y + 4, segmentX + ATTRIBUTE_SEGMENT_STEP - 1, y + 9, filled ? (maxed ? GOLD : GOLD_DIM) : BAR_BACK);
		}

		Component levelText = panelFont(Component.translatable("screen.hxh.nen.attribute_level", level));
		graphics.text(font, levelText, barX - 5 - font.width(levelText), y + 3, maxed ? GOLD : TEXT, false);
	}

	private void renderBaseStats(GuiGraphicsExtractor graphics, NenData data, int x, int y, int width) {
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.base_stats"), x, y, width);
		y += 14;
		// Status internos do Nen, ja com as tecnicas ligadas
		NenBonuses now = NenBonuses.of(data);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.health"), number(20 + now.maxHealth()), x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.damage"), damage(now), x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.attack_speed"), signedPercent(now.attackSpeed()), x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.defense"), percent(1 - now.damageTaken()), x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.speed"), percent(1 + now.moveSpeed()), x, y, width);

		y += 6;
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.aura_stats"), x, y, width);
		y += 14;
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.max_aura"), String.valueOf(data.maxAura()), x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.aura_regen"),
				number(data.withActive(Technique.NONE).auraRegen()) + "/s", x, y, width);
		y = valueLine(graphics, Component.translatable("screen.hxh.nen.stat.upkeep"),
				signedPercent(-data.attributeLevel(NenStat.AURA_FLOW) * NenRules.FLOW_UPKEEP), x, y, width);
		valueLine(graphics, Component.translatable("screen.hxh.nen.stat.en_range"),
				Component.translatable("screen.hxh.nen.blocks", Math.round(data.enMaxSize())).getString(), x, y, width);
	}

	/** "+1,5 +25%": dano fixo dos pontos e bonus em porcentagem das tecnicas. */
	private static String damage(NenBonuses bonuses) {
		String flat = "+" + number(bonuses.flatDamage());
		return bonuses.damageBonus() > 0 ? flat + " " + signedPercent(bonuses.damageBonus()) : flat;
	}

	private static String percent(double fraction) {
		return Math.round(fraction * 100) + "%";
	}

	private static String signedPercent(double fraction) {
		return (fraction >= 0 ? "+" : "") + percent(fraction);
	}

	private int valueLine(GuiGraphicsExtractor graphics, Component name, String value, int x, int y, int width) {
		row(graphics, x, y, width, ROW_HEIGHT - 3, false);
		graphics.text(font, panelFont(name), x + 6, y + 3, TEXT, false);
		Component shown = Component.literal(value);
		graphics.text(font, shown, x + width - 6 - font.width(shown), y + 3, TEXT, false);
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
			boolean hover = hovered(x, rowY, TECH_LIST_WIDTH, TECH_ROW_HEIGHT - 2, mouseX, mouseY);
			if (hover) {
				hoveredTechnique = i;
			}
			row(graphics, x, rowY, TECH_LIST_WIDTH, TECH_ROW_HEIGHT - 2, selected, hover);
			int index = i;
			addClickable(x, rowY, TECH_LIST_WIDTH, TECH_ROW_HEIGHT - 2, true, () -> selectedTechnique = index);

			boolean unlocked = data.isUnlocked(technique);
			boolean active = data.isActive(technique);
			// Aplicacoes avancadas ficam recuadas, embaixo do principio
			int indent = isAdvanced(technique) ? 14 : 6;
			graphics.text(font, panelFont(technique.displayName()), x + indent, rowY + 3, active ? GOLD : unlocked ? TEXT : LOCKED, false);
			Component mark = active ? Component.literal("●") : unlocked ? Component.literal("✔") : Component.literal("✖");
			int markColor = active ? GOLD : unlocked ? GOOD : LOCKED;
			graphics.text(font, mark, x + TECH_LIST_WIDTH - 12, rowY + 3, markColor, false);
		}

		// Detalhes com rolagem (rodinha em cima deles), cortados acima do rodape
		int detailX = x + TECH_LIST_WIDTH + MARGIN;
		int detailRight = left + panelWidth - MARGIN;
		int viewBottom = top + panelHeight - 20;
		detailsLeft = detailX;
		if (selectedTechnique != scrolledTechnique) {
			scrolledTechnique = selectedTechnique;
			detailScroll = 0;
		}
		clipTop = y - 2;
		clipBottom = viewBottom;
		graphics.enableScissor(detailX - 2, clipTop, detailRight + 2, clipBottom);
		int end = renderTechniqueDetails(graphics, data, tabs.get(selectedTechnique), detailX, y - detailScroll,
				detailRight - detailX, mouseX, mouseY);
		graphics.disableScissor();
		clipTop = Integer.MIN_VALUE;
		clipBottom = Integer.MAX_VALUE;

		int contentHeight = end + detailScroll - y;
		maxDetailScroll = Math.max(0, contentHeight - (viewBottom - y));
		detailScroll = Math.min(detailScroll, maxDetailScroll);
		if (maxDetailScroll > 0) {
			// Barrinha mostrando onde esta a rolagem
			int trackHeight = viewBottom - y;
			int thumbHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
			int thumbY = y + (trackHeight - thumbHeight) * detailScroll / maxDetailScroll;
			graphics.fill(detailRight + 3, y, detailRight + 5, viewBottom, BAR_BACK);
			graphics.fill(detailRight + 3, thumbY, detailRight + 5, thumbY + thumbHeight, GOLD_DIM);
		}
		footer(graphics, Component.translatable("screen.hxh.nen.hint.techniques"));
	}

	private static boolean isAdvanced(Technique technique) {
		return switch (technique) {
			case TEN, ZETSU, REN, HATSU -> false;
			default -> true;
		};
	}

	/** Desenha os detalhes da tecnica e devolve o y logo abaixo do conteudo. */
	private int renderTechniqueDetails(GuiGraphicsExtractor graphics, NenData data, Technique technique,
			int x, int y, int width, int mouseX, int mouseY) {
		boolean unlocked = data.isUnlocked(technique);

		// Nome grande + significado
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(1.5F, 1.5F);
		Component name = panelFont(technique.displayName());
		graphics.text(font, name, 0, 0, GOLD, false);
		graphics.pose().popMatrix();
		int nameWidth = (int) (font.width(name) * 1.5F);
		graphics.text(font, panelFont(Component.literal("— ").append(technique.meaning())), x + nameWidth + 6, y + 3, DIM, false);
		graphics.text(font, panelFont(technique.origin()), x, y + 15, DIM, false);

		// A Cinzel ocupa mais espaco: limite folgado, porque os detalhes rolam
		y = wrapped(graphics, panelFont(technique.lore()), x, y + 28, width, TEXT, 8);
		y = wrapped(graphics, panelFont(Component.translatable("screen.hxh.nen.effect", technique.effect())), x, y + 3, width, GOOD, 8);

		// Estado: proficiencia que falta para aprender, ou treino e botao de ativar
		y += 5;
		if (!unlocked) {
			y = wrapped(graphics, panelFont(requirements(data, technique)), x, y + 1, width, TEXT, 4) + 8;
		} else {
			Component info = Component.translatable("screen.hxh.nen.training_info", data.mastery(technique),
					data.masteryProgress(technique),
					data.auraDrain(technique) > 0
							? Component.translatable("screen.hxh.nen.drain", Math.round(data.auraDrain(technique) * 10) / 10.0)
							: Component.translatable("screen.hxh.nen.no_drain"));
			Component label = Component.translatable(data.isActive(technique)
					? "screen.hxh.nen.deactivate" : "screen.hxh.nen.activate");
			int buttonWidth = technique.activatable() ? buttonWidth(label) : 0;
			int infoEnd = wrapped(graphics, panelFont(info), x, y + 1, width - buttonWidth - 6, TEXT, 4);
			if (technique.activatable()) {
				button(graphics, label, x + width - buttonWidth, y, buttonWidth, 16, true, mouseX, mouseY,
						() -> ClientPlayNetworking.send(new NenPayloads.ToggleTechnique(technique)));
			}
			y = Math.max(y + 24, infoEnd + 5);
			if (technique.maxIntensity() > 1) {
				Component intensity = technique == Technique.EN
						? Component.translatable("nen.hxh.en.size", Math.round(data.enSize()))
						: Component.translatable("nen.hxh.intensity", technique.displayName(), data.intensityOf(technique), technique.maxIntensity());
				Component change = Component.translatable("screen.hxh.nen.change_intensity");
				int changeWidth = buttonWidth(change);
				graphics.text(font, panelFont(intensity), x, y + 4, TEXT, false);
				button(graphics, change, x + width - changeWidth, y, changeWidth, 16, true, mouseX, mouseY,
						() -> ClientPlayNetworking.send(new NenPayloads.CycleIntensity(technique)));
				y += 22;
			}
		}

		// Funcionalidades
		// Passivas exclusivas de outro tipo (ou de um tipo ainda nao revelado) ficam escondidas
		List<NenUpgrade> upgrades = NenUpgrade.of(technique).stream()
				.filter(upgrade -> upgrade.availableTo(data) || data.has(upgrade)).toList();
		if (upgrades.isEmpty()) {
			return y;
		}
		sectionTitle(graphics, Component.translatable("screen.hxh.nen.features"), x, y, width);
		y += 16;
		for (NenUpgrade upgrade : upgrades) {
			y = renderUpgrade(graphics, data, upgrade, x, y, width, mouseX, mouseY);
		}
		return y;
	}

	private int renderUpgrade(GuiGraphicsExtractor graphics, NenData data, NenUpgrade upgrade, int x, int y, int width,
			int mouseX, int mouseY) {
		boolean owned = data.has(upgrade);
		NenData.UnlockProblem problem = data.upgradeProblem(upgrade);
		Component label = Component.translatable("screen.hxh.nen.unlock", upgrade.cost());
		int buttonWidth = buttonWidth(label);
		Component upgradeName = panelFont(upgrade.displayName());
		graphics.text(font, upgradeName, x, y, owned ? GOOD : TEXT, false);
		Component requirement = panelFont(Component.translatable("screen.hxh.nen.upgrade_requirement", upgrade.minLevel(), upgrade.cost())
				.withColor(data.level() >= upgrade.minLevel() ? DIM : BAD));
		graphics.text(font, requirement, x + font.width(upgradeName) + 6, y, DIM, false);
		if (owned) {
			graphics.text(font, panelFont(Component.translatable("screen.hxh.nen.owned")), x + width - buttonWidth + 8, y + 2, GOOD, false);
		} else {
			button(graphics, label, x + width - buttonWidth, y - 3, buttonWidth, 16, problem == null, mouseX, mouseY,
					() -> ClientPlayNetworking.send(new NenPayloads.BuyUpgrade(upgrade)));
		}
		return wrapped(graphics, panelFont(upgrade.description()), x, y + 14, width, DIM, 4) + 5;
	}

	/**
	 * "Requisitos: Ten 12/20% · Ren 45/40%", com o que falta em vermelho.
	 * A tecnica e aprendida sozinha quando todos ficam verdes.
	 */
	Component requirements(NenData data, Technique technique) {
		if (technique.requirements().isEmpty()) {
			return Component.translatable("screen.hxh.nen.base_technique").withColor(DIM);
		}
		MutableComponent text = Component.translatable("screen.hxh.nen.requirements").copy();
		boolean first = true;
		for (Technique.Requirement requirement : technique.requirements()) {
			if (!first) {
				text.append(Component.literal(" · "));
			}
			first = false;
			int mastery = data.mastery(requirement.technique());
			text.append(Component.translatable("screen.hxh.nen.requirement", requirement.technique().displayName(),
					mastery, requirement.mastery()).withColor(mastery >= requirement.mastery() ? GOOD : BAD));
		}
		if (data.blocked().contains(technique)) {
			text.append(Component.literal(" · "));
			text.append(Component.translatable("screen.hxh.nen.blocked").withColor(BAD));
		}
		return text;
	}

	// --------------------------------------------------------------- Status

	private void renderStatusPage(GuiGraphicsExtractor graphics, NenData data, int mouseX, int mouseY) {
		int columnWidth = (panelWidth - 4 * MARGIN - PREVIEW_WIDTH) / 2;
		renderPreview(graphics, left + panelWidth - MARGIN - PREVIEW_WIDTH, top + HEADER_HEIGHT + 8, PREVIEW_WIDTH,
				panelHeight - HEADER_HEIGHT - 30, mouseX, mouseY);
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
				activeNames(data),
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
			rightY = infoLine(graphics, technique.displayName(), Component.translatable("screen.hxh.nen.mastery_short", data.mastery(technique),
						duration(data.training(technique))),
					rightX, rightY, columnWidth);
		}
		footer(graphics, Component.translatable("screen.hxh.nen.hint.status"));
	}

	/** Tecnicas ligadas separadas por virgula, ou "Nenhuma". */
	private static Component activeNames(NenData data) {
		if (data.active().isEmpty()) {
			return Component.translatable("nen.hxh.technique.none");
		}
		MutableComponent names = Component.empty();
		Technique.TABS.stream().filter(data::isActive).forEach(technique -> {
			if (!names.getSiblings().isEmpty()) {
				names.append(", ");
			}
			names.append(technique.displayName());
		});
		return names;
	}

	private int infoLine(GuiGraphicsExtractor graphics, String key, Component value, int x, int y, int width) {
		return infoLine(graphics, Component.translatable(key), value, x, y, width);
	}

	private int infoLine(GuiGraphicsExtractor graphics, Component name, Component value, int x, int y, int width) {
		row(graphics, x, y, width, ROW_HEIGHT - 3, false);
		Component shownValue = panelFont(value);
		graphics.text(font, panelFont(name), x + 6, y + 3, TEXT, false);
		graphics.text(font, shownValue, x + width - 6 - font.width(shownValue), y + 3, TEXT, false);
		return y + ROW_HEIGHT - 1;
	}

	/** O proprio personagem, sentado em meditacao, olhando para o mouse. */
	private void renderPreview(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
		if (minecraft.player != null) {
			int size = Math.min(width * 3 / 5, height * 2 / 5);
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + width, y + height, size, 0.0625F,
					mouseX, mouseY, minecraft.player);
		}
	}

	// ------------------------------------------------------------- Pecas

	private static Component serif(Component text) {
		return text.copy().withStyle(style -> style.withFont(SERIF));
	}

	/**
	 * O texto usa a Cinzel, menos os numeros, que ficam na fonte do jogo porque o 1 da Cinzel
	 * parece um I. Em escala 2 ou menor a Cinzel nao tem pixels para ficar legivel nesse tamanho,
	 * entao tudo fica na fonte do jogo. As cores de cada trecho sao mantidas.
	 */
	Component panelFont(Component text) {
		if (minecraft.getWindow().getGuiScale() <= 2) {
			return text;
		}
		MutableComponent result = Component.empty();
		text.visit((style, string) -> {
			int start = 0;
			for (int i = 1; i <= string.length(); i++) {
				if (i == string.length() || numeric(string.charAt(i)) != numeric(string.charAt(start))) {
					Style partStyle = numeric(string.charAt(start)) ? style : style.withFont(SERIF);
					result.append(Component.literal(string.substring(start, i)).withStyle(partStyle));
					start = i;
				}
			}
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}

	private static boolean numeric(char character) {
		return Character.isDigit(character) || "+-−×%/".indexOf(character) >= 0;
	}

	/** Nome da imagem deste texto: o fim da chave de traducao ("screen.hxh.nen.confirm" vira "confirm"). */
	private static String artKey(Component text) {
		if (text.getContents() instanceof TranslatableContents contents) {
			String key = contents.getKey();
			return key.substring(key.lastIndexOf('.') + 1);
		}
		return "";
	}

	void sectionTitle(GuiGraphicsExtractor graphics, Component title, int x, int y, int width) {
		if (NenArt.text(graphics, "section_" + artKey(title), x, y - 2)) {
			graphics.fill(x, y + 10, x + width, y + 11, GOLD_DIM);
			return;
		}
		graphics.text(font, Component.literal("✦ ").append(title.copy().withStyle(ChatFormatting.BOLD)), x, y, GOLD, false);
		graphics.fill(x, y + 10, x + width, y + 11, GOLD_DIM);
	}

	private void row(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected) {
		row(graphics, x, y, width, height, selected, false);
	}

	void row(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean selected, boolean hover) {
		NenArt.frame(graphics, selected ? "frame_row_selected" : hover ? "frame_row_hover" : "frame_row", x, y, width, height, 5);
	}

	private void button(GuiGraphicsExtractor graphics, Component label, int x, int y, int width, int height,
			boolean enabled, int mouseX, int mouseY, Runnable action) {
		boolean hover = enabled && hovered(x, y, width, height, mouseX, mouseY);
		String state = !enabled ? "_off" : hover ? "_on" : "";
		NenArt.frame(graphics, "frame_button" + state, x, y, width, height, 6);
		String key = "button_" + artKey(label);
		int labelWidth = NenArt.width(key, 0);
		if (!NenArt.text(graphics, key + state, x + (width - labelWidth) / 2, y + (height - 14) / 2)) {
			graphics.centeredText(font, panelFont(label), x + width / 2, y + (height - 8) / 2, enabled ? (hover ? GOLD : TEXT) : DIM);
		}
		addClickable(x, y, width, height, enabled, action);
	}

	/** Largura do botao para caber o texto, com um minimo para os botoes ficarem alinhados. */
	private int buttonWidth(Component label) {
		return Math.max(64, NenArt.width("button_" + artKey(label), font.width(panelFont(label))) + 16);
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

	void footer(GuiGraphicsExtractor graphics, Component hint) {
		int y = top + panelHeight - 13;
		graphics.fill(left + MARGIN, y - 4, left + panelWidth - MARGIN, y - 3, GOLD_DIM);
		graphics.text(font, panelFont(hint), left + MARGIN, y, DIM, false);
	}

	/** Area que recebe a posicao do mouse no clique, como as barras da aba Aura. */
	void addPressable(int x, int y, int width, int height, DoubleConsumer action) {
		addClickable(x, y, width, height, true, () -> action.accept(pressX));
	}

	/** Centro de uma cor pronta na aba Aura. */
	public int[] auraPresetCenter(int index) {
		return aura.presetCenter(index);
	}

	/** Ponto na barra de matiz da aba Aura. */
	public int[] auraHuePoint(float fraction) {
		return aura.huePoint(fraction);
	}

	void addClickable(int x, int y, int width, int height, boolean enabled, Runnable action) {
		addClickable(x, y, width, height, enabled, action, null);
	}

	private void addClickable(int x, int y, int width, int height, boolean enabled, Runnable action, Runnable rightAction) {
		// Corta a parte que ficou fora da area rolavel
		int clippedTop = Math.max(y, clipTop);
		int clippedBottom = Math.min(y + height, clipBottom);
		if (clippedBottom <= clippedTop) {
			return;
		}
		y = clippedTop;
		height = clippedBottom - clippedTop;
		clickables.add(new Clickable(x, y, width, height, enabled, action, rightAction));
	}

	static boolean hovered(int x, int y, int width, int height, int mouseX, int mouseY) {
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
	// Entrada
	// ======================================================================

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
		pressX = event.x();
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT || right) {
			// Percorre de tras para frente: o que foi desenhado por ultimo fica por cima
			for (int i = clickables.size() - 1; i >= 0; i--) {
				Clickable clickable = clickables.get(i);
				Runnable action = right ? clickable.rightAction() : clickable.action();
				if (action == null || !clickable.contains(event.x(), event.y())) {
					continue;
				}
				if (clickable.enabled()) {
					action.run();
				}
				// Clique duplo numa tecnica da lista: liga/desliga
				if (doubleClick && !right && page == Page.TECHNIQUES && hoveredTechnique >= 0) {
					handleTechniqueKey(InputConstants.KEY_RETURN);
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return aura.drag(event.x()) || super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return aura.release() || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0 || !data().awakened()) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int direction = scrollY > 0 ? 1 : -1;
		if (page == Page.LEVEL) {
			handleLevelKey(direction > 0 ? InputConstants.KEY_UP : InputConstants.KEY_DOWN);
			return true;
		}
		if (page == Page.TECHNIQUES) {
			if (mouseX >= detailsLeft) {
				detailScroll = Math.clamp(detailScroll - direction * DETAIL_SCROLL_STEP, 0, maxDetailScroll);
			} else {
				handleTechniqueKey(direction > 0 ? InputConstants.KEY_UP : InputConstants.KEY_DOWN);
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (HxHModClient.OPEN_NEN_PANEL.matches(event)) {
			onClose();
			return true;
		}
		int key = event.key();
		if (key == InputConstants.KEY_TAB) {
			if (page == Page.TECHNIQUES) {
				setPage(Page.TREE);
				return true;
			}
			int step = event.hasShiftDown() ? TABS.size() - 1 : 1;
			setPage(TABS.get((TABS.indexOf(page) + step) % TABS.size()));
			return true;
		}
		if (page == Page.LEVEL && handleLevelKey(key)) {
			return true;
		}
		if (page == Page.TECHNIQUES && handleTechniqueKey(key)) {
			return true;
		}
		return super.keyPressed(event);
	}

	private boolean handleLevelKey(int key) {
		int count = NenStat.values().length;
		switch (key) {
			case InputConstants.KEY_UP -> selectedStat = (selectedStat + count - 1) % count;
			case InputConstants.KEY_DOWN -> selectedStat = (selectedStat + 1) % count;
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
				if (data().isUnlocked(technique) && technique.activatable()) {
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
