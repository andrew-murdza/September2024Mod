"""Raise both crab shells above short seagrass, with grounded feet.

Run with Python + numpy: python scripts/generate_tall_crabs.py
The numerical audit uses GeckoLib's reflected-X, Z/Y/X rotation convention.
"""
import copy
import itertools
import json
import math
from pathlib import Path
import zipfile

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/aoemod'
ARCHIVE = zipfile.ZipFile(ROOT / 'libs/pack/hybrid_aquatic-1.7.4.jar')
GROUND = 0.025
HEAD_TOP = 17.0 / 16.0  # One pixel above a complete short-seagrass block.
LEG_ANGLE = 45.0
NATIVE_SCALE = 1.25  # Native maximum individual size of five.
SCALES = {'flower_crab': 1.925, 'lightfoot_crab': 2.2}
# Retain the accepted lightfoot horizontal fan, steepening both species to
# the same declination. Solve lengths separately because shell scales differ.
LIGHTFOOT_LEFT_LEG_ROTATIONS = {
    1: [0, 19.23377569, 32.61469390],
    2: [0, 0, 31.16045769],
    3: [0, -19.20853638, 32.61000769],
    4: [0, -37.53663531, 38.73381542]}


def load(kind, category):
    folder = 'geo/crustacean' if category == 'geo' else 'animations/entity/crustacean'
    return json.loads(ARCHIVE.read(f'assets/hybrid_aquatic/{folder}/{kind}/{kind}.{category}.json'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def matrix(rotation):
    x, y, z = np.radians(np.asarray(rotation) * [-1, -1, 1])
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rz @ ry @ rx


def evaluate(value, time):
    if not isinstance(value, str):
        return value
    functions = {'sin': lambda x: math.sin(math.radians(x)), 'cos': lambda x: math.cos(math.radians(x)),
                 'clamp': lambda x, lo, hi: max(lo, min(hi, x)), 't': time}
    return eval(value.replace('math.', '').replace('query.anim_time', 't'), {'__builtins__': {}}, functions)


def transformed(model, animation=None, time=0):
    bones = {b['name']: b for b in model['minecraft:geometry'][0]['bones']}
    poses = animation.get('bones', {}) if animation else {}
    transforms = {}

    def transform(name):
        if name in transforms:
            return transforms[name]
        bone, pose = bones[name], poses.get(name, {})
        pivot = np.array(bone['pivot']) * [-1, 1, 1]
        rotation = np.array(bone.get('rotation', [0, 0, 0]), dtype=float)
        rotation += [evaluate(v, time) for v in pose.get('rotation', [0, 0, 0])]
        position = np.array([evaluate(v, time) for v in pose.get('position', [0, 0, 0])]) * [-1, 1, 1]
        local = np.eye(4)
        local[:3, :3] = matrix(rotation)
        local[:3, 3] = pivot + position - local[:3, :3] @ pivot
        result = transform(bone['parent']) @ local if 'parent' in bone else local
        transforms[name] = result
        return result

    result = []
    for name, bone in bones.items():
        for cube in bone.get('cubes', []):
            corners = np.array([np.array(cube['origin']) + np.array(cube['size']) * bits
                                for bits in itertools.product([0, 1], repeat=3)]) * [-1, 1, 1]
            corners = (transform(name) @ np.c_[corners, np.ones(8)].T).T[:, :3]
            result.append((name, corners))
    return result


def freeze_uv(cube):
    # A longer leg reuses its original face rectangles rather than reading
    # adjacent parts of the native texture atlas as box UV sizes grow.
    u, v = cube['uv']
    x, y, z = cube['size']
    cube['uv'] = {
        'north': {'uv': [u + z, v + z], 'uv_size': [x, y]},
        'east': {'uv': [u, v + z], 'uv_size': [z, y]},
        'south': {'uv': [u + 2 * z + x, v + z], 'uv_size': [x, y]},
        'west': {'uv': [u + z + x, v + z], 'uv_size': [z, y]},
        'up': {'uv': [u + z, v], 'uv_size': [x, z]},
        'down': {'uv': [u + z + x, v + z], 'uv_size': [x, -z]}}
    # GeckoLib swaps UP/DOWN vertices for mirrored per-face UVs, but not for
    # mirrored box UVs. Compensate when freezing a native box texture.
    if cube.get('mirror', False):
        cube['uv']['up'], cube['uv']['down'] = cube['uv']['down'], cube['uv']['up']


def lightfoot_claw_uv(cube, index):
    # The flower claw's shape includes transparent pixels: its north end is
    # open, the front third of both side walls is open, and the internal plate
    # faces north only. Reusing the lightfoot's old box rectangles put these
    # holes on different faces. Remap actual native claw pixels to match every
    # flower face's opacity/orientation while retaining lightfoot colors.
    if index == 0:
        cube['uv'] = {
            'east': {'uv': [3, 11], 'uv_size': [-3, 1]},
            'west': {'uv': [0, 11], 'uv_size': [3, 1]},
            'south': {'uv': [5, 10], 'uv_size': [1, 1]},
            'up': {'uv': [2, 8], 'uv_size': [1, 2]},
            'down': {'uv': [2, 10], 'uv_size': [1, -2]}}
        if cube.get('mirror', False):
            cube['uv']['up'], cube['uv']['down'] = cube['uv']['down'], cube['uv']['up']
    else:
        cube['uv'] = {'north': {'uv': [7, 8], 'uv_size': [1, 1]}}


def raised_leg_rotation(leg, sign):
    direction = matrix(LIGHTFOOT_LEFT_LEG_ROTATIONS[leg]) @ np.array([1, 0, 0])
    yaw = math.atan2(direction[2], direction[0])
    angle = math.radians(LEG_ANGLE)
    # GeckoLib composes roll before yaw; recover the existing horizontal fan
    # and solve the Euler angles for the new shaft declination.
    return [0, sign * round(math.degrees(math.asin(math.sin(yaw) * math.cos(angle))), 8),
            sign * round(math.degrees(math.atan2(math.sin(angle), math.cos(angle) * math.cos(yaw))), 8)]


def make_model(kind):
    model = load(kind, 'geo')
    geometry = model['minecraft:geometry'][0]
    geometry['description'].update(visible_bounds_width=3, visible_bounds_height=3,
                                   visible_bounds_offset=[0, 1, 0])
    lift = HEAD_TOP * 16 / (NATIVE_SCALE * SCALES[kind]) - 3
    flower = {b['name']: b for b in load('flower_crab', 'geo')['minecraft:geometry'][0]['bones']}
    for index, bone in enumerate(geometry['bones']):
        if kind == 'lightfoot_crab' and '_claw' in bone['name']:
            bone = copy.deepcopy(flower[bone['name']])
            for cube_index, cube in enumerate(bone['cubes']):
                lightfoot_claw_uv(cube, cube_index)
            geometry['bones'][index] = bone
        if kind == 'flower_crab' and '_leg_' in bone['name'] and bone['name'].endswith('_4'):
            # Rear legs are ordinary solid legs with exactly the same native
            # texture faces and cross-section as the front three pairs.
            side = bone['name'].split('_')[0]
            cube = copy.deepcopy(flower[f'{side}_leg_1']['cubes'][0])
            bone['pivot'][1] = flower[f'{side}_leg_1']['pivot'][1]
            cube['origin'][1] = bone['pivot'][1]
            cube['origin'][2] = bone['pivot'][2] - .5
            bone['cubes'] = [cube]
        bone['pivot'][1] += lift
        for cube in bone.get('cubes', []):
            cube['origin'][1] += lift
            if 'pivot' in cube:
                cube['pivot'][1] += lift
        if '_leg_' not in bone['name']:
            continue
        sign = 1 if bone['name'].startswith('left') else -1
        leg = int(bone['name'].rsplit('_', 1)[1])
        bone['rotation'] = raised_leg_rotation(leg, sign)
        cube = bone['cubes'][0]
        freeze_uv(cube)

        def set_length(length):
            cube['size'][0] = length
            cube['origin'][0] = bone['pivot'][0] if sign > 0 else bone['pivot'][0] - length

        def bottom():
            return next(points for name, points in transformed(model) if name == bone['name'])[:, 1].min()

        # Keep the shared angle and equal world-space shell height; fit each
        # solid shaft to the floor rather than letting longer legs penetrate it.
        lo, hi = 0.0, 12.0
        for _ in range(50):
            mid = (lo + hi) / 2
            set_length(mid)
            if bottom() > GROUND:
                lo = mid
            else:
                hi = mid
        set_length(round((lo + hi) / 2, 8))
    return model


def make_animations(kind):
    data = load(kind, 'animation')
    flower = load('flower_crab', 'animation')['animations']
    for name, animation in data['animations'].items():
        for side in ['left', 'right']:
            claw = f'{side}_claw'
            if claw in flower[name]['bones']:
                animation['bones'][claw] = copy.deepcopy(flower[name]['bones'][claw])
    for animation_name in ['misc.idle', 'misc.dig', 'move.walk']:
        animation = data['animations'][animation_name]
        animation['bones']['body'] = {'rotation': [0, -90, 0], 'position': [0, 0, 0]}
        walking = animation_name == 'move.walk'
        frequency, lift = (300, 12) if walking else (135, 2)
        if walking:
            animation['animation_length'] = 1.2
        for side in ['left', 'right']:
            for leg in range(1, 5):
                phase = (leg % 2) * 180 + (180 if side == 'right' else 0)
                direction = '-' if side == 'left' else ''
                animation['bones'][f'{side}_leg_{leg}'] = {'rotation': [0, 0,
                    f'{direction}math.clamp(math.sin(query.anim_time*{frequency}+{phase})*{lift},0,{lift})']}
    return data


def audit(kind, model, animations):
    vertices = np.concatenate([points for _, points in transformed(model)])
    legs = [b for b in model['minecraft:geometry'][0]['bones'] if '_leg_' in b['name']]
    angles = []
    for bone in legs:
        direction = matrix(bone['rotation']) @ np.array([1, 0, 0])
        angles.append(math.degrees(math.atan2(abs(direction[1]), math.hypot(direction[0], direction[2]))))
    min_feet, planted = math.inf, 8
    for animation_name in ['misc.idle', 'move.walk']:
        for frame in range(241):
            points = transformed(model, animations['animations'][animation_name], frame / 200)
            feet = [corners[:, 1].min() for name, corners in points if '_leg_' in name]
            min_feet = min(min_feet, min(feet))
            planted = min(planted, sum(y < .026 for y in feet))
    scale = NATIVE_SCALE * SCALES[kind]
    shell = next(b for b in model['minecraft:geometry'][0]['bones'] if b['name'] == 'torso')['cubes'][0]
    shell_top = (shell['origin'][1] + shell['size'][1]) * scale / 16
    assert abs(shell_top - HEAD_TOP) < 1e-6
    assert all(abs(angle - LEG_ANGLE) < 1e-6 for angle in angles)
    assert min_feet >= 0, (kind, min_feet)
    assert planted >= 4, (kind, planted)
    assert all(b['cubes'][0]['size'][1:] == [1, 1] for b in legs)
    rest = {'bones': {'body': {'rotation': [0, -90, 0]}}}
    feet = np.concatenate([points for name, points in transformed(model, rest) if '_leg_' in name])
    radius = float(np.hypot(feet[:, 0], feet[:, 2]).max() * scale / 16)
    return {'static_height_model_units': float(vertices[:, 1].max()),
            'rendered_head_top_blocks': shell_top,
            'rendered_eye_top_blocks': float(vertices[:, 1].max() * scale / 16),
            'shadow_radius_blocks': radius,
            'footprint_diameter_blocks': radius * 2,
            'minimum_leg_angle_degrees': min(angles),
            'maximum_leg_angle_degrees': max(angles),
            'minimum_animated_foot_y_model_units': float(min_feet),
            'minimum_planted_legs_in_walk_and_idle': int(planted),
            'body_scale_vs_previous_maximum_individual': SCALES[kind],
            'idle_and_walk_frames_audited': 482}


if __name__ == '__main__':
    report = {'method': "Native GeckoLib geometry with reflected-X and Z/Y/X transforms; 241 samples each of idle and walking animation, including every leg cube corner.",
              'reference': "Both solid shell tops stand 17/16 blocks above the visible supporting surface: one pixel above full-height short seagrass. AquaticGroundSupport keeps aquatic feet on the visible top of mud and soul sand. Eye stalks extend one model unit above the shell; the shadow ends at the outermost resting leg corner.",
              'species': {}}
    for kind in ['flower_crab', 'lightfoot_crab']:
        model, animations = make_model(kind), make_animations(kind)
        write(ASSETS / f'geo/crustacean/{kind}/{kind}.geo.json', model)
        write(ASSETS / f'animations/entity/crustacean/{kind}/{kind}.animation.json', animations)
        report['species'][kind] = audit(kind, model, animations)
    write(ROOT / 'scripts/tall-crab-geometry-calibration.json', report)
    print(json.dumps(report, indent=2))
