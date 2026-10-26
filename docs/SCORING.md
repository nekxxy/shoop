# Shoop scoring formulas

All scores are computed on-device from recorded heart rate. They are original,
transparent approximations - not WHOOP's proprietary algorithms, and not medical
measurements.

## Strain (0 - 21)

Cardiovascular load accumulates per sample:

    intensity = clamp((bpm - restingHr) / (maxHr - restingHr), 0, 1.2)
    load += intensity^1.3 * minutes
    strain = 21 * (1 - e^(-load / 180))

A day at rest stays near 0. Sustained work near max HR approaches 21 asymptotically.

## Recovery (0 - 100)

Compares this morning's HRV and resting HR to your 14-day personal baseline:

    hrvScore = scaled ln(RMSSD) ratio vs baseline (0.85x .. 1.15x -> 0..1)
    rhrScore = 1 at/below baseline, 0 at baseline +9 bpm
    recovery = 100 * (0.7 * hrvScore + 0.3 * rhrScore)

Green >= 67, Yellow 34-66, Red < 34.

## Sleep (0 - 100)

    sleep = 100 * (0.65 * (asleep / sleepNeed) + 0.35 * restfulFraction)

`asleep` = minutes below resting HR + 8; `restfulFraction` = share of asleep time
below resting HR + 5. `sleepNeed` defaults to 8 h and is adjustable in the baseline.

## Zones

Zone 1-5 at 50/60/70/80/90% of max HR (default max 190, adjustable).
