"""Audit source JSON and required tag/recipe references against a real Forge registry dump."""
from pathlib import Path
import argparse, json

parser=argparse.ArgumentParser()
parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
parser.add_argument('--registry',type=Path)
parser.add_argument('--report',type=Path)
args=parser.parse_args()
errors=[];warnings=[];checks=0;files=0
registry=json.loads(args.registry.read_text(encoding='utf-8')) if args.registry else {}
def check(ok,message):
 global checks
 checks+=1
 if not ok:errors.append(message)
def parse(path):
 try:return json.loads(path.read_text(encoding='utf-8-sig'))
 except Exception as e: errors.append(f'{path}: {e}');return None
for base in ('src/main/resources','src/generated/resources'):
 for path in (args.root/base).rglob('*.json'):
  files+=1;doc=parse(path)
  if doc is None:continue
  checks+=1
  rel=path.relative_to(args.root/base).as_posix()
  parts=rel.split('/')
  if len(parts)<5 or parts[0]!='data':continue
  namespace=parts[1]
  if parts[2]=='tags':
   kind=parts[3] if parts[3]!='worldgen' else '/'.join(parts[3:5])
   suffix=parts[4:] if parts[3]!='worldgen' else parts[5:]
   tagid=namespace+':'+('/'.join(suffix))[:-5]
   check(isinstance(doc.get('values'),list),f'{rel}: tag values must be an array')
   if kind not in registry:continue
   ids=set(registry[kind]['ids']);tags=registry[kind]['tags'];seen=set()
   for value in doc.get('values',[]):
    ident=value if isinstance(value,str) else value.get('id')
    required=True if isinstance(value,str) else value.get('required',True)
    check(isinstance(ident,str),f'{rel}: invalid tag entry {value}')
    if not isinstance(ident,str):continue
    if ident in seen:warnings.append(f'{rel}: repeated entry {ident}')
    seen.add(ident)
    raw=ident[1:] if ident.startswith('#') else ident
    canonical=raw if ':' in raw else 'minecraft:'+raw
    exists=canonical in tags if ident.startswith('#') else canonical in ids
    if required:check(exists,f'{rel}: required {kind} entry does not exist: {ident}')
    elif not exists:warnings.append(f'{rel}: absent optional entry {ident}')
   if doc.get('values'):check(tagid in tags,f'{rel}: tag failed to load: {tagid}')
  if parts[2]=='recipes' and registry and not doc.get('conditions'):
   def visit(value):
    if isinstance(value,dict):
     for key,val in value.items():
      if key=='item' and isinstance(val,str):check(val in registry['items']['ids'],f'{rel}: missing recipe item {val}')
      elif key=='tag' and isinstance(val,str):check(val in registry['items']['tags'],f'{rel}: missing ingredient tag {val}')
      else:visit(val)
    elif isinstance(value,list):
     for child in value:visit(child)
   visit(doc)
  if parts[2]=='loot_tables' and registry:
   def visit_loot(value):
    if isinstance(value,dict):
     if value.get('type') in ('minecraft:item','item'):
      ident=value.get('name','');ident=ident if ':' in ident else 'minecraft:'+ident
      check(ident in registry['items']['ids'],f'{rel}: missing loot item {ident}')
     if 'entries' in value:
      check(isinstance(value['entries'],list) and all(isinstance(entry,dict) for entry in value['entries']),f'{rel}: loot entries must be a flat array of objects')
     for child in value.values():visit_loot(child)
    elif isinstance(value,list):
     for child in value:visit_loot(child)
   visit_loot(doc)
report={'json_files':files,'checks':checks,'registry_validated':bool(registry),'passed':not errors,'errors':errors,'warnings':warnings}
if args.report:
 args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
raise SystemExit(bool(errors))
