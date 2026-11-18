#!/usr/bin/env python3
"""Version-eleven village: taller wood gateways and 127 authored orc residents."""
import copy
import json
import math
import random
from pathlib import Path
import runpy
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CODEC = runpy.run_path(str(ROOT / 'scripts/generate-orc-camp-overrides.py'))
Tag, read_nbt, write_nbt = (CODEC[n] for n in ('Tag', 'read_nbt', 'write_nbt'))
rotated = runpy.run_path(str(ROOT / 'scripts/generate-orc-settlement.py'))['rotated']
RADIUS, CENTER, HEIGHT = 96, 98, 18
SIZE = 2 * CENTER + 1
HOUSE_SPACING, CENTRAL_EDGE, CROP_EDGE = 18, 38, 51
HOUSE_CENTERS = (-27, -9, 9, 27)
PEN_INNER, PEN_OUTER = 59, 87
DIRECTIONS = [(0, -1, 'north'), (1, 0, 'east'), (0, 1, 'south'), (-1, 0, 'west')]


def inside(x, z):
    return max(abs(x), abs(z)) <= RADIUS


def square_wall():
    lo, hi = -RADIUS, RADIUS - 1
    return ([(x, lo) for x in range(lo, hi)]
            + [(hi, z) for z in range(lo, hi)]
            + [(x, hi) for x in range(hi, lo, -1)]
            + [(lo, z) for z in range(hi, lo, -1)])


def native_stockade():
    path = 'data/orcz/structures/orc_camp_normal_loot.nbt'
    with zipfile.ZipFile(ROOT / 'libs/pack/orcz-0.87-1.20.1.jar') as archive:
        data = read_nbt(archive.read(path))[1].value
    palette, columns = data['palette'].value[1], {}
    for cell in data['blocks'].value[1]:
        x, y, z = cell['pos'].value[1]
        columns.setdefault((x, z), {})[y] = palette[cell['state'].value]['Name'].value
    posts = [(26, 4), (27, 7), (29, 11), (30, 15), (30, 21), (29, 27), (28, 31), (26, 33)]
    profiles = []
    for pos in posts:
        cells = columns[pos]
        top = max(y for y, n in cells.items() if n.endswith('_fence'))
        profiles.append([cells[y] for y in range(3, top+1)])
    gaps = [abs(a-c)+abs(b-d) for (a, b), (c, d) in zip(posts, posts[1:])]
    bushes = [sum(n == 'minecraft:oak_leaves' for n in c.values())
              for c in columns.values() if 'minecraft:oak_leaves' in c.values()]
    return profiles, gaps, bushes


def main():
    palette, keys, blocks = [], {}, {}
    reserved, pen_lawns, ponds = set(), set(), set()

    def put(x, y, z, name=None, properties=None, state=None, nbt=None):
        assert -CENTER <= x <= CENTER and -CENTER <= z <= CENTER and 0 <= y < HEIGHT, (x, y, z)
        if state is None:
            state = {'Name': Tag(8, name)}
            if properties:
                state['Properties'] = Tag(10, {k: Tag(8, str(v)) for k, v in properties.items()})
        if state['Name'].value == 'minecraft:air':
            blocks.pop((x, y, z), None)
            return
        key = (state['Name'].value, tuple(sorted((k, v.value) for k, v in state.get('Properties', Tag(10, {})).value.items())))
        if key not in keys:
            keys[key] = len(palette)
            palette.append(copy.deepcopy(state))
        cell = {'pos': Tag(9, (3, [x+CENTER, y, z+CENTER])), 'state': Tag(3, keys[key])}
        if nbt is not None: cell['nbt'] = Tag(10, nbt)
        blocks[x, y, z] = cell

    def name(pos):
        return palette[blocks[pos]['state'].value]['Name'].value if pos in blocks else 'minecraft:air'

    # Even-width paths/tents share the half-block center at (-.5,-.5).
    def half_radius(value): return value if value >= 0 else -value-1
    def reflect(value, sign): return value if sign > 0 else -value-1
    def lane(value): return -3 <= value <= 2
    for x in range(-RADIUS-1, RADIUS+2):
        for z in range(-RADIUS-1, RADIUS+2):
            radial = max(half_radius(x), half_radius(z))
            road = radial <= CENTRAL_EDGE or lane(x) or lane(z) or 53 <= radial <= 58 or 88 <= radial <= 93
            for y in range(3):
                put(x, y, z, 'minecraft:rooted_dirt' if y < 2 else 'minecraft:dirt_path' if road else 'minecraft:grass_block')

    source = read_nbt((ROOT / 'scripts/data/orc-tent-12-authored.nbt').read_bytes())[1].value
    native = source['palette'].value[1]
    chests=[]
    removed_crops={'minecraft:wheat','minecraft:beetroots','minecraft:carrots','minecraft:potatoes','farmersdelight:cabbages'}
    for cx in HOUSE_CENTERS:
        for cz in HOUSE_CENTERS:
            for cell in source['blocks'].value[1]:
                x,y,z=cell['pos'].value[1]
                state=copy.deepcopy(native[cell['state'].value]); block=state['Name'].value
                # The authored north fascia accidentally has one extra eight-block row.
                if y == 5 and z == 0 and 2 <= x <= 9 and block == 'minecraft:red_wool':
                    state={'Name':Tag(8,'minecraft:air')}
                elif block in removed_crops or block in ('minecraft:grass','minecraft:tall_grass'):
                    state={'Name':Tag(8,'minecraft:air')}
                elif y == 0 and block in ('minecraft:grass_block','minecraft:farmland'):
                    state={'Name':Tag(8,'minecraft:dirt_path')}
                px,pz=cx+x-6,cz+z-6
                put(px,y+2,pz,state=state)
                if block=='minecraft:chest':chests.append((px,y+2,pz))
    assert len(chests)==64
    for index,pos in enumerate(sorted(chests)):
        put(*pos,state=palette[blocks[pos]['state'].value],nbt={'id':Tag(8,'minecraft:chest'),
            'LootTable':Tag(8,'aoemod:chests/orc_camp'),'ForgeData':Tag(10,{'aoemod_village_chest':Tag(3,index),
                'aoemod_village_chest_count':Tag(3,64)})})

    # All rows turn at the same diagonal, making continuous L-shaped corners.
    crops=['minecraft:wheat','pumpkin_stem_pairs','minecraft:wheat','minecraft:beetroots','minecraft:wheat',
        'farmersdelight:cabbages','minecraft:wheat','minecraft:potatoes','minecraft:wheat','minecraft:carrots',
        'minecraft:wheat','hearthandharvest:corn_stalk']
    # 4*12+3*6=66 tent blocks. Six clear path cells and one fence cell leave
    # the same seven 12x12 crop tiles per quadrant as version eight, no podzol.
    tiles=[(a,b) for a in (4,16,28,40) for b in (4,16,28,40) if max(a,b)==40]
    patch_audit=[]
    crop_fences=[]
    for sx in (-1,1):
        for sz in (-1,1):
            for a in range(4,52):
                for b in range(4,52):
                    if max(a,b)<40:continue
                    x,z=reflect(a,sx),reflect(b,sz);reserved.add((x,z))
                    row=max(a,b)-40
                    put(x,2,z,'minecraft:farmland',{'moisture':7})
                    if row==1:
                        # P,stem->P,<-stem,P repeats away from each L corner.
                        phase=(41-min(a,b))%4
                        if phase in (0,3):
                            put(x,2,z,'minecraft:dirt');put(x,3,z,'minecraft:pumpkin')
                        else:
                            toward=1 if phase==1 else -1
                            direction=('south' if toward*sz>0 else 'north') if a>=b else ('east' if toward*sx>0 else 'west')
                            put(x,3,z,'minecraft:attached_pumpkin_stem',{'facing':direction})
                    elif row==11:
                        for dy,section in enumerate(('bottom','middle','top'),3):
                            put(x,dy,z,crops[row],{'age':5,'section':section,'crow_proof':'false','passable':'false',
                                'trim_east':'false','trim_north':'false','trim_south':'false','trim_west':'false'})
                    else:put(x,3,z,crops[row],{'age':3 if row==3 else 7})
            # Close the L-shaped plot on every edge; each pond aligns with
            # four-wide gates in both parallel boundary fences.
            for a in range(3,53):
                for b in range(3,53):
                    outer=max(a,b)==52 or min(a,b)==3 and max(a,b)>=39
                    inner=max(a,b)==39 and min(a,b)>=3
                    if not(outer or inner):continue
                    x,z=reflect(a,sx),reflect(b,sz)
                    gate=(a in (39,52) and 8<=b<=47 and (b-8)%12<4
                        or b in (39,52) and 8<=a<=47 and (a-8)%12<4
                        or a==3 and 44<=b<=47 or b==3 and 44<=a<=47)
                    if gate:
                        facing='east' if a in (3,39,52) else 'north'
                        put(x,3,z,'minecraft:acacia_fence_gate',{'facing':facing,'open':'false','powered':'false','in_wall':'false'})
                    else:put(x,3,z,'minecraft:acacia_fence')
                    crop_fences.append({'pos':[x,z],'gate':gate})
            # Runtime chooses ten positions in one actual world chunk per
            # patch. Sixteen-block offsets guarantee distinct owner chunks.
            for start in (63,79):
                cells={ (reflect(a,sx),reflect(b,sz)) for a in range(start,start+7) for b in range(start,start+7) }
                for x,z in cells:put(x,2,z,'minecraft:grass_block')
                reserved.update(cells)
                patch_audit.append({'label':'forest_corner','cells':sorted(cells),'red':5,'brown':5})
            for a,b in tiles:
                for dx in range(4,8):
                    for dz in range(4,8):
                        x,z=reflect(a+dx,sx),reflect(b+dz,sz);ponds.add((x,z))
                        for y in range(3,7):put(x,y,z,'minecraft:air')
                        put(x,0,z,'minecraft:mud');put(x,1,z,'minecraft:mud')
                        if dx in (4,7) or dz in (4,7):
                            for y in range(2,5):put(x,y,z,'minecraft:sugar_cane',{'age':0,'waterlogged':str(y==2).lower()})
                        else:put(x,2,z,'minecraft:water',{'level':0})

    pen_audit=[]
    for sz in (-1,1):
        for sx in (-1,1):
            for swap in (False,True):
                def point(a,b):return (reflect(b if swap else a,sx),reflect(a if swap else b,sz))
                interior={point(a,b) for a in range(60,87) for b in range(4,31)}
                pen_lawns.update(interior);gates=[]
                for a in range(59,88):
                    for b in range(3,32):
                        if a not in (59,87) and b not in (3,31):continue
                        x,z=point(a,b)
                        gate=(a in (59,87) and 15<=b<=19) or (b in (3,31) and 71<=a<=75)
                        if gate:
                            facing='north' if (a in (59,87))==swap else 'east'
                            put(x,3,z,'minecraft:acacia_fence_gate',{'facing':facing,'open':'false','powered':'false','in_wall':'false'})
                            gates.append((x,z))
                        else:put(x,3,z,'minecraft:acacia_fence')
                for a in range(70,77):
                    for b in range(14,21):
                        x,z=point(a,b);put(x,2,z,'minecraft:water',{'level':0});put(x,1,z,'minecraft:mud')
                        reserved.add((x,z))
                for a0 in (64,81):
                    for b0 in (8,25):
                        for a in (a0,a0+1):
                            for b in (b0,b0+1):
                                x,z=point(a,b);put(x,3,z,'minecraft:hay_block',{'axis':'y'})
                pen_audit.append({'area':729,'interior':sorted(interior),'pond':49,'hay':16,'gates':gates,
                    'spawn':[point(a,b) for a,b in [(68,8),(77,8),(64,17),(82,17),(68,26),(77,26),(68,12),(78,22)]]})

    profiles,_,bush_heights=native_stockade()
    wall=square_wall();posts=[]
    lo,hi=-RADIUS,RADIUS-1
    post_coordinates=set(range(lo,-4,7)) | set(range(4,hi+1,7))
    for x,z in wall:
        across=z if x in (lo,hi) else x
        if across < -5 or across > 4:
            for y in range(3,6):put(x,y,z,'aoemod:acacia_palisade')
    # Corner and regular posts have exactly six panels between them. The wide
    # gate pillars are measured from their outer edge, around a six-wide door.
    for x,z in wall:
        across=z if x in (lo,hi) else x
        if across not in post_coordinates:continue
        post_index=len(posts);profile=profiles[post_index%len(profiles)]
        gateway=across in (-5,4);skull=not gateway and post_index%3==0
        if not gateway:
            for y,block in enumerate(profile,3):
                converted='minecraft:acacia_log' if block=='minecraft:dark_oak_log' else 'aoemod:acacia_palisade' if block=='orcz:palisadedarkoak' else 'minecraft:acacia_fence'
                put(x,y,z,converted,{'axis':'y'} if converted.endswith('_log') else None)
            if skull:
                corner=x in (lo,hi) and z in (lo,hi)
                rotation=round(math.atan2(x+.5,-z-.5)*8/math.pi)%16 if corner else 4 if x==hi else 12 if x==lo else 8 if z==hi else 0
                put(x,3+len(profile),z,'minecraft:skeleton_skull',{'rotation':rotation})
        posts.append({'pos':[x,z],'skull':skull,'gateway':gateway})
    gates=[]
    for direction in range(4):
        def gateway_point(across,depth=0):
            x,z=across,hi-depth
            for _ in range(direction):x,z=-z-1,x
            return x,z
        pillars=[]
        for across in range(-5,5):
            for depth in (0,1):
                x,z=gateway_point(across,depth)
                if across < -3 or across > 2:
                    for y in range(3,10):put(x,y,z,'minecraft:acacia_wood',{'axis':'x' if direction%2==0 else 'z'})
                    pillars.append([x,z])
                else:
                    for y in (8,9):put(x,y,z,'minecraft:acacia_wood',{'axis':'x' if direction%2==0 else 'z'})
        gates.append({'pillars':pillars,'clear_width':6,'clear_height':5,'depth':2,'pillar_width':2,'lintel_height':2})
    bush_audit=[]
    for index,(x,z) in enumerate(wall):
        across=z if x in (lo,hi) else x
        if index%4 or -5<=across<=4:continue
        dx,dz=(-1 if x==lo else 1,0) if x in (lo,hi) else (0,-1 if z==lo else 1)
        for sign in (-1,1):
            bx,bz=x+sign*dx,z+sign*dz
            if (bx,bz) in wall:continue
            if name((bx,2,bz))!='minecraft:grass_block':continue
            height=bush_heights[index%len(bush_heights)]
            for y in range(3,3+height):put(bx,y,bz,'minecraft:acacia_leaves',{'distance':1,'persistent':'true','waterlogged':'false'})
            bush_audit.append([bx,bz])
    # Native trees use the corner lawn around the two protected mushroom patches.
    for pos in list(blocks):
        block=name(pos)
        if block not in ('aoemod:acacia_palisade','minecraft:acacia_fence'):continue
        x,y,z=pos;props={'waterlogged':'false'};connected=[]
        for dx,dz,direction in DIRECTIONS:
            yes=name((x+dx,y,z+dz)) in ('aoemod:acacia_palisade','minecraft:acacia_fence','minecraft:acacia_log','minecraft:acacia_wood','minecraft:acacia_fence_gate')
            connected.append(yes)
            props[direction]=('tall' if name((x,y+1,z))==block else 'low') if yes and block.startswith('aoemod:') else 'none' if block.startswith('aoemod:') else str(yes).lower()
        if block.startswith('aoemod:'):props['up']=str(connected not in ([True,False,True,False],[False,True,False,True])).lower()
        put(x,y,z,block,props)
    tree_reserved=set(reserved)|pen_lawns
    resident_audit=[]
    for house,(cx,cz) in enumerate(( (x,z) for x in HOUSE_CENTERS for z in HOUSE_CENTERS )):
        for i in range(3 if house==5 else 4):
            resident_audit.append({'index':len(resident_audit),'type':'orcz:bigorcboss' if house==5 and i==0
                else 'orcz:orcshield' if i%2==0 else 'orcz:orcsavagemace',
                'pos':[cx+(-2 if i%2==0 else 1),3,cz+(-4 if i<2 else 3)],'forest':False})
    for sx in (-1,1):
        for sz in (-1,1):
            for i,(a,b) in enumerate(( (a,b) for a in (36,48,60,72) for b in (60,68,76,84) )):
                x,z=reflect(a,sx),reflect(b,sz)
                resident_audit.append({'index':len(resident_audit),'type':'orcz:orcshield' if i%2==0 else 'orcz:orcsavagemace',
                    'pos':[x,3,z],'forest':True})
                # Keep native trunks and canopies clear of the authored bodies.
                tree_reserved.update((x+dx,z+dz) for dx in range(-1,2) for dz in range(-1,2))
    for pos in blocks:
        if name(pos) not in ('minecraft:air','minecraft:grass_block','minecraft:rooted_dirt','minecraft:dirt','minecraft:mud'):
            x,_,z=pos;reserved.add((x,z));tree_reserved.add((x,z))
    def procedural_ground(pos):
        x,y,z=pos
        if max(abs(x),abs(z))>RADIUS+1 or y not in (0,1,2):return False
        state=palette[blocks[pos]['state'].value]
        properties={k:v.value for k,v in state.get('Properties',Tag(10,{})).value.items()}
        if y<2:return state['Name'].value=='minecraft:rooted_dirt' and not properties
        return state['Name'].value=='minecraft:grass_block' and properties in ({},{'snowy':'false'}) and not (lane(x) or lane(z))
    template_blocks=[blocks[p] for p in sorted(blocks,key=lambda p:(p[1],p[2],p[0])) if not procedural_ground(p)]
    data={'DataVersion':Tag(3,3465),'size':Tag(9,(3,[SIZE,HEIGHT,SIZE])),'palette':Tag(9,(10,palette)),
        'blocks':Tag(9,(10,template_blocks)),'entities':Tag(9,(10,[]))}
    (ROOT/'src/main/resources/data/aoemod/structures/orc_village.nbt').write_bytes(write_nbt(('',Tag(10,data))))
    mask={'size':SIZE,'reserved_columns':sorted((z+CENTER)*SIZE+x+CENTER for x,z in reserved),
        'tree_reserved_columns':sorted((z+CENTER)*SIZE+x+CENTER for x,z in tree_reserved),
        'mud_pond_columns':sorted((z+CENTER)*SIZE+x+CENTER for x,z in ponds)}
    (ROOT/'src/main/resources/data/aoemod/structure_data/orc_village_vegetation.json').write_text(json.dumps(mask,indent=2)+'\n')
    audit={'version':11,'shape':'square','radius':RADIUS,'size':[SIZE,HEIGHT,SIZE],'tents':16,'house_size':12,
        'house_spacing':HOUSE_SPACING,'clear_house_gap':6,'tent_farmland_gap':6,'central_edge':CENTRAL_EDGE,'crop_edge':CROP_EDGE,'road_width':6,
        'post_gap':6,'crop_tiles':28,'crop_ponds':len(ponds)//16,'pond_columns':sorted(ponds),'crops':crops,
        'pens':pen_audit,'posts':posts,'gates':gates,'crop_fences':crop_fences,'patches':patch_audit,'bushes':bush_audit,
        'chests':64,'beds':128,'cushions':128,'animals':64,'residents':resident_audit,'blocks':len(template_blocks)}
    (ROOT/'scripts/data/orc-village-audit.json').write_text(json.dumps(audit,indent=2)+'\n')
    print(json.dumps({k:v for k,v in audit.items() if k not in ('pens','posts','pond_columns','bushes','patches','crop_fences','residents')},indent=2))


if __name__ == '__main__':
    main()
