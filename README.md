# SenPai Scanner 🔍

اپلیکیشن اندرویدی SenPai Scanner — همراه با سرویس VPN داخلی (`AyasaVpnService`).

## ✨ امکانات

- رابط کاربری Jetpack Compose
- سرویس VPN سیستمی از طریق `VpnService` اندروید
- APK آماده نصب — بدون نیاز به بیلد گرفتن

## 📦 دانلود مستقیم APK

فایل `app-debug.apk` مستقیماً از این مخزن قابل دانلود است:

**[⬇️ دانلود app-debug.apk](https://github.com/3krbkbkrbrbg/scanner-senpai-android-apk/raw/main/.build-outputs/app-debug.apk)**

### مراحل نصب

1. APK را دانلود کن.
2. گزینه «نصب از منابع ناشناس» را برای فایل‌منیجر یا مرورگرت فعال کن.
3. فایل را باز کن و Install را بزن.
4. هنگام اجرای سرویس VPN، دسترسی سیستم را تأیید کن.

## 🛠 بیلد از سورس

پیش‌نیاز: [Android Studio](https://developer.android.com/studio)

1. پروژه را در Android Studio باز کن.
2. اجازه بده ناسازگاری‌های import خودکار اصلاح شود.
3. در صورت نیاز یک فایل `.env` در ریشه پروژه بساز و `GEMINI_API_KEY` را داخلش قرار بده.
4. **Run** ▶️ — یا برای خروجی APK از منوی Build ← Build Bundle(s)/APK(s) استفاده کن.

این پروژه از [AI Studio](https://ai.studio/apps/6f14678a-3e2a-4a71-9bd5-efdc2ac783a6) هم قابل دسترسی است.
