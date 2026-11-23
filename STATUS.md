# STATUS

Newest entry first. Add new entries at the top using Edit only.

## 2026-11-23 17:47 (Cairo) - المرحلة 4: سنة 2026 كاملة من المساحة، والمعادلة مطابقة 99.15%

قرار أحمد: نسحب كل أيام 2026 كمرجع داخلي يوم بيوم (مش للنشر)، ونضبط المعادلة على القيم الحقيقية، والتحليل يتعمل على Opus (أحمد حوّل الموديل).

السحب:
- 365 يوم كاملين من 2026، كل يوم فيه كل المدن (78 مدينة، ويومين بس فيهم 77). فاصل 3 إلى 5 ثواني بين الطلبات، من غير أي حجب أو خطأ. البيانات (4.5 ميجا) في scratchpad بس، مش في المستودع ومش منشورة. ملحوظة: الـscratchpad ممكن يتمسح لما الجلسة تخلص.

المعادلة اللي وصلنالها (مستنتجة من أرقام المساحة):
- موقع الشمس بصيغة Meeus الكاملة (مع النوتيشن)، محسوب عند وقت كل صلاة لوحدها (مش مرة واحدة في اليوم).
- الفجر 19.5 درجة تحت الأفق، العشاء 17.5 درجة.
- الشروق والمغرب: الشمس 0.845 درجة تحت الأفق (مش 0.833 المعتادة).
- العصر: ظل الشيء مثله + ظل الزوال، وظل الزوال محسوب بميل الشمس وقت الظهر.
- التوقيت: UTC+2، والصيفي UTC+3 من آخر جمعة في أبريل لآخر خميس في أكتوبر (اتأكد من بيانات المساحة: 24 أبريل و29 أكتوبر 2026).
- التقريب لأقرب دقيقة، وإحداثيات كل مدينة مستنتجة من أرقامها (مثلًا القاهرة 30.0725 شمال، 31.2776 شرق).

النتيجة على كل المدن طول السنة (حوالي 170 ألف قيمة):
- مطابقة بالظبط 99.15%. لكل صلاة: الفجر 99.13%، الشروق 99.06%، الظهر 99.23%، العصر 99.23%، المغرب 99.09%، العشاء 99.17%.
- أقصى فرق دقيقة واحدة بس، في أي مدينة وأي يوم وأي صلاة.
- لكل مدينة: أقل مطابقة 98.63% (شرق العوينات) وأعلى 99.77% (حلايب). 57 مدينة فوق 99%.
- الفروق الباقية كلها على حدود التقريب (الفرق الحقيقي ثانية لـ4 ثواني). جربت فرضيات تانية ومانفعتش: حساب الشمس مرة في اليوم (أسوأ)، صيغة NOAA المختصرة (أسوأ)، إحداثيات بالدقيقة القوسية (أسوأ).

في انتظار قرار أحمد: طريقة الوصول من 99.15% لصفر، ومكان حفظ البيانات الخام والسكريبتات.

## 2026-11-23 17:02 (Cairo) - المرحلة 4 (الجزء الأول): سحب عينة صغيرة للمقارنة الداخلية، ونتائج أولية

قرار أحمد: الخيارات ب + ج + د معًا (حساب بدون نشر بيانات المساحة، دار الإفتاء كمرجع محتمل، سحب للمقارنة الداخلية فقط). ماتنشرش أي بيانات من المساحة في أي مكان.

اللي اتعمل:
- دار الإفتاء: بحثت في الصفحة الرئيسية وفي sitemap عن شروط استخدام ومالقيتش صفحة شروط. الموجود: robots.txt فقط (بيمنع الروابط اللي فيها ? ومسار _TimePrayer). عندهم إيميل info@dar-alifta.org وتليفون 107 للاستفسار. وعندهم خدمة رسمية للتاريخ الهجري مقدمة لأصحاب المواقع (التاريخ الهجري مسموح استخدامه من هناك).
- المساحة: صفحة "المواقيت شهريًا" (monthlymwaket.aspx) فيها قايمة مدينة وقايمة شهر لكن مفيهاش جدول ظاهر (غالبًا صورة)، فمااستخدمتهاش.
- سحبت من المساحة عينة بس: 83 يوم (أيام 1 و6 و11 و16 و21 و26 من كل شهر في 2026 + أيام 23 و24 و25 أبريل و28 و29 و30 أكتوبر + 31 ديسمبر)، كل المدن في كل يوم، بفاصل 3 إلى 5 ثواني بين الطلبات، حوالي 100 طلب كلها، من غير أي خطأ أو حجب. الملفات في scratchpad بس مش في المستودع. مفيش سحب تاني قبل ما أحمد يوافق.

نتائج أولية (القاهرة، إحداثيات تقريبية، 83 يوم):
- التوقيت الصيفي: المساحة بتطبقه. الظهر بيتنقل من 11 إلى 12 ساعة بين 23 و24 أبريل 2026 (بداية الجمعة 24 أبريل)، وبيرجع بين 29 و30 أكتوبر 2026 (آخر يوم صيفي الخميس 29 أكتوبر). ده مطابق للقانون المصري (آخر جمعة من أبريل لآخر خميس من أكتوبر).
- الفرق بين المعادلة والمساحة في القاهرة: أقصى فرق دقيقة واحدة في كل الصلوات، لكن فيه فروق دقيقة في أيام كتير (الفجر 55 يوم من 83، الشروق 38، الظهر 15، العصر 10، المغرب 17، العشاء 13). يعني لسه مش صفر. محتاج ضبط (طريقة التقريب والإحداثيات) وده شغل Opus.
- جودة بيانات المصدر: يوم 26 مايو 2026 رجّع 77 مدينة بدل 78 وأسماء بتطويل. التاريخ الهجري في الجدول فيه تطويل ("رجـــب"، "ذوالحجـــــــة") ومحتاج تنضيف. أي سكريبت لازم يتحمل الحالات دي من غير ما يوقع.
- في كل يوم التاريخ الهجري جاي مع الجدول وبيتحرك يوم بيوم (مثلاً 1 المحرم 1448 = 16 يونيو 2026 عند المساحة).

اقتراح للخطوة الجاية (محتاج موافقة أحمد): نستنتج الإحداثيات الفعلية لكل مدينة من أرقام المساحة نفسها (الظهر بيدل على خط الطول، وطول النهار على خط العرض) بدل ما نجيب إحداثيات من مصدر خارجي، ونضبط طريقة التقريب. ده بيحتاج Opus، وبيتطلب إن أحمد يحوّل الموديل.

في انتظار قرار أحمد.

## 2026-11-23 16:45 (Cairo) - المرحلة 4: وقفت السحب بسبب شروط استخدام هيئة المساحة

- أحمد أمر ببدء المرحلة 4 وطلب فاصل أكتر من ثانية بين الطلبات. بدأت سكريبت سحب (فاصل 3 إلى 5 ثواني) وسحب 6 أيام من نوفمبر 2026 فقط (ملفات في scratchpad، مش في المستودع).
- قرأت صفحة https://www.esa.gov.eg/TERMSOFUSE.aspx: الموقع بيقول إن كل المواد ملك للهيئة وبيمنع نسخ أو نشر أو تحميل أو تحويل أو استخدام محتوى الموقع بأي حال. ملف robots.txt مفيهوش منع، لكن الشروط بتمنع.
- وقفت السكريبت فورًا. ماكملتش ولا هكمل لحد ما أحمد يقرر.
- لقيت كمان صفحة https://www.esa.gov.eg/monthlymwaket.aspx (المواقيت شهريًا، فيها قايمتين: مدينة وشهر على الأرجح)، ماجبتش بياناتها. تعديل: الصفحة دي ممكن تقلل عدد الطلبات من 365 لحوالي 12 لكل مدينة.
- صفحة ESA الرئيسية بتقول إن الإدارة العامة للجيودسيا والحساب هي اللي بتحسب المواقيت، ومفيهاش طريقة الحساب (زوايا الفجر والعشاء). الموقع بيعرض مواقيت اليوم كصورة قابلة للمشاركة على واتساب (dailyImage.aspx)، يعني الهيئة نفسها بتشجع مشاركة مواقيت اليوم.
- أخطاء اتسجلت كدروس في docs/lessons-learned.md: (1) بدأت سحب بالجملة قبل قراءة شروط الاستخدام. (2) كتبت ملفات الخطة والمواصفات بالإنجليزي بدل العربي.
- في انتظار قرار أحمد: الاتجاه القانوني (طلب إذن من الهيئة أو اتجاه تاني). لا سحب تاني قبل كده.

## 2026-11-23 16:38 (Cairo) - Spec R1-R4 confirmed by Ahmed

- R1: reminder before a prayer = "did you pray?" (yes stops, no repeats every N minutes until the next prayer); reminder after = "get up to pray". N is edited by Ahmed (default 10). No prayer history for now.
- R2: Ahmed will find an open-source adhan; meanwhile the user can pick any ringtone/song from the phone. No recording with unknown license is shipped.
- R3: flash blinks with the alert like call/notification flash, no fixed duration, stops when the alert ends or is dismissed; off by default.
- R4: confirmed.
- docs/spec.md updated. Awaiting Ahmed's approval of the whole spec before phase 4.

## 2026-11-23 16:31 (Cairo) - Ahmed's answers on spec Q1-Q7 recorded in docs/spec.md

- Approved: Q1 (look, editable after testing), Q2 option (a), Q3 no share/print, Q7 name. Q4-Q6 detailed: any-number-of-minutes reminders before/after, per prayer or shared, "did you pray?" follow-up that repeats until the next prayer, phone sound or adhan sound, camera flash alert, prohibited time (sunrise + 25 min) shown under sunrise with its own reminders, any reminder cancellable.
- New points to confirm: R1 ("did you pray?" meaning and repeat interval), R2 ("انولة" read as adhan, needs a legally usable recording), R3 (flash limits), R4 (prohibited time display).
- Spec stays a draft until Ahmed confirms R1-R4. No code started.

## 2026-11-23 16:06 (Cairo) - Phase 3 (spec) draft written, awaiting Ahmed's answers

- Ahmed clarified the "official result" sentence: the printed yearly prayer timetable distributed to the public. The app must replace it so he never needs the paper. Result: a "Year timetable" screen (every day: Gregorian date, Hijri date, six times, offline) is part of the spec. The earlier "official data record" interpretation stays as the internal correction source.
- docs/spec.md written (screens: Today, Year timetable, Reminders, Widgets W1-W4, City/location; data promises with what is behind each; acceptance checks; out of scope; open questions Q1-Q7). No code.
- Honest limit recorded: Hijri month starts are announced about a day ahead, so Hijri dates for later months can only be estimates (marked) unless Ahmed chooses otherwise (Q2).
- Awaiting Ahmed's answers to Q1-Q7 and his approval of the spec before phase 4.

## 2026-11-23 16:01 (Cairo) - Ahmed's answers on N1-N8 recorded in docs/plan.md

- Approved: N1, N3, N5, N6, N8. Changed: N2 (measure formula vs ESA for a full year, correct via the 1/11/21 checks), N4 (Hijri can change only on Hijri days 29, 30, 1, 2, 3), N7 (check whether the ESA vs Dar al-Ifta difference is permanent and in which prayers).
- Product principles: simple, light, Islamic identity, no user setup; user only chooses widgets and sets reminders (before upcoming prayers or one prayer, fixed or variable offsets, several per prayer).
- Open: Ahmed's last sentence about an "official result" record with prayer times and an updated Hijri date is interpreted as one official dataset (times + Hijri); awaiting his confirmation.
- Model note: this edit was made on Sonnet; planning itself was done on Opus earlier. No code started.

## 2026-11-23 15:40 (Cairo) - Final plan written (Opus), awaiting Ahmed's decision

- Full plan in docs/plan.md: fixed decisions D1-D5, new decisions N1-N8 (recommendations marked), phases 3-13.
- New evidence: the header of https://www.dar-alifta.org/ar/prayer shows the official Hijri date ("13 جُمادى الآخرة 1448 هـ"), same as the di107 API Ahmed opened. It is HTTPS, reachable from the cloud, and the URL has no query string (allowed by robots.txt). This is the recommended Hijri source (N4).
- No code started. Awaiting Ahmed's answers on N1-N8 and approval to start phase 3 (spec).

## 2026-11-23 15:35 (Cairo) - Ahmed's decisions on the plan review

Decisions (Ahmed's words, summarized):
- D1 source: compute on the phone. On the 1st, 11th and 21st of every Gregorian month the app goes online, compares with ESA / Dar al-Ifta, and corrects itself if different (internet 3 times a month for prayer times).
- D2 cities: both (ESA city list with coordinates + optional GPS).
- D3 tolerance vs ESA: zero minutes.
- D4 Hijri: source is Dar al-Ifta only. NO manual +/-1 day adjustment, ever. Check Dar al-Ifta only on Hijri days 27, 28, 29, 30, 1, 2, 3; no re-check after that.
- D5 minimum Android: 8 (API 26).
- D6 scraper role: Ahmed asks whether D1 already answers it (to be settled in the final plan).
- Widgets: several widget designs, all prayers + next prayer, more than one available; Ahmed picks or edits later.
- Play Console: Ahmed already has a paid developer account with an app published. The $25 step is removed.

Next: final plan to be written on Opus (Ahmed's request). Current session model is Sonnet, so planning waits for Ahmed to switch the model. No code started.

## 2026-11-23 15:23 (Cairo) - Plan review (Ahmed asked for a full critique and a full roadmap)

Phase 3 is NOT started. Ahmed asked for a critique of phases 1-3 and a full plan for all remaining phases, then he decides.

New evidence gathered for the review (read-only, scratchpad only):
- Calculation test: a standard astronomical formula with the Egyptian General Authority of Survey method (fajr 19.5 deg, isha 17.5 deg, Shafi asr) and rough city coordinates matched ESA within 1 minute for Cairo (2026-11-01, 2026-11-23) and Alexandria (2026-11-23). So prayer times can likely be computed on the phone instead of scraped.
- City lists: ESA table 78 cities, ESA dropdown 83 (5 extra not in the all-cities table: ابوتشت, ديربنجم, سمالوط, منوف + "كل المدن"). Dar al-Ifta 78; differs from ESA by spelling (سفاجه/سفاجا), ESA has مدينة 15 مايو, Ifta has an entry named "45792" (data error in the source).
- ESA month navigation: a direct jump to July 2026 (V9678) returned HTTP 500. Only previous/next month links are accepted, so reaching other months needs step-by-step postbacks. Not retried. Summer time (DST) data from ESA is still unverified.

Lessons recorded in docs/lessons-learned.md (premature data-pipeline design, no full roadmap, generated data in the app repo).

Awaiting Ahmed's decision on the full roadmap.

## 2026-11-23 15:17 (Cairo) - Phase 2 addendum: Hijri API works from Ahmed's browser

Ahmed opened http://di107.dar-alifta.org/api/HijriDate?langID=1 in his own browser (screenshot). Reply is a bare JSON string: "13 جمادى الآخرة 1448" (day, month name, year, Arabic). So the API is live; it is only unreachable from the cloud session. Note the month spelling differs from ESA ("13 جماد الآخر 1448"), so a month-name mapping table is needed if both are used. Today's date matches ESA (13, 1448). Phase 3 plan is being presented to Ahmed; no code started.

## 2026-11-23 15:15 (Cairo) - Phase 2 (read-only): Hijri API unreachable, ESA postback works

Approved by Ahmed: all phase-1 recommendations; phase 2 started on his explicit order. Network: di107.dar-alifta.org added to the personal "Mawaqit" environment allowlist (approved by Ahmed). No code changed; downloads kept in the scratchpad.

Findings:
- Dar al-Ifta Hijri API (http://di107.dar-alifta.org/api/HijriDate?langID=1): NOT reachable from the cloud session. HTTP: "Could not resolve host" (no DNS record from here). HTTPS: connection reset by peer. Not a 403, no workaround attempted. Response format still unknown. Possible causes: the host is internal/stale or blocks cloud IPs. Needs another way to check (Ahmed's own browser, or ask Dar al-Ifta).
- ESA postback works (HTTP 200): POST to /praytimes.aspx with __VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION taken from the page, __EVENTTARGET=ctl00$placeholder1$Calendar1 and __EVENTARGUMENT=<day code>. The reply is the full table of 78 cities for that day (tested 2026-11-01, code 9801; table identical in layout to today's). Day code appears to be days since 2000-01-01 (9801 = 1 Nov 2026); month arrows use V-codes (V9770 previous, V9831 next month).
- So one month from ESA = about 30 postbacks (one per day), all cities each. Hijri date comes with every row ("21 جماد الأول 1448"), so ESA alone can give Hijri dates.
- Cross-check: Cairo on 2026-11-01 is identical in ESA (4:41, 6:9, 11:38, 2:45, 5:8, 6:27) and Dar al-Ifta (04:41, 06:09, 11:38, 02:45, 05:08, 06:27), with the Ifta dhuhr AM/PM typo again. They look like the same underlying data for Cairo; other cities not compared yet.

Recommendation for phase 3 (needs Ahmed's approval): ESA postback as the main source (all 78 cities, Hijri included); Dar al-Ifta /ar/prayer (Cairo, no query string) only as a cross-check; Hijri API dropped until we can reach it. Scrape politely (sequential, about 1 request/second, one run per month plus a daily refresh).

Awaiting Ahmed's approval before phase 3 (parser + validator + JSON schema).

## 2026-11-23 15:06 (Cairo) - Phase 1 (read-only): source pages inspected, all three open (HTTP 200)

Result: the 403 from the previous entry is gone. All three pages returned HTTP 200 via curl. HTML saved in the session scratchpad only (not in the repo). No code changed.

Pages:
- ESA https://www.esa.gov.eg/praytimes.aspx: 200, ~104 KB, ASP.NET WebForms behind Cloudflare. No CAPTCHA or challenge text found.
  - Table 1 has 78 city rows. Columns: city, Gregorian date (YYYY-MM-DD), Hijri date text ("13 جماد الآخر 1448"), fajr, sunrise, dhuhr, asr, maghrib, isha. Times are 12h Arabic ("4:56 ص", not zero-padded: "5:2 ص").
  - A dropdown (83 options, first is "كل المدن") and a calendar control. Changing them is an ASP.NET postback (__VIEWSTATE / __EVENTVALIDATION). No JSON or API found. Page shows today only (monthly data needs a postback, not tested yet).
  - City names contain decorative tatweel ("القـاهـرة", "طنطــا") and need normalizing.
  - robots.txt: no Disallow rules, only a Cloudflare content-signals comment block (no signal values set).
- Dar al-Ifta https://www.dar-alifta.org/ar/prayer: 200, ~79 KB, plain server-rendered HTML.
  - Table 0 = today for the default city (Cairo). Table 1 = full current month (30 rows): day, fajr, sunrise, dhuhr, asr, maghrib, isha.
  - City dropdown has 78 options (ids 280898.. and names). Changing city calls GET /ar/Prayer/GetPrayer?town=<name> (returns HTML fragment) and /ar/Partial/_TimePrayer?town=<name> (today only).
  - Data issue: dhuhr is printed with "م" (PM) as "11:41 م" although it is 11:41 AM. The source has a typo, so the parser must not trust the AM/PM marker for dhuhr.
  - robots.txt: `Disallow: /ar/Partial/_TimePrayer/` and `Disallow: /*?` (any URL with a query string). So calling /ar/Prayer/GetPrayer?town=... contradicts robots.txt for the "all cities" path. The plain /ar/prayer page is not disallowed.
- Dar al-Ifta Hijri page https://www.dar-alifta.org/ar/services/details/12/...: 200, ~49 KB. Descriptive article, no table. It documents an official public widget/API: http://di107.dar-alifta.org/api/HijriDate?langID=1 (1 Arabic, 2 English, 3 French, 4 German). That host (di107) is outside the allowed network list, so it was NOT called. Response format unknown.

Bot blocking: none seen on these requests (no 403, no challenge, no CAPTCHA). Dar al-Ifta sets a TS... session cookie (F5 style) but did not require it. One request per page only; no load testing, no heavy scraping done.

Proposed JSON output (needs Ahmed's decision):
- Option A: one file per source per month, e.g. esa/2026-11.json, ifta/2026-11.json, each = {source, fetchedAt, month, cities:[{id, nameAr, days:[{date, hijri, fajr, sunrise, dhuhr, asr, maghrib, isha}]}]} with times as 24h "HH:mm", plus index.json listing months and cities. Pros: small files (app downloads only what it needs), easy to version/diff, matches "one month at a time" source limits. Cons: many files; a city list is repeated per file.
- Option B: one big yearly file per source. Pros: simplest for the app. Cons: large download, ESA/Ifta do not publish a full year, so it would need many scrapes merged.
- Option C: one file per city per month. Pros: smallest download for a user with one city. Cons: ~150 files per month, more Pages churn.
- Recommendation: Option A with a shared cities.json (stable id, Arabic name, aliases, coordinates later) so ESA and Dar al-Ifta city names map to one id. Store times as 24h "HH:mm" and keep the raw Hijri string plus parsed {y,m,d}. Fix the dhuhr AM/PM typo in the parser with a sanity rule (dhuhr between 10:30 and 13:30) and keep a validation step that fails the run on bad data instead of publishing it.

Open questions for Ahmed:
1. Does the Dar al-Ifta robots.txt rule (no query-string URLs) block using /ar/Prayer/GetPrayer for the other cities? Recommendation: scrape only what the plain page serves, and ask Dar al-Ifta for permission or use the ESA postback for all cities.
2. Is calling the official Hijri API on di107.dar-alifta.org allowed (network list)? Recommendation: yes, add it to the allowed hosts for a single test call.
3. Where does the scraper run (GitHub Actions scheduled job is the recommendation)?

Awaiting Ahmed's approval before any further phase.

## 2026-11-23 15:02 (Cairo) - Phase 0: infrastructure ready, sources unreachable from cloud session

Done:
- Public repos: ahmedhatemamh/mawaqit-app (empty), ahmedhatemamh/mawaqit-data (README + docs/index.html + docs/CNAME).
- GitHub Pages on mawaqit-data (main, /docs), custom domain data.ahmedhatem.work (DNS check pending).
- CodeRabbit installed on both repos.
- Cloudflare CNAME data -> ahmedhatemamh.github.io (DNS only).
- Package name decided: work.ahmedhatem.mawaqit. App name: Mawaqit Misr.

Findings:
- esa.gov.eg and dar-alifta.org return 403 from the cloud session proxy (CONNECT tunnel failed), so page structure is unknown.

Pending (needs Ahmed's approval):
- Where the scraper runs and how we inspect the source pages.
- Verify https://data.ahmedhatem.work opens and Enforce HTTPS is on.
- Pending "Permission updates requested" on CodeRabbit and Claude apps (untouched on purpose).
