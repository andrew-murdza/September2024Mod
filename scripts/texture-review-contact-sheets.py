"""Create labeled QA contact sheets without changing delivered Minecraft captures."""
from pathlib import Path
import argparse
import math
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--phase', choices=['before', 'after'], default='after')
args = parser.parse_args()
files = sorted((ROOT / 'build/texture-gallery/screenshots' / args.phase).glob('*.png'))
out = ROOT / 'tmp/texture-review-world-qa' / args.phase
out.mkdir(parents=True, exist_ok=True)
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 16)
for start in range(0, len(files), 12):
    page = files[start:start + 12]
    sheet = Image.new('RGB', (1440, math.ceil(len(page) / 3) * 365), '#26313b')
    draw = ImageDraw.Draw(sheet)
    for index, path in enumerate(page):
        x, y = (index % 3) * 480, (index // 3) * 365
        with Image.open(path) as frame:
            frame.thumbnail((480, 333))
            sheet.paste(frame, (x, y))
        draw.text((x + 5, y + 336), path.stem, font=font, fill='white')
    target = out / f'page-{start // 12 + 1:02d}.jpg'
    sheet.save(target, quality=92)
    print(target)
