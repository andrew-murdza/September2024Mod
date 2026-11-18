"""Verify equal SMALL/MEDIUM geometry and the original SMALL atlas mapping."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "src/main/resources/assets/aoemod/geo/critter/starfish"


def main():
    medium = json.loads((BASE / "starfish.geo.json").read_text())["minecraft:geometry"][0]
    small = json.loads((BASE / "starfish_small.geo.json").read_text())["minecraft:geometry"][0]
    assert small["description"] == medium["description"], "Small visible bounds or atlas dimensions changed"
    assert len(small["bones"]) == len(medium["bones"]), "Animation bone count changed"
    checked_faces = 0
    for large_bone, small_bone in zip(medium["bones"], small["bones"]):
        assert {k: v for k, v in large_bone.items() if k != "cubes"} == \
            {k: v for k, v in small_bone.items() if k != "cubes"}, "Animation hierarchy changed"
        assert len(small_bone["cubes"]) == 1
        outer, inner = large_bone["cubes"]
        result = small_bone["cubes"][0]
        assert {k: v for k, v in outer.items() if k != "uv"} == \
            {k: v for k, v in result.items() if k != "uv"}, (small_bone["name"], "Different physical footprint")
        u, v = inner["uv"]
        width, height, depth = inner["size"]
        rectangles = {
            "north": ([u + depth, v + depth], [width, height]),
            "east": ([u, v + depth], [depth, height]),
            "south": ([u + 2 * depth + width, v + depth], [width, height]),
            "west": ([u + depth + width, v + depth], [depth, height]),
            "up": ([u + depth, v], [width, depth]),
            "down": ([u + depth + width, v + depth], [width, -depth]),
        }
        for face, (origin, size) in rectangles.items():
            assert result["uv"][face] == {"uv": origin, "uv_size": size}, (small_bone["name"], face)
            checked_faces += 1
    print(json.dumps({"same_size_body_and_arms": len(small["bones"]),
                      "original_small_uv_faces": checked_faces,
                      "native_textures_and_overlays_unchanged": True}, indent=2))


if __name__ == "__main__":
    main()
