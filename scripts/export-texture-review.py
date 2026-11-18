"""Package current edited artwork, source references and real client gallery screenshots.

Run --prepare before launch-texture-gallery.ps1, then --package after both clients finish.
No files outside this project's release/build directories are changed.
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from hashlib import sha256
import argparse
import io
import json
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets'
OUT = ROOT / 'release/texture-review-2026-11-08'
WORK = ROOT / 'build/texture-gallery'
EXCLUDE = {'raw_squid', 'raw_glow_squid', 'cooked_squid', 'cooked_glow_squid',
           'endfish', 'end_fish', 'cooked_endfish', 'cooked_end_fish', 'sulfur_end_fish', 'cooked_sulfur_end_fish',
           'warped_toad', 'raw_warped_toad', 'cooked_warped_toad',
           'pufferfish', 'cooked_pufferfish', 'tropical_fish', 'cooked_tropical_fish'}
ARCHIVES = []


def reset_generated_directory(path):
    # Only these generated review directories may be replaced. Never touch config or saves.
    allowed = {OUT.resolve(), (WORK / 'before-overrides').resolve(),
               (WORK / 'screenshots').resolve(), (WORK / 'derived-textures').resolve()}
    resolved = path.resolve()
    if resolved not in allowed or ROOT.resolve() not in resolved.parents:
        raise ValueError('Unexpected generated review directory: ' + str(resolved))
    if resolved.exists():
        shutil.rmtree(resolved)
    resolved.mkdir(parents=True)


def archives():
    if not ARCHIVES:
        paths = sorted((ROOT / 'libs').rglob('*.jar'))
        dependencies = json.loads((ROOT / 'production-dependencies.json').read_text())
        for entry in dependencies['artifacts']:
            if entry.get('source', {}).get('path'):
                paths.append(Path.home() / '.gradle/caches/modules-2/files-2.1' / entry['source']['path'])
        paths += [Path.home() / '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar',
                  Path.home() / 'Downloads/assets_lendal mods2-v8.zip']
        for path in paths:
            if path.exists():
                archive = ZipFile(path)
                ARCHIVES.append((path, archive, set(archive.namelist())))
    return ARCHIVES


def source(relative, current=True):
    path = ASSETS / relative
    if current and path.exists():
        return path.read_bytes(), 'project:src/main/resources/assets/' + relative
    for jar, archive, names in archives():
        key = 'assets/' + relative
        if key in names:
            return archive.read(key), jar.name + '!' + key
    raise FileNotFoundError(relative)


def save(relative, data):
    path = OUT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return path


def reference(relative, original, overrides=True):
    try:
        data, provenance = source(original, False)
    except FileNotFoundError:
        # Preserve a verified earlier export when the user's source download moved.
        # This is a recorded upstream reference, never newly invented artwork.
        with ZipFile(OUT.with_suffix('.zip')) as previous:
            key = 'before/assets/' + relative
            data = previous.read(key)
            hashes = {row['path']: row['sha256'] for row in json.loads(previous.read('SHA256.json'))}
            if sha256(data).hexdigest() != hashes[key]:
                raise RuntimeError('Earlier reference checksum mismatch: ' + key)
            provenance = 'previous verified review archive!' + key
    save('before/assets/' + relative, data)
    if overrides:
        path = WORK / 'before-overrides/assets' / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return provenance


def seafood_pairs():
    text = (ROOT / 'src/main/java/net/amurdza/examplemod/config/DocumentedContentBalance.java').read_text()
    pairs = []
    for entity, raw, cooked in re.findall(r'fish\("([^"]+)", "([^"]+)", "(aoemod:[^"]+)"', text):
        if any(x.split(':')[-1] in EXCLUDE for x in (entity, raw, cooked)):
            continue
        pairs.append(dict(entity=entity, raw=raw, cooked=cooked))
    for row in pairs:
        row['availability'] = 'current food pair'
    represented = {(row['raw'], row['cooked']) for row in pairs}
    missing = []
    for recipe in (ROOT / 'src/main/resources/data/aoemod/recipes').rglob('*.json'):
        data = json.loads(recipe.read_text())
        result = data.get('result')
        if not isinstance(result, str) or not result.startswith('aoemod:cooked_') or result.split(':')[1] in EXCLUDE:
            continue
        ingredient = data.get('ingredient', {}).get('item') if isinstance(data.get('ingredient'), dict) else None
        if ingredient and (ingredient, result) not in represented:
            missing.append([ingredient, result, recipe.relative_to(ROOT).as_posix()])
    if missing:
        raise RuntimeError('Unrepresented AOE cooked-food recipe inputs: ' + str(missing))
    return pairs


def texture_for_item(identifier):
    namespace, name = identifier.split(':')
    model, model_origin = source(namespace + '/models/item/' + name + '.json')
    model = json.loads(model)
    texture = model.get('textures', {}).get('layer0', identifier.replace(':', ':item/'))
    if ':' not in texture:
        texture = 'minecraft:' + texture
    namespace, path = texture.split(':')
    return namespace + '/textures/' + path + '.png'


def prepare():
    for directory in [OUT, WORK / 'before-overrides', WORK / 'screenshots', WORK / 'derived-textures']:
        reset_generated_directory(directory)
    WORK.mkdir(parents=True, exist_ok=True)
    inventory = (ROOT / 'TEXTURE_INVENTORY.md').read_text(encoding='utf-8')
    authored_section = inventory.split('## Supplied or upstream artwork')[0]
    edited = sorted(set(re.findall(r'\]\(src/main/resources/assets/([^)]*\.png)\)', authored_section)))
    edited += ['hybrid_aquatic/textures/item/mahi.png']
    edited += [f'aoemod/textures/block/{prefix}blue_berry_bush_stage{stage}.png'
               for prefix in ['', 'snowy_'] for stage in [0, 1]]
    edited = sorted(set(edited))
    manifest = []
    for relative in edited:
        if Path(relative).stem in EXCLUDE or '/endfish/' in relative:
            continue
        path = ASSETS / relative
        if not path.exists():
            continue
        save('after/assets/' + relative, path.read_bytes())
        manifest.append(dict(file='after/assets/' + relative, kind='edited artwork', source='project', sha256=sha256(path.read_bytes()).hexdigest()))

    pairs = seafood_pairs()
    for row in pairs:
        for key in ('raw', 'cooked'):
            relative = texture_for_item(row[key])
            data, origin = source(relative)
            target = 'seafood/' + key + '/' + row[key].replace(':', '__') + '.png'
            save(target, data)
            row[key + '_file'] = target
            row[key + '_source'] = origin
            manifest.append(dict(file=target, kind='seafood ' + key, source=origin, sha256=sha256(data).hexdigest()))
    save('seafood/pairs.json', (json.dumps(pairs, indent=2) + '\n').encode())

    # Complete geometry and animation resources for the revised aquatic models.
    for directory in ['geo/cephalopod/arrow_squid', 'geo/crustacean/flower_crab', 'geo/crustacean/lightfoot_crab',
                      'animations/entity/crustacean/flower_crab', 'animations/entity/crustacean/lightfoot_crab']:
        for path in (ASSETS / 'aoemod' / directory).rglob('*'):
            if path.is_file():
                relative = path.relative_to(ASSETS).as_posix()
                save('after/assets/' + relative, path.read_bytes())
                reference(relative, relative.replace('aoemod/', 'hybrid_aquatic/', 1))
    reference('aoemod/textures/entity/cephalopod/arrow_squid/arrow_squid.png',
              'hybrid_aquatic/textures/entity/cephalopod/arrow_squid/arrow_squid.png')
    relative = 'hybrid_aquatic/animations/entity/cephalopod/arrow_squid/arrow_squid.animation.json'
    data, origin = source(relative, False)
    for phase in ['before', 'after']: save(phase + '/assets/' + relative, data)
    for name in ['starfish', 'starfish_small', 'starfish_crown_of_thorns']:
        relative = 'aoemod/geo/critter/starfish/' + name + '.geo.json'
        save('after/assets/' + relative, (ASSETS / relative).read_bytes())
        original = 'starfish' if name == 'starfish_small' else name
        reference(relative, 'hybrid_aquatic/geo/critter/starfish/' + original + '.geo.json')
    for jar, archive, names in archives():
        for key in sorted(names):
            if key.startswith(('assets/hybrid_aquatic/textures/entity/critter/starfish/',
                               'assets/hybrid_aquatic/animations/entity/critter/starfish/')):
                if not key.endswith('/') and 'brittlestar' not in key:
                    save('after/' + key, archive.read(key))
                    save('before/' + key, archive.read(key))
    for name in ['flower_crab', 'lightfoot_crab']:
        relative = f'hybrid_aquatic/textures/entity/crustacean/{name}/{name}.png'
        data, origin = source(relative, False)
        save('after/assets/' + relative, data)
        save('before/assets/' + relative, data)

    for name in ['orc_savage', 'orc_savage_shield', 'orc_savage_big_2sword']:
        relative = f'aoemod/textures/entity/orcz/{name}.png'
        reference(relative, f'orcz/textures/entities/{name}.png')
    # Current boss geometry relocates three sword UV faces. Copy their original pixels
    # to the same unused atlas island so the BEFORE image retains native metal colors.
    boss = WORK / 'before-overrides/assets/aoemod/textures/entity/orcz/orc_savage_big_2sword.png'
    quoted = str(boss).replace("'", "''")
    subprocess.run(['powershell', '-NoProfile', '-Command',
        "Add-Type -AssemblyName System.Drawing; $inputAtlas=[Drawing.Bitmap]::FromFile('" + quoted + "'); "
        "$copiedAtlas=[Drawing.Bitmap]::new($inputAtlas); "
        "for($y=0;$y -lt 36;$y++){for($x=0;$x -lt 64;$x++){"
        "$copiedAtlas.SetPixel(160+$x,160+$y,$inputAtlas.GetPixel($x,36+$y))}}; "
        "$inputAtlas.Dispose(); $copiedAtlas.Save('" + quoted + "',[Drawing.Imaging.ImageFormat]::Png); $copiedAtlas.Dispose()"], check=True)
    # Reference file in the ZIP remains the untouched original atlas.

    for prefix in ['', 'snowy_']:
        for stage in range(4):
            relative = f'aoemod/textures/block/{prefix}blue_berry_bush_stage{stage}.png'
            original = ('snowrealmagic' if prefix else 'minecraft') + f'/textures/block/{prefix}sweet_berry_bush_stage{stage}.png'
            reference(relative, original)
    for relative in edited:
        if relative.startswith('hearthandharvest/'):
            reference(relative, relative)
    reference('hybrid_aquatic/textures/item/mahi.png', 'hybrid_aquatic/textures/item/mahi.png', False)
    reference('aoemod/textures/block/soul_soil_quartz_ore.png', 'modid/textures/block/soul_soil_quartz_ore.png')
    for leaf in ['azalea_leaves', 'flowering_azalea_leaves']:
        reference(f'aoemod/textures/block/tropical_{leaf}.png', f'minecraft/textures/block/{leaf}.png', False)
    reference('aoemod/textures/block/wooden_portal.png', 'minecraft/textures/block/nether_portal.png', False)
    reference('ambientadditions/textures/entity/blue_spotted_stingray.png',
              'ambientadditions/textures/entity/blue_spotted_stingray.png')
    # The current queen uses vanilla spider geometry/art at king-spider scale.
    # The reference is the original Twilight Forest king-spider atlas, not an invented draft.
    spider_source = next((n for _, _, names in archives() for n in names if n.endswith('/kingspider.png')), None)
    if spider_source:
        data, origin = source(spider_source.removeprefix('assets/'), False)
        save('before/assets/' + spider_source.removeprefix('assets/'), data)
        path = WORK / 'before-overrides/assets/minecraft/textures/entity/spider/spider.png'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    for name in ['stalker_cobalt', 'stalker_cobaltglow']:
        data, origin = source(f'outer_end/textures/entity/{name}.png', False)
        save(f'after/assets/outer_end/textures/entity/{name}.png', data)
        save(f'before/assets/outer_end/textures/entity/{name}.png', data)
    for name in ['stalker_rose', 'stalker_roseglow']:
        data, origin = source(f'outer_end/textures/entity/{name}.png', False)
        save(f'before/assets/outer_end/textures/entity/{name}.png', data)
    for name in ['entity/model/ArthropleuraModel.java', 'entity/render/ArthropleuraRenderer.java',
                 'entity/ArthropleuraEntity.java', 'entity/SpiderQueenEntity.java',
                 'entity/render/SpiderQueenRenderer.java', 'client/StalkerGlowLayer.java',
                 'mixins/othermods/orcz/BoneBossSwordUV.java', 'util/StingrayTextureColors.java',
                 'mixins/othermods/hybridaquatic/TallCrabModels.java', 'mixins/othermods/hybridaquatic/TallCrabRenderer.java',
                 'mixins/othermods/hybridaquatic/ArrowSquidModel.java',
                 'mixins/othermods/hybridaquatic/StarfishModels.java', 'util/AquaticMobSizes.java',
                 'entity/render/IllagerLordRenderer.java', 'entity/model/IllagerLordModel.java']:
        path = ROOT / 'src/main/java/net/amurdza/examplemod' / name
        if path.exists():
            save('after/source/' + name, path.read_bytes())
    relative = 'aoemod/geo/illager_king.geo.json'
    save('after/assets/' + relative, (ASSETS / relative).read_bytes())
    baseline = subprocess.check_output(['git', 'show', 'HEAD:src/main/resources/assets/' + relative], cwd=ROOT)
    save('before/assets/' + relative, baseline)
    path = WORK / 'before-overrides/assets' / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(baseline)
    relative = 'aoemod/textures/entity/illager_lord.png'
    for phase in ['before', 'after']: save(phase + '/assets/' + relative, (ASSETS / relative).read_bytes())
    relative = 'aoemod/animations/illager_lord.animation.json'
    for phase in ['before', 'after']: save(phase + '/assets/' + relative, (ASSETS / relative).read_bytes())
    for name in ['ArthropleuraModel-before.java', 'ArthropleuraRenderer-before.java']:
        path = ROOT / 'tmp/seafood-refinements/models' / name
        save('before/source/' + name, path.read_bytes())
    # The archived historical recolor is no longer bound by the queen renderer.
    save('after/assets/minecraft/textures/entity/spider/spider.png', source('minecraft/textures/entity/spider/spider.png', False)[0])
    # The source audit is historical. Do not carry removed assets back into the review.
    filtered_inventory = '\n'.join(line for line in inventory.splitlines()
                                   if not any(word in line for word in EXCLUDE))
    save('provenance/TEXTURE_INVENTORY.md', ('Historical source audit; removed asset rows have been omitted. '
         'Counts below describe the earlier audit, not this revised archive.\n\n' + filtered_inventory).encode())
    save('provenance/asset-manifest.json', (json.dumps(manifest, indent=2) + '\n').encode())
    for path in (ASSETS / 'aoemod/models/block').glob('*.json*'):
        if any(word in path.name for word in ['blue_berry', 'soul_soil_quartz', 'tropical_azalea', 'tropical_flowering']):
            save('after/assets/' + path.relative_to(ASSETS).as_posix(), path.read_bytes())

    species = list(dict.fromkeys(row['entity'] for row in pairs))
    species += ['aoemod:arthropleura', 'aoemod:spider_queen', 'aoemod:illager_lord', 'orcz:orcsavagemace', 'orcz:orcshield',
                'orcz:bigorcboss', 'outer_end:stalker']
    compare = {'aoemod:arthropleura', 'aoemod:spider_queen', 'aoemod:illager_lord', 'orcz:orcsavagemace', 'orcz:orcshield',
               'orcz:bigorcboss', 'outer_end:stalker', 'hybrid_aquatic:arrow_squid',
               'hybrid_aquatic:flower_crab', 'hybrid_aquatic:lightfoot_crab', 'hybrid_aquatic:starfish',
               'ambientadditions:blue_spotted_stingray'}
    before, after = [], []
    scales = {'aoemod:arthropleura':130, 'aoemod:spider_queen':115, 'orcz:orcsavagemace':105,
              'orcz:orcshield':105, 'orcz:bigorcboss':80,
              'hybrid_aquatic:flower_crab':130, 'hybrid_aquatic:lightfoot_crab':130,
              'hybrid_aquatic:arrow_squid':145, 'outer_end:stalker':95,
              'hybrid_aquatic:octopus':230, 'hybrid_aquatic:starfish':230}
    for entity in species:
        for angle, label in [(25, 'front'), (115, 'side'), (205, 'rear')]:
            view = dict(id=entity, title=entity.split(':')[1].replace('_', ' ').title(),
                        label=entity.replace(':', '_') + '_' + label, angle=angle,
                        angleName='rear / top' if label == 'rear' else label, pitch=-45 if label == 'rear' else -20,
                        scale=scales.get(entity, 200))
            if entity == 'aoemod:illager_lord': view['visualRadius'] = 2.5
            after.append(view)
            if entity in compare:
                old = dict(view)
                old['baselineKind'] = 'upstream reference rendered using the shared/current compatible geometry'
                old['baselineSource'] = entity.split(':')[0] + ' installed dependency archive'
                if entity == 'aoemod:arthropleura': old['note'] = 'Archived fourteen-plate model before the eight-plate textured revision'
                if entity == 'aoemod:spider_queen': old['note'] = 'Twilight Forest king-spider source texture on the shared spider geometry'
                if entity.startswith('orcz:'): old['note'] = 'Native Orcz metal equipment before bone recolor'
                if entity == 'aoemod:arthropleura':
                    old['baselineKind'] = 'exact archived pre-revision implementation'
                    old['baselineSource'] = 'tmp/seafood-refinements/models/ArthropleuraModel-before.java and ArthropleuraRenderer-before.java'
                if entity == 'aoemod:spider_queen': old['baselineSource'] = 'twilightforest-4.3.2508.jar!assets/twilightforest/textures/model/kingspider.png'
                if entity.startswith('hybrid_aquatic:'):
                    old['baselineKind'] = 'native upstream geometry, animation and texture; crab scale compensation removes AOE enlargement'
                if entity == 'outer_end:stalker':
                    old['baselineKind'] = 'native upstream renderer and rose variant without the added AOE emissive layer'
                    old['baselineSource'] = 'outer_end-1.0.0.jar!assets/outer_end/textures/entity/stalker_rose.png'
                if entity == 'aoemod:illager_lord':
                    old['baselineKind'] = 'prior committed equipment-anchor geometry with the current compatible item renderer'
                    old['baselineSource'] = 'git HEAD:src/main/resources/assets/aoemod/geo/illager_king.geo.json; imported texture unchanged'
                before.append(old)
    for rows in [before, after]:
        originals = [row for row in rows if row['id'] == 'hybrid_aquatic:starfish']
        rows[:] = [row for row in rows if row['id'] != 'hybrid_aquatic:starfish']
        for variant in ['SMALL', 'MEDIUM', 'CROWN_OF_THORNS']:
            for row in originals:
                view = dict(row)
                view['variant'] = variant
                view['title'] = 'Starfish ' + variant.lower().replace('_', ' ')
                view['label'] = row['label'].replace('starfish_', 'starfish_' + variant.lower() + '_')
                if view['angleName'] == 'rear / top': view['pitch'] = -70
                rows.append(view)
    blocks = [('aoemod:blue_berry_bush', 'Blue berries stage 2', {'age':'2'}, None),
              ('aoemod:blue_berry_bush', 'Snowy blue berries stage 2', {'age':'2'}, 'aoemod:block/snowy_blue_berry_bush_stage2'),
              ('aoemod:blue_berry_bush', 'Blue berries', {'age':'3'}, None),
              ('aoemod:blue_berry_bush', 'Snowy blue berries', {'age':'3'}, 'aoemod:block/snowy_blue_berry_bush_stage3'),
              ('aoemod:soul_soil_quartz_ore', 'Soul soil quartz ore', {}, None),
              ('minecraft:azalea_leaves', 'Tropical azalea leaves', {}, 'aoemod:block/tropical_azalea_leaves'),
              ('minecraft:flowering_azalea_leaves', 'Tropical flowering azalea leaves', {}, 'aoemod:block/tropical_flowering_azalea_leaves')]
    destinations = portal_destinations()
    for destination in destinations:
        name = destination['id']
        blocks.append((f'aoemod:{name}_portal', 'Wooden ' + name + ' portal', {'axis':'x'}, None))
        for directory in ['models/block', 'blockstates']:
            path = ASSETS / 'aoemod' / directory / (name + '_portal.json')
            if path.exists(): save('after/assets/' + path.relative_to(ASSETS).as_posix(), path.read_bytes())
    for axis in ['x', 'z']:
        path = ASSETS / 'aoemod/models/block' / ('wooden_portal_' + axis + '.json')
        save('after/assets/' + path.relative_to(ASSETS).as_posix(), path.read_bytes())
    save('after/portal-colors.json', (json.dumps(destinations, indent=2) + '\n').encode())
    for name in ['portal/PortalDestination.java', 'client/PortalTexturePack.java', 'client/PortalClient.java']:
        path = ROOT / 'src/main/java/net/amurdza/examplemod' / name
        save('after/source/' + name, path.read_bytes())
    for age in ['5']:
        blocks.append(('hearthandharvest:corn_stalk', 'Corn stage ' + age, {'age':age,'section':'bottom'}, None))
    for entity, title, properties, model in blocks:
        for angle, label in [(30,'front'), (120,'side')]:
            view = dict(id=entity, title=title, label=title.lower().replace(' ','_')+'_'+label,
                        angle=angle, angleName=label, scale=190, block=True, properties=properties)
            if model: view['model'] = model
            if 'azalea' in entity: view['dimension'] = 'aoemod:tropical_dimension'
            after.append(view)
            old = dict(view)
            old['baselineKind'] = 'upstream block artwork before AOE palette/composite change'
            old['baselineSource'] = 'before/assets/'
            if 'blue_berry' in entity:
                old['id'] = 'minecraft:sweet_berry_bush'
                if model: old['model'] = 'snowrealmagic:block/snowy_sweet_berry_bush_stage' + properties['age']
                before.append(old)
            elif 'azalea' in entity:
                old.pop('model', None)
                old['dimension'] = 'minecraft:overworld'
                before.append(old)
            elif 'corn_stalk' in entity:
                before.append(old)
            elif 'soul_soil_quartz' in entity:
                before.append(old)
            elif entity.endswith('_portal'):
                old['id'] = 'minecraft:nether_portal'
                old['baselineKind'] = 'vanilla portal source animation before neutralization and destination tint'
                before.append(old)
    first_checks = ['angling_anglerfish_side', 'finsandtails_rubber_belly_glider_side',
                    'hybrid_aquatic_arrow_squid_side', 'hybrid_aquatic_starfish_medium_rear',
                    'hybrid_aquatic_lightfoot_crab_side', 'orcz_bigorcboss_front',
                    'aoemod_arthropleura_side', 'blue_berries_front', 'snowy_blue_berries_side',
                    'wooden_tropical_portal_front', 'corn_stage_5_side',
                    'aoemod_illager_lord_front', 'tropical_azalea_leaves_front']
    # Inspect the important subjects first, keeping each specimen's angles together.
    # This also preserves one real entity/variant across its three photographs.
    groups = {}
    for row in after:
        key = (row['id'], json.dumps(row.get('properties'), sort_keys=True), row.get('variant'),
               row.get('model'), row.get('dimension'))
        groups.setdefault(key, []).append(row)
    rank = lambda row: first_checks.index(row['label']) if row['label'] in first_checks else len(first_checks)
    after = [row for group in sorted(groups.values(), key=lambda group: min(map(rank, group)))
             for row in sorted(group, key=rank)]
    for row in before + after:
        if row['id'] == 'hybrid_aquatic:arrow_squid':
            row['settleTicks'] = {'front': 60, 'side': 70, 'rear / top': 80}[row['angleName']]
        if row['id'] in {'hybrid_aquatic:flower_crab', 'hybrid_aquatic:lightfoot_crab'}:
            row['visualRadius'] = 1.8
        if row['id'] == 'hybrid_aquatic:starfish':
            row['visualRadius'] = .7
    for label, rows in [('before', before), ('after', after)]:
        (WORK / (label + '-views.json')).write_text(json.dumps(rows, indent=2)+'\n')
        save('provenance/' + label + '-view-manifest.json', (json.dumps(rows, indent=2)+'\n').encode())
    print(json.dumps(dict(edited_files=len(manifest), seafood_pairs=len(pairs), before_views=len(before), after_views=len(after))))


def portal_destinations():
    text = (ROOT / 'src/main/java/net/amurdza/examplemod/portal/PortalDestination.java').read_text()
    return [dict(id=name, rgb='#' + color, texture='portal-colors/' + name + '_portal.png')
            for name, color in re.findall(r'\w+\("([a-z]+)", [^,]+, 0x([0-9A-F]{6})\)', text)]


def public_view(view):
    result = dict(view)
    if not view.get('block'):
        suffix = view['label'].rsplit('_', 1)[-1]
        labels = {'front': ('view_1', 'View 1'), 'side': ('view_2', 'View 2'),
                  'rear': ('view_3_above', 'View 3 / above')}
        name, title = labels[suffix]
        result['label'] = view['label'][:-len(suffix)] + name
        result['angleName'] = title
    return result


def package():
    for phase in ['before', 'after']:
        source_dir = WORK / 'screenshots' / phase
        report = json.loads((source_dir / 'capture-report.json').read_text())
        expected = json.loads((WORK / (phase + '-views.json')).read_text())
        if (report['status'] != 'passed' or len(report['captures']) != report['requested']
                or {row['label'] for row in report['captures']} != {row['label'] for row in expected}):
            raise RuntimeError(phase + ' gallery did not complete')
        unexpected = [p.name for p in source_dir.glob('*.png') if p.stem not in {v['label'] for v in expected}]
        if unexpected:
            raise RuntimeError('Stale screenshot files: ' + str(unexpected))
        public_views = [public_view(view) for view in expected]
        labels = {view['label']: public['label'] for view, public in zip(expected, public_views)}
        for original, public in labels.items():
            save(f'screenshots/{phase}/{public}.png', (source_dir / (original + '.png')).read_bytes())
        for capture in report['captures']:
            capture['label'] = labels[capture['label']]
            capture['path'] = f"screenshots/{phase}/{capture['label']}.png"
        save(f'screenshots/{phase}/capture-report.json', (json.dumps(report, indent=2) + '\n').encode())
        save(f'provenance/{phase}-view-manifest.json', (json.dumps(public_views, indent=2) + '\n').encode())
    for path in (WORK / 'derived-textures').rglob('*'):
        if path.is_file():
            save('after/runtime-assets/' + path.relative_to(WORK / 'derived-textures').as_posix(), path.read_bytes())
    readme = '''Texture and model review — 8 November 2026

seafood/: native-resolution raw and cooked PNGs, including the upstream raw items used by AOE cooked foods. pairs.json maps items to mobs and source archives.
after/assets/: current verified edited artwork, requested native crab atlases, geometry, and animations.
after/runtime-assets/: textures resolved by Minecraft's runtime resource packs. portal-colors/ contains all seven full animation strips with the exact destination tint baked in for convenient review; production uses one neutral strip plus the tint in after/portal-colors.json.
before/assets/: available original source artwork. before/source/ contains the exact archived prior Arthropleura implementation.
screenshots/: actual in-world Minecraft framebuffer captures, three angles per mob and two per block. Aquatic mobs occupy real water tanks; land mobs stand on land. Blocks are placed in the world with ordinary world lighting. Mob view numbers identify distinct camera positions; the third view looks down from above. Each phase has a capture report recording entity, camera, habitat and readiness. Screenshots are complete native frames, without cropping or retouching.

The two larger crabs are Flower Crab and Lightfoot Crab. Their texture is native; geometry/size/gait changed. The queen currently uses vanilla spider art; the historical edited queen atlas is retained for reference, and the BEFORE screenshot shows the Twilight Forest king-spider source. Arthropleura BEFORE is the archived fourteen-plate model, AFTER is the current eight-plate textured model. Orc BEFORE shows native metal equipment; AFTER shows bone. Stalker BEFORE shows the native purple variant without the added AOE glow layer; AFTER shows the retained cobalt variant with that glow layer. Unchanged seafood mobs have current views only. Ordinary squid, glow squid, their foods, and removed endfish are excluded. Arrow squid and vampire squid are separate requested species and remain included.

All three revised starfish variants are included with their native source geometry. Illager Lord artwork is unchanged imported artwork; its comparison uses the previous hand-anchor geometry with the current compatible item renderer. Per-image camera and provenance records are in provenance/before-view-manifest.json and after-view-manifest.json. Available exact historical implementations are identified explicitly; other before images are upstream references. Earlier generated item drafts not preserved in the repository are unavailable and have not been invented.

provenance/TEXTURE_INVENTORY.md records available attribution evidence as of 7 November, with excluded asset rows omitted. Its historical counts describe the earlier audit, not this archive. Sources retain their original authorship. Some assets are adaptations of upstream mods. No claim is made to ownership of underlying artwork. This is an asset-review archive, not an installable mod or resource pack.
'''
    save('README.txt', readme.encode('utf-8'))
    from html import escape
    gallery = ['<!doctype html><meta charset="utf-8"><title>AOE texture review</title>',
               '<style>body{background:#17232b;color:#e8eff2;font:16px system-ui;margin:30px;max-width:1500px}',
               'a{color:#8fdde5}section{margin:42px 0}.grid{display:grid;grid-template-columns:repeat(2,minmax(300px,1fr));gap:16px}',
               'img{width:100%;background:#253640}figure{margin:0}figcaption{padding:8px;color:#b8cbd3}',
               '.items{display:flex;flex-wrap:wrap;gap:18px}.items img{width:96px;height:96px;image-rendering:pixelated}',
               '</style><h1>AOE texture and model review</h1><p>8 November 2026. ',
               '<a href="README.txt">Read scope and source notes</a>. Images below are actual Minecraft screenshots in water tanks or on land.</p>',
               '<h2>Raw and cooked seafood</h2><div class="items">']
    pairs = json.loads((OUT / 'seafood/pairs.json').read_text())
    shown = set()
    for row in pairs:
        for kind in ['raw', 'cooked']:
            path = row[kind + '_file']
            if path in shown: continue
            shown.add(path)
            gallery.append(f'<figure><a href="{escape(path)}"><img src="{escape(path)}"></a><figcaption>{escape(row[kind])}</figcaption></figure>')
    gallery.append('</div>')
    after_files = sorted((OUT / 'screenshots/after').glob('*.png'))
    for path in after_files:
        counterpart = OUT / 'screenshots/before' / path.name
        gallery.append('<section><h2>' + escape(path.stem.replace('_', ' ')) + '</h2><div class="grid">')
        for image in ([counterpart] if counterpart.exists() else []) + [path]:
            relative = image.relative_to(OUT).as_posix()
            gallery.append(f'<figure><a href="{escape(relative)}"><img loading="lazy" src="{escape(relative)}"></a><figcaption>{escape(image.parent.name)}</figcaption></figure>')
        gallery.append('</div></section>')
    save('index.html', ''.join(gallery).encode('utf-8'))
    subprocess.run(['pwsh', '-NoProfile', '-File', str(ROOT / 'scripts/create-texture-review-overview.ps1')], check=True)
    files = sorted(p for p in OUT.rglob('*') if p.is_file() and p.name != 'SHA256.json')
    checksums = [{'path':p.relative_to(OUT).as_posix(),'bytes':p.stat().st_size,'sha256':sha256(p.read_bytes()).hexdigest()} for p in files]
    save('SHA256.json', (json.dumps(checksums, indent=2)+'\n').encode())
    excluded = [str(p.relative_to(OUT)) for p in OUT.rglob('*')
                if p.is_file() and (p.stem.split('__')[-1] in EXCLUDE
                                   or any(word in p.name for word in ['warped_toad', 'pufferfish', 'tropical_fish']))]
    if excluded:
        raise RuntimeError('Removed artwork remains in export: ' + str(excluded))
    archive_path = OUT.with_suffix('.zip')
    with ZipFile(archive_path, 'w', ZIP_DEFLATED) as archive:
        for path in sorted(OUT.rglob('*')):
            if path.is_file(): archive.write(path, path.relative_to(OUT).as_posix())
    print(json.dumps(dict(zip=str(archive_path), files=len(checksums)+1, bytes=archive_path.stat().st_size)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--prepare', action='store_true')
    parser.add_argument('--package', action='store_true')
    args = parser.parse_args()
    if args.prepare: prepare()
    if args.package: package()
