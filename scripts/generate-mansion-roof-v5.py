#!/usr/bin/env python3
"""Extend the mansion roof three blocks out; placement lowers it three blocks.

The previous resources remain immutable for saved mansions. Enlarged flat tiles
and extended straight strips overlap identically, then the corner templates
replace their intersections. The Java layout applies each rotated offset.
"""
from pathlib import Path
import copy
import runpy

ROOT = Path(__file__).resolve().parents[1]
CODEC = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (CODEC[key] for key in ('Tag', 'read_nbt', 'write_nbt'))
DIRECTORY = ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion'


def generate():
    for name in ('roof', 'roof_front', 'roof_corner', 'roof_inner_corner'):
        root = read_nbt((DIRECTORY / f'aoe_compact_{name}_v3.nbt').read_bytes())
        data = root[1].value
        cells = {tuple(entry['pos'].value[1]): entry for entry in data['blocks'].value[1]}
        result = {}
        if name == 'roof':
            # Template origin moves (-3,-3) horizontally. The 100 enlarged
            # tiles form one continuous plate, including all four overhangs.
            for x in range(14):
                for z in range(14):
                    for y in range(2):
                        source = (min(7, max(0, x - 3)), y, min(7, max(0, z - 3)))
                        result[x, y, z] = copy.deepcopy(cells[source])
            data['size'] = Tag(9, (3, [14, 2, 14]))
        elif name == 'roof_front':
            # Each end extends three blocks. Adjacent strips overlap with
            # identical states; corner templates finish the outer junctions.
            for z in range(14):
                source_z = min(7, max(0, z - 3))
                for (x, y, old_z), entry in cells.items():
                    if old_z == source_z:
                        result[x, y, z] = copy.deepcopy(entry)
            data['size'] = Tag(9, (3, [4, 5, 14]))
        else:
            result = cells
        for pos, entry in result.items():
            entry['pos'] = Tag(9, (3, list(pos)))
        data['blocks'] = Tag(9, (10, [result[pos] for pos in sorted(result, key=lambda pos: (pos[1], pos[2], pos[0]))]))
        (DIRECTORY / f'aoe_compact_{name}_v5.nbt').write_bytes(write_nbt(root))
    print('Generated four version-five roof templates; preserved version-three resources')


if __name__ == '__main__':
    generate()
