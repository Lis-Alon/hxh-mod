package com.hxhmod.client;

import com.hxhmod.HxHMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Fundo da pagina de Nivel: ceu escuro com estrelas que derivam devagar e cintilam.
 * Estrelas perto (maiores) andam mais rapido que as distantes, o que da profundidade.
 */
final class StarrySky {
	private static final Identifier TEXTURE = HxHMod.id("textures/gui/sky.png");
	private static final int TEXTURE_WIDTH = 1024;
	private static final int TEXTURE_HEIGHT = 576;
	private static final int STAR_COUNT = 260;

	/** Posicao em fracao da tela, velocidade em fracao por segundo. */
	private record Star(float x, float y, float speed, float phase, float twinkleSpeed, float brightness, int size, int tint) {
	}

	private static final Star[] STARS = createStars();

	private StarrySky() {
	}

	private static Star[] createStars() {
		Random random = new Random(1998L);
		Star[] stars = new Star[STAR_COUNT];
		for (int i = 0; i < STAR_COUNT; i++) {
			float depth = random.nextFloat();
			depth *= depth;
			int size = depth > 0.72F ? 3 : depth > 0.3F ? 2 : 1;
			int tint = random.nextInt(4) == 0 ? 0xE8C98A : random.nextInt(4) == 0 ? 0xAFC4F0 : 0xF2EDE0;
			stars[i] = new Star(random.nextFloat(), random.nextFloat(), 0.002F + depth * 0.010F,
					random.nextFloat() * 6.28F, 0.5F + random.nextFloat() * 1.8F, 0.35F + depth * 0.65F, size, tint);
		}
		return stars;
	}

	static void render(GuiGraphicsExtractor graphics, int width, int height, int guiScale, long millis) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0, 0, width, height,
				TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

		// Desenha em pixels de verdade, para as estrelas nao ficarem quadradas com a escala da interface
		int pixelWidth = width * guiScale;
		int pixelHeight = height * guiScale;
		float seconds = (millis % 3_600_000L) / 1000F;
		graphics.pose().pushMatrix();
		graphics.pose().scale(1F / guiScale, 1F / guiScale);
		for (Star star : STARS) {
			float x = wrap(star.x - star.speed * seconds);
			float y = wrap(star.y + star.speed * 0.25F * seconds);
			float twinkle = 0.55F + 0.45F * (float) Math.sin(seconds * star.twinkleSpeed + star.phase);
			int alpha = (int) (255 * star.brightness * twinkle);
			drawStar(graphics, (int) (x * pixelWidth), (int) (y * pixelHeight), star.size, alpha << 24 | star.tint, (alpha / 3) << 24 | star.tint);
		}
		graphics.pose().popMatrix();
	}

	private static float wrap(float value) {
		return value - (float) Math.floor(value);
	}

	/** As maiores ganham uma cruz fraca em volta, como um brilho. */
	private static void drawStar(GuiGraphicsExtractor graphics, int x, int y, int size, int color, int glow) {
		if (size >= 3) {
			graphics.fill(x - 2, y, x + 4, y + 2, glow);
			graphics.fill(x, y - 2, x + 2, y + 4, glow);
			graphics.fill(x, y, x + 2, y + 2, color);
		} else {
			graphics.fill(x, y, x + size, y + size, color);
		}
	}
}
