"""Check food atlas dimensions, own-texture model references and preserved masks."""
from io import BytesIO
from hashlib import sha256
import json
from pathlib import Path
from zipfile import ZipFile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/aoemod"
CUSTOM = {"cooked_blue_spotted_stingray", "raw_blue_spotted_stingray",
          "sulfur_end_fish", "cooked_sulfur_end_fish", "cooked_triops"}
MATCHED_PAIRS = {
    "squid", "glow_squid", "arrow_squid", "vampire_squid",
    "octopus", "umbrella_octopus", "triops",
    "blue_spotted_stingray",
}
DETAILED = {f"{state}_{name}" for name in MATCHED_PAIRS for state in ("raw", "cooked")} | {
    "cooked_starfish", "cooked_tuna",
}
# This approved sprite intentionally retains its existing 16px artwork.
APPROVED = {
    "cooked_flying_fish": "b7140ca6ba60eb75d989006d7b27b87229813df2b817200dcb500f3aca577be7",
}
RETIRED = {"raw_mahi_mahi", "cooked_mahi_mahi", "raw_rubber_belly_glider",
           "cooked_rubber_belly_glider", "cooked_carp", "raw_anomalocaris",
           "cooked_anomalocaris", "cooked_swamp_mucker", "cooked_lost_tentacle"}
SOURCE_MASKS = [
    ("cooked_crab_meat", "ecologics-2.2.7.jar", "assets/ecologics/textures/item/crab_meat.png"),
    ("cooked_flying_fish", "alexsmobs-1.22.9.jar", "assets/alexsmobs/textures/item/flying_fish.png"),
    ("cooked_tuna", "hybrid_aquatic-1.7.4.jar", "assets/hybrid_aquatic/textures/item/tuna.png"),
    ("cooked_starfish", "hybrid_aquatic-1.7.4.jar", "assets/hybrid_aquatic/textures/item/starfish.png"),
]


def main():
    checked = []
    for name in RETIRED:
        assert not (ASSETS / "models/item" / f"{name}.json").exists(), (name, "retired model remains")
        assert not (ASSETS / "textures/item" / f"{name}.png").exists(), (name, "retired texture remains")
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
    assert CUSTOM <= set(checked), ("Missing custom food models", sorted(CUSTOM - set(checked)))
    for name in MATCHED_PAIRS:
        with Image.open(ASSETS / "textures/item" / f"raw_{name}.png") as raw, \
                Image.open(ASSETS / "textures/item" / f"cooked_{name}.png") as cooked:
            assert raw.convert("RGBA").getchannel("A").tobytes() == \
                cooked.convert("RGBA").getchannel("A").tobytes(), (name, "raw/cooked silhouette mismatch")
    for name, expected_hash in APPROVED.items():
        assert sha256((ASSETS / "textures/item" / f"{name}.png").read_bytes()).hexdigest() == expected_hash, \
            (name, "approved artwork changed")
    for name, jar, texture in SOURCE_MASKS:
        with ZipFile(ROOT / "libs/pack" / jar) as archive:
            source = Image.open(BytesIO(archive.read(texture))).convert("RGBA")
        result = Image.open(ASSETS / "textures/item" / (name + ".png")).convert("RGBA")
        if name in DETAILED:
            source = source.resize((32, 32), Image.Resampling.NEAREST)
        assert source.getchannel("A").tobytes() == result.getchannel("A").tobytes(), name
        assert source.tobytes() != result.tobytes(), name
    print(json.dumps({"foods_checked": len(checked), "detailed_32px_foods": len(DETAILED),
                      "other_food_size": "16x16", "matched_raw_cooked_masks": len(MATCHED_PAIRS),
                      "approved_unchanged": len(APPROVED), "preserved_source_masks": len(SOURCE_MASKS),
                      "retired_food_names_absent": sorted(RETIRED),
                      "custom_placeholder_replacements": sorted(CUSTOM)}, indent=2))


if __name__ == "__main__":
    main()
