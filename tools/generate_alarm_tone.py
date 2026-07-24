#!/usr/bin/env python3
"""Generate the bundled alarm tone: a rising two-tone chime that loops cleanly.

Public-domain / CC0 — generated from scratch, no samples. Run:
    python3 tools/generate_alarm_tone.py
then (optional) encode to ogg with ffmpeg. See README.
Output: tools/alarm_default.wav
"""
import math, struct, wave

SR = 44100          # sample rate
AMP = 0.62          # peak amplitude (0..1)

def tone(freq, dur, fade=0.008):
    n = int(SR * dur)
    out = []
    fade_n = max(1, int(SR * fade))
    for i in range(n):
        env = 1.0
        if i < fade_n:            # fade in
            env = i / fade_n
        elif i > n - fade_n:      # fade out — avoids clicks, keeps loop seamless
            env = (n - i) / fade_n
        out.append(AMP * env * math.sin(2 * math.pi * freq * i / SR))
    return out

def silence(dur):
    return [0.0] * int(SR * dur)

# Rising arpeggio (G5, B5, D6), then a short rest — the whole thing loops.
pattern = []
for f in (784.0, 988.0, 1175.0):
    pattern += tone(f, 0.16)
    pattern += silence(0.05)
pattern += silence(0.30)
# Repeat once so a single loop is ~1.6s and feels like a real alarm cadence.
samples = pattern + pattern

with wave.open("tools/alarm_default.wav", "w") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    frames = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples)
    w.writeframes(frames)
print(f"wrote tools/alarm_default.wav  ({len(samples)/SR:.2f}s, {len(samples)} samples)")
