package com.hxhmod.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hxhmod.HxHMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Imagens do painel geradas por tools/gen_panel_art.py. Cada uma existe em uma versao por escala de
 * interface, e a desenhada aqui cai pixel a pixel na tela. Textos em idioma sem imagem voltam a ser texto.
 */
final class NenArt {
	private static final Identifier MANIFEST = HxHMod.id("textures/gui/art/manifest.json");
	private static final int MAX_SCALE = 6;

	private record Size(int width, int height) {
	}

	private static Map<String, Size> sizes;

	private NenArt() {
	}

	private static Map<String, Size> sizes() {
		if (sizes == null) {
			sizes = new HashMap<>();
			Optional<net.minecraft.server.packs.resources.Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MANIFEST);
			if (resource.isPresent()) {
				try (Reader reader = resource.get().openAsReader()) {
					JsonObject all = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("sizes");
					all.entrySet().forEach(entry -> sizes.put(entry.getKey(), new Size(
							entry.getValue().getAsJsonArray().get(0).getAsInt(), entry.getValue().getAsJsonArray().get(1).getAsInt())));
				} catch (IOException | RuntimeException exception) {
					HxHMod.LOGGER.warn("Nao deu para ler as imagens do painel", exception);
				}
			}
		}
		return sizes;
	}

	private static String localized(String key) {
		return Minecraft.getInstance().getLanguageManager().getSelected() + "/" + key;
	}

	private static Identifier texture(String path, int scale) {
		return HxHMod.id("textures/gui/art/" + path + "_" + scale + ".png");
	}

	private static int scale() {
		return Math.clamp(Minecraft.getInstance().getWindow().getGuiScale(), 1, MAX_SCALE);
	}

	/** Largura da imagem deste texto no idioma atual, ou o valor dado se nao houver imagem. */
	static int width(String key, int fallback) {
		Size size = sizes().get(localized(key));
		return size != null ? size.width : fallback;
	}

	/** Desenha o texto em imagem; devolve false se este idioma nao tiver a imagem. */
	static boolean text(GuiGraphicsExtractor graphics, String key, int x, int y) {
		String path = localized(key);
		Size size = sizes().get(path);
		if (size == null) {
			return false;
		}
		image(graphics, path, size, x, y);
		return true;
	}

	/** Imagem usada em todos os idiomas. */
	static void shared(GuiGraphicsExtractor graphics, String key, int x, int y) {
		String path = "shared/" + key;
		Size size = sizes().get(path);
		if (size != null) {
			image(graphics, path, size, x, y);
		}
	}

	private static void image(GuiGraphicsExtractor graphics, String path, Size size, int x, int y) {
		int scale = scale();
		int texWidth = size.width * scale;
		int texHeight = size.height * scale;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture(path, scale), x, y, 0, 0, size.width, size.height,
				texWidth, texHeight, texWidth, texHeight);
	}

	/** Moldura esticada para o tamanho pedido: os cantos ficam intactos e so o miolo estica. */
	static void frame(GuiGraphicsExtractor graphics, String key, int x, int y, int width, int height, int corner) {
		String path = "shared/" + key;
		Size size = sizes().get(path);
		if (size == null) {
			return;
		}
		int scale = scale();
		Identifier texture = texture(path, scale);
		int[] destX = {x, x + corner, x + width - corner};
		int[] destW = {corner, width - 2 * corner, corner};
		int[] srcX = {0, corner, size.width - corner};
		int[] srcW = {corner, size.width - 2 * corner, corner};
		int[] destY = {y, y + corner, y + height - corner};
		int[] destH = {corner, height - 2 * corner, corner};
		int[] srcY = {0, corner, size.height - corner};
		int[] srcH = {corner, size.height - 2 * corner, corner};
		for (int column = 0; column < 3; column++) {
			for (int line = 0; line < 3; line++) {
				if (destW[column] > 0 && destH[line] > 0) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, texture, destX[column], destY[line], srcX[column] * scale, srcY[line] * scale,
							destW[column], destH[line], srcW[column] * scale, srcH[line] * scale, size.width * scale, size.height * scale);
				}
			}
		}
	}
}
