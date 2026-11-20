# Art brief: ruins parallax background and hero pixel art

You are the lead pixel artist and technical artist for a 2D action platformer. You will draw
every image listed here, turn the images into game-ready files (PNG layers, texture atlases,
JSON metadata) and deliver one zip that a developer (Claude Code) drops into the repository
unchanged. You cannot see the repository, so this brief contains every number, name and file
format you need. The attachments listed in §13 are the visual references.

Work in this order and **stop for the user's approval at each gate (⛔)**:

1. Read the whole brief and the attachments.
2. Art-direction sheet: palette, lighting rules, materials, depth examples (§3). ⛔ approval.
3. Hero: three silhouette variants (§7.2). ⛔ the user picks one.
4. The foreground layer (§4.3) as a finished, game-ready PNG plus a test composite. ⛔ approval.
   Then the midground, then the rooftops, then the far city, then the sky and the smoke and fog
   layers, **one layer at a time, nearest first**, with a test composite after each.
5. The 15 modular ruins sets (§5) and their atlas.
6. All hero animations (§7), atlas and metadata, with hitbox overlay sheets.
7. Run the QA checks (§11) and deliver the zip (§12).

The user has rejected two earlier attempts, which were made by writing code that placed shapes.
They looked like a 20th-century brick city, the ruins did not look ruined, detail was far too
low and many elements were meaningless filler. Read §2.6 (what failed) before you start.
Quality bar: the attached posters. Every image must be **beautiful, dense with deliberate
detail and full of meaning from the story**. A concept or a rough generation is not a finished
asset.

---

## 1. The game and its technical frame

- Engine: libGDX 1.12.1 with Box2D, desktop (LWJGL3). Side view, orthographic camera: no
  perspective and no vanishing points in any layer.
- **Project grid: 32 art pixels = 1 world unit.** One Box2D tile is 1 × 1 unit = 32 × 32 px.
  All new art, including the background layers, the modules and the hero, is drawn at exactly this
  density and displayed at scale 1. Never deliver an image where one "pixel" is a 2 × 2 or
  4 × 4 block. Mixed pixel sizes are forbidden.
- Internal reference resolution: 640 × 360 px. The camera shows **20 × 11.25 units** at 16:9;
  wider windows show more width (21:9 shows about 26.7 units), so every layer must tile
  horizontally without a visible seam.
- All textures are sampled with Nearest filtering. Sharp pixels only.
- **The walk line**: the street surface the hero stands on. All vertical positions in this brief
  are measured from it, in units. At rest the camera shows from **2.6 units below** to **8.65
  units above** the walk line. When the hero jumps, the camera rises by up to about 3.5 units,
  and every layer moves with it according to its vertical parallax factor.
- **The road is approved and must not change.** Files `ruins-road-A/B/C.png` (attached) are
  drawn over all back layers from the walk line down to 3.85 units below it. So **every back
  layer is only visible above the walk line**. Its lowest ~1 unit sits behind the road, but it
  must still be painted down to its bottom edge so no gap appears. The road was painted at a
  finer apparent density (about 60 px per unit) and is not on the 32 px grid. Match its palette
  and light, not its pixel size.
- Gameplay tiles (stone walls and ledges the hero can stand on) are drawn by the game in front of
  the back layers; see the attached capture. The background must never look walkable: no
  flat-topped ledges or platforms at jump height in the midground that a player could mistake for
  gameplay geometry.

### 1.1 Pixel-art rules (hard requirements)

- Every pixel sits on the integer grid at 1:1. No anti-aliasing, soft brushes, vector curves,
  blur, gradients drawn with smooth blending, or glow made of semi-transparent pixels.
- Alpha is binary: every pixel is either fully opaque (255) or fully transparent (0). No
  semi-transparent edge pixels.
- Every colour comes from the delivered palette (§3.1).
- Diagonals use regular steps (1:1, 1:2, 2:1, 1:3). Curves are built from deliberate pixel runs
  that shorten or lengthen evenly, never jagged noise.
- Materials are readable masses (stone, timber, slate, cloth, ash) built from clusters, not
  single-pixel noise. Ordered dithering is allowed for sky bands, smoke density and firelight
  falloff, used sparingly and deliberately.
- One key light from the **top left** (the glow of the burning sky over the city), plus local
  warm firelight from visible fires. Every lit area must have a visible source.
- All characters face **right**; the game mirrors them for left.

---

## 2. World, story and what the street must say

### 2.1 The world

An old gothic city of stone and timber, medieval in its architecture and crafts, with a dark
fantasy and post-punk tone: alienation, urban ruin, graffiti on shrines, rough repairs, people
against a system. No modern buildings, no brick apartment blocks, no neon, no cyberpunk, no
forest, no colourful fantasy. Four powers divide the city:

- **Red Congregation (KK).** Kills in the name of balance and keeps red census lists. Motifs:
  pale grey-white stone, gothic cathedrals with statues in niches, torn red banners with a gold
  sun-cross and scales, braziers, red light. (Poster: red-congregation.)
- **Ink Coven (ChK).** Shelters refugees in "Houses of Continuation" and records every life,
  demanding labour and obedience in return. Motifs: black ink poured and dripping over stone,
  ink swirls and inscriptions, violet lanterns, crimson banners, scrolls, gothic arcades.
  (Poster: ink-coven.)
- **Mechanical Congregation (MK).** Takes people as labour. Motifs: copper and brass gears,
  chains, pipes, iron gantries, cyan-glowing vats. (Poster: mechanical-congregation.) In this
  slice it appears only far away and as sorting marks on crates.
- **Bandit gangs** in the outskirts. Motifs: timber scaffolds, tattered cloth awnings, chains,
  barrels with fires. (Poster: bandit-gangs.)

The ruins where the game begins lie on the **border between the Red Congregation and the Ink
Coven**. Surviving houses, chapels, workshops and houses of entertainment show who owned each
place before it was destroyed and who uses it now. Most people you would see are ordinary
residents.

### 2.2 The Neighbours

The residents call their network of mutual help the **Neighbours**: families and courtyards with
no leader and no uniform. You see their life in the ruins: lookouts on rooftops and rubble
heaps, water carriers, people repairing walls, shelter keepers, adults guiding children across
dangerous crossings. Their homes are **ruins patched with boards, cloth and salvaged masonry**.

### 2.3 This slice: "Missing Neighbours" — the raid at the mother's shelter

- The hero is a young son of a factionless family. He lives with his mother in the restored part
  of a destroyed house that she repaired with boards, cloth and found stone. The first room is
  that house; its left edge is closed by a visible collapse and the way leads right.
- **The raid has just happened.** It was an organised capture, not a massacre. Hired hunters and
  scavengers blocked **two exits** of the street, called out names from a **forged extract of the
  red census lists** and led away people fit for labour. Witnesses were beaten or killed only if
  they resisted. The raiders wore **red stripes and seals** like the Red Congregation, but their
  formation and commands did not quite match an official red patrol. In truth the raid was
  ordered by an Ink Coven official and the captives are being sold to the Mechanical
  Congregation, but early evidence deliberately points at the Reds.
- The street afterwards: partially burned buildings, ash-covered paving, hanging smoke,
  collapsed facades, charred beams and rubble, doors broken in, belongings dropped in the
  street, cart tracks of the convoy.
- Places further along the route: an optional **alley to a former laundry** above an old
  catchment cellar (the laundry's facade collapsed long ago; the hunters use the surviving cellar
  as a **temporary holding place** for captives), and the **transfer gate** where captives are
  handed over to a Mechanical Congregation intermediary.
- Enemies (already drawn, not your task): the **alley hunter** (a hired captor with a club and a
  red armband), the **scavenger archer** (a guide from a suburban gang), the **red collector**
  (former aide of the Elder Hunter, with a genuine red bracer).

### 2.4 Meaningful details to place (each one earns its place by telling the story)

Use these instead of generic filler. Not all in one spot; spread them along the strip.

- Doors broken in by the hunters, with **red stripes and a ringed seal daubed beside them**: the
  marks of houses whose people were on the list. Sloppy, dripping paint, not official
  craftsmanship.
- The Neighbours' repairs: planked-over windows, cloth hung as walls and roofs, ropes, props
  holding up a sagging lintel, rain cloth funnelling water into a barrel, laundry lines strung
  between ruins (the laundry is part of the slice).
- A Neighbours' **lookout post** on top of a ruin: a plank platform, a cloth windbreak, a
  ladder.
- The convoy's traces: wheel ruts in the ash, a dropped bundle, a lost shoe, an overturned water
  cart, a broken plank barricade at one of the two exits.
- A street **shrine** with graffiti scratched over it, a candle still burning in its niche.
- Ink Coven marks: a charity-distribution sign of a House of Continuation, ink inscriptions on a
  wall, a violet lantern. Red Congregation marks: a torn red banner on a chapel buttress,
  pale stone.
- Far away: a hunters' **convoy crossing an arcade bridge**: small dark silhouettes with torches
  leading a line of captives. Keep it distant and quiet, never gruesome. Violence and
  exploitation are context, never decoration for shock.
- Fires still burning inside gutted houses, smoke rising from several blocks, ash in the air.

### 2.5 What a ruin looks like here (the most important visual rule)

A building in these ruins is **destroyed**, not "an intact house with fire in its windows".
Show:

- **Gutted shells**: stone walls standing without roofs; through empty windows you see the sky or
  the next layer behind. Gable walls broken off in jagged steps, upper storeys missing on one
  side.
- **Floors gone**: rows of joist sockets in the walls, a few charred joists still sticking out,
  a slab hanging at an angle.
- **Timber houses** burned to their frames: charred posts and braces with the infill fallen out,
  rafters standing against the sky like ribs, a jettied upper storey sagging.
- **Collapse**: heaps of fallen masonry at the foot of walls and spilling into the street, whole
  facades fallen forward, an arch standing alone with nothing behind it.
- **Fire damage**: soot plumes above every window the fire came out of, blackened timber, glowing
  embers, still-burning interiors lighting the stone around them orange.
- **Stone**, not brick: large weathered blocks and rubble masonry, grey with grime streaks and
  soot, joints barely visible and broken. The ruins are stone, timber, slate and lime plaster.
  At most an occasional brick chimney.

### 2.6 What failed before (do not repeat)

- Rows of small rectangular blocks with clean mortar lines everywhere: they read as **bricks**.
- Intact houses with tidy window grids: they read as a **20th-century city**, not ruins.
- Flat fills with a one-pixel edge highlight: **no detail**.
- Meaningless filler: a gear floating on a wall, random crossed beams, pipes going nowhere, orange
  light blotches with no fire, identical buildings repeated. These read as **AI
  hallucinations**.
- Fake text or symbols. Never draw letters or text of any kind, readable or not. Inscriptions
  are abstract strokes.

---

## 3. Art direction

Deliver `documents/art-previews/ruins-art-direction.png` first, with:

- the palette ramps (primary stone, smoke, ash and soot, timber, slate and plaster, fire and
  glow, distant haze, faction accents, hero);
- the light rules (key light top left; firelight; how a face that turns away darkens);
- material examples (weathered stone wall, rubble masonry, charred timber, slate roof, plaster
  with exposed lath, cloth, ash on paving);
- the same ruined facade shown at foreground, gameplay, midground and far values (depth);
- allowed and forbidden detail examples (cluster versus noise; stepped diagonal versus jagged;
  stone versus brick);
- a small composite of all layers.

Also write `documents/RUINS_ART_DIRECTION.md` (in Russian, like the rest of the project's
documents) explaining the rules, the palette roles and the layer list.

### 3.1 Palette

The current palette file (`ruins-palette.json`) is attached. Its structure:

```json
{ "name": "ruins", "version": 1, "note": "...",
  "ramps": { "outline": ["#140e18"], "env_stone": ["#3a2a32", "#4e3c44", "#64505a", "#7c6466"], ... } }
```

Rules:

- You own the background palette. Keep these existing ramps **unchanged**, because already-shipped
  enemy sprites use them: `outline, env_soil, env_stone, env_warm, env_ash, hero_coat, hero_cloth,
  skin, bone, leather, wood, steel, ink_olive, red_mark, ochre, brass, iron, fur, fx`.
- Delete or replace every other ramp (they belonged to the rejected attempts) with your own ramps,
  dark to light, for example `ruin_stone`, `ruin_stone_lit`, `char_wood`, `slate`, `plaster`,
  `ash`, `smoke`, `haze_far`, `haze_near`, `sky`, `fire`, `ember`, `red_paint`, `ink`,
  `ink_violet`, `cloth`, `hero3_*`.
- Ramp lengths of 5–9 steps suit shading. Total background colours: keep under about 80.
- Deliver the same palette as `ruins-palette.gpl` (GIMP/Aseprite palette).
- Colour roles in the ruins: grey smoke and almost-black ash and soot are the core environment
  colours. Dark grey stone. Brown and red-brown burned timber. Dirty earth tones. Dim orange
  firelight and occasional warm light sources. Muted cool tones in distant areas. Black only for
  soot and deep shadow, orange only for fire and its glow, brown for timber.
- The posters are references for **pixel-art density, lighting, depth and volumetric
  composition**. Do not copy their assets, their saturated red skies or their exact palette. The
  ruins are muted: dirty grey sky, weak orange glow.
- The hero's cold teal accent stays unique on screen: no environment colour may be a saturated
  teal or cyan.

---

## 4. Background layers

### 4.1 How the game draws a layer

For each layer the game draws one PNG, repeated horizontally (`repeatMode: "repeatX"`), at
1 texel = 1/32 unit:

```
layerX = baseX - cameraX * parallaxFactor          (horizontal)
layerY = walkLine + verticalOffset + (cameraY - restCameraY) * (1 - verticalFactor)
```

`verticalOffset` is the position of the texture's **bottom edge** relative to the walk line, in
units. Layers with `depth > 0` are drawn behind gameplay, from the largest depth to the smallest;
layers with `depth < 0` are drawn in front of the hero and enemies. Optional solid fills
(`fillAbove`, `fillBelow`, hex colours) extend a layer up to the top or down to the bottom of the
view, so no empty area appears. Offsets snap to whole screen pixels.

Ash and embers are drawn by the game as single-pixel particles; **you do not draw them**. Smoke
and fog banks are textures you draw.

### 4.2 Layer list, sizes and placement

Widths are multiples of 32 and at most 4096 px. The street is 400–500 units long, so use wide
textures to hide repetition. "Row of the walk line" counts from the top of the texture (row 0).

| # | id (file) | group | parallax / vertical factor | size (px) | verticalOffset (units) | walk line at row | visible part |
|---|---|---|---|---|---|---|---|
| 1 | `sky` (`sky.png`) | sky | 0.08 / 0.04 | 1280 × 360 | −0.4 | 347 | everything; `fillAbove` continues it upward |
| 2 | `background_city` (`far.png`) | background | 0.20 / 0.16 | 2560 × 224 | +0.4 | (texture bottom is 13 px above the walk line) | the whole texture |
| 3 | `atmosphere_fog_far` (`fog_far.png`) | atmosphere | 0.28 / 0.24 | 2048 × 96 | −0.2 | 90 | lower ~2.8 units |
| 4 | `background_rooftops` (`rooftops.png`) | background | 0.45 / 0.40 | 3200 × 208 | −0.4 | 195 | from the walk line up |
| 5 | `atmosphere_smoke` (`smoke.png`) | atmosphere | 0.50 / 0.45 (drifts left 0.12 unit/s) | 2048 × 192 | +1.0 | (bottom is 32 px above the walk line) | the whole texture |
| 6 | `atmosphere_fog_near` (`fog_near.png`) | atmosphere | 0.55 / 0.50 | 2560 × 80 | −0.6 | 61 | lower ~1.9 units |
| 7 | `midground` (`mid.png`) | midground | 0.72 / 0.70 | 3840 × 240 | −1.1 | 205 | from the walk line up (≈ 6.4 units) |
| — | gameplay: road, tiles, hero, enemies | — | 1.00 | — | — | — | — |
| 8 | `foreground_rubble` (`foreground.png`) | foreground | 1.15 / 1.10 | 4096 × 224 | −3.4 | 115 | everything |

You may adjust sizes, factors and offsets if your composition needs it. Keep the order and the
factor ranges (foreground 1.10–1.20, midground 0.65–0.80, background 0.30–0.50 for the nearer
background and lower for the far city, sky 0.05–0.15). Write every change into the config (§6).

### 4.3 Foreground (draw first)

Nearby building edges, charred beams, facade fragments, posts, large debris, roof edges and wall
fragments, from the near side of the street. Rules:

- Mostly **below the walk line** (rows 115–224 in the default size), standing in front of the
  road's front face, so they never hide the hero's feet.
- At most one or two tall pieces per 64 units may rise above the walk line (a broken house corner,
  a post), passing in front of the hero only briefly. Never a continuous strip, never a black mask
  over the screen.
- Darker than gameplay, with a clear warm rim light from street fires and a dark contour so they
  separate from the road. Rich detail: they are the closest and largest things on screen.
- Long empty gaps between pieces (at least as wide as the pieces); 5–7 set pieces across the
  4096 px strip. Ideas from §2.4: the broken quoined corner of a burned house with joist stubs; the
  Neighbours' overturned water cart with a split barrel and a dropped bundle; a smouldering fallen
  roof truss across a masonry heap; a street shrine on a broken column (graffiti, candle, red mark
  over it); the plank barricade the hunters broke through; a fallen bell or statue fragment.

### 4.4 Midground

The street's own back row, in full detail but a step darker and lower in contrast than
gameplay: **gutted stone house shells** (sky visible through empty windows, broken gables, joist
sockets, soot plumes), **timber houses burned to the frame**, a ruined **Red Congregation chapel**
(empty lancet window with broken tracery, buttresses, a torn red banner) and an **Ink Coven house**
(ink poured over the stone, a violet lantern, a charity mark). Also the **Neighbours' patched
shelters** (boards, cloth roof, a lit doorway, a rain barrel), a **lookout post** on a ruin,
laundry lines between ruins, red marks beside broken doors, rubble heaps spilling forward.
Transparent pixels through windows and gaps let the rooftops layer show through.

### 4.5 Background rooftops

The burning quarter just behind the street: a dense, broken skyline of gables, bare rafters,
chimneys, a tower or two, three or four blocks still burning with smoke rising. Lower detail and
contrast than the midground, values closer to the smoke haze. Silhouettes must be clearly ruined.

### 4.6 Far city

The old city on the horizon, backlit by the glowing sky: cathedral spires (some broken), the
city wall and towers, an arcade bridge with the hunters' tiny convoy (§2.4), a few distant fires.
Very low contrast, cool haze, few warm points, almost silhouette.

### 4.7 Sky

Dirty grey sky, heavy smoke cloud masses lit from below by the fires, a weak orange glow low on
the horizon, darker overhead. Dithered bands are fine. No clear blue sky, no sun, no moon. It
must tile horizontally (1280 px).

### 4.8 Smoke and fog

`smoke.png`: three or four smoke columns rising from the burning quarter and bending with the
wind, built from volumetric masses with three or four values. `fog_far.png` and `fog_near.png`:
banks of smoke lying between depth planes with lumpy tops and solid bodies. They hide the base of
the plane behind them, which is what separates the depths on the posters. All three tile
horizontally and use binary alpha.

### 4.9 Composition checks for every layer

- It tiles: place two copies side by side and the seam must be invisible.
- No large empty areas; no area of the screen goes uncovered at any camera position (use
  `fillBelow` / `fillAbove` colours that match the layer's edge rows).
- The hero (cold teal, about 38 px tall) stays readable against every part of the midground and
  the rooftops.
- Depth reads in order: far planes are hazier, flatter and closer to the sky colour; near planes
  are darker and sharper.

---

## 5. Modular ruins sets

Reusable kits that all belong to the same architecture, for building rooms and future layers.
Fifteen sets, each with ten parts:

`base, left_end, middle, right_end, corner, top, bottom, damaged_variant, burnt_variant, smoke_variant`

(`middle` tiles horizontally with itself; `left_end` and `right_end` close a run with a broken
edge; `corner` is the quoined corner; `top` is the broken crown; `bottom` is the footing with
rubble; `smoke_variant` adds rising smoke above the burnt version.)

| set | size per part (px) |
|---|---|
| `burned_wall` | 64 × 96 |
| `collapsed_facade` (jettied timber storey over a stone ground floor) | 96 × 128 |
| `wooden_beam` | 96 × 16 |
| `floor_slab` (second-floor slab or crossbeam) | 96 × 24 |
| `rubble` (stone and timber rubble) | 64 × 32 |
| `window_opening` | 32 × 48 |
| `burned_door` | 32 × 64 |
| `destroyed_roof` | 96 × 48 |
| `staircase` | 64 × 64 |
| `cellar_fragment` | 64 × 64 |
| `sewer_wall` | 64 × 64 |
| `wet_stone_floor` | 64 × 16 |
| `smoking_object` | 32 × 48 |
| `charred_post` | 16 × 96 |
| `residential_room` (remaining residential room seen in cut-away) | 96 × 96 |

Deliver them as one atlas, `modules.png` + `modules.atlas` (format in §8.2), region names
`<set>_<variant>` (for example `burned_wall_left_end`), `index: -1`. Also deliver a review sheet,
`documents/art-previews/ruins-modules-sheet.png`: every set in one row and every variant in one
column, at 2× on a neutral background.

---

## 6. Parallax config file

Deliver `assets/config/parallax/ruins.json`. Keep the two particle layers exactly as below (the
game draws them). For every texture layer fill in all fields:

```json
{
  "version": 1,
  "enabled": true,
  "note": "short description of the layers",
  "locations": ["start", "ruins"],
  "pixelsPerUnit": 32,
  "cameraDistance": 20,
  "layers": [
    {
      "id": "sky", "group": "sky", "texture": "backgrounds/ruins/sky.png",
      "depth": 100, "parallaxFactor": 0.08, "verticalFactor": 0.04,
      "repeatMode": "repeatX", "verticalOffset": -0.4, "scale": 1, "tint": "#ffffff", "opacity": 1,
      "fillAbove": "#16131a", "fillBelow": "#9a4a2a",
      "transitionDepth": 100, "worldDepth": 230, "planeWidth": 40, "planeHeight": 11.25,
      "transitionVisibility": "backdrop"
    },
    {
      "id": "atmosphere_embers", "group": "atmosphere",
      "particles": {"kind": "embers", "count": 18, "speed": 0.55, "sway": 0.25, "size": 1, "band": [0.0, 6.0]},
      "depth": 20, "parallaxFactor": 0.8, "verticalFactor": 0.8,
      "repeatMode": "wrap", "verticalOffset": 0, "scale": 1, "tint": "#ffffff", "opacity": 1,
      "transitionDepth": 20, "worldDepth": 5, "planeWidth": 0, "planeHeight": 0, "transitionVisibility": "hidden"
    },
    {
      "id": "atmosphere_ash", "group": "atmosphere",
      "particles": {"kind": "ash", "count": 46, "speed": 0.35, "sway": 0.4, "size": 1, "band": [-0.5, 9.0]},
      "depth": -5, "parallaxFactor": 1.0, "verticalFactor": 1.0,
      "repeatMode": "wrap", "verticalOffset": 0, "scale": 1, "tint": "#ffffff", "opacity": 1,
      "transitionDepth": -5, "worldDepth": 0, "planeWidth": 0, "planeHeight": 0, "transitionVisibility": "hidden"
    }
  ]
}
```

Field meanings:

- `group`: one of `sky`, `background`, `midground`, `atmosphere`, `foreground`.
- `depth`: render order (larger is drawn first); positive is behind gameplay, negative in front.
- `driftX`: optional slow drift in units per second (smoke and fog).
- `scale` must stay 1.
- `tint` and `opacity` must stay `#ffffff` and 1. Bake colour into the pixels; uniform
  transparency would create blended colours outside the palette.
- `fillAbove` / `fillBelow`: hex colours from your palette matching the texture's top and bottom
  rows.
- Data for a future 2.5D turn into the alley:
  - `worldDepth`: the plane's distance behind the gameplay plane, in units, chosen so that
    `parallaxFactor ≈ cameraDistance / (cameraDistance + worldDepth)`. Use a negative value in
    front of gameplay.
  - `planeWidth` and `planeHeight`: the plane's size in units.
  - `transitionDepth`: the order during that turn.
  - `transitionVisibility`: one of `backdrop`, `plane`, `fade`, `hidden`.

---

## 7. The hero

### 7.1 Who he is

A young man of the ruins, the son of a factionless family, raised among violence, poverty and
feuding factions. He is not chosen, not noble, not a cultist and has no special blood. At the
start of the game he has only what he could make or find: an **improvised spear** (a pole with a
blade lashed to it, or a boat hook), patched clothes, a scarf or hood against smoke and ash. He
lives with his mother in a repaired ruin. He must:

- read instantly against grey-black ruins, orange fire and grey smoke;
- have a distinctive silhouette, recognisable in solid black at 25 % size;
- look native to this world (medieval, worn, handmade) and belong to no faction (no red, no
  violet, no brass);
- stay separate from the smoke and ash values (he is not grey);
- use a limited palette (about 20 colours, in your `hero3_*` ramps plus `skin`, `bone`,
  `leather`, `wood`, `steel`, `outline`). The cold teal of his coat or hood is his signature and
  the only saturated cool colour in the scene.

**Do not use the current hero sprite as a reference** (it is being replaced).

### 7.2 Silhouette gate ⛔

Draw three silhouette variants at game scale (and at 4× for viewing), each in solid black and in
colour, in idle, run, thrust and finisher end pose, over a crop of your midground. Starting ideas
from the previous review; improve them freely:

- **A, hooded scavenger:** a deep hood whose point swings behind him, a bone-coloured scarf, a long
  teal coat that flares in motion, a spear with a lashed blade.
- **B, bare-headed runner:** tied-back hair with a trailing bone headband, a short teal jacket,
  bandaged forearms, a satchel, a boat hook.
- **C, mantled watcher:** a cowl with a short teal mantle over a dark coat, a field bill (a hooked
  blade).

Present them to the user and wait for the choice. Build all animations on the chosen one.

### 7.3 Frame, scale and anchor

- Physics body: 1 × 1 unit = **32 × 32 px**, centred on the hero's position. Its bottom edge is
  the feet.
- Figure height about **38 px** (1.2 units) standing, head to sole. Width about 14–18 px for the
  body.
- **Every frame is 160 × 80 px** (5 × 2.5 units), the same canvas for every animation, with no
  trimming. The wide canvas is needed for the spear's reach (§7.5).
- **Anchor = (80, 77)**: the pixel column under the centre of the body and the **lowest pixel row
  of the feet** standing on the ground (rows count from the top, starting at 0). Rows 78–79 stay
  free for the outline and dust. The anchor is identical in every frame: feet on the ground sit on
  row 77 in every grounded frame.
- `pixelsPerUnit: 32`, `visualHeight: 2.5` (= 80 / 32, native scale).
- One-pixel dark outline (`outline` colour) outside the silhouette. Inner shapes are separated by
  shading, not by outlines.

### 7.4 Animation list (names and counts are read by the game code)

Frame selection rules, as the code implements them:

- **Attacks** (`light1`, `light2`, `finisher`) pick the frame by **phase and phase progress**,
  not by time. Within a phase, the frames listed in `phases` divide the phase evenly. Timings come
  from the combat data (below) and must not be changed.
- `block`, `hit`, `stomp`, `recover`, `dash_charge` and `dash_stop` are picked by **index**
  (fixed counts).
- Everything else plays by `frameDuration`, looping where `loop` is true.

| name | what | frames | frameDuration (s) | loop | notes |
|---|---|---|---|---|---|
| `idle` | standing, breathing, spear held upright | 4–6 (free) | 0.18 | yes | body bobs 1 px; no foot movement |
| `walk` | walk | 8 (free) | 0.09 | yes | feet must not slide: plant and lift; body dips 1–2 px on contact |
| `run` | sprint | 8 (free) | 0.06–0.07 | yes | top speed is 13 units/s (416 px/s), so the feet cannot be planted; draw long sprinting strides, strong lean, coat and scarf trailing |
| `jump` | rising | 2 (free) | 0.10 | no | takeoff, then rise |
| `fall` | falling | 2 (free) | 0.12 | yes | |
| `light1` | first light strike: a quick thrust | 5 (free) | — | no | phases startup [0,1], active [2,2], recovery [3,4]; startup 0.10 s, active 0.09 s, recovery 0.20 s |
| `light2` | second strike: an overhead cut coming down | 6 (free) | — | no | startup [0,1], active [2,3], recovery [4,5]; startup 0.07 s, active 0.12 s, recovery 0.29 s; also used for the jump attack |
| `finisher` | third strike: a sweep around the body ending in a **distinct final pose** (spear planted forward-down, wide stance) | 7 (free) | — | no | startup [0,1], active [2,3], recovery [4,6]; startup 0.10 s, active 0.15 s, recovery 0.68 s; recovery opens on the final pose |
| `block` | guard | **2** | 0.10 | no | 0 = guard held; 1 = shown while a block flash is active or the guard is broken (a recoil) |
| `hit` | hurt | **2** | 0.12 | no | 0 = staggered; 1 = just took damage |
| `stomp` | falling onto an enemy's head | **3** | 0.10 | no | 0 = dropping (first half of the fall), 1 = impact, 2 = tumbling off (start of recovery) |
| `recover` | getting up after the fall | **3** | 0.12 | no | kneel, half up, up |
| `death` | death | 6 (free) | 0.12 | no | reel, knees, topple, down; the last frame holds |
| `alley_turn` | **transition-ready pose**: turns from side view to seen from behind | 4 (free) | 0.12 | no | 0 = side view, last = back view; played backwards when leaving an alley |
| `dash_charge` | heavy charge: crouch before the dash | **2** | 0.06 | no | 0 then 1 within the first 12 % of a 0.30 s roll |
| `dash` | heavy dash: body leaning about 30°, coat flying back | 3 (free) | 0.05 | yes | middle 68 % of the roll; the roll moves at 16.25 units/s |
| `dash_stop` | heavy recovery: hard stop, front foot braced | **2** | 0.08 | no | 0 then 1 in the last 20 % of the roll |
| `climb` (optional) | climbing a ladder, back to the viewer | 4 | 0.12 | yes | the game does not use it yet; the developer will hook it up |

Descents between floors reuse `walk` and `fall`.

Attack design rules:

- **startup**: a readable wind-up; the weapon goes back.
- **active**: one or two frames with a short smear trail; the weapon is inside the hitbox.
- **recovery**: returns to the guard; it never repeats an active frame.
- The finisher's final pose differs from every other pose. The dash has three beats: compress,
  travel, brace.

### 7.5 Hitboxes the drawing must match

Measured from the anchor (80, 77), in pixels. "Forward" is to the right; "up" is toward smaller
row numbers. In the active frames the weapon and its smear must cover the zone.

| attack | zone forward of the feet | zone above the feet | rows | columns |
|---|---|---|---|---|
| `light1` | 17.6–46.4 px | 3.5–29.8 px | 47–73 | 98–126 |
| `light2` | 23.0–65.3 px | 10.6–31.7 px | 45–66 | 103–145 |
| `finisher` | 20.8–73.6 px (the drawing may sweep both sides) | 1.0–36.2 px | 41–76 | 101–153 |

A hurried strike shortens the reach to 85 %. Deliver
`documents/art-previews/hero-v3-hitbox-overlay.png`: every active frame with its zone outlined.

### 7.6 Metadata

Deliver `assets/config/hero_animations.json`:

```json
{
  "version": 1,
  "sprite": "hero-v3",
  "frame": [160, 80],
  "anchor": [80, 77],
  "pixelsPerUnit": 32,
  "visualHeight": 2.5,
  "facing": "right",
  "feetPosition": [80, 77],
  "animations": {
    "light1": {
      "frames": 5, "frameDuration": 0.083, "loop": false,
      "weaponTip": [[x, y], [x, y], [x, y], [x, y], [x, y]],
      "attackOrigin": [[x, y], ...],
      "phases": {"startup": [0, 1], "active": [2, 2], "recovery": [3, 4]},
      "notes": "..."
    },
    "dash": {
      "frames": 3, "frameDuration": 0.05, "loop": true,
      "weaponTip": [[x, y], [x, y], [x, y]],
      "dashOrigin": [[x, y], ...]
    }
  }
}
```

- `weaponTip`: one `[x, y]` per frame, the pixel of the spear's point in frame coordinates (from
  the top left). It is required for **every** animation: the game draws the combo-window glint
  there.
- `attackOrigin`: per frame of each attack, the pixel where the strike starts (the hands). Used by
  tools and the future 2.5D turn.
- `dashOrigin`: per frame of the dash animations, the body's centre of mass.
- Every animation needs `frames`, `frameDuration` and `loop`. Attacks also need `phases`
  (first and last frame index, inclusive). `notes` is optional.

### 7.7 Atlas

`assets/actor/hero-v3/hero-v3.png` + `hero-v3.atlas`, in the format of §8.2. One region per
frame, region name = animation name, `index` = frame number starting at 0, every region the full
160 × 80 (no trimming: `orig` = `size`, `offset: 0, 0`). Leave 2 px of padding between regions.
Use a power-of-two page of at most 4096 × 4096. Also deliver
`documents/art-previews/hero-v3-sheet.png`: one row per animation, at 2×, with a ground line.

Checks: feet on row 77 in every grounded frame; no foot sliding in `walk`; consistent
proportions, palette and canvas; sharp pixels; the weapon matches the hitboxes; readable against
the midground at 1×.

---

## 8. File formats

### 8.1 PNG

8-bit RGBA, non-interlaced, binary alpha, palette colours only, exact sizes as listed. No colour
profile or gamma tricks. Fully transparent pixels should be stored as `(0, 0, 0, 0)`.

### 8.2 libGDX texture atlas (text format, exactly like this)

```

hero-v3.png
size: 2048, 1024
format: RGBA8888
filter: Nearest, Nearest
repeat: none
idle
  rotate: false
  xy: 0, 0
  size: 160, 80
  orig: 160, 80
  offset: 0, 0
  index: 0
idle
  rotate: false
  xy: 162, 0
  size: 160, 80
  orig: 160, 80
  offset: 0, 0
  index: 1
```

- The file starts with an empty line, then the page file name.
- `xy` is the top-left corner of the region on the page (y grows downward).
- For the modules atlas, region names are `<set>_<variant>`, sizes are the part sizes, and
  `index: -1`.

---

## 9. Producing the images with an image generator (process requirements)

Image generators output large images with fake pixels, blur and drifting colours. Every image must
be rebuilt into true game pixel art before delivery:

1. **Generate** the scene or sprite in a pixel-art style with an orthographic side view, using
   the brief's content. Generate several variants and pick the best composition.
2. **Rebuild the pixel grid.** Find the generated "pixel" size, then rebuild the image at the real
   size (for example 3840 × 240 for the midground) by sampling the centre of each block. Never
   downscale with smoothing.
3. **Map to the palette.** Map every pixel to the nearest palette colour (compare in a perceptual
   colour space, for example OKLab or CIELAB). No colours outside the palette may remain.
4. **Clean up by hand, pixel by pixel.**
   - Remove orphan pixels and noise.
   - Straighten stepped diagonals, close outlines and fix broken shapes.
   - Remove every artefact that has no meaning: fake letters, melted objects, impossible
     structures, extra limbs, repeated clones.
   - Make light come from the top left everywhere, and give every lit area its fire.
   - Ruins must look ruined (§2.5); stone must not read as brick.
5. **Make it tile.** Repaint the seam so that the last column meets the first, then check two
   copies side by side.
6. **Binary alpha.** Threshold the alpha, then redraw any silhouette edge that became jagged.
7. **Consistency across the hero's frames.**
   - Draw the key poses first, then the in-between frames on the same 160 × 80 canvas, using the
     previous frame as the base.
   - Keep proportions, palette, outline, the anchor and the weapon length identical across frames.
   - Generators are poor at frame-consistent animation, so expect to redraw the in-betweens by
     hand rather than generating each one.
8. **Review** every asset at 1× and 4×, in a test composite over the road and against the other
   layers.

Keep the prompts you used and the source generations in `source/` inside the delivery zip.

---

## 10. Test composites to show at each gate

For each layer, render a 640 × 360 frame (and a 1280 × 720 frame at 2×) the way the game does
(§4.1). Composite all finished layers plus the attached road texture (repeated along the walk
line, its top surface on the walk line) and a hero frame standing on the walk line at x = 320.
Show three camera positions (x = 0, 300 and 900 px) and one camera raised by 3 units. Deliver the
final one as `documents/art-previews/ruins-parallax-composite.png`.

---

## 11. QA checklist (run it, report the results)

Write and run a script that checks, for every delivered PNG:

- the exact size;
- RGBA, alpha only 0 or 255;
- every opaque pixel is a palette colour;
- horizontal layers tile (the first and last columns continue each other; also show it visually);
- the atlas regions are inside the page and do not overlap, and their counts match the JSON frame
  counts;
- every hero animation in §7.4 exists with the right fixed counts;
- feet on row 77 in grounded frames;
- `weaponTip` present for every frame;
- the active frames cover their hitboxes.

Then check by eye: ruins look ruined; no bricks; no meaningless objects; no fake text; story
details from §2.4 present and spread out; depth order reads; the hero is readable everywhere;
nothing hides the hero's feet; no area of the screen stays empty.

---

## 12. Delivery (one zip, repository paths)

```
assets/art/ruins-palette.json
assets/art/ruins-palette.gpl
assets/backgrounds/ruins/sky.png
assets/backgrounds/ruins/far.png
assets/backgrounds/ruins/fog_far.png
assets/backgrounds/ruins/rooftops.png
assets/backgrounds/ruins/smoke.png
assets/backgrounds/ruins/fog_near.png
assets/backgrounds/ruins/mid.png
assets/backgrounds/ruins/foreground.png
assets/backgrounds/ruins/modules.png
assets/backgrounds/ruins/modules.atlas
assets/config/parallax/ruins.json
assets/actor/hero-v3/hero-v3.png
assets/actor/hero-v3/hero-v3.atlas
assets/config/hero_animations.json
documents/RUINS_ART_DIRECTION.md
documents/art-previews/ruins-art-direction.png
documents/art-previews/ruins-modules-sheet.png
documents/art-previews/ruins-parallax-composite.png
documents/art-previews/hero-v3-silhouettes.png
documents/art-previews/hero-v3-sheet.png
documents/art-previews/hero-v3-hitbox-overlay.png
source/   (prompts, raw generations, layered or .aseprite working files, the QA script and its output)
```

Do not include or change anything else: not the road, the gameplay tiles, the enemy sprites or
any code.

After delivery the developer will:

- point the game at `hero-v3`;
- run the regression tests;
- capture the game for review;
- report any technical mismatch back to you through the user.

---

## 13. Attachments

- `posters/`: the quality and mood reference (city, red-congregation, ink-coven,
  mechanical-congregation, bandit-gangs). Match the density, lighting and depth; do not copy them.
- `road/ruins-road-A.png`, `B`, `C`: the approved road (512 × 1024 each, drawn at 256 px per unit,
  its walking surface 38 px below the top of the image). It must stay unchanged; match its palette
  and light.
- `game-capture-layout.png`: the game at 1280 × 720 (2×). It shows the road, a gameplay stone
  tile ledge, the current placeholder hero and the camera framing. **Its background layers are
  the rejected attempt: use it only for layout and scale.**
- `ruins-palette.json`: the current palette (keep the ramps listed in §3.1).
- `parallax-ruins.json`: the current config (format reference).
- `hero_animations.current.json`: the current hero metadata (format reference only; the new
  frame and anchor values are in §7.3).
