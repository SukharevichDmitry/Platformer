"""Synthesize the shared melee/feedback sounds (hit, block, break, whiff, death, bump, raider cues).

Reuses the helpers of generate_archer_sfx.py so the whole combat set sits in one mix.
Run:  python3 tools/generate_combat_sfx.py   (needs numpy and scipy)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_archer_sfx import *  # noqa: F401,F403,E402
import generate_archer_sfx as g  # noqa: E402


def build_combat():
    s = {}
    # Hit on an enemy: meaty thud + short crack. Heavy: deeper, longer, with a low boom.
    thud = g.sweep(180, 70, .12) * g.decay(.12, 28)
    crack = g.bandpass(g.noise(.06), 1200, 4500) * g.decay(.06, 70)
    s["hit_light"] = g.finish(g.mix((thud, 0, 1.0), (crack, 0, .7)), .8)
    boom = g.sweep(110, 38, .3, (1.0, .4)) * g.decay(.3, 9)
    s["hit_heavy"] = g.finish(g.mix((boom, 0, 1.0), (g.bandpass(g.noise(.1), 800, 3500) * g.decay(.1, 40), 0, .8),
                                    (thud, .0, .6)), .9)
    # Metal block: bright inharmonic ring + scrape tick.
    t = g.time(.35)
    ring = sum(a * g.np.sin(2 * g.np.pi * f * t) for f, a in ((1320, 1.0), (2110, .6), (3180, .4))) * g.decay(.35, 14)
    tick = g.highpass(g.noise(.04), 3000) * g.decay(.04, 120)
    s["block_metal"] = g.finish(g.mix((ring, 0, .8), (tick, 0, 1.0)), .75)
    # Block break: metal shatter, descending ring plus noise burst.
    t = g.time(.5)
    shatter = g.highpass(g.noise(.5), 1500) * g.decay(.5, 9)
    fall = g.sweep(1800, 300, .5) * g.decay(.5, 7)
    s["block_break"] = g.finish(g.mix((shatter, 0, .9), (fall, 0, .6), (boom[:int(g.SR * .2)], 0, .7)), .9)
    # Hero block: dull cloth/leather thud, not metallic.
    s["block_hero"] = g.finish(g.mix((g.sweep(140, 90, .16) * g.decay(.16, 22), 0, 1.0),
                                     (g.lowpass(g.noise(.1), 1200) * g.decay(.1, 40), 0, .6)), .7)
    # Whiff: thin airy swish.
    s["whiff"] = g.finish(g.highpass(g.noise(.2), 1800) * g.swell(.2, .35), .45)
    # Death: falling body thud + descending groan.
    s["death"] = g.finish(g.mix((g.sweep(220, 55, .45, (1.0, .3)) * g.decay(.45, 6), 0, .9),
                                (g.lowpass(g.noise(.2), 700) * g.decay(.2, 20), .22, .7)), .85)
    # Obstacle bump: stony crunch.
    s["wall_bump"] = g.finish(g.mix((g.lowpass(g.noise(.18), 1800) * g.decay(.18, 30), 0, 1.0),
                                    (g.sweep(120, 60, .12) * g.decay(.12, 25), 0, .8)), .8)
    # Stagger pop: short dizzy double-tick.
    pop = g.np.sin(2 * g.np.pi * 900 * g.time(.05)) * g.decay(.05, 70)
    s["stagger_pop"] = g.finish(g.mix((pop, 0, 1.0), (pop, .07, .7)), .55)
    # Raider cues.
    s["raider_dash"] = g.finish(g.mix((g.lowpass(g.noise(.28), 2600) * g.swell(.28, .25), 0, 1.0),
                                      (g.sweep(300, 700, .2) * g.decay(.2, 12), 0, .25)), .6)
    s["raider_guard"] = g.finish(g.mix((g.sweep(500, 900, .08) * g.decay(.08, 30), 0, .6),
                                       (g.highpass(g.noise(.06), 2500) * g.decay(.06, 80), 0, .8)), .55)
    return s


if __name__ == "__main__":
    g.OUT.mkdir(parents=True, exist_ok=True)
    for name, data in build_combat().items():
        g.save(name, data)
        print(f"wrote {name}.wav  {len(data) / g.SR:.2f}s")
