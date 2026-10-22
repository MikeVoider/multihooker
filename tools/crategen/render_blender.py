"""True 3D renders of the Trove models, straight from the shipped model JSONs.

make_model_preview.py draws the block models with an orthographic camera and flat
per-face shading. That is the right tool for checking geometry and UVs, but it
reads as an isometric sticker rather than an object: no perspective, no cast
shadow, no environment. This script renders the same models in Blender with a
perspective camera, a sky-dominant sun and a ground plane to catch the shadow,
which is what makes a crate look like an object sitting in a world.

Nothing here writes into src/main/resources. The model JSONs and textures are
read only, and every render lands in tools/crategen/preview/blender. That matters
because build_crate.py - the other Blender script in this folder - writes model
JSONs back into the mod, and re-running it would overwrite the shipped geometry.

    blender --background --python render_blender.py -- [tier ...]

Renders each tier shut; pass --open to render the lid hinged up as well.
"""

import json
import math
import os
import sys

import bpy
import bmesh
import numpy as np
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
MODELS = os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", "models", "block")
TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "multihooker", "textures", "block")
OUT = os.path.join(HERE, "preview", "blender")

RES = 520
RES_TURN = 760                # the turntable is the proof shot, so it gets more pixels

# How hard the bump map pushes. DETAIL_GAIN is the high-pass multiplier that
# carves out fine relief - the engraved tier name above all - while FORM_SHARE is
# the small contribution of the broad luminance that keeps the planks and metal
# bands from going dead flat. At 16x16 a letter is four or five pixels, so the
# detail term needs to be strong to read at all.
DETAIL_GAIN = 9.0
FORM_SHARE = 0.45
BUMP_STRENGTH = 1.0
BUMP_DISTANCE = 0.020
SAMPLES = 96

# A block spans 0..1 in Minecraft's x/y/z, and to_blender negates z, so the crate
# occupies y -1..0 once mapped. The camera has to aim at the mapped centre, not
# the model-space one.
CENTRE = (0.5, -0.5, 0.5)

# Minecraft lights a block from the sky above, so the sun sits high and only
# slightly off-axis; a big soft fill from the camera side keeps the shaded faces
# readable without flattening them.
SUN_ANGLE = math.radians(18.0)


# ------------------------------------------------------------ model JSON ----
def corners(face, x1, y1, z1, x2, y2, z2):
    """Face corners as (mc_point, uv), ordered top-left, top-right, bottom-right,
    bottom-left seen from outside - the same layout Minecraft uses."""
    return {
        "up": [((x1, y2, z1), (0, 0)), ((x2, y2, z1), (1, 0)), ((x2, y2, z2), (1, 1)), ((x1, y2, z2), (0, 1))],
        "down": [((x1, y1, z1), (0, 1)), ((x2, y1, z1), (1, 1)), ((x2, y1, z2), (1, 0)), ((x1, y1, z2), (0, 0))],
        "north": [((x1, y2, z1), (1, 0)), ((x2, y2, z1), (0, 0)), ((x2, y1, z1), (0, 1)), ((x1, y1, z1), (1, 1))],
        "south": [((x1, y2, z2), (0, 0)), ((x2, y2, z2), (1, 0)), ((x2, y1, z2), (1, 1)), ((x1, y1, z2), (0, 1))],
        "west": [((x1, y2, z1), (0, 0)), ((x1, y2, z2), (1, 0)), ((x1, y1, z2), (1, 1)), ((x1, y1, z1), (0, 1))],
        "east": [((x2, y2, z1), (1, 0)), ((x2, y2, z2), (0, 0)), ((x2, y1, z2), (1, 1)), ((x2, y1, z1), (0, 1))],
    }[face]


def to_blender(point):
    """MC (x right, y up, z south) -> Blender (x, y, z), scaled to 1 unit/block.

    Blender's -Y is front, so z maps negated; this keeps the mapping right-handed
    and stops the render coming out mirrored.
    """
    x, y, z = point
    return (x / 16.0, -z / 16.0, y / 16.0)


def rotate_x(point, degrees, pivot_y, pivot_z):
    rad = math.radians(degrees)
    cos, sin = math.cos(rad), math.sin(rad)
    x, y, z = point
    dy, dz = y - pivot_y, z - pivot_z
    return (x, pivot_y + dy * cos - dz * sin, pivot_z + dy * sin + dz * cos)


def faces_of(model, rotation=None):
    """Flatten a model JSON into faces carrying mc-space corners and uv.

    A face names its texture as "#side"; the model's own textures map turns that
    into "multihooker:block/common_crate_side". Both hops matter - skipping the
    map leaves the renderer looking for a literal side.png.
    """
    texture_map = model.get("textures", {})
    out = []
    for element in model["elements"]:
        x1, y1, z1 = element["from"]
        x2, y2, z2 = element["to"]
        for face, spec in element["faces"].items():
            quad = corners(face, x1, y1, z1, x2, y2, z2)
            if rotation is not None:
                degrees, py, pz = rotation
                quad = [(rotate_x(p, degrees, py, pz), uv) for p, uv in quad]
            name = texture_map.get(spec["texture"].lstrip("#"), spec["texture"])
            out.append(
                {
                    "quad": quad,
                    "uv": spec.get("uv", [0, 0, 16, 16]),
                    "texture": name.split("/")[-1] + ".png",
                }
            )
    return out


# --------------------------------------------------------------- shading ----
_MATERIALS = {}


def _box_blur(array, radius):
    """Separable box blur via summed-area table, edges clamped."""
    padded = np.pad(array, radius, mode="edge")
    integral = np.cumsum(np.cumsum(padded, axis=0), axis=1)
    integral = np.pad(integral, ((1, 0), (1, 0)), mode="constant")
    span = 2 * radius + 1
    total = (
        integral[span:, span:]
        - integral[:-span, span:]
        - integral[span:, :-span]
        + integral[:-span, :-span]
    )
    return total[: array.shape[0], : array.shape[1]] / float(span * span)


def _height_field(name, source):
    """A bump map derived from the texture's own luminance.

    The crate art already encodes depth in its shading: the tier name is cut into
    the wood with a dark groove and a light lip, the rivets have a lit top edge,
    the barnacles are rounded. All of that is flat colour to a renderer, so the
    engraved name reads as a sticker. Feeding luminance back as height makes those
    pixels catch the same light the art was painted for - the lip rises, the
    groove sinks - and the face stops looking painted on.

    The height field is a high-pass, not raw luminance. Raw luminance is dominated
    by the broad plank and metal areas, so fine relief - the letters most of all -
    only sits a little above the local average and barely registers. Subtracting a
    blurred copy isolates the detail that is actually meant to be carved, while a
    small share of the overall luminance keeps the large forms from flattening out.

    Derived from the texture rather than authored, so it can never disagree with
    the colour map. Minecraft itself ignores bump maps, so this changes the
    preview only; what it shows is how the texture would react to a shader.
    """
    width, height = source.size
    pixels = np.empty(width * height * 4, dtype=np.float32)
    source.pixels.foreach_get(pixels)
    pixels = pixels.reshape(height, width, 4)

    luminance = pixels[:, :, :3].mean(axis=2)
    broad = _box_blur(luminance, 2)
    detail = luminance - broad
    relief = detail * DETAIL_GAIN + (luminance - luminance.mean()) * FORM_SHARE

    out = np.ones_like(pixels)
    out[:, :, 0] = out[:, :, 1] = out[:, :, 2] = np.clip(relief + 0.5, 0.0, 1.0)

    bump = bpy.data.images.new(name + "_bump", width, height, alpha=False)
    bump.pixels.foreach_set(out.ravel())
    bump.pack()
    return bump


def material_for(name):
    """One flat material per texture, cached across the whole run.

    Nearest filtering and no mipmaps matter: at 16x16 a filtered sample turns the
    rivets and the engraved tier name to mush, which would misrepresent the art.
    """
    if name in _MATERIALS:
        return _MATERIALS[name]
    path = os.path.join(TEXTURES, name)
    image = bpy.data.images.load(path, check_existing=True)
    image.colorspace_settings.name = "sRGB"
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    bsdf = nt.nodes["Principled BSDF"]
    bsdf.inputs["Roughness"].default_value = 0.92
    if "Specular IOR Level" in bsdf.inputs:
        bsdf.inputs["Specular IOR Level"].default_value = 0.25
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = image
    tex.interpolation = "Closest"
    tex.extension = "EXTEND"
    tex.location = (-620, 60)
    nt.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    if "Alpha" in bsdf.inputs:
        nt.links.new(tex.outputs["Alpha"], bsdf.inputs["Alpha"])

    try:
        bump_tex = nt.nodes.new("ShaderNodeTexImage")
        bump_tex.image = _height_field(name, image)
        bump_tex.interpolation = "Closest"
        bump_tex.location = (-620, -260)
        bump_node = nt.nodes.new("ShaderNodeBump")
        bump_node.inputs["Strength"].default_value = BUMP_STRENGTH
        bump_node.inputs["Distance"].default_value = BUMP_DISTANCE
        bump_node.location = (-300, -260)
        nt.links.new(bump_tex.outputs["Color"], bump_node.inputs["Height"])
        nt.links.new(bump_node.outputs["Normal"], bsdf.inputs["Normal"])
    except Exception as exc:  # numpy missing, or an image with no pixel buffer
        print("  no bump map for %s (%s)" % (name, exc))

    # Minecraft's cutout blocks are opaque or gone - never a soft edge.
    if hasattr(mat, "surface_render_method"):
        mat.surface_render_method = "DITHERED"
    _MATERIALS[name] = mat
    return mat


def build(name, parts):
    """One mesh object for the whole crate: a quad per face, uv'd per the JSON.

    Minecraft's v runs top-down while Blender's runs bottom-up, so v is flipped
    here rather than by touching the textures. UVs and material indices are set on
    the bmesh, where each face's loops are still in the order the quad was passed
    in - doing it after to_mesh would mean trusting face/loop ordering.
    """
    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    uv_layer = bm.loops.layers.uv.new("UVMap")
    mat_names = []

    for face in parts:
        verts = [bm.verts.new(to_blender(point)) for point, _ in face["quad"]]
        f = bm.faces.new(verts)
        f.material_index = len(mat_names)
        mat_names.append(face["texture"])
        fu1, fv1, fu2, fv2 = face["uv"]
        for loop, (_, uvr) in zip(f.loops, face["quad"]):
            u = (fu1 + uvr[0] * (fu2 - fu1)) / 16.0
            v = (fv1 + uvr[1] * (fv2 - fv1)) / 16.0
            loop[uv_layer].uv = (u, 1.0 - v)

    bm.normal_update()
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    bm.to_mesh(mesh)
    bm.free()

    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    for texture in mat_names:
        mesh.materials.append(material_for(texture))
    return obj


def setup_world():
    world = bpy.data.worlds.new("Sky")
    bpy.context.scene.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    bg.inputs[0].default_value = (0.055, 0.065, 0.085, 1.0)
    bg.inputs[1].default_value = 1.0


def setup_lights():
    sun_data = bpy.data.lights.new("Sun", type="SUN")
    sun_data.energy = 4.2
    sun_data.angle = SUN_ANGLE
    sun = bpy.data.objects.new("Sun", sun_data)
    bpy.context.collection.objects.link(sun)
    sun.rotation_euler = (math.radians(38.0), 0.0, math.radians(-28.0))

    fill_data = bpy.data.lights.new("Fill", type="AREA")
    fill_data.energy = 90.0
    fill_data.size = 3.0
    fill = bpy.data.objects.new("Fill", fill_data)
    bpy.context.collection.objects.link(fill)
    fill.location = (2.4, -2.6, 2.2)
    fill.rotation_euler = (math.radians(52.0), 0.0, math.radians(42.0))


def setup_ground():
    """A dark plane just under the crate so it has something to cast onto."""
    bpy.ops.mesh.primitive_plane_add(size=14.0, location=(0.0, 0.0, 0.0))
    ground = bpy.context.active_object
    ground.name = "Ground"
    mat = bpy.data.materials.new("Ground")
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (0.055, 0.058, 0.07, 1.0)
    bsdf.inputs["Roughness"].default_value = 1.0
    ground.data.materials.append(mat)


def scene_bounds(objs):
    """Centre and size of everything just built, in world units.

    Framing from a fixed distance only works for one silhouette. The opened crate
    stands nearly twice as tall as the shut one once the lid is up, so the camera
    distance is derived from the real bounds instead of being a constant.
    """
    pts = []
    for obj in objs:
        for corner in obj.bound_box:
            pts.append(obj.matrix_world @ Vector(corner))
    xs, ys, zs = [p.x for p in pts], [p.y for p in pts], [p.z for p in pts]
    centre = Vector(((min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, (min(zs) + max(zs)) / 2))
    size = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs))
    return centre, size


def setup_camera(objs, yaw_deg, pitch_deg, lens=62.0, margin=1.22):
    target, size = scene_bounds(objs)

    cam_data = bpy.data.cameras.new("Cam")
    cam_data.lens = lens
    cam = bpy.data.objects.new("Cam", cam_data)
    bpy.context.collection.objects.link(cam)
    bpy.context.scene.camera = cam

    # pull back far enough that the bounding sphere fits the narrower of the two
    # field-of-view axes, which for a square render is the vertical one
    half_fov = math.atan((cam_data.sensor_width / 2.0) / lens)
    dist = ((size / 2.0) * margin) / math.tan(half_fov)

    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    cam.location = (
        target.x + dist * math.cos(pitch) * math.sin(yaw),
        target.y - dist * math.cos(pitch) * math.cos(yaw),
        target.z + dist * math.sin(pitch),
    )
    # A camera looks down its own -Z, so aim that axis at the subject. Working
    # this out from Euler angles by hand is easy to get subtly wrong and silently
    # renders the floor; to_track_quat is the one that cannot.
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()

    # Park the fill just behind the camera. Without this the far side of a turntable
    # sits in shadow and you cannot read it - which is exactly the case that matters
    # when checking whether the crate's rear text is mirrored.
    fill = bpy.data.objects.get("Fill")
    if fill is not None:
        offset = (cam.location - target).normalized()
        fill.location = target + offset * 3.0 + Vector((0.0, 0.0, 1.2))
        fill.rotation_euler = (target - fill.location).to_track_quat("-Z", "Y").to_euler()
    return target


def setup_engine(res=None):
    scene = bpy.context.scene
    for engine in ("BLENDER_EEVEE_NEXT", "BLENDER_EEVEE"):
        try:
            scene.render.engine = engine
            break
        except TypeError:
            continue
    try:
        scene.eevee.taa_render_samples = SAMPLES
        scene.eevee.use_shadows = True
    except AttributeError:
        pass
    size = res or RES
    scene.render.resolution_x = size
    scene.render.resolution_y = size
    scene.render.film_transparent = False
    scene.render.image_settings.file_format = "PNG"
    try:
        scene.view_settings.view_transform = "AgX"
        scene.view_settings.look = "AgX - Medium Contrast"
    except TypeError:
        pass


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete()


def render_to(objs, path, yaw, pitch, margin=1.22):
    setup_camera(objs, yaw, pitch, margin=margin)
    bpy.context.scene.render.filepath = path
    bpy.ops.render.render(write_still=True)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    want_open = "--open" in argv
    turntable = None
    if "--turntable" in argv:
        i = argv.index("--turntable")
        turntable = int(argv[i + 1]) if len(argv) > i + 1 else 6
    # Keep only the tier names, dropping flags and the number after --turntable.
    # Filtering on "isalpha" or "no underscore" would silently drop ultra_divine,
    # mythic, unspoken and ghost_unspoken - four of the twelve - and leave them
    # to fall back to the flat renderer with no error anywhere.
    keys = []
    skip_next = False
    for arg in argv:
        if skip_next:
            skip_next = False
            continue
        if arg == "--turntable":
            skip_next = True
            continue
        if arg.startswith("--"):
            continue
        keys.append(arg)
    if not keys:
        keys = ["common"]
    os.makedirs(OUT, exist_ok=True)

    for key in keys:
        clear_scene()
        setup_world()
        setup_lights()
        setup_ground()
        setup_engine()

        with open(os.path.join(MODELS, f"{key}_crate.json"), encoding="utf-8") as handle:
            shut = json.load(handle)

        if turntable:
            # The same crate walked all the way round. A flat or sprite-based
            # renderer cannot produce this: the silhouette and the visible faces
            # change continuously, which is the whole point of the check.
            setup_engine(res=RES_TURN)
            obj = build(f"{key}_shut", faces_of(shut))
            step = 360.0 / turntable
            for n in range(turntable):
                render_to([obj], os.path.join(OUT, f"{key}_turn_{n:02d}.png"),
                          n * step, 24.0)
                print("  turn %d/%d" % (n + 1, turntable))
            print("rendered", key)
            continue

        render_to([build(f"{key}_shut", faces_of(shut))],
                  os.path.join(OUT, f"{key}_shut.png"), 34.0, 22.0, margin=1.52)

        if want_open:
            with open(os.path.join(MODELS, f"{key}_crate_body.json"), encoding="utf-8") as handle:
                body = json.load(handle)
            with open(os.path.join(MODELS, f"{key}_crate_lid.json"), encoding="utf-8") as handle:
                lid = json.load(handle)
            parts = [build(f"{key}_body", faces_of(body))]
            # A quarter turn about the crate's north-bottom rim - the angle
            # CrateLidRenderer animates (Axis.XP.rotationDegrees(-angle * 90)).
            # Both the sign and the pivot matter: the wrong pivot leaves the lid
            # swung the other way, showing its plank top to the camera instead of
            # the dark inside, which is exactly what it did the first time.
            parts.append(build(f"{key}_lid", faces_of(lid, rotation=(-90.0, 12.0, 0.5))))
            render_to(parts, os.path.join(OUT, f"{key}_open.png"), 34.0, 14.0, margin=1.10)

        print("rendered", key)


if __name__ == "__main__":
    main()