"""Gera as texturas da arvore de tecnicas (fundo, aneis dos nos, emblema e icones). Rode na raiz do projeto."""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = 'src/main/resources/assets/hxh/textures/gui/tree'
ICON_DIR = OUT + '/icon'
WIDTH, HEIGHT = 1280, 600
SS = 4  # desenha maior e reduz para suavizar as bordas
random.seed(7)
os.makedirs(ICON_DIR, exist_ok=True)


# ---------------------------------------------------------------- Fundo

def smoke(size, blobs, radius, blur, level):
    layer = Image.new('L', size, 0)
    draw = ImageDraw.Draw(layer)
    for _ in range(blobs):
        x, y = random.randint(0, size[0]), random.randint(0, size[1])
        r = random.randint(radius // 3, radius)
        draw.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=random.randint(level // 2, level))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def limb(draw, points, width):
    draw.line(points, fill=255, width=width, joint='curve')
    for x, y in points:
        draw.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=255)


def fighter(pose):
    """Silhueta de lutador em tela 400x400, com as juntas dadas por pose."""
    image = Image.new('L', (400, 400), 0)
    draw = ImageDraw.Draw(image)
    head = pose['head']
    limb(draw, [pose['neck'], pose['hip']], 64)
    for part, width in (('arm_a', 30), ('arm_b', 30), ('leg_a', 42), ('leg_b', 42)):
        limb(draw, pose[part], width)
    draw.ellipse((head[0] - 25, head[1] - 27, head[0] + 25, head[1] + 27), fill=255)
    # Cabelo em pontas
    spikes = []
    for i in range(9):
        angle = math.radians(200 + i * 18)
        reach = 58 if i % 2 == 0 else 28
        spikes.append((head[0] + math.cos(angle) * reach, head[1] + math.sin(angle) * reach))
    draw.polygon([(head[0] - 26, head[1] + 2)] + spikes + [(head[0] + 26, head[1] + 2)], fill=255)
    return image


POSES = [
    # Guarda baixa, olhando para o lado
    {'head': (205, 92), 'neck': (205, 135), 'hip': (195, 245),
     'arm_a': [(180, 145), (140, 190), (175, 215)], 'arm_b': [(232, 145), (262, 190), (232, 222)],
     'leg_a': [(182, 250), (150, 320), (140, 400)], 'leg_b': [(212, 250), (250, 320), (272, 400)]},
    # Soco carregado, com a aura subindo
    {'head': (190, 100), 'neck': (195, 142), 'hip': (205, 250),
     'arm_a': [(170, 150), (120, 170), (82, 142)], 'arm_b': [(222, 150), (272, 186), (250, 225)],
     'leg_a': [(190, 255), (140, 320), (112, 400)], 'leg_b': [(222, 255), (272, 315), (300, 400)]},
    # Bracos erguidos, pronto para atacar
    {'head': (210, 96), 'neck': (208, 138), 'hip': (200, 248),
     'arm_a': [(182, 148), (150, 118), (128, 72)], 'arm_b': [(236, 148), (280, 170), (300, 130)],
     'leg_a': [(186, 252), (170, 325), (130, 400)], 'leg_b': [(216, 252), (240, 320), (262, 400)]},
]

BANDS = [(0, 196), (204, 396), (404, 600)]
PANEL_X = 1060


def background():
    base = Image.new('RGB', (WIDTH, HEIGHT), (9, 11, 16))
    base.paste((62, 66, 74), (0, 0, WIDTH, HEIGHT), smoke((WIDTH, HEIGHT), 90, 170, 42, 110))
    base.paste((24, 40, 44), (0, 0, WIDTH, HEIGHT), smoke((WIDTH, HEIGHT), 30, 220, 60, 90))

    # Rachaduras discretas nos cantos
    cracks = Image.new('L', (WIDTH, HEIGHT), 0)
    draw = ImageDraw.Draw(cracks)
    for start in ((0, 40), (0, 560), (1040, 0), (560, 600), (300, 0)):
        x, y = start
        angle = random.uniform(0, math.tau)
        for _ in range(26):
            angle += random.uniform(-0.8, 0.8)
            nx, ny = x + math.cos(angle) * 18, y + math.sin(angle) * 18
            draw.line((x, y, nx, ny), fill=70, width=1)
            x, y = nx, ny
    base.paste((70, 72, 78), (0, 0, WIDTH, HEIGHT), cracks)

    # Faixas da direita com a silhueta e a aura
    for index, (top, bottom) in enumerate(BANDS):
        height = bottom - top
        band = Image.new('L', (WIDTH, height), 0)
        band_draw = ImageDraw.Draw(band)
        for x in range(PANEL_X - 140, WIDTH):
            band_draw.line((x, 0, x, height), fill=int(min(1.0, (x - PANEL_X + 140) / 200) * 125))
        base.paste((40, 44, 52), (0, top), band)

        aura = Image.new('L', (WIDTH, height), 0)
        aura_draw = ImageDraw.Draw(aura)
        center = 1180
        for _ in range(60):
            x = random.randint(center - 95, center + 95)
            y = random.randint(0, height)
            length = random.randint(30, 90)
            aura_draw.line((x, y, x + random.randint(-14, 14), y - length), fill=random.randint(60, 170),
                           width=random.randint(5, 14))
        aura = aura.filter(ImageFilter.GaussianBlur(9))
        base.paste((150, 158, 170), (0, top), aura)

        figure = fighter(POSES[index]).resize((height, height), Image.LANCZOS)
        canvas = Image.new('L', (WIDTH, height), 0)
        canvas.paste(figure, (center - height // 2 + 10, 0))
        glow = canvas.filter(ImageFilter.GaussianBlur(10))
        base.paste((205, 210, 220), (0, top), ImageChops.multiply(glow, Image.new('L', glow.size, 120)))
        base.paste((12, 13, 18), (0, top), canvas)

        # Escurece a parte de baixo para o texto da faixa ficar legivel
        shade = Image.new('L', (WIDTH, height), 0)
        shade_draw = ImageDraw.Draw(shade)
        for y in range(height // 2, height):
            alpha = int((y - height // 2) / (height / 2) * 190)
            shade_draw.line((PANEL_X - 40, y, WIDTH, y), fill=alpha)
        base.paste((7, 8, 12), (0, top), shade.filter(ImageFilter.GaussianBlur(8)))

    # Linhas que separam as faixas
    line = ImageDraw.Draw(base)
    for top, bottom in BANDS:
        line.line((PANEL_X - 60, bottom, WIDTH, bottom), fill=(70, 72, 80), width=2)
    base.save(OUT + '/background.png')


# ---------------------------------------------------------------- Aneis

def ring(name, outer, inner, fill, glow=None):
    size = 128 * SS
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    center = size / 2
    if glow:
        halo = Image.new('L', (size, size), 0)
        ImageDraw.Draw(halo).ellipse((center - 25 * SS * 2, center - 25 * SS * 2, center + 25 * SS * 2, center + 25 * SS * 2), fill=255)
        halo = halo.filter(ImageFilter.GaussianBlur(5 * SS * 2))
        aura = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        aura.paste(glow + (255,), (0, 0), halo)
        aura.resize((128, 128), Image.LANCZOS).save(f"{OUT}/{name.replace('ring_', 'glow_')}.png")
    draw = ImageDraw.Draw(image)
    draw.ellipse((center - 24 * SS * 2, center - 24 * SS * 2, center + 24 * SS * 2, center + 24 * SS * 2), fill=outer)
    draw.ellipse((center - 20 * SS * 2, center - 20 * SS * 2, center + 20 * SS * 2, center + 20 * SS * 2), fill=inner)
    draw.ellipse((center - 17 * SS * 2, center - 17 * SS * 2, center + 17 * SS * 2, center + 17 * SS * 2), fill=fill)
    image.resize((128, 128), Image.LANCZOS).save(f'{OUT}/{name}.png')


def emblem():
    size = 128 * SS
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    center = size / 2
    halo = Image.new('L', (size, size), 0)
    ImageDraw.Draw(halo).ellipse((center - 56 * SS, center - 56 * SS, center + 56 * SS, center + 56 * SS), fill=200)
    image.paste((216, 180, 106, 255), (0, 0), halo.filter(ImageFilter.GaussianBlur(5 * SS)))
    draw = ImageDraw.Draw(image)
    draw.ellipse((center - 56 * SS, center - 56 * SS, center + 56 * SS, center + 56 * SS), fill=(216, 180, 106, 255))
    draw.ellipse((center - 51 * SS, center - 51 * SS, center + 51 * SS, center + 51 * SS), fill=(24, 34, 52, 255))
    draw.ellipse((center - 45 * SS, center - 45 * SS, center + 45 * SS, center + 45 * SS), fill=(30, 72, 112, 255))
    # Marcas em volta do anel
    for i in range(4):
        angle = math.radians(45 + i * 90)
        x, y = center + math.cos(angle) * 53 * SS, center + math.sin(angle) * 53 * SS
        draw.ellipse((x - 4 * SS, y - 4 * SS, x + 4 * SS, y + 4 * SS), fill=(240, 214, 150, 255))
    # Chama de aura no centro
    for scale, color in ((1.0, (216, 180, 106, 255)), (0.6, (250, 232, 180, 255))):
        points = []
        for x, y in ((0, -36), (10, -16), (22, -8), (16, 8), (24, 26), (0, 34), (-24, 26), (-16, 8), (-22, -8), (-10, -16)):
            points.append((center + x * scale * SS, center + (y * scale + 2) * SS))
        draw.polygon(points, fill=color)
    image.resize((128, 128), Image.LANCZOS).save(OUT + '/emblem.png')


# ---------------------------------------------------------------- Icones

def polygon_circle(cx, cy, r, count, start=0.0, inner=None):
    points = []
    for i in range(count):
        radius = r if inner is None or i % 2 == 0 else inner
        angle = start + i * math.tau / count
        points.append((cx + math.cos(angle) * radius, cy + math.sin(angle) * radius))
    return points


def icon_drop(d):
    d.polygon([(24, 6), (36, 26), (12, 26)], fill=255)
    d.ellipse((12, 18, 36, 42), fill=255)


def icon_heart(d):
    d.ellipse((7, 10, 25, 28), fill=255)
    d.ellipse((23, 10, 41, 28), fill=255)
    d.polygon([(8, 24), (40, 24), (24, 42)], fill=255)


def icon_eye_off(d):
    d.arc((5, 12, 43, 42), 200, 340, fill=255, width=4)
    d.arc((5, 6, 43, 36), 20, 160, fill=255, width=4)
    d.ellipse((18, 18, 30, 30), fill=255)
    d.line((9, 40, 39, 8), fill=255, width=4)


def icon_moon(d):
    d.ellipse((8, 8, 40, 40), fill=255)
    d.ellipse((18, 4, 46, 32), fill=0)


def icon_step(d):
    d.ellipse((10, 22, 24, 42), fill=255)
    for x, y in ((10, 14), (15, 11), (20, 11), (25, 14)):
        d.ellipse((x - 2, y - 2, x + 3, y + 3), fill=255)
    d.ellipse((28, 10, 40, 28), fill=255)


def icon_gauge(d):
    d.arc((6, 8, 42, 44), 180, 360, fill=255, width=5)
    d.line((24, 28, 34, 14), fill=255, width=4)
    d.ellipse((20, 24, 28, 32), fill=255)
    d.line((6, 36, 42, 36), fill=255, width=3)


def icon_fist(d):
    d.rounded_rectangle((12, 16, 36, 38), 6, fill=255)
    for x in (12, 20, 28):
        d.rounded_rectangle((x, 8, x + 7, 22), 3, fill=255)
    d.rectangle((16, 38, 32, 44), fill=255)
    d.line((12, 24, 36, 24), fill=0, width=2)


def icon_target(d):
    d.ellipse((7, 7, 41, 41), outline=255, width=4)
    d.ellipse((17, 17, 31, 31), fill=255)
    for a, b, c, e in ((24, 2, 24, 10), (24, 38, 24, 46), (2, 24, 10, 24), (38, 24, 46, 24)):
        d.line((a, b, c, e), fill=255, width=3)


def icon_arrows(d):
    d.line((8, 16, 34, 16), fill=255, width=4)
    d.polygon([(32, 8), (44, 16), (32, 24)], fill=255)
    d.line((14, 32, 40, 32), fill=255, width=4)
    d.polygon([(16, 24), (4, 32), (16, 40)], fill=255)


def icon_mask(d):
    d.chord((6, 8, 42, 44), 0, 360, fill=255)
    d.ellipse((12, 20, 22, 28), fill=0)
    d.ellipse((26, 20, 36, 28), fill=0)
    d.arc((16, 26, 32, 38), 20, 160, fill=0, width=3)


def icon_hourglass(d):
    d.polygon([(10, 6), (38, 6), (24, 24), (38, 42), (10, 42), (24, 24)], fill=255)
    d.rectangle((8, 4, 40, 8), fill=255)
    d.rectangle((8, 40, 40, 44), fill=255)


def icon_shield(d):
    d.polygon([(24, 4), (42, 10), (40, 28), (24, 44), (8, 28), (6, 10)], fill=255)


def icon_shield_plus(d):
    icon_shield(d)
    d.rectangle((21, 12, 27, 34), fill=0)
    d.rectangle((13, 20, 35, 26), fill=0)


def icon_burst(d):
    d.polygon(polygon_circle(24, 24, 22, 16, 0.2, 9), fill=255)


def icon_timer(d):
    d.ellipse((7, 10, 41, 44), outline=255, width=4)
    d.rectangle((20, 3, 28, 8), fill=255)
    d.line((24, 27, 24, 16), fill=255, width=3)
    d.line((24, 27, 32, 31), fill=255, width=3)


def icon_wave(d):
    for y in (14, 24, 34):
        points = [(x, y + math.sin(x / 42 * math.tau) * 4) for x in range(4, 45, 2)]
        d.line(points, fill=255, width=4)


def icon_blade(d):
    d.polygon([(38, 4), (44, 10), (18, 36), (12, 30)], fill=255)
    d.line((8, 26, 22, 40), fill=255, width=4)
    d.line((12, 36, 5, 43), fill=255, width=5)


def icon_coat(d):
    d.ellipse((5, 5, 43, 43), outline=255, width=3)
    d.line((14, 34, 34, 14), fill=255, width=6)


def icon_expand(d):
    d.rectangle((17, 17, 31, 31), fill=255)
    for a, b, c in (((6, 14), (6, 6), (14, 6)), ((34, 6), (42, 6), (42, 14)),
                    ((42, 34), (42, 42), (34, 42)), ((14, 42), (6, 42), (6, 34))):
        d.line((a, b, c), fill=255, width=4)


def icon_compass(d):
    d.ellipse((6, 6, 42, 42), outline=255, width=3)
    d.polygon([(32, 12), (27, 27), (16, 36), (21, 21)], fill=255)


def icon_star(d):
    d.polygon(polygon_circle(24, 25, 21, 10, -math.pi / 2, 9), fill=255)


def icon_rune(d):
    d.polygon(polygon_circle(24, 24, 20, 6, math.pi / 6), outline=255, width=3)
    d.line((24, 12, 24, 36), fill=255, width=3)
    d.line((15, 20, 33, 28), fill=255, width=3)


ICONS = {name[5:]: fn for name, fn in list(globals().items()) if name.startswith('icon_')}


def icons():
    for name, draw_icon in ICONS.items():
        size = 48 * SS
        mask = Image.new('L', (48, 48), 0)
        draw_icon(ImageDraw.Draw(mask))
        mask = mask.resize((size, size), Image.BICUBIC).filter(ImageFilter.GaussianBlur(2)).point(lambda v: 255 if v > 110 else 0)
        image = Image.new('RGBA', (size, size), (255, 255, 255, 0))
        image.putalpha(mask)
        image.resize((48, 48), Image.LANCZOS).save(f'{ICON_DIR}/{name}.png')


background()
ring('ring_locked', (92, 95, 102, 255), (30, 32, 38, 255), (52, 55, 62, 255))
ring('ring_soon', (58, 60, 66, 255), (22, 24, 28, 255), (34, 36, 42, 255))
ring('ring_available', (232, 232, 220, 255), (34, 38, 44, 255), (66, 72, 82, 255), (150, 150, 140))
ring('ring_learned', (205, 232, 92, 255), (48, 62, 18, 255), (98, 124, 34, 255), (178, 216, 60))
ring('ring_active', (255, 220, 130, 255), (84, 58, 14, 255), (168, 120, 34, 255), (255, 196, 70))
emblem()
icons()
print('ok', len(ICONS))
