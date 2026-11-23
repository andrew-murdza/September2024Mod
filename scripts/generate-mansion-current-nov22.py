#!/usr/bin/env python3
"""Build the current mansion facade and room details without changing saved V8 templates."""
from pathlib import Path
import copy
import json
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
    (OUT / ('aoe_v9_' + name + '.nbt')).write_bytes(write_nbt(root))


def generate():
    sources = {p.stem.removeprefix('aoe_v8_'): decode(read_nbt(p.read_bytes()))
               for p in OUT.glob('aoe_v8_*.nbt')}
    if len(sources) != 81:
        raise ValueError('Expected the complete preserved V8 template collection')
    for name in ('wall_window', 'wall_window_top', 'wall_flat'):
        _, cells = sources[name]
        for x in range(2):
            for z in range(8):
                cells[x, 6, z] = state('dark_oak_log', axis='z')
    sources['wall_flat_top'] = copy.deepcopy(sources['wall_flat'])
    for name in ('wall_window_top', 'wall_flat_top'):
        _, cells = sources[name]
        for x in range(2):
            for z in range(8):
                cells[x, 7, z] = state('dark_oak_planks')
        sources[name + '_trim'] = [2, 2, 8], {
            (x, y - 6, z): copy.deepcopy(cells[x, y, z])
            for x in range(2) for y in range(6, 8) for z in range(8)}

    # Only the top storey's corner rises another course into the eaves.
    # Its sparse trim is placed after the roof so the roof cannot erase it.
    size, corner = copy.deepcopy(sources['wall_corner'])
    for x in (7, 8):
        for z in range(2):
            corner[x, 8, z] = copy.deepcopy(corner[x, 7, z])
    # Extend the post into the eave without adding a detached cap above the roof.
    sources['wall_corner_top'] = size, corner
    sources['wall_corner_top_trim'] = [size[0], 3, size[2]], {
        (x, y - 6, z): copy.deepcopy(block) for (x, y, z), block in corner.items() if y >= 6}

    brackets = {}
    for name, (size, cells) in sources.items():
        if not name.startswith(('1x2_', '2x2_')) or size[1] != 8:
            continue
        sides = []
        if size[2] == 15:
            sides.extend([(0, 7, 'west'), (size[0] - 1, 7, 'east')])
        if size[0] == 15:
            sides.extend([(7, 0, 'north'), (7, size[2] - 1, 'south')])
        added = []
        for x, z, support in sides:
            # Mount above a bookcase when the normal bracket height is occupied.
            # All furniture and the one-room-wide end walls stay intact.
            for y in (3, 5):
                if all(cells.get((x, yy, z), state('structure_void'))['Name'].value == 'minecraft:air'
                       for yy in (y, y + 1)):
                    cells[x, y, z] = state('oak_fence', **{support: 'true'})
                    cells[x, y + 1, z] = state('torch')
                    added.append([x, y, z])
                    break
        if added:
            brackets[name] = added
    for name, (size, cells) in sources.items():
        save(name, size, cells)
    print(json.dumps({'templates': len(sources), 'long_wall_brackets': brackets}, indent=2))


if __name__ == '__main__':
    generate()
