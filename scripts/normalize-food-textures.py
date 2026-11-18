"""Regenerate auxiliary food artwork and a preview without rewriting seafood.

Run with Pillow. Reviewed seafood PNGs are final artwork: do not replace their
palettes or shapes here. Source textures are read from pinned dependency jars.
"""
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "src/main/resources/assets/aoemod/textures/item"


def source(jar, name):
    with ZipFile(jar) as archive:
        return Image.open(BytesIO(archive.read(name))).convert("RGBA")


def save(name, image):
    assert image.size == (16, 16), (name, image.size)
    image.save(ITEMS / (name + ".png"))


def pixel_art(rows, palette):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows)
    image = Image.new("RGBA", (16, 16))
    image.putdata([(*palette[c], 255) if c != "." else (0, 0, 0, 0) for row in rows for c in row])
    return image


def main():
    # Seafood is reviewed artwork. Preserve both the new 32px assets and the
    # approved 16px assets byte-for-byte, including cooked flying fish.

    # The bowl is vanilla's beetroot-soup bowl; only its three soup colors change
    # to the actual soul-beet flesh palette, with no seed-colored cyan pixels.
    vanilla = Path.home() / ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"
    soup = source(vanilla, "assets/minecraft/textures/item/beetroot_soup.png")
    soup_colors = {(109, 22, 21, 255): (39, 28, 30, 255),
                   (132, 22, 13, 255): (56, 41, 44, 255),
                   (184, 42, 48, 255): (80, 60, 63, 255)}
    soup.putdata([soup_colors.get(pixel, pixel) for pixel in soup.getdata()])
    save("soul_beetroot_soup", soup)

    # Plum colors are sampled from the existing plum pie's filling.
    save("plum", pixel_art([
        "................", ".........ss.....", "........ss......", ".......as.......",
        ".....aabaaa.....", "....abcccbba....", "...abdecccbba...", "...abedcccbba...",
        "..abbddccbbbba..", "..abbcccbbbbba..", "..abcccbbbbaaa..", "...abbbbbbbaa...",
        "...abbbbbbaaa...", "....abbbbaaa....", ".....aaaaaa.....", "................",
    ], {"s": (101, 66, 33), "a": (50, 12, 70), "b": (82, 28, 109),
        "c": (110, 38, 135), "d": (129, 54, 153), "e": (146, 80, 166)}))

    # Reviewed seafood PNGs are preserved instead of regenerated.

    paths = sorted(path for path in ITEMS.glob("*.png") if path.stem.startswith(("raw_", "cooked_"))
                   or path.stem in {"plum", "plum_pie", "soul_beetroot", "soul_beetroot_soup"})
    sheet = Image.new("RGB", (768, ((len(paths) + 5) // 6) * 152), (55, 61, 68))
    draw = ImageDraw.Draw(sheet)
    for index, path in enumerate(paths):
        image = Image.open(path).convert("RGBA").resize((96, 96), Image.Resampling.NEAREST)
        x, y = (index % 6) * 128 + 16, (index // 6) * 152 + 8
        sheet.paste(image, (x, y), image)
        draw.text((x - 13, y + 104), path.stem.replace("_", "\n", 1), fill="white")
    out = ROOT / "tmp/food-textures-after.png"
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main()
