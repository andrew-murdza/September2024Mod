"""Check food atlas dimensions, own-texture model references and preserved masks."""
from io import BytesIO
from hashlib import sha256
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/aoemod"
CUSTOM = {"cooked_blue_spotted_stingray", "raw_blue_spotted_stingray", "raw_mahi_mahi",
          "sulfur_end_fish", "cooked_sulfur_end_fish", "cooked_triops"}
MATCHED_PAIRS = {
    "squid", "glow_squid", "arrow_squid", "vampire_squid",
    "anomalocaris", "octopus", "umbrella_octopus", "triops", "rubber_belly_glider",
    "blue_spotted_stingray",
}
DETAILED = {f"{state}_{name}" for name in MATCHED_PAIRS for state in ("raw", "cooked")} | {
    "cooked_starfish", "cooked_tuna",
}
# These four approved sprites intentionally retain their existing 16px artwork.
APPROVED = {
    "cooked_swamp_mucker": "97de9d4f097fb6c43bd54ab7208aadb3556227e9b748da96ae3a1dcfea210923",
    "cooked_carp": "48d55b2a70ddf3df8803ab6c03323f71263da4b8a4406ef7e181801c07a877c3",
    "cooked_flying_fish": "b7140ca6ba60eb75d989006d7b27b87229813df2b817200dcb500f3aca577be7",
    "cooked_mahi_mahi": "8819b48d14acf98f6a36b7c3b31fe35683a8948b8a93086a37108d17a25de51c",
}


def main():
    checked = []
    for path in (ASSETS / "models/item").glob("*.json"):
        if not (path.stem.startswith(("raw_", "cooked_")) or path.stem in CUSTOM):
            continue
        model = json.loads(path.read_text(encoding="utf-8"))
        assert model["textures"]["layer0"] == "aoemod:item/" + path.stem, path
        with Image.open(ASSETS / "textures/item" / (path.stem + ".png")) as image:
            size = 32 if path.stem in DETAILED else 16
            assert image.size == (size, size), (path, image.size, "expected", size)
            assert set(image.convert("RGBA").getchannel("A").getdata()) <= {0, 255}, path
        checked.append(path.stem)
    assert DETAILED <= set(checked), ("Missing detailed food models", sorted(DETAILED - set(checked)))
    for name in MATCHED_PAIRS:
        with Image.open(ASSETS / "textures/item" / f"raw_{name}.png") as raw, \
                Image.open(ASSETS / "textures/item" / f"cooked_{name}.png") as cooked:
            assert raw.convert("RGBA").getchannel("A").tobytes() == \
                cooked.convert("RGBA").getchannel("A").tobytes(), (name, "raw/cooked silhouette mismatch")
    for name, expected_hash in APPROVED.items():
        assert sha256((ASSETS / "textures/item" / f"{name}.png").read_bytes()).hexdigest() == expected_hash, \
            (name, "approved artwork changed")
    for name, jar, texture in [
        ("cooked_crab_meat", "ecologics-2.2.7.jar", "assets/ecologics/textures/item/crab_meat.png"),
        ("cooked_lost_tentacle", "alexsmobs-1.22.9.jar", "assets/alexsmobs/textures/item/lost_tentacle.png"),
        ("cooked_flying_fish", "alexsmobs-1.22.9.jar", "assets/alexsmobs/textures/item/flying_fish.png"),
        ("cooked_tuna", "hybrid_aquatic-1.7.4.jar", "assets/hybrid_aquatic/textures/item/tuna.png"),
        ("cooked_starfish", "hybrid_aquatic-1.7.4.jar", "assets/hybrid_aquatic/textures/item/starfish.png"),
    ]:
        with ZipFile(ROOT / "libs/pack" / jar) as archive:
            source = Image.open(BytesIO(archive.read(texture))).convert("RGBA")
        result = Image.open(ASSETS / "textures/item" / (name + ".png")).convert("RGBA")
        if name in DETAILED:
            source = source.resize((32, 32), Image.Resampling.NEAREST)
        assert source.getchannel("A").tobytes() == result.getchannel("A").tobytes(), name
        assert source.tobytes() != result.tobytes(), name
    print(json.dumps({"foods_checked": len(checked), "detailed_32px_foods": len(DETAILED),
                      "other_food_size": "16x16", "matched_raw_cooked_masks": len(MATCHED_PAIRS),
                      "approved_unchanged": len(APPROVED), "preserved_source_masks": 5,
                      "custom_placeholder_replacements": sorted(CUSTOM)}, indent=2))


if __name__ == "__main__":
    main()
