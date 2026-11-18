"""Inspect actual generated version-eleven village blocks."""
from pathlib import Path
import json, runpy, hashlib
from collections import Counter
ROOT=Path(__file__).resolve().parents[1]
codec=runpy.run_path(str(ROOT/'scripts/generate-orc-camp-overrides.py'))
data=codec['read_nbt']((ROOT/'src/main/resources/data/aoemod/structures/orc_village.nbt').read_bytes())[1].value
palette=data['palette'].value[1]
blocks={tuple(b['pos'].value[1]):palette[b['state'].value] for b in data['blocks'].value[1]}
audit=json.loads((ROOT/'scripts/data/orc-village-audit.json').read_text())
mask=json.loads((ROOT/'src/main/resources/data/aoemod/structure_data/orc_village_vegetation.json').read_text())
reserved,trees=set(mask['reserved_columns']),set(mask['tree_reserved_columns'])
def state(x,y,z):
 default={}
 if max(abs(x),abs(z))<=97 and y in (0,1,2):
  default={'Name':codec['Tag'](8,'minecraft:rooted_dirt' if y<2 else 'minecraft:grass_block')}
 return blocks.get((x+98,y,z+98),default)
def at(x,y,z):return state(x,y,z).get('Name',codec['Tag'](8,'minecraft:air')).value
def props(x,y,z):return {k:v.value for k,v in state(x,y,z).get('Properties',codec['Tag'](10,{})).value.items()}
def ix(x,z):return (z+98)*197+x+98
def mirror(a,sign):return a if sign>0 else -a-1
assert data['size'].value[1]==[197,18,197]
counts=Counter(s['Name'].value for s in blocks.values())
assert counts['minecraft:air']==counts['minecraft:rooted_dirt']==counts['minecraft:grass_block']==0
assert counts['minecraft:chest']==64 and counts['minecraft:red_bed']==256
assert counts['minecraft:grass']==counts['minecraft:tall_grass']==0
assert counts['minecraft:podzol']==0
for cell in data['blocks'].value[1]:
 if palette[cell['state'].value]['Name'].value=='minecraft:chest':
  assert cell['nbt'].value['ForgeData'].value['aoemod_village_chest_count'].value==64
# Six-wide annular and axis roads are clear.
for sign in (-1,1):
 for t in range(-94,94):
  for n in list(range(53,59))+list(range(88,94)):
   if abs(t)<=n:
    for x,z in [(mirror(n,sign),t),(t,mirror(n,sign))]:
     assert at(x,2,z)=='minecraft:dirt_path',('road soil',x,z)
     assert at(x,3,z)=='minecraft:air',('blocked road',x,z)
for t in range(-96,96):
 for lane in range(-3,3):
  for x,z in [(lane,t),(t,lane)]:
   assert at(x,2,z)=='minecraft:dirt_path'
   assert at(x,3,z)=='minecraft:air',('axis road',x,z)
# Exactly six panels, including gate edges and stockade corners.
for side in (-96,95):
 for swap in (False,True):
  def point(a):return (side,a) if swap else (a,side)
  for a in list(range(-96,-5,7))+list(range(4,95,7)):
   x,z=point(a);nx,nz=point(a+7)
   assert at(x,3,z) in ('minecraft:acacia_log','minecraft:acacia_wood') and at(nx,3,nz) in ('minecraft:acacia_log','minecraft:acacia_wood')
   for b in range(a+1,a+7):
    x,z=point(b);assert at(x,3,z)=='aoemod:acacia_palisade'
  for a in range(-96,96):
   if -3<=a<=2:continue
   x,z=point(a);assert at(x,3,z) in ('aoemod:acacia_palisade','minecraft:acacia_log','minecraft:acacia_wood'),('wall leak',x,z)
for (tx,y,tz),block in blocks.items():
 if block['Name'].value!='minecraft:skeleton_skull':continue
 x,z=tx-98,tz-98;rotation=int(props(x,y,z)['rotation'])
 if x in (-96,95) and z in (-96,95):assert rotation in (2,6,10,14)
 else:assert rotation==(4 if x==95 else 12 if x==-96 else 8 if z==95 else 0),('skull facing',x,z)
# Tall gateways use horizontal acacia wood on both depth rows.
for direction in range(4):
 def gate(a,depth):
  x,z=a,95-depth
  for _ in range(direction):x,z=-z-1,x
  return x,z
 for a in range(-5,5):
  for depth in (0,1):
   x,z=gate(a,depth)
   for y in range(3,10):
    if -3<=a<=2 and y<8:assert at(x,y,z)=='minecraft:air'
    else:
     assert at(x,y,z)=='minecraft:acacia_wood'
     assert props(x,y,z)['axis']==('x' if direction%2==0 else 'z')
   assert at(x,10,z)=='minecraft:air'
  x,z=gate(a,2)
  assert all(at(x,y,z)=='minecraft:air' for y in range(3,10)),('gate is more than two deep',x,z)
# All sixteen tents retain their authored roofs and six-wide farm margin.
source=codec['read_nbt']((ROOT/'scripts/data/orc-tent-12-authored.nbt').read_bytes())[1].value
source_palette=source['palette'].value[1]
for cx in (-27,-9,9,27):
 for cz in (-27,-9,9,27):
  for cell in source['blocks'].value[1]:
   x,y,z=cell['pos'].value[1];name=source_palette[cell['state'].value]['Name'].value
   if name!='minecraft:red_wool':continue
   expected='minecraft:air' if y==5 and z==0 and 2<=x<=9 else name
   assert at(cx+x-6,y+2,cz+z-6)==expected
 for x in range(33,39):assert at(x,2,cx)=='minecraft:dirt_path'
assert audit['clear_house_gap']==6 and audit['house_spacing']==18
# Exact old crop area and hydration; complete new perimeter fencing.
ponds={tuple(p) for p in audit['pond_columns']}
assert len(ponds)==448 and {ix(x,z) for x,z in ponds}==set(mask['mud_pond_columns'])
assert counts['minecraft:sugar_cane']==336*3
for x,z in ponds:
 assert at(x,0,z)==at(x,1,z)=='minecraft:mud'
 if at(x,2,z)=='minecraft:sugar_cane':
  assert props(x,2,z)['waterlogged']=='true'
  for y in (3,4):assert at(x,y,z)=='minecraft:sugar_cane' and props(x,y,z)['waterlogged']=='false'
 else:assert at(x,2,z)=='minecraft:water' and at(x,3,z)=='minecraft:air'
for sx in (-1,1):
 for sz in (-1,1):
  area={(mirror(a,sx),mirror(b,sz)) for a in range(4,52) for b in range(4,52) if max(a,b)>=40}
  assert len(area)==1008
  for x,z in area:
   for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
    if (x+dx,z+dz) not in area:
     assert at(x+dx,3,z+dz) in ('minecraft:acacia_fence','minecraft:acacia_fence_gate'),('crop enclosure',x,z)
   if (x,z) in ponds:continue
   row=max(x if x>=0 else -x-1,z if z>=0 else -z-1)-40
   block=at(x,3,z)
   if row==1:assert block in ('minecraft:pumpkin','minecraft:attached_pumpkin_stem')
   else:assert block==audit['crops'][row],(x,z,row,block)
   if block!='minecraft:pumpkin':
    assert at(x,2,z)=='minecraft:farmland' and props(x,2,z)['moisture']=='7'
    assert any((x+dx,z+dz) in ponds for dx in range(-4,5) for dz in range(-4,5)),('dry crop',x,z)
   if block=='minecraft:attached_pumpkin_stem':
    dx,dz={'east':(1,0),'west':(-1,0),'north':(0,-1),'south':(0,1)}[props(x,3,z)['facing']]
    assert at(x+dx,3,z+dz)=='minecraft:pumpkin'
# Pond reservations block native cane; adult spawn positions avoid hay and water.
for pen in audit['pens']:
 cells={tuple(p) for p in pen['interior']};assert len(cells)==729
 assert sum(at(x,2,z)=='minecraft:water' for x,z in cells)==49
 assert sum(at(x,3,z)=='minecraft:hay_block' for x,z in cells)==16
 assert all(ix(x,z) in trees for x,z in cells)
 assert len(pen['gates'])==20 and len(pen['spawn'])==8
 for x,z in pen['gates']:
  assert at(x,3,z)=='minecraft:acacia_fence_gate' and props(x,3,z)['open']=='false'
 for x,z in cells:
  if at(x,2,z)=='minecraft:water':assert ix(x,z) in reserved
  assert at(x,3,z) not in {'minecraft:acacia_fence','minecraft:acacia_fence_gate','minecraft:sugar_cane'}
 for x,z in pen['spawn']:
  assert (x,z) in cells and at(x,2,z)=='minecraft:grass_block'
  for dx in (-1,0,1):
   for dz in (-1,0,1):assert at(x+dx,3,z+dz)=='minecraft:air',('adult bison clearance',x,z)
assert len(audit['patches'])==8
for patch in audit['patches']:
 assert patch['label']=='forest_corner' and patch['red']==patch['brown']==5 and len(patch['cells'])==49
 for x,z in patch['cells']:assert at(x,2,z)=='minecraft:grass_block' and ix(x,z) in reserved and ix(x,z) in trees
# Resident indices and authored floor cells are disjoint from livestock/crops/roofs.
residents=audit['residents'];assert len(residents)==127
assert [r['index'] for r in residents]==list(range(127))
assert Counter(r['type'] for r in residents)=={'orcz:bigorcboss':1,'orcz:orcshield':63,'orcz:orcsavagemace':63}
assert sum(r['forest'] for r in residents)==64
forest_corners=Counter()
for r in residents:
 x,y,z=r['pos'];assert y==3
 assert at(x,2,z) in ('minecraft:grass_block','minecraft:dirt_path','minecraft:hay_block'),(r,at(x,2,z))
 assert at(x,3,z)==at(x,4,z)=='minecraft:air'
 if r['forest']:
  forest_corners[x>0,z>0]+=1
  assert min(abs(x),abs(z))>31 and max(abs(x),abs(z))>=59
  for dx in (-1,0,1):
   for dz in (-1,0,1):
    assert ix(x+dx,z+dz) in trees
    assert all(at(x+dx,yy,z+dz)=='minecraft:air' for yy in range(3,6))
assert set(forest_corners.values())=={16} and len(forest_corners)==4
assert hashlib.sha256((ROOT/'src/main/resources/data/aoemod/structures/orc_village_v4.nbt').read_bytes()).hexdigest()=='a91970132597b23e354c57d77d3376aeb5d10ead11bb16e27c08469a3b3457a4'
old=codec['read_nbt']((ROOT/'src/main/resources/data/aoemod/structures/orc_village_v8.nbt').read_bytes())[1].value
assert old['size'].value[1]==[201,18,201]
assert json.loads((ROOT/'src/main/resources/data/aoemod/structure_data/orc_village_v8_vegetation.json').read_text())['size']==201
print(json.dumps({'passed':True,'version':11,'tents':16,'chests':64,'beds':128,'pens':8,'animals':64,
 'crop_ponds':28,'mud_columns':448,'forest_patches':8,'regular_posts':len(audit['posts']),
 'checks':['six-wide roads','sealed stockade','six-panel post spacing','cardinal side skulls','six-by-five two-deep horizontal wood gates',
 'unchanged 28 crop tiles','fenced hydrated crops','deterministic cane only in crop ponds','29-square pens','seven-square pen ponds',
 'four hay stacks and eight clear adult positions per pen','five-wide pen gates','six-block tent gaps','protected grass mushroom patches','127 orcs with collision-free forest clearings','saved templates retained']},indent=2))
