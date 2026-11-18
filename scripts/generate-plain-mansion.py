#!/usr/bin/env python3
"""Create saved-template-safe counterparts without decorative cobblestone wall blocks."""
from pathlib import Path
import argparse
import runpy
import zipfile

ROOT = Path(__file__).resolve().parents[1]
codec = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (codec[key] for key in ('Tag', 'read_nbt', 'write_nbt'))


def generate(jar_path):
    destination = ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion'
    prefix = 'data/minecraft/structures/woodland_mansion/'
    with zipfile.ZipFile(jar_path) as archive:
        sources = {Path(name).stem: archive.read(name) for name in archive.namelist()
                   if name.startswith(prefix) and name.endswith('.nbt')}
    sources.update({path.stem: path.read_bytes() for path in destination.glob('*.nbt')
                    if not path.stem.startswith('aoe_plain_')})
    generated = []
    for name, source in sorted(sources.items()):
        root = read_nbt(source)
        palette = root[1].value['palette'].value[1]
        changed = False
        for index, state in enumerate(palette):
            if state['Name'].value == 'minecraft:cobblestone_wall':
                palette[index] = {'Name': Tag(8, 'minecraft:air')}
                changed = True
        if changed:
            if name == 'aoe_compact_wall_corner_v3':
                # The new cap exists solely to keep the wall pillar symmetric.
                # Its plain counterpart has no pillar beneath it.
                air = {'Name': Tag(8, 'minecraft:air')}
                if air not in palette:
                    palette.append(air)
                for entry in root[1].value['blocks'].value[1]:
                    if entry['pos'].value[1] == [8, 8, 1]:
                        entry['state'] = Tag(3, palette.index(air))
            (destination / ('aoe_plain_' + name + '.nbt')).write_bytes(write_nbt(root))
            generated.append(name)
    print('Generated plain facade counterparts:', ', '.join(generated))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--minecraft-jar', type=Path,
                        default=Path.home() / '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar')
    generate(parser.parse_args().minecraft_jar)
