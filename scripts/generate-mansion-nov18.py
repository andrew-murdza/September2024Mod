#!/usr/bin/env python3
"""Create oak-only November 18 templates without changing saved mansion pieces."""
from pathlib import Path
import copy
import runpy
import zipfile

ROOT = Path(__file__).resolve().parents[1]
codec = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (codec[k] for k in ('Tag', 'read_nbt', 'write_nbt'))
OUT = ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion'
JAR = Path.home() / '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar'


def state(name, **properties):
    result = {'Name': Tag(8, 'minecraft:' + name)}
    if properties:
        result['Properties'] = Tag(10, {k: Tag(8, v) for k, v in properties.items()})
    return result


def decode(root):
    data = root[1].value
    palette = data['palette'].value[1]
    cells = {}
    for entry in data['blocks'].value[1]:
        block = copy.deepcopy(palette[entry['state'].value])
        if 'nbt' in entry:
            block['_nbt'] = copy.deepcopy(entry['nbt'])
        cells[tuple(entry['pos'].value[1])] = block
    return data['size'].value[1], cells


def save(name, size, cells):
    palette, blocks = [], []
    for pos, block in sorted(cells.items(), key=lambda v: (v[0][1], v[0][2], v[0][0])):
        block = copy.deepcopy(block)
        metadata = block.pop('_nbt', None)
        if block['Name'].value == 'minecraft:birch_planks':
            block = state('oak_planks')
        if block not in palette:
            palette.append(block)
        entry = {'pos': Tag(9, (3, list(pos))), 'state': Tag(3, palette.index(block))}
        if metadata is not None:
            entry['nbt'] = metadata
        blocks.append(entry)
    root = ('', Tag(10, {'DataVersion': Tag(3, 3465), 'size': Tag(9, (3, list(size))),
                        'palette': Tag(9, (10, palette)), 'blocks': Tag(9, (10, blocks)), 'entities': Tag(9, (10, []))}))
    (OUT / ('aoe_v6_' + name + '.nbt')).write_bytes(write_nbt(root))


def rotate_left(block):
    result = copy.deepcopy(block)
    props = result.get('Properties')
    if props:
        directions = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}
        source = props.value
        props.value = {directions.get(k, k): copy.deepcopy(v) for k, v in source.items()}
        if 'facing' in props.value:
            v = props.value['facing'].value
            props.value['facing'] = Tag(8, directions.get(v, v))
        if 'axis' in props.value and props.value['axis'].value != 'y':
            props.value['axis'] = Tag(8, 'z' if props.value['axis'].value == 'x' else 'x')
    return result


def generate():
    sources = {}
    with zipfile.ZipFile(JAR) as archive:
        prefix = 'data/minecraft/structures/woodland_mansion/'
        for path in archive.namelist():
            if path.startswith(prefix) and path.endswith('.nbt'):
                sources[Path(path).stem] = decode(read_nbt(archive.read(path)))
        entrance_size, vanilla_entrance = sources['entrance']
    # Prefer the current authored/compacted room geometry over vanilla heights.
    for name in list(sources):
        path = OUT / ('aoe_compact_' + name + '.nbt')
        if path.exists():
            sources[name] = decode(read_nbt(path.read_bytes()))
    overrides = {'1x2_d1': '1x2_d1_v3', '1x2_c4': '1x2_c4_v4', 'wall_corner': 'wall_corner_v3',
                 'stair_1x1': 'stair_1x1_v5', 'stair_frame': 'stair_frame_v5',
                 'basement_lobby': 'basement_lobby_v5', 'basement_frame': 'basement_frame_v5', 'ceiling': 'ceiling_v3'}
    overrides.update({name: name + '_v5' for name in ('roof', 'roof_front', 'roof_corner', 'roof_inner_corner')})
    for key, suffix in overrides.items():
        sources[key] = decode(read_nbt((OUT / ('aoe_compact_' + suffix + '.nbt')).read_bytes()))

    # Window lower rails match the upper end-grain log border. Preserve the
    # outer cobblestone railing on every storey below the roof.
    _, window = sources['wall_window']
    for z in range(1, 6):
        window[0, 1, z] = state('oak_log', axis='x')

    # Remove the asymmetric bookshelf/display, including its wooden surround.
    _, room = sources['2x2_b4']
    for x in range(5, 10):
        for y in range(1, 6):
            room[x, y, 14] = state('air')

    # The two marked bedroom blocks are the shelf and the support below it.
    _, room = sources['1x2_d1']
    room[6, 1, 0] = room[6, 2, 0] = state('air')

    # Four native-style three-wide beds replace the large booth/library room.
    # Each counts as two beds, with broad clear central aisles on both axes.
    beds = {(x, y, z): state('oak_planks' if y == 0 else 'air')
            for x in range(15) for z in range(15) for y in range(8)}
    for start_x in (1, 11):
        for start_z in (1, 10):
            for x in range(start_x, start_x + 3):
                for z in range(start_z, start_z + 4):
                    beds[x, 1, z] = state('white_wool')
                    if z != (start_z if start_z == 1 else start_z + 3):
                        beds[x, 2, z] = state('red_carpet')
            head = 0 if start_z == 1 else 14
            for x in range(start_x, start_x + 3):
                beds[x, 1, head] = state('oak_planks')
    sources['2x2_b3'] = ([15, 8, 15], beds)

    # A second inset stair course forms the requested stepped room cornice.
    _, room = sources['1x2_c4']
    for inset, y in ((0, 5), (1, 6)):
        for x in range(inset, 7 - inset):
            for z in range(inset, 15 - inset):
                if x not in (inset, 6 - inset) and z not in (inset, 14 - inset):
                    continue
                facing = 'west' if x == inset else 'east' if x == 6 - inset else 'north' if z == inset else 'south'
                shape = 'straight'
                if x in (inset, 6 - inset) and z in (inset, 14 - inset):
                    facing = 'north' if z == inset else 'south'
                    shape = 'inner_left' if (x == inset) == (z == inset) else 'inner_right'
                room[x, y, z] = state('oak_stairs', facing=facing, half='top', shape=shape, waterlogged='false')
                for fill_y in range(y + 1, 8):
                    room[x, fill_y, z] = state('oak_planks')

    # Additional lamps illuminate the underside of the lower stair doorway.
    _, stair = sources['stair_1x1']
    stair[2, 3, 0] = state('wall_torch', facing='east')
    stair[4, 3, 0] = state('wall_torch', facing='west')

    # All cavities below each roof column's highest solid cell become timber.
    # Only straight roof strips lose their cobblestone band; corner masonry
    # and the facade's lower-storey cobblestone decorations remain intact.
    for name in ('roof_front', 'roof_corner', 'roof_inner_corner'):
        size, roof = sources[name]
        for x in range(size[0]):
            for z in range(size[2]):
                solid = [y for xx, y, zz in roof if xx == x and zz == z and roof[xx, y, zz]['Name'].value != 'minecraft:air']
                if not solid:
                    continue
                for y in range(max(solid) + 1):
                    current = roof.get((x, y, z), state('air'))
                    if current['Name'].value == 'minecraft:air' or name == 'roof_front' and current['Name'].value in ('minecraft:cobblestone', 'minecraft:cobblestone_wall'):
                        roof[x, y, z] = state('oak_planks')

    # Copy the real vanilla exterior platform. Transform vanilla outward +X
    # to canonical outward -Z. Only the first two entry columns are replaced;
    # the regular windows and room layout behind them are untouched.
    portal = {(z - 4, y, 16 - x): rotate_left(vanilla_entrance[x, y, z])
              for x in (15, 16) for z in range(4, 11) for y in range(8)}
    sources['entrance'] = ([7, 8, 2], portal)
    platform = {}
    for x in range(15, 21):
        for z in range(2, 13):
            if x < 17 and not 4 <= z <= 10:
                continue
            for y in range(2):
                platform[z - 2, y, 20 - x] = rotate_left(vanilla_entrance[x, y, z])
    sources['entrance_platform'] = ([11, 2, 6], platform)

    for name, (size, cells) in sources.items():
        save(name, size, cells)
    print('Generated', len(sources), 'version-six mansion templates; older resources untouched')


if __name__ == '__main__':
    generate()
