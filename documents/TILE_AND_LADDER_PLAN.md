с# Ladder and tile update proposal

Status: all options A–H and the one-column ladder contract approved and implemented on 2026-10-19. A later half-size terrain update is recorded at the end of this document.

## Current contract and observed problem

- At the time of this proposal, one tile was one world unit. `#` was a full solid cell, `_` a solid lower half-cell, and a space empty. The current half-size runtime contract is recorded below.
- The current ruins ladder is not a tile. `WorldRenderer` draws a roughly 0.38-unit-wide ladder using the `block` atlas region; `VerticalConnection.contains` accepts a hero whose center is within approximately 0.75 units of its center. Thus its usable width is far greater than its visible width.
- The bell-hall roof and watchtower floor use matching two-cell openings (`span: 2`). The ladder is centered on the boundary between those cells, so it appears to cut across adjacent blocks. A climb can start from either side while the hero is still visibly outside the ladder.
- The source sketch is the dark 32 × 32 stone `assets/block.png`; `assets/actor/block.png` is an identical copy. The atlas currently has one `block` region. The lower half-block reuses a crop of that region. `assets/palette.aseprite` is present, but no separate ladder or stone-variant texture exists.

## Proposed ladder contract (foundation work)

1. Place a ladder on the centerline of **one tile column**, not between two columns. Change the matching vertical doorway and floor/ceiling hole from two cells to one. Keep horizontal doorways two cells high. Update template parsing and reachability rules accordingly; do not loosen horizontal clearance rules.
2. Give the ladder one explicit visible rectangle and one activation zone derived from that rectangle. Proposed art width: about 0.35–0.45 world units. Proposed hero-center grab tolerance: about 0.3–0.35 units, allowing a small input margin without permitting grabs from a neighboring tile. Require actual vertical overlap and a fresh Up/Down intent to attach; merely crossing beside it must not auto-grab.
3. Keep the ladder nonsolid. Existing blocks retain their full collision fixtures; the one-column shaft is empty terrain. Do not create a wide hidden fixture or erase tiles at render time. Draw ladder segments only in validated empty shaft cells, with explicit bottom/top caps. Keep Left/Right travel and Space jump-off, and ensure a climb releases above the upper floor lip. If the hero moves beyond the grip zone, gravity resumes.
4. Build validation around the shared geometry: no solid tile intersects the ladder's art or grip zone; the one-cell shaft accommodates the 0.5-unit hero with clearance; upper-floor landing and re-entry from either side work. Test an attempted Up press just outside the visible ladder, stepping off the top, descending, lateral exit midway, and jump-off. Preserve discover-on-entry mini-map behavior.
5. Because the generated tile rows and doorway geometry change, bump the ruins generation profile version. Old saved generated runs must be invalidated by the existing versioned loader. Recheck 5,000 seeds, encounter placement, persistence, desktop traversal, and packaged smoke on JDK 17 / libGDX 1.12.1.

## Visual tile choices based on the stone sketch

The option wording below is the original proposal. All eight options were subsequently approved; G and H are no longer deferred.

All stone options below retain the sketch's small, dark masonry shapes, near-black mortar, and restrained muted highlights. Variants should read at the current 32-pixel-per-tile scale and remain visually distinct from empty space. A visual-only variant must **not** silently change a tile's collision or save semantics.

| ID | Option | Intended purpose | Physics / scope |
|---|---|---|---|
| A | Intact stone: base plus 2–3 shifted-brick/light-wear variants | Break up repeated wall and floor patterns while keeping a coherent ruins material | Visual only; same full-cell `#` collision. Deterministic selection from room seed and coordinates. Recommended first. |
| B | Edge and corner caps: top, exposed side, outside/inside corner | Make platforms and openings read as deliberately built shapes; remove abrupt repeated-square silhouettes | Visual only; same collision, chosen from adjacent tile occupancy. Recommended first. |
| C | Cracked and chipped masonry | Signal age and possible points of interest without promising destruction yet | Visual only for now; same collision. Do not use a dramatic “break here” mark unless a breakable mechanic is approved later. |
| D | Damp/mossy stone | Give lower ruins and shaded rooms a distinct mood and directional environmental storytelling | Visual only; same collision. Use sparse, muted green/blue accents so the hero and enemies remain readable. |
| E | Dedicated lower-half stone step and matching edge | Make existing `_` geometry unmistakably half-height and improve jump-route readability | Existing 1 × 0.5 solid collision; dedicated 32 × 16-style art, not a crop of the full block. Recommended first. |
| F | Background recess / broken arch infill | Add depth to large empty wall areas and identify landmarks without cluttering traversal | Nonsolid background decoration in a separate layer; must not resemble a walkable ledge or hidden opening. |
| G | One-way wooden or stone platform | Add vertical-routing flexibility and recoverable falls | **New gameplay tile**: top-only support/contact behavior, new generator legality and traversal tests. Defer unless specifically approved. |
| H | Breakable cracked block | Gate secrets or shortcuts with attacks | **New gameplay tile**: damage, destruction, persistence, and path validation. Defer unless specifically approved. |

The ladder needs its own narrow segment plus top/bottom art; it is foundation work, not an optional stone variant. Approved visual variations can be generated as bitmap derivatives of the existing sketch, then packed into the atlas. Keep source images and atlas outputs identifiable; remove neither existing `block.png` copy without checking build references. Prefer a renderer-side variant/layer rule over adding one JSON character per cosmetic texture. New physical behavior receives an explicit tile type, bounds, and tests.

## Suggested order and acceptance

This section records the original implementation order; current results are in the completion record below.

1. Approve the ladder contract and select tile IDs. Suggested small first set: **A + B + E**, with **C** if more ruin character is wanted. F–H can wait.
2. Implement one-column ladder geometry and tests first. Confirm the hero cannot attach from a neighboring cell, can climb both directions, step onto the upper floor, and jump/exit laterally.
3. Create the approved texture variants from the sketch, pack them into the atlas, and apply deterministic visual selection. Verify full/half visible edges match Box2D shapes and that art does not suggest false collision.
4. Run `core:check`, `lwjgl3:simulationRegressionTest`, `lwjgl3:generatedLocationTest`, `lwjgl3:generatedRunPersistenceTest`, and `lwjgl3:packagedSmokeTest`. Inspect the 5,000-seed report and perform a visible playtest of ladder attachment, exit, and stone readability at normal camera scale.

## Completion record

| Area | Status | Evidence / remaining work |
|---|---|---|
| One-column ladder and narrow grip | Implemented; automated pass | One-cell shaft, narrow grab zone, top landing, descent, side exit, and jump-off checked by generated-location tests. Hands-on feel review remains. |
| A–F stone/decorative variants | Implemented; automated pass | Separate ruins atlas, deterministic full-stone variants and edge caps, dedicated half-step art, decorative recesses. Real-GL camera test passes; inspect readability in a visible run. |
| G one-way platform | Implemented; automated pass | `=` tile has top-only collision, generated placements, jump-through/landing desktop tests, and route checks. |
| H breakable block | Implemented; automated pass | `B` tile breaks from a real X attack, destroys its own fixture, persists by tile ID, and is placed off the critical walking route. |
| Background wall infill | Implemented; automated pass | Dim non-solid masonry is drawn behind rooms and visible empty perimeter space. |
| End-to-end release checks | Automated pass; manual review pending | JDK 17 `core:check`, simulation, generated-location, generated-run persistence, interactive-tile, and packaged-smoke tests pass; 5,000 seeded ruins pass. Test the visible build for readability and feel. |

## End-to-end playtest handoff

Use the packaged build or `lwjgl3:run`. Enter ruins from the right of `start`, climb the bell-hall ladder into the watchtower, step off its top, descend, and try Up while standing beside (not on) the ladder. Try jumping up through a wooden `=` ledge and landing on it. Face a heavily cracked `B` block and press X within attack range; after reloading the same run, confirm it remains absent. Check the half-step art against its collision and look for dim masonry behind the walls without encountering invisible barriers. Share any precise seed, room, and screenshot where behavior or readability feels wrong.

## Screenshot review follow-up (2026-10-19)

The first hands-on screenshots revealed transparent gutters and incompatible side/corner caps, plus random per-cell masonry finishes that read as a patchwork. The atlas builder now fills the complete visual cell for solid blocks and packs the ladder closer to its narrow grip width. The initial correction removed all directional faces, which lost intended side and corner detail. A second correction adds one compatible nine-face masonry family (center, four sides, four corners) and selects by world-neighbor occupancy across room seams. The old incompatible cap art remains in the source atlas but is not selected. Non-solid exterior masonry continues beyond room bounds and darkens with distance. This is a visual compatibility rule, not a change to generated collision or doorway geometry. Recheck the supplied screenshots in a visible build; automated generation and packaged checks cannot judge the final look.

## Connected masonry follow-up (2026-10-20)

The first attempt at a seamless mural lost the crisp floor, wall, and corner shapes of the earlier ruins sheet; it also enlarged and softened the stones. The original nine painted faces are restored using one fixed crop and nearest-neighbor reduction, so all faces retain the same scale and sharpness. The renderer selects their orientation from exposed top, right, bottom, and left neighbors in world coordinates, including across room seams. Sixteen baked combinations retain the painted stone ends on two-block hanging ledges and standalone blocks. Flat floors consistently use the same top face; vertical walls use the exposed left or right face; corners use their matching corner art. No drawn line or rectangular border is added. The dim room and outside backgrounds use the same masonry family. This is visual only; collision remains unchanged.

## Interior-facing stone light (2026-10-20)

Stone faces now receive an illuminated variant only when the neighboring cell is open space inside a room. Exterior cells beyond every room are unlit; an open cell in an adjacent room still lights a face across the seam. An intact full-height breakable block blocks that light until removed. Bottom-facing stone uses the painted masonry turned toward the room, and the underside of a thin hanging run brightens its existing lower bricks without replacing their shapes. Small painted corner highlights continue the adjacent exposed face at a three-stone inside joint. `core:validateStoneLighting` checks exterior walls, ceiling undersides, hanging stones, room seams, breakable removal, and both sides of the platform/pillar joint. The JDK 17 core, simulation, generated-location, and packaged-smoke checks pass. A visible playthrough is still useful for final art judgment.

## Connected corner blocks (2026-10-20)

The small corner overlays still left visible breaks where a ceiling meets a wall and where a hanging pillar joins a platform. The ruins atlas now contains a complete block variant for each combination of room-facing sides and inside corners. The renderer selects one variant per stone cell from its neighboring room cells. The variants retain the original painted masonry scale, connect the bottom face across a hanging platform, and blend corner stones into the two adjoining faces. Outside-facing edges remain dark. The stone-lighting check covers the ceiling/wall joints, three-sided hanging ends, and a standalone four-sided block; a visible playthrough should judge the final look at the normal camera scale.

The raised-platform screenshot exposed one more three-stone joint: its supporting floor block touches open room space only diagonally, so it has no exposed face of its own. The selector now uses the matching upper corner variant on each support block, connecting it to the lit floor face beside the platform. Open diagonals outside all rooms remain dark. The stone-lighting check covers both support corners, and the existing atlas already contains their baked art; no texture change is needed.

## Partial-ledge collision seams (2026-10-20)

Adjacent wooden one-way slabs and stone half-blocks now share a horizontal Box2D rectangle per uninterrupted run, including runs across a room boundary. This removes internal collision edges that could catch the hero while walking. The one-way slab's merged fixture retains its top-only support rule; breakable tiles keep individual fixtures so they can still be removed. The native interactive-tile check exercises both directions across a wooden run, upward passage and landing, and a run crossing two rooms.

## Half-size terrain follow-up (2026-10-26)

Each runtime terrain cell is now 0.5 × 0.5 world units. Authored and generated rooms retain their world-space dimensions; legacy JSON characters expand into a 2 × 2 cell area at load time. A lower-half tile is 0.5 × 0.25, a one-way ledge is 0.5 units wide with its top at the existing world height, and breakable blocks are removed and saved by fine-grid cell coordinate. Generated chambers receive paired platform and ceiling motifs with half-cell stone ribs. The ladder opening, hero size, and room borders remain at their previous world coordinates. JDK 17 core, generated-location, interactive-tile, persistence, simulation, camera, and packaged-smoke checks pass. Judge the smaller masonry scale and each room's silhouette in a visible run before treating the art composition as final.
