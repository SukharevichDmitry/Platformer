"""Pack the approved ruins sheets into a deterministic libGDX atlas.

The source sheets remain alongside the shipped atlas. Additional exposed
faces and inside corners brighten the same painted bricks in each tile.
"""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/tiles/ruins-concepts-v1.png"
AUTOTILE_SOURCE = ROOT / "assets/tiles/ruins-autotile-v2.png"
ATLAS_IMAGE = ROOT / "assets/atlases/ruins-tiles.png"
ATLAS_FILE = ROOT / "assets/atlases/ruins-tiles.atlas"
NAMES = (
    "stone_a", "stone_b", "stone_c", "stone_chipped",
    "stone_moss", "stone_damp", "stone_top", "stone_right",
    "stone_left", "stone_corner", "stone_half", "stone_recess",
    "platform_oneway", "block_breakable", "ladder_segment", "ladder_cap",
)
CELL = 64
AUTOTILE_NAMES = (
    "wall_top_left", "wall_top", "wall_top_right",
    "wall_left", "wall_center", "wall_right",
    "wall_bottom_left", "wall_bottom", "wall_bottom_right",
)
WALL_VARIANT_NAMES = tuple(
    f"wall_mask_{faces}" if corners == 0 else f"wall_mask_{faces}_corner_{corners}"
    for corners in range(16) for faces in range(16)
)
ATLAS_COLUMNS = 16


def wall_faces() -> list[list[Image.Image]]:
    """Keep the original nine faces at one scale and preserve their hard pixels."""
    source = Image.open(AUTOTILE_SOURCE).convert("RGBA")
    if source.width % 3 or source.height % 3:
        raise ValueError("Ruins autotile art must be a three-by-three sheet")
    source_cell_x, source_cell_y = source.width // 3, source.height // 3
    faces = []
    for row in range(3):
        face_row = []
        for column in range(3):
            # Use one crop for every face. Cropping each tile to its separate
            # content bounds changed the apparent stone scale and edge position.
            left = column * source_cell_x + 12
            top = row * source_cell_y + 10
            tile = source.crop((left, top, left + source_cell_x - 24,
                                top + source_cell_y - 22))
            face_row.append(tile.resize((CELL, CELL), Image.Resampling.NEAREST))
        faces.append(face_row)
    return faces


def connect_right_face(tile: Image.Image, right_face: Image.Image) -> Image.Image:
    """Fit a second painted side into a narrow strip at the right edge."""
    alpha = Image.new("L", (CELL, CELL))
    pixels = alpha.load()
    for y in range(CELL):
        for x in range(CELL - 12, CELL):
            pixels[x, y] = min(255, round(255 * (x - (CELL - 13)) / 8))
    joined = Image.composite(right_face, tile, alpha)
    stones = joined.load()
    for y in range(CELL):
        for x in range(CELL - 8, CELL):
            r, g, b, a = stones[x, y]
            if max(r, g, b) < 30:
                continue
            factor = 1 + 0.30 * (x - (CELL - 9)) / 8
            stones[x, y] = (min(255, round(r * factor)),
                            min(255, round(g * factor)),
                            min(255, round(b * factor)), a)
    return joined


def connect_bottom_face(tile: Image.Image, bottom_face: Image.Image,
                        left: bool, right: bool) -> Image.Image:
    """Give a hanging run one continuous painted underside across its tiles."""
    alpha = Image.new("L", (CELL, CELL))
    pixels = alpha.load()
    for y in range(CELL - 10, CELL):
        for x in range(CELL):
            amount = min(1.0, (y - (CELL - 11)) / 8)
            if left:
                amount *= min(1.0, x / 9)
            if right:
                amount *= min(1.0, (CELL - 1 - x) / 9)
            pixels[x, y] = round(255 * amount)
    return Image.composite(bottom_face, tile, alpha)


def wall_mask_face(faces: list[list[Image.Image]], mask: int) -> Image.Image:
    """North/east/south/west bits select the actual lit stone face."""
    top, right, bottom, left = (bool(mask & bit) for bit in (1, 2, 4, 8))
    row = 0 if top or bottom else 1
    column = 0 if left else 2 if right else 1
    tile = faces[row][column].copy()
    if bottom and not top:
        tile = tile.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    if left and right:
        right_face = faces[row][2].copy()
        if bottom and not top:
            right_face = right_face.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        tile = connect_right_face(tile, right_face)
    if bottom and top:
        common_underside = faces[0][1].transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        tile = connect_bottom_face(tile, common_underside, left, right)
    return tile


def join_inside_corner(tile: Image.Image, faces: list[list[Image.Image]],
                       corner: int) -> Image.Image:
    """Fit the source sheet's painted corner stone into a three-stone joint."""
    right = corner in (0, 2)
    bottom = corner in (2, 3)
    painted = faces[0][2 if right else 0].copy()
    if bottom:
        painted = painted.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    alpha = Image.new("L", (CELL, CELL))
    pixels = alpha.load()
    for y in range(CELL):
        for x in range(CELL):
            dx = CELL - 1 - x if right else x
            dy = CELL - 1 - y if bottom else y
            distance = max(dx, dy)
            if distance < 8:
                pixels[x, y] = 220
            elif distance < 20:
                pixels[x, y] = round(220 * (20 - distance) / 12)
    return Image.composite(painted, tile, alpha)


def wall_variant_face(faces: list[list[Image.Image]], lighting: int) -> Image.Image:
    """Bake convex and adjoining inside corners into the selected face art."""
    exposed = lighting & 15
    inside = (lighting >> 4) & 15
    tile = wall_mask_face(faces, exposed)
    for corner in range(4):
        if inside & (1 << corner):
            tile = join_inside_corner(tile, faces, corner)
    top, right, bottom, left = (bool(exposed & bit) for bit in (1, 2, 4, 8))
    convex = (int(top and right) | int(top and left) << 1 |
              int(bottom and right) << 2 | int(bottom and left) << 3)
    strengths = [0.20 if inside & (1 << i) else 0.28 if convex & (1 << i) else 0
                 for i in range(4)]
    if not any(strengths):
        return tile
    pixels = tile.load()
    for y in range(CELL):
        for x in range(CELL):
            r, g, b, a = pixels[x, y]
            if max(r, g, b) < 30:
                continue
            corner_distance = (
                max(CELL - 1 - x, y), max(x, y),
                max(CELL - 1 - x, CELL - 1 - y), max(x, CELL - 1 - y),
            )
            boost = max((strength * (14 - distance) / 14
                         for strength, distance in zip(strengths, corner_distance)
                         if distance < 14), default=0)
            if boost:
                pixels[x, y] = (min(255, round(r * (1 + boost))),
                                min(255, round(g * (1 + boost))),
                                min(255, round(b * (1 + boost))), a)
    return tile


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    width, height = source.size
    names = NAMES + AUTOTILE_NAMES + WALL_VARIANT_NAMES
    atlas_rows = (len(names) + ATLAS_COLUMNS - 1) // ATLAS_COLUMNS
    atlas = Image.new("RGBA", (CELL * ATLAS_COLUMNS, CELL * atlas_rows), (0, 0, 0, 0))
    for index, name in enumerate(NAMES):
        source_row, source_column = divmod(index, 4)
        row, column = divmod(index, ATLAS_COLUMNS)
        tile = source.crop((
            round(source_column * width / 4), round(source_row * height / 4),
            round((source_column + 1) * width / 4), round((source_row + 1) * height / 4),
        ))
        # The sheet has stray almost-transparent pixels in the cell margins.
        # Bounds from opaque material retain the intended silhouettes.
        mask = tile.getchannel("A").point(lambda alpha: 255 if alpha > 100 else 0)
        bounds = mask.getbbox()
        if bounds is None:
            raise ValueError(f"No visible art in {name}")
        tile = tile.crop(bounds)
        tile.putalpha(tile.getchannel("A").point(lambda alpha: 0 if alpha < 40 else alpha))
        if name == "stone_half":
            # This half block occupies the lower half of its *full* grid cell.
            tile = tile.resize((CELL, CELL // 2), Image.Resampling.LANCZOS)
            x, y = column * CELL, row * CELL + CELL // 2
        elif name == "platform_oneway":
            # Its thin wood top aligns with the one-way fixture at cell y+.82.
            tile = tile.resize((CELL, 12), Image.Resampling.LANCZOS)
            x, y = column * CELL, row * CELL
        elif name.startswith("ladder_"):
            # The source sheet's ladder is only about half a cell wide. Pack
            # the visible rungs tightly so a 0.56-unit draw matches its grip.
            tile = tile.resize((56, CELL), Image.Resampling.LANCZOS)
            x = column * CELL + 4
            y = row * CELL
        elif name in ("stone_recess",):
            tile = tile.resize((CELL, CELL), Image.Resampling.LANCZOS)
            x, y = column * CELL, row * CELL
        else:
            # Full-cell fixtures need full-cell art. Centered thumbnails left
            # transparent gutters that exposed the background between stones.
            tile = tile.resize((CELL, CELL), Image.Resampling.LANCZOS)
            backing = Image.new("RGBA", (CELL, CELL), (25, 20, 24, 255))
            backing.alpha_composite(tile)
            tile = backing
            x, y = column * CELL, row * CELL
        atlas.alpha_composite(tile, (x, y))

    # Store the original face art and every exposed-face/inside-corner variant.
    faces = wall_faces()
    for local_index, name in enumerate(AUTOTILE_NAMES):
        source_row, source_column = divmod(local_index, 3)
        tile = faces[source_row][source_column]
        row, column = divmod(len(NAMES) + local_index, ATLAS_COLUMNS)
        atlas.alpha_composite(tile, (column * CELL, row * CELL))

    start = len(NAMES) + len(AUTOTILE_NAMES)
    for lighting, name in enumerate(WALL_VARIANT_NAMES):
        row, column = divmod(start + lighting, ATLAS_COLUMNS)
        atlas.alpha_composite(wall_variant_face(faces, lighting), (column * CELL, row * CELL))

    atlas.save(ATLAS_IMAGE)
    lines = [
        "ruins-tiles.png", f"size: {CELL * ATLAS_COLUMNS}, {CELL * atlas_rows}", "format: RGBA8888",
        "filter: Nearest, Nearest", "repeat: none",
    ]
    for index, name in enumerate(names):
        row, column = divmod(index, ATLAS_COLUMNS)
        lines.extend((
            name, "  rotate: false", f"  xy: {column * CELL}, {row * CELL}",
            f"  size: {CELL}, {CELL}", f"  orig: {CELL}, {CELL}",
            "  offset: 0, 0", "  index: -1",
        ))
    ATLAS_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
