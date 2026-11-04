"""
Gera as imagens do painel do Nen: titulos, abas e botoes por idioma, e molduras compartilhadas.
Cada imagem sai em 6 tamanhos, um por escala de interface, para aparecer pixel a pixel na tela.
Os textos vem dos arquivos de idioma, entao mudou o texto, e so rodar de novo.
"""
import json
import math
import os
import shutil

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hxh')
OUT = os.path.join(ROOT, 'textures', 'gui', 'art')
FONT = os.path.join(ROOT, 'font', 'cinzel.ttf')
EMBLEM = os.path.join(ROOT, 'textures', 'gui', 'tree', 'emblem.png')
LANGS = ['pt_br', 'en_us']
SCALES = range(1, 7)
SUPER = 4
TITLE_MAX_WIDTH = 94

CREAM = ((244, 239, 224), (205, 198, 178))
GOLD = ((248, 226, 164), (188, 142, 72))
DIM = ((128, 124, 114), (96, 93, 86))

sizes = {}


def font(size, weight):
    result = ImageFont.truetype(FONT, size)
    result.set_variation_by_axes([weight])
    return result


def gradient(width, height, colors):
    top, bottom = colors
    image = Image.new('RGB', (width, height))
    for y in range(height):
        t = y / max(1, height - 1)
        color = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        ImageDraw.Draw(image).line((0, y, width, y), fill=color)
    return image


def star_points(cx, cy, r):
    points = []
    for i in range(8):
        radius = r if i % 2 == 0 else r * 0.3
        angle = math.pi / 4 * i - math.pi / 2
        points.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    return points


def text_image(lang, key, text, colors, size, height, baseline, weight, star=False):
    """A largura em unidades da interface e inteira e igual em todas as escalas."""
    probe = font(size * 6 * SUPER, weight)
    box = probe.getbbox(text, anchor='ls')
    star_width = 11 if star else 0
    width = math.ceil((box[2] - box[0]) / (6 * SUPER)) + 2 + star_width
    for scale in SCALES:
        s = scale * SUPER
        mask = Image.new('L', (width * s, height * s), 0)
        draw = ImageDraw.Draw(mask)
        f = font(size * s, weight)
        draw.text(((1 + star_width) * s - f.getbbox(text, anchor='ls')[0], baseline * s), text, font=f, fill=255, anchor='ls')
        if star:
            draw.polygon(star_points(4.5 * s, (baseline - 3.5) * s, 4 * s), fill=255)
        shadow = mask.filter(ImageFilter.GaussianBlur(0.5 * s)).point(lambda v: int(v * 0.7))
        shadow = Image.composite(Image.new('L', mask.size, 255), Image.new('L', mask.size, 0), shadow.transform(
            mask.size, Image.AFFINE, (1, 0, 0, 0, 1, -int(0.5 * s))))
        result = Image.new('RGBA', mask.size, (0, 0, 0, 0))
        result.paste((4, 4, 6, 170), mask=shadow)
        result.paste(gradient(mask.size[0], mask.size[1], colors), mask=mask)
        save(result.resize((width * scale, height * scale), Image.LANCZOS), lang, key, scale, (width, height))


def save(image, group, key, scale, size):
    folder = os.path.join(OUT, group)
    os.makedirs(folder, exist_ok=True)
    image.save(os.path.join(folder, f'{key}_{scale}.png'), optimize=True)
    sizes[f'{group}/{key}'] = list(size)


def frame(key, width, height, paint):
    for scale in SCALES:
        s = scale * SUPER
        image = Image.new('RGBA', (width * s, height * s), (0, 0, 0, 0))
        paint(image, s)
        save(image.resize((width * scale, height * scale), Image.LANCZOS), 'shared', key, scale, (width, height))


def vertical_fill(image, s, top, bottom, inset=0.0):
    w, h = image.size
    draw = ImageDraw.Draw(image, 'RGBA')
    for y in range(int(inset * s), h - int(inset * s)):
        t = y / max(1, h - 1)
        color = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(4))
        draw.line((inset * s, y, w - inset * s - 1, y), fill=color)


def button_frame(top, bottom, edge, glow):
    def paint(image, s):
        w, h = image.size
        vertical_fill(image, s, top, bottom, 0.5)
        draw = ImageDraw.Draw(image, 'RGBA')
        draw.rectangle((0, 0, w - 1, h - 1), outline=edge, width=max(1, int(0.75 * s)))
        draw.rectangle((2 * s, 2 * s, w - 2 * s - 1, h - 2 * s - 1), outline=glow, width=max(1, int(0.4 * s)))
        for cx in (2.5 * s, w - 2.5 * s):
            for cy in (2.5 * s, h - 2.5 * s):
                draw.polygon([(cx, cy - 2 * s), (cx + 2 * s, cy), (cx, cy + 2 * s), (cx - 2 * s, cy)], fill=edge)
    return paint


def row_frame(fill, line):
    """Faixa que some nas pontas, com um fio fino embaixo."""
    def paint(image, s):
        w, h = image.size
        for x in range(w):
            fade = min(1.0, min(x, w - 1 - x) / (5.0 * s))
            draw = ImageDraw.Draw(image, 'RGBA')
            draw.line((x, 0, x, h - s - 1), fill=fill[:3] + (int(fill[3] * fade),))
            draw.line((x, h - s, x, h - 1), fill=line[:3] + (int(line[3] * fade),))
    return paint


def selected_frame(image, s):
    w, h = image.size
    vertical_fill(image, s, (108, 88, 50, 215), (50, 40, 24, 215), 0.5)
    draw = ImageDraw.Draw(image, 'RGBA')
    draw.rectangle((0, 0, w - 1, h - 1), outline=(226, 190, 118, 255), width=max(1, int(0.75 * s)))
    draw.line((s, 1.25 * s, w - s, 1.25 * s), fill=(255, 236, 190, 60), width=max(1, int(0.5 * s)))


def emblem_image():
    source = Image.open(EMBLEM).convert('RGBA')
    for scale in SCALES:
        s = scale * SUPER
        size = 18 * s
        image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        vertical_fill(image, s, (46, 32, 46, 255), (26, 18, 28, 255), 0.5)
        draw = ImageDraw.Draw(image, 'RGBA')
        draw.rectangle((0, 0, size - 1, size - 1), outline=(190, 150, 84, 255), width=max(1, int(0.75 * s)))
        inner = 15 * s
        image.alpha_composite(source.resize((inner, inner), Image.LANCZOS), (int(1.5 * s), int(1.5 * s)))
        save(image.resize((18 * scale, 18 * scale), Image.LANCZOS), 'shared', 'emblem', scale, (18, 18))


def close_image(key, color):
    def paint(image, s):
        w, h = image.size
        draw = ImageDraw.Draw(image, 'RGBA')
        for line in ((1.5 * s, 1.5 * s, w - 1.5 * s, h - 1.5 * s), (w - 1.5 * s, 1.5 * s, 1.5 * s, h - 1.5 * s)):
            draw.line(line, fill=color, width=max(1, int(1.1 * s)))
    frame(key, 9, 9, paint)


def points_icon(image, s):
    draw = ImageDraw.Draw(image, 'RGBA')
    for offset, color in ((0, (232, 196, 122, 255)), (6, (176, 138, 74, 255))):
        y = offset * s
        draw.polygon([(9 * s, 1 * s + y), (17 * s, 8 * s + y), (13 * s, 8 * s + y), (9 * s, 4.5 * s + y), (5 * s, 8 * s + y), (1 * s, 8 * s + y)], fill=color)


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    for lang in LANGS:
        with open(os.path.join(ROOT, 'lang', f'{lang}.json'), encoding='utf-8') as file:
            texts = json.load(file)

        def t(key):
            return texts[f'screen.hxh.nen.{key}']

        for page in ('level', 'techniques', 'tree', 'status'):
            name = t(f'page.{page}')
            # Titulo longo encolhe para nao encostar nas abas na menor tela (854x480)
            title = name.upper()
            box = font(17 * 6 * SUPER, 500).getbbox(title, anchor='ls')
            size = min(17.0, 17.0 * TITLE_MAX_WIDTH * 6 * SUPER / (box[2] - box[0]))
            text_image(lang, f'title_{page}', title, CREAM, size, 23, 18, 500)
            text_image(lang, f'tab_{page}', name, CREAM, 11, 14, 11, 600)
            text_image(lang, f'tab_{page}_on', name, GOLD, 11, 14, 11, 600)
        for key in ('attributes', 'base_stats', 'aura_stats', 'records', 'training', 'dormant'):
            text_image(lang, f'section_{key}', t(key).upper(), GOLD, 10, 13, 10, 600, star=True)
        for key in ('confirm', 'reset'):
            text_image(lang, f'button_{key}', t(key), CREAM, 11, 14, 11, 600)
            text_image(lang, f'button_{key}_on', t(key), GOLD, 11, 14, 11, 600)
            text_image(lang, f'button_{key}_off', t(key), DIM, 11, 14, 11, 600)

    for lang in LANGS:
        with open(os.path.join(ROOT, 'lang', f'{lang}.json'), encoding='utf-8') as file:
            texts = json.load(file)
        soft = ((170, 166, 154), (138, 134, 124))
        for band in ('ten_zetsu', 'ren_ken', 'ryu_shu'):
            key = f'screen.hxh.nen.tree.band.{band}'
            text_image(lang, f'band_{band}', texts[key], CREAM, 12, 15, 12, 600)
            text_image(lang, f'band_{band}_desc', texts[key + '.desc'], soft, 8, 11, 8, 500)
        text_image(lang, 'tree_points', texts['screen.hxh.nen.tree.points'], soft, 8, 11, 8, 500)
        text_image(lang, 'tree_techniques', texts['screen.hxh.nen.page.techniques'], CREAM, 9, 11, 9, 600)
        text_image(lang, 'tree_techniques_on', texts['screen.hxh.nen.page.techniques'], GOLD, 9, 11, 9, 600)
        for key in ('reset', 'confirm'):
            label = texts[f'screen.hxh.nen.tree.{key}']
            text_image(lang, f'tree_{key}', label, CREAM, 9, 11, 9, 600)
            text_image(lang, f'tree_{key}_on', label, GOLD, 9, 11, 9, 600)
            text_image(lang, f'tree_{key}_off', label, DIM, 9, 11, 9, 600)

    frame('points_icon', 18, 16, points_icon)
    frame('frame_button', 24, 16, button_frame((30, 30, 36, 225), (12, 12, 17, 225), (140, 112, 64, 255), (140, 112, 64, 110)))
    frame('frame_button_on', 24, 16, button_frame((56, 46, 28, 235), (28, 22, 14, 235), (228, 190, 118, 255), (228, 190, 118, 120)))
    frame('frame_button_off', 24, 16, button_frame((22, 22, 26, 200), (12, 12, 15, 200), (64, 62, 58, 255), (64, 62, 58, 110)))
    frame('frame_row', 24, 14, row_frame((20, 22, 30, 130), (255, 255, 255, 34)))
    frame('frame_row_hover', 24, 14, row_frame((44, 46, 58, 170), (232, 200, 140, 70)))
    frame('frame_row_selected', 24, 14, selected_frame)
    emblem_image()
    close_image('close', (233, 228, 212, 255))
    close_image('close_on', (232, 196, 122, 255))

    with open(os.path.join(OUT, 'manifest.json'), 'w', encoding='utf-8') as file:
        json.dump({'sizes': sizes}, file, indent='\t', sort_keys=True)
    print('ok', len(sizes), 'imagens em', len(list(SCALES)), 'escalas')


main()
