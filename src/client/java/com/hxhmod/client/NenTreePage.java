package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.nen.Technique;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
	private static final int ROOT_ELBOW_X = 70;
	private static final float MAX_SCALE = 2.0F;
	private static final float LABEL_SCALE = 0.75F;
	private static final float NAME_SCALE = 0.62F;
	private static final int NAME_WRAP = 96;
	private static final int NAME_WRAP_ARC = 70;
	private static final int ARC_X = 132;
	private static final int TOOLTIP_WIDTH = 170;
	private static final int TECHNIQUES_RIGHT = 495;
	private static final int TECHNIQUES_TOP = 12;

	private static final int LINE_OFF = 0xFF7A7D86;
	private static final int LINE_ON = 0xFFC4E254;
	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF9A968C;
	private static final int GOLD = 0xFFD8B46A;
	private static final int GREEN = 0xFFC4E254;
	private static final int BAD = 0xFFD46A6A;
	private static final int BOX = 0xD0101218;
	private static final int TOOLTIP = 0xF00C0D12;
	private static final int TOOLTIP_EDGE = 0xFFB08A4A;
	private static final int OPTIONS_HEADER = 0xFF2F6F96;
	private static final int ICON_ON = 0xFFF4FAD8;
	private static final int ICON_AVAILABLE = 0xFFFFFFFF;
	private static final int ICON_LOCKED = 0xFF9DA0A8;
	private static final int ICON_SOON = 0xFF62656C;

	private static final Identifier BACKGROUND = texture("background");
	private static final Identifier EMBLEM = texture("emblem");
	private static final Identifier GLOW_AVAILABLE = texture("glow_available");
	private static final Identifier GLOW_LEARNED = texture("glow_learned");
	private static final Identifier GLOW_ACTIVE = texture("glow_active");
	private static final Identifier LINE_THIN = texture("line_off");
	private static final Identifier LINE_GLOW = texture("line_on");
	private static final Identifier DOT = texture("dot");
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

	/** Ponto onde as sub-habilidades de uma tecnica se juntam de novo, fechando o losango. */
	private record Junction(Technique technique, int x, int y) {
	}

	private static final List<Node> NODES = List.of(
			technique(Technique.TEN, 100, 30), technique(Technique.ZETSU, 100, 100),
			technique(Technique.REN, 100, 175), technique(Technique.HATSU, 100, 250),
			upgrade(NenUpgrade.TEN_FLOW, 140, 15, "drop"), upgrade(NenUpgrade.TEN_YOUTH, 140, 45, "heart"),
			upgrade(NenUpgrade.ZETSU_SUPPRESS, 132, 77, "eye_off"), upgrade(NenUpgrade.ZETSU_REST, 168, 100, "moon"),
			upgrade(NenUpgrade.ZETSU_STEP, 132, 123, "step"),
			upgrade(NenUpgrade.REN_CONTROL, 140, 160, "gauge"), upgrade(NenUpgrade.REN_SURGE, 140, 190, "fist"),
			upgrade(NenUpgrade.HATSU_EFFICIENCY, 132, 227, "star"),
			soon(Technique.HATSU, "exclusive_1", 168, 250, "rune"), soon(Technique.HATSU, "exclusive_2", 132, 273, "rune"),

			technique(Technique.GYO, 225, 30), technique(Technique.IN, 225, 100),
			technique(Technique.KEN, 225, 175), technique(Technique.SHU, 225, 250),
			upgrade(NenUpgrade.GYO_PRECISE, 270, 15, "target"), upgrade(NenUpgrade.GYO_INSTINCT, 270, 45, "arrows"),
			upgrade(NenUpgrade.IN_CONTROL, 270, 85, "mask"), upgrade(NenUpgrade.IN_SHADOW, 270, 115, "hourglass"),
			upgrade(NenUpgrade.KEN_ENDURANCE, 270, 160, "shield"), upgrade(NenUpgrade.KEN_IRON, 270, 190, "shield_plus"),
			upgrade(NenUpgrade.SHU_COAT, 270, 235, "coat"), upgrade(NenUpgrade.SHU_EDGE, 270, 265, "blade"),

			technique(Technique.EN, 360, 72), technique(Technique.RYU, 360, 175), technique(Technique.KO, 360, 250),
			upgrade(NenUpgrade.EN_WIDE, 405, 57, "expand"), upgrade(NenUpgrade.EN_CONTROL, 405, 87, "compass"),
			upgrade(NenUpgrade.RYU_MASTER, 405, 160, "arrows"), upgrade(NenUpgrade.RYU_FLOW, 405, 190, "wave"),
			upgrade(NenUpgrade.KO_ABSOLUTE, 405, 235, "burst"), upgrade(NenUpgrade.KO_FOCUS, 405, 265, "timer"));

	private static final List<Junction> JUNCTIONS = List.of(
			new Junction(Technique.TEN, 190, 30), new Junction(Technique.ZETSU, 190, 100),
			new Junction(Technique.REN, 190, 175), new Junction(Technique.HATSU, 190, 250),
			new Junction(Technique.GYO, 315, 30), new Junction(Technique.IN, 315, 100),
			new Junction(Technique.KEN, 315, 175), new Junction(Technique.SHU, 315, 250),
			new Junction(Technique.EN, 450, 72), new Junction(Technique.RYU, 450, 175), new Junction(Technique.KO, 450, 250));

	// Os requisitos saem da juncao da tecnica e correm por dois troncos verticais, em x = 190 e x = 335
	private static final List<Edge> EDGES = List.of(
			new Edge(Technique.TEN, Technique.GYO, 190, 30, 225, 30),
			new Edge(Technique.TEN, Technique.EN, 190, 30, 190, 72, 360, 72),
			new Edge(Technique.TEN, Technique.KEN, 190, 30, 190, 175, 225, 175),
			new Edge(Technique.TEN, Technique.SHU, 190, 30, 190, 250, 225, 250),
			new Edge(Technique.TEN, Technique.KO, 190, 30, 190, 218, 335, 218, 335, 250, 360, 250),
			new Edge(Technique.ZETSU, Technique.IN, 190, 100, 225, 100),
			new Edge(Technique.ZETSU, Technique.KO, 190, 100, 190, 218, 335, 218, 335, 250, 360, 250),
			new Edge(Technique.REN, Technique.EN, 190, 175, 190, 72, 360, 72),
			new Edge(Technique.REN, Technique.KEN, 190, 175, 225, 175),
			new Edge(Technique.REN, Technique.KO, 190, 175, 190, 218, 335, 218, 335, 250, 360, 250),
			new Edge(Technique.HATSU, Technique.KO, 190, 250, 190, 218, 335, 218, 335, 250, 360, 250),
			new Edge(Technique.GYO, Technique.KO, 315, 30, 335, 30, 335, 250, 360, 250),
			new Edge(Technique.GYO, Technique.RYU, 315, 30, 335, 30, 335, 175, 360, 175),
			new Edge(Technique.KEN, Technique.RYU, 315, 175, 360, 175));

	private static final List<Band> BANDS = List.of(
			new Band("ten_zetsu", 62), new Band("ren_ken", 159), new Band("ryu_shu", 256));

	private final NenScreen screen;
	private boolean showNames;
	private boolean resetArmed;

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

		graphics.pose().pushMatrix();
		graphics.pose().translate(originX, originY);
		graphics.pose().scale(scale, scale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0, 0, WIDTH, HEIGHT, 1280, 600, 1280, 600);

		// Brilho embaixo das linhas: elas chegam inteiras ate a borda de cada no
		for (Node node : nodes) {
			drawGlow(graphics, data, node, node == hovered);
		}
		drawLines(graphics, data);
		for (Node node : nodes) {
			drawNode(graphics, font, data, node, node == hovered);
		}
		drawRoot(graphics, font);
		drawOptions(graphics, font);
		graphics.pose().popMatrix();

		// Por cima, fora da escala: imagens pixel a pixel e texto nitido
		drawBands(graphics, originX, originY, scale);
		drawControls(graphics, font, data, originX, originY, scale, mouseX, mouseY);
		if (hovered != null) {
			drawTooltip(graphics, font, data, hovered, originX + hovered.x() * scale, originY + hovered.y() * scale,
					hovered.radius() * scale, areaLeft, areaTop, areaWidth, areaHeight);
		}

		// Areas de clique na tela, sem a escala
		for (Node node : nodes) {
			int size = (int) ((node.radius() * 2 + 2) * scale);
			int nodeX = (int) (originX + node.x() * scale) - size / 2;
			int nodeY = (int) (originY + node.y() * scale) - size / 2;
			screen.addClickable(nodeX, nodeY, size, size, true, () -> click(data, node));
		}
		clickable(originX, originY, scale, 4, 16, 92, 13, () -> showNames = !showNames);
	}

	private void clickable(float originX, float originY, float scale, int x, int y, int width, int height, Runnable action) {
		screen.addClickable((int) (originX + x * scale), (int) (originY + y * scale),
				(int) (width * scale), (int) (height * scale), true, action);
	}

	private void click(NenData data, Node node) {
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
		// Principios saindo do emblema
		for (Technique base : List.of(Technique.TEN, Technique.ZETSU, Technique.REN, Technique.HATSU)) {
			Node node = nodeOf(base);
			// Chega pela esquerda para nao passar por cima do nome da tecnica
			polyline(graphics, new int[] {ROOT_X, ROOT_Y, ROOT_ELBOW_X, node.y(), node.x(), node.y()}, data.isUnlocked(base));
		}
		// Losangos: cada sub-habilidade liga a tecnica a sua juncao
		for (boolean lit : new boolean[] {false, true}) {
			for (Node node : nodes(data)) {
				if (!node.isTechnique() && (node.upgrade() != null && data.has(node.upgrade())) == lit) {
					Node owner = nodeOf(node.technique());
					Junction junction = junctionOf(node.technique());
					line(graphics, owner.x(), owner.y(), node.x(), node.y(), lit);
					line(graphics, node.x(), node.y(), junction.x(), junction.y(), lit);
				}
			}
		}
		// Requisitos: primeiro os que faltam, depois os cumpridos por cima
		for (boolean met : new boolean[] {false, true}) {
			for (Edge edge : EDGES) {
				if (requirementMet(data, edge) == met) {
					polyline(graphics, edge.points(), met);
				}
			}
		}
		for (Junction junction : JUNCTIONS) {
			dot(graphics, junction.x(), junction.y(), 3, LINE_OFF);
		}
	}

	private static Junction junctionOf(Technique technique) {
		return JUNCTIONS.stream().filter(junction -> junction.technique() == technique).findFirst().orElseThrow();
	}

	private static void dot(GuiGraphicsExtractor graphics, int x, int y, int radius, int color) {
		image(graphics, DOT, x - radius, y - radius, radius * 2, 32, color);
	}

	private static boolean requirementMet(NenData data, Edge edge) {
		return edge.child().requirements().stream().anyMatch(requirement -> requirement.technique() == edge.parent()
				&& data.mastery(edge.parent()) >= requirement.mastery());
	}

	private static void polyline(GuiGraphicsExtractor graphics, int[] points, boolean lit) {
		for (int i = 0; i + 3 < points.length; i += 2) {
			line(graphics, points[i], points[i + 1], points[i + 2], points[i + 3], lit);
			if (i > 0) {
				joint(graphics, points[i], points[i + 1], lit);
			}
		}
	}

	/** Arredonda a curva onde dois trechos se encontram. */
	private static void joint(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(lit ? 0.05F : 0.03F, lit ? 0.05F : 0.03F);
		image(graphics, DOT, -16, -16, 32, 32, lit ? LINE_ON : LINE_OFF);
		graphics.pose().popMatrix();
	}

	/** Linha em qualquer angulo: uma faixa de textura com bordas suaves, girada. Acesa, ela brilha. */
	private static void line(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, boolean lit) {
		int thickness = 8;
		graphics.pose().pushMatrix();
		graphics.pose().translate(x1, y1);
		graphics.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
		graphics.blit(RenderPipelines.GUI_TEXTURED, lit ? LINE_GLOW : LINE_THIN, 0, -thickness / 2, 0, 0,
				(int) Math.round(Math.hypot(x2 - x1, y2 - y1)), thickness, 8, 64, 8, 64, lit ? LINE_ON : LINE_OFF);
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------- Nos

	private static void drawGlow(GuiGraphicsExtractor graphics, NenData data, Node node, boolean hover) {
		Identifier glow = glow(data, node);
		if (hover && glow == null) {
			glow = GLOW_AVAILABLE;
		}
		if (glow != null) {
			int size = node.radius() * 64 / 24;
			image(graphics, glow, node.x() - size / 2, node.y() - size / 2, size, 128, 0xFFFFFFFF);
		}
	}

	private static Identifier glow(NenData data, Node node) {
		if (node.isTechnique()) {
			return data.isActive(node.technique()) ? GLOW_ACTIVE : data.isUnlocked(node.technique()) ? GLOW_LEARNED : null;
		}
		if (node.upgrade() == null) {
			return null;
		}
		return data.has(node.upgrade()) ? GLOW_LEARNED : data.upgradeProblem(node.upgrade()) == null ? GLOW_AVAILABLE : null;
	}

	private void drawNode(GuiGraphicsExtractor graphics, Font font, NenData data, Node node, boolean hover) {
		Identifier ring = ring(data, node);
		// O anel tem halo em volta: a textura e maior que o circulo
		int size = node.radius() * 64 / 24;
		image(graphics, ring, node.x() - size / 2, node.y() - size / 2, size, 128, 0xFFFFFFFF);
		if (hover) {
			image(graphics, RING_AVAILABLE, node.x() - size / 2, node.y() - size / 2, size, 128, 0x60FFFFFF);
		}

		if (node.isTechnique()) {
			boolean learned = data.isUnlocked(node.technique());
			image(graphics, texture("kanji/" + node.technique().getSerializedName()), node.x() - 10, node.y() - 10, 20, 96,
					learned ? 0xFFFFFFFF : ICON_LOCKED);
			int value = learned ? data.mastery(node.technique()) : data.requirementProgress(node.technique());
			Component label = screen.panelFont(node.technique().displayName().copy().append(" " + value + "%"));
			small(graphics, font, label, node.x(), node.y() + node.radius() + 2, LABEL_SCALE, learned ? TEXT : DIM, true);
			return;
		}
		int iconSize = node.radius() * 2 - 6;
		image(graphics, HxHMod.id("textures/gui/tree/icon/" + node.icon() + ".png"), node.x() - iconSize / 2, node.y() - iconSize / 2,
				iconSize, 48, iconColor(data, node));
		if (showNames) {
			// Nome embaixo do no, em ate duas linhas, como na referencia
			List<FormattedCharSequence> lines = font.split(screen.panelFont(title(data, node)), node.x() == ARC_X ? NAME_WRAP_ARC : NAME_WRAP);
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(node.x(), node.y() + node.radius() + 1 + i * 6.5F);
				graphics.pose().scale(NAME_SCALE, NAME_SCALE);
				graphics.text(font, lines.get(i), -font.width(lines.get(i)) / 2, 0, TEXT, true);
				graphics.pose().popMatrix();
			}
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

	/** Nome de cada grupo sobre a sua silhueta, encostado na direita. */
	private static void drawBands(GuiGraphicsExtractor graphics, float originX, float originY, float scale) {
		int right = (int) (originX + (WIDTH - 6) * scale);
		for (Band band : BANDS) {
			int y = (int) (originY + band.textY() * scale);
			NenArt.text(graphics, "band_" + band.key(), right - NenArt.width("band_" + band.key(), 0), y);
			NenArt.text(graphics, "band_" + band.key() + "_desc", right - NenArt.width("band_" + band.key() + "_desc", 0), y + 13);
		}
	}

	/** Descricao que aparece ao lado do no embaixo do mouse. */
	private void drawTooltip(GuiGraphicsExtractor graphics, Font font, NenData data, Node node, float nodeX, float nodeY, float radius,
			int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		int width = Math.min(TOOLTIP_WIDTH, areaWidth / 2);
		int textWidth = width - 12;
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		addLines(font, screen.panelFont(title(data, node)), GOLD, textWidth, lines, colors);
		addLines(font, screen.panelFont(subtitle(node)), DIM, textWidth, lines, colors);
		for (Component text : details(data, node)) {
			addLines(font, screen.panelFont(text), text.getStyle().getColor() != null ? text.getStyle().getColor().getValue() | 0xFF000000 : TEXT,
					textWidth, lines, colors);
		}
		int height = lines.size() * (font.lineHeight + 1) + 11;
		int x = (int) (nodeX + radius + 6);
		if (x + width > areaLeft + areaWidth) {
			x = (int) (nodeX - radius - 6) - width;
		}
		int y = Math.clamp((int) nodeY - height / 2, areaTop, Math.max(areaTop, areaTop + areaHeight - height));
		graphics.nextStratum();
		graphics.fill(x, y, x + width, y + height, TOOLTIP);
		graphics.fill(x, y, x + width, y + 1, TOOLTIP_EDGE);
		graphics.fill(x, y + height - 1, x + width, y + height, TOOLTIP_EDGE);
		graphics.fill(x, y + 1, x + 1, y + height - 1, TOOLTIP_EDGE);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, TOOLTIP_EDGE);
		int line = y + 6;
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), x + 6, line, colors.get(i), false);
			line += font.lineHeight + 1;
		}
	}

	private static void addLines(Font font, Component text, int color, int width, List<FormattedCharSequence> lines, List<Integer> colors) {
		for (FormattedCharSequence line : font.split(text, width)) {
			lines.add(line);
			colors.add(color);
		}
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

	private void drawOptions(GuiGraphicsExtractor graphics, Font font) {
		graphics.fillGradient(4, 4, 80, 16, 0xFF3A7FA8, OPTIONS_HEADER);
		graphics.text(font, screen.panelFont(Component.translatable("screen.hxh.nen.tree.options")), 8, 6, 0xFFFFFFFF, false);
		graphics.fill(4, 16, 80, 29, BOX);
		graphics.fill(7, 19, 14, 26, 0xFFFFFFFF);
		graphics.fill(8, 20, 13, 25, showNames ? 0xFF2F6F96 : 0xFF20232A);
		small(graphics, font, screen.panelFont(Component.translatable("screen.hxh.nen.tree.show_names")), 17, 19, 0.8F, TEXT, false);
	}

	/** Pontos e o botao de resetar, em tamanho fixo na tela para o texto sempre caber na moldura. */
	private void drawControls(GuiGraphicsExtractor graphics, Font font, NenData data, float originX, float originY, float scale,
			int mouseX, int mouseY) {
		int x = (int) (originX + 8 * scale);
		int bottom = (int) (originY + 295 * scale);
		int height = 14;
		int y = bottom - height;
		int pointsY = y - 31;
		NenArt.shared(graphics, "points_icon", x, pointsY);
		Component points = Component.literal(String.valueOf(data.pointsAvailable()));
		// Numero grande so se nao encostar no Hatsu
		int numberScale = x + 22 + font.width(points) * 2 < originX + 84 * scale ? 2 : 1;
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 22, pointsY);
		graphics.pose().scale(numberScale, numberScale);
		graphics.text(font, points, 0, 0, TEXT, false);
		graphics.pose().popMatrix();
		NenArt.text(graphics, "tree_points", x, pointsY + 17);

		// Resetar: pede confirmacao antes
		boolean canReset = data.upgrades().stream().anyMatch(upgrade -> !data.grantedUpgrades().contains(upgrade));
		if (!canReset) {
			resetArmed = false;
		}
		String key = resetArmed ? "tree_confirm" : "tree_reset";
		int width = NenArt.width(key, 40) + 16;
		boolean hover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		String state = !canReset ? "_off" : hover || resetArmed ? "_on" : "";
		NenArt.frame(graphics, "frame_button" + state, x, y, width, height, 5);
		NenArt.text(graphics, key + state, x + 8, y + (height - 11) / 2);
		screen.addClickable(x, y, width, height, true, () -> clickReset(canReset));

		// As tecnicas abrem por aqui: a arvore ja mostra todas, entao nao precisam de aba propria
		String techniques = "tree_techniques";
		int techniquesWidth = NenArt.width(techniques, 50) + 16;
		int techniquesX = (int) (originX + TECHNIQUES_RIGHT * scale) - techniquesWidth;
		int techniquesY = (int) (originY + TECHNIQUES_TOP * scale);
		boolean techniquesHover = mouseX >= techniquesX && mouseX < techniquesX + techniquesWidth
				&& mouseY >= techniquesY && mouseY < techniquesY + height;
		NenArt.frame(graphics, "frame_button" + (techniquesHover ? "_on" : ""), techniquesX, techniquesY, techniquesWidth, height, 5);
		NenArt.text(graphics, techniques + (techniquesHover ? "_on" : ""), techniquesX + 8, techniquesY + (height - 11) / 2);
		screen.addClickable(techniquesX, techniquesY, techniquesWidth, height, true, screen::openTechniques);
	}
}
