#!/usr/bin/env python3
"""Preserve native Orcz camps while correcting their palisades and paired bone horns.

The small tent uses a low arm and an upturned tip; the large tent uses two
stairs joined by a full middle block. Both profiles are mirrored about the tent.
This small typed NBT codec preserves every source tag type without dependencies.
"""
from dataclasses import dataclass
import copy
import gzip
import io
from pathlib import Path
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]


@dataclass(eq=True)
class Tag:
    kind: int
    value: object


def read_nbt(data):
    stream = io.BytesIO(gzip.decompress(data))

    def number(fmt):
        return struct.unpack('>' + fmt, stream.read(struct.calcsize('>' + fmt)))[0]

    def string():
        return stream.read(number('H')).decode('utf-8')

    def payload(kind):
        if 1 <= kind <= 6:
            return number(['', 'b', 'h', 'i', 'q', 'f', 'd'][kind])
        if kind == 7:
            return stream.read(number('i'))
        if kind == 8:
            return string()
        if kind == 9:
            item_kind, count = number('b'), number('i')
            return item_kind, [payload(item_kind) for _ in range(count)]
        if kind == 10:
            result = {}
            while (item_kind := number('b')) != 0:
                name = string()
                result[name] = Tag(item_kind, payload(item_kind))
            return result
        if kind in (11, 12):
            return [number('i' if kind == 11 else 'q') for _ in range(number('i'))]
        raise ValueError(f'Unsupported NBT tag {kind}')

    kind = number('b')
    name = string()
    result = name, Tag(kind, payload(kind))
    assert not stream.read(), 'Unexpected bytes following NBT root'
    return result


def write_nbt(root):
    stream = io.BytesIO()

    def number(fmt, value):
        stream.write(struct.pack('>' + fmt, value))

    def string(value):
        encoded = value.encode('utf-8')
        number('H', len(encoded))
        stream.write(encoded)

    def payload(kind, value):
        if 1 <= kind <= 6:
            number(['', 'b', 'h', 'i', 'q', 'f', 'd'][kind], value)
        elif kind == 7:
            number('i', len(value))
            stream.write(value)
        elif kind == 8:
            string(value)
        elif kind == 9:
            item_kind, items = value
            number('b', item_kind)
            number('i', len(items))
            for item in items:
                payload(item_kind, item)
        elif kind == 10:
            for name, tag in value.items():
                number('b', tag.kind)
                string(name)
                payload(tag.kind, tag.value)
            number('b', 0)
        elif kind in (11, 12):
            number('i', len(value))
            for item in value:
                number('i' if kind == 11 else 'q', item)
        else:
            raise ValueError(f'Unsupported NBT tag {kind}')

    name, tag = root
    number('b', tag.kind)
    string(name)
    payload(tag.kind, tag.value)
    return gzip.compress(stream.getvalue(), mtime=0)


def revise(root, y_offset, z_offset):
    data = root[1].value
    palette = data['palette'].value[1]
    blocks = data['blocks'].value[1]
    by_pos = {tuple(block['pos'].value[1]): block for block in blocks}
    replacements = {
        'orcz:palisadedarkoak': 'aoemod:acacia_palisade',
        'orcz:pallisade': 'aoemod:acacia_palisade',
        # The desert skin uses sandstone walls for the same perimeter palisade.
        'minecraft:sandstone_wall': 'aoemod:acacia_palisade',
        'minecraft:smooth_quartz_stairs': 'alexscaves:smooth_bone_stairs',
        'minecraft:smooth_quartz_slab': 'alexscaves:smooth_bone_slab',
        'orcz:orc_wood_log': 'minecraft:black_wool',
        'orcz:orc_wood_planks': 'minecraft:black_wool',
        'orcz:orc_wood_slab': 'minecraft:black_wool_slab',
        'orcz:scrapblock': 'minecraft:bone_block',
    }
    for state in palette:
        state['Name'].value = replacements.get(state['Name'].value, state['Name'].value)
        if state['Name'].value == 'minecraft:black_wool':
            state.pop('Properties', None)
        if state['Name'].value.startswith('orcz:orcsymbol'):
            state.clear()
            state['Name'] = Tag(8, 'minecraft:air')

    def mirror(source, target, opposite):
        source = (source[0], source[1] + y_offset, source[2] + z_offset)
        target = (target[0], target[1] + y_offset, target[2] + z_offset)
        state = copy.deepcopy(palette[by_pos[source]['state'].value])
        properties = state.get('Properties')
        if properties is not None and 'facing' in properties.value:
            facing = properties.value['facing']
            facing.value = opposite.get(facing.value, facing.value)
        if state not in palette:
            palette.append(state)
        by_pos[target]['state'].value = palette.index(state)

    # Small building, center X=15: preserve the reference's flat arm and raised tip.
    for x in (12, 13):
        mirror((x, 8, 32), (30 - x, 8, 32), {'west': 'east', 'east': 'west'})
    # Large building, center Z=18: the full middle block joins the two stair steps.
    middle = {'Name': Tag(8, 'alexscaves:smooth_bone')}
    if middle not in palette:
        palette.append(middle)
    by_pos[(24, 9 + y_offset, 13 + z_offset)]['state'].value = palette.index(middle)
    for y, z in ((9, 13), (10, 13), (9, 14)):
        mirror((24, y, z), (24, y, 36 - z), {'north': 'south', 'south': 'north'})


def main():
    with zipfile.ZipFile(ROOT / 'libs/pack/orcz-0.87-1.20.1.jar') as archive:
        for biome, offsets in [('normal', (0, 0)), ('desert', (1, 0)), ('snow', (1, -1))]:
            name = f'data/orcz/structures/orc_camp_{biome}_loot.nbt'
            source = read_nbt(archive.read(name))
            assert read_nbt(write_nbt(source)) == source, 'NBT round trip changed source tags'
            revised = copy.deepcopy(source)
            revise(revised, *offsets)
            encoded = write_nbt(revised)
            assert read_nbt(encoded) == revised, 'Output did not preserve revised NBT'
            target = ROOT / 'src/main/resources' / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(encoded)
            print(f'Wrote {target.relative_to(ROOT)} ({len(encoded)} bytes)')


if __name__ == '__main__':
    main()
