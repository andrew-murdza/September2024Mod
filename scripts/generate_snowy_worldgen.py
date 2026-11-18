"""Derive Snowy's router from the installed Minecraft 1.20.1 vanilla data.

Run with the path to Mojang's client-extra.jar; no network or pack mutation is used.
The audit baseline differs only by removing noodles and adapting the bottom slide.
"""
import copy
import argparse
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/aoemod/worldgen'
parser = argparse.ArgumentParser()
parser.add_argument('vanilla_jar')
parser.add_argument('--stretch', type=float, default=2.0)
parser.add_argument('--deep-water-width', type=float, default=110.0,
                    help='Compress only the flat deep floor to reach this total deep-water width; zero preserves the profile')
parser.add_argument('--lake-threshold', type=float, default=0.1)
parser.add_argument('--dappled-threshold', type=float, default=0.35)
parser.add_argument('--ridge-only', action='store_true', help='Audit-only legacy approximation; do not use for the shipped profile')
args = parser.parse_args()
archive = zipfile.ZipFile(args.vanilla_jar)
PREFIX = 'data/minecraft/worldgen/density_function/overworld/'


def read(name):
    return json.loads(archive.read(name))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def remap(value):
    if isinstance(value, str):
        return value.replace('minecraft:overworld/', 'aoemod:snowy/')
    if isinstance(value, list):
        return [remap(item) for item in value]
    if isinstance(value, dict):
        return {key: remap(item) for key, item in value.items()}
    return value


def binary(kind, a, b):
    return {'type': 'minecraft:' + kind, 'argument1': a, 'argument2': b}


def expand(value, bias):
    """Cave-only threshold bias; terrain and the protective pillar graph are intact."""
    if isinstance(value, list):
        return [expand(item, bias) for item in value]
    if not isinstance(value, dict):
        return value
    result = {key: expand(item, bias) for key, item in value.items()}
    if result.get('type') == 'minecraft:add' and result.get('argument1') == 0.27:
        # The cheese-cave threshold is calibrated by SnowyVanillaTerrainTests.
        result['argument1'] = 0.27 - bias
    return result


# Reallocate the existing expanded volume from narrow spaghetti passages into
# fewer, broader cheese chambers. Calibrated against the previous shipped graph.
PREVIOUS_CAVE_BIAS = 0.016
CAVE_BIAS = 0.136
CAVERN_NOISE_SCALE = 0.6
functions = {name[len(PREFIX):-5]: remap(read(name)) for name in archive.namelist()
             if name.startswith(PREFIX) and name.endswith('.json')}
functions.pop('caves/noodle')
functions['continents'] = binary('max', 0.4, functions['continents'])
functions['erosion'] = binary('min', -0.15, functions['erosion'])
raw_ridges = functions.pop('ridges')
if not args.ridge_only:
    raw_ridges = {'type': 'minecraft:flat_cache', 'argument': {
        'type': 'aoemod:mountain_ridges', 'ridge': 'minecraft:ridge',
        'shift': 'minecraft:offset', 'stretch': args.stretch}}
    if args.deep_water_width > 0:
        raw_ridges['argument']['deep_water_width'] = args.deep_water_width
else:
    raw_ridges['argument']['xz_scale'] = 0.25 / args.stretch
functions['ridges_input'] = raw_ridges
a, b = args.lake_threshold, args.dappled_threshold
assert 0 < a < b < 1
lake_slope, forest_slope, high_slope = 0.1 / a, 0.25 / (b - a), 0.65 / (1 - b)
lake_derivative = 2 * lake_slope * forest_slope / (lake_slope + forest_slope)
forest_derivative = 2 * forest_slope * high_slope / (forest_slope + high_slope)
points = [(-1.0, -1.0, high_slope), (-b, -0.35, forest_derivative),
          (-a, -0.1, lake_derivative), (0.0, 0.0, lake_slope),
          (a, 0.1, lake_derivative), (b, 0.35, forest_derivative), (1.0, 1.0, high_slope)]
functions['ridges'] = {'type': 'minecraft:flat_cache', 'argument': {
    'type': 'minecraft:spline', 'spline': {'coordinate': 'aoemod:snowy/ridges_input',
    'points': [{'location': x, 'value': y, 'derivative': d} for x, y, d in points]}}}
if a == 0.1 and b == 0.35:
    # The chosen profile doubles every ridge wavelength, retaining vanilla's
    # weirdness distribution and taiga coverage without an extra value remap.
    functions['ridges'] = copy.deepcopy(raw_ridges)
functions['vanilla_offset'] = functions.pop('offset')
functions['offset'] = {'type': 'minecraft:flat_cache', 'argument': {
    'type': 'aoemod:mountain_lake_offset', 'offset': 'aoemod:snowy/vanilla_offset',
    'ridges': 'aoemod:snowy/ridges', 'continents': 'aoemod:snowy/continents', 'erosion': 'aoemod:snowy/erosion'}}

settings = remap(read('data/minecraft/worldgen/noise_settings/overworld.json'))
settings['noise'] = {'min_y': -96, 'height': 416, 'size_horizontal': 1, 'size_vertical': 2}
settings['sea_level'] = 63
settings['ore_veins_enabled'] = False
settings['spawn_target'] = []
settings['surface_rule'] = {'type': 'aoemod:dimension_surface', 'dimension': 'snowy'}
router = settings['noise_router']
# Noodles are a separate final min() in vanilla: remove just that branch.
final_density = router['final_density']['argument1']


def bottom_slide(value):
    if isinstance(value, dict):
        for item in value.values():
            bottom_slide(item)
        if (value.get('type') == 'minecraft:y_clamped_gradient'
                and value.get('from_y') == -64 and value.get('to_y') == -40):
            value['from_y'], value['to_y'] = -96, -72
    elif isinstance(value, list):
        for item in value:
            bottom_slide(item)


bottom_slide(final_density)
bottom_slide(router['initial_density_without_jaggedness'])
# Both baselines use precisely the same terrain; only cavity branches differ.
functions['vanilla_final_density'] = copy.deepcopy(final_density)
functions['vanilla_caves/entrances'] = copy.deepcopy(functions['caves/entrances'])
functions['vanilla_caves/spaghetti_2d'] = copy.deepcopy(functions['caves/spaghetti_2d'])


def baseline_refs(value):
    if isinstance(value, str):
        return value.replace('snowy/caves/entrances', 'snowy/vanilla_caves/entrances').replace(
            'snowy/caves/spaghetti_2d', 'snowy/vanilla_caves/spaghetti_2d')
    if isinstance(value, dict):
        return {key: baseline_refs(item) for key, item in value.items()}
    if isinstance(value, list):
        return [baseline_refs(item) for item in value]
    return value


functions['vanilla_final_density'] = baseline_refs(functions['vanilla_final_density'])


def previous_refs(value):
    if isinstance(value, str):
        return value.replace('snowy/caves/entrances', 'snowy/previous_caves/entrances').replace(
            'snowy/caves/spaghetti_2d', 'snowy/previous_caves/spaghetti_2d')
    if isinstance(value, dict):
        return {key: previous_refs(item) for key, item in value.items()}
    if isinstance(value, list):
        return [previous_refs(item) for item in value]
    return value


def broad_caverns(value):
    if isinstance(value, list):
        return [broad_caverns(item) for item in value]
    if not isinstance(value, dict):
        return value
    # This min branch is the entire narrow 2-D spaghetti generator.
    if (value.get('type') == 'minecraft:min'
            and isinstance(value.get('argument2'), dict)
            and value['argument2'].get('argument1') == 'aoemod:snowy/caves/spaghetti_2d'):
        return broad_caverns(value['argument1'])
    result = {key: broad_caverns(item) for key, item in value.items()}
    if result.get('type') == 'minecraft:noise' and result.get('noise') in {
            'minecraft:cave_cheese', 'minecraft:cave_layer', 'minecraft:cave_entrance'}:
        result['xz_scale'] *= CAVERN_NOISE_SCALE
        result['y_scale'] *= CAVERN_NOISE_SCALE
    return result


functions['previous_caves/entrances'] = binary('add', -PREVIOUS_CAVE_BIAS, functions['caves/entrances'])
functions['previous_caves/spaghetti_2d'] = binary('add', -PREVIOUS_CAVE_BIAS, functions['caves/spaghetti_2d'])
functions['previous_final_density'] = previous_refs(expand(final_density, PREVIOUS_CAVE_BIAS))
# Entrances normally min() broad cave mouths with narrow 3-D spaghetti tubes.
# Retain only the broad mouths, then enlarge them with the chamber noise domain.
functions['caves/entrances'] = broad_caverns(binary('add', -PREVIOUS_CAVE_BIAS,
    functions['caves/entrances']['argument']['argument1']))
functions['caves/spaghetti_2d'] = 1.0
functions['final_density'] = broad_caverns(expand(final_density, CAVE_BIAS))
router['final_density'] = 'aoemod:snowy/final_density'
router['lava'] = 0.0


def without_caves(value):
    if isinstance(value, dict):
        if value.get('type') == 'minecraft:range_choice' and value.get('input') == 'aoemod:snowy/sloped_cheese':
            return 'aoemod:snowy/sloped_cheese'
        return {key: without_caves(item) for key, item in value.items()}
    if isinstance(value, list):
        return [without_caves(item) for item in value]
    return value


functions['terrain_density'] = without_caves(final_density)
for profile in ['final_density', 'previous_final_density']:
    functions[profile] = {'type': 'minecraft:range_choice',
        'input': {'type': 'minecraft:abs', 'argument': 'aoemod:snowy/ridges'},
        'min_inclusive': 0.0, 'max_exclusive': 0.1,
        'when_in_range': 'aoemod:snowy/terrain_density',
        'when_out_of_range': functions[profile]}
# Restore the vanilla cheese/layer scales and 0.27 threshold. Vanilla's
# negative cheese tail carves chambers; -abs(noise) includes the positive tail
# at exactly the same threshold without adding spaghetti, noodles or outlets.
def noise(name, horizontal, vertical):
    return {'type': 'minecraft:noise', 'noise': 'minecraft:' + name,
            'xz_scale': horizontal, 'y_scale': vertical}


layer = binary('mul', 4.0, {'type': 'minecraft:square',
    'argument': noise('cave_layer', 1.0, 8.0)})
cheese = binary('mul', -1.0, {'type': 'minecraft:abs',
    'argument': noise('cave_cheese', 1.0, 2.0 / 3.0)})
functions['caves/cheese'] = binary('add', layer,
    {'type': 'minecraft:clamp', 'min': -1.0, 'max': 1.0,
     'input': binary('add', 0.27, cheese)})
# Old resource references remain loadable, but resolve to the same cheese-only graph.
functions['caves/continuous_cheese'] = copy.deepcopy(functions['caves/cheese'])
functions['caves/entrances'] = 1.0
# Keep the historical resource loadable for old references. Active Snowy
# supports are circular columns placed after the short-ceiling operation.
functions['caves/natural_pillars'] = copy.deepcopy(functions['caves/pillars'])
functions['caves/natural_pillars']['argument']['argument1']['argument1']['argument2']['xz_scale'] = 18.0
profile = {'type': 'aoemod:snowy_terrain_profile', 'ridges': 'aoemod:snowy/ridges',
           'caverns': 'aoemod:snowy/caves/cheese'}
functions['terrain_density'] = dict(profile, surface_only=True)
functions['final_density'] = {'type': 'minecraft:interpolated', 'argument': profile}
router['initial_density_without_jaggedness'] = 'aoemod:snowy/terrain_density'
settings['aquifers_enabled'] = False
for key in ['barrier', 'fluid_level_floodedness', 'fluid_level_spread', 'lava']:
    router[key] = 0.0
for name, value in functions.items():
    write(ROOT / 'density_function/snowy' / (name + '.json'), value)
write(ROOT / 'noise_settings/snowy_dimension.json', settings)

# Only cheese noise may open chambers. Independent carvers stay disabled to
# preserve the pillars and avoid reintroducing tunnel or canyon caves.
radius = 1.3 ** 0.5
for name in ['cave', 'cave_extra_underground', 'canyon']:
    carver = read('data/minecraft/worldgen/configured_carver/' + name + '.json')
    cfg = carver['config']
    cfg['probability'] = 0.0
    cfg['lava_level'] = {'absolute': -1000}
    if name == 'canyon':
        cfg['shape']['horizontal_radius_factor']['value']['min_inclusive'] *= radius
        cfg['shape']['horizontal_radius_factor']['value']['max_exclusive'] *= radius
        cfg['shape']['vertical_radius_default_factor'] *= radius
    else:
        cfg['probability'] *= 0.25
        for key in ['horizontal_radius_multiplier', 'vertical_radius_multiplier']:
            cfg[key]['value']['min_inclusive'] *= radius * 2
            cfg[key]['value']['max_exclusive'] *= radius * 2
    write(ROOT / 'configured_carver' / ('snowy_' + name + '.json'), carver)

# Choose the trunk size before its coordinate. A two-block trunk's center is
# origin+1, while an ordinary trunk's center is origin+0.5; both must stay in
# the central ten-block square [3, 13). The outer pass still selects once.
write(ROOT / 'configured_feature/snowy/centered_trees.json', {
    'type': 'aoemod:snowy_trees',
    'config': {'features': [{'feature': 'aoemod:snowy/centered_mega_spruce',
                             'chance': 1.0 / 15.0}],
               'default': 'aoemod:snowy/centered_spruce'}})
write(ROOT / 'placed_feature/snowy/trees.json', {
    'feature': 'aoemod:snowy/centered_trees',
    'placement': [{'type': 'minecraft:count', 'count': 1}]})
for tree, maximum in [('spruce', 12), ('mega_spruce', 11)]:
    branch = read('data/minecraft/worldgen/placed_feature/' + tree + '_checked.json')
    branch['placement'] = [
        {'type': 'minecraft:random_offset', 'xz_spread': {
            'type': 'minecraft:uniform', 'value': {'min_inclusive': 3, 'max_inclusive': maximum}},
         'y_spread': 0},
        {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
        {'type': 'minecraft:biome'},
    ] + branch['placement']
    write(ROOT / 'placed_feature/snowy' / ('centered_' + tree + '.json'), branch)
