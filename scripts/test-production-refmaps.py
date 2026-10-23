"""Regression test for the two upstream jars sharing Mixin's default resource name."""
import importlib.util
import io
import json
from pathlib import Path
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('production_stage', Path(__file__).with_name('prepare-production-mods.py'))
stage = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stage)


class RefmapIsolationTests(unittest.TestCase):
    def archive(self, mod, mapping):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as jar:
            jar.writestr(mod + '.mixins.json', json.dumps({'package': mod, 'mixins': ['Example']}))
            jar.writestr('mixin.refmap.json', json.dumps({'mappings': mapping}))
            jar.writestr(mod + '/Example.class', b'unchanged-bytecode')
            jar.writestr('LICENSE', b'unchanged-upstream-license')
        return stream.getvalue()

    def test_shared_default_maps_keep_their_own_targets(self):
        resources = {}
        originals = {}
        for mod, target in [('flowers', 'byCandle'), ('structures', 'load')]:
            original = self.archive(mod, {mod: target})
            patched, changes = stage.isolate_default_refmap(original, mod + '.jar')
            self.assertEqual(patched, stage.isolate_default_refmap(original, mod + '.jar')[0])
            self.assertEqual(changes, ['unique-default-mixin-refmap'])
            with zipfile.ZipFile(io.BytesIO(patched)) as jar:
                resources.update({name: jar.read(name) for name in jar.namelist()})
                self.assertNotIn('mixin.refmap.json', jar.namelist())
                self.assertEqual(jar.read(mod + '/Example.class'), b'unchanged-bytecode')
                self.assertEqual(jar.read('LICENSE'), b'unchanged-upstream-license')
            originals[mod] = target
        # Simulate both patched archives sharing the same resource lookup namespace.
        for mod, target in originals.items():
            config = json.loads(resources[mod + '.mixins.json'])
            self.assertEqual(json.loads(resources[config['refmap']])['mappings'], {mod: target})


if __name__ == '__main__':
    unittest.main()
