"""Build pixel-art character frames from a bone rig and pose data.

Usage: python3 tools/sprites/build_character_sprites.py [rig.json ...]
(default: tools/sprites/hero_rig.json)

The rig JSON describes bone lengths, part colours (palette ramps), key poses and
animations. Every frame is drawn facing right on a strict pixel grid, shaded
with three ramp steps under one top-front light, outlined by one outer pixel
and packed into a deterministic libGDX atlas. Only the Python 3 standard
library is used, so the build runs without Pillow. See documents/ART_ASSET_GUIDE.md.
"""

import json
import math
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PALETTE = ROOT / "assets/art/ruins-palette.json"


# ---------------------------------------------------------------- PNG output

def write_png(path, width, height, pixels):
    """pixels: list of rows of (r, g, b, a)."""
    raw = b"".join(b"\0" + bytes(v for px in row for v in px) for row in pixels)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data +
                struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" +
                     chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) +
                     chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def hex_rgba(value):
    return (int(value[1:3], 16), int(value[3:5], 16), int(value[5:7], 16), 255)


# ---------------------------------------------------------------- geometry

def rot(angle_from_down, length, forward=1):
    """Vector of a bone measured from straight down; positive angles swing forward (+x)."""
    a = math.radians(angle_from_down)
    return (math.sin(a) * length * forward, -math.cos(a) * length)


def add(a, b):
    return (a[0] + b[0], a[1] + b[1])


def lerp(a, b, t):
    return a + (b - a) * t


def two_bone(shoulder, target, l1, l2, bend_sign):
    dx, dy = target[0] - shoulder[0], target[1] - shoulder[1]
    d = max(.01, min(math.hypot(dx, dy), l1 + l2 - .3))
    base = math.atan2(dy, dx)
    a = math.acos(max(-1., min(1., (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d))))
    elbow = (shoulder[0] + math.cos(base + bend_sign * a) * l1,
             shoulder[1] + math.sin(base + bend_sign * a) * l1)
    hand = (shoulder[0] + math.cos(base) * d, shoulder[1] + math.sin(base) * d)
    return elbow, hand


class Canvas:
    """Part-id raster. Later draws cover earlier ones (painter's order)."""

    def __init__(self, width, height, anchor):
        self.w, self.h = width, height
        self.ax, self.ay = anchor
        self.part = [[None] * width for _ in range(height)]
        self.parts = []          # (ramp, far)
        self.fx = {}             # (x, y) -> colour drawn after outlining

    def to_px(self, p):
        return self.ax + p[0], self.ay - p[1]

    def new_part(self, ramp, far=False):
        self.parts.append((ramp, far))
        return len(self.parts) - 1

    def plot(self, x, y, pid):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.part[y][x] = pid

    def disc(self, c, r, pid):
        cx, cy = self.to_px(c)
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r:
                    self.plot(x, y, pid)

    def capsule(self, a, b, r0, pid, r1=None):
        r1 = r0 if r1 is None else r1
        steps = max(2, int(math.hypot(b[0] - a[0], b[1] - a[1]) * 2) + 1)
        for i in range(steps + 1):
            t = i / steps
            self.disc((lerp(a[0], b[0], t), lerp(a[1], b[1], t)), lerp(r0, r1, t), pid)

    def poly(self, pts, pid):
        px = [self.to_px(p) for p in pts]
        ys = [p[1] for p in px]
        for y in range(int(min(ys)), int(max(ys)) + 2):
            yc = y + .5
            xs = []
            for i in range(len(px)):
                (x0, y0), (x1, y1) = px[i], px[(i + 1) % len(px)]
                if (y0 <= yc < y1) or (y1 <= yc < y0):
                    xs.append(x0 + (yc - y0) * (x1 - x0) / (y1 - y0))
            xs.sort()
            for j in range(0, len(xs) - 1, 2):
                for x in range(int(math.ceil(xs[j] - .5)), int(math.floor(xs[j + 1] - .5)) + 1):
                    self.plot(x, y, pid)

    def fx_arc(self, centre, r0, r1, a0, a1, colours):
        """Swing smear between radii r0..r1; the weapon travels from angle a0 to a1 (degrees, 0 = forward, CCW)."""
        cx, cy = self.to_px(centre)
        lo, hi = min(a0, a1), max(a0, a1)
        for y in range(self.h):
            for x in range(self.w):
                dx, dy = x + .5 - cx, cy - (y + .5)
                d = math.hypot(dx, dy)
                if not (r0 <= d <= r1):
                    continue
                ang = math.degrees(math.atan2(dy, dx))
                while ang < lo:
                    ang += 360
                if ang > hi:
                    continue
                # Thin smear: bright leading band at the weapon's radius, dithered fading tail.
                t = (ang - a0) / (a1 - a0) if a1 != a0 else 1
                edge = (d - r0) / max(.01, r1 - r0)
                if t < .55 and ((x + y) & 1 or t < .3 and (x & 1)):
                    continue
                if edge < .35 and t < .85:
                    continue
                self.fx[(x, y)] = colours[2 if t > .8 or edge > .8 else 1 if t > .55 else 0]

    def fx_line(self, a, b, colour):
        steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
        for i in range(steps + 1):
            t = i / steps
            x, y = self.to_px((lerp(a[0], b[0], t), lerp(a[1], b[1], t)))
            self.fx[(int(x), int(y))] = colour

    def render(self, ramps, outline):
        out = [[(0, 0, 0, 0)] * self.w for _ in range(self.h)]
        part = self.part
        for y in range(self.h):
            for x in range(self.w):
                pid = part[y][x]
                if pid is None:
                    continue
                ramp_name, far = self.parts[pid]
                ramp = ramps[ramp_name]
                up = part[y - 1][x] if y > 0 else None
                down = part[y + 1][x] if y + 1 < self.h else None
                back = part[y][x - 1] if x > 0 else None
                step = 1
                if up != pid:
                    step = 2
                elif down != pid or back != pid and back is not None and self.parts[back][1] != far:
                    step = 0
                if far:
                    step = max(0, step - 1)
                out[y][x] = ramp[min(step, len(ramp) - 1)]
        # One outer outline pixel (4-neighbourhood); never inside the silhouette.
        filled = [[part[y][x] is not None for x in range(self.w)] for y in range(self.h)]
        for y in range(self.h):
            for x in range(self.w):
                if filled[y][x]:
                    continue
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if 0 <= nx < self.w and 0 <= ny < self.h and filled[ny][nx]:
                        out[y][x] = outline
                        break
        for (x, y), colour in self.fx.items():
            if 0 <= x < self.w and 0 <= y < self.h and part[y][x] is None:
                out[y][x] = colour
        return out


# ---------------------------------------------------------------- rig

POSE_KEYS = ("hipX", "hipY", "lean", "head", "turn", "weapon", "grip", "coat", "scarf")
PAIR_KEYS = ("armF", "armB", "legF", "legB")


def blend(a, b, t):
    pose = {}
    for k in set(a) | set(b):
        va, vb = a.get(k), b.get(k)
        if va is None or vb is None:
            pose[k] = va if vb is None else vb if t >= .5 or va is None else va
        elif isinstance(va, list):
            pose[k] = [lerp(x, y, t) for x, y in zip(va, vb)]
        elif isinstance(va, (int, float)) and not isinstance(va, bool):
            pose[k] = lerp(va, vb, t)
        else:
            pose[k] = va if t < .5 else vb
    return pose


def resolve(rig, ref):
    """A pose is a name, or {"base": name, ...overrides}."""
    if isinstance(ref, str):
        ref = rig["poses"][ref]
    pose = dict(rig["defaultPose"])
    if "base" in ref:
        pose.update(resolve(rig, ref["base"]))
    pose.update({k: v for k, v in ref.items() if k != "base"})
    return pose


def draw_figure(rig, pose, canvas):
    b = rig["bones"]
    parts = rig["parts"]
    turn = pose.get("turn", 0)          # 0 = side view, 1 = seen from behind
    squeeze = 1 - turn * .75
    hip = (pose["hipX"], pose["hipY"])
    neck = add(hip, (math.sin(math.radians(pose["lean"])) * b["torso"] * squeeze,
                     math.cos(math.radians(pose["lean"])) * b["torso"]))
    head = add(neck, (math.sin(math.radians(pose["lean"] + pose["head"])) * b["neck"] * squeeze,
                      math.cos(math.radians(pose["lean"] + pose["head"])) * b["neck"] + b["headRadius"] - 1))
    spread = turn * b["hipWidth"]

    def leg(key, side):
        thigh, bend = pose[key]
        root = add(hip, (side * spread, 0))
        knee = add(root, tuple(c * (squeeze if i == 0 else 1)
                               for i, c in enumerate(rot(thigh, b["thigh"]))))
        foot = add(knee, tuple(c * (squeeze if i == 0 else 1)
                               for i, c in enumerate(rot(thigh - bend, b["shin"]))))
        return root, knee, foot

    def arm(key, side):
        upper, bend = pose[key]
        shoulder = add(neck, (side * spread * 1.3 - 1, -2))
        elbow = add(shoulder, tuple(c * (squeeze if i == 0 else 1)
                                    for i, c in enumerate(rot(upper, b["upperArm"]))))
        hand = add(elbow, tuple(c * (squeeze if i == 0 else 1)
                                for i, c in enumerate(rot(upper + bend, b["forearm"]))))
        return shoulder, elbow, hand

    legF, legB = leg("legF", 1), leg("legB", -1)
    shoulderF, elbowF, handF = arm("armF", 1)
    shoulderB, elbowB, handB = arm("armB", -1)

    weapon = None
    if pose.get("weapon") is not None:
        wa = math.radians(pose["weapon"])
        length = b["weapon"]
        direction = (math.cos(wa) * squeeze, math.sin(wa))
        grip = pose.get("grip", .4)
        butt = (handF[0] - direction[0] * length * grip, handF[1] - direction[1] * length * grip)
        tip = (butt[0] + direction[0] * length, butt[1] + direction[1] * length)
        weapon = (butt, tip, direction)
        if pose.get("twoHand") is not None:
            target = (butt[0] + direction[0] * length * pose["twoHand"],
                      butt[1] + direction[1] * length * pose["twoHand"])
            elbowB, handB = two_bone(shoulderB, target, b["upperArm"], b["forearm"], -1)

    def limb_arm(shoulder, elbow, hand, far):
        cid = canvas.new_part(parts["sleeve"], far)
        canvas.capsule(shoulder, elbow, b["armRadius"], cid)
        canvas.capsule(elbow, hand, b["armRadius"] - .4, cid, b["armRadius"] - .7)
        canvas.disc(hand, b["handRadius"], canvas.new_part(parts["hand"], far))

    def limb_leg(root, knee, foot, far):
        canvas.capsule(root, knee, b["legRadius"], canvas.new_part(parts["leg"], far), b["legRadius"] - .4)
        boot = canvas.new_part(parts["boot"], far)
        mid = (lerp(knee[0], foot[0], .45), lerp(knee[1], foot[1], .45))
        canvas.capsule(knee, mid, b["legRadius"] - .6, canvas.new_part(parts["leg"], far))
        canvas.capsule(mid, foot, b["legRadius"] - .4, boot)
        toe = add(foot, (b["foot"] * squeeze, 0))
        canvas.capsule(foot, toe, 1.6, boot)

    def draw_weapon():
        if weapon is None:
            return
        butt, tip, direction = weapon
        blade = (tip[0] - direction[0] * b["blade"], tip[1] - direction[1] * b["blade"])
        canvas.capsule(butt, blade, .9, canvas.new_part(parts["shaft"]))
        canvas.capsule(blade, tip, 1.6, canvas.new_part(parts["blade"]), .6)
        wrap = (lerp(blade[0], butt[0], .08), lerp(blade[1], butt[1], .08))
        canvas.capsule(blade, wrap, 1.3, canvas.new_part(parts["wrap"]))

    back_view = turn > .55
    # Back to front: far arm, far leg, scarf tail, coat skirt, torso, near leg, head, near arm, weapon.
    if not back_view:
        limb_arm(shoulderB, elbowB, handB, True)
    limb_leg(*legB, True)

    scarf_angle = math.radians(pose.get("scarf", 200))
    tail = add(neck, (math.cos(scarf_angle) * b["scarfTail"] * squeeze, math.sin(scarf_angle) * b["scarfTail"]))
    canvas.capsule(add(neck, (-1, -1)), tail, 2.2, canvas.new_part(parts["scarf"]), 1.2)

    limb_leg(*legF, False)

    flare = pose.get("coat", 0)
    lean = math.radians(pose["lean"])
    across = (math.cos(lean) * squeeze, -math.sin(lean))
    hw = b["torsoWidth"] / 2 * (1 + turn * .3)
    hem_y = hip[1] - b["coatLength"]
    coat = canvas.new_part(parts["coat"])
    canvas.poly([
        add(hip, (-across[0] * (hw + 1), -across[1] * (hw + 1))),
        add(hip, (across[0] * hw, across[1] * hw)),
        (hip[0] + hw * .8 + flare * .2, hem_y + 2),
        (hip[0] - hw - 2 - flare, hem_y + flare * .35),
    ], coat)
    canvas.poly([
        add(hip, (-across[0] * hw, -across[1] * hw)),
        add(hip, (across[0] * hw, across[1] * hw)),
        add(neck, (across[0] * (hw + .5), across[1] * (hw + .5))),
        add(neck, (-across[0] * (hw + .5), -across[1] * (hw + .5))),
    ], coat)
    canvas.capsule(add(hip, (-across[0] * hw, 1)), add(hip, (across[0] * hw, 1)), 1.1,
                   canvas.new_part(parts["belt"]))

    hr = b["headRadius"]
    hood = canvas.new_part(parts["hood"])
    canvas.disc(add(head, (-1.2 * squeeze, .6)), hr + .8, hood)
    if not back_view:
        face = canvas.new_part(parts["face"])
        canvas.disc(add(head, (1.6 * squeeze, -.6)), hr - 1.6, face)
        canvas.capsule(add(neck, (-hw * .6, .5)), add(neck, (hw * .9, 1)), 2.2, canvas.new_part(parts["scarf"]))
        ex, ey = canvas.to_px(add(head, (3.2 * squeeze, .2)))
        canvas.fx[(int(ex), int(ey))] = None     # placeholder, eye is drawn after render
        canvas.eye = (int(ex), int(ey))
    else:
        canvas.capsule(add(neck, (-hw, .5)), add(neck, (hw, .5)), 2.2, canvas.new_part(parts["scarf"]))
        canvas.eye = None

    if back_view:
        limb_arm(shoulderB, elbowB, handB, True)
    draw_weapon()
    limb_arm(shoulderF, elbowF, handF, False)
    if pose.get("twoHand") is not None and not back_view:
        canvas.disc(handB, b["handRadius"], canvas.new_part(parts["hand"]))

    tip_px = canvas.to_px(weapon[1]) if weapon else canvas.to_px(handF)
    return {"hip": hip, "tip": weapon[1] if weapon else handF, "tipPx": tip_px}


def apply_fx(rig, pose, canvas, joints, ramps):
    for fx in pose.get("fx", []):
        colours = [ramps[fx.get("ramp", "steel")][i] for i in (1, 2, 3)]
        centre = add(joints["hip"], tuple(fx.get("centre", (0, 12))))
        if fx["type"] == "arc":
            canvas.fx_arc(centre, fx["r0"], fx["r1"], fx["from"], fx["to"], colours)
        elif fx["type"] == "streak":
            # Thrust streak: parallel lines behind the weapon tip.
            tip = joints["tip"]
            for k, off in enumerate(fx.get("offsets", (-2, 0, 2))):
                canvas.fx_line((tip[0] - fx["length"], tip[1] + off), (tip[0] - 3, tip[1] + off * .5),
                               colours[2 if off == 0 else 0])
        elif fx["type"] == "dust":
            for i, (dx, dy) in enumerate(((-6, 0), (-10, 1), (-13, 0), (-8, 2), (6, 0), (10, 1))[:fx.get("count", 4)]):
                x, y = canvas.to_px((joints["hip"][0] * 0 + dx * fx.get("side", 1), dy))
                canvas.fx[(int(x), int(y) - 1)] = ramps["env_ash"][1 + (i & 1)]
                canvas.fx[(int(x) + 1, int(y) - 1)] = ramps["env_ash"][0]


# ---------------------------------------------------------------- build

def frames_for(rig, anim):
    keys = [resolve(rig, k) for k in anim["keys"]]
    tween = anim.get("tween", 0)
    frames = []
    count = len(keys)
    loop = anim.get("loop", False)
    for i, key in enumerate(keys):
        frames.append(key)
        if i + 1 < count or loop:
            nxt = keys[(i + 1) % count]
            for t in range(1, tween + 1):
                mid = blend(key, nxt, t / (tween + 1))
                mid.pop("fx", None)
                frames.append(mid)
    return frames


def build(rig_path):
    rig = json.loads(Path(rig_path).read_text(encoding="utf-8"))
    palette = json.loads(PALETTE.read_text(encoding="utf-8"))["ramps"]
    ramps = {k: [hex_rgba(c) for c in v] for k, v in palette.items()}
    allowed = {c for r in ramps.values() for c in r}
    outline = ramps["outline"][0]
    fw, fh = rig["frame"]
    anchor = rig["anchor"]
    eye_colour = ramps["outline"][0]

    rendered = []          # (anim, index, pixels, tip)
    meta = {"version": 1, "sprite": rig["name"], "frame": [fw, fh], "anchor": anchor,
            "pixelsPerUnit": rig["pixelsPerUnit"], "facing": "right", "animations": {}}
    for name, anim in rig["animations"].items():
        poses = frames_for(rig, anim)
        tips = []
        for index, pose in enumerate(poses):
            canvas = Canvas(fw, fh, anchor)
            joints = draw_figure(rig, pose, canvas)
            canvas.fx = {k: v for k, v in canvas.fx.items() if v is not None}
            apply_fx(rig, pose, canvas, joints, ramps)
            pixels = canvas.render(ramps, outline)
            if canvas.eye:
                ex, ey = canvas.eye
                if 0 <= ex < fw and 0 <= ey < fh:
                    pixels[ey][ex] = eye_colour
            for row in pixels:
                for px in row:
                    if px[3] and px not in allowed:
                        raise ValueError(f"{name}[{index}] uses off-palette colour {px}")
            tx, ty = joints["tipPx"]
            tips.append([round(tx), round(ty)])
            rendered.append((name, index, pixels))
        entry = {"frames": len(poses), "frameDuration": anim.get("frameDuration", .083),
                 "loop": anim.get("loop", False), "weaponTip": tips}
        if "phases" in anim:
            entry["phases"] = expand_phases(anim, len(poses))
        if "notes" in anim:
            entry["notes"] = anim["notes"]
        meta["animations"][name] = entry

    out = ROOT / rig["output"]["dir"]
    pack(rendered, fw, fh, out / (rig["output"]["name"] + ".png"), out / (rig["output"]["name"] + ".atlas"))
    meta_path = ROOT / rig["output"]["metadata"]
    meta_path.write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    preview(rendered, fw, fh, ROOT / rig["output"]["preview"], rig)
    print(f"{rig['name']}: {len(rendered)} frames, {len(rig['animations'])} animations")


def expand_phases(anim, total):
    """Phases list key-frame counts; tween frames belong to the phase of the key before them."""
    tween = anim.get("tween", 0)
    out, start = {}, 0
    for phase, keys in anim["phases"].items():
        n = keys * (tween + 1)
        out[phase] = [start, min(total, start + n) - 1]
        start += n
    return out


def pack(rendered, fw, fh, png_path, atlas_path):
    pad = 2
    cols = max(1, 2048 // (fw + pad))
    rows = (len(rendered) + cols - 1) // cols
    width = 2048
    height = 1
    while height < rows * (fh + pad):
        height *= 2
    sheet = [[(0, 0, 0, 0)] * width for _ in range(height)]
    lines = ["", png_path.name, f"size: {width}, {height}", "format: RGBA8888",
             "filter: Nearest, Nearest", "repeat: none"]
    for i, (name, index, pixels) in enumerate(rendered):
        x0, y0 = (i % cols) * (fw + pad), (i // cols) * (fh + pad)
        for y in range(fh):
            sheet[y0 + y][x0:x0 + fw] = pixels[y]
        lines += [name, "  rotate: false", f"  xy: {x0}, {y0}", f"  size: {fw}, {fh}",
                  f"  orig: {fw}, {fh}", "  offset: 0, 0", f"  index: {index}"]
    write_png(png_path, width, height, sheet)
    atlas_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def preview(rendered, fw, fh, path, rig):
    """Contact sheet: one row per animation, 2x, on the road's dark soil colour."""
    scale = 2
    names = list(rig["animations"])
    longest = max(sum(1 for r in rendered if r[0] == n) for n in names)
    bg = hex_rgba("#2a1c20")
    ground = hex_rgba("#64505a")
    w, h = longest * fw * scale, len(names) * fh * scale
    img = [[bg] * w for _ in range(h)]
    for name, index, pixels in rendered:
        oy, ox = names.index(name) * fh * scale, index * fw * scale
        for y in range(fh):
            for x in range(fw):
                px = pixels[y][x]
                for sy in range(scale):
                    for sx in range(scale):
                        img[oy + y * scale + sy][ox + x * scale + sx] = px if px[3] else (
                            ground if y >= rig["anchor"][1] else bg)
    write_png(path, w, h, img)


if __name__ == "__main__":
    for rig_file in sys.argv[1:] or [Path(__file__).with_name("hero_rig.json")]:
        build(rig_file)
