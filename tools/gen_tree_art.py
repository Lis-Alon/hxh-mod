"""
Gera o fundo da arvore (fumaca e silhuetas) e as pecas novas: linhas, ponto de juncao e kanjis.
As silhuetas vem de tools/art/tree_reference.png, a imagem de referencia da arvore.
Rode na raiz do projeto, depois de gen_tree_textures.py.
"""
import json
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

OUT = 'src/main/resources/assets/hxh/textures/gui/tree'
REFERENCE = 'tools/art/tree_reference.png'
KANJI_FONT = r'C:\Windows\Fonts\YuGothB.ttc'
WIDTH, HEIGHT = 1280, 600
random.seed(11)


def smooth(file):
    """Pede ao jogo filtro suave: essas texturas aparecem em tamanhos variados."""
    with open(file + '.mcmeta', 'w', encoding='utf-8') as out:
        json.dump({'texture': {'blur': True}}, out)


def fade_mask(size, left, right, top, bottom):
    mask = Image.new('L', size, 255)
    pixels = mask.load()
    for y in range(size[1]):
        for x in range(size[0]):
            f = min(1.0, x / max(1, left), (size[0] - 1 - x) / max(1, right), y / max(1, top), (size[1] - 1 - y) / max(1, bottom))
            pixels[x, y] = int(255 * f * f * (3 - 2 * f))
    return mask


def background():
    image = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    smoke = Image.new('L', (WIDTH, HEIGHT), 0)
    draw = ImageDraw.Draw(smoke)
    for _ in range(46):
        x, y = random.randint(150, 1000), random.randint(0, HEIGHT)
        r = random.randint(50, 170)
        draw.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=random.randint(24, 62))
    smoke = smoke.filter(ImageFilter.GaussianBlur(44))
    smoke = ImageChops.multiply(smoke, fade_mask(smoke.size, 120, 120, 90, 90))
    image.paste((96, 100, 118, 255), (0, 0), smoke)

    reference = Image.open(REFERENCE).convert('RGB')
    for box, y in (((860, 50, 1024, 143), 4), ((872, 186, 1024, 284), 198), ((862, 324, 1024, 418), 392)):
        crop = reference.crop(box).resize((290, 176), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.7))
        crop.putalpha(fade_mask(crop.size, 70, 30, 22, 34))
        image.alpha_composite(crop, (WIDTH - 290, y))
    image.save(OUT + '/background.png', optimize=True)
    smooth(OUT + '/background.png')


def line(name, core, glow):
    """Faixa vertical: o miolo e a linha, o resto e o brilho em volta. Sera tingida no jogo."""
    w, h = 8, 64
    alpha = Image.new('L', (w, h * 4), 0)
    draw = ImageDraw.Draw(alpha)
    mid = h * 2
    if glow:
        draw.rectangle((0, mid - core * 8, w, mid + core * 8), fill=130)
        alpha = alpha.filter(ImageFilter.GaussianBlur(glow * 4))
        draw = ImageDraw.Draw(alpha)
    draw.rectangle((0, mid - core * 4, w, mid + core * 4), fill=255)
    alpha = alpha.filter(ImageFilter.GaussianBlur(3)).resize((w, h), Image.LANCZOS)
    result = Image.new('RGBA', (w, h), (255, 255, 255, 0))
    result.putalpha(alpha)
    result.save(f'{OUT}/{name}.png')
    smooth(f'{OUT}/{name}.png')


def dot():
    size = 32 * 4
    image = Image.new('L', (size, size), 0)
    ImageDraw.Draw(image).ellipse((size * 0.1, size * 0.1, size * 0.9, size * 0.9), fill=255)
    result = Image.new('RGBA', (32, 32), (255, 255, 255, 0))
    result.putalpha(image.resize((32, 32), Image.LANCZOS))
    result.save(OUT + '/dot.png')
    smooth(OUT + '/dot.png')


def kanji():
    with open('src/main/resources/assets/hxh/lang/pt_br.json', encoding='utf-8') as file:
        texts = json.load(file)
    os.makedirs(OUT + '/kanji', exist_ok=True)
    for key, value in texts.items():
        if key.startswith('nen.hxh.technique.') and key.endswith('.kanji'):
            name = key.split('.')[3]
            size = 96 * 4
            image = Image.new('L', (size, size), 0)
            ImageDraw.Draw(image).text((size / 2, size / 2), value, font=ImageFont.truetype(KANJI_FONT, 78 * 4), fill=255, anchor='mm')
            result = Image.new('RGBA', (96, 96), (255, 255, 255, 0))
            result.putalpha(image.resize((96, 96), Image.LANCZOS))
            result.save(f'{OUT}/kanji/{name}.png', optimize=True)
            smooth(f'{OUT}/kanji/{name}.png')


background()
line('line_off', 3, 0)
line('line_on', 5, 3)
dot()
kanji()
for name in ('ring_locked', 'ring_soon', 'ring_available', 'ring_learned', 'ring_active', 'emblem'):
    smooth(f'{OUT}/{name}.png')
for name in os.listdir(OUT + '/icon'):
    if name.endswith('.png'):
        smooth(f'{OUT}/icon/{name}')
print('ok')
