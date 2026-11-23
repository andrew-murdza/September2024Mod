#!/usr/bin/env python3
"""Revise new mansion details, preserving every saved V6 template byte-for-byte."""
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
    (OUT / ('aoe_v7_' + name + '.nbt')).write_bytes(write_nbt(root))


def generate():
    sources = {p.stem.removeprefix('aoe_v6_'): decode(read_nbt(p.read_bytes()))
               for p in OUT.glob('aoe_v6_*.nbt')}
    # The four-bed invention is not a new-generation room. Keep the native
    # template identity available; the planner splits large bedroom slots.
    sources['2x2_b3'] = decode(read_nbt((OUT / 'aoe_compact_2x2_b3.nbt').read_bytes()))
    for _, cells in sources.values():
        for pos, block in list(cells.items()):
            if block['Name'].value == 'minecraft:birch_planks':
                cells[pos] = state('oak_planks')

    # Only the fifth-floor header loses its cobblestone. Other floor rails
    # retain their existing masonry and all windows retain their glass.
    size, window = copy.deepcopy(sources['wall_window'])
    for pos, block in list(window.items()):
        if pos[1] == 7 and 'cobblestone' in block['Name'].value:
            window[pos] = state('oak_planks')
    sources['wall_window_top'] = size, window

    # The native flower/library room has a plain ceiling again. Keep all
    # bookshelves, flowers, furniture and lower room walls unchanged.
    size, room = sources['1x2_c4']
    for x in range(size[0]):
        for z in range(size[2]):
            for y in range(5, 8):
                room[x, y, z] = state('oak_planks' if y == 7 else 'air')

    # Match the photographed block platform and continuous red threshold.
    for name in ('entrance', 'entrance_platform'):
        size, cells = sources[name]
        for pos, block in list(cells.items()):
            if block['Name'].value == 'minecraft:cobblestone_stairs':
                cells[pos] = state('cobblestone')

    # Bring the roof rim two blocks inward: one-block overhang instead of
    # three. The layout applies matching rotated origin offsets. Adjacent
    # flat tiles overlap by one identical column on the eight-block pitch.
    # The plate ends on the slope's peak, never over its next stair row.
    for name in ('roof', 'roof_front'):
        size, cells = sources[name]
        if name == 'roof':
            cells = {(x, y, z): copy.deepcopy(cells[x + 2, y, z + 2])
                     for x in range(9) for z in range(9) for y in range(2)}
            sources[name] = [9, 2, 9], cells
        else:
            cells = {(x, y, z - 2): block for (x, y, z), block in cells.items() if 2 <= z < 12}
            sources[name] = [4, 5, 10], cells

    for name in ('roof_front', 'roof_corner', 'roof_inner_corner'):
        size, cells = sources[name]
        # Remove the flat under-eave board and all exposed corner masonry.
        # Fill the slope behind its stairs; air above the slope must not
        # erase a neighboring roof piece at overlapping edge intersections.
        for x in range(size[0]):
            for z in range(size[2]):
                top = max((y for (xx, y, zz), b in cells.items()
                           if xx == x and zz == z and b['Name'].value != 'minecraft:air'), default=0)
                # This course overlaps the fifth-floor facade. Omit the
                # under-eave board without clearing the wall beneath it.
                cells[x, 0, z] = state('structure_void')
                for y in range(1, top + 1):
                    block = cells.get((x, y, z), state('air'))
                    if block['Name'].value == 'minecraft:air' or 'cobblestone' in block['Name'].value:
                        cells[x, y, z] = state('oak_planks')
                for y in range(top + 1, size[1]):
                    # Convex corners trim the extended straight-strip ends.
                    # Their empty space must clear those four excess slab
                    # tips, while other overlaps retain their neighbors.
                    cells[x, y, z] = state('air' if name == 'roof_corner' else 'structure_void')
    for name, (size, cells) in sources.items():
        save(name, size, cells)
    print(f'Generated {len(sources)} V7 templates; earlier templates unchanged')


if __name__ == '__main__':
    generate()
