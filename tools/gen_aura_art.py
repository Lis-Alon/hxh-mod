"""
Gera as texturas da aura: faixa de energia, brilho, particula quadrada e a superficie pixelada do En.
Rode na raiz do projeto.
"""
import json
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = 'src/main/resources/assets/hxh/textures/aura'
random.seed(23)
os.makedirs(OUT, exist_ok=True)


def smooth(file):
    with open(file + '.mcmeta', 'w', encoding='utf-8') as out:
        json.dump({'texture': {'blur': True}}, out)


def wisp():
    """Faixa branca (o jogo tinge): borda fina e forte de um lado, veu suave no resto, estrias ao longo."""
    width, height = 256, 64
    streaks = [random.uniform(0.35, 1.0) for _ in range(16)]
    image = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    pixels = image.load()
    for x in range(width):
        # Soma de ondas com periodo inteiro: emenda quando a textura repete
        wave = 0.8 + 0.2 * math.sin(x / width * math.tau * 3) * math.sin(x / width * math.tau * 7 + 1)
        for y in range(height):
            v = y / (height - 1)
            edge = math.exp(-((v - 0.18) / 0.06) ** 2)
            second = 0.55 * math.exp(-((v - 0.82) / 0.07) ** 2)
            veil = 0.48 * math.sin(math.pi * v) ** 1.2
            streak = streaks[min(15, int(v * 16))]
            fade = min(1.0, v * 12, (1 - v) * 12)
            alpha = min(1.0, edge + second * wave + veil * streak * wave) * fade
            pixels[x, y] = (255, 255, 255, int(255 * alpha))
    image.save(OUT + '/wisp.png')
    smooth(OUT + '/wisp.png')


def rim():
    """Corte da chama, de dentro (v=0) para fora (v=1): veu que cresce, borda forte e um halo curto."""
    width, height = 256, 64
    noise = value_noise(width, height, 10, 8)
    image = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    pixels = image.load()
    for x in range(width):
        for y in range(height):
            v = y / (height - 1)
            n = noise(x, y)
            veil = 0.62 * (min(v, 0.74) / 0.74) ** 1.5 * (0.5 + 0.5 * n)
            edge = 0.5 * math.exp(-((v - 0.74) / 0.075) ** 2)
            halo = 0.32 * math.exp(-((v - 0.78) / 0.16) ** 2)
            fade = min(1.0, (1 - v) * 6)
            alpha = min(1.0, veil + edge * (0.7 + 0.3 * n) + halo) * fade
            pixels[x, y] = (255, 255, 255, int(255 * alpha))
    image.save(OUT + '/rim.png')
    smooth(OUT + '/rim.png')


def vignette(frame):
    """Chamas lentas nas bordas da tela para o Gyo: fumaca e fios curvos, com o miolo livre."""
    width, height = 640, 360
    random.seed(100 + frame)
    smoke = Image.new('L', (width, height), 0)
    draw = ImageDraw.Draw(smoke)

    def edge_point():
        side = random.choice('LLLRRRTB')
        if side == 'L':
            return random.uniform(-10, 110), random.uniform(0, height)
        if side == 'R':
            return width - random.uniform(-10, 110), random.uniform(0, height)
        if side == 'T':
            return random.uniform(0, width), random.uniform(-10, 60)
        return random.uniform(0, width), height - random.uniform(-10, 40)

    for _ in range(120):
        x, y = edge_point()
        radius = random.randint(14, 42)
        draw.ellipse((x - radius, y - radius * 1.4, x + radius, y + radius * 1.4), fill=random.randint(80, 170))
    smoke = smoke.filter(ImageFilter.GaussianBlur(14))

    lines = Image.new('L', (width * 2, height * 2), 0)
    draw = ImageDraw.Draw(lines)
    for _ in range(60):
        x, y = edge_point()
        x, y = x * 2, y * 2
        angle = random.uniform(0, math.tau)
        turn = random.uniform(-0.05, 0.05)
        points = []
        for _ in range(random.randint(30, 70)):
            points.append((x, y))
            angle += turn + random.uniform(-0.03, 0.03)
            x += math.cos(angle) * 5
            y += math.sin(angle) * 5
            # Curvas que vao para o miolo voltam para a borda
            middle = math.hypot((x - width) / width, (y - height) / height)
            if middle < 0.55:
                angle = math.atan2(y - height, x - width) + random.uniform(-0.7, 0.7)
        draw.line(points, fill=random.randint(120, 235), width=random.choice((2, 2, 3)))
    glow = lines.filter(ImageFilter.GaussianBlur(5)).point(lambda value: min(255, value * 2))
    lines = ImageChops.lighter(lines.filter(ImageFilter.GaussianBlur(0.9)), glow).resize((width, height), Image.LANCZOS)

    alpha = ImageChops.add(smoke, lines)
    # Protege o centro da tela, com uma queda suave
    falloff = Image.new('L', (width, height), 255)
    pixels = falloff.load()
    for y in range(height):
        for x in range(width):
            d = max(abs(x - width / 2) / (width / 2), abs(y - height / 2) / (height / 2) * 0.86, math.hypot((x - width / 2) / (width / 2), (y - height / 2) / (height / 2)) * 0.74)
            pixels[x, y] = int(255 * min(1.0, max(0.0, (d - 0.42) / 0.32)))
    alpha = ImageChops.multiply(alpha, falloff)
    result = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    result.putalpha(alpha)
    result.save(f'{OUT}/vignette_{frame}.png', optimize=True)
    smooth(f'{OUT}/vignette_{frame}.png')


def glow():
    """Halo redondo que fica atras do corpo."""
    size = 128
    image = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - size / 2 + 0.5, y - size / 2 + 0.5) / (size / 2)
            pixels[x, y] = (255, 255, 255, int(255 * max(0.0, 1 - d) ** 2.2))
    image.save(OUT + '/glow.png')
    smooth(OUT + '/glow.png')


def mote():
    """Particula quadrada com halo curto, no estilo dos blocos do jogo."""
    size = 64
    alpha = Image.new('L', (size, size), 0)
    pixels = alpha.load()
    for y in range(20, 44):
        for x in range(20, 44):
            pixels[x, y] = 255
    halo = alpha.filter(ImageFilter.GaussianBlur(8))
    pixels_halo = halo.load()
    result = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    out = result.load()
    for y in range(size):
        for x in range(size):
            out[x, y] = (255, 255, 255, max(pixels[x, y], int(pixels_halo[x, y] * 0.7)))
    result.save(OUT + '/mote.png')
    smooth(OUT + '/mote.png')


def value_noise(width, height, cells_x, cells_y):
    """Ruido que emenda nas duas direcoes."""
    grid = [[random.random() for _ in range(cells_x)] for _ in range(cells_y)]

    def sample(x, y):
        fx, fy = x / width * cells_x, y / height * cells_y
        ix, iy = int(fx), int(fy)
        tx, ty = fx - ix, fy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = grid[iy % cells_y][ix % cells_x]
        b = grid[iy % cells_y][(ix + 1) % cells_x]
        c = grid[(iy + 1) % cells_y][ix % cells_x]
        d = grid[(iy + 1) % cells_y][(ix + 1) % cells_x]
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty

    return sample


def dome():
    """Superficie do En em cinza (o jogo tinge): vidro com manchas claras em pixels grandes, sem filtro suave."""
    width, height = 128, 64
    coarse = value_noise(width, height, 8, 4)
    medium = value_noise(width, height, 20, 10)
    fine = value_noise(width, height, 48, 24)
    image = Image.new('RGBA', (width, height))
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            n = coarse(x, y) * 0.5 + medium(x, y) * 0.32 + fine(x, y) * 0.18
            if n > 0.62:
                pixels[x, y] = (255, 255, 255, 190)
            elif n > 0.55:
                pixels[x, y] = (225, 225, 225, 130)
            elif n > 0.47:
                pixels[x, y] = (190, 190, 190, 74)
            else:
                pixels[x, y] = (150, 150, 150, 44)
    image.save(OUT + '/en_dome.png')


wisp()
rim()
for frame in range(3):
    vignette(frame)
random.seed(23)
glow()
mote()
dome()
print('ok')
