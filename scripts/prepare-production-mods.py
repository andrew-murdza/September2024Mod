#!/usr/bin/env python3
"""Stage audited dependencies for an isolated production server, never a live profile."""
from pathlib import Path
import argparse, copy, hashlib, json, shutil, zipfile, io

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def isolate_default_refmap(data, filename):
    """Keep upstream code/assets intact while preventing a shared resource-name collision."""
    with zipfile.ZipFile(io.BytesIO(data)) as source:
        if 'mixin.refmap.json' not in source.namelist():
            return data, []
        configs = {}
        for name in source.namelist():
            if name.endswith('.mixins.json'):
                config = json.loads(source.read(name))
                if config.get('refmap', 'mixin.refmap.json') == 'mixin.refmap.json':
                    configs[name] = config
        if not configs:
            return data, []
        unique_name = 'aoe_compat/' + Path(filename).stem + '.refmap.json'
        for config in configs.values():
            config['refmap'] = unique_name
        output = io.BytesIO()
        with zipfile.ZipFile(output, 'w') as target:
            for info in source.infolist():
                if info.filename == 'mixin.refmap.json':
                    renamed = copy.copy(info)
                    renamed.filename = unique_name
                    target.writestr(renamed, source.read(info.filename))
                elif info.filename in configs:
                    target.writestr(info, json.dumps(configs[info.filename], indent=2) + '\n')
                else:
                    target.writestr(info, source.read(info.filename))
        return output.getvalue(), ['unique-default-mixin-refmap']

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release', type=Path, help='Reobfuscated AoE -all.jar; omit to stage only dependencies')
    parser.add_argument('--optional-mod', type=Path, action='append', default=[], help='Explicit optional integration fixture, never added to the dependency lock')
    args = parser.parse_args()
    destination = ROOT / 'run/september-production/mods'
    destination.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((ROOT / 'production-dependencies.json').read_text())
    staged = []
    expected = set()
    for artifact in manifest['artifacts']:
        if artifact['category'] == 'embedded-in-release':
            continue
        locator = artifact['source']
        source = (ROOT if locator['kind'] == 'project' else CACHE) / locator['path']
        data = source.read_bytes()
        if sha(data) != artifact['sha256']:
            raise RuntimeError('Dependency differs from audited original: ' + str(source))
        data, patches = isolate_default_refmap(data, artifact['filename'])
        staged_hash = sha(data)
        target = destination / artifact['filename']
        if target.exists() and sha(target.read_bytes()) not in (artifact['sha256'], staged_hash):
            raise RuntimeError('Refusing to overwrite a different staged dependency: ' + str(target))
        if not target.exists() or sha(target.read_bytes()) != staged_hash:
            target.write_bytes(data)
        expected.add(target.name)
        staged.append({'file': target.name, 'sha256': staged_hash, 'source_sha256': artifact['sha256'],
                       'compatibility_patches': patches, 'mods': artifact['mods']})
    if args.release:
        release = args.release.resolve()
        with zipfile.ZipFile(release) as jar:
            if 'META-INF/jarjar/metadata.json' not in jar.namelist():
                raise RuntimeError('Release must embed MixinExtras; use the reobfuscated -all.jar')
            embedded = json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']
            paths = [entry['path'] for entry in embedded]
            for artifact in manifest['artifacts']:
                if artifact['category'] == 'embedded-in-release' and not any(sha(jar.read(path)) == artifact['sha256'] for path in paths):
                    raise RuntimeError('Release is missing exact embedded dependency ' + artifact['coordinate'])
            if 'aoemod.mixins.refmap.json' not in jar.namelist():
                raise RuntimeError('Release is missing its production mixin reference map')
        target = destination / release.name
        # This isolated output may be refreshed after each successful release build.
        shutil.copy2(release, target)
        expected.add(target.name)
        staged.append({'file': target.name, 'sha256': sha(target.read_bytes()), 'mods': [{'modId': 'aoemod'}]})
    for optional in args.optional_mod:
        source = optional.resolve()
        target = destination / source.name
        shutil.copy2(source, target)
        expected.add(target.name)
        staged.append({'file': target.name, 'sha256': sha(target.read_bytes()), 'mods': [], 'qa_optional_pair': True})
    extras = [p.name for p in destination.glob('*.jar') if p.name not in expected and not (not args.release and p.name.startswith('aoemod-'))]
    if extras:
        raise RuntimeError('Unexpected jars in isolated mods directory: ' + ', '.join(extras))
    report = {'staged_jars': len(staged), 'minecraft': '1.20.1', 'forge': '47.4.22', 'release_included': bool(args.release), 'artifacts': staged}
    (destination.parent / 'staged-mods.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'staged_jars': len(staged), 'release_included': bool(args.release), 'destination': str(destination)}, indent=2))

if __name__ == '__main__':
    main()
