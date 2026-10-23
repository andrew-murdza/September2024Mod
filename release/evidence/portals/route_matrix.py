"""Drive the isolated real client through explicit requests; never changes release code."""
from pathlib import Path
import argparse,json,time,sys
P=argparse.ArgumentParser(); P.add_argument('--game',default='run/portal-production-client/game'); P.add_argument('phase',choices=['assets','connect','setup','forbidden','routes','survival','mixed','persistence','reload','snapshot']);P.add_argument('--start',type=int,default=0);A=P.parse_args()
GAME=Path(A.game).resolve(); counter=0
DIMS=['overworld','tropical','savanna','plains','snowy','desert','nether']
COLORS=dict(zip(DIMS,['86a9dc','48d05a','e99d36','929292','ffffff','f8dd39','ef4444']))
PLANKS={'overworld':'minecraft:oak_planks','tropical':'minecraft:oak_planks','savanna':'minecraft:acacia_planks','plains':'minecraft:oak_planks','snowy':'minecraft:spruce_planks','desert':'aoemod:pale_oak_planks','nether':'minecraft:crimson_planks'}
LANG=json.loads(Path('src/main/resources/assets/aoemod/lang/en_us.json').read_text())
LABELS={d:LANG['portal.aoemod.destination.'+d] for d in DIMS}
RUN=str(int(time.time())); PICKER_CAPTURES=set()

def dim(d):return 'minecraft:overworld' if d=='overworld' else 'aoemod:'+d+'_dimension'
def y(d):return 100 if d=='nether' else 200
def allowed(s,t):return s!=t and ((t=='desert') if s=='nether' else ((s=='desert') if t=='nether' else True))
def load(p):
 try:return json.loads(p.read_text())
 except (OSError,json.JSONDecodeError):return {}
def request(text,timeout=60):
 (GAME/'qa-request.txt').write_text(text,encoding='utf-8')
 deadline=time.monotonic()+timeout
 while time.monotonic()<deadline:
  if load(GAME/'qa-ack.json').get('request')==text:return
  time.sleep(.1)
 raise TimeoutError('Client did not acknowledge '+text)
def snap(label='poll'):
 global counter
 counter+=1;name=f'{RUN}-{label}-{counter}'
 request('snapshot '+name)
 return load(GAME/('qa-'+name+'.json'))
def wait(predicate,label,timeout=60):
 deadline=time.monotonic()+timeout;last={}
 while time.monotonic()<deadline:
  last=snap(label)
  if label=='joined' and last.get('screen','').endswith('DisconnectedScreen'):
   request('capture join-disconnect');raise RuntimeError('Client disconnected during join; see network diagnostic log')
  if predicate(last):return last
  time.sleep(.35)
 raise TimeoutError(label+' '+json.dumps(last))
def cmd(c):request('command '+c);time.sleep(.2)
def save(name,result):
 (GAME/('qa-'+name+'.json')).write_text(json.dumps(result,indent=2));print(name,json.dumps(result),flush=True)
def relocate(d):
 cmd(f'execute in {dim(d)} run tp @s 1.5 {y(d)} -3.0 0 0')
 return wait(lambda s:s.get('dimension')==dim(d) and abs(s.get('xyz',[999,999,999])[2]+3)<.5,'position-'+d)
def openpicker(d):
 request(f'interact 0 {y(d)+1} 0')
 state=wait(lambda s:s.get('screen','').endswith('PortalSelectionScreen'),'picker-'+d,15)
 expected={LABELS[t] for t in DIMS if allowed(d,t)}|{'Cancel'}
 assert set(state['buttons'])==expected,('Wrong picker routes',d,state['buttons'],expected)
 if d not in PICKER_CAPTURES:
  request('capture picker-'+d);time.sleep(1.2);PICKER_CAPTURES.add(d)
 return state

def center_portals(state,d):
 return sorted([(p['position'],p['id'],p['tint']) for p in state.get('portals',[]) if p['position'] in [f'{x}, {yy}, 0' for x in [1,2] for yy in range(y(d),y(d)+3)]])
def choose(d,t):
 openpicker(d);request('choose '+t)
 return wait(lambda s:len(center_portals(s,d))==6 and all(p[1]=='aoemod:'+t+'_portal' and p[2].lower()==COLORS[t] for p in center_portals(s,d)),'activate-'+d+'-'+t,20)
def setup():
 cmd('gamerule doMobSpawning false');cmd('gamerule doDaylightCycle false');cmd('time set day');cmd('weather clear');cmd('gamemode spectator @s')
 for d in DIMS:
  relocate(d);height=y(d);planks=PLANKS[d]
  cmd(f'fill -5 {height} -5 5 {height+7} 5 minecraft:air')
  cmd(f'fill -5 {height-1} -5 5 {height-1} 5 {planks}')
  cmd(f'fill 0 {height} 0 0 {height+3} 0 {planks}')
  cmd(f'fill 3 {height} 0 3 {height+3} 0 {planks}')
  cmd(f'fill 0 {height+3} 0 3 {height+3} 0 {planks}')
  print('SETUP',d,flush=True)
 cmd('gamemode creative @s');cmd('item replace entity @s weapon.mainhand with minecraft:stick');relocate('overworld')
 save('route-setup',{'dimensions':DIMS,'planks':PLANKS,'frameBottomLeft':[1,200,0],'netherBottomLeft':[1,100,0]})
def forbidden():
 results=[]
 for d in DIMS:
  relocate(d)
  for t in DIMS:
   if allowed(d,t):continue
   before=center_portals(snap('forbidden-before'),d);picker=openpicker(d)
   request('forge '+t);time.sleep(.7)
   after=snap('forbidden-after')
   assert after.get('dimension')==dim(d) and center_portals(after,d)==before,('Forbidden route modified portal',d,t,after)
   results.append({'source':d,'forbiddenDestination':t,'pickerButtons':picker['buttons'],'unchanged':True})
   save('forbidden-routes',results)
 save('forbidden-summary',{'expected':17,'passed':len(results),'allPassed':len(results)==17})
def routes():
 routes=[(s,t) for s in DIMS for t in DIMS if allowed(s,t)]
 assert len(routes)==32
 results=load(GAME/'qa-route-matrix.json') if A.start else []
 if not isinstance(results,list):results=[]
 for index,(source,target) in enumerate(routes):
  if index<A.start:continue
  relocate(source);activated=choose(source,target)
  request(f'capture route-{index:02}-{source}-{target}')
  time.sleep(2.2)
  if index==0:request('capture animation-second-frame')
  time.sleep(3.4)
  start=time.monotonic();cmd(f'tp @s 1.5 {y(source)} 0.5 0 0')
  arrived=wait(lambda s:s.get('dimension')==dim(target) and len([p for p in s.get('portals',[]) if p['id']=='aoemod:'+source+'_portal'])>=6,'arrival-'+source+'-'+target,120)
  returns=[p for p in arrived.get('portals',[]) if p['id']=='aoemod:'+source+'_portal']
  assert len(returns)>=6,('Missing return portal',source,target,arrived)
  assert all(p['tint'].lower()==COLORS[source] for p in returns),('Wrong return tint',source,target,returns)
  result={'index':index,'source':source,'destination':target,'actualDimension':arrived['dimension'],'seconds':round(time.monotonic()-start,3),'arrivalXYZ':arrived['xyz'],'sourcePortal':center_portals(activated,source),'returnPortal':returns,'passed':True}
  results.append(result);save('route-matrix',results)
  relocate(target)
 save('route-matrix-summary',{'expected':32,'passed':len(results),'allPassed':len(results)==32})
def survival():
 relocate('desert');choose('desert','nether');time.sleep(5.6)
 cmd('gamemode survival @s');wait(lambda s:s.get('creative') is False,'survival-abilities')
 request('travel 1.5 200 0.5 survival-desert-nether')
 result=wait(lambda s:s.get('dimension')==dim('nether'),'survival-arrival',120)
 # Staying in arrival portal beyond the original five-second cooldown must not bounce back.
 time.sleep(6);staying=snap('survival-still-arrived');assert staying['dimension']==dim('nether')
 route=load(GAME/'qa-travel-survival-desert-nether.json');assert route.get('changedDimension')
 changed=[s for s in route['samples'] if s['dimension']==dim('nether')][0]
 assert changed['clientTicks']>=75,('Survival charge too short',route)
 arrival=result['xyz'];cmd(f'tp @s {arrival[0]} {arrival[1]} {arrival[2]-1}')
 time.sleep(5.6);request(f'travel {arrival[0]} {arrival[1]} {arrival[2]} survival-return')
 returned=wait(lambda s:s.get('dimension')==dim('desert'),'survival-return',120)
 save('survival-portal-result',{'outbound':route,'stayedInArrivalWithoutBounce':True,'netherArrivalXYZ':arrival,'returnedDimension':returned['dimension'],'returnedXYZ':returned['xyz']})
 cmd('gamemode creative @s');relocate('desert')

def mixed():
 relocate('overworld')
 cmd('fill 7 200 -5 13 207 5 minecraft:air')
 cmd('fill 7 199 -5 13 199 5 minecraft:birch_planks')
 cmd('fill 10 200 0 10 203 0 minecraft:crimson_planks')
 cmd('fill 10 200 3 10 203 3 minecraft:oak_planks')
 cmd('fill 10 203 0 10 203 3 minecraft:warped_planks')
 cmd('tp @s 10.5 200 -3.0 0 0')
 wait(lambda s:abs(s.get('xyz',[0])[0]-10.5)<.1,'mixed-camera')
 request('interact 10 201 0')
 picker=wait(lambda s:s.get('screen','').endswith('PortalSelectionScreen'),'mixed-picker')
 assert set(picker['buttons'])=={LABELS[t] for t in DIMS if allowed('overworld',t)}|{'Cancel'}
 request('capture mixed-rotated-picker');time.sleep(1.2);request('choose tropical')
 expected={f'10, {yy}, {zz}' for yy in range(200,203) for zz in [1,2]}
 state=wait(lambda s:len([p for p in s.get('portals',[]) if p['position'] in expected and p['id']=='aoemod:tropical_portal' and 'axis=z' in p['state']])==6,'mixed-activated')
 request('capture mixed-rotated-portal');time.sleep(5.6)
 cmd('tp @s 10.5 200 1.5')
 arrived=wait(lambda s:s.get('dimension')==dim('tropical'),'mixed-arrival',120)
 save('mixed-rotated-frame',{'planks':['birch','crimson','oak','warped'],'axis':'z','picker':picker['buttons'],'portalStates':[p for p in state['portals'] if p['position'] in expected],'actualDestination':arrived['dimension'],'passed':True})
 relocate('tropical')

def persistence():
 previous=load(GAME/'qa-survival-portal-result.json')
 assert previous.get('stayedInArrivalWithoutBounce'),previous
 relocate('desert');time.sleep(5.6)
 cmd('tp @s 1.5 200 0.5')
 arrived=wait(lambda s:s.get('dimension')==dim('nether'),'persisted-outbound',120)
 assert all(abs(a-b)<.01 for a,b in zip(arrived['xyz'],previous['netherArrivalXYZ'])),('Saved link moved after restart',arrived['xyz'],previous)
 xyz=arrived['xyz'];cmd(f'tp @s {xyz[0]} {xyz[1]} {xyz[2]-1}');time.sleep(5.6)
 cmd(f'tp @s {xyz[0]} {xyz[1]} {xyz[2]}')
 returned=wait(lambda s:s.get('dimension')==dim('desert'),'persisted-return',120)
 assert all(abs(a-b)<.01 for a,b in zip(returned['xyz'],previous['returnedXYZ'])),('Saved reverse link moved after restart',returned['xyz'],previous)
 save('restart-persistence',{'sameNetherArrival':arrived['xyz'],'sameDesertReturn':returned['xyz'],'passed':True})
 relocate('desert')

def assets(prefix='production'):
 actions=['texture','animations','eyes','brewing','recipebooks','sounds']
 for action in actions:request(action+' '+prefix+'-'+action)
 data={action:load(GAME/('qa-'+prefix+'-'+action+'.json')) for action in actions}
 assert data['texture'].get('pixelMismatches')==0 and data['texture'].get('identicalAnimationMetadata'),data['texture']
 assert len(data['animations'])==4 and all(v.get('resourcePresent') and v.get('repairedAnimationPresent') for v in data['animations'].values()),data['animations']
 assert len(data['eyes'])==3 and all(len(v)==3 and all(v.values()) for v in data['eyes'].values()),data['eyes']
 assert len(data['brewing'].get('recipes',[]))==3 and all(v.get('passed') for v in data['brewing']['recipes']),data['brewing']
 assert data['recipebooks'].get('typeCount')==7 and data['recipebooks'].get('settingsBytes')==14,data['recipebooks']
 assert len(data['sounds'])==7 and all(v.get('eventPresent') and v.get('sampleExists') and v.get('weight',0)>0 for v in data['sounds'].values()),data['sounds']
 diagnostics=load(GAME/'qa-model-diagnostics.json')
 assert diagnostics.get('missingBlockModels')==[] and diagnostics.get('missingMenuScreens')==[] and diagnostics.get('menuScreenCheckFailed') is False and diagnostics.get('entityRendererCheckFailed') is False,diagnostics
 request('capture '+prefix+'-capture')
 save(prefix+'-asset-summary',{'allPassed':True,'checks':data,'modelDiagnostics':diagnostics})

def reload_resources():
 previous=(GAME/'qa-model-diagnostics.json').stat().st_mtime_ns
 request('reload final-resource-reload')
 deadline=time.monotonic()+120
 while (GAME/'qa-model-diagnostics.json').stat().st_mtime_ns==previous and time.monotonic()<deadline:time.sleep(.5)
 assert (GAME/'qa-model-diagnostics.json').stat().st_mtime_ns!=previous,'Resource reload did not rebake models'
 time.sleep(2)
 assets('post-reload')
 state=snap('post-resource-reload')
 assert 'dimension' in state and state['screen']=='none',state
 save('resource-reload',{'connected':True,'dimension':state['dimension'],'resourcePacks':state['resourcePacks'],'allPassed':True})

if A.phase=='assets':assets()
elif A.phase=='connect':
 request('connect 127.0.0.1:25579');print(wait(lambda s:'dimension' in s,'joined',120),flush=True)
elif A.phase=='setup':setup()
elif A.phase=='forbidden':forbidden()
elif A.phase=='routes':routes()
elif A.phase=='survival':survival()
elif A.phase=='mixed':mixed()
elif A.phase=='persistence':persistence()
elif A.phase=='reload':reload_resources()
else:print(json.dumps(snap('explicit'),indent=2),flush=True)
