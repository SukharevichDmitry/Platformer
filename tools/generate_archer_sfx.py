"""Synthesize the archer's placeholder sound effects into assets/sfx.

Deterministic (fixed seed) 16-bit mono 22.05 kHz WAVs, matching the existing slash sounds.
Each archer action has its own sound so it can be recognised by ear as well as by sight:

  bow_draw_standard / _charged / _volley   preparation, longer and brighter per shot type
  arrow_shot_standard / _charged / _volley release, heavier per shot type (volley = three twangs)
  arrow_hit_hero                           dull fleshy thud with a sharp tick
  arrow_hit_wall                           bright wooden/stone clack with a shaft rattle
  archer_melee_windup / _strike            blade rasp, then a blade swish
  archer_retreat_dash                      soft cloth and boot scuff
  archer_dodge                             airy high "shing"

Run:  python3 tools/generate_archer_sfx.py   (needs numpy and scipy)
"""

import wave
from pathlib import Path

import numpy as np
from scipy.signal import lfilter

SR = 22050
OUT = Path(__file__).resolve().parents[1] / "assets/sfx"
rng = np.random.default_rng(20261103)


def time(duration):
    return np.arange(int(SR * duration)) / SR


def noise(duration):
    return rng.uniform(-1.0, 1.0, int(SR * duration))


def lowpass(x, cutoff):
    a = 1.0 - np.exp(-2.0 * np.pi * cutoff / SR)
    return lfilter([a], [1.0, a - 1.0], x)


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, low, high):
    return lowpass(highpass(x, low), high)


def sweep(f0, f1, duration, harmonics=(1.0,)):
    """Sine (plus optional harmonics) gliding exponentially from f0 to f1."""
    t = time(duration)
    freq = f0 * (f1 / f0) ** (t / duration)
    phase = 2.0 * np.pi * np.cumsum(freq) / SR
    return sum(amp * np.sin((i + 1) * phase) for i, amp in enumerate(harmonics))


def decay(duration, rate, attack=.002):
    t = time(duration)
    return np.minimum(1.0, t / attack) * np.exp(-rate * t)


def swell(duration, peak=.6):
    """Rise to a peak at `peak` of the duration, then fall (for draws and whooshes)."""
    t = time(duration) / duration
    return np.where(t < peak, (t / peak) ** 1.5, ((1.0 - t) / (1.0 - peak)) ** 1.2)


def pad(x, length):
    return np.pad(x, (0, max(0, length - len(x))))[:length]


def mix(*parts):
    """Sum (signal, start_seconds, gain) layers."""
    end = max(int(SR * start) + len(sig) for sig, start, _ in parts)
    out = np.zeros(end)
    for sig, start, gain in parts:
        i = int(SR * start)
        out[i:i + len(sig)] += gain * sig
    return out


def twang(freq, duration=.22, bright=.3):
    t = time(duration)
    body = (np.sin(2 * np.pi * freq * t) + .5 * np.sin(2 * np.pi * 2.01 * freq * t) +
            .25 * np.sin(2 * np.pi * 3.02 * freq * t)) * decay(duration, 16)
    snap = highpass(noise(duration), 1800) * decay(duration, 90)
    return body + bright * snap


def creak(duration, f0, f1, wobble=14.0, grit=.35):
    """Stressed wood: a gliding tone with irregular amplitude chatter and a bit of rasp."""
    t = time(duration)
    tone = sweep(f0, f1, duration, (1.0, .45, .2))
    chatter = .55 + .45 * np.sin(2 * np.pi * wobble * t + 3.0 * np.sin(2 * np.pi * 3.1 * t))
    rasp = bandpass(noise(duration), 250, 1600) * grit
    return (tone * chatter + rasp) * swell(duration, .8)


LEVEL = .6  # overall trim: the shipped slash sounds peak near 0.3, so keep the archer in the same mix


def finish(x, peak=.8, fade=.01):
    x = lowpass(np.asarray(x, dtype=float), 7000)  # tame the hiss of the noise layers
    n = int(SR * fade)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    x = x / max(1e-9, np.abs(x).max()) * peak * LEVEL
    return x


def save(name, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(str(OUT / f"{name}.wav"), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(pcm.tobytes())


def build():
    sounds = {}

    # --- Bow draw: longer and brighter for charged shots, three nocks for the volley -----------------
    sounds["bow_draw_standard"] = finish(creak(.45, 120, 190), .55)
    charge_shimmer = sweep(500, 1700, .8) * (.5 + .5 * np.sin(2 * np.pi * 18 * time(.8))) * swell(.8, .95)
    sounds["bow_draw_charged"] = finish(
        mix((creak(.8, 100, 310, wobble=9, grit=.45), 0, 1.0), (charge_shimmer, 0, .35)), .65)
    click = highpass(noise(.03), 1500) * decay(.03, 160)
    sounds["bow_draw_volley"] = finish(
        mix((creak(.95, 110, 260, wobble=11), 0, 1.0), (click, .08, .9), (click, .3, .9), (click, .52, .9),
            (sweep(300, 900, .95) * swell(.95, .9), 0, .25)), .65)

    # --- Release: heavier per shot type; the volley is three overlapping twangs -------------------------
    whoosh = lambda d, lo, hi: bandpass(noise(d), lo, hi) * swell(d, .25)
    sounds["arrow_shot_standard"] = finish(
        mix((twang(190, .22), 0, 1.0), (whoosh(.18, 900, 3500), .01, .5)), .7)
    crack = highpass(noise(.06), 2500) * decay(.06, 55)
    sounds["arrow_shot_charged"] = finish(
        mix((twang(130, .34, .5), 0, 1.0), (crack, 0, .9), (whoosh(.3, 500, 4500), .01, .7),
            (sweep(90, 45, .3) * decay(.3, 14), 0, .8)), .85)
    sounds["arrow_shot_volley"] = finish(
        mix((twang(210, .22), 0, 1.0), (twang(165, .26), .03, 1.0), (twang(250, .22), .06, 1.0),
            (whoosh(.3, 800, 4000), .01, .6)), .8)

    # --- Impacts: unmistakably different from one another ----------------------------------------------
    thud = sweep(110, 45, .28) * decay(.28, 14)
    flesh = lowpass(noise(.2), 900) * decay(.2, 26)
    tick = bandpass(noise(.03), 1800, 5000) * decay(.03, 170)
    sounds["arrow_hit_hero"] = finish(mix((thud, 0, 1.0), (flesh, 0, .8), (tick, 0, .7)), .85)
    clack = bandpass(noise(.06), 1500, 5500) * decay(.06, 70)
    rattle_t = time(.2)
    rattle = np.sin(2 * np.pi * 360 * rattle_t + 5 * np.sin(2 * np.pi * 42 * rattle_t)) * decay(.2, 20)
    ring = np.sin(2 * np.pi * 1250 * time(.14)) * decay(.14, 38)
    sounds["arrow_hit_wall"] = finish(mix((clack, 0, 1.0), (ring, 0, .45), (rattle, .02, .5)), .7)

    # --- Melee: rasp to telegraph, swish to strike (no bow/string sounds anywhere in these) --------------
    rasp = bandpass(noise(.24), 700, 4200) * (time(.24) / .24) ** 1.4
    sounds["archer_melee_windup"] = finish(
        mix((rasp, 0, 1.0), (sweep(520, 980, .24) * swell(.24, .9), 0, .25)), .55)
    swish_t = time(.22)
    swish_src = highpass(noise(.22), 900)
    bright = np.exp(-9 * swish_t)  # the blade sound starts bright and darkens as it passes
    swish = (bright * lowpass(swish_src, 5000) + (1 - bright) * lowpass(swish_src, 1500)) * swell(.22, .3)
    sounds["archer_melee_strike"] = finish(
        mix((swish, 0, 1.0), (np.sin(2 * np.pi * 2300 * time(.12)) * decay(.12, 30), .05, .3)), .8)

    # --- Retreat/evade: soft cloth scuff vs. airy sparkle ------------------------------------------------
    scuff = lowpass(noise(.26), 2200) * swell(.26, .3)
    sounds["archer_retreat_dash"] = finish(
        mix((scuff, 0, 1.0), (lowpass(noise(.05), 600) * decay(.05, 60), 0, .8),
            (lowpass(noise(.05), 600) * decay(.05, 60), .16, .5)), .5)
    shing = sweep(1500, 3000, .24) * decay(.24, 13)
    sounds["archer_dodge"] = finish(
        mix((highpass(noise(.24), 3000) * swell(.24, .2), 0, .8), (shing, 0, .6)), .55)

    return sounds


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for name, data in build().items():
        save(name, data)
        print(f"wrote {name}.wav  {len(data) / SR:.2f}s")
