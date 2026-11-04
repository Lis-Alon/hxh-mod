"""
Gera as texturas da aura: faixa de energia, brilho, particula quadrada e a superficie pixelada do En.
Rode na raiz do projeto.
"""
import json
import math
import os
import random

from PIL import Image, ImageFilter

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


def flame():
    """Lencol macio do corpo das chamas: sem bordas marcadas, com estrias leves ao longo."""
    width, height = 256, 64
    noise = value_noise(width, height, 8, 12)
    image = Image.new('RGBA', (width, height), (255, 255, 255, 0))
    pixels = image.load()
    for x in range(width):
        for y in range(height):
            v = y / (height - 1)
            body = math.sin(math.pi * v) ** 1.6
            alpha = body * (0.55 + 0.45 * noise(x, y))
            pixels[x, y] = (255, 255, 255, int(255 * alpha))
    image.save(OUT + '/flame.png')
    smooth(OUT + '/flame.png')


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
flame()
glow()
mote()
dome()
print('ok')
