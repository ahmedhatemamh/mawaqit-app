# Lessons learned

Newest first. Each entry: what happened, the lesson, and the rule that follows.

## 2026-11-23 - Data pipeline designed before checking a simpler path

- What happened: phases 1-3 designed a scraper, a JSON layout and a publishing flow before checking whether prayer times could simply be computed. A 1-minute match with ESA using the standard Egyptian method showed the scraper may not be needed as the main source.
- Lesson: the cheapest possible approach must be ruled out with a quick test before designing infrastructure.
- Rule: before designing any data ingestion, run a small prototype of the simplest alternative (calculation, official API, static file) and show the comparison.

## 2026-11-23 - Phases planned one at a time with no full roadmap

- What happened: each phase was proposed only after the previous one finished, so there was no view of the whole project (Android app, notifications, widgets, release).
- Lesson: phase-by-phase approvals need a full roadmap behind them.
- Rule: keep a full roadmap of all phases (goal, deliverable, approval gate) and update it whenever a phase changes the picture.

## 2026-11-23 - Generated data proposed inside the app repo

- What happened: the phase-3 plan put generated JSON (out/) in mawaqit-app, mixing code and data, although mawaqit-data exists for that.
- Lesson: generated data and code have different life cycles.
- Rule: generated data is published only to mawaqit-data; mawaqit-app holds code and small test fixtures only.
