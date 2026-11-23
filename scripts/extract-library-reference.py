"""Read an authored library from a preserved world copy; never write to the world."""
from pathlib import Path
import argparse,copy,gzip,hashlib,io,json,runpy,struct,zlib

ROOT=Path(__file__).resolve().parents[1]
codec=runpy.run_path(str(ROOT/'scripts/generate-orc-camp-overrides.py'))
Tag,read_nbt,write_nbt=(codec[k] for k in ('Tag','read_nbt','write_nbt'))

def raw(tag):
    if tag.kind==10:return {k:raw(v) for k,v in tag.value.items()}
    if tag.kind==9:return [raw(Tag(tag.value[0],v)) for v in tag.value[1]]
    return tag.value

def chunks(path,selected):
    data=path.read_bytes();rx,rz=map(int,path.stem.split('.')[1:])
    for idx in range(1024):
        if (rx*32+idx%32,rz*32+idx//32) not in selected:continue
        offset=int.from_bytes(data[idx*4:idx*4+3],'big')*4096
        if not offset:raise RuntimeError('Reference chunk is absent')
        length=int.from_bytes(data[offset:offset+4],'big');kind=data[offset+4]
        compressed=data[offset+5:offset+4+length]
        payload=gzip.decompress(compressed) if kind==1 else zlib.decompress(compressed) if kind==2 else compressed
        yield read_nbt(gzip.compress(payload))[1].value

def reverse_rotation(state):
    result=copy.deepcopy(state);props=result.get('Properties')
    if props:
        directions={'north':'south','south':'north','east':'west','west':'east'}
        vals={directions.get(k,k):v for k,v in props.value.items()}
        if 'facing' in vals:vals['facing']=Tag(8,directions.get(vals['facing'].value,vals['facing'].value))
        if 'rotation' in vals:vals['rotation']=Tag(8,str((int(vals['rotation'].value)+8)%16))
        result['Properties']=Tag(10,vals)
    return result

def template(size,cells):
    palette=[];blocks=[]
    for p,st in sorted(cells.items()):
        if st not in palette:palette.append(st)
        blocks.append({'pos':Tag(9,(3,list(p))),'state':Tag(3,palette.index(st))})
    return ('',Tag(10,{'DataVersion':Tag(3,3465),'size':Tag(9,(3,list(size))),'palette':Tag(9,(10,palette)),'blocks':Tag(9,(10,blocks)),'entities':Tag(9,(10,[]))}))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('world',type=Path);args=parser.parse_args()
    world=args.world.resolve();refs=ROOT/'references';refs.mkdir(exist_ok=True)
    region=world/'dimensions/aoemod/plains_dimension/region/r.2.-2.mca'
    origin=(1242,129,-526);size=(7,8,15)
    positions={(origin[0]-x,origin[1]+y,origin[2]-z):(x,y,z) for x in range(-1,8) for y in range(8) for z in range(-1,16)}
    selected={(p[0]//16,p[2]//16) for p in positions};cells={}
    for ch in chunks(region,selected):
        cx=ch['xPos'].value;cz=ch['zPos'].value
        for sec in ch['sections'].value[1]:
            sy=sec['Y'].value
            if sy!=8:continue
            bs=sec['block_states'].value;pal=bs['palette'].value[1];values=bs.get('data')
            bits=max(4,(len(pal)-1).bit_length());per=64//bits;mask=(1<<bits)-1
            for i in range(4096):
                p=(cx*16+i%16,sy*16+i//256,cz*16+(i//16)%16)
                if p not in positions:continue
                n=((values.value[i//per]&((1<<64)-1))>>((i%per)*bits))&mask if values else 0
                cells[positions[p]]=reverse_rotation(pal[n])
    if len(cells)!=len(positions):raise RuntimeError('Incomplete reference region')
    target=ROOT/'scripts/data/illager-library-nov22-authored.nbt';target.parent.mkdir(exist_ok=True)
    interior={p:st for p,st in cells.items() if all(0<=p[i]<size[i] for i in range(3))}
    target.write_bytes(write_nbt(template(size,interior)))
    frame_target=target.with_name('illager-library-nov22-boundary.nbt')
    frame={(p[0]+1,p[1],p[2]+1):st for p,st in cells.items() if p not in interior}
    frame_target.write_bytes(write_nbt(template((9,8,17),frame)))
    boundaries={}
    for side in ['west','east','north','south']:
        openings=[]
        for t in range(15 if side in ('west','east') else 7):
            x,z=(-1,t) if side=='west' else (7,t) if side=='east' else (t,-1) if side=='north' else (t,15)
            names=[cells[x,y,z]['Name'].value for y in (1,2,3)]
            if all(n in ['minecraft:air','minecraft:cave_air'] for n in names):openings.append(t)
        boundaries[side]=openings
    manifest={'sourceWorldName':world.name,'sourceDimension':'aoemod:plains_dimension','sourceOrigin':origin,'sourceRotation':'CLOCKWISE_180','sourceMirror':'NONE','sourceTemplate':'aoe_v8_1x2_c4','size':size,'sourceRegionSHA256':hashlib.sha256(region.read_bytes()).hexdigest(),'referenceSHA256':hashlib.sha256(target.read_bytes()).hexdigest(),'boundarySHA256':hashlib.sha256(frame_target.read_bytes()).hexdigest(),'doorwayOpeningsInSurroundingWalls':boundaries}
    (refs/'library-authored-provenance.json').write_text(json.dumps(manifest,indent=2))
    (target.parent/'illager-library-nov22-provenance.json').write_text(json.dumps(manifest,indent=2))
    (refs/'library-authored-cells.json').write_text(json.dumps([{'pos':p,'state':raw(Tag(10,st))} for p,st in cells.items()]))
    symbols={'minecraft:air':' ','minecraft:oak_planks':'O','minecraft:bookshelf':'B','minecraft:oak_stairs':'S','minecraft:oak_fence':'f','minecraft:torch':'t','minecraft:white_carpet':'w','minecraft:red_carpet':'r','minecraft:oak_log':'L','minecraft:glass_pane':'|','minecraft:potted_oxeye_daisy':'d','minecraft:potted_poppy':'p','minecraft:potted_dandelion':'y','minecraft:oak_slab':'s'}
    print(json.dumps(manifest))
    for y in range(8):
        print('Y',y)
        for z in range(-1,16):print(str(z).rjust(2),''.join(symbols.get(cells[x,y,z]['Name'].value,'?') for x in range(-1,8)))

if __name__=='__main__':main()
