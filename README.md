<p align="center">
  <img src="app/src/main/res/drawable-nodpi/hi.png" alt="H&H Programming Services" width="260">
</p>

<h1 align="center">DriveTunes</h1>

<p align="center">
  <a href="https://github.com/H-H-ps/DriveTunes/releases/latest/download/DriveTunes.apk">
    <img src="https://img.shields.io/badge/Download-APK-8B5CF6?style=for-the-badge&logo=android&logoColor=white" alt="Download APK">
  </a>
  <a href="https://github.com/H-H-ps/DriveTunes/releases/latest">
    <img src="https://img.shields.io/github/v/release/H-H-ps/DriveTunes?style=for-the-badge&color=EC4899&label=Latest" alt="Latest release">
  </a>
</p>

<div dir="rtl">

## 🎵 ما هو DriveTunes؟

**DriveTunes** مشغّل موسيقى مفتوح المصدر لأندرويد، فكرته بسيطة: أول ما هاتفك يتصل ببلوتوث سيارتك (أو أي جهاز تحدده)، يبدأ تلقائياً بتشغيل أغنية من ملفات الهاتف، بدون ما تلمس الشاشة وأنت تسوق.

كل شيء يعمل على هاتفك فقط: **بدون إنترنت، بدون حسابات، بدون إعلانات، وبدون أي تتبع.**

## ⬇️ التحميل والتثبيت

1. اضغط زر **Download APK** أعلاه، أو افتح صفحة [Releases](https://github.com/H-H-ps/DriveTunes/releases/latest) وحمّل الملف `DriveTunes.apk` من قسم **Assets**.
2. افتح الملف على هاتفك، وإذا طلب النظام السماح بالتثبيت من هذا المصدر فوافق.
3. النسخ الجديدة موقّعة بنفس المفتاح، فتقدر تحدّث فوق النسخة القديمة بدون ما تخسر إعداداتك وإحصائيات التشغيل.

> ملاحظة: قد يعرض Google Play Protect تنبيهاً لأن التطبيق غير منشور على المتجر. الكود كامل هنا ويمكنك مراجعته، ويوجد بجانب كل نسخة ملف `DriveTunes.apk.sha256` للتحقق من سلامة الملف.

## ✨ المميزات

**التشغيل التلقائي**
- يبدأ التشغيل عند اتصال جهاز بلوتوث تختاره (سيارتك مثلاً)، ويمكن اختيار أكثر من جهاز.
- تأخير قابل للضبط من 0 إلى 8 ثوانٍ لإعطاء السيارة وقتاً لتحويل الصوت على البلوتوث.
- ثلاث طرق للتشغيل التلقائي: **عشوائي تماماً**، أو **آخر أغنية من نفس النقطة**، أو **من البداية (أ - ي)**.
- إذا منع النظام التشغيل من الخلفية يظهر إشعار "اضغط لتشغيل الموسيقى" كحل احتياطي.

**المكتبة**
- مصدر الأغاني: كل الموسيقى على الجهاز، أو **مجلد معيّن**، أو **المفضلة فقط**.
- بحث بالاسم أو الفنان.
- ترتيب حسب: الأكثر استماعاً، الاسم (أ - ي / ي - أ)، الفنان، الأحدث، الأقدم، الأطول.
- عدّاد لعدد مرات تشغيل كل أغنية.
- المفضلة، ويمكن استبعاد أغانٍ من التشغيل العشوائي.
- زر تشغيل عشوائي لكل المكتبة.

**المشغّل**
- غلاف الألبوم وشريط تقدم متحرك بالألوان.
- تشغيل عشوائي (Shuffle) وتكرار الكل أو الأغنية الحالية.
- سحب الغلاف يميناً أو يساراً لتغيير الأغنية، أو لتشغيل أغنية عشوائية (خيار في الإعدادات).
- إشعار تحكم وودجت للشاشة الرئيسية (تشغيل/إيقاف، التالي، السابق).

**عام**
- واجهة داكنة أنيقة، وتدعم **العربية والإنجليزية** مع اتجاه RTL كامل.
- شاشة "حول التطبيق" فيها التواصل، وسياسة الخصوصية، ودعم المشروع.

## 📱 المتطلبات

- أندرويد 10 (API 29) أو أحدث.
- ملفات موسيقى مخزّنة على الهاتف.
- جهاز بلوتوث مقترن مسبقاً (السيارة مثلاً) لاستخدام التشغيل التلقائي.

## 🚀 أول تشغيل

1. اسمح بالصلاحيات: الموسيقى، البلوتوث (الأجهزة القريبة)، والإشعارات.
2. الإعدادات ← **أجهزة البلوتوث** ← اختر السيارة (يجب أن تكون مقترنة من قبل).
3. الإعدادات ← **العمل بالخلفية** ← سماح (مهم ليعمل التشغيل التلقائي والتطبيق مغلق).
4. اختياري: اختر مجلد الأغاني، وطريقة التشغيل التلقائي، وفعّل "السوايب يشغل أغنية عشوائية".

على بعض الهواتف (شاومي، هواوي، سامسونج...) اسمح أيضاً للتطبيق بـ **التشغيل التلقائي / Autostart** من إعدادات النظام.

## 🔐 الصلاحيات ولماذا

| الصلاحية | السبب |
|---|---|
| الصوتيات (READ_MEDIA_AUDIO) | عرض وتشغيل الأغاني الموجودة على هاتفك |
| الأجهزة القريبة (BLUETOOTH_CONNECT) | معرفة الأجهزة المقترنة ووقت اتصال جهازك المختار |
| الإشعارات | أزرار التحكم بالتشغيل وتنبيه "اضغط للتشغيل" |
| خدمة أمامية للوسائط | استمرار التشغيل عند إغلاق الشاشة |
| استثناء تحسين البطارية (اختياري) | عمل التشغيل التلقائي والتطبيق مغلق |

## 🛡️ الخصوصية

التطبيق **لا يملك صلاحية الإنترنت** ولا يجمع أو يرسل أي بيانات. الإعدادات والمفضلة وعدّاد التشغيل تُحفظ على جهازك فقط وتُحذف بحذف التطبيق. سياسة الخصوصية الكاملة داخل التطبيق: الإعدادات ← عن DriveTunes.

## 🛠️ التقنيات

Kotlin • Jetpack Compose (Material 3) • AndroidX Media3 (ExoPlayer + MediaSession) • Gradle Kotlin DSL • GitHub Actions

## 🧱 البناء من الكود

```bash
gradle :app:assembleRelease
```
ملف الـ APK يظهر في `app/build/outputs/apk/release/`. مفتاح التوقيع الرسمي غير موجود في المستودع (يُحفظ في GitHub Secrets)، فعند البناء محلياً يُوقَّع الملف بمفتاح التطوير ويعمل بشكل عادي، لكنه لن يتحدّث فوق النسخ الرسمية. وعند كل push على الفرع الرئيسي يبني GitHub Actions الملف وينشره تلقائياً في [Releases](https://github.com/H-H-ps/DriveTunes/releases).

## 📬 التواصل

hh.programming.services@gmail.com — **H&H Programming Services**

</div>

---

## 🎵 What is DriveTunes?

**DriveTunes** is an open-source Android music player with one simple idea: as soon as your phone connects to your car's Bluetooth (or any device you choose), it automatically starts playing a song from your phone — no touching the screen while driving.

Everything stays on your phone: **no internet, no accounts, no ads, no tracking.**

## ⬇️ Download & install

1. Tap **Download APK** above, or open [Releases](https://github.com/H-H-ps/DriveTunes/releases/latest) and get `DriveTunes.apk` from **Assets**.
2. Open the file on your phone and allow installing from this source if asked.
3. New versions are signed with the same key, so you can update in place and keep your settings and play stats.

> Note: Google Play Protect may show a warning because the app isn't on the Play Store. The full source is here, and each release includes a `DriveTunes.apk.sha256` file to verify the download.

## ✨ Features

**Auto-play**
- Starts when a Bluetooth device you choose connects (your car, for example); multiple devices supported.
- Adjustable delay (0–8 s) so the car has time to switch audio to Bluetooth.
- Three modes: **fully random**, **resume the last song from where it stopped**, or **from the beginning (A–Z)**.
- If Android blocks background playback, a "tap to play" notification is shown as a fallback.

**Library**
- Song source: all music on the device, **a specific folder**, or **favorites only**.
- Search by title or artist.
- Sort by: most played, title A–Z / Z–A, artist, newest, oldest, longest.
- Per-song play counter.
- Favorites, plus the option to exclude songs from random play.
- One-tap random play for the whole library.

**Player**
- Album art and an animated gradient progress bar.
- Shuffle and repeat (all / one).
- Swipe the cover left or right to change song, or to play a random one (setting).
- Media notification and a home-screen widget (play/pause, next, previous).

**General**
- Dark, modern UI in **Arabic and English** with full RTL support.
- "About" screen with contact links, privacy policy and ways to support the project.

## 📱 Requirements

- Android 10 (API 29) or newer.
- Music files stored on the phone.
- A previously paired Bluetooth device (your car, for example) to use auto-play.

## 🚀 First run

1. Grant the permissions: music, Bluetooth (nearby devices) and notifications.
2. Settings → **Bluetooth devices** → pick your car (it must already be paired).
3. Settings → **Run in the background** → Allow (needed so auto-play works while the app is closed).
4. Optional: choose a music folder and the auto-play mode, and enable "Swipe plays a random song".

On some phones (Xiaomi, Huawei, Samsung…) also allow **Autostart** in the system settings.

## 🔐 Permissions

| Permission | Why |
|---|---|
| Audio (READ_MEDIA_AUDIO) | List and play the songs on your phone |
| Nearby devices (BLUETOOTH_CONNECT) | See paired devices and detect when your chosen one connects |
| Notifications | Playback controls and the "tap to play" prompt |
| Media foreground service | Keep playing when the screen is off |
| Battery optimization exemption (optional) | Auto-play while the app is closed |

## 🛡️ Privacy

The app has **no internet permission** and doesn't collect or send any data. Settings, favorites and play counts stay on your device and are removed when you uninstall. Full policy in the app: Settings → About DriveTunes.

## 🛠️ Tech

Kotlin • Jetpack Compose (Material 3) • AndroidX Media3 (ExoPlayer + MediaSession) • Gradle Kotlin DSL • GitHub Actions

## 🧱 Build from source

```bash
gradle :app:assembleRelease
```
The APK appears in `app/build/outputs/apk/release/`. The official signing key is not in the repo (it lives in GitHub Secrets), so local builds are signed with the debug key: they install fine but won't update over the official releases. On every push to the main branch, GitHub Actions builds it and publishes it under [Releases](https://github.com/H-H-ps/DriveTunes/releases).

## 📬 Contact

hh.programming.services@gmail.com — **H&H Programming Services**
