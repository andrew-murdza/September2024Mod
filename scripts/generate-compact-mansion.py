#!/usr/bin/env python3
"""Build versioned eight-block mansion templates without changing vanilla/legacy resources."""
from pathlib import Path
import argparse, copy, json, runpy, zipfile
ROOT = Path(__file__).resolve().parents[1]
codec = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (codec[k] for k in ('Tag', 'read_nbt', 'write_nbt'))
ROOMS = ['1x1_a1','1x1_a3','1x1_a5','1x1_b2','1x1_b3','1x1_b4','1x1_as4',
         '1x2_c1','1x2_c3','1x2_c4','1x2_d1','2x2_b1','2x2_b2','2x2_b3','2x2_b4']
ROOFS = ['roof','roof_front','roof_corner','roof_inner_corner']

def generate(jar_path):
    out = ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion'
    out.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar_path) as archive:
        for name in ROOMS + ['entrance','1x2_d_stairs'] + ROOFS:
            root = read_nbt(archive.read('data/minecraft/structures/woodland_mansion/' + name + '.nbt'))
            data = root[1].value
            palette = data['palette'].value[1]
            blocks = {tuple(b['pos'].value[1]): b for b in data['blocks'].value[1]}
            size = data['size'].value[1]
            def state(pos):
                return palette[blocks[pos]['state'].value]['Name'].value if pos in blocks else 'minecraft:air'
            def put(pos, name, properties=None):
                p = {'Name': Tag(8, name)}
                if properties: p['Properties'] = Tag(10, {k: Tag(8,v) for k,v in properties.items()})
                if p not in palette: palette.append(p)
                blocks[pos] = {'pos':Tag(9,(3,list(pos))), 'state':Tag(3,palette.index(p))}
            if name in ROOMS or name == 'entrance':
                height = 16 if name == 'entrance' else 8
                if name == '1x2_d1':
                    # Preserve the raised center banner under the lower ceiling.
                    banner = copy.deepcopy(blocks[(3,8,13)])
                    banner['pos'] = Tag(9,(3,[3,7,13]))
                    blocks[(3,7,13)] = banner
                blocks = {p:b for p,b in blocks.items() if p[1] < height}
                size[1] = height
            elif name == '1x2_d_stairs':
                original = copy.deepcopy(blocks)
                for x in range(7):
                    for z in range(15):
                        for y in range(8,22): put((x,y,z),'minecraft:air')
                        if z <= 5: put((x,8,z),'minecraft:birch_planks')
                # Retain the last native cobblestone tread, then step directly
                # onto the floor at Y8; all three raised birch steps disappear.
                for x in range(2,5):
                    blocks[(x,8,6)] = copy.deepcopy(original[(x,8,6)])
                # Keep the native illager face above the back of the stairwell,
                # lowered with the room, while leaving the upper landing clear.
                for p,b in original.items():
                    if p[1] >= 12 and p[2] >= 13:
                        q=(p[0],p[1]-3,p[2])
                        c=copy.deepcopy(b);c['pos']=Tag(9,(3,list(q)));blocks[q]=c
                blocks={p:b for p,b in blocks.items() if p[1]<16};size[1]=16
            else:
                # Cap only full wood blocks. Native stair slopes already have
                # their correct half-step silhouette and remain unchanged.
                tops={}
                for p in blocks:
                    if state(p)!='minecraft:air':tops[(p[0],p[2])]=max(tops.get((p[0],p[2]),-1),p[1])
                for (x,z),y in tops.items():
                    if state((x,y,z)) in ('minecraft:dark_oak_planks','minecraft:oak_planks'):
                        put((x,y+1,z),'minecraft:oak_slab',{'type':'bottom','waterlogged':'false'})
                    elif state((x,y,z)).endswith('_stairs'):
                        # An edge/corner is placed after the flat roof tile. Its
                        # slope replaces that tile's plank, so also clear the
                        # earlier cap immediately above the stair.
                        put((x,y+1,z),'minecraft:air')
                size[1]=max(p[1] for p in blocks)+1
            data['blocks']=Tag(9,(10,[blocks[p] for p in sorted(blocks,key=lambda p:(p[1],p[2],p[0]))]))
            (out / ('aoe_compact_' + name + '.nbt')).write_bytes(write_nbt(root))
    # A new identifier keeps already-saved compact entrances byte-for-byte
    # compatible. The normalized capture is independent of the user's save.
    root = read_nbt((out / 'aoe_compact_entrance.nbt').read_bytes())
    data = root[1].value
    palette = data['palette'].value[1]
    blocks = {tuple(b['pos'].value[1]): b for b in data['blocks'].value[1]}
    authored = json.loads((ROOT / 'src/main/resources/data/aoemod/structure_data/mansion_entrance_v2.json').read_text())
    def state_tag(state):
        result = {'Name': Tag(8, state['Name'])}
        if 'Properties' in state:
            result['Properties'] = Tag(10, {k: Tag(8,v) for k,v in state['Properties'].items()})
        return result
    for x,y,z,index in authored['blocks']:
        state = state_tag(authored['palette'][index])
        if state not in palette: palette.append(state)
        blocks[x,y,z] = {'pos':Tag(9,(3,[x,y,z])), 'state':Tag(3,palette.index(state))}
    data['blocks']=Tag(9,(10,[blocks[p] for p in sorted(blocks,key=lambda p:(p[1],p[2],p[0]))]))
    (out / 'aoe_compact_entrance_v2.nbt').write_bytes(write_nbt(root))
    print('Generated',len(ROOMS)+2+len(ROOFS),'versioned compact mansion templates')
    print('Generated the authored November16 entrance as aoe_compact_entrance_v2')
    generate_revision_three(jar_path, out)
    generate_revision_four(out)
    generate_revision_five(out)


def generate_revision_three(jar_path, out):
    """New identities preserve saved mansions while replacing their stair/roof plan."""
    def save(name, size, cells):
        palette, blocks = [], []
        for pos, state in sorted(cells.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
            tag = {'Name': Tag(8, state['Name'])}
            if state.get('Properties'):
                tag['Properties'] = Tag(10, {k: Tag(8, v) for k, v in state['Properties'].items()})
            if tag not in palette:
                palette.append(tag)
            blocks.append({'pos': Tag(9, (3, list(pos))), 'state': Tag(3, palette.index(tag))})
        root = ('', Tag(10, {'DataVersion': Tag(3, 3465), 'size': Tag(9, (3, size)),
                            'palette': Tag(9, (10, palette)), 'blocks': Tag(9, (10, blocks)),
                            'entities': Tag(9, (10, []))}))
        (out / (name + '.nbt')).write_bytes(write_nbt(root))

    def block(name, **properties):
        return {'Name': 'minecraft:' + name, **({'Properties': properties} if properties else {})}

    # The player's latest spawn: a three-wide straight flight in one 7x7 room.
    # Z0 and Z8 are the shared doorway planes; the eighth tread is in Z8.
    cells = {}
    for x in range(7):
        for z in range(9):
            for y in range(16):
                state = block('birch_planks' if y == 0 else 'oak_planks')
                if 2 <= x <= 4:
                    if z == 0 and 1 <= y <= 3:
                        state = block('air')
                    elif z >= 1 and y == z:
                        state = block('oak_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
                    elif z >= 1 and z < y <= z + 4:
                        state = block('air')
                cells[x, y, z] = state
    save('aoe_compact_stair_1x1_v3', [7, 16, 9], cells)

    # The cellar piece supplies its descending treads; this versioned marker
    # reserves the sole ground-floor cell and its north-facing hall doorway.
    cells = {}
    for x in range(7):
        for z in range(8):
            for y in range(8):
                state = block('birch_planks' if y in (0, 7) else 'oak_planks')
                if 2 <= x <= 4 and ((z == 0 and 1 <= y <= 3) or (z >= 1 and y <= 5 - z)):
                    state = block('air')
                cells[x, y, z] = state
    save('aoe_compact_basement_lobby_v3', [7, 8, 8], cells)

    # A one-cell facade doorway faces south into the house. It is overlaid on
    # the native exterior so the four entrances retain matching oak surrounds.
    cells = {}
    for x in range(7):
        for z in range(2):
            for y in range(8):
                state = block('birch_planks' if y == 0 else 'oak_planks')
                if 1 <= x <= 5 and 1 <= y <= 4:
                    state = block('air')
                cells[x, y, z] = state
    save('aoe_compact_entrance_v3', [7, 8, 2], cells)
    save('aoe_compact_ceiling_v3', [8, 2, 8],
         {(x, y, z): block('birch_planks' if y == 0 else 'oak_planks')
          for x in range(8) for z in range(8) for y in range(2)})

    # The canopy in the large bedroom intercepts the chandelier stem.
    root = read_nbt((out / 'aoe_compact_1x2_d1.nbt').read_bytes())
    data = root[1].value
    palette = data['palette'].value[1]
    air = {'Name': Tag(8, 'minecraft:air')}
    if air not in palette:
        palette.append(air)
    for entry in data['blocks'].value[1]:
        if entry['pos'].value[1][1] >= 3 and palette[entry['state'].value]['Name'].value in (
                'minecraft:oak_slab', 'minecraft:oak_planks', 'minecraft:oak_fence'):
            entry['state'] = Tag(3, palette.index(air))
    (out / 'aoe_compact_1x2_d1_v3.nbt').write_bytes(write_nbt(root))

    for name in ROOFS:
        root = read_nbt((out / ('aoe_compact_' + name + '.nbt')).read_bytes())
        data = root[1].value
        palette = data['palette'].value[1]
        # Native edge templates clear their lower empty cells. Omit those
        # entries so the new solid ceiling/beam survives below the slope.
        # STRUCTURE_VOID is not ignored by WoodlandMansionPiece settings.
        data['blocks'] = Tag(9, (10, [entry for entry in data['blocks'].value[1]
                if not (entry['pos'].value[1][1] <= 1
                        and palette[entry['state'].value]['Name'].value == 'minecraft:air')]))
        (out / ('aoe_compact_' + name + '_v3.nbt')).write_bytes(write_nbt(root))

    with zipfile.ZipFile(jar_path) as archive:
        root = read_nbt(archive.read('data/minecraft/structures/woodland_mansion/wall_corner.nbt'))
        data = root[1].value
        for state in data['palette'].value[1]:
            if state['Name'].value == 'minecraft:cobblestone_wall':
                # Both inward arms have identical height at every storey.
                state['Properties'].value['north'] = Tag(8, 'tall')
                state['Properties'].value['west'] = Tag(8, 'tall')
        # Template placement recomputes wall shapes. A full cap gives both
        # inward arms identical physical support, so that recomputation and
        # later neighbor updates keep the whole pillar symmetric.
        palette = data['palette'].value[1]
        stone = next(i for i, state in enumerate(palette) if state['Name'].value == 'minecraft:cobblestone')
        data['blocks'].value[1].append({'pos': Tag(9, (3, [8, 8, 1])), 'state': Tag(3, stone)})
        data['size'].value[1][1] = 9
        (out / 'aoe_compact_wall_corner_v3.nbt').write_bytes(write_nbt(root))
    print('Generated revision-three stairs, entrances, ceiling, bedroom and corner templates')


def generate_revision_four(out):
    """November 17's taller authored doorway and translated stepped ceiling."""
    def block(name, **properties):
        return {'Name': 'minecraft:' + name, **({'Properties': properties} if properties else {})}

    def save(name, size, cells):
        palette, blocks = [], []
        for pos, state in sorted(cells.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
            tag = {'Name': Tag(8, state['Name'])}
            if state.get('Properties'):
                tag['Properties'] = Tag(10, {k: Tag(8, v) for k, v in state['Properties'].items()})
            if tag not in palette:
                palette.append(tag)
            blocks.append({'pos': Tag(9, (3, list(pos))), 'state': Tag(3, palette.index(tag))})
        root = ('', Tag(10, {'DataVersion': Tag(3, 3465), 'size': Tag(9, (3, size)),
                            'palette': Tag(9, (10, palette)), 'blocks': Tag(9, (10, blocks)),
                            'entities': Tag(9, (10, []))}))
        (out / (name + '.nbt')).write_bytes(write_nbt(root))

    # Captured read-only in New World (4): the taller wooden flight begins at
    # (-597,113,134), facing north, above floor112. Its lower door is four
    # blocks high; the second flight still has the old three-high doorway.
    # Canonical +Z ascends. Keep every walking tread and both landing floors.
    cells = {}
    for x in range(7):
        for z in range(9):
            for y in range(16):
                state = block('birch_planks' if y == 0 else 'oak_planks')
                if 2 <= x <= 4:
                    if z == 0 and 1 <= y <= 5:
                        state = block('air')
                    elif z >= 1 and y == z:
                        state = block('oak_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
                    elif z >= 1 and y > z and (z >= 6 or y < z + 6):
                        state = block('air')
                cells[x, y, z] = state
    # The old stepped soffit spanned z0..8 at y=z+5. Moving it three
    # toward the lower hall and three down, then raising the wooden ceiling
    # one, puts its endpoints at (-3,3) and (5,11). The last three columns
    # open to the flat birch ceiling; their walking treads remain unchanged.
    for x in (2, 4):
        for y, z in ((5, 0), (9, 4)):
            cells[x, y, z] = block('wall_torch', facing='south')
    # Blank faces opposite each deliberate doorway use the native log cross.
    for z, center_y in ((8, 4), (0, 12)):
        for step in range(-2, 3):
            cells[3 + step, center_y, z] = block('oak_log', axis='z')
            cells[3, center_y + step, z] = block('oak_log', axis='z')
    save('aoe_compact_stair_1x1_v4', [7, 16, 9], cells)

    # Sparse shared walls close the native side doorway and all of its trim.
    # Keeping this frame separate leaves the room-access graph's one-cell
    # staircase bounds unchanged. Its origin is core(-1,0,-3).
    frame = {}
    def frame_put(x, y, z, state):
        frame[x + 1, y, z + 3] = state
    for x in (-1, 7):
        for z in range(1, 8):
            for y in list(range(1, 8)) + list(range(9, 16)):
                center_y = 4 if y < 8 else 12
                cross = y == center_y and abs(z - 4) <= 2 or z == 4 and abs(y - center_y) <= 2
                frame_put(x, y, z, block('oak_log', axis='x') if cross else block('oak_planks'))
    for x in range(7):
        for z in range(9):
            frame_put(x, 16, z, block('birch_planks'))
    for x in range(2, 5):
        for z in range(-3, 0):
            for y in range(z + 6, 8):
                frame_put(x, y, z, block('oak_planks'))
    save('aoe_compact_stair_frame_v4', [9, 17, 12], frame)

    # The independently versioned cellar supplies the descending flight.
    # Its upper room retains a full eight-block storey and a taller doorway.
    cells = {}
    for x in range(7):
        for z in range(8):
            for y in range(9):
                state = block('birch_planks' if y in (0, 8) else 'oak_planks')
                if 2 <= x <= 4 and ((z == 0 and 1 <= y <= 4) or z >= 1 and y <= 7 - z):
                    state = block('air')
                cells[x, y, z] = state
    save('aoe_compact_basement_lobby_v4', [7, 9, 8], cells)
    frame = {}
    for x in (-1, 7):
        for z in range(1, 8):
            for y in range(1, 8):
                cross = y == 4 and abs(z - 4) <= 2 or z == 4 and abs(y - 4) <= 2
                frame[x + 1, y, z] = block('oak_log', axis='x') if cross else block('oak_planks')
    for x in range(-1, 8):
        for y in range(1, 9):
            cross = y == 4 and abs(x - 3) <= 2 or x == 3 and abs(y - 4) <= 2
            frame[x + 1, y, 8] = block('birch_planks') if y == 8 else block('oak_log', axis='z') if cross else block('oak_planks')
    save('aoe_compact_basement_frame_v4', [9, 9, 9], frame)

    # Six solid posts interrupted the old stair-only repair, leaving holes
    # between the cornice and the ceiling. Fill the entire perimeter course.
    root = read_nbt((out / 'aoe_compact_1x2_c4.nbt').read_bytes())
    data = root[1].value
    palette = data['palette'].value[1]
    oak = {'Name': Tag(8, 'minecraft:oak_planks')}
    if oak not in palette:
        palette.append(oak)
    for entry in data['blocks'].value[1]:
        x, y, z = entry['pos'].value[1]
        if y == 7 and (x in (0, 6) or z in (0, 14)):
            entry['state'] = Tag(3, palette.index(oak))
    (out / 'aoe_compact_1x2_c4_v4.nbt').write_bytes(write_nbt(root))
    print('Generated revision-four stair, shared frame, cellar lobby, and continuous room cornice')


def generate_revision_five(out):
    """Correct the ceiling scope without changing saved revision-four pieces."""
    def load(name):
        root = read_nbt((out / (name + '.nbt')).read_bytes())
        data = root[1].value
        return root, data, {tuple(b['pos'].value[1]): b for b in data['blocks'].value[1]}

    def put(data, cells, pos, name, **properties):
        state = {'Name': Tag(8, 'minecraft:' + name)}
        if properties:
            state['Properties'] = Tag(10, {k: Tag(8, v) for k, v in properties.items()})
        palette = data['palette'].value[1]
        if state not in palette:
            palette.append(state)
        cells[pos] = {'pos': Tag(9, (3, list(pos))), 'state': Tag(3, palette.index(state))}

    def save(root, data, cells, name, size):
        data['size'] = Tag(9, (3, size))
        data['blocks'] = Tag(9, (10, [cells[p] for p in sorted(cells, key=lambda p: (p[1], p[2], p[0]))]))
        (out / (name + '.nbt')).write_bytes(write_nbt(root))

    root, data, cells = load('aoe_compact_stair_1x1_v4')
    # The down/out request concerns the mansion roof, not this flight. Restore
    # its full authored four-air stepped ceiling and the original torch pairs.
    # Only the lower doorway's original header at y5 moves to y6.
    for x in range(2, 5):
        for y in range(1, 6):
            put(data, cells, (x, y, 0), 'air')
        for z in range(1, 9):
            for y in range(z + 1, 16):
                put(data, cells, (x, y, z), 'air' if y <= z + 4 else 'oak_planks')
    for x in (2, 4):
        for y, z in ((7, 3), (11, 7)):
            put(data, cells, (x, y, z), 'wall_torch', facing='south')
    save(root, data, cells, 'aoe_compact_stair_1x1_v5', [7, 16, 9])

    root, data, old_cells = load('aoe_compact_stair_frame_v4')
    cells = {}
    for (x, y, z), cell in old_cells.items():
        if z < 3:
            continue  # remove the incorrectly added canopy over the lower hall
        pos = (x, y, z - 3)
        cell['pos'] = Tag(9, (3, list(pos)))
        cells[pos] = cell
    # Y16 is the existing next-storey floor, not a changed interior ceiling.
    save(root, data, cells, 'aoe_compact_stair_frame_v5', [9, 17, 9])

    root, data, old_cells = load('aoe_compact_basement_lobby_v4')
    cells = {p: b for p, b in old_cells.items() if p[1] < 8}
    for x in range(7):
        for z in range(8):
            put(data, cells, (x, 7, z), 'birch_planks')
    save(root, data, cells, 'aoe_compact_basement_lobby_v5', [7, 8, 8])

    root, data, old_cells = load('aoe_compact_basement_frame_v4')
    cells = {p: b for p, b in old_cells.items() if p[1] < 8}
    for x in range(9):
        put(data, cells, (x, 7, 8), 'birch_planks')
    save(root, data, cells, 'aoe_compact_basement_frame_v5', [9, 8, 9])
    print('Generated revision-five authored flights with only the lower wooden doorway raised')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--minecraft-jar',type=Path,default=Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar')
    parser.add_argument('--revision-four-only', action='store_true')
    parser.add_argument('--revision-five-only', action='store_true')
    args = parser.parse_args()
    if args.revision_five_only:
        generate_revision_five(ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion')
    elif args.revision_four_only:
        generate_revision_four(ROOT / 'src/main/resources/data/minecraft/structures/woodland_mansion')
    else:
        generate(args.minecraft_jar)
