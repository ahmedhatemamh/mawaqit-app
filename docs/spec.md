# Mawaqit Misr - product spec (phase 3, draft for Ahmed's approval)

Language of the app: Arabic only, right-to-left. Package: work.ahmedhatem.mawaqit. Minimum Android 8 (API 26).
Items marked "Q#" are open questions at the end; the draft assumes the recommended answer.

## 1. Purpose

A light, simple app with an Islamic look that fully replaces the printed yearly prayer timetable distributed to the public: official Egyptian prayer times (ESA method, checked against Dar al-Ifta) with the Hijri date from Dar al-Ifta, for every day of the year, on the phone, offline. Ahmed: the user should never need the printed timetable.

## 2. Principles

- Zero setup: first launch shows today's times immediately (location automatic, nearest ESA city).
- The user only does two things: choose widgets, and set reminders. Nothing else is asked.
- Everything else is automatic: location, data checks, corrections, alarm rescheduling.
- No account, no ads, no analytics, location never leaves the phone.

## 3. Screens

### 3.1 Today (home)
- City name (automatic, with a small way to switch city), Gregorian date, Hijri date (with a small "unconfirmed" mark when not yet confirmed by Dar al-Ifta).
- Six rows: fajr, sunrise, dhuhr, asr, maghrib, isha. The next prayer is highlighted with a live countdown. Sunrise is shown as a time, not a prayer, with a small line under it: the end of the prohibited time, sunrise + 25 minutes (Ahmed's value, one constant in the code, not a setting).
- Works with no internet.

### 3.2 Year timetable (replaces the printed paper)
- Month by month: for each day, Gregorian date, Hijri date, and the six times, in a table like the printed one. Swipe or arrows between months; the whole current year available, plus the next year once its data exists.
- Uses the official data where available and the computed values elsewhere; the user is not told which (the numbers must match the official ones, D3 = zero difference).
- Hijri dates: confirmed ones from Dar al-Ifta; dates for later months are estimates and carry a small mark (see Q2), because the Hijri month start is announced only around the 29th/30th.
- Works offline; the data is already on the phone.
- No share/print option (Q3 answered: not needed if the app works as Ahmed imagines).

### 3.3 Reminders
- Per prayer (and sunrise, and the end of the prohibited time after sunrise): any number of reminders. Each reminder: before and/or after the time, by any number of minutes the user types (for example 30), on or off. Different reminders for different prayers, and the same reminder can be applied to several prayers or all of them. Any reminder can be removed or turned off from the app.
- Two kinds of reminder moment (Ahmed's design): a reminder BEFORE a prayer asks "هل صليت؟" (did you pray?) with two buttons in the notification: Yes stops it, No repeats it every N minutes until the next prayer's time. A reminder AFTER the prayer time says "قم للصلاة" (get up to pray). N is a number Ahmed/the user edits (default 10). No prayer history is stored for now.
- Alert type, chosen per reminder: the phone's notification sound, the built-in adhan, or any ringtone/song the user picks from the phone (R2); optionally the camera flash, which blinks on and off like a phone call or notification flash alert for as long as the alert is active, and stops when the alert ends or is dismissed (R3).
- Android permissions (notifications, exact alarms) are requested in plain words only at the moment a reminder is created; battery-saving guidance (Xiaomi/Oppo/Realme) appears only if the phone is known to block alarms.

### 3.4 Widgets (all available, the user chooses)
- W1 small: next prayer + countdown.
- W2 strip: six times in a row, next highlighted.
- W3 large: Hijri + Gregorian date, six times, next highlighted, countdown.
- W4 tall: six times in a column.
- Widgets update at every prayer time and at midnight, no manual action.

### 3.5 City / location
- Automatic by phone location (nearest ESA city + GPS correction, N3). The user can switch to choosing a city by name from the ESA list (83 cities). No other settings.

### 3.6 Data status (small, inside Today)
- A discreet line "last updated ..." shown only if data is stale for more than a month; no screen of settings.

## 4. Data behaviour (what each promise needs behind it)

| Promise | Behind it |
|---|---|
| Times are right with no internet | Prayer engine on the phone (golden tests against official data) |
| Times match the official ones exactly | Checks on the 1st, 11th, 21st: official data replaces computed times where they differ; daily retry if missed |
| Hijri date is Dar al-Ifta's | Checks only on Hijri days 29, 30, 1, 2, 3; otherwise count by itself; "unconfirmed" mark until confirmed |
| Year timetable shows the whole year | Computed locally for the year; official data overrides; Hijri estimated beyond the confirmed month |
| Reminders ring on time | Exact alarms, rescheduled after reboot, time/zone change, app update, city change |
| Widgets show the next prayer | Updated by alarms at each prayer time |

## 5. Acceptance checks (to be turned into tests)

- Fresh install, no internet: Today shows six times and the Hijri date within 2 seconds.
- Year timetable: every day of the year has six valid, ordered times; Hijri dates monotonic; a month matches official data exactly where official data exists.
- Reminders: created reminder fires at the right minute; survives reboot; survives city change; two reminders on the same prayer both fire.
- Widgets: next prayer changes at the prayer time; after midnight the date and Hijri update.
- Data checks: wrong/missing official file is rejected and the old data stays; daily retry after a missed check.
- No data sent that identifies the user; location is not stored outside the phone.

## 6. Out of scope (for now)

Qibla, Quran, adhkar, other countries, user accounts, manual date adjustment (forbidden by Ahmed's rule D4), themes, more languages.

## 7. Ahmed's answers (2026-11-23)

- Q1 approved (green/gold Islamic look, light and dark automatic), open to change after testing.
- Q2 option (a): estimated Hijri dates for later months, with a small mark.
- Q3 no share/print.
- Q4 as described in 3.3: any number of minutes, several reminders, before and/or after, different per prayer or the same for several/all, plus the "did you pray?" follow-up.
- Q5 phone notification sound or adhan sound, user chooses; also camera flash as an eye-catching alert.
- Q6 sunrise row has a small line under it with the prohibited time (sunrise + 25 minutes); reminders possible for both; any reminder can be cancelled in the app.
- Q7 name approved: Mawaqit Misr / مواقيت مصر.

New points to confirm (Ahmed):

- R1 "Did you pray?" meaning and repeat. My reading: at the offset the user chose before the next prayer, a notification asks about the prayer whose time is ending. Yes: stop. No: ask again every N minutes until the next prayer's time. For fajr the end is sunrise. Options for N: (a) fixed 10 minutes (recommended, simplest); (b) one number the user chooses, default 10. Nothing about the answer is stored or sent (no prayer history), unless Ahmed wants a history later.
- R2 "صوت انولة": I read it as the adhan sound; please confirm. A recording needs one we are allowed to use legally (own recording, or a free license); I do not copy one from the internet without permission.
- R3 Camera flash: blinks for at most 10 seconds or until dismissed, off by default, per reminder; uses the torch only (no camera access); some phones restrict it in the background or when the battery is low, so it can fail on some devices and the sound still plays.
- R4 Prohibited time after sunrise: shown as "until HH:mm" (sunrise + 25) under the sunrise row; it is reminder-capable like the prayers. No other prohibited times were requested, none added.

## 8. Original questions (answered above, kept for history)

- Q1 Look and feel: calm green/gold Islamic style with an Arabic calligraphy font, light and dark. Recommendation: yes, follows the phone's light/dark mode automatically.
- Q2 Hijri dates later in the year: Dar al-Ifta announces month starts only about a day before. Options: (a) show estimated Hijri dates for later months with a small mark (recommended, replaces the printed paper); (b) show Hijri only for the current confirmed month (less useful, but never wrong); (c) show estimated dates with no mark (risky: may be off by one day). The estimate uses the Umm al-Qura calendar, which can differ from Dar al-Ifta by a day.
- Q3 Share/print a month from the Year timetable (image or PDF)? Recommendation: not now (no speculative features); add later if you want it.
- Q4 "Fixed or variable offsets": do you mean (a) the user types any number of minutes for each reminder (recommended), or (b) the offset also changes by day or season, or (c) preset choices only (5, 10, 15, 30 minutes)?
- Q5 Adhan sound inside the app: (a) system notification sound only (recommended for v1, simplest, small app), (b) one built-in adhan recording, (c) several. A recording needs a recording we are legally allowed to use.
- Q6 Reminder before sunrise (end of fajr time): include as a reminder option? Recommendation: yes.
- Q7 App icon and name: "Mawaqit Misr" in Arabic "مواقيت مصر". Confirm the name; the icon is designed in a later phase.

Confirmed by Ahmed (2026-11-23):

- R1 The reminder before a prayer asks "did you pray?"; the reminder after it says "get up to pray". N (repeat interval) is a number he edits himself. No prayer history for now.
- R2 Ahmed will look for an open-source (freely licensed) adhan recording himself; until then, and always as an option, the user can pick any ringtone or song from the phone. The app never ships a recording whose license is unknown.
- R3 Flash: no fixed duration; it blinks on and off with the alert like a phone call or notification flash, and stops when the alert ends or is dismissed. Off by default, per reminder. Phones that restrict the torch in the background will just play the sound.
- R4 Confirmed as written.

Awaiting Ahmed's approval of the whole spec; phase 4 does not start before his approval.
