package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.HatsuAbility;
import com.hxhmod.nen.HatsuActions;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * HUD de combate no canto superior esquerdo: retrato, vida, Nen e tecnicas aprendidas. As pecas vem de
 * tools/gen_hud_art.py, em uma versao por escala de interface; as medidas das barras precisam bater com as de la.
 */
public final class NenHud {
	private static final int X = 3;
	private static final int Y = 3;

	private static final int BAR_X = 34;
	private static final int HP_Y = 4;
	private static final int HP_WIDTH = 104;
	private static final int HP_BOTTOM_WIDTH = 99;
	private static final int HP_HEIGHT = 11;
	private static final int NEN_Y = 17;
	private static final int NEN_WIDTH = 78;
	private static final int NEN_BOTTOM_WIDTH = 74;
	private static final int NEN_HEIGHT = 8;
	private static final int ABSORB_HEIGHT = 3;
	private static final int NEN_FLOW_PER_SECOND = 18;

	private static final int FACE_X = 4;
	private static final int FACE_SIZE = 24;
	private static final int BADGE_Y = 30;
	private static final int XP_X = 5;
	private static final int XP_Y = 41;
	private static final int XP_WIDTH = 18;
	private static final int CHIP_X = 110;
	private static final int CHIP_Y = 16;
	private static final int HUD_BOTTOM = 44;

	private static final int ICON_X = 34;
	private static final int ICON_Y = 26;
	private static final int ICON_SIZE = 14;
	private static final int ICON_STEP = 13;
	private static final int ICONS_PER_ROW = 8;
	private static final int HALO_SIZE = 22;
	private static final int INACTIVE_TINT = 0xFF949494;

	private static final int LINE_WIDTH = 82;
	private static final int LINE_BAR_HEIGHT = 5;
	private static final int UNKNOWN_TYPE_COLOR = 0xFFE8E4D8;
	private static final int DORMANT_COLOR = 0xFF5A5A66;
	private static final int XP_COLOR = 0xFFE8BC62;
	private static final int LEVEL_COLOR = 0xFFF8E2A4;
	private static final int KO_COLOR = 0xFFE05A3A;
	private static final int COOLDOWN_COLOR = 0xFFB8C4D0;

	private static final GyoApplications.Action[] GYO_ORDER = {
			GyoApplications.Action.PUNCH, GyoApplications.Action.GUARD, GyoApplications.Action.JUMP, GyoApplications.Action.DASH};
	private static final List<Technique> ICON_ORDER = List.of(Technique.TEN, Technique.ZETSU, Technique.REN, Technique.GYO, Technique.IN,
			Technique.SHU, Technique.KO, Technique.RYU, Technique.KEN, Technique.EN, Technique.HATSU);

	private static final DamageTrail TRAIL = new DamageTrail();
	private static float trailShown;
	private static long lastFrame;

	private NenHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || player.isSpectator() || minecraft.gui.screen() instanceof NenScreen) {
			return;
		}
		NenData data = player.getAttached(ModAttachments.NEN);
		boolean awakened = data != null && data.awakened();
		// Antes do despertar a HUD so substitui os coracoes, que o modo criativo tambem nao mostra
		if (!awakened && !minecraft.gameMode.canHurtPlayer()) {
			return;
		}

		float maxHealth = Math.max(1.0F, player.getMaxHealth());
		float health = Mth.clamp(player.getHealth() / maxHealth, 0.0F, 1.0F);
		long now = System.nanoTime();
		float seconds = lastFrame == 0L ? 0.0F : Math.min(0.25F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;
		trailShown = TRAIL.update(health, seconds);

		image(graphics, awakened ? "frame_nen" : "frame_basic", X, Y, 144, awakened ? 28 : 18, -1);
		renderBars(graphics, player, awakened ? data : null, maxHealth, health, now);
		renderPortrait(graphics, player, awakened ? data : null);
		renderArmor(graphics, minecraft.font, player);
		if (awakened) {
			renderIcons(graphics, data, now);
			renderLines(graphics, minecraft, player, data);
		}
	}

	/** Ate onde o rastro de dano foi desenhado no ultimo quadro, de 0 a 1. */
	public static float trailShown() {
		return trailShown;
	}

	/** As tecnicas com icone na HUD, na ordem em que aparecem: so as ja aprendidas. */
	public static List<Technique> icons(NenData data) {
		return ICON_ORDER.stream().filter(data::isUnlocked).toList();
	}

	/**
	 * Largura de uma linha da barra. A ponta e inclinada, mas a inclinacao diminui junto com a barra,
	 * entao 0 continua vazio e 1 enche o trilho inteiro em toda linha.
	 */
	public static int rowWidth(float fraction, int bottomWidth, int topWidth, int row, int rows) {
		float width = bottomWidth + (topWidth - bottomWidth) * (rows - 1 - row) / (float) Math.max(1, rows - 1);
		return Math.round(width * Mth.clamp(fraction, 0.0F, 1.0F));
	}

	private static void renderBars(GuiGraphicsExtractor graphics, LocalPlayer player, NenData data, float maxHealth, float health, long now) {
		Font font = Minecraft.getInstance().font;
		String hpTexture = player.hasEffect(MobEffects.WITHER) ? "bar_wither" : player.hasEffect(MobEffects.POISON) ? "bar_poison" : "bar_hp";
		bar(graphics, "bar_trail", HP_Y, HP_WIDTH, HP_BOTTOM_WIDTH, HP_HEIGHT, trailShown, 0, HP_WIDTH, HP_HEIGHT);
		bar(graphics, hpTexture, HP_Y, HP_WIDTH, HP_BOTTOM_WIDTH, HP_HEIGHT, health, 0, HP_WIDTH, HP_HEIGHT);
		if (player.getAbsorptionAmount() > 0.0F) {
			bar(graphics, "bar_absorb", HP_Y, HP_WIDTH, HP_BOTTOM_WIDTH, HP_HEIGHT, player.getAbsorptionAmount() / maxHealth, 0, HP_WIDTH, ABSORB_HEIGHT);
		}
		String hpText = Mth.ceil(player.getHealth()) + "/" + Mth.ceil(maxHealth);
		outlined(graphics, font, hpText, X + BAR_X + HP_BOTTOM_WIDTH - 4 - font.width(hpText), Y + HP_Y + 2);

		if (data != null) {
			int period = NEN_WIDTH * NenArt.scale();
			int offset = period - (int) (now / 1_000_000L * NEN_FLOW_PER_SECOND * NenArt.scale() / 1000L % period);
			float aura = data.maxAura() > 0 ? data.aura() / (float) data.maxAura() : 0.0F;
			bar(graphics, "bar_nen", NEN_Y, NEN_WIDTH, NEN_BOTTOM_WIDTH, NEN_HEIGHT, aura, offset, NEN_WIDTH * 2, NEN_HEIGHT);
			String auraText = data.aura() + "/" + data.maxAura();
			outlined(graphics, font, auraText, X + BAR_X + NEN_BOTTOM_WIDTH - 4 - font.width(auraText), Y + NEN_Y);
		}
	}

	/** Numero branco com contorno preto, para ler tanto em cima da barra cheia quanto do trilho vazio. */
	private static void outlined(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
		graphics.text(font, text, x - 1, y, 0xFF000000, false);
		graphics.text(font, text, x + 1, y, 0xFF000000, false);
		graphics.text(font, text, x, y - 1, 0xFF000000, false);
		graphics.text(font, text, x, y + 1, 0xFF000000, false);
		graphics.text(font, text, x, y, 0xFFFFFFFF, false);
	}

	/** Desenha a barra linha a linha, em pixels da textura, para a ponta ficar inclinada como o trilho. */
	private static void bar(GuiGraphicsExtractor graphics, String key, int y, int topWidth, int bottomWidth, int height, float fraction,
			int uOffset, int textureWidth, int shownHeight) {
		if (fraction <= 0.0F) {
			return;
		}
		int scale = NenArt.scale();
		int rows = height * scale;
		Identifier texture = texture(key, scale);
		graphics.pose().pushMatrix();
		graphics.pose().scale(1.0F / scale, 1.0F / scale);
		for (int row = 0; row < shownHeight * scale; row++) {
			int width = rowWidth(fraction, bottomWidth * scale, topWidth * scale, row, rows);
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture, (X + BAR_X) * scale, (Y + y) * scale + row, uOffset, row, width, 1,
					textureWidth * scale, height * scale);
		}
		graphics.pose().popMatrix();
	}

	private static void renderPortrait(GuiGraphicsExtractor graphics, LocalPlayer player, NenData data) {
		if (data != null) {
			image(graphics, "badge", X + 2, Y + BADGE_Y, 28, 14, -1);
			Font font = Minecraft.getInstance().font;
			String level = String.valueOf(data.level());
			graphics.text(font, level, X + 16 - font.width(level) / 2, Y + BADGE_Y + 2, LEVEL_COLOR, true);
			int xp = data.maxLevel() ? XP_WIDTH : XP_WIDTH * data.xpIntoLevel() / NenRules.xpToNext(data.level());
			graphics.fill(X + 2 + XP_X, Y + XP_Y, X + 2 + XP_X + xp, Y + XP_Y + 2, XP_COLOR);
		}
		image(graphics, "portrait", X, Y, 32, 32, -1);
		// O tipo de Nen virou a cor em volta do rosto
		int ring = data == null ? DORMANT_COLOR : data.revealed() ? 0xFF000000 | data.type().barColor() : UNKNOWN_TYPE_COLOR;
		graphics.fill(X + FACE_X - 1, Y + FACE_X - 1, X + FACE_X + FACE_SIZE + 1, Y + FACE_X + FACE_SIZE + 1, ring);
		PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), X + FACE_X, Y + FACE_X, FACE_SIZE);
	}

	/** A armadura deixou de ter barra propria: virou um escudo com o numero, so quando o jogador usa alguma. */
	private static void renderArmor(GuiGraphicsExtractor graphics, Font font, LocalPlayer player) {
		int armor = player.getArmorValue();
		if (armor > 0) {
			image(graphics, "chip", X + CHIP_X, Y + CHIP_Y, 30, 10, -1);
			graphics.text(font, String.valueOf(armor), X + CHIP_X + 13, Y + CHIP_Y + 1, 0xFFFFFFFF, true);
		}
	}

	private static void renderIcons(GuiGraphicsExtractor graphics, NenData data, long now) {
		float seconds = (now / 1_000_000L % 100_000L) / 1000.0F;
		int alpha = (int) (255 * (0.65F + 0.35F * Mth.sin(seconds * 5.0F)));
		List<Technique> icons = icons(data);
		// Os brilhos vao antes, para nao cobrirem os icones vizinhos
		for (int i = 0; i < icons.size(); i++) {
			if (data.isActive(icons.get(i))) {
				int margin = (HALO_SIZE - ICON_SIZE) / 2;
				image(graphics, "halo_" + icons.get(i).getSerializedName(), iconX(i) - margin, iconY(i) - margin, HALO_SIZE, HALO_SIZE, alpha << 24 | 0xFFFFFF);
			}
		}
		for (int i = 0; i < icons.size(); i++) {
			image(graphics, "icon_" + icons.get(i).getSerializedName(), iconX(i), iconY(i), ICON_SIZE, ICON_SIZE,
					data.isActive(icons.get(i)) ? -1 : INACTIVE_TINT);
		}
	}

	private static int iconX(int index) {
		return X + ICON_X + index % ICONS_PER_ROW * ICON_STEP;
	}

	private static int iconY(int index) {
		return Y + ICON_Y + index / ICONS_PER_ROW * ICON_STEP;
	}

	/** Carga do Ko, postura do Ryu e recargas do Gyo, em texto embaixo da HUD. */
	private static void renderLines(GuiGraphicsExtractor graphics, Minecraft minecraft, LocalPlayer player, NenData data) {
		int rows = (icons(data).size() + ICONS_PER_ROW - 1) / ICONS_PER_ROW;
		int nextY = Y + Math.max(HUD_BOTTOM, ICON_Y + (rows - 1) * ICON_STEP + ICON_SIZE) + 3;
		int x = X + 2;
		if (data.isActive(Technique.KO)) {
			graphics.text(minecraft.font, Component.translatable("nen.hxh.ko.charge"), x, nextY, 0xFFFFFFFF, true);
			nextY += minecraft.font.lineHeight + 1;
			graphics.fill(x - 1, nextY - 1, x + LINE_WIDTH + 1, nextY + LINE_BAR_HEIGHT + 1, 0xC0000000);
			graphics.fill(x, nextY, x + (int) (LINE_WIDTH * TechniqueControl.koCharge(data)), nextY + LINE_BAR_HEIGHT, KO_COLOR);
			nextY += LINE_BAR_HEIGHT + 3;
		}
		if (data.isActive(Technique.RYU)) {
			graphics.text(minecraft.font, Component.translatable("nen.hxh.ryu.stance."
					+ TechniqueControl.stance().name().toLowerCase(Locale.ROOT)), x, nextY, 0xFFFFFFFF, true);
			nextY += minecraft.font.lineHeight + 1;
		}
		for (GyoApplications.Action action : GYO_ORDER) {
			int ticks = GyoApplications.remaining(player, action);
			if (ticks > 0) {
				Component name = Component.translatable("nen.hxh.gyo.name." + action.name().toLowerCase(Locale.ROOT));
				graphics.text(minecraft.font, Component.translatable("nen.hxh.gyo.cooldown", name, (ticks + 19) / 20), x, nextY, COOLDOWN_COLOR, true);
				nextY += minecraft.font.lineHeight + 1;
			}
		}
		int hatsuTicks = HatsuActions.remaining(player);
		if (hatsuTicks > 0) {
			Optional<HatsuAbility> equipped = HatsuAbility.of(player.getAttachedOrCreate(ModAttachments.HATSU_STATE).equipped());
			if (equipped.isPresent()) {
				graphics.text(minecraft.font, Component.translatable("nen.hxh.gyo.cooldown", equipped.get().upgrade().displayName(), (hatsuTicks + 19) / 20),
						x, nextY, COOLDOWN_COLOR, true);
			}
		}
	}

	private static void image(GuiGraphicsExtractor graphics, String key, int x, int y, int width, int height, int color) {
		int scale = NenArt.scale();
		int textureWidth = width * scale;
		int textureHeight = height * scale;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture(key, scale), x, y, 0.0F, 0.0F, width, height,
				textureWidth, textureHeight, textureWidth, textureHeight, color);
	}

	private static Identifier texture(String key, int scale) {
		return HxHMod.id("textures/gui/hud/" + key + "_" + scale + ".png");
	}
}
