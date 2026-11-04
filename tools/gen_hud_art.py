"""
Gera as pecas da HUD de combate: moldura, retrato, barras e icones das tecnicas.
Cada imagem sai em 6 tamanhos, um por escala de interface, para aparecer pixel a pixel na tela.
As medidas das barras precisam bater com as constantes de NenHud.java.
Rode na raiz do projeto.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = 'src/main/resources/assets/hxh/textures/gui'
OUT = os.path.join(ROOT, 'hud')
TREE_ICONS = os.path.join(ROOT, 'tree', 'icon')
SCALES = range(1, 7)
SUPER = 4

GOLD = ((250, 226, 160), (146, 102, 48))
STEEL = ((48, 48, 64), (20, 20, 28))
WELL = (9, 8, 12, 255)

HP_WIDTH, HP_HEIGHT, HP_BOTTOM_WIDTH = 104, 11, 99
NEN_WIDTH, NEN_HEIGHT, NEN_BOTTOM_WIDTH = 78, 8, 74
BAR_X, HP_Y, NEN_Y = 34, 4, 17


def save(image, key, scale):
    os.makedirs(OUT, exist_ok=True)
    image.save(os.path.join(OUT, f'{key}_{scale}.png'), optimize=True)


def render(key, width, height, paint):
    for scale in SCALES:
        s = scale * SUPER
        image = Image.new('RGBA', (width * s, height * s), (0, 0, 0, 0))
        paint(image, s)
        save(image.resize((width * scale, height * scale), Image.LANCZOS), key, scale)


def scaled(points, s):
    return [(x * s, y * s) for x, y in points]


def gradient(size, top, bottom):
    image = Image.new('RGB', size)
    draw = ImageDraw.Draw(image)
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        draw.line((0, y, size[0], y), fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return image


def polygon_mask(size, points):
    mask = Image.new('L', size, 0)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return mask


def plate(image, s, outline, fill=STEEL, trim=GOLD, trim_width=1.0):
    """Placa angular: sombra, chapa escura e filete dourado, com cantos vivos."""
    points = scaled(outline, s)
    size = image.size
    shadow = polygon_mask(size, [(x + 0.75 * s, y + 0.75 * s) for x, y in points]).filter(ImageFilter.GaussianBlur(0.5 * s))
    image.paste((0, 0, 0, 150), mask=shadow)
    mask = polygon_mask(size, points)
    image.paste(gradient(size, *trim), mask=mask)
    # A chapa e a mesma forma menos o filete; a borda fica com a espessura pedida
    edge = Image.new('L', size, 0)
    ImageDraw.Draw(edge).line(points + points[:2], fill=255, width=max(1, int(trim_width * 2 * s)))
    inner = ImageChops.subtract(mask, edge)
    image.paste(gradient(size, *fill), mask=inner)
    # Brilho fino logo abaixo do filete de cima
    light = ImageChops.subtract(inner, inner.transform(size, Image.AFFINE, (1, 0, 0, 0, 1, -0.5 * s)))
    image.paste((116, 116, 140, 255), mask=light)


def well(image, s, x, y, top_width, bottom_width, height):
    """Trilho vazio da barra: reto na esquerda e inclinado na direita, como a barra desenhada no jogo."""
    points = scaled([(x, y), (x + top_width, y), (x + bottom_width, y + height), (x, y + height)], s)
    border = scaled([(x - 1, y - 1), (x + top_width + 1.5, y - 1), (x + bottom_width + 1, y + height + 1), (x - 1, y + height + 1)], s)
    draw = ImageDraw.Draw(image)
    draw.polygon(border, fill=(0, 0, 0, 255))
    draw.polygon(points, fill=WELL)


def frame_nen(image, s):
    plate(image, s, [(26, 1), (143, 1), (137, 16), (114, 16), (109.5, 27), (26, 27)])
    well(image, s, BAR_X, HP_Y, HP_WIDTH, HP_BOTTOM_WIDTH, HP_HEIGHT)
    well(image, s, BAR_X, NEN_Y, NEN_WIDTH, NEN_BOTTOM_WIDTH, NEN_HEIGHT)


def frame_basic(image, s):
    plate(image, s, [(26, 1), (143, 1), (137, 17), (26, 17)])
    well(image, s, BAR_X, HP_Y, HP_WIDTH, HP_BOTTOM_WIDTH, HP_HEIGHT)


def portrait(image, s):
    c = 7
    plate(image, s, [(c, 0), (32 - c, 0), (32, c), (32, 32 - c), (32 - c, 32), (c, 32), (0, 32 - c), (0, c)], trim_width=1.5)
    ImageDraw.Draw(image).rectangle(scaled([(3, 3), (29, 29)], s), fill=(12, 12, 16, 255))


def badge(image, s):
    plate(image, s, [(0, 0), (28, 0), (25, 14), (3, 14)])
    ImageDraw.Draw(image).rectangle(scaled([(4, 10.5), (24, 13)], s), fill=WELL)


def chip(image, s):
    plate(image, s, [(4.5, 0), (30, 0), (25.5, 10), (0, 10)])
    shield = Image.open(os.path.join(TREE_ICONS, 'shield.png')).convert('RGBA').getchannel('A')
    size = int(8 * s)
    shield = shield.resize((size, size), Image.LANCZOS)
    tint = gradient((size, size), (232, 236, 244), (138, 150, 172))
    image.paste(tint, (int(4.5 * s), int(1 * s)), shield)


def bar_texture(key, width, height, stops, gloss=1.0):
    """Gradiente na horizontal, com brilho em cima e sombra embaixo: conforme esvazia, sobra so a ponta inicial."""
    for scale in SCALES:
        w, h = width * scale, height * scale
        image = Image.new('RGBA', (w, h))
        pixels = image.load()
        for x in range(w):
            t = x / max(1, w - 1)
            for i in range(len(stops) - 1):
                if t <= stops[i + 1][0]:
                    f = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
                    base = [stops[i][1][c] + (stops[i + 1][1][c] - stops[i][1][c]) * f for c in range(3)]
                    break
            for y in range(h):
                v = (y + 0.5) / h
                if v < 0.32:
                    light = (0.32 - v) / 0.32 * 0.55 * gloss
                    color = [c + (255 - c) * light for c in base]
                else:
                    dark = max(0.0, (v - 0.55) / 0.45) * 0.5
                    color = [c * (1 - dark) for c in base]
                pixels[x, y] = tuple(int(c) for c in color) + (255,)
        save(image, key, scale)


def nen_texture():
    """Azul eletrico com raios que se repetem a cada NEN_WIDTH; a textura tem o dobro para poder rolar."""
    period = NEN_WIDTH
    zigzags = [
        [(0, 3), (5, 1.5), (9, 4), (16, 2), (22, 5.5), (29, 3)],
        [(33, 5), (40, 2.5), (44, 5.5), (52, 3.5), (58, 6)],
        [(62, 2), (67, 4.5), (73, 2.5), (78, 3)],
    ]
    for scale in SCALES:
        s = scale * SUPER
        w, h = period * 2 * s, NEN_HEIGHT * s
        image = gradient((w, h), (84, 214, 255), (10, 66, 224)).convert('RGBA')
        glow = Image.new('L', (w, h), 0)
        core = Image.new('L', (w, h), 0)
        for offset in (-period, 0, period, 2 * period):
            for line in zigzags:
                points = [((x + offset) * s, y * s) for x, y in line]
                ImageDraw.Draw(glow).line(points, fill=255, width=int(1.6 * s))
                ImageDraw.Draw(core).line(points, fill=255, width=max(1, int(0.55 * s)))
        glow = glow.filter(ImageFilter.GaussianBlur(0.6 * s))
        image.paste((80, 190, 255), mask=glow.point(lambda v: int(v * 0.8)))
        image.paste((225, 248, 255), mask=core)
        shade = Image.new('L', (w, h), 0)
        draw = ImageDraw.Draw(shade)
        for y in range(h):
            v = y / h
            draw.line((0, y, w, y), fill=int(max(0.0, v - 0.6) / 0.4 * 110))
        image.paste((4, 20, 90), mask=shade)
        highlight = Image.new('L', (w, h), 0)
        draw = ImageDraw.Draw(highlight)
        for y in range(h):
            v = y / h
            draw.line((0, y, w, y), fill=int(max(0.0, 0.3 - v) / 0.3 * 130))
        image.paste((255, 255, 255), mask=highlight)
        save(image.resize((period * 2 * scale, NEN_HEIGHT * scale), Image.LANCZOS), 'bar_nen', scale)


# ---- Simbolos das tecnicas ----

def tree_symbol(name):
    def symbol(size):
        mask = Image.open(os.path.join(TREE_ICONS, name + '.png')).convert('RGBA').getchannel('A')
        return mask.resize((size, size), Image.LANCZOS)
    return symbol


def smooth(mask, size):
    blur = mask.filter(ImageFilter.GaussianBlur(size * 0.012))
    return blur.point(lambda v: 255 if v > 127 else 0)


def unit(size, points):
    return [((x + 1) / 2 * size, (y + 1) / 2 * size) for x, y in points]


def flame(size):
    outer = [(0.02, -1), (0.2, -0.62), (0.52, -0.3), (0.72, 0.1), (0.68, 0.52), (0.38, 0.86), (0, 0.97), (-0.38, 0.86), (-0.68, 0.52),
             (-0.72, 0.14), (-0.56, -0.2), (-0.42, 0.02), (-0.3, -0.3), (-0.2, -0.62)]
    return smooth(polygon_mask((size, size), unit(size, outer)), size)


def flame_core(size):
    inner = [(0.02, -0.25), (0.2, 0.05), (0.38, 0.35), (0.28, 0.7), (0, 0.85), (-0.28, 0.7), (-0.38, 0.38), (-0.15, 0.1)]
    return smooth(polygon_mask((size, size), unit(size, inner)), size)


def hood(size):
    outline = [(0, -1), (0.42, -0.84), (0.66, -0.4), (0.7, 0.1), (0.98, 0.98), (-0.98, 0.98), (-0.7, 0.1), (-0.66, -0.4), (-0.42, -0.84)]
    return smooth(polygon_mask((size, size), unit(size, outline)), size)


def hood_face(size):
    face = [(0, -0.58), (0.36, -0.36), (0.38, 0.0), (0, 0.32), (-0.38, 0.0), (-0.36, -0.36)]
    return smooth(polygon_mask((size, size), unit(size, face)), size)


def hood_eyes(size):
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    for x in (-0.17, 0.17):
        a, b = unit(size, [(x - 0.09, -0.2), (x + 0.09, -0.08)])
        draw.ellipse((a, b), fill=255)
    return mask


def eye(size):
    points = [(x / 20, -0.62 * (1 - (x / 20) ** 2)) for x in range(-20, 21)] + [(x / 20, 0.62 * (1 - (x / 20) ** 2)) for x in range(20, -21, -1)]
    return polygon_mask((size, size), unit(size, points))


def eye_iris(size):
    mask = Image.new('L', (size, size), 0)
    a, b = unit(size, [(-0.36, -0.36), (0.36, 0.36)])
    ImageDraw.Draw(mask).ellipse((a, b), fill=255)
    return mask


def eye_pupil(size):
    mask = Image.new('L', (size, size), 0)
    a, b = unit(size, [(-0.15, -0.15), (0.15, 0.15)])
    ImageDraw.Draw(mask).ellipse((a, b), fill=255)
    return mask


def dome(size):
    mask = Image.new('L', (size, size), 0)
    a, b = unit(size, [(-0.92, -0.72), (0.92, 1.12)])
    ImageDraw.Draw(mask).pieslice((a, b), 180, 360, fill=255)
    a, b = unit(size, [(-1, 0.3), (1, 0.5)])
    ImageDraw.Draw(mask).rectangle((a, b), fill=255)
    return mask


def dome_inner(size):
    mask = Image.new('L', (size, size), 0)
    a, b = unit(size, [(-0.72, -0.52), (0.72, 0.92)])
    ImageDraw.Draw(mask).pieslice((a, b), 180, 360, fill=255)
    return mask


# Cada tecnica: desenho (mascara, cor de cima, cor de baixo) e a cor do brilho quando ativa
ICONS = {
    'ten': ([(tree_symbol('shield'), (170, 222, 255), (40, 120, 240))], (70, 170, 255)),
    'zetsu': ([(hood, (176, 176, 188), (96, 96, 110)), (hood_face, (10, 10, 14), (10, 10, 14)), (hood_eyes, (255, 255, 255), (220, 220, 230))], (190, 196, 214)),
    'ren': ([(flame, (255, 176, 48), (232, 62, 20)), (flame_core, (255, 246, 170), (255, 196, 70))], (255, 120, 40)),
    'gyo': ([(eye, (240, 246, 255), (190, 204, 226)), (eye_iris, (120, 226, 255), (24, 110, 220)), (eye_pupil, (6, 10, 26), (6, 10, 26))], (90, 210, 255)),
    'in': ([(tree_symbol('eye_off'), (214, 178, 255), (130, 80, 226))], (160, 100, 255)),
    'shu': ([(tree_symbol('blade'), (236, 246, 255), (110, 170, 236))], (130, 196, 255)),
    'ko': ([(tree_symbol('burst'), (255, 214, 110), (240, 62, 36))], (255, 80, 40)),
    'ryu': ([(tree_symbol('wave'), (170, 250, 240), (30, 172, 190))], (60, 226, 214)),
    'ken': ([(tree_symbol('shield_plus'), (255, 230, 150), (216, 138, 40))], (255, 190, 70)),
    'en': ([(dome, (150, 242, 255), (30, 150, 214)), (dome_inner, (14, 48, 74), (20, 90, 120))], (70, 214, 255)),
    'hatsu': ([(tree_symbol('star'), (255, 240, 170), (230, 160, 40))], (255, 214, 90)),
}

ICON_SIZE, HALO_SIZE = 14, 22


def disc(size, center, radius):
    mask = Image.new('L', size, 0)
    ImageDraw.Draw(mask).ellipse((center - radius, center - radius, center + radius, center + radius), fill=255)
    return mask


def icon(key, layers, glow):
    for scale in SCALES:
        s = scale * SUPER
        full = ICON_SIZE * s
        center = full / 2
        image = Image.new('RGBA', (full, full), (0, 0, 0, 0))
        image.paste(gradient((full, full), (226, 186, 112), (98, 66, 34)), mask=disc((full, full), center, 7 * s))
        image.paste((30, 20, 12, 255), mask=disc((full, full), center, 5.6 * s))
        inner = disc((full, full), center, 5.1 * s)
        tint = tuple(int(14 + c * 0.3) for c in glow)
        image.paste(gradient((full, full), tint, (12, 12, 18)), mask=inner)

        size = int(8.4 * s)
        corner = int(center - size / 2)
        first = layers[0][0](size)
        halo = Image.new('L', (full, full), 0)
        halo.paste(first, (corner, corner))
        image.paste(glow, mask=halo.filter(ImageFilter.GaussianBlur(0.7 * s)).point(lambda v: int(v * 0.65)))
        for mask, top, bottom in layers:
            image.paste(gradient((size, size), top, bottom), (corner, corner), mask(size))
        save(image.resize((ICON_SIZE * scale, ICON_SIZE * scale), Image.LANCZOS), 'icon_' + key, scale)

        full = HALO_SIZE * s
        center = full / 2
        ring = disc((full, full), center, 8 * s).filter(ImageFilter.GaussianBlur(1.5 * s))
        result = Image.new('RGBA', (full, full), glow + (0,))
        result.putalpha(ring)
        save(result.resize((HALO_SIZE * scale, HALO_SIZE * scale), Image.LANCZOS), 'halo_' + key, scale)


def main():
    render('frame_nen', 144, 28, frame_nen)
    render('frame_basic', 144, 18, frame_basic)
    render('portrait', 32, 32, portrait)
    render('badge', 28, 14, badge)
    render('chip', 30, 10, chip)
    bar_texture('bar_hp', HP_WIDTH, HP_HEIGHT, [(0, (206, 14, 22)), (0.55, (250, 92, 16)), (1, (255, 214, 52))])
    bar_texture('bar_poison', HP_WIDTH, HP_HEIGHT, [(0, (24, 110, 40)), (1, (148, 214, 56))])
    bar_texture('bar_wither', HP_WIDTH, HP_HEIGHT, [(0, (36, 30, 42)), (1, (108, 96, 118))])
    bar_texture('bar_trail', HP_WIDTH, HP_HEIGHT, [(0, (255, 244, 176)), (1, (255, 226, 120))], gloss=0.6)
    bar_texture('bar_absorb', HP_WIDTH, HP_HEIGHT, [(0, (255, 214, 70)), (1, (255, 240, 150))], gloss=0.8)
    nen_texture()
    for key, (layers, glow) in ICONS.items():
        icon(key, layers, glow)
    print('ok')


if __name__ == '__main__':
    main()
