# Mawaqit Misr - full plan

Status: draft for Ahmed's approval (2026-11-23). Items marked "N#" are new decisions awaiting approval; the plan assumes the recommended choice.

## Fixed decisions (Ahmed, 2026-11-23)

- D1 Prayer times are computed on the phone (Egyptian General Authority of Survey method). On the 1st, 11th and 21st of every Gregorian month the app goes online, compares with the official times, and corrects itself if different.
- D2 Cities: ESA city list with coordinates, plus optional GPS.
- D3 Tolerance vs ESA: zero minutes.
- D4 Hijri date: Dar al-Ifta is the only source. No manual adjustment ever. The app checks only on Hijri days 27, 28, 29, 30, 1, 2, 3.
- D5 Minimum Android 8 (API 26).
- Widgets: several designs, all available at once (all prayers + next prayer).
- Play Console: Ahmed's existing paid account is used.

## New decisions (recommended choice in bold)

- N1 Who reads the official sites: **GitHub Actions reads ESA and Dar al-Ifta and publishes small JSON files on data.ahmedhatem.work (HTTPS); the phone downloads those files.** Alternative: every phone scrapes ESA itself (fragile ASP.NET postback inside the app, an app update for every site change, many phones hitting a government site).
- N2 What "correct" means: **the official file for the month overrides computed times for every day it covers.** Computed times are the fallback only when no official file is available. This is what makes D3 (zero) hold even if the formula is off by a minute somewhere.
- N3 GPS mode: **compute for the GPS point, then apply the per-prayer difference (official minus computed) of the nearest ESA city for that day.** Alternative: GPS uses pure calculation with no correction.
- N4 Hijri route: **Actions reads the Hijri date shown in the header of https://www.dar-alifta.org/ar/prayer once a day and publishes hijri.json; the phone downloads it only on Hijri days 27-3.** Alternative: the phone calls http://di107.dar-alifta.org/api/HijriDate directly (plain HTTP, needs a cleartext exception, unreachable from cloud).
- N5 Phone offline during the whole 27-3 window: **keep counting days and show a small "unconfirmed" mark next to the Hijri date until Dar al-Ifta confirms.** No manual adjustment (D4).
- N6 Missed check (phone off on the 1st/11th/21st or a Hijri check day): **retry once a day until it succeeds, until the next scheduled check.**
- N7 Cross-check: **Actions compares ESA with Dar al-Ifta for Cairo; if they differ it opens a GitHub issue for Ahmed and changes nothing automatically.** ESA stays the official reference for times.
- N8 Where the scraper lives: **in mawaqit-data** (code + scheduled workflow + published files in one repo; the workflow publishes with its own token, no extra secret). Alternative: scraper in mawaqit-app pushing to mawaqit-data (needs a cross-repo token in Secrets).

## Answer to D6

D1 answered it, with one change: the scraper is no longer a weekly monitor. It becomes the official-data pipeline that the 1/11/21 checks and the Hijri checks depend on (N1, N4).

## Phases

Every phase: goal, inputs/outputs and risks presented first; starts only on Ahmed's approval; ends with tests shown and a STATUS.md entry. Code goes through a topic branch + PR + CodeRabbit.

| # | Phase | Output | Main risks |
|---|---|---|---|
| 0-2 | Infrastructure, source inspection | Done | - |
| 3 | Product spec | docs/spec.md: screens, notification behaviour (adhan sound, pre-reminder), language, the widget designs, the check schedules, what each screen promises | Open questions for Ahmed; no code |
| 4 | Calculation proof to zero | Script fetches ESA month by month (step via previous/next only, 1 request/second, about 300 requests once) for all cities over several months including summer (DST); city coordinate table fitted to reproduce ESA exactly; report: number of minutes differing per city | Zero may not be reachable for every city/day; N2 covers that. ESA may block repeated requests: stop and report |
| 5 | Data pipeline in mawaqit-data | Scheduled workflow: ESA current + next month on the 1st, 11th, 21st (before the phone checks); Dar al-Ifta Hijri daily; validation that fails the run instead of publishing bad data; Cairo cross-check (N7); files: cities.json, esa/YYYY-MM.json, hijri.json | Cloudflare may block GitHub runners; site layout changes; Hijri change time unknown (measured here) |
| 6 | Android skeleton + CI | Kotlin + Jetpack Compose, minSdk 26, build + unit tests + lint on GitHub Actions | Android SDK/Gradle hosts may need network approval in the cloud session |
| 7 | Core logic (Kotlin) | Prayer engine (golden tests vs phase-4 data), official-override (N2), GPS correction (N3), Hijri day counting + confirmation (N4, N5) | Two implementations (script + Kotlin) must agree: golden tests guard it |
| 8 | Sync | WorkManager jobs: times on 1/11/21, Hijri on days 27-3, daily retry (N6), all offline-safe | Android may delay background work |
| 9 | Main UI | Today screen, city/GPS choice, Hijri + Gregorian date, next prayer | - |
| 10 | Reminders | Exact alarms (Android 12-14 permissions), reschedule after reboot / time or zone change / DST, battery guidance for Xiaomi, Oppo, Realme | Hardest phase; OEM battery killers |
| 11 | Widgets | W1 small: next prayer + countdown. W2 strip: six prayers in a row, next highlighted. W3 large: Hijri + Gregorian date, six prayers, next highlighted, countdown. W4 tall: six prayers in a column. All four available; Ahmed picks/edits later | Widget refresh limits: updates driven by alarms at each prayer |
| 12 | Signing + release | New app entry in Ahmed's existing Play account, Play App Signing, upload keystore created on Ahmed's PC (step-by-step) and stored only in GitHub Secrets, privacy policy page, internal testing track | Release only on Ahmed's explicit order |
| 13 | Failure alerts | Any failed workflow or validation opens an issue; the app shows when data is stale | - |

## Evidence behind the plan

See STATUS.md entries of 2026-11-23: ESA postback works (all cities per day), formula matched ESA within 1 minute with rough coordinates, Dar al-Ifta header shows the official Hijri date over HTTPS, direct month jumps on ESA return 500.
