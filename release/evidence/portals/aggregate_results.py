"""Aggregate observed installed-client QA without inferring unrun checks."""
from pathlib import Path
import argparse,hashlib,json,datetime
p=argparse.ArgumentParser();p.add_argument('sha256');args=p.parse_args()
root=Path(__file__).resolve().parents[2];profile=root/'run/portal-production-client';game=profile/'game'
def read(name):return json.loads((game/('qa-'+name+'.json')).read_text())
jar=root/'build/libs/aoemod-1.0.0-1.20.1-all.jar';actual=hashlib.sha256(jar.read_bytes()).hexdigest();assert actual==args.sha256
matrix=read('route-matrix');assert len(matrix)==32 and len({(r['source'],r['destination']) for r in matrix})==32 and all(r['passed'] for r in matrix)
forbidden=read('forbidden-routes');assert len(forbidden)==17 and all(r['unchanged'] for r in forbidden)
assets=read('production-asset-summary');post=read('post-reload-asset-summary');mixed=read('mixed-rotated-frame');survival=read('survival-portal-result');persist=read('restart-persistence');reload=read('resource-reload')
assert assets['allPassed'] and post['allPassed'] and mixed['passed'] and survival['stayedInArrivalWithoutBounce'] and persist['passed'] and reload['allPassed']
colors={'tropical':'route-00-overworld-tropical','savanna':'route-01-overworld-savanna','plains':'route-02-overworld-plains','snowy':'route-03-overworld-snowy','desert':'route-04-overworld-desert','overworld':'route-05-tropical-overworld','nether':'route-30-desert-nether'}
def image(name):
 f=game/'screenshots'/('aoe-portal-qa-'+name+'.png');assert f.is_file(),f;return str(f.relative_to(root))
report={'verifiedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'artifact':{'path':str(jar.relative_to(root)),'sha256':actual,'sizeBytes':jar.stat().st_size},'runtime':{'minecraft':'1.20.1','forge':'47.4.22','java':'17.0.9','client':'Isolated stock Forge installed production client','server':'Isolated dedicated stock Forge server at 127.0.0.1:25579','player':'AOEPortalQA','dependencyJars':69,'qaHelper':'One local instrumentation helper, excluded from release'},'tests':{'allowedDirectedRoutes':{'expected':32,'passed':len(matrix),'evidence':'run/portal-production-client/game/qa-route-matrix.json'},'forbiddenSelections':{'expected':17,'passed':len(forbidden),'evidence':'run/portal-production-client/game/qa-forbidden-routes.json'},'allSevenPickers':True,'startupAssets':assets,'mixedBurnableNonburnableRotatedFrame':mixed,'survivalChargeAndArrivalCooldown':survival,'clientResourceReload':reload,'postReloadAssets':post,'restartSavedLinkRoundtrip':persist},'screenshots':{'colors':{k:image(v) for k,v in colors.items()},'pickers':{d:image('picker-'+d) for d in colors},'animationPair':[image('route-00-overworld-tropical'),image('animation-second-frame')],'mixedRotated':image('mixed-rotated-portal'),'postReload':image('post-reload-capture')},'method':'Explicit operator commands position the player and prepare fixtures; crouch-stick interactions use the real game client, selections use the actual picker/network packets, and each route requires actual dimension travel and return portal state/tint. Forbidden choices use the genuine issued picker token with a deliberately forbidden destination.','limits':['One local offline test player; external latency and concurrent-player stress were not exercised.','All 32 directed routes were traversed in creative mode; survival charge and return cooldown received an additional dedicated Desert/Nether roundtrip.','This is focused portal and repaired-asset compatibility QA, not exhaustive gameplay coverage for every dependency.']}
report['tests']['cleanClientShutdown']=read('client-shutdown')
assert report['tests']['cleanClientShutdown']['passed'] and report['tests']['cleanClientShutdown']['exitCode']==0
report['tests']['visibleAnimation']=read('visible-animation')
assert report['tests']['visibleAnimation']['animationVisible'] and report['tests']['visibleAnimation']['sha256']==actual
report['runMetadata']=json.loads((profile/'final-run-metadata.json').read_text())
assert report['runMetadata']['sha256']==actual
report['nativeServerValidationReport']='build/reports/native-server-validation.json'
report['screenshots']['afterRestart']=image('final-after-restart')
report['allPassed']=True
out=profile/'final-qa-report.json';out.write_text(json.dumps(report,indent=2));print(out)
