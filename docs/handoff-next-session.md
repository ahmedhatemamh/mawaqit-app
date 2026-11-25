# تسليم للجلسة الجاية — 2026-11-25 17:38 (القاهرة)

الغرض: جلسة جديدة تحمّل الخبراء الأربعة الجداد (`.claude/agents/`) وتكمّل 10ب2 من غير ما تسأل أحمد عن اللي اتعمل.

## اقرا بالترتيب
1. `CLAUDE.md` ثم `docs/standing-rules.md` (فيه قاعدة الخبراء وقاعدة «كرت الـPR»، وكلاهما على main).
2. `STATUS.md` (الإدخال الأعلى).
3. الملف ده.

## الرد والأسلوب (من قواعد أحمد)
- كل الرد بالعربي المصري البسيط، والكود بالإنجليزي. الساعة من أمر `TZ=Africa/Cairo date` فعلي، مش تخمين.
- آخر كل رد: «اللي عليك دلوقتي».
- أي مرة تذكر PR في رد: اكتب **كرت نصي كامل** (رقم وعنوان، الوصف بنصه، الملفات، حالة build وemulator وdecision-link وCodeRabbit، mergeable، ملفات حراسة، ثم الرابط). أحمد بيقرا من الرد من غير ما يفتح GitHub (قاعدة DEC-038، مدموجة في PR #16).
- أي قرار من أحمد يتسجل في `docs/decisions-log.md` بكلامه الحرفي (آخر رقم DEC دلوقتي DEC-038).
- الدمج بأمر أحمد الصريح بس. ملفات الحراسة (`.github/workflows/` و`CLAUDE.md` و`docs/standing-rules.md` و`.claude/`) أحمد اللي يدمجها.
- لا commit بـ`git add -A`. أضف الملفات بأسمائها.

## الوضع الحالي (حتى 17:38 القاهرة)
| الحاجة | الحالة |
|---|---|
| `main` | فيه الخبراء الأربعة وسجل القرارات (لحد DEC-038) وقاعدة CodeRabbit 62 دقيقة و`.coderabbit.yaml` وقاعدة الكرت. PR #16 اتدمج (commit الدمج `359ef98`) |
| PR #14 و#15 و#16 | كلهم اتدمجوا. مفيش PR مفتوح دلوقتي |
| فرع `feat/alerts-scheduling` | آخر commit `edd1e3b` (دمج main). كود 10ب2 في `20d6a38`. **مدفوع ومفيش PR له** |
| الخبراء الأربعة | على main، لكن جلسة 2026-11-25 السابقة ما حمّلتهمش (`Agent type ... not found`). الجلسة الجديدة لازم تلاقيهم |
| CodeRabbit | المراجعة التلقائية مقفولة. آخر طلب مقبول 12:54 UTC، فالطلب الجاي مسموح. مراجعة كل 62 دقيقة. الحد مشترك مع مستودع ERP |

## الخطوة الأولى: اتأكد إن الخبراء اتحملوا
استدعِ الـAgent tool بنوع `alarm-reliability-auditor`. لو رجع `not found`، **وقف** وقول لأحمد، ولا تستخدم مساعد عام بأدوات كاملة بدل الخبير (أمره: القراءة فقط تبقى مجرد تعليمات هناك). القرار له.

## 10ب2 — اللي اتكتب (فرع `feat/alerts-scheduling`)
كله تحت `app/`:
- `alerts/FiredAlert.kt`، `alerts/AlertTexts.kt`، `alerts/AlertScheduler.kt` (منبه واحد للتنبيه الجاي)، `alerts/SystemAlarmBackend.kt` (`AlarmManager`، دقيق لو مسموح وإلا تقريبي)، `alerts/AlertNotifications.kt` (قناتين: تذكيرات ومنبهات)، `alerts/AlertReceivers.kt` (`AlertReceiver` و`RescheduleReceiver`).
- `MawaqitApp.kt` (بيعيد التخطيط كل ما القواعد أو «صليت» أو المدينة تتغير).
- `AndroidManifest.xml` (4 صلاحيات + receiver)، `res/drawable/ic_notification.xml` (أيقونة مؤقتة).
- اختبارات: JVM `AlertSchedulerTest` و`AlertTextsTest` و`FiredAlertTest`، ومحاكي `androidTest/.../alerts/AlertsOnDeviceTest.kt`.
- نتيجة محليًا على `20d6a38`: `:engine:test` 76، `:app:testDebugUnitTest` 153، 0 فشل؛ `:app:lintDebug` 0 أخطاء؛ `assembleDebug` و`compileDebugAndroidTestKotlin` نجحوا.

## 10ب2 — اللي ما اتكتبش
أزرار «صليت» و«إيقاف» في الإشعار، تكرار السؤال لو «لا» (`repeatEveryMinutes`)، الفلاش، الصوت المختار وحد الـ5 دقايق، شاشة التنبيهات (10ج)، طلب الصلاحيات وقت إنشاء أول تنبيه وملحوظة الإسقاط (قرار 3أ)، 10د (الصوت والأذان).

## لم أتحقق
1. `AlertsOnDeviceTest` ما اتشغلش (CI بس، بعد فتح PR).
2. سلوك المنبه تحت Doze وتوفير الطاقة على جهاز حقيقي.
3. سياسة Google Play لصلاحية `SCHEDULE_EXACT_ALARM`.
4. هل `RescheduleReceiver` بيستلم `TIME_SET` على كل الإصدارات.
5. جدولة أيام مراجعة الهجري في `mawaqit-data` بتطابق DEC-033 (28 و29 و30 و1 و2 و3).

## الخطوات قبل فتح PR الجدولة (أمر أحمد + DEC-035)
1. `git checkout feat/alerts-scheduling` وتأكد إنه على `edd1e3b` أو أحدث.
2. استدعِ **`alarm-reliability-auditor`** على `app/src/main/java/work/ahmedhatem/mawaqit/alerts/` و`MawaqitApp.kt` و`AndroidManifest.xml` والمخطط في `engine/.../alerts/AlertPlanner.kt`، والمرجع `docs/spec.md` قسم 8.10 وDEC-022 وDEC-023. اذكر له اللي لسه ما اتكتبش عن قصد (فوق).
3. استدعِ كمان حسب القاعدة: **`android-test-auditor`** (فيه اختبار محاكي جديد)، و**`privacy-security-auditor`** (فيه تعديل على `AndroidManifest.xml`).
4. اتحقق بنفسك من كل نقطة في الكود (حقيقية ولا إنذار كاذب). سجّل كل استدعاء في `docs/agents-log.md` (صف لكل خبير: التاريخ، الخبير، sonnet، السبب، عدد النقاط، الحقيقي، الإنذارات الكاذبة، الإجراء). لا تحسب نسبة صحة لأقل من 5 ردود متحقق منها.
5. صلّح الحقيقي، وأعد تشغيل: `./gradlew --offline :engine:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
6. افتح PR 10ب2 (وصفه فيه سطر «القرار المرتبط: DEC-023 وDEC-035»)، اشترك فيه، واستنى build وemulator.
7. **مراجعة ثانية مستقلة**: اطلب `@coderabbitai review` مرة واحدة بعد ما كل الفحوصات تخضر (مسموح بعد 13:56 UTC)، واقرا الـthreads بنفسك (`get_review_comments`) قبل أي دمج. سجّلها في `docs/agents-log.md` كصف مستقل.
8. لا تدمج من غير أمر أحمد.

## شغل مؤجل
- PR منفصل لقياس وقت أول شاشة مقابل وعد الثانيتين في المواصفات.
- بعد دمج 10ب2: 10ب3 (أزرار «صليت»/«إيقاف» وتكرار السؤال)، 10ج (شاشة التنبيهات)، 10د (الصوت والفلاش)، ثم المرحلة 11 (الويدجت، كلها قابلة لتغيير الحجم)، ولمسة الشهر الهجري، ثم 12 و13.
- بانتظار أحمد: تصميماته (الشاشات والويدجت وثيمات الشهور)، ردود ESA وNRIAG (هو بيبعتها)، الأيقونة «أ» (DEC-030) لما نوصل لمرحلة 12.
- خطوة سنوية: `EgyptDst.LAST_CONFIRMED_YEAR`.

## تنبيهات من أخطاء النهارده
- بعد أي دمج، افحص CI على `main` (درس 2026-11-25). تشغيل الدمج بيتلغي عادةً بدفعة بعده، فاحكم بآخر run على آخر commit.
- لا تقول «مفيش ملاحظات» من ملخص CodeRabbit المقطوع؛ اقرا `get_review_comments`.
- أدوات GitHub (`mcp__github__*`) deferred وبتفصل أحيانًا؛ حمّلها بـ`ToolSearch`. مفيش `gh` خارج البحث.
