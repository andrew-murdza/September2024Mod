#!/usr/bin/env python3
"""Build V8 mansion details from preserved V7 templates."""
from pathlib import Path
import copy
import runpy

ROOT = Path(__file__).resolve().parents[1]
codec = runpy.run_path(str(ROOT / 'scripts/generate-mansion-nov18.py'))
Tag, read_nbt, write_nbt, decode, state = (codec[k] for k in ('Tag', 'read_nbt', 'write_nbt', 'decode', 'state'))
OUT = codec['OUT']


def save(name, size, cells):
    palette, blocks = [], []
    for pos, original in sorted(cells.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        block = copy.deepcopy(original)
        metadata = block.pop('_nbt', None)
        if block not in palette:
            palette.append(block)
        entry = {'pos': Tag(9, (3, list(pos))), 'state': Tag(3, palette.index(block))}
        if metadata is not None:
            entry['nbt'] = metadata
        blocks.append(entry)
    root = ('', Tag(10, {'DataVersion': Tag(3, 3465), 'size': Tag(9, (3, list(size))),
                        'palette': Tag(9, (10, palette)), 'blocks': Tag(9, (10, blocks)), 'entities': Tag(9, (10, []))}))
    (OUT / ('aoe_v8_' + name + '.nbt')).write_bytes(write_nbt(root))


def generate():
    sources = {p.stem.removeprefix('aoe_v7_'): decode(read_nbt(p.read_bytes()))
               for p in OUT.glob('aoe_v7_*.nbt')}
    if len(sources) != 80:
        raise ValueError('Expected the complete preserved V7 template collection')

    # The pictured rug begins behind one bare wooden row inside the doorway.
    # Its white end border belongs to the rug, with no red runner outdoors.
    size, entrance = sources['entrance']
    for x in range(2, 5):
        entrance[x, 1, 0] = state('air')
    for x in range(7):
        entrance[x, 1, 1] = state('air')
        entrance[x, 0, 2] = state('oak_planks')
        entrance[x, 1, 2] = state('white_carpet' if 1 <= x <= 5 else 'air')
        for y in range(2, 8):
            entrance[x, y, 2] = state('structure_void')
    sources['entrance'] = [7, 8, 3], entrance
    _, platform = sources['entrance_platform']
    for pos, block in list(platform.items()):
        if block['Name'].value in ('minecraft:red_carpet', 'minecraft:white_carpet'):
            platform[pos] = state('air')

    # Fill both upper facade courses, including the projecting header above
    # the fence jambs. The separate trim is placed after the overlapping roof.
    size, window = sources['wall_window_top']
    trim = {}
    for x in range(2):
        for y in range(6, 8):
            for z in range(8):
                block = state('dark_oak_log', axis='x' if x == 0 else 'z') if y == 6 and 1 <= z <= 5 else state('dark_oak_planks')
                window[x, y, z] = copy.deepcopy(block)
                trim[x, y - 6, z] = block
    sources['wall_window_top_trim'] = [2, 2, 8], trim

    for name, (size, cells) in sources.items():
        save(name, size, cells)
    print(f'Generated {len(sources)} V8 templates; all saved V7 templates unchanged')


if __name__ == '__main__':
    generate()
