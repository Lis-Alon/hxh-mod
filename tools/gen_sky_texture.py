"""Gera o fundo da pagina de Nivel: quase preto, com nevoa fraca. As estrelas sao animadas em StarrySky.java."""
import os
import random

from PIL import Image, ImageDraw, ImageFilter

WIDTH, HEIGHT = 1024, 576
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'hxh', 'textures', 'gui', 'sky.png')

random.seed(7)
image = Image.new('RGB', (WIDTH, HEIGHT), (6, 7, 11))
fog = Image.new('RGB', (WIDTH, HEIGHT), (0, 0, 0))
draw = ImageDraw.Draw(fog)
for _ in range(26):
    x = random.randint(-100, WIDTH + 100)
    y = random.randint(-80, HEIGHT + 80)
    w = random.randint(120, 380)
    h = random.randint(60, 200)
    tone = random.choice([(26, 24, 26), (20, 22, 30), (30, 25, 20), (16, 18, 26)])
    draw.ellipse((x - w, y - h, x + w, y + h), fill=tone)
fog = fog.filter(ImageFilter.GaussianBlur(70))

pixels = image.load()
fog_pixels = fog.load()
for y in range(HEIGHT):
    for x in range(WIDTH):
        # Bordas mais escuras
        dx = (x / WIDTH - 0.5) * 2
        dy = (y / HEIGHT - 0.5) * 2
        shade = max(0.35, 1 - 0.55 * (dx * dx + dy * dy))
        base = pixels[x, y]
        extra = fog_pixels[x, y]
        pixels[x, y] = tuple(int((base[i] + extra[i]) * shade + random.random()) for i in range(3))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
image.save(OUT, optimize=True)
print('ok', os.path.getsize(OUT))
