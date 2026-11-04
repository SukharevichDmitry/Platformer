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
    # Head stomp: soft low thump with a short squeak of leather.
    s["head_stomp"] = g.finish(g.mix((g.sweep(150, 70, .14) * g.decay(.14, 24), 0, 1.0),
                                     (g.sweep(900, 600, .06) * g.decay(.06, 50), .02, .25)), .65)
    # Alert: short rising two-note ping, clearly "noticed you".
    ping = g.np.sin(2 * g.np.pi * 700 * g.time(.07)) * g.decay(.07, 40)
    ping2 = g.np.sin(2 * g.np.pi * 1000 * g.time(.09)) * g.decay(.09, 35)
    s["enemy_alert"] = g.finish(g.mix((ping, 0, .8), (ping2, .06, 1.0)), .5)
    # Overseer: rising shimmer while casting, bell chime when the buff lands.
    t = g.time(1.0)
    shimmer = g.sweep(300, 900, 1.0, (1.0, .4)) * g.swell(1.0, .85) * (.6 + .4 * g.np.sin(2 * g.np.pi * 9 * t))
    s["overseer_cast"] = g.finish(g.mix((shimmer, 0, .8), (g.highpass(g.noise(1.0), 3500) * g.swell(1.0, .9), 0, .15)), .5)
    t = g.time(.5)
    bell = sum(a * g.np.sin(2 * g.np.pi * f * t) for f, a in ((880, 1.0), (1320, .5), (1760, .3))) * g.decay(.5, 7)
    s["overseer_buff"] = g.finish(bell, .55)
    # Breaker: low groan to telegraph, heavy ground thump for the slam.
    s["breaker_windup"] = g.finish(g.mix((g.sweep(70, 140, .7, (1.0, .5)) * g.swell(.7, .85), 0, .9),
                                         (g.lowpass(g.noise(.7), 500) * g.swell(.7, .8), 0, .5)), .6)
    s["breaker_slam"] = g.finish(g.mix((g.sweep(90, 28, .55, (1.0, .5)) * g.decay(.55, 6), 0, 1.0),
                                       (g.lowpass(g.noise(.4), 900) * g.decay(.4, 11), 0, .8),
                                       (g.bandpass(g.noise(.08), 1500, 5000) * g.decay(.08, 60), 0, .5)), .95)
    # Hurried strike: a choppy, uneven swish that cuts off plus a dull scuff of a slipping foot.
    t = g.time(.24)
    choppy = g.highpass(g.noise(.24), 900) * g.decay(.24, 14) * (.55 + .45 * g.np.sign(g.np.sin(2 * g.np.pi * 38 * t)))
    scuff = g.lowpass(g.noise(.12), 1400) * g.decay(.12, 26)
    stumble = g.sweep(210, 120, .1) * g.decay(.1, 30)
    s["slash_hurried"] = g.finish(g.mix((choppy, 0, .8), (scuff, .06, .7), (stumble, .1, .5)), .6)
    # Combo continuation: short bright two-note tick that says "well timed".
    t = g.time(.16)
    tick = (g.np.sin(2 * g.np.pi * 1180 * t) + .5 * g.np.sin(2 * g.np.pi * 1770 * t)) * g.decay(.16, 24)
    t2 = g.time(.2)
    chime = (g.np.sin(2 * g.np.pi * 1570 * t2) + .4 * g.np.sin(2 * g.np.pi * 2355 * t2)) * g.decay(.2, 18)
    s["combo_chain"] = g.finish(g.mix((tick, 0, .7), (chime, .07, .8)), .5)
    # Breakthrough: a rising, tightening hum for the charge and a short bright blip when it is ready.
    t = g.time(.75)
    hum = g.sweep(90, 430, .75, (1.0, .5)) * g.swell(.75, .9)
    shimmer = g.highpass(g.noise(.75), 2500) * g.swell(.75, .95) * .35
    s["breakthrough_charge"] = g.finish(g.mix((hum, 0, .9), (shimmer, 0, .5)), .6)
    t = g.time(.14)
    blip = (g.np.sin(2 * g.np.pi * 1480 * t) + .5 * g.np.sin(2 * g.np.pi * 2220 * t)) * g.decay(.14, 22)
    s["breakthrough_ready"] = g.finish(blip, .55)
    return s


if __name__ == "__main__":
    g.OUT.mkdir(parents=True, exist_ok=True)
    for name, data in build_combat().items():
        g.save(name, data)
        print(f"wrote {name}.wav  {len(data) / g.SR:.2f}s")
