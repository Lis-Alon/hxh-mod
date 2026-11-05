package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.HatsuAbility;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenAffinity;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Aba Hatsu do painel: o hexagono dos seis tipos de Nen, com um galho para cada tipo. O galho do tipo inato
 * brilha forte, os outros apagam conforme a afinidade, e os de afinidade zero ficam escuros e sem clique.
 * Usa as mesmas coordenadas fixas da aba Arvore, escaladas para caber na tela.
 */
public final class NenHatsuPage {
	private static final int CENTER_X = NenTreePage.WIDTH / 2;
	private static final int CENTER_Y = NenTreePage.HEIGHT / 2;
	// Distancia de cada no ate o centro, do Hatsu ativo ate a ponta do galho
	private static final int[] NODE_DISTANCE = {56, 94, 132};
	private static final int ACTIVE_RADIUS = 15;
	private static final int PASSIVE_RADIUS = 12;
	private static final int CENTER_RADIUS = 30;
	// As imagens dos galhos cobrem 400x300 da area, centradas no hexagono
	private static final int BRANCH_WIDTH = 400;
	private static final int BRANCH_HEIGHT = 300;
	private static final int BRANCH_TEXTURE_WIDTH = 800;
	private static final int BRANCH_TEXTURE_HEIGHT = 600;
	private static final int TOOLTIP_WIDTH = 180;
	private static final int LABEL_GAP = 5;
	// Distancia dos textos ate a borda da imagem, por dentro do filete do canto
	private static final int TEXT_MARGIN = 16;
	private static final long PULSE_MILLIS = 1600;
	// Quanto o brilho cai com a afinidade: a curva deixa 40% bem mais apagado que 80%
	private static final float GLOW_SCALE = 0.75F;
	private static final float GLOW_EXPONENT = 1.5F;
	private static final int DEAD_BRANCH = 0xD0000000;

	private static final int TEXT = 0xFFE9E4D4;
	private static final int DIM = 0xFF9A968C;
	private static final int GOLD = 0xFFD8B46A;
	private static final int GREEN = 0xFFC4E254;
	private static final int BAD = 0xFFD46A6A;
	private static final int DEAD = 0xFF55555C;
	private static final int ICON_LOCKED = 0xFF9DA0A8;
	private static final int ICON_DEAD = 0xFF3A3A40;
	private static final int RING_DEAD = 0xFF4A4A4A;

	private static final Identifier BACKGROUND = texture("background");
	private static final Identifier CENTER_GLOW = texture("center_glow");
	private static final Identifier KANJI = NenTreePage.texture("kanji/hatsu");
	private static final Identifier GLOW_AVAILABLE = NenTreePage.texture("glow_available");
	private static final Identifier GLOW_LEARNED = NenTreePage.texture("glow_learned");
	private static final Identifier GLOW_ACTIVE = NenTreePage.texture("glow_active");
	private static final Identifier RING_LOCKED = NenTreePage.texture("ring_locked");
	private static final Identifier RING_AVAILABLE = NenTreePage.texture("ring_available");
	private static final Identifier RING_LEARNED = NenTreePage.texture("ring_learned");
	private static final Identifier RING_ACTIVE = NenTreePage.texture("ring_active");

	private record Node(NenType branch, NenUpgrade upgrade, int x, int y, boolean active, String icon) {
		int radius() {
			return active ? ACTIVE_RADIUS : PASSIVE_RADIUS;
		}
	}

	private static final Map<NenUpgrade, String> ICONS = new EnumMap<>(Map.ofEntries(
			Map.entry(NenUpgrade.HATSU_QUAKE, "burst"), Map.entry(NenUpgrade.HATSU_VITALITY, "heart"), Map.entry(NenUpgrade.HATSU_IMPACT, "fist"),
			Map.entry(NenUpgrade.HATSU_PROPERTY, "wave"), Map.entry(NenUpgrade.HATSU_PERSISTENT, "timer"), Map.entry(NenUpgrade.HATSU_ADAPTATION, "shield"),
			Map.entry(NenUpgrade.HATSU_ARMOR, "shield_plus"), Map.entry(NenUpgrade.HATSU_STRUCTURE, "coat"), Map.entry(NenUpgrade.HATSU_MANIFEST, "hourglass"),
			Map.entry(NenUpgrade.HATSU_MARK, "target"), Map.entry(NenUpgrade.HATSU_READING, "compass"), Map.entry(NenUpgrade.HATSU_MEMORY, "rune"),
			Map.entry(NenUpgrade.HATSU_COMMAND, "mask"), Map.entry(NenUpgrade.HATSU_BOND, "expand"), Map.entry(NenUpgrade.HATSU_ORDER, "moon"),
			Map.entry(NenUpgrade.HATSU_BLAST, "star"), Map.entry(NenUpgrade.HATSU_COHESION, "gauge"), Map.entry(NenUpgrade.HATSU_SWIFT, "step")));

	private static final List<Node> NODES = nodes();

	private final NenScreen screen;

	NenHatsuPage(NenScreen screen) {
		this.screen = screen;
	}

	private static Identifier texture(String name) {
		return HxHMod.id("textures/gui/hatsu/" + name + ".png");
	}

	private static Identifier branchTexture(NenType branch) {
		return texture("branch_" + branch.getSerializedName());
	}

	/** Cor forte do galho, a mesma das imagens de tools/gen_hatsu_art.py. */
	private static int color(NenType branch) {
		return switch (branch) {
			case ENHANCEMENT -> 0xFFFF5A4A;
			case TRANSMUTATION -> 0xFFFF66E0;
			case CONJURATION -> 0xFF4FE3FF;
			case SPECIALIZATION -> 0xFFB86CFF;
			case MANIPULATION -> 0xFF5CFF7A;
			case EMISSION -> 0xFFFFD84A;
			case NONE -> TEXT;
		};
	}

	private static int[] tip(int branchIndex, int distance) {
		double angle = Math.toRadians(-90 + 60 * branchIndex);
		return new int[] {CENTER_X + (int) Math.round(Math.cos(angle) * distance), CENTER_Y + (int) Math.round(Math.sin(angle) * distance)};
	}

	private static List<Node> nodes() {
		List<Node> nodes = new ArrayList<>();
		for (int i = 0; i < NenAffinity.HEXAGON.size(); i++) {
			NenType branch = NenAffinity.HEXAGON.get(i);
			List<NenUpgrade> upgrades = NenUpgrade.ofBranch(branch);
			for (int j = 0; j < upgrades.size(); j++) {
				int[] point = tip(i, NODE_DISTANCE[j]);
				nodes.add(new Node(branch, upgrades.get(j), point[0], point[1], j == 0, ICONS.get(upgrades.get(j))));
			}
		}
		return nodes;
	}

	/** Posicao na tela de um no (para os testes). */
	public static float[] screenPoint(NenUpgrade upgrade, int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		Node node = NODES.stream().filter(candidate -> candidate.upgrade() == upgrade).findFirst().orElseThrow();
		return NenTreePage.screenPoint(node.x(), node.y(), areaLeft, areaTop, areaWidth, areaHeight);
	}

	/** Forca do brilho do galho: 1 no tipo inato, caindo com a afinidade ate sumir. */
	public static float glow(int percent) {
		return percent >= 100 ? 1 : GLOW_SCALE * (float) Math.pow(percent / 100.0, GLOW_EXPONENT);
	}

	private static NenType equipped() {
		var player = Minecraft.getInstance().player;
		return player == null ? NenType.NONE : player.getAttachedOrCreate(ModAttachments.HATSU_STATE).equipped();
	}

	private static boolean isEquipped(NenData data, Node node) {
		return node.active() && node.branch() == equipped() && data.has(node.upgrade());
	}

	private static boolean blocked(NenData data, NenType branch) {
		return data.revealed() && NenAffinity.blocked(data.type(), branch);
	}

	void render(GuiGraphicsExtractor graphics, Font font, NenData data, int areaLeft, int areaTop, int areaWidth,
			int areaHeight, int mouseX, int mouseY) {
		float scale = NenTreePage.scale(areaWidth, areaHeight);
		float originX = areaLeft + (areaWidth - NenTreePage.WIDTH * scale) / 2;
		float originY = areaTop + (areaHeight - NenTreePage.HEIGHT * scale) / 2;
		float treeX = (mouseX - originX) / scale;
		float treeY = (mouseY - originY) / scale;
		float pulse = 0.5F + 0.5F * Mth.sin((float) (Util.getMillis() % PULSE_MILLIS) / PULSE_MILLIS * Mth.TWO_PI);

		Node hovered = null;
		for (Node node : NODES) {
			if (Math.hypot(treeX - node.x(), treeY - node.y()) <= node.radius() + 1) {
				hovered = node;
			}
		}
		boolean centerHovered = hovered == null && Math.hypot(treeX - CENTER_X, treeY - CENTER_Y) <= CENTER_RADIUS;

		graphics.pose().pushMatrix();
		graphics.pose().translate(originX, originY);
		graphics.pose().scale(scale, scale);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, 0, 0, NenTreePage.WIDTH, NenTreePage.HEIGHT, 1280, 600, 1280, 600);
		drawBranches(graphics, data, pulse);
		drawCenter(graphics, data, pulse);
		for (Node node : NODES) {
			drawNode(graphics, data, node, node == hovered);
		}
		graphics.pose().popMatrix();

		// Por cima, fora da escala: texto nitido
		drawLabels(graphics, font, data, originX, originY, scale);
		drawCorners(graphics, font, data, originX, originY, scale);
		if (hovered != null) {
			List<Component> texts = details(data, hovered);
			tooltip(graphics, font, texts, originX + hovered.x() * scale, originY + hovered.y() * scale, hovered.radius() * scale,
					areaLeft, areaTop, areaWidth, areaHeight);
		} else if (centerHovered) {
			tooltip(graphics, font, table(data), originX + CENTER_X * scale, originY + CENTER_Y * scale, CENTER_RADIUS * scale,
					areaLeft, areaTop, areaWidth, areaHeight);
		}

		// Areas de clique: os nos de afinidade zero nem entram
		for (Node node : NODES) {
			if (blocked(data, node.branch())) {
				continue;
			}
			int size = (int) ((node.radius() * 2 + 2) * scale);
			int nodeX = (int) (originX + node.x() * scale) - size / 2;
			int nodeY = (int) (originY + node.y() * scale) - size / 2;
			screen.addClickable(nodeX, nodeY, size, size, true, () -> click(data, node));
		}
	}

	private static void click(NenData data, Node node) {
		if (data.has(node.upgrade())) {
			if (node.active()) {
				ClientPlayNetworking.send(new NenPayloads.EquipHatsu(node.branch()));
			}
		} else if (data.upgradeProblem(node.upgrade()) == null) {
			ClientPlayNetworking.send(new NenPayloads.BuyUpgrade(node.upgrade()));
		}
	}

	// ---------------------------------------------------------------- Hexagono

	private static void drawBranches(GuiGraphicsExtractor graphics, NenData data, float pulse) {
		if (!data.revealed()) {
			return;
		}
		for (NenType branch : NenAffinity.HEXAGON) {
			int percent = NenAffinity.percent(data.type(), branch);
			if (percent == 0) {
				// O galho inteiro vira sombra
				drawBranch(graphics, branch, DEAD_BRANCH);
			} else {
				drawBranch(graphics, branch, alpha(glow(percent)) | 0xFFFFFF);
				if (percent == 100) {
					// O tipo inato brilha mais e pulsa
					drawBranch(graphics, branch, alpha(0.35F + 0.4F * pulse) | 0xFFFFFF);
				}
			}
		}
	}

	private static int alpha(float alpha) {
		return (int) (Mth.clamp(alpha, 0, 1) * 255) << 24;
	}

	private static void drawBranch(GuiGraphicsExtractor graphics, NenType branch, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, branchTexture(branch), CENTER_X - BRANCH_WIDTH / 2, CENTER_Y - BRANCH_HEIGHT / 2, 0, 0,
				BRANCH_WIDTH, BRANCH_HEIGHT, BRANCH_TEXTURE_WIDTH, BRANCH_TEXTURE_HEIGHT, BRANCH_TEXTURE_WIDTH, BRANCH_TEXTURE_HEIGHT, color);
	}

	private static void drawCenter(GuiGraphicsExtractor graphics, NenData data, float pulse) {
		int color = data.revealed() ? color(data.type()) : DIM;
		int glowSize = CENTER_RADIUS * 3;
		NenTreePage.image(graphics, CENTER_GLOW, CENTER_X - glowSize / 2, CENTER_Y - glowSize / 2, glowSize, 256,
				alpha(data.revealed() ? 0.55F + 0.35F * pulse : 0.2F) | (color & 0xFFFFFF));
		int kanji = 34;
		NenTreePage.image(graphics, KANJI, CENTER_X - kanji / 2, CENTER_Y - kanji / 2, kanji, 96, data.revealed() ? 0xFFFFFFFF : ICON_LOCKED);
	}

	private void drawNode(GuiGraphicsExtractor graphics, NenData data, Node node, boolean hover) {
		boolean owned = data.has(node.upgrade());
		boolean dead = blocked(data, node.branch());
		boolean available = !owned && data.upgradeProblem(node.upgrade()) == null;
		boolean equipped = isEquipped(data, node);
		int size = node.radius() * 64 / 24;
		int left = node.x() - size / 2;
		int top = node.y() - size / 2;
		Identifier glow = equipped ? GLOW_ACTIVE : owned ? GLOW_LEARNED : available || (hover && !dead) ? GLOW_AVAILABLE : null;
		if (glow != null) {
			NenTreePage.image(graphics, glow, left, top, size, 128, 0xFFFFFFFF);
		}
		Identifier ring = equipped ? RING_ACTIVE : owned ? RING_LEARNED : available ? RING_AVAILABLE : RING_LOCKED;
		NenTreePage.image(graphics, ring, left, top, size, 128, dead ? RING_DEAD : 0xFFFFFFFF);
		if (hover && !dead) {
			NenTreePage.image(graphics, RING_AVAILABLE, left, top, size, 128, 0x60FFFFFF);
		}
		int iconSize = node.radius() * 2 - 6;
		int icon = dead ? ICON_DEAD : owned ? color(node.branch()) : available ? 0xFFFFFFFF : ICON_LOCKED;
		NenTreePage.image(graphics, HxHMod.id("textures/gui/tree/icon/" + node.icon() + ".png"), node.x() - iconSize / 2, node.y() - iconSize / 2,
				iconSize, 48, icon);
	}

	// ---------------------------------------------------------------- Textos

	/** Nome e afinidade ao lado da ponta de cada galho. */
	private void drawLabels(GuiGraphicsExtractor graphics, Font font, NenData data, float originX, float originY, float scale) {
		int lineHeight = font.lineHeight + 1;
		for (int i = 0; i < NenAffinity.HEXAGON.size(); i++) {
			NenType branch = NenAffinity.HEXAGON.get(i);
			int[] point = tip(i, NODE_DISTANCE[NODE_DISTANCE.length - 1]);
			int percent = NenAffinity.percent(data.type(), branch);
			int color = !data.revealed() ? DIM : percent == 0 ? DEAD : percent == 100 ? color(branch) : mix(color(branch), DIM, percent);
			Component name = screen.panelFont(branch.displayName().copy().withColor(color & 0xFFFFFF));
			Component value = screen.panelFont(!data.revealed() ? Component.literal("?") : percent == 100
					? Component.translatable("screen.hxh.nen.hatsu.innate_mark", percent) : Component.literal(percent + "%"));
			// O nome do galho de baixo vai para a esquerda: a direita fica a dica
			boolean onLeft = i >= NenAffinity.HEXAGON.size() / 2;
			int edge = (int) ((PASSIVE_RADIUS + 1) * scale) + LABEL_GAP;
			int x = (int) (originX + point[0] * scale) + (onLeft ? -edge : edge);
			int y = (int) (originY + point[1] * scale) - lineHeight;
			graphics.text(font, name, onLeft ? x - font.width(name) : x, y, color, true);
			graphics.text(font, value, onLeft ? x - font.width(value) : x, y + lineHeight, color, true);
		}
	}

	/** Mistura a cor do galho com o cinza: quanto menor a afinidade, mais cinza. */
	private static int mix(int color, int gray, int percent) {
		return 0xFF000000 | ((int) Mth.lerp(percent / 100.0F, gray >> 16 & 255, color >> 16 & 255) << 16)
				| ((int) Mth.lerp(percent / 100.0F, gray >> 8 & 255, color >> 8 & 255) << 8)
				| (int) Mth.lerp(percent / 100.0F, gray & 255, color & 255);
	}

	/** Tipo inato, Hatsu na tecla, pontos e a dica, nos quatro cantos da area. */
	private void drawCorners(GuiGraphicsExtractor graphics, Font font, NenData data, float originX, float originY, float scale) {
		int lineHeight = font.lineHeight + 1;
		int left = (int) (originX + TEXT_MARGIN * scale);
		int right = (int) (originX + (NenTreePage.WIDTH - TEXT_MARGIN) * scale);
		int top = (int) (originY + TEXT_MARGIN * scale);
		int bottom = (int) (originY + (NenTreePage.HEIGHT - TEXT_MARGIN) * scale) - font.lineHeight;

		graphics.text(font, screen.panelFont(Component.translatable("screen.hxh.nen.hatsu.innate")), left, top, DIM, true);
		graphics.text(font, screen.panelFont(data.revealed() ? data.type().displayName() : Component.translatable("screen.hxh.nen.hatsu.unknown")),
				left, top + lineHeight, data.revealed() ? color(data.type()) : TEXT, true);

		Optional<HatsuAbility> ability = HatsuAbility.of(equipped()).filter(equipped -> data.has(equipped.upgrade()));
		Component label = screen.panelFont(Component.translatable("screen.hxh.nen.hatsu.equipped"));
		Component equipped = screen.panelFont(ability.map(value -> value.upgrade().displayName().copy())
				.orElseGet(() -> Component.translatable("screen.hxh.nen.hatsu.none")));
		Component key = screen.panelFont(Component.literal("[").append(HxHModClient.USE_HATSU.getTranslatedKeyMessage()).append("]"));
		graphics.text(font, label, right - font.width(label), top, DIM, true);
		graphics.text(font, equipped, right - font.width(equipped), top + lineHeight, ability.isPresent() ? color(ability.get().branch()) : TEXT, true);
		graphics.text(font, key, right - font.width(key), top + lineHeight * 2, GOLD, true);

		NenArt.shared(graphics, "points_icon", left, bottom - 5);
		graphics.text(font, screen.panelFont(Component.translatable("screen.hxh.nen.hatsu.points", data.pointsAvailable())), left + 20, bottom, TEXT, true);

		Component hint = screen.panelFont(Component.translatable("screen.hxh.nen.hatsu.hint"));
		graphics.text(font, hint, right - font.width(hint), bottom, DIM, true);
	}

	private void tooltip(GuiGraphicsExtractor graphics, Font font, List<Component> texts, float x, float y, float radius,
			int areaLeft, int areaTop, int areaWidth, int areaHeight) {
		int width = Math.min(TOOLTIP_WIDTH, areaWidth / 2);
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (Component text : texts) {
			NenTreePage.addLines(font, screen.panelFont(text), text.getStyle().getColor() != null ? text.getStyle().getColor().getValue() | 0xFF000000 : TEXT,
					width - 12, lines, colors);
		}
		NenTreePage.tooltipBox(graphics, font, lines, colors, width, x, y, radius, areaLeft, areaTop, areaWidth, areaHeight);
	}

	/** O que o centro do hexagono mostra: as afinidades do tipo inato. */
	private static List<Component> table(NenData data) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.hxh.nen.hatsu.table").withColor(GOLD));
		if (!data.revealed()) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.reveal").withColor(DIM));
			return lines;
		}
		for (NenType branch : NenAffinity.HEXAGON) {
			int percent = NenAffinity.percent(data.type(), branch);
			lines.add(Component.translatable("screen.hxh.nen.hatsu.table_row", branch.displayName().getString(), percent).withColor(percent == 0 ? DEAD : color(branch)));
		}
		return lines;
	}

	private static List<Component> details(NenData data, Node node) {
		NenUpgrade upgrade = node.upgrade();
		int percent = NenAffinity.percent(data.type(), node.branch());
		List<Component> lines = new ArrayList<>();
		lines.add(upgrade.displayName().copy().withColor(GOLD));
		lines.add((data.revealed() ? Component.translatable("screen.hxh.nen.hatsu.branch", node.branch().displayName(), percent)
				: Component.translatable("screen.hxh.nen.hatsu.branch_unknown", node.branch().displayName())).withColor(DIM));
		lines.add(upgrade.description().copy());
		if (blocked(data, node.branch())) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.blocked").withColor(BAD));
			return lines;
		}
		if (!data.revealed()) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.reveal").withColor(DIM));
			return lines;
		}
		lines.add(Component.translatable("screen.hxh.nen.hatsu.power", percent).withColor(color(node.branch())));
		HatsuAbility.of(upgrade).ifPresent(ability -> addStats(lines, data, ability));
		if (data.has(upgrade)) {
			lines.add(Component.translatable("screen.hxh.nen.owned").withColor(GREEN));
			if (node.active()) {
				lines.add((isEquipped(data, node)
						? Component.translatable("screen.hxh.nen.hatsu.on_key", HxHModClient.USE_HATSU.getTranslatedKeyMessage())
						: Component.translatable("screen.hxh.nen.hatsu.equip")).withColor(DIM));
			}
			return lines;
		}
		NenData.UnlockProblem problem = data.upgradeProblem(upgrade);
		lines.add(Component.translatable("screen.hxh.nen.upgrade_requirement", upgrade.minLevel(), upgrade.costFor(data.type()))
				.withColor(problem == null ? TEXT : BAD));
		if (upgrade.requires() != null && !data.has(upgrade.requires())) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.needs_previous", upgrade.requires().displayName()).withColor(BAD));
		}
		lines.add(Component.translatable(problem == null ? "screen.hxh.nen.tree.buy" : "screen.hxh.nen.tree.cannot_buy").withColor(DIM));
		return lines;
	}

	/** Os numeros reais do Hatsu ativo, ja com a afinidade e as passivas. */
	private static void addStats(List<Component> lines, NenData data, HatsuAbility ability) {
		if (ability.damage(data) > 0) {
			lines.add(Component.translatable(ability == HatsuAbility.ARMOR ? "screen.hxh.nen.hatsu.extra_health" : "screen.hxh.nen.hatsu.damage",
					number(ability.damage(data))).withColor(TEXT));
		}
		if (ability == HatsuAbility.PROPERTY) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.bonus", Math.round(HatsuAbility.propertyBonus(data) * 100)).withColor(TEXT));
		} else if (ability == HatsuAbility.MARK) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.bonus", Math.round(HatsuAbility.markBonus(data) * 100)).withColor(TEXT));
		} else if (ability == HatsuAbility.BLAST) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.range", number(HatsuAbility.blastRange(data))).withColor(TEXT));
		}
		if (ability.radius(data) > 0) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.area", number(ability.radius(data))).withColor(TEXT));
		}
		if (ability.durationTicks(data) > 0) {
			lines.add(Component.translatable("screen.hxh.nen.hatsu.duration", number(ability.durationTicks(data) / 20.0)).withColor(TEXT));
		}
	}

	private static String number(double value) {
		return String.format(Locale.ROOT, "%.1f", value).replace(".", I18n.get("screen.hxh.nen.hatsu.decimal"));
	}
}
