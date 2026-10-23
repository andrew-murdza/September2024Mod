#!/usr/bin/env python3
"""Validate the pack-specific model adapters against original installed assets (Python 3.11+)."""
import argparse
import io
import json
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NAMESPACES = {
    'biodiverse', 'blueprint', 'born_in_chaos_v1', 'bountiful_critters',
    'collectorsreap', 'farmersdelight', 'floral', 'pineapple_delight', 'vintagedelight', 'outer_end',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--minecraft-assets', type=Path, default=Path.home() /
                        '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar')
    args = parser.parse_args()
    assets, archives, errors, checked = {}, [], [], set()

    def index_zip(source):
        archive = zipfile.ZipFile(source)
        archives.append(archive)
        for name in archive.namelist():
            if name.startswith('assets/'):
                assets[name] = (archive, name)
        for name in archive.namelist():
            if name.startswith('META-INF/jarjar/') and name.endswith('.jar'):
                index_zip(io.BytesIO(archive.read(name)))

    def read(path):
        entry = assets[path]
        return json.loads(entry.read_text(encoding='utf-8') if isinstance(entry, Path)
                          else entry[0].read(entry[1]))

    def asset_path(ref, kind, extension='json'):
        ns, name = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
        if not re.fullmatch('[a-z0-9_.-]+', ns) or not re.fullmatch('[a-z0-9/._-]+', name):
            errors.append(f'Invalid resource identifier: {ref}')
        return f'assets/{ns}/{kind}/{name}.{extension}'

    def check_model(ref, stack=()):
        if ref in ('builtin/generated', 'builtin/entity',
                   'minecraft:builtin/generated', 'minecraft:builtin/entity'):
            return
        path = asset_path(ref, 'models')
        if path in stack:
            errors.append('Model cycle: ' + ' -> '.join((*stack, path)))
            return
        if path in checked:
            return
        checked.add(path)
        if path not in assets:
            errors.append('Missing model: ' + path)
            return
        doc = read(path)
        if 'parent' in doc:
            check_model(doc['parent'], (*stack, path))
        for override in doc.get('overrides', []):
            check_model(override['model'], (*stack, path))
        for texture in doc.get('textures', {}).values():
            if not texture.startswith('#') and asset_path(texture, 'textures', 'png') not in assets:
                errors.append(f'Missing texture {texture} from {path}')

    def walk(node):
        if isinstance(node, dict):
            if 'model' in node:
                check_model(node['model'])
            for value in node.values():
                walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)

    try:
        index_zip(args.minecraft_assets)
        manifest = json.loads((ROOT / 'production-dependencies.json').read_text())
        for artifact in manifest['artifacts']:
            location = artifact['source']
            base = ROOT if location['kind'] == 'project' else Path.home() / '.gradle/caches/modules-2/files-2.1'
            index_zip(base / location['path'])
        for base in [ROOT / 'src/generated/resources', ROOT / 'src/main/resources']:
            for path in base.glob('assets/**/*'):
                if path.is_file():
                    assets[path.relative_to(base).as_posix()] = path
        selected = []
        for path, entry in assets.items():
            if not isinstance(entry, Path):
                continue
            parts = path.split('/')
            if len(parts) > 3 and parts[1] in NAMESPACES and parts[2] in ('models', 'blockstates'):
                selected.append(path)
                if parts[2] == 'models':
                    check_model(parts[1] + ':' + path.split('/models/')[1][:-5])
                else:
                    walk(read(path))
        result = {
            'source_resources': sorted(selected),
            'source_resource_count': len(selected),
            'transitive_models_checked': len(checked),
            'errors': sorted(set(errors)),
            'scope': 'Model parent/override chains and concrete texture references only; runtime baking and rendering still required.',
        }
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({key: value for key, value in result.items() if key != 'source_resources'}, indent=2))
        return bool(errors)
    finally:
        for archive in archives:
            archive.close()


if __name__ == '__main__':
    sys.exit(main())
