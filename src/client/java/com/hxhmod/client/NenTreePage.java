package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Aba Arvore do painel: o emblema do Nen a esquerda, os quatro principios saindo dele, as tecnicas
 * avancadas ligadas ao que elas exigem e as sub-habilidades em volta de cada tecnica.
 * Tudo usa coordenadas fixas ({@link #WIDTH} x {@link #HEIGHT}), escaladas para caber na tela.
 */
public final class NenTreePage {
	public static final int WIDTH = 640;
	public static final int HEIGHT = 300;
	private static final int TECHNIQUE_RADIUS = 13;
	private static final int SUB_RADIUS = 9;
	private static final int ROOT_RADIUS = 24;
	private static final int ROOT_X = 32;
	private static final int ROOT_Y = 150;
	private static final int ROOT_ELBOW_X = 66;
	private static final float MAX_SCALE = 2.0F;
	private static final float LABEL_SCALE = 0.75F;
	private static final float NAME_SCALE = 0.65F;

	private static final int LINE_OFF = 0xFF5A5D66;
	private static final int LINE_ON = 0xFFC4E254;
	private static final int LINE_SUB = 0xFF70747E;
	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF9A968C;
	private static final int GOLD = 0xFFD8B46A;
	private static final int GREEN = 0xFFC4E254;
	private static final int BAD = 0xFFD46A6A;
	private static final int BOX = 0xD0101218;
	private static final int BOX_EDGE = 0xFF4A4C54;
	private static final int OPTIONS_HEADER = 0xFF2F6F96;
	private static final int ICON_ON = 0xFFF4FAD8;
	private static final int ICON_AVAILABLE = 0xFFFFFFFF;
	private static final int ICON_LOCKED = 0xFF9DA0A8;
	private static final int ICON_SOON = 0xFF62656C;

	private static final Identifier BACKGROUND = texture("background");
	private static final Identifier EMBLEM = texture("emblem");
	private static final Identifier RING_LOCKED = texture("ring_locked");
	private static final Identifier RING_SOON = texture("ring_soon");
	private static final Identifier RING_AVAILABLE = texture("ring_available");
	private static final Identifier RING_LEARNED = texture("ring_learned");
	private static final Identifier RING_ACTIVE = texture("ring_active");

	/** Um ponto da arvore: tecnica, sub-habilidade comprada com pontos ou passiva que ainda nao existe no jogo. */
	public record Node(Technique technique, NenUpgrade upgrade, String soonKey, int x, int y, String icon) {
		boolean isTechnique() {
			return upgrade == null && soonKey == null;
		}

		int radius() {
			return isTechnique() ? TECHNIQUE_RADIUS : SUB_RADIUS;
		}
	}

	/** Ligacao de requisito: da tecnica base ate a que depende dela, passando pelos pontos intermediarios. */
	private record Edge(Technique parent, Technique child, int... points) {
	}

	/** Faixa da direita, com o nome do grupo de tecnicas. */
	private record Band(String key, int textY) {
	}

	private static final List<Node> NODES = List.of(
			technique(Technique.TEN, 92, 40), technique(Technique.ZETSU, 92, 110),
			technique(Technique.REN, 92, 180), technique(Technique.HATSU, 92, 255),
			upgrade(NenUpgrade.TEN_FLOW, 138, 25, "drop"), upgrade(NenUpgrade.TEN_YOUTH, 138, 55, "heart"),
			upgrade(NenUpgrade.ZETSU_SUPPRESS, 138, 87, "eye_off"), upgrade(NenUpgrade.ZETSU_REST, 138, 110, "moon"),
			upgrade(NenUpgrade.ZETSU_STEP, 138, 133, "step"),
			upgrade(NenUpgrade.REN_CONTROL, 138, 165, "gauge"), upgrade(NenUpgrade.REN_SURGE, 138, 195, "fist"),
			upgrade(NenUpgrade.HATSU_EFFICIENCY, 138, 232, "star"),
			soon(Technique.HATSU, "exclusive_1", 138, 255, "rune"), soon(Technique.HATSU, "exclusive_2", 138, 278, "rune"),

			technique(Technique.GYO, 245, 25), technique(Technique.IN, 245, 80),
			technique(Technique.EN, 245, 140), technique(Technique.KEN, 245, 190), technique(Technique.SHU, 245, 255),
			upgrade(NenUpgrade.GYO_PRECISE, 292, 10, "target"), upgrade(NenUpgrade.GYO_INSTINCT, 292, 40, "arrows"),
			upgrade(NenUpgrade.IN_CONTROL, 292, 65, "mask"), upgrade(NenUpgrade.IN_SHADOW, 292, 95, "hourglass"),
			upgrade(NenUpgrade.EN_WIDE, 292, 125, "expand"), upgrade(NenUpgrade.EN_CONTROL, 292, 155, "compass"),
			upgrade(NenUpgrade.KEN_ENDURANCE, 292, 175, "shield"), upgrade(NenUpgrade.KEN_IRON, 292, 205, "shield_plus"),
			upgrade(NenUpgrade.SHU_COAT, 292, 240, "coat"), upgrade(NenUpgrade.SHU_EDGE, 292, 270, "blade"),

			technique(Technique.RYU, 400, 170), technique(Technique.KO, 400, 250),
			upgrade(NenUpgrade.RYU_MASTER, 447, 155, "arrows"), upgrade(NenUpgrade.RYU_FLOW, 447, 185, "wave"),
			upgrade(NenUpgrade.KO_ABSOLUTE, 447, 235, "burst"), upgrade(NenUpgrade.KO_FOCUS, 447, 265, "timer"));

	// As linhas descem por colunas proprias e passam nos vaos entre as sub-habilidades
	private static final List<Edge> EDGES = List.of(
			new Edge(Technique.TEN, Technique.GYO, 92, 40, 170, 40, 245, 25),
			new Edge(Technique.TEN, Technique.EN, 92, 40, 170, 40, 170, 140, 245, 140),
			new Edge(Technique.TEN, Technique.KEN, 92, 40, 170, 40, 170, 190, 245, 190),
			new Edge(Technique.TEN, Technique.SHU, 92, 40, 170, 40, 170, 255, 245, 255),
			new Edge(Technique.TEN, Technique.KO, 92, 40, 170, 40, 170, 222, 320, 222, 400, 250),
			new Edge(Technique.ZETSU, Technique.IN, 92, 110, 122, 98, 170, 98, 245, 80),
			new Edge(Technique.ZETSU, Technique.KO, 92, 110, 122, 98, 178, 98, 178, 222, 320, 222, 400, 250),
			new Edge(Technique.REN, Technique.EN, 92, 180, 186, 180, 186, 140, 245, 140),
			new Edge(Technique.REN, Technique.KEN, 92, 180, 186, 180, 245, 190),
			new Edge(Technique.REN, Technique.KO, 92, 180, 186, 180, 186, 222, 320, 222, 400, 250),
			new Edge(Technique.HATSU, Technique.KO, 92, 255, 122, 266, 170, 266, 194, 222, 320, 222, 400, 250),
			new Edge(Technique.GYO, Technique.KO, 245, 25, 320, 25, 320, 222, 400, 250),
			new Edge(Technique.GYO, Technique.RYU, 245, 25, 320, 25, 320, 170, 400, 170),
			new Edge(Technique.KEN, Technique.RYU, 245, 190, 332, 190, 400, 170));

	private static final List<Band> BANDS = List.of(
			new Band("screen.hxh.nen.tree.band.ten_zetsu", 74),
			new Band("screen.hxh.nen.tree.band.ren_ken", 174),
			new Band("screen.hxh.nen.tree.band.ryu_shu", 276));

	private final NenScreen screen;
	private boolean showNames;
	private boolean resetArmed;
	private Node selected;

	NenTreePage(NenScreen screen) {
		this.screen = screen;
	}

	private static Identifier texture(String name) {
		return HxHMod.id("textures/gui/tree/" + name + ".png");
	}

	private static Node technique(Technique technique, int x, int y) {
		return new Node(technique, null, null, x, y, null);
	}

	private static Node upgrade(NenUpgrade upgrade, int x, int y, String icon) {
		return new Node(upgrade.technique(), upgrade, null, x, y, icon);
	}

	private static Node soon(Technique technique, String key, int x, int y, String icon) {
		return new Node(technique, null, key, x, y, icon);
	}

	private static Node nodeOf(Technique technique) {
		return NODES.stream().filter(node -> node.isTechnique() && node.technique() == technique).findFirst().orElseThrow();
	}

	/** Escala da arvore na area disponivel. */
	public static float scale(int areaWidth, int areaHeight) {
		return Math.min(MAX_SCALE, Math.min(areaWidth / (float) WIDTH, areaHeight / (float) HEIGHT));
	}

	/** Posicao na tela de um ponto da arvore (para os testes acharem os nos e botoes). */
	public static float[] screenPoint(int x, int y, int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		float scale = scale(areaWidth, areaHeight);
		return new float[] {areaLeft + (areaWidth - WIDTH * scale) / 2 + x * scale,
				areaTop + (areaHeight - HEIGHT * scale) / 2 + y * scale};
	}

	public static float[] screenPoint(Technique technique, int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		Node node = nodeOf(technique);
		return screenPoint(node.x(), node.y(), areaLeft, areaTop, areaWidth, areaHeight);
	}

	/** Passiva que este ponto vira para quem revelou o Reforco; os outros tipos seguem em pausa. */
	private static NenUpgrade exclusiveOf(Node node) {
		if ("exclusive_1".equals(node.soonKey())) {
			return NenUpgrade.HATSU_VITALITY;
		}
		return "exclusive_2".equals(node.soonKey()) ? NenUpgrade.HATSU_IMPACT : null;
	}

	/** O ponto como este jogador deve ver: a passiva exclusiva do tipo dele, se ja existir no jogo. */
	private static Node resolve(NenData data, Node node) {
		NenUpgrade exclusive = exclusiveOf(node);
		if (exclusive == null || !exclusive.availableTo(data)) {
			return node;
		}
		return new Node(node.technique(), exclusive, null, node.x(), node.y(), exclusive == NenUpgrade.HATSU_VITALITY ? "heart" : "fist");
	}

	private static List<Node> nodes(NenData data) {
		return NODES.stream().map(node -> resolve(data, node)).toList();
	}

	public static float[] screenPoint(NenUpgrade upgrade, int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		Node node = NODES.stream().filter(candidate -> candidate.upgrade() == upgrade || exclusiveOf(candidate) == upgrade)
				.findFirst().orElseThrow();
		return screenPoint(node.x(), node.y(), areaLeft, areaTop, areaWidth, areaHeight);
	}

	void render(GuiGraphicsExtractor graphics, Font font, NenData data, int areaLeft, int areaTop, int areaWidth,
			int areaHeight, int mouseX, int mouseY) {
		float scale = scale(areaWidth, areaHeight);
		float originX = areaLeft + (areaWidth - WIDTH * scale) / 2;
		float originY = areaTop + (areaHeight - HEIGHT * scale) / 2;
		float treeX = (mouseX - originX) / scale;
		float treeY = (mouseY - originY) / scale;

		List<Node> nodes = nodes(data);
		Node hovered = null;
		for (Node node : nodes) {
			if (Math.hypot(treeX - node.x(), treeY - node.y()) <= node.radius() + 1) {
				hovered = node;
			}
		}
		if (hovered != null) {
			selected = hovered;
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(originX, originY);
		graphics.pose().scale(scale, scale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0, 0, WIDTH, HEIGHT, 1280, 600, 1280, 600);

		drawLines(graphics, data);
		for (Node node : nodes) {
			drawNode(graphics, font, data, node, node == hovered);
		}
		drawRoot(graphics, font);
		drawBands(graphics, font);
		drawDetails(graphics, font, data);
		boolean[] controls = drawControls(graphics, font, data, treeX, treeY);
		graphics.pose().popMatrix();

		// Areas de clique na tela, sem a escala
		for (Node node : nodes) {
			int size = (int) ((node.radius() * 2 + 2) * scale);
			int nodeX = (int) (originX + node.x() * scale) - size / 2;
			int nodeY = (int) (originY + node.y() * scale) - size / 2;
			screen.addClickable(nodeX, nodeY, size, size, true, () -> click(data, node));
		}
		clickable(originX, originY, scale, 4, 16, 92, 13, () -> showNames = !showNames);
		clickable(originX, originY, scale, 8, 280, 56, 15, () -> clickReset(controls[0]));
	}

	private void clickable(float originX, float originY, float scale, int x, int y, int width, int height, Runnable action) {
		screen.addClickable((int) (originX + x * scale), (int) (originY + y * scale),
				(int) (width * scale), (int) (height * scale), true, action);
	}

	private void click(NenData data, Node node) {
		selected = node;
		if (node.isTechnique()) {
			screen.openTechnique(node.technique());
		} else if (node.upgrade() != null && data.upgradeProblem(node.upgrade()) == null) {
			ClientPlayNetworking.send(new NenPayloads.BuyUpgrade(node.upgrade()));
		}
	}

	/** Primeiro clique pede confirmacao; o segundo devolve os pontos das sub-habilidades. */
	private void clickReset(boolean canReset) {
		if (!canReset) {
			return;
		}
		if (!resetArmed) {
			resetArmed = true;
			return;
		}
		resetArmed = false;
		ClientPlayNetworking.send(new NenPayloads.ResetUpgrades());
	}

	void disarm() {
		resetArmed = false;
	}

	// ---------------------------------------------------------------- Linhas

	private static void drawLines(GuiGraphicsExtractor graphics, NenData data) {
		// Sub-habilidades ligadas a sua tecnica
		for (Node node : nodes(data)) {
			if (!node.isTechnique()) {
				Node owner = nodeOf(node.technique());
				boolean owned = node.upgrade() != null && data.has(node.upgrade());
				line(graphics, owner.x(), owner.y(), node.x(), node.y(), owned ? LINE_ON : LINE_SUB);
			}
		}
		// Principios saindo do emblema
		for (Technique base : List.of(Technique.TEN, Technique.ZETSU, Technique.REN, Technique.HATSU)) {
			Node node = nodeOf(base);
			// Chega pela esquerda para nao passar por cima do nome da tecnica
			polyline(graphics, new int[] {ROOT_X, ROOT_Y, ROOT_ELBOW_X, node.y(), node.x(), node.y()},
					data.isUnlocked(base) ? LINE_ON : LINE_OFF);
		}
		// Requisitos: primeiro os que faltam, depois os cumpridos por cima
		for (boolean met : new boolean[] {false, true}) {
			for (Edge edge : EDGES) {
				if (requirementMet(data, edge) == met) {
					polyline(graphics, edge.points(), met ? LINE_ON : LINE_OFF);
				}
			}
		}
	}

	private static boolean requirementMet(NenData data, Edge edge) {
		return edge.child().requirements().stream().anyMatch(requirement -> requirement.technique() == edge.parent()
				&& data.mastery(edge.parent()) >= requirement.mastery());
	}

	private static void polyline(GuiGraphicsExtractor graphics, int[] points, int color) {
		for (int i = 0; i + 3 < points.length; i += 2) {
			line(graphics, points[i], points[i + 1], points[i + 2], points[i + 3], color);
		}
	}

	/** Linha em qualquer angulo: um retangulo fino girado. */
	private static void line(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x1, y1);
		graphics.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
		graphics.fill(0, -1, (int) Math.round(Math.hypot(x2 - x1, y2 - y1)), 1, color);
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------- Nos

	private void drawNode(GuiGraphicsExtractor graphics, Font font, NenData data, Node node, boolean hover) {
		Identifier ring = ring(data, node);
		// O anel tem halo em volta: a textura e maior que o circulo
		int size = node.radius() * 64 / 24;
		image(graphics, ring, node.x() - size / 2, node.y() - size / 2, size, 64, 0xFFFFFFFF);
		if (hover) {
			image(graphics, RING_AVAILABLE, node.x() - size / 2, node.y() - size / 2, size, 64, 0x60FFFFFF);
		}

		if (node.isTechnique()) {
			boolean learned = data.isUnlocked(node.technique());
			Component kanji = Component.translatable("nen.hxh.technique." + node.technique().getSerializedName() + ".kanji");
			graphics.centeredText(font, kanji, node.x(), node.y() - 4, learned ? 0xFFFFFFFF : ICON_LOCKED);
			int value = learned ? data.mastery(node.technique()) : data.requirementProgress(node.technique());
			Component label = node.technique().displayName().copy().append(" " + value + "%");
			small(graphics, font, label, node.x(), node.y() + node.radius() + 2, LABEL_SCALE, learned ? TEXT : DIM, true);
			return;
		}
		int iconSize = node.radius() * 2 - 6;
		image(graphics, HxHMod.id("textures/gui/tree/icon/" + node.icon() + ".png"), node.x() - iconSize / 2, node.y() - iconSize / 2,
				iconSize, 48, iconColor(data, node));
		if (showNames) {
			small(graphics, font, title(data, node), node.x() + node.radius() + 2, node.y() - 3, NAME_SCALE, TEXT, false);
		}
	}

	private static Identifier ring(NenData data, Node node) {
		if (node.isTechnique()) {
			return data.isActive(node.technique()) ? RING_ACTIVE : data.isUnlocked(node.technique()) ? RING_LEARNED : RING_LOCKED;
		}
		if (node.upgrade() == null) {
			return RING_SOON;
		}
		return data.has(node.upgrade()) ? RING_LEARNED : data.upgradeProblem(node.upgrade()) == null ? RING_AVAILABLE : RING_LOCKED;
	}

	private static int iconColor(NenData data, Node node) {
		if (node.upgrade() == null) {
			return ICON_SOON;
		}
		return data.has(node.upgrade()) ? ICON_ON : data.upgradeProblem(node.upgrade()) == null ? ICON_AVAILABLE : ICON_LOCKED;
	}

	private static void drawRoot(GuiGraphicsExtractor graphics, Font font) {
		image(graphics, EMBLEM, ROOT_X - ROOT_RADIUS - 2, ROOT_Y - ROOT_RADIUS - 2, ROOT_RADIUS * 2 + 4, 128, 0xFFFFFFFF);
	}

	private static void image(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size, int textureSize, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, size, size, textureSize, textureSize, textureSize, textureSize, color);
	}

	private static void small(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, float scale, int color, boolean centered) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, centered ? -font.width(text) / 2 : 0, 0, color, true);
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------- Textos

	private static void drawBands(GuiGraphicsExtractor graphics, Font font) {
		for (Band band : BANDS) {
			Component title = Component.translatable(band.key()).withStyle(ChatFormatting.BOLD);
			Component subtitle = Component.translatable(band.key() + ".desc");
			graphics.text(font, title, WIDTH - 6 - font.width(title), band.textY(), TEXT, true);
			graphics.pose().pushMatrix();
			graphics.pose().translate(WIDTH - 6, band.textY() + 11);
			graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
			graphics.text(font, subtitle, -font.width(subtitle), 0, DIM, true);
			graphics.pose().popMatrix();
		}
	}

	/** Quadro no alto a direita com o no embaixo do mouse. */
	private void drawDetails(GuiGraphicsExtractor graphics, Font font, NenData data) {
		// Comeca depois dos nomes das sub-habilidades da coluna do meio
		int x = 382;
		int y = 6;
		int width = 144;
		int height = 132;
		graphics.fill(x, y, x + width, y + height, BOX);
		graphics.fill(x, y, x + width, y + 1, BOX_EDGE);
		graphics.fill(x, y + height - 1, x + width, y + height, BOX_EDGE);
		int textX = x + 6;
		int textWidth = width - 12;
		int line = y + 6;
		if (selected == null) {
			paragraph(graphics, font, Component.translatable("screen.hxh.nen.tree.help"), textX, line, textWidth, DIM);
			return;
		}
		graphics.text(font, title(data, selected).copy().withStyle(ChatFormatting.BOLD), textX, line, GOLD, true);
		line += 11;
		small(graphics, font, subtitle(selected), textX, line, LABEL_SCALE, DIM, false);
		line += 10;
		for (Component text : details(data, selected)) {
			line = paragraph(graphics, font, text, textX, line, textWidth, TEXT) + 3;
		}
	}

	private static int paragraph(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = font.split(text, (int) (width / 0.85F));
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(0.85F, 0.85F);
		int lineY = 0;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, 0, lineY, color, false);
			lineY += font.lineHeight + 1;
		}
		graphics.pose().popMatrix();
		return y + (int) (lineY * 0.85F);
	}

	private static Component title(NenData data, Node node) {
		if (node.isTechnique()) {
			return node.technique().displayName();
		}
		node = resolve(data, node);
		if (node.upgrade() != null) {
			return node.upgrade().displayName();
		}
		return Component.translatable(soonKey(data, node));
	}

	private static Component subtitle(Node node) {
		if (node.isTechnique()) {
			return node.technique().meaning();
		}
		return Component.translatable("screen.hxh.nen.tree.sub_of", node.technique().displayName());
	}

	/** Passivas exclusivas mudam com o tipo de Nen; antes de revelar o tipo elas ficam genericas. */
	private static String soonKey(NenData data, Node node) {
		if (node.soonKey().startsWith("exclusive") && data.revealed()) {
			return "nen.hxh.tree." + node.soonKey() + "." + data.type().getSerializedName();
		}
		return "nen.hxh.tree." + node.soonKey();
	}

	private List<Component> details(NenData data, Node original) {
		Node node = resolve(data, original);
		List<Component> lines = new ArrayList<>();
		if (node.isTechnique()) {
			Technique technique = node.technique();
			if (data.isUnlocked(technique)) {
				lines.add(Component.translatable("screen.hxh.nen.tree.proficiency", data.mastery(technique)).withColor(GREEN));
			} else {
				lines.add(screen.requirements(data, technique));
			}
			lines.add(technique.effect());
			lines.add(Component.translatable("screen.hxh.nen.tree.open").withColor(DIM));
		} else if (node.upgrade() != null) {
			NenUpgrade upgrade = node.upgrade();
			lines.add(upgrade.description());
			if (data.has(upgrade)) {
				lines.add(Component.translatable("screen.hxh.nen.owned").withColor(GREEN));
			} else {
				lines.add(Component.translatable("screen.hxh.nen.upgrade_requirement", upgrade.minLevel(), upgrade.cost())
						.withColor(data.upgradeProblem(upgrade) == null ? TEXT : BAD));
				lines.add(Component.translatable(data.upgradeProblem(upgrade) == null
						? "screen.hxh.nen.tree.buy" : "screen.hxh.nen.tree.cannot_buy").withColor(DIM));
			}
		} else {
			lines.add(Component.translatable(soonKey(data, node) + ".desc"));
			lines.add(Component.translatable("screen.hxh.nen.tree.soon").withColor(DIM));
		}
		return lines;
	}

	/** Opcoes, pontos e o botao de resetar. Devolve {pode resetar}. */
	private boolean[] drawControls(GuiGraphicsExtractor graphics, Font font, NenData data, float treeX, float treeY) {
		// Opcoes
		graphics.fill(4, 4, 96, 16, OPTIONS_HEADER);
		graphics.text(font, Component.translatable("screen.hxh.nen.tree.options"), 8, 6, 0xFFFFFFFF, false);
		graphics.fill(4, 16, 96, 29, BOX);
		graphics.fill(7, 19, 14, 26, 0xFFFFFFFF);
		graphics.fill(8, 20, 13, 25, showNames ? 0xFF2F6F96 : 0xFF20232A);
		small(graphics, font, Component.translatable("screen.hxh.nen.tree.show_names"), 17, 19, 0.8F, TEXT, false);

		// Pontos
		Component points = Component.literal(String.valueOf(data.pointsAvailable()));
		graphics.pose().pushMatrix();
		graphics.pose().translate(8, 248);
		graphics.pose().scale(2.0F, 2.0F);
		graphics.text(font, Component.literal("▲"), 0, 0, GOLD, true);
		graphics.text(font, points, 10, 0, TEXT, true);
		graphics.pose().popMatrix();
		small(graphics, font, Component.translatable("screen.hxh.nen.tree.points"), 8, 268, LABEL_SCALE, DIM, false);

		// Resetar: pede confirmacao antes
		boolean canReset = data.upgrades().stream().anyMatch(upgrade -> !data.grantedUpgrades().contains(upgrade));
		if (!canReset) {
			resetArmed = false;
		}
		boolean hover = treeX >= 8 && treeX < 64 && treeY >= 280 && treeY < 295;
		graphics.fill(8, 280, 64, 295, !canReset ? 0xFF2A2C32 : hover ? 0xFF6A6E78 : 0xFF4C4F58);
		Component label = Component.translatable(resetArmed ? "screen.hxh.nen.tree.confirm" : "screen.hxh.nen.tree.reset");
		graphics.centeredText(font, label, 36, 284, !canReset ? DIM : resetArmed ? BAD : 0xFFFFFFFF);
		return new boolean[] {canReset};
	}
}
