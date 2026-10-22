"""Build the MultiHooker Divine crate in Blender and render preview angles.

Blender is the authoring tool: the crate geometry is defined here, instantiated in
Blender, textured with our own 16x16 art, and rendered for review. The same box
data is written out as a Minecraft block model JSON so the in-game model matches.

Run:  blender --background --python build_crate.py -- [tier]

MC coordinate system: x right, y up, z south. Element coords are 1/16 block.
Blender mapping used for the preview: (mcX, mcY, mcZ) -> (mcX, -mcZ, mcY) / 16.
The -z keeps the mapping right-handed so the preview is not mirrored (Blender
-Y is "front", and MC's front face is +z/south).
"""
import json
import math
import os
import sys

import bpy
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
RENDER = os.path.join(HERE, "preview", "render")
RES = os.path.join(
    HERE, "..", "..", "src", "main", "resources", "assets", "multihooker", "models", "block"
)

# Bigger, chunky crate: it nearly fills the block now (a 0.5px gap on each side
# and on top, so neighbouring crates never end up with coplanar faces) which
# gives it the full-bodied bulk of a barrel. The handle has been removed.
BOXES = [
    ("body", (0.5, 0, 0.5), (15.5, 12, 15.5), "side"),
    ("lid", (0.5, 12, 0.5), (15.5, 15.5, 15.5), "top"),
]
# UVs are in Minecraft's 0..16 model space (16 = the WHOLE texture, regardless of
# the texture's pixel size). Do NOT use the texture pixel size here.
TEX = 16
# which texture each face gets: faces are +x/-x/+y/-y/+z/-z in mc space.
# Front and back are the clean faces; the engraved name lives on east/west.
# This mapping must match FACE_TEX in gen_resources.py, which writes the models
# the game actually loads.
FACE_TEX = {
    "body": {"up": "side", "down": "side", "north": "front", "south": "front", "east": "side", "west": "side"},
    "lid": {"up": "top", "down": "side", "north": "front", "south": "front", "east": "side", "west": "side"},
}
# The lid's four sides are only 3/16 of a block tall, so mapping the whole
# texture onto them would squash it into a smear. Sample just the top 3/16 strip
# of the same texture the face below uses instead - that strip is the metal band,
# so the lid ends up with a clean banded rim that lines up with the body.
UV_OVERRIDE = {
    ("lid", "north"): [0, 0, TEX, 3],
    ("lid", "south"): [0, 0, TEX, 3],
    ("lid", "east"): [0, 0, TEX, 3],
    ("lid", "west"): [0, 0, TEX, 3],
}


def clean():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def to_blender(p):
    return Vector((p[0] / 16.0, -p[2] / 16.0, p[1] / 16.0))


def mc_face(n):
    """Minecraft face name for a Blender-space face normal."""
    if n.z > 0.5:
        return "up"
    if n.z < -0.5:
        return "down"
    if n.x > 0.5:
        return "east"
    if n.x < -0.5:
        return "west"
    if n.y < -0.5:
        return "south"
    return "north"


def face_uv(face, lx, ly, lz, sx, sy, sz):
    """Normalised (fu, fv) of a point on a face, matching how Minecraft lays a
    face out: viewed from outside, u runs right and v runs down, with the
    texture's top-left corner at the face's top-left."""
    if face == "up":
        return lx / sx, lz / sz
    if face == "down":
        return lx / sx, 1.0 - lz / sz
    if face == "north":
        return 1.0 - lx / sx, 1.0 - ly / sy
    if face == "south":
        return lx / sx, 1.0 - ly / sy
    if face == "west":
        return lz / sz, 1.0 - ly / sy
    return 1.0 - lz / sz, 1.0 - ly / sy   # east


def assign_uvs(obj, name, mn, mx):
    """Give the cube Minecraft-faithful per-face UVs, so the preview shows the
    same crop of each texture the game does. (Blender's default cube UVs sample
    only a quarter of the sprite, which made earlier previews misleading.)"""
    sx, sy, sz = mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2]
    loc = obj.location
    me = obj.data
    uvl = me.uv_layers.active or me.uv_layers.new()
    for poly in me.polygons:
        face = mc_face(poly.normal)
        u1, v1, u2, v2 = UV_OVERRIDE.get((name, face), [0, 0, TEX, TEX])
        for li in poly.loop_indices:
            co = me.vertices[me.loops[li].vertex_index].co
            wx, wy, wz = co.x + loc.x, co.y + loc.y, co.z + loc.z
            lx = 16.0 * wx - mn[0]          # inverse of to_blender:
            ly = 16.0 * wz - mn[1]          #   (X, Y, Z) -> (16X, 16Z, -16Y)
            lz = -16.0 * wy - mn[2]
            fu, fv = face_uv(face, lx, ly, lz, sx, sy, sz)
            u = (u1 + fu * (u2 - u1)) / 16.0
            v = 1.0 - (v1 + fv * (v2 - v1)) / 16.0
            uvl.data[li].uv = (u, v)


def make_material(name, image):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    bsdf = nt.nodes.get("Principled BSDF")
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = image
    tex.interpolation = "Closest"
    tex.extension = "CLIP"
    nt.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    if "Alpha" in bsdf.inputs:
        nt.links.new(tex.outputs["Alpha"], bsdf.inputs["Alpha"])
    try:
        mat.blend_method = "BLEND"
    except Exception:
        pass
    try:
        mat.surface_render_method = "BLENDED"
    except Exception:
        pass
    return mat


def add_box(name, mn, mx, mats):
    a = to_blender(mn)
    b = to_blender(mx)
    centre = (a + b) / 2.0
    size = (b - a)
    bpy.ops.mesh.primitive_cube_add(size=2.0, location=centre)
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = (size.x / 2.0, size.y / 2.0, size.z / 2.0)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    # one material slot per distinct texture this box's faces use, then point
    # each face at the right slot so the preview matches the Minecraft model
    slots = []
    for face in FACE_TEX[name].values():
        if face not in slots:
            slots.append(face)
    for slot in slots:
        obj.data.materials.append(mats[slot])
    for poly in obj.data.polygons:
        poly.material_index = slots.index(FACE_TEX[name][mc_face(poly.normal)])
    assign_uvs(obj, name, mn, mx)
    return obj


def load_images(tier):
    imgs = {}
    for slot in ("side", "front", "top", "lock"):
        path = os.path.join(OUT, f"{tier}_crate_{slot}.png")
        imgs[slot] = bpy.data.images.load(path)
    return imgs


def setup_world():
    world = bpy.data.worlds.new("W")
    bpy.context.scene.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    bg.inputs[0].default_value = (0.62, 0.64, 0.68, 1.0)
    bg.inputs[1].default_value = 1.1


def setup_lights():
    bpy.ops.object.light_add(type="SUN", location=(2, -2, 3))
    sun = bpy.context.active_object
    sun.data.energy = 3.0
    sun.rotation_euler = (math.radians(50), 0, math.radians(35))
    bpy.ops.object.light_add(type="AREA", location=(-2, 1.5, 2))
    area = bpy.context.active_object
    area.data.energy = 120.0
    area.data.size = 4.0


def setup_camera(target, radius):
    cam_data = bpy.data.cameras.new("Cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = 1.5
    cam = bpy.data.objects.new("Cam", cam_data)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    empty = bpy.data.objects.new("Target", None)
    empty.location = target
    bpy.context.scene.collection.objects.link(empty)
    con = cam.constraints.new("TRACK_TO")
    con.target = empty
    con.track_axis = "TRACK_NEGATIVE_Z"
    con.up_axis = "UP_Y"
    return cam


def pick_engine(scene):
    for eng in ("BLENDER_EEVEE_NEXT", "BLENDER_EEVEE", "CYCLES"):
        try:
            scene.render.engine = eng
            return eng
        except Exception:
            continue
    return scene.render.engine


def setup_render():
    scene = bpy.context.scene
    eng = pick_engine(scene)
    scene.render.resolution_x = 360
    scene.render.resolution_y = 360
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    if eng.startswith("BLENDER"):
        try:
            scene.eevee.taa_render_samples = 32
        except Exception:
            pass
    return eng


def centre_and_radius():
    lo = Vector((1e9, 1e9, 1e9))
    hi = Vector((-1e9, -1e9, -1e9))
    for _, mn, mx, _ in BOXES:
        a = to_blender(mn)
        b = to_blender(mx)
        lo = Vector((min(lo[i], a[i]) for i in range(3)))
        hi = Vector((max(hi[i], b[i]) for i in range(3)))
    return (lo + hi) / 2.0, max(hi - lo)


def render_angles(cam, centre, radius):
    os.makedirs(RENDER, exist_ok=True)
    # yaw(deg), pitch(deg), label
    angles = [
        (35, 24, "hero"),
        (125, 22, "back"),
        (0, 12, "front"),
        (55, 62, "top"),
    ]
    r = radius * 3.2
    paths = []
    for yaw, pitch, label in angles:
        yr = math.radians(yaw)
        pr = math.radians(pitch)
        x = centre.x + r * math.cos(pr) * math.sin(yr)
        y = centre.y - r * math.cos(pr) * math.cos(yr)
        z = centre.z + r * math.sin(pr)
        cam.location = Vector((x, y, z))
        bpy.context.view_layer.update()
        path = os.path.join(RENDER, f"{label}.png")
        bpy.context.scene.render.filepath = path
        bpy.ops.render.render(write_still=True)
        paths.append(path)
        print("rendered", path)
    return paths


def write_mc_model(tier):
    tex = {
        "particle": f"multihooker:block/{tier}_crate_side",
        "side": f"multihooker:block/{tier}_crate_side",
        "front": f"multihooker:block/{tier}_crate_front",
        "top": f"multihooker:block/{tier}_crate_top",
    }
    elements = []
    for name, mn, mx, _ in BOXES:
        faces = {}
        for face, slot in FACE_TEX[name].items():
            uv = UV_OVERRIDE.get((name, face), [0, 0, TEX, TEX])
            faces[face] = {"uv": uv, "texture": f"#{slot}"}
        elements.append({"from": list(mn), "to": list(mx), "faces": faces})
    model = {
        "credit": "Authored in Blender by tools/crategen/build_crate.py",
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": tex,
        "elements": elements,
    }
    os.makedirs(RES, exist_ok=True)
    path = os.path.join(RES, f"{tier}_crate.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(model, fh, indent=2)
    print("wrote model", os.path.abspath(path))


def main():
    argv = sys.argv
    tier = argv[argv.index("--") + 1] if "--" in argv else "divine"
    clean()
    setup_world()
    imgs = load_images(tier)
    mats = {slot: make_material(f"{tier}_{slot}", img) for slot, img in imgs.items()}
    for name, mn, mx, slot in BOXES:
        add_box(name, mn, mx, mats)
    setup_lights()
    centre, extents = centre_and_radius()
    cam = setup_camera(centre, extents)
    eng = setup_render()
    print("engine:", eng)
    render_angles(cam, centre, extents)
    write_mc_model(tier)


if __name__ == "__main__":
    main()
