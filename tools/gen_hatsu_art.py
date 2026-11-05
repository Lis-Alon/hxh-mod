"""Gera as imagens da aba Hatsu: o fundo com o hexagono escuro e o brilho colorido de cada galho. Rode na raiz do projeto."""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join('src', 'main', 'resources', 'assets', 'hxh', 'textures', 'gui', 'hatsu')

# A area logica da aba tem 640x300; as imagens tem o dobro
SCALE = 2
BACK_SIZE = (1280, 600)
BRANCH_SIZE = (800, 600)
SUPER = 2

# Os galhos no sentido horario, a partir do topo, com as cores de NenHatsuPage
BRANCHES = [
    ('enhancement', (255, 90, 74)),
    ('transmutation', (255, 102, 224)),
    ('conjuration', (79, 227, 255)),
    ('specialization', (184, 108, 255)),
    ('manipulation', (92, 255, 122)),
    ('emission', (255, 216, 74)),
]

# Distancias dos nos ao centro em NenHatsuPage: o galho passa exatamente por eles
NODES = (56, 94, 132)
SPACING = NODES[1] - NODES[0]
ROOT = 26
END = 146


def smoke(size, blur, seed):
    rng = random.Random(seed)
    small = Image.new('L', (size[0] // 8, size[1] // 8))
    small.putdata([rng.randint(0, 255) for _ in range(small.width * small.height)])
    return small.resize(size, Image.BICUBIC).filter(ImageFilter.GaussianBlur(blur))


def branch_point(index, distance, sway):
    """Ponto do galho: ondula entre os nos, mas passa em cima de cada um."""
    angle = math.radians(-90 + 60 * index)
    wave = sway * math.sin(math.pi * (distance - NODES[0]) / SPACING)
    return (math.cos(angle) * distance - math.sin(angle) * wave, math.sin(angle) * distance + math.cos(angle) * wave)


def stroke(draw, points, start_width, end_width, fill=255):
    """Linha com grossura variavel, feita de circulos."""
    total = len(points) - 1
    for i, (x, y) in enumerate(points):
        width = start_width + (end_width - start_width) * i / total
        draw.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=fill)


def limb_points(index, sway, center, unit, steps=260):
    points = []
    for i in range(steps + 1):
        distance = ROOT + (END - ROOT) * i / steps
        x, y = branch_point(index, distance, sway)
        points.append((center[0] + x * unit, center[1] + y * unit))
    return points


def sprig_points(index, distance, side, length, center, unit, sway):
    """Broto que sai do galho e curva para fora."""
    angle = math.radians(-90 + 60 * index)
    base = branch_point(index, distance, sway)
    bend = angle + side * math.radians(38)
    points = []
    for i in range(61):
        t = i / 60
        spread = bend + side * math.radians(35) * t * t
        x = base[0] + math.cos(spread) * length * t
        y = base[1] + math.sin(spread) * length * t
        points.append((center[0] + x * unit, center[1] + y * unit))
    return points


def branch_mask(index, size, center):
    """A forma do galho: tronco, brotos, folhas e raizes saindo do centro."""
    rng = random.Random(40 + index)
    unit = SCALE * SUPER
    big = (size[0] * SUPER, size[1] * SUPER)
    center = (center[0] * SUPER, center[1] * SUPER)
    mask = Image.new('L', big, 0)
    draw = ImageDraw.Draw(mask)
    sway = rng.choice((-1, 1)) * 4.0
    stroke(draw, limb_points(index, sway, center, unit), 8.5 * unit, 2.6 * unit)
    for side, base in ((-1, 0), (1, 0.5)):
        for k, distance in enumerate((38, 75, 113)):
            length = rng.uniform(15, 24) * (1 - k * 0.12)
            points = sprig_points(index, distance + base * 8, side, length, center, unit, sway)
            stroke(draw, points, 2.4 * unit, 0.8 * unit)
            x, y = points[-1]
            leaf = 2.3 * unit
            draw.polygon([(x, y - leaf), (x + leaf, y), (x, y + leaf), (x - leaf, y)], fill=255)
    return mask.resize(size, Image.LANCZOS)


def hexagon_points(center, radius):
    return [(center[0] + math.cos(math.radians(-90 + 60 * i)) * radius, center[1] + math.sin(math.radians(-90 + 60 * i)) * radius) for i in range(6)]


def corner(draw, x, y, flip_x, flip_y, color):
    """Filete dourado no canto, fino e encostado na borda para nao passar por cima dos textos."""
    sx, sy = (-1 if flip_x else 1), (-1 if flip_y else 1)
    for reach, inset, width in ((120, 0, 3), (40, 8, 2)):
        draw.line((x + sx * inset, y + sy * inset, x + sx * reach, y + sy * inset), fill=color, width=width)
        draw.line((x + sx * inset, y + sy * inset, x + sx * inset, y + sy * reach), fill=color, width=width)
    size = 5
    for cx, cy in ((x + sx * 120, y), (x, y + sy * 120)):
        draw.polygon([(cx, cy - size), (cx + size, cy), (cx, cy + size), (cx - size, cy)], fill=color)


def medallion(draw, center):
    """O centro de onde saem os galhos."""
    cx, cy = center
    for radius, fill, outline, width in ((66, (14, 13, 18), (186, 148, 84), 5), (54, (9, 9, 13), (112, 88, 50), 2)):
        draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=fill, outline=outline, width=width)
    for i in range(24):
        angle = math.radians(15 * i)
        inner, outer = (56, 64) if i % 2 == 0 else (58, 62)
        draw.line((cx + math.cos(angle) * inner, cy + math.sin(angle) * inner, cx + math.cos(angle) * outer, cy + math.sin(angle) * outer),
                  fill=(186, 148, 84), width=2)


def background():
    width, height = BACK_SIZE
    center = (width // 2, height // 2)
    image = Image.new('RGB', BACK_SIZE, (8, 9, 14))
    image.paste((36, 38, 50), (0, 0), smoke(BACK_SIZE, 26, 7).point(lambda v: max(0, v - 90) * 2))
    image.paste((26, 22, 34), (0, 0), smoke(BACK_SIZE, 44, 11).point(lambda v: max(0, v - 100) * 2))

    # Bordas escuras e um halo fraco atras do hexagono
    halo = Image.new('L', BACK_SIZE, 0)
    ImageDraw.Draw(halo).ellipse((center[0] - 290, center[1] - 290, center[0] + 290, center[1] + 290), fill=52)
    image.paste((96, 78, 52), (0, 0), halo.filter(ImageFilter.GaussianBlur(70)))

    # Anel do hexagono, atras dos galhos
    guide = Image.new('L', BACK_SIZE, 0)
    guide_draw = ImageDraw.Draw(guide)
    for radius, width_line, strength in ((NODES[2] * SCALE, 2, 120), (NODES[0] * SCALE, 2, 70)):
        points = hexagon_points(center, radius)
        guide_draw.line(points + points[:1], fill=strength, width=width_line)
    image.paste((176, 140, 82), (0, 0), guide)

    # Os seis galhos apagados, em bronze com luz em cima
    bronze = Image.new('L', BACK_SIZE, 0)
    for index in range(6):
        bronze = ImageChops.lighter(bronze, branch_mask(index, BACK_SIZE, center))
    shadow = bronze.filter(ImageFilter.GaussianBlur(9)).point(lambda v: int(v * 0.8))
    image.paste((0, 0, 0), (0, 0), shadow)
    image.paste((72, 57, 34), (0, 0), bronze)
    core = bronze.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.GaussianBlur(1)).point(lambda v: int(v * 0.65))
    image.paste((142, 112, 66), (0, 0), core)

    draw = ImageDraw.Draw(image)
    medallion(draw, center)
    gold = (150, 118, 68)
    margin = 10
    corner(draw, margin, margin, False, False, gold)
    corner(draw, width - margin, margin, True, False, gold)
    corner(draw, margin, height - margin, False, True, gold)
    corner(draw, width - margin, height - margin, True, True, gold)
    return image


def branch_glow(index, color):
    """O brilho do galho, com fundo transparente: miolo claro e halo colorido."""
    center = (BRANCH_SIZE[0] // 2, BRANCH_SIZE[1] // 2)
    shape = branch_mask(index, BRANCH_SIZE, center)
    alpha = ImageChops.lighter(shape, shape.filter(ImageFilter.GaussianBlur(5)).point(lambda v: min(255, int(v * 1.1))))
    alpha = ImageChops.lighter(alpha, shape.filter(ImageFilter.GaussianBlur(16)).point(lambda v: min(255, int(v * 1.1))))

    # Fagulhas soltas em volta do galho
    rng = random.Random(90 + index)
    sparks = Image.new('L', BRANCH_SIZE, 0)
    draw = ImageDraw.Draw(sparks)
    for _ in range(70):
        distance = rng.uniform(ROOT, END + 4)
        x, y = branch_point(index, distance, rng.uniform(-24, 24))
        x, y = center[0] + x * SCALE, center[1] + y * SCALE
        size = rng.uniform(0.8, 2.4)
        draw.ellipse((x - size, y - size, x + size, y + size), fill=rng.randint(120, 240))
    alpha = ImageChops.lighter(alpha, ImageChops.lighter(sparks, sparks.filter(ImageFilter.GaussianBlur(2))))

    image = Image.new('RGB', BRANCH_SIZE, color)
    core = shape.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.GaussianBlur(1.5))
    light = tuple(min(255, c + (255 - c) * 7 // 10) for c in color)
    image.paste(light, (0, 0), core)
    image.putalpha(alpha)
    return image


def center_glow():
    size = 256
    alpha = Image.new('L', (size, size), 0)
    ImageDraw.Draw(alpha).ellipse((64, 64, 192, 192), fill=255)
    alpha = alpha.filter(ImageFilter.GaussianBlur(28))
    image = Image.new('RGB', (size, size), (255, 255, 255))
    image.putalpha(alpha)
    return image


def main():
    os.makedirs(OUT, exist_ok=True)
    background().save(os.path.join(OUT, 'background.png'), optimize=True)
    for index, (name, color) in enumerate(BRANCHES):
        branch_glow(index, color).save(os.path.join(OUT, f'branch_{name}.png'), optimize=True)
    center_glow().save(os.path.join(OUT, 'center_glow.png'), optimize=True)


if __name__ == '__main__':
    main()
