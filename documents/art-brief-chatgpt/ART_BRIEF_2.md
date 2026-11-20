# Art brief 2: native-scale ruins, tiles, alley interiors, walk-through building, enemies, NPCs, items, UI

You are the lead pixel artist and technical artist for this game, continuing your first delivery
(the modular ruins street and the dagger-and-buckler hero, `hero-v3`). This brief is
self-contained: it repeats every number, name and file format you need, because you cannot see
the repository. The attachments are listed in §18.

**Goal: a serious, full-fledged commercial product**, not a prototype to show friends. Every
asset must be finished, consistent with every other asset, readable in motion, and meaningful in
the story. A concept, a rough generation or an upscale is not a finished asset.

**Deliver art and data only.** Do not change game code. Every runtime behaviour this brief
describes will be implemented by the developer (Claude Code) against the contracts given here:
the alley-entrance splice, platform anchors, per-area tile sets, the walk-through building, new
enemies and NPCs, and the UI skin. If you need a different contract, state it in your handoff
notes and do not ship code.

Work in this order. **Stop for the user's approval at every gate (⛔)** and show a game-scale test
composite each time (§15):

0. Recolour test: the hero and two facades (§3). ⛔
1. The street facades at native scale, with platform alignment (§4). ⛔
2. The platform anchor kit (§5). ⛔
3. The alley-entrance modules (§6). ⛔
4. The gameplay tile sets (§7). ⛔
5. The rooftops, far city and sky (§8). ⛔
6. The alley interior (§9). ⛔
7. The laundry (§10). ⛔
8. The catchment cellar and the vault (§11). ⛔
9. The walk-through building (§12). ⛔
10. The enemies (§13), one type at a time, starting with the alley hunter. ⛔ each.
11. The NPCs, items and UI (§14). ⛔
12. QA (§16) and the final delivery zip (§17).

---

## 1. What is in the game now

Attachments show all of this.

- **Engine and view:** libGDX, side view, orthographic camera, no perspective.
- **Grid:** **32 art pixels = 1 world unit = 1 tile**. All art is drawn at that density and
  displayed at a whole-number screen scale, with Nearest filtering.
- **Camera framing:**
  - The view aims at 270 art pixels of height. At 1080p it shows 8.44 × 15 units at 4×; at 720p,
    7.5 × 13.3 units at 3×; at 1440p, 9 units tall at 5×.
  - About 23 % of the view height lies below the walk line.
  - When the hero jumps, the camera rises with him.
- **Hero:** `hero-v3`, with frames of 160 × 80 px and the anchor (80, 77) at the feet.
  - The figure is 37 px tall (1.16 units) and 17 px wide; his collision body is 1 × 1 unit.
  - **The user likes his current style.** It is the style reference for every character in
    this brief.
- **Road:** the approved road (`ruins-road-A/B/C.png`) stays unchanged.
  - Its paving surface reaches 0.18 units past the walk line, which shows the depth behind the
    hero's feet, and its front face covers 3.85 units below.
  - The street layer stands on the paving's back edge, **5 px above the walk line**
    (`GROUND_LIFT`).
- **Street (your first delivery):** `street.atlas` holds 12 base facades `F01–F12` (each with
  `_l`, `_r` and `_rear` parts) and 36 inserts `N01–N36` with their own `_left` and `_right`
  edges. `street.json` defines the routes F → N → F and the direct arch F09 → F10.
  - `alley_entry` and `alley_entry_rear` mark the alley door. Three backdrops sit behind:
    `sky.png`, `far.png` and `rooftops.png`.
- **Temporary scaling:** your street is 205 px tall and is currently drawn **×1.5**, so that
  doors are about 1.4× the hero's height. The rooftops and far city are drawn **×0.75** for
  depth. These scale factors (`facadeScale`, `rooftopsScale`, `farScale` in `street.json`) are a
  stopgap. They mix pixel sizes (street pixels are 6 screen pixels at 1080p, the hero's 4) and
  give uneven pixels at 720p and 1440p. **This brief replaces them with native art, after which
  all scale factors return to 1.**
- **Backdrop motion:** the backdrops move at a fraction of the camera's speed. The sky follows
  0.85 of the camera's motion, the far city 0.63 and the rooftops 0.38.
- **Gameplay tiles:** stone walls, half-block ledges, one-way plank platforms, breakable blocks
  and ladders. They are still 64 × 64 px drawn at 1 unit, so their pixels are half the size of
  everything else. You redraw them (§7).
- **Placeholder areas:** the alley, laundry, cellar and vault use a tinted wall-tile backdrop.
  You draw them properly (§9–§11).
- **Enemies:** the alley hunter and scavenger archer have generated 128 × 96 frames. The other
  five types use placeholder or painted art. You redraw all seven (§13).
- **NPCs, items and UI:** NPCs and items are paintings or procedural shapes. The inventory and
  dialogue windows are plain rectangles drawn in the default font. You redraw all of them (§14).

## 2. Global rules

### 2.1 Pixel art (hard requirements, unchanged from brief 1)

- One pixel size everywhere: 32 px per unit, drawn at 1:1. No upscales, no 2 × 2 "pixels", no
  scale factors.
- No anti-aliasing, blur, soft brushes or smooth gradients.
- Binary alpha (0 or 255), palette colours only.
- Diagonals use regular steps; curves are built from deliberate pixel runs.
- Materials are readable clusters, not single-pixel noise. Dithering only where it means
  something: sky bands, smoke density, light falloff.
- Key light from the top left; warm local light only from visible sources (fires, lamps,
  windows). Every lit area has its source.
- Characters face right; the game mirrors them.
- **No text or letters of any kind**, readable or fake. Inscriptions, signs and graffiti are
  abstract strokes and symbols.

### 2.2 Updated versions, not new designs

Every redraw is **an updated version of the existing art**: the same buildings, the same
composition, the same joins, the same characters' silhouettes and costumes. Redraw them at
native resolution with more detail, better materials and the new colour rules. Do not invent
replacements.

- Facade `F05` must still be recognisably `F05`, and the hunter must still be the hunter.
- Areas that do not exist yet (alley interiors, laundry, cellar, building, UI) are new, and must
  look like they belong to the same world and the same hand.

### 2.3 World and story

Brief 1 §2 is unchanged. In short:

- **The world:** an old gothic city of stone and timber, medieval, dark fantasy with a post-punk
  tone, on the border of the Red Congregation (KK) and the Ink Coven (ChK).
- **The slice:** "Missing Neighbours". Hunters wearing false Red signs have raided the street,
  closed its two exits and led people away from a forged census list. The Neighbours live in
  ruins patched with boards and cloth.
- **The alley:** it leads to an old laundry above a catchment cellar. The hunters use the cellar
  as a holding place.
- **The transfer gate:** captives are handed over there to a Mechanical Congregation
  intermediary.
- **Story rules:** every prop must mean something in this story. No gratuitous gore, no
  meaningless filler.

## 3. Colour and value: the hero must win the screen

The user's playtest found that **the hero is not vivid enough and blends into the facades, and
the facades are too bright and overpower him.** Measured on the current art (luminance 0–1):

| | mean luminance | brightest 10 % | mean saturation |
|---|---|---|---|
| hero v3 (idle) | 0.32 | 0.67 | 0.26 |
| street facades | 0.29 | 0.51 | 0.24 |
| rooftops | 0.29 | 0.46 | 0.09 |
| road surface | 0.30 | 0.45 | 0.15 |

The hero and the facades sit at almost the same brightness and saturation, so nothing separates
him. Required:

- **Street facades** (the near street layer):
  - mean luminance **≤ 0.22**, brightest 10 % **≤ 0.42**, mean saturation **≤ 0.18**;
  - shadows that read as smoky, slightly cool greys;
  - warm firelight only in small, deliberate areas (windows, doorways, embers), never across
    whole walls;
  - far less bright speckle: high-contrast single-pixel highlights strobe when the street scrolls
    by at running speed and strain the eyes (the user reported this).
- **Rooftops:** mean luminance about 0.24, contrast lower than the facades. **Far city:** hazier
  still, close to the sky's values.
- **Hero** (recolour only):
  - same pixels, frames, anchors and markers; only colours change;
  - mean luminance **0.38–0.42**, brightest 10 % **≥ 0.75**, mean saturation **≥ 0.35**,
    concentrated in his signature cold teal (coat, scarf) plus one small warm accent;
  - a clear 1-px dark outline and a lighter rim on the edges facing the key light, so he reads
    against both dark and lit backgrounds.
- **Separation:** at least **0.15** between the hero's mean luminance and the facade area behind
  him, everywhere on the street.
- **Enemies:** more readable than the background but less vivid than the hero. Each type has its
  own accent (§13). **No environment or enemy colour may be a saturated teal or cyan**: that hue
  belongs to the hero.
- **Interiors and alley:** darker (§9–§11). The hero stays the brightest, most saturated figure
  there too.

Gate 0 ⛔: the recoloured hero and two recoloured facades (F01 and N05) in a composite at 1080p,
plus a table of the measured values.

## 4. Street facades at native scale

Redraw every region of `street.atlas` (`F01–F12` with `_l`, `_r`, `_rear`; `N01–N36` with
`_left`, `_right`; `alley_entry`, `alley_entry_rear`) at **1.5× its current size, drawn natively
at 32 px per unit**. This is a redraw with new detail, not an upscale.

- **Height:** 205 → **308 px** (9.6 units). The facade bottom stands 5 px above the walk line.
- **Width:** `round(old width × 1.5)`, adjusted by a pixel where needed so that **every join
  still matches pixel-perfectly**:
  - every `F*_r` against every `N*_left` it meets in `street.json`;
  - every `N*_right` against the following `F*_l`;
  - `F09` against `F10` for the arch.
  Update `street.json` (`overlap` values scale too) and the atlas.
- **Doors:** about **52 px** tall (1.4× the hero). Windows, lintels, steps, shutters and stones
  scale with them. Use the extra pixels for detail and material: stone grain, timber grain,
  plaster damage, soot plumes, slate courses, red raider marks, Neighbours' repairs, laundry.
  - Keep the information density of a commercial pixel-art game (see the posters) without
    single-pixel noise.
- **Ruin language** (brief 1 §2.5):
  - gutted shells, missing floors with joist stubs, timber burned to the frame, collapsed
    masonry heaps;
  - sky visible through empty windows, as transparent pixels where the rooftops layer should
    show through.

### 4.1 Architecture that meets the platforms

The level puts stand-on surfaces at fixed heights above the walk line. Measured over 520
generated street rooms:

| surface | heights above the walk line (units) | how common |
|---|---|---|
| half-block stone ledge (top of a half block) | **4.5**, 6.5, 8.5 | 4.5 is the most common surface of all |
| one-way plank platform (runs of 4–12 tiles) | **6.0**, 8.0, 10.0 | 6.0 most common |
| top of solid stone blocks and columns | 8.0–12.0 | 10.0 most common |

The facade bottom is the walk line plus 5 px, so a surface at height *h* units lies
**32·h − 5 px above the facade's bottom row**. In a 308-px facade, counting rows from its top:

| height | px above the facade bottom | row from the top of a 308-px facade |
|---|---|---|
| 4.5 | 139 | 169 |
| 6.0 | 187 | 121 |
| 6.5 | 203 | 105 |
| 8.0 | 251 | 57 |
| 8.5 | 267 | 41 |
| 10.0 | 315 | above the facade (−7) |

At these rows, every base facade and insert must offer a plausible horizontal architectural
line that a stone ledge or a plank walkway could rest on or attach to:

- string courses;
- the sill beams of jettied storeys;
- balcony floors and rails;
- burned floor slabs with joist ends;
- eaves;
- window sills and lintels;
- scaffold boards.

Requirements:

- **At least the 4.5 and 6.0 lines on every facade; 8.0 and 8.5 on most.** Platforms are placed
  by the level generator at any horizontal position, so the lines must run across the facade
  wherever possible. Do not put an isolated feature in one spot.
- Lines should **not** look walkable where no platform is: keep them shallow (2–4 px deep, flush
  or slightly projecting). A deep shelf is a platform the player will try to stand on.
- Put the rows on a marked ruler in the composites so the user can check the alignment.

## 5. Platform anchor kit

The game will draw these decals **behind every platform**, at gameplay depth, so platforms look
built into the street instead of floating. Draw each kit twice: **street** (stone, timber,
firelit) and **interior** (darker, damp; used in the alley, laundry, cellar and building).

- **One-way plank platform** (its walk surface is the top 6 px, 0.18 units, of its 32 × 32 tile;
  the tile set draws the planks):
  - `anchor_oneway_left`, `anchor_oneway_mid` and `anchor_oneway_right`, each **32 × 64 px**;
  - row 0 is the platform's top surface; they show what holds the planks up: joists, brackets,
    a hanging beam, ropes, a propped post reaching down. `mid` tiles horizontally;
  - also `anchor_oneway_post`, **32 × 192 px**: a long prop from the platform down toward the
    ground, used under long runs.
- **Half-block stone ledge** (the block fills the lower half of its tile; its top is the ledge
  surface):
  - `anchor_half_left`, `anchor_half_mid` and `anchor_half_right`, each **32 × 48 px**;
  - row 0 is the ledge's top surface. Show corbels, a broken floor slab edge, beam ends.
- **Wall attachment:** `anchor_wall_joint`, **32 × 32 px**: where a platform meets a solid wall
  column (beam pocket, iron strap).

Deliver both kits in `anchors.png` + `anchors.atlas`, region names
`<kit>_<name>` (for example `street_anchor_oneway_left`).

## 6. Alley entrance modules (a natural opening in the street)

The alley door is a gameplay portal: the hero presses E there to enter the alley. It must look
like a real passage between two houses, flowing seamlessly into its neighbours.

- **`alley_entry`** (front, **308 px tall**, about 160–200 px wide):
  - the passage opening between two houses; beams or an arch overhead, perhaps a hanging lamp or
    laundry across it;
  - an opening at least **96 px wide and 80 px tall** (the hero is 37 px), so the opening reads
    instantly as the way in;
  - transparent inside the opening, so the rear module shows through.
- **`alley_entry_rear`** (same size, drawn behind the front): the lane receding into darkness
  behind the opening.
  - Walls of houses converging, a drainpipe, a lit window or lamp halfway down, the far end lost
    in smoke.
  - Darker than the street, with a cooler light falling from the sky slit above.
- **`alley_entry_left`** and **`alley_entry_right`**:
  - facade modules that stand directly left and right of the entrance and share its walls,
    beams and roofline, so the three read as one place;
  - their outer edges match the street's standard join, so they connect to any `F*_l` or
    `F*_r` like an insert does.
- The same set serves the **far alley mouth** (the alley's second exit onto the street). Either
  mirror the set, or draw `alley_mouth_*` variants of the four modules.
- The developer will cut the generated street around the portal and place left + entry + right
  there, with rear behind entry. Give in the atlas metadata (or your notes) the **pixel column
  of the passage centre** in `alley_entry`: the portal's position is aligned to it.
- Platform alignment rows (§4.1) apply to these modules too.

## 7. Gameplay tile sets (redraw all tiles)

The current tiles are 64 × 64 and must be redrawn at **32 × 32 px per tile** (1 unit), as
updated versions of the current stone tiles.

**Three sets**, each `tiles-<set>.png` + `.atlas`:

- **`street`:** matches the street facades' stone, so a ledge reads as part of a building.
- **`alley`:** darker and damper, with soot.
- **`interior`:** the laundry, cellar and vault. Wet rough stone, cellar-dark; it may be split
  into `laundry` and `cellar` variants if needed.

**Regions per set:**

- **Autotile sheet `wall`:** a 3 × 3 sheet of 32 × 32 cells, top-left to bottom-right:
  `wall_top_left`, `wall_top`, `wall_top_right`, `wall_left`, `wall_center`, `wall_right`,
  `wall_bottom_left`, `wall_bottom`, `wall_bottom_right`.
  - Faces open to the room are lit and worn; faces against other stone are plain.
  - The developer's tool builds all 256 lighting variants from this sheet. For that, every cell
    must keep consistent stone courses across its edges, so any combination joins.
  - Also deliver `wall_inner_corner_light`: a 32 × 32 overlay showing how an inside corner
    brightens.
- **`stone_half`:** the block fills the **lower half** (rows 16–31); rows 0–15 are transparent.
  The top row is the ledge's walking edge.
- **`platform_oneway`:** planks in **rows 0–5** (the 0.18-unit walk surface). Rows 6–31 are
  transparent, except for small plank ends or nails. The anchors (§5) draw the supports.
- **`block_breakable`:** cracked masonry that reads clearly as breakable, different from normal
  wall.
- **`ladder_segment`:** two rails and rungs, tiling vertically every 32 px.
- **`ladder_cap`:** the top end.
- A few 32 × 32 **wall variants** (`wall_center_b`, `_c`, `_damp`, `_soot`) to break repetition.

Show a test room that uses every tile next to the facades.

## 8. Rooftops, far city and sky

- Redraw `rooftops.png` and `far.png` at **0.75× their current size, natively**, as updated
  versions of the same skylines:
  - `rooftops.png`: 2656 × 224 → **1992 × 168**;
  - `far.png`: 2560 × 224 → **1920 × 168**.
  - Both tile horizontally.
- Recolour them to §3: rooftops darker and flatter than the facades, far city hazier.
- **Vertical placement** (texture bottom relative to the street's floor line, which is the walk
  line + 5 px):
  - rooftops bottom at **−29 px**;
  - far city bottom at **+31 px**.
  Change these only if your composition needs it, and say so.
- **Sky:** keep `sky.png` at 1280 × 360. Recolour it to sit behind the darker street: dirty grey,
  smoke masses, a weak orange glow low down.

## 9. The alley interior (darker)

The alley is the `laundry_alley` location: narrow lanes between the backs of houses, entered
from the street.

- **Rooms:** `entrance` (from the street), one or two `corridor` rooms, a `side_room` above the
  first corridor (reached by a ladder), the `alley_corridor` above the laundry, the
  `transfer_gate`, and the `exit` back to the street.
  - Rooms are 32–40 units wide and 14–16 units tall. The camera frames one room at a time.
- **Light:** less light reaches the alley.
  - Mean luminance of the backgrounds **≤ 0.15**.
  - A cold, dim light falls from the sky slit far above.
  - Warm light comes only from a few lamps, lit windows and doorways.
- **Back wall:** modules in the same scheme as the street: **480 px tall** (15 units, covering a
  room plus camera margin), **96–192 px wide**, tiling with matching joins.
  - The backs of houses: doors, steps, drainpipes, rain barrels, outside stairs, washing lines
    strung across the lane.
  - Neighbours' repairs, raider marks on doors that were broken in.
- **Transfer gate room:** a heavy gate in a wall where the captives are handed over.
  - A cart with a cage, crates with Mechanical Congregation sorting marks (abstract symbols,
    no text), a lantern on a hook.
- **Foreground pieces:** sparse near-side wall edges, hanging cloth. In front of the hero, so
  they must be rare and narrow: at most one per room, at most 2 units wide.
- **Turning from the street:** the transition into the alley is a 2.5D turn. Deliver the
  entrance room's first 20 units so its look continues the street's `alley_entry_rear`.

## 10. The laundry (below the alley corridor)

The former laundry that once served the surrounding courtyards; its street facade collapsed long
ago, and the hunters now use it. Room 32 × 14 units.

- **Back wall modules** (480 px tall): soot-stained plaster over stone, a vaulted ceiling line,
  a stove with a chimney flue (the main warm light).
- **Props** (separate sprites with transparent backgrounds, standing on the floor line):
  - washing vats and cauldrons, wooden troughs, a wringer, soap barrels;
  - drying lines with sheets;
  - the hunters' traces: ropes, a bench, crates.
- **Steam:** two or three static steam or haze shapes, kept in separate files.
- **Light:** darker than the street. Mean luminance ≤ 0.17, the stove's glow the brightest area.

## 11. The catchment cellar and the vault (darkest)

- **Catchment cellar** (room 36 × 14): the old water-collection cellar the hunters use to hold
  captives.
  - Stone barrel vaults, a cistern basin with dark water, channels and an iron grate.
  - Holding pens or cages with straw, shackles and chains, a guard's lamp (the only warm light).
  - Story evidence, as small props:
    - crates with the Mechanical Congregation's sorting marks;
    - a torn scrap of a red register with a seal;
    - an ink mark of a House of Continuation disguised as a charity stamp.
  - The captive NPC stands here (§14).
- **Vault** (room 26 × 14): the deepest, smallest room; old storage, collapsed shelving, the
  sewing kit item.
- **Light:** mean luminance ≤ 0.12. Cold, wet stone; warm light only from the lamp.
- Same module scheme (480-px back walls, separate props), using the `interior` tile set.

## 12. Walk-through building on the street (new feature)

A building the street runs **through**: the hero walks in at one end, crosses its ground floor
and walks out the other end. Art required:

- **Exterior front pieces (front layer, drawn in front of the road and the hero):**
  - `building_front_entry` and `building_front_exit`: the building's near wall at its two
    doorways.
    - A doorway pillar or jamb at most **1.5 units (48 px) wide** with the door frame, and the
      wall above the doorway up to the roof.
    - They span from **4 units below the walk line** (in front of the road's face) to the
      building's top.
  - `building_front_header`: a horizontal strip, tileable at 64 px, carrying the near wall's
    upper part (beam, wall, eaves) across the interior.
    - Its bottom edge must stay **at least 4.5 units above the walk line** so it never hides the
      hero, even mid-jump.
  - The front layer **must never cover the hero inside the building**, except for the instant he
    passes a doorway pillar.
- **Exterior facade:** `building_facade_*` modules so the building also stands in the street row
  (seen from outside before you enter), joining like inserts (§4).
- **Interior:**
  - **Back wall modules:** `building_interior_left`, `_mid` (tileable), `_right`, 308 px tall, a
    lived-in ground floor wrecked by the raid.
    - A hearth with embers, a table knocked over, shelves, a bed frame, stairs to the upper
      floor, Neighbours' patched repairs, a lit lamp.
  - **Floor:** `building_floor`, a module **64 px wide × 128 px tall** that tiles horizontally
    and replaces the road inside.
    - Rows 0–5 show the floor's back edge (wooden boards or flagstones) above the walk line,
      which is at row 6.
    - Rows 6–127 show the floor's front face (beams, foundation, cellar darkness) down to the
      bottom of the view.
  - **Ceiling:** `building_ceiling`: upper-floor joists seen from below, tileable at 64 px. Its
    bottom edge is at least 4.5 units above the walk line.
- **Inner walls:** `building_partition`: an interior wall with a doorway, between two rooms of
  the building. It is in the front layer like the entry pillar, with the same width limit.
- **Size:** the building's interior is 16–32 units long and 5–6 units high to the ceiling. It may
  contain half-block ledges at 4.5 and a plank gallery at 6.0, using the `interior` anchors.
- **Light:** interior mean luminance about 0.18, warm from the hearth and lamp. The hero must
  stand out (§3).

## 13. Enemies (redraw all seven, hero style)

Redraw every enemy **in the style of the current hero** (`hero-v3`, attached): the same pixel
density, outline, shading method and level of detail. Use the same frame and metadata format as
the hero (§13.3).

- **Updated versions, not new designs:** the alley hunter and scavenger archer keep their current
  silhouettes and costumes (attached sheets), redrawn. The other five types are designed from
  their roles below and their current placeholder art.
- **Anchor:** bottom centre of each frame, three rows above the bottom: `(width/2, height − 3)`.

| type id | who | collision body (units) | figure height | frame (px) | role, silhouette and accent |
|---|---|---|---|---|---|
| `default` | alley hunter, hired captor | 1.0 | ~38 px | 160 × 80 | straight, medium build, club or cleaver, short cape; grey-olive cloth, red armband of the false Red signs; a visible closing lunge |
| `scavenger` | scavenger archer, a guide from a suburban gang | 0.9 | ~35 px (bow taller) | 160 × 96 | tall and thin, hood; a composite bow of wood and salvaged steel taller than his head; ochre and sun-bleached leather |
| `infiltrator` | light scavenger | 0.85 | ~30 px (hunched) | 160 × 80 | low, hunched, long arms with hooks, dirty ochre, torn ribbons; dodges with a pause and punishes mindless repeats |
| `overseer` | the raid's overseer, who empowers other hunters | 1.0 | ~38 px | 160 × 112 | upright, a staff with a small banner or censer above his head; dark cloth with red marks; stands still to channel a buff onto an ally, and a stagger interrupts the channel |
| `red_collector` | "collector of measure", the Elder Hunter's former aide | 1.4 | ~52 px | 192 × 112 | robe to the ground, a **genuine** red bracer used for a frontal block, vials and a censer for healing allies; crimson and pale grey |
| `heavy_hunter` | heavy hunter | 1.7 | ~62 px, broad | 224 × 128 | massive shoulders, hammer or shield, iron and brass parts from Mechanical salvage; an area slam with a telegraphed safe zone |
| `boss` | the Elder Hunter, former Red servant struck from the register | 3.0 | ~108 px | 384 × 192 | the tallest figure, long coat, white fur collar, a chain trap; dark crimson and white; part Red drill guard, part dirty mercenary tricks |

### 13.1 Animations

Frame selection follows the hero's rules:

- Attacks pick frames by **phase and phase progress** (`phases` with first and last frame index).
- `block`, `hit` and `stagger` are picked by state.
- Everything else plays by `frameDuration`.

**Every type has:**

- `idle` (4 frames), `walk` (8), `patrol` (8, a slower and wary walk), `detect` (3, noticing
  the hero);
- `attack` (5 frames: startup [0,1], active [2,2], recovery [3,4]);
- `hit` (2), `stagger` (3, looped), `death` (6, last frame holds), `recover` (3, getting up);
- `reposition` (4, sidestep between combat positions);
- `jump` (2) and `fall` (2).

**Type-specific animations** (the names are fixed):

| type | animations |
|---|---|
| `default` | `jump_attack` (6: startup [0,1] with a visible crouch, active [2,3] airborne strike, recovery [4,5] landing); `lunge` (5: startup [0,1], active [2,2], recovery [3,4]); `block` (2) |
| `scavenger` | `shot` (5: startup [0,2] nock, draw, full draw; active [3,3] release; recovery [4,4]); `charged_shot` (6: a longer draw, then the release); `volley` (6: several arrows fanned); `bash` (4: point-blank bow bash, startup [0,0], active [1,1], recovery [2,3]); `dodge` (3); `retreat` (4) |
| `infiltrator` | `dodge` (3, with a held pause frame); `flank_dash` (4); `hook_strike` (5) |
| `overseer` | `cast` (4, looped while channelling: staff raised, standing still); `cast_release` (3: the buff leaves the staff toward an ally); `cast_interrupted` (2) |
| `red_collector` | `block` (3: the red bracer raised; frame 2 is the impact); `heal` (6: raises a vial or censer, a glow at the hands); `bracer_break` (2) |
| `heavy_hunter` | `slam` (7: startup [0,2] long telegraph crouch, active [3,3], recovery [4,6]); `sweep` (6); `block` (2, shield) |
| `boss` | `light_combo` (8, two strikes); `area_strike` (8, long telegraph); `chain_throw` (7: the chain trap); `guard` (3); `taunt` (4); `intro` (6) |

### 13.2 Readability

- Recognisable in solid black at 25 % size, and by accent colour.
- Every attack's startup is readable: the weapon pulls back and the body compresses.
- The active frame visibly covers the strike area in front of the body (about 1.0–1.5 body
  widths for melee).
- None is more vivid than the hero (§3).

### 13.3 Frame and metadata format (same as the hero)

- **Atlas:** one atlas per type, `assets/actor/enemies-v3/<type>.png` + `.atlas`.
  - Region name = animation name, `index` = frame number.
  - Every region is the full frame size (no trimming), with 2 px padding.
- **Metadata:** `assets/config/enemy_animations_v3/<type>.json`:
  - top level: `version`, `sprite`, `frame`, `anchor`, `pixelsPerUnit: 32`,
    `visualHeight = frame height / 32`, `facing: "right"`;
  - per animation: `frames`, `frameDuration`, `loop`, `phases` (attacks), and `weaponTip`
    (one `[x, y]` per frame);
  - for attack animations also `attackOrigin` per frame.
- **Review sheet:** `documents/art-previews/enemy-<type>-sheet.png`, at 2× with a ground line.

## 14. NPCs, items and UI

### 14.1 NPCs

| id | who | notes |
|---|---|---|
| `keeper` | the Keeper | a mysterious figure who gives the hero the Echo dash for one point of maximum health; bound to no faction; quiet, unsettling |
| `shelter_warden` | the Warden | the woman who keeps the Neighbours' shelter; practical, tired, protective; patched clothes, keys, a satchel of bandages |
| `cellar_prisoner` | the Captive | a Neighbour held in the cellar, who knows only the hunters and the direction of transport; frightened, bound hands, torn clothes |
| `mother` (optional) | the hero's mother | for the start room: a woman who repaired the house with boards and cloth |

Deliver per NPC:

- **World sprite:** `idle` (4) and `talk` (4); the Warden also `gesture` (3).
  - Frame **96 × 80 px** at 32 px per unit, anchor (48, 77).
  - Figure height about 35–38 px; base each on its current painting or figure.
- **Dialogue portrait:** a **128 × 128 px** bust with three expressions (neutral, worried,
  hopeful); for the Keeper, neutral plus one unsettling expression.

### 14.2 Items

Nine items, each with:

- a **world pickup sprite**: 32 × 32 canvas (1 unit), object about 20–24 px, sitting on the
  bottom row;
- an **inventory icon**: 32 × 32, shown at an integer scale in the bag.

| id | what it is (story) |
|---|---|
| `field_bandage` | ordinary first aid, made by the Neighbours or taken from the hunters' stores (base on the current bandage art) |
| `reinforced_bandage` | a better bandage, stitched with the Neighbours' sewing kit |
| `sewing_kit` | the Neighbours' suture kit, given for saving a captive or helping a courtyard |
| `smoke_incense` | a smoke censer the hunters use during captures; blinds archers |
| `red_bracer` | a genuine Red Congregation bracer from the collector of measure; strengthens the block |
| `weighted_bracer` | a cruder bracer weighted with iron |
| `blade_shard` | a shard of the Elder Hunter's or his old unit's blade; increases melee damage |
| `impact_core` | a part of a Mechanical tool from a ruined workshop; increases knockback |
| `fused_relic` | the hero's own improvisation: the blade shard fused to the impact core, a human hack, not a faction relic |

Also redraw the **sealed note** pickup (the order: number of people, time and place of
handover). Base it on the current note art.

### 14.3 UI

All UI is drawn in **art pixels**. The game shows it at a whole-number scale chosen by the
developer (about 2× at 720p and 3× at 1080p), so every UI pixel is crisp. The style is worn leather, timber
and iron, matching the world; warm parchment for text areas.

- **Inventory window** (bag):
  - A 3 × 2 grid of cells, with two separate pockets to its left and right.
  - Each cell is **34 × 34 art px** with 4-px gaps.
  - The panel frame and backing are a 9-slice.
  - Cell states: `normal`, `hover`, `selected`, `craft_first` (the first item chosen for
    crafting).
  - An item name and description strip below the grid.
  - A pocket frame with its own look (pockets are quick-use).
- **Dialogue window:**
  - a name plate;
  - a message box for 3–5 lines (9-slice);
  - a portrait frame for the 128 × 128 portraits;
  - three choice buttons (`normal`, `hover`, `pressed`, `disabled`);
  - a hint line.
- **HUD:**
  - a health bar: frame, fill, the chunk lost on damage, a low-health state;
  - a boss bar.
- **Font:** a bitmap pixel font, Latin and Cyrillic, in two sizes (small for world labels and
  hints, regular for dialogue and UI). Deliver it in BMFont text format (`.fnt` + `.png`).
- Deliver everything in `ui.png` + `ui.atlas`, with 9-slice splits written in the atlas
  (`split: left, right, top, bottom`).
- A mockup of the bag and of a dialogue at 1080p, for the gate.

## 15. Test composites for every gate

Render 1920 × 1080 frames the way the game does (§1):

- the approved road;
- the street at native scale with the hero standing on the walk line at the screen's centre
  column;
- the platform-height ruler (§4.1);
- one frame with a platform run at 6.0 and a half-block ledge at 4.5 placed in front of the
  facades, with their anchors;
- for interiors, the room with its tiles, props and the hero.

Show every enemy next to the hero at true size. Report the §3 measurements for each composite.

## 16. QA checklist (run it, report the results)

- **Sizes:** every PNG has its exact size, RGBA, alpha only 0 or 255, palette colours only.
- **Joins:** street joins are pixel-perfect for every route in `street.json` (render all routes
  in a strip and show it).
- **Tiling:** every tiling module (back walls, floor, header, ceiling, rooftops, far, sky) tiles
  seamlessly.
- **Tiles:** every tile set has all regions; the autotile sheet joins across all edges.
- **Atlases:**
  - regions inside their pages and not overlapping;
  - pages a power of two and at most 4096 × 4096;
  - frame counts match the metadata;
  - every animation in §13.1 exists, and `weaponTip` is given for every frame.
- **Colour:** the §3 targets are met, and the hero is the most vivid figure on every composite.
- **By eye:**
  - no text, no meaningless objects, no repeated clones;
  - ruins look ruined;
  - updated versions stay recognisable;
  - platform lines fall on the ruler rows;
  - the front layers never hide the hero.

## 17. Delivery (one zip, repository paths)

```
assets/art/ruins-palette.json, assets/art/ruins-palette.gpl        (palette, all new ramps)
assets/backgrounds/ruins/street-*.png, street.atlas, street.json  (native scale; facadeScale/rooftopsScale/farScale = 1)
assets/backgrounds/ruins/sky.png, far.png, rooftops.png
assets/backgrounds/ruins/anchors.png, anchors.atlas
assets/backgrounds/alley/*      (back-wall modules, props, foreground pieces + atlas + a JSON catalog like street.json)
assets/backgrounds/laundry/*
assets/backgrounds/cellar/*     (catchment cellar and vault)
assets/backgrounds/building/*   (front, facade, interior, floor, ceiling, partition)
assets/tiles/tiles-street.png/.atlas, tiles-alley.png/.atlas, tiles-interior.png/.atlas  (and laundry/cellar if split)
assets/actor/hero-v3/hero-v3.png, hero-v3.atlas    (recoloured, same layout)
assets/config/hero_animations_v3.json              (same markers; update only if a marker moved)
assets/actor/enemies-v3/<type>.png/.atlas          (7 types)
assets/config/enemy_animations_v3/<type>.json
assets/actor/npc/<id>.png/.atlas, assets/config/npc/<id>.json
assets/ui/portraits/<id>-<expression>.png
assets/items/sprites/<id>.png, assets/items/icons/<id>.png
assets/ui/ui.png, ui.atlas, assets/ui/font-small.fnt/.png, font-regular.fnt/.png
documents/RUINS_ART_HANDOFF_2.md   (Russian, like the project's documents: what was delivered, decisions, contracts, open questions)
documents/art-previews/*           (every gate composite, every sheet, the platform ruler frames, the §3 measurement table)
source/                            (prompts, raw generations, layered working files, the QA script and its output)
```

Do not change the road, the game code or any file not listed.

## 18. Attachments

- `references/street/`: the current `street-1/2/3.png`, `street.atlas`, `street.json`, `sky.png`,
  `far.png`, `rooftops.png`. These are the art to update.
- `references/tiles/`: the current tile atlas and its source sheets.
- `references/hero-v3/`: the current hero atlas and metadata. This is **the style reference** for
  all characters and the recolour target.
- `references/enemies/`:
  - the hunter and archer atlases, metadata and sheets;
  - the painted placeholders of the scavenger and the red collector;
  - the legacy frame atlas used by the other types.
- `references/npc-items/`: the warden's painting, the bandage and sealed-note art, the boss icon.
- `captures/`:
  - the game at 1080p as it is now (street at ×1.5, rooftops at ×0.75);
  - every room of the alley location (placeholder art);
  - the scale check (hero in front of a facade with a unit ruler).
- `road/`: the approved road textures (unchanged).
- `posters/`: the mood and quality references (do not copy them).
- `ruins-palette.json`: the current palette.
- `RUINS_ART_HANDOFF.md`: your handoff from the first delivery.
- `ART_BRIEF.md`: brief 1 (world, story, pixel rules, formats). Brief 2 overrides it where they
  differ.
