"""Install the November 22 player library plus the requested long-wall brackets.

The source was copied from New World (2), Plains dimension, at the player's
forced respawn (1238, 130, -537). Interior origin is (1242,129,-526), rotated
180 degrees. Its one doorway is the south short end in normalized coordinates.
"""
from pathlib import Path
import copy, hashlib, json, runpy

ROOT = Path(__file__).resolve().parents[1]
codec = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (codec[k] for k in ('Tag', 'read_nbt', 'write_nbt'))
OUT = ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion'
SOURCE = ROOT / 'scripts/data/illager-library-nov22-authored.nbt'


def generate():
    # Preserve the full original palette and block array. Generic room processing
    # is disabled so only the four explicit air cells below change in the room.
    data = SOURCE.read_bytes()
    expected = 'df52037bfeb8c5736025094158b33ef06ded397780aaf0b168e208f81400010e'
    assert hashlib.sha256(data).hexdigest() == expected, 'Authored library changed'
    OUT.mkdir(parents=True, exist_ok=True)
    library = copy.deepcopy(read_nbt(data)[1].value)
    palette = library['palette'].value[1]
    changes = {}
    for x, support in ((0, 'west'), (6, 'east')):
        properties = {side: Tag(8, 'true' if side == support else 'false')
                      for side in ('north', 'east', 'south', 'west', 'waterlogged')}
        changes[x, 3, 7] = {'Name': Tag(8, 'minecraft:oak_fence'), 'Properties': Tag(10, properties)}
        changes[x, 4, 7] = {'Name': Tag(8, 'minecraft:torch')}
    changed = []
    for entry in library['blocks'].value[1]:
        pos = tuple(entry['pos'].value[1])
        if pos not in changes:
            continue
        assert palette[entry['state'].value]['Name'].value == 'minecraft:air', 'Bracket would replace authored furniture'
        block = changes[pos]
        if block not in palette:
            palette.append(block)
        entry['state'] = Tag(3, palette.index(block))
        changed.append(pos)
    assert set(changed) == set(changes), 'Expected exactly two fence and torch pairs'
    final = write_nbt(('', Tag(10, library)))
    (OUT / 'aoe_v9_library.nbt').write_bytes(final)
    boundary = SOURCE.with_name('illager-library-nov22-boundary.nbt').read_bytes()
    assert hashlib.sha256(boundary).hexdigest() == '2770698249cd7d1ea9c5e05e06cad3cfcb4fe3a315a7340175535fcf39a711e2', 'Authored doorway changed'
    root = read_nbt(boundary)
    frame = copy.deepcopy(root[1].value)
    palette = frame['palette'].value[1]
    planks = {'Name': Tag(8, 'minecraft:oak_planks')}
    if planks not in palette:
        palette.append(planks)
    # The example's northern wall was an exterior facade. The library now sits
    # strictly indoors: replace only that wall with a solid indoor partition.
    # Keep both long walls and the authored southern doorway exactly as saved.
    for entry in frame['blocks'].value[1]:
        if entry['pos'].value[1][2] == 0:
            entry['state'] = Tag(3, palette.index(planks))
    (OUT / 'aoe_v9_library_frame.nbt').write_bytes(write_nbt(('', Tag(10, frame))))
    provenance_path = SOURCE.with_name('illager-library-nov22-provenance.json')
    provenance = json.loads(provenance_path.read_text())
    provenance['generatedTemplate'] = 'aoe_v9_library'
    provenance['generatedTemplateSHA256'] = hashlib.sha256(final).hexdigest()
    provenance['authoredInteriorCellCount'] = 840
    provenance['preservedInteriorCellCount'] = 836
    provenance['requestedAirCellAdditions'] = [{'pos': list(pos), 'block': changes[pos]['Name'].value} for pos in sorted(changes)]
    provenance['boundaryAdaptation'] = 'North exterior facade replaced with an indoor oak partition; other boundary cells preserved.'
    provenance_path.write_text(json.dumps(provenance, indent=2))
    refs = ROOT / 'references'
    refs.mkdir(exist_ok=True)
    (refs / 'library-generation-report.json').write_text(json.dumps(provenance, indent=2))
    print('Preserved 836 authored interior cells and the doorway; added two fence/torch brackets in four former air cells.')


if __name__ == '__main__':
    generate()
