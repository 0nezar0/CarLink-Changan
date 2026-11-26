<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/carlink-mark-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/assets/carlink-mark-light.svg">
  <img alt="CarLink logo" src="docs/assets/carlink-mark-light.svg" width="120">
</picture>
&nbsp;&nbsp;&nbsp;
<img alt="Changan" src="docs/assets/changan-logo.png" width="120">

# CarLink

**CarPlay و Android Auto في تطبيق واحد لعموم الشاشات العاملة بنظام أندرويد، مع دعم وتخصيص خاص لشاشة شانجان CS75 Plus 2023.**

**CarPlay and Android Auto in one app for Android car head units in general, with dedicated support and tuning for the Changan CS75 Plus 2023 head unit.**

[![Release](https://img.shields.io/github/v/release/0nezar0/carlink-releases?label=release)](https://github.com/0nezar0/carlink-releases/releases/latest)
[![License: AGPL-3.0](https://img.shields.io/badge/license-AGPL--3.0-blue)](https://github.com/F-e-n-y-x/CarLink/blob/main/LICENSE)
[![Android](https://img.shields.io/badge/Android-car%20head%20units-3DDC84?logo=android&logoColor=white)](#المتطلبات)
[![Languages](https://img.shields.io/badge/languages-11-8A2BE2)](#الميزات)

[**التحميل / Download**](https://github.com/0nezar0/carlink-releases/releases) ·
[دليل المستودعات](#دليل-المستودعات) · [إعدادات شانجان](#إعدادات-شانجان) · [الميزات](#الميزات) · [التثبيت والتحديث](#التثبيت-والتحديث) · [التواصل](#التواصل) · [شكر وتقدير](#شكر-وتقدير) · [English](#english)

</div>

<div dir="rtl">

ركّب CarLink على شاشة السيارة العاملة بنظام أندرويد، ووصّل جوالك بالسلك أو لاسلكياً: **الآيفون يشتغل CarPlay** و**جوال الأندرويد يشتغل Android Auto**، والتطبيق يعرف نوع الجوال لحاله.

هذه النسخة مبنية على مشروع [CarLink](https://github.com/F-e-n-y-x/CarLink) للمطوّر **F-e-n-y-x**، وتعمل على **عموم الشاشات العاملة بنظام أندرويد**، مع **دعم وتخصيص خاص لشاشة شانجان CS75 Plus 2023** (أندرويد 9 ومعالج Allwinner T7): شعار شانجان، والهامش العلوي حتى لا يختفي شيء تحت شريط حالة السيارة، وضبط الصورة بما يناسب هذه الشاشة.

</div>

<table>
  <tr>
    <td width="50%"><a href="docs/screenshots/home-ar.png"><img src="docs/screenshots/home-ar.png" alt="الشاشة الرئيسية"></a></td>
    <td width="50%"><a href="docs/screenshots/settings-main-ar.png"><img src="docs/screenshots/settings-main-ar.png" alt="الإعدادات"></a></td>
  </tr>
  <tr>
    <td><a href="docs/screenshots/language-picker-ar.png"><img src="docs/screenshots/language-picker-ar.png" alt="اختيار اللغة"></a></td>
    <td><a href="docs/screenshots/connection-status-ar.png"><img src="docs/screenshots/connection-status-ar.png" alt="حالة الاتصال"></a></td>
  </tr>
  <tr>
    <td><a href="docs/screenshots/car-picker-ar.png"><img src="docs/screenshots/car-picker-ar.png" alt="اختيار السيارة"></a></td>
    <td><a href="docs/screenshots/settings-display-ar.png"><img src="docs/screenshots/settings-display-ar.png" alt="الشاشة والهامش العلوي"></a></td>
  </tr>
</table>

<div dir="rtl">

الصور من محاكي أندرويد 9 بواجهة الوضع الليلي واللغة العربية، مو من السيارة.

## دليل المستودعات

| الوجهة | المحتوى |
| --- | --- |
| [التطوير الخاص](https://github.com/0nezar0/carlink-private-build) | كود العمل والنسخ التجريبية الخاصة عبر Actions. |
| [الكود المصدري العام](https://github.com/0nezar0/CarLink-Changan) | مشروع CarLink لشانجان بترخيص AGPL-3.0. |
| [تحميل الإصدارات](https://github.com/0nezar0/carlink-releases/releases) | ملفات APK المنشورة وملاحظات الإصدارات والتحقق من SHA-256. |

## إعدادات شانجان

اختر **شانجان CS75 Plus 2022–2023** ليظهر قسم شانجان داخل الإعدادات. التعديلات التالية موجودة في كود التطوير الخاص:

- **شريط التنقّل** — مفتاح في الإعدادات › السيارة، لكل السيارات. اسحب للداخل من الحافة اليمنى أو اليسرى ليظهر الشريط على نفس الجهة، وكرّر السحبة لإخفائه. أزراره: رجوع، الرئيسية، التطبيقات المثبّتة (الضغط يفتح التطبيق والضغط المطوّل يفتح معلومات التطبيق)، CarLink، وإعدادات أندرويد. اللمس العادي على حافة الشاشة يصل للتطبيق تحتها، والحافتان تتوقفان أثناء CarPlay و Android Auto. يتطلب تفعيل خدمة CarLink في إمكانية الوصول. اختيار كل سيارة يُحفظ لوحده.
- **الهامش العلوي** — في الإعدادات › السيارة لكل السيارات (اختيار مستقل لكل ماركة، يبدأ مطفأ). وفي قسم شانجان أيضاً عند اختيار CS75 Plus، وبنفس القيمة.
- **نقطة اتصال السيارة** — في إعدادات الاتصال تنويه يشرح تشغيل نقطة الاتصال، وزر **إعدادات نقطة الاتصال** يفتح إعدادات النقطة أو إعدادات الشبكة حسب ما تدعمه شاشة السيارة.

## الميزات

- **CarPlay و Android Auto معاً:** بالسلك أو لاسلكياً، والتطبيق يعرف الآيفون من الأندرويد بدون ما تختار.
- **لعموم شاشات أندرويد، مع تخصيص لشانجان CS75 Plus 2023:** يعمل على شاشات السيارات العاملة بنظام أندرويد عموماً. وإذا اخترت شانجان: شعارها على زر الرجوع للسيارة في CarLink وفي الإعدادات › السيارة. الهامش العلوي الاختياري (48 بكسل) وشريط التنقّل متاحان لكل السيارات من الإعدادات › السيارة، واختيار كل ماركة مستقل. باقي السيارات (أو بدون اختيار ماركة): أيقونة السيارة الافتراضية بدون شعار ماركة.
- **التشغيل مع السيارة:** يفتح مع تشغيل الشاشة، ويتصل بآخر جوال، ويجرّب أول الطريقة اللي نجحت آخر مرة.
- **11 لغة من داخل التطبيق:** عربي، إنجليزي، إسباني، برتغالي، فرنسي، إيطالي، ألماني، ماليزي، تركي، صيني مبسّط ومجري. التبديل من الإعدادات › عام › اللغة (أو متابعة لغة الجهاز).
- **حالة الاتصال:** من الإعدادات › الاتصال › حالة الاتصال (وفي التشخيص). أرقام مباشرة تتحدّث نحو مرة كل ثانية أثناء الاتصال: نوع الاتصال والطريقة اللاسلكية واسم الجوال، والفيديو (الدقة ومعدل الإطارات والترميز والمفكّك والإطارات الواردة والمسقطة والمتأخرة وزمن فك التشفير ومعدل النقل)، والصوت، ونطاق الواي فاي وسرعته والإشارة عند الاتصال اللاسلكي، ومدة الجلسة وعدد إعادة الاتصال. أي رقم غير متاح يظهر «غير متوفر».
- **التحديث من داخل التطبيق:** الإعدادات › حول › «التحقق من التحديثات»، مع بحث تلقائي كل 6 ساعات إن فعّلته. يُحفظ الملف في Downloads/CarLink ويُفحص (sha256 ومفتاح التوقيع) قبل التثبيت. خيار «الحصول على نسخ تجريبية» يعرض الإصدارات التجريبية المنشورة قبل الإصدار المستقر.
- **خادم الوحدة الرئيسية لـ Android Auto:** إذا كان «Start head unit server» شغّال في الجوال والاثنين على نفس الشبكة، يتصل عن طريق المنفذ 5277 تلقائياً.
- **الوضع الخفيف (اختياري، مطفّي افتراضياً):** إيقاف / تلقائي / دائماً. 30 إطار بدل 60 والصورة ترسم مباشرة على الشاشة، وتأثيرات CarLink تصير بسيطة، بنفس الدقة والجودة.
- **HEVC لـ CarPlay مفعّل من البداية:** ويرجع لـ H.264 تلقائياً إذا الشاشة ما تدعمه.
- **حفظ تقرير كامل بضغطة، وأزرار الطارة** تصل لـ CarPlay و Android Auto حتى لو التطبيق بالخلفية.

## المتطلبات

- **الشاشة:** شاشة سيارة عاملة بنظام أندرويد. هذه النسخة فيها دعم وتخصيص خاص لشاشة شانجان CS75 Plus 2023 (أندرويد 9، Allwinner T7). ما يحتاج روت ولا تعديل على النظام.
- **الجوال:** آيفون لـ CarPlay أو جوال أندرويد لـ Android Auto، بالسلك أو بالبلوتوث والواي فاي للاسلكي.

## التثبيت والتحديث

1. نزّل ملف APK من [صفحة الإصدارات](https://github.com/0nezar0/carlink-releases/releases). كل إصدار معه ملف `.sha256` للتحقق.
2. انقل الملف إلى شاشة السيارة وثبّته (يُسمح بتثبيت التطبيقات من مصادر غير متجر التطبيقات إن طلبت الشاشة ذلك).
3. بعد التثبيت، حدّث من داخل التطبيق: **الإعدادات › حول › التحقق من التحديثات**.
4. إذا تبي تجرب النسخ التجريبية قبل الإصدار المستقر، فعّل **الإعدادات › حول › الحصول على نسخ تجريبية**.

الإصدار الحالي: [CS75 Plus 2023 v1.6.6 (تجريبي)](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.6). كل النسخ في [صفحة الإصدارات](https://github.com/0nezar0/carlink-releases/releases).

## الجديد

| النسخة | الجديد |
|---|---|
| [CS75 Plus 2023 v1.6.6 (تجريبي)](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.6) | مبني على CarLink 1.0.18، ورقم البناء 10049 (يثبت فوق كل النسخ السابقة). شريط التنقّل بالسحب من الحافتين وزر التطبيقات المثبّتة والهامش العلوي متاحة لكل السيارات من الإعدادات › السيارة (اختيار مستقل لكل ماركة). ونقطة اتصال السيارة تختار IPv4 أو IPv4 مع IPv6 تلقائياً بدون أي إعداد: تفحص الشاشة قبل الإعلان، وتبقى على IPv4 فقط (مثل v1.6.5) إلا إذا كان IPv6 صالح فعلاً. وإذا علق الاتصال مع IPv6 (بدون اتصال أو عند مرحلة الصورة) ترجع تلقائياً لـ IPv4 فقط وتحفظ ذلك لهذه الشاشة. خيار CS75 Plus 2023 يبقى على IPv4 فقط. وفي التشخيص قسم جديد يبيّن نوع العنوان المستخدم وهل الشاشة تدعم IPv6 والسبب ووقت القرار. |
| [CS75 Plus 2023 v1.6.5](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.5) | الهامش العلوي 48 بكسل (اختبار الهوامش) صار مطفي افتراضياً لخيار CS75، مع ترحيل مرة واحدة إن كانت القيمة من الافتراضي القديم فقط. وعند اختيار السيارة يظهر حوار إخفاء شريط الإشعارات (تشغيل / تخطي) بدل سؤال الهامش. ومظهر تطبيق CarLink نفسه صار الوضع الداكن افتراضياً للجميع ولكل السيارات (بدون تغيير من اختار الفاتح). وتشغيل الصوت صار عبر Bluetooth في السيارة افتراضياً (بدون تغيير من اختار عبر CarLink). وقائمة «تطبيقات أختارها» تعرض كل التطبيقات القابلة للتشغيل لا بضعة تطبيقات فقط. وعبارة التنبيه في ماركة السيارة تحدّثت لتناسب هذا السلوك. وبلاغات المشاكل (رمز QR والروابط) تروح لمستودع الإصدارات هذا بدل مستودع CarLink، وبريد الدعم هو alhazmi.n.car@gmail.com (في حول التطبيق وشاشة البلاغ). ويشمل كل جديد v1.6.4: مسار نقطة اتصال السيارة IPv4 فقط. تفاصيل العمل في docs/CS75_PLUS_CHANGES.md. |
| [CS75 Plus 2023 v1.6.4](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.4) (تجريبي) | إصلاح CarPlay عبر نقطة اتصال السيارة: المسار IPv4 فقط (بدون AAAA/IPv6، والمستمعات على عنوان IPv4 لنقطة الاتصال، بدون قلب عائلة العناوين). ويشمل كل جديد v1.6.3: المسار مثل الشبكة المشتركة وهوية AirPlay المحفوظة، وتصنيف إعدادات CS75، وإصلاح الصورة وسؤال أول تشغيل. تفاصيل العمل في docs/CS75_PLUS_CHANGES.md. |
| [CS75 Plus 2023 v1.6.3](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.3) (تجريبي) | إصلاح CarPlay عبر نقطة اتصال السيارة: بعدما تشتغل النقطة، المسار يصير مثل الشبكة المشتركة (نفس اكتشاف العنوان وBonjour والمستمعات وهوية AirPlay المحفوظة)، والنشر دائماً على IPv4. ويشمل كل جديد v1.6.2: تصنيف إعدادات CS75 أولاً فوق «الاتصال»، وإصلاح ترتيب العنوان العربي، وخيار CS75 والترحيل، وأوضاع إخفاء الشريط، والافتراضيات، وإصلاح صورة CarPlay، وسؤال أول تشغيل. تفاصيل العمل في docs/CS75_PLUS_CHANGES.md. |
| [CS75 Plus 2023 v1.6.2](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.2) (تجريبي) | إعدادات CS75 صارت تصنيفاً عادياً في قائمة الإعدادات، أولاً فوق «الاتصال»، ومحتواها في الجزء الأيمن القابل للتمرير (بدل بطاقة ثابتة بعرض الشاشة كانت تمنع التمرير). إصلاح ترتيب العنوان العربي مع الاسم اللاتيني. ويشمل كل جديد v1.6.1: خيار CS75 والترحيل، أوضاع إخفاء الشريط وتنبيه إمكانية الوصول، الهامش 48 بكسل والافتراضيات (400 mm وHEVC و200 DPI)، إصلاح صورة CarPlay على نقطة اتصال السيارة، وسؤال أول تشغيل بعد تثبيت جديد. تفاصيل العمل في docs/CS75_PLUS_CHANGES.md. |
| [CS75 Plus 2023 v1.6.1](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.1) (تجريبي) | قسم جديد أعلى الإعدادات بعنوان «اعدادات خاصة لشانجان /CS75 Plus 2023-2022 فقط»، يظهر فقط عند اختيار «شانجان /CS75 Plus 2023-2022 فقط». إخفاء شريط الحالة: إيقاف، أو في كل التطبيقات ما عدا الشاشة الرئيسية، أو في تطبيقات تختارها. لازم تفعّل إذن إمكانية الوصول. عند اختيار هذا الخيار (أو عند الترحيل من شانجان العامة) تُفعَّل افتراضياً عرض CarPlay 400 mm وHEVC وكثافة Android Auto 200 DPI والهامش العلوي 48 بكسل، بدون تعديل قيمة غيّرتها بنفسك. شانجان العامة مثل باقي السيارات. إصلاح: نقطة اتصال السيارة كانت توصل CarPlay لآخر خطوة (الصورة) بدون ما تظهر الشاشة، بينما Android Auto على نفس الشبكة يشتغل؛ منافذ الصورة والتوقيت صارت تُربط على نفس عنوان جلسة التحكم. عند أول تشغيل بعد تثبيت جديد يظهر سؤال مرة واحدة: «هل سيارتك شانجان /CS75 Plus 2023-2022؟» — نعم يختار خيار CS75 ويطبّق إعداداته الافتراضية. تفاصيل العمل في docs/CS75_PLUS_CHANGES.md. |
| [CS75 Plus 2023 v1.6](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6) | تنويه شانجان في الإعدادات › حول ما عاد يوصف التطبيق كنسخة خاصة غير مخصصة للتوزيع. التنويه يبقى يقول إن الاسم والشعار للتعريف بالسيارة فقط، وإن مافي علاقة رسمية بين CarLink والشركة. |
| [CS75 Plus 2023 v1.5](https://github.com/0nezar0/carlink-releases/releases/tag/v1.5) (تجريبي) | إصدار تجريبي يشمل لغات التطبيق وصفحة حالة الاتصال، والتحديث من داخل التطبيق مع خيار النسخ التجريبية. |
| [CS75 Plus 2023 v1.4](https://github.com/0nezar0/carlink-releases/releases/tag/v1.4) (تجريبي) | 11 لغة مع اختيار اللغة من الإعدادات › عام › اللغة · صفحة «حالة الاتصال» المباشرة في الإعدادات › الاتصال والتشخيص. |
| [CS75 Plus 2023 v1.2](https://github.com/0nezar0/carlink-releases/releases/tag/v1.2) | إصدار لاختبار التحديث من داخل التطبيق من v1.1. |
| [CS75 Plus 2023 v1.1](https://github.com/0nezar0/carlink-releases/releases/tag/v1.1) | تحديث من CarLink 1.0.17 للمطوّر F-e-n-y-x: طريقة اتصال «خادم الوحدة الرئيسية» · خفض الموسيقى وقت إرشادات CarPlay · أحجام واجهة أكبر · HEVC لـ CarPlay · زر الهامش العلوي لشانجان · شعار شانجان في CarPlay لشانجان فقط · تحسينات الوضع الخفيف وغيرها. |

## التواصل

للدعم أو الإبلاغ عن مشكلة: [مسائل GitHub](https://github.com/0nezar0/carlink-releases/issues) أو البريد [alhazmi.n.car@gmail.com](mailto:alhazmi.n.car@gmail.com). للمشاكل الأمنية راسل البريد نفسه، ولا تفتح مسألة عامة.

## شكر وتقدير

مبنية على [CarLink](https://github.com/F-e-n-y-x/CarLink) للمطوّر **F-e-n-y-x**، مع شكر خاص له على هذا العمل الرائع، وعلى مشروعَي [DiPlay](https://github.com/shihabal3amri/DiPlay) (CarPlay) و[DiAuto](https://github.com/shihabal3amri/DiAuto) / Open Headunit (Android Auto). الرخص والشكر موجودة داخل التطبيق في «حول CarLink».

## الرخصة

مرخّصة برخصة **GNU AGPL-3.0**، مثل CarLink للمطوّر F-e-n-y-x ([LICENSE](https://github.com/F-e-n-y-x/CarLink/blob/main/LICENSE)).

## العلامات التجارية

شعار شانجان واسمها علامات تجارية مملوكة لشركة Changan Automobile، واستخدامها هنا للتعريف بالسيارة فقط، ولا توجد أي علاقة رسمية مع الشركة. الشعار من [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Changan_icon.svg) (292Jacob، CC BY-SA 4.0).
CarPlay علامة تجارية لـ Apple، و Android Auto علامة تجارية لـ Google.

## للمطوّرين

البناء والاختبار في [docs/BUILDING.md](docs/BUILDING.md)، وإصدار النسخ في [docs/RELEASING.md](docs/RELEASING.md). حلقة التطوير المحلية:

```sh
bash .claude/skills/carlink-dev/scripts/dev.sh build
bash .claude/skills/carlink-dev/scripts/dev.sh test
```

</div>

---

## English

Use [public source](https://github.com/0nezar0/CarLink-Changan) for the AGPL-licensed project, [public releases](https://github.com/0nezar0/carlink-releases/releases) for APK downloads, and this private repository for development and test builds.

Install CarLink on an **Android car head unit** and connect your phone by cable or wirelessly: an **iPhone gets CarPlay**, an **Android phone gets Android Auto**, detected automatically.

This build is based on [CarLink](https://github.com/F-e-n-y-x/CarLink) by **F-e-n-y-x**. It is meant for **Android car head units in general**, with **dedicated support and tuning for the Changan CS75 Plus 2023 head unit** (Android 9, Allwinner T7): the Changan logo, a top margin so nothing hides under the car's status bar, and picture defaults that suit this screen.

### Features

- **CarPlay and Android Auto in one app**, wired or wireless, with automatic phone detection.
- **For Android head units in general, with special Changan CS75 Plus 2023 support:** choose Changan for its logo on CarPlay's back-to-car button and in Settings › Car. The optional 48 px top margin and the navigation bar are in Settings › Car for every brand (each car keeps its own choice). Other cars, or no brand, get the default car icon.
- **Starts with the car:** opens when the screen turns on, reconnects the last phone and tries the path that worked last time first.
- **11 languages in the app:** Arabic, English, Spanish, Portuguese, French, Italian, German, Malay, Turkish, Simplified Chinese and Hungarian. Switch in Settings › General › Language (or follow the system language).
- **Connection status:** Settings › Connection › Connection status (also in Diagnostics). Live figures, refreshed about once a second while connected: connection type and wireless method, phone name, video (resolution, frame rate, codec, decoder, incoming fps, dropped and late frames, decode latency, bitrate), audio, Wi-Fi band, link speed and signal when wireless, session uptime and reconnect count. Anything unavailable is shown as such.
- **In-app updates:** Settings › About › Check for updates, plus an optional automatic check every 6 hours. The APK is saved in Downloads/CarLink and checked (sha256 and signing key) before installing. Turn on **Get test versions** to also be offered pre-releases.
- **Android Auto head unit server**, Lite mode (optional), HEVC for CarPlay with an H.264 fallback, one-tap full reports, and steering-wheel keys.

### Requirements

- **The display:** an Android car head unit. This build includes dedicated tuning for the Changan CS75 Plus 2023 (Android 9, Allwinner T7). No root and no system changes.
- **The phone:** an iPhone (CarPlay) or an Android phone (Android Auto), with a cable, or Bluetooth and Wi-Fi for wireless.

### Install and update

1. Download the APK from the [releases page](https://github.com/0nezar0/carlink-releases/releases). Each release includes a `.sha256` file.
2. Copy it to the head unit and install it (allow installation from outside the store if the unit asks).
3. Later updates can be installed from **Settings › About › Check for updates**.
4. To receive test builds, turn on **Settings › About › Get test versions**.

The current build is [CS75 Plus 2023 v1.6.6 (test)](https://github.com/0nezar0/carlink-releases/releases/tag/v1.6.6). All builds are on the [releases page](https://github.com/0nezar0/carlink-releases/releases).

### Contact

For support or to report a problem, use [GitHub issues](https://github.com/0nezar0/carlink-releases/issues) or email [alhazmi.n.car@gmail.com](mailto:alhazmi.n.car@gmail.com). For a security problem, write to that address privately; do not open a public issue.

### Credits

Based on [CarLink](https://github.com/F-e-n-y-x/CarLink) by **F-e-n-y-x**, with special thanks for this wonderful work, and on the [DiPlay](https://github.com/shihabal3amri/DiPlay) (CarPlay) and [DiAuto](https://github.com/shihabal3amri/DiAuto) / Open Headunit (Android Auto) engines. Licences and credits are in the app under About CarLink.

### License

Licensed under the **GNU AGPL-3.0**, like CarLink by F-e-n-y-x ([LICENSE](https://github.com/F-e-n-y-x/CarLink/blob/main/LICENSE)).

### Trademarks

The Changan name and logo are trademarks of Changan Automobile, used here only to identify the car; there is no official connection with the company. Logo from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Changan_icon.svg) (292Jacob, CC BY-SA 4.0).
CarPlay is a trademark of Apple Inc.; Android Auto is a trademark of Google LLC.

### For developers

Build and test notes are in [docs/BUILDING.md](docs/BUILDING.md); releasing is in [docs/RELEASING.md](docs/RELEASING.md). The local loop:

```sh
bash .claude/skills/carlink-dev/scripts/dev.sh build
bash .claude/skills/carlink-dev/scripts/dev.sh test
```

