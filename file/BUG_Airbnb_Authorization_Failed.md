# باگ: «Airbnb service authorization failed. Please try again later.»

## ۱. باگ چیست؟

در صفحه‌ی **Registering Home Information**، کاربر لینک Airbnb را وارد می‌کند
(`https://www.airbnb.ca/rooms/173077937794...`) و دکمه‌ی **Retrieve from Airbnb** را می‌زند.
به جای اطلاعات خانه، این Toast نمایش داده می‌شود:

> Airbnb service authorization failed. Please try again later.

### این پیام از کجا می‌آید؟

`app/src/main/java/com/marvelous/betterrank/viewModel/HomeRegisterViewModel.kt:71`

```kotlin
showMessage.value = when (response.code()) {
    401, 403 -> "Airbnb service authorization failed. Please try again later."
    else -> "Server error: ${response.code()}"
}
```

یعنی سرور جواب **HTTP 401 یا 403** داده است.

نکته‌ی مهم: اپ **مستقیماً به Airbnb وصل نمی‌شود**، بلکه به سرویس **BrightData** وصل می‌شود:

| فایل | مقدار |
|---|---|
| `HiltNetworkModule.kt` | `BRIGHT_DATA_URL = "https://api.brightdata.com/datasets/v3/scrape"` |
| `HiltNetworkModule.kt` | `BRIGHT_DATA_DATA_SET_ID = "gd_ld7ll037kqy322v05"` |
| `RemoteConfigManager.kt` | توکن از Firebase Remote Config با کلید `bright_data_token` خوانده می‌شود |

پس این پیام یعنی: **BrightData توکن ما را قبول نکرده است.** مشکل از لینک Airbnb یا کاربر نیست.

---

## ۲. مسیر درخواست (برای فهم باگ)

```
HiltApplication.onCreate
   └─ RemoteConfigManager.fetchAndActivate()        ← توکن از Firebase می‌آید
   └─ HiltNetworkModule.loadKeysFromRemoteConfig()  ← در BRIGHT_DATA_TOKEN ذخیره می‌شود

کاربر «Retrieve from Airbnb» را می‌زند
   └─ HomeRegisterViewModel.getAirbnbInfo(url)
       └─ AppRepository.getAirbnbInfo(url)
           ├─ اگر توکن خالی بود → یک بار دیگر Remote Config را fetch می‌کند
           └─ RequestApi.getAirbnbInfo  (POST به BrightData)
               └─ Interceptor در HiltNetworkModule:
                    اگر توکن خالی نباشد → هدر  Authorization: Bearer <token>
                    اگر توکن خالی باشد  → درخواست **بدون هدر** ارسال می‌شود ❌
   └─ پاسخ 401/403 → Toast «authorization failed»
```

---

## ۳. علت‌های احتمالی (به ترتیب احتمال)

### علت A — توکن BrightData نامعتبر / منقضی / حذف شده (سمت سرور) ⭐ محتمل‌ترین
- توکن در پنل BrightData منقضی یا Revoke شده،
- یا **موجودی (balance) حساب BrightData تمام شده**،
- یا توکن دسترسی به dataset `gd_ld7ll037kqy322v05` را ندارد،
- یا در پنل BrightData، **IP Allowlist** فعال است و IP گوشی کاربر مجاز نیست.

در این حالت کد اپ درست کار می‌کند ولی BrightData جواب 401/403 می‌دهد.

### علت B — توکن کهنه در کش Remote Config (سمت اپ)
در `RemoteConfigManager.kt`:
```kotlin
.setMinimumFetchIntervalInSeconds(3600) // 1 hour
```
اگر توکن را در Firebase عوض کنید، گوشی‌هایی که قبلاً توکن قدیمی را گرفته‌اند تا **حداقل یک ساعت**
(و اگر fetch خطا بخورد، حتی بیشتر) همان توکن قدیمیِ باطل را می‌فرستند و 401/403 می‌گیرند.
در ضمن چون توکن خالی نیست، شرط `isBlank()` در `AppRepository` هم دوباره fetch نمی‌کند.

### علت C — توکن خالی، ولی درخواست باز هم ارسال می‌شود (سمت اپ)
اگر Remote Config fetch نشود (اینترنت ضعیف، Firebase در دسترس نباشد، یا مقدار
`bright_data_token` در کنسول Firebase تعریف/Publish نشده باشد)، توکن خالی می‌ماند و
Interceptor درخواست را **بدون هدر Authorization** می‌فرستد → 401 «Credentials are missing».

### علت D — هیچ اطلاعاتی برای عیب‌یابی نداریم
در `HomeRegisterViewModel` و `AppRepository` بدنه‌ی خطا (`errorBody`) خوانده می‌شود ولی
لاگ‌ها کامنت شده‌اند؛ پس نمی‌دانیم BrightData دقیقاً چه گفته است.

---

## ۴. حل مرحله‌به‌مرحله

### مرحله ۱ — اول علت واقعی را پیدا کن (۵ دقیقه)

اپ از **Chucker** استفاده می‌کند (`ChuckerInterceptor` در `HiltNetworkModule`).
1. اپ را در حالت debug اجرا کن و خطا را بازتولید کن.
2. نوتیفیکیشن Chucker را باز کن → درخواست `api.brightdata.com/datasets/v3/scrape` را پیدا کن.
3. این دو چیز را نگاه کن:
   - **Request Headers**: آیا `Authorization: Bearer ...` وجود دارد؟
     - **ندارد** → علت C (برو مرحله ۳ و ۴).
     - **دارد** → توکن ارسال شده ولی رد شده → علت A یا B (برو مرحله ۲).
   - **Response Body**: متن خطای BrightData را بخوان (مثلاً `Credentials are missing`،
     `Invalid token`، `Insufficient balance`، ...).

می‌توانی توکن را مستقل از اپ هم تست کنی (توکن را از کنسول Firebase بردار):

```bash
curl -i -X POST \
  "https://api.brightdata.com/datasets/v3/scrape?dataset_id=gd_ld7ll037kqy322v05" \
  -H "Authorization: Bearer <BRIGHT_DATA_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"input":[{"url":"https://www.airbnb.ca/rooms/<ROOM_ID>"}]}'
```

- اگر curl هم 401/403 داد → مشکل **فقط** سمت BrightData است (مرحله ۲). کد اپ را لازم نیست برای حل خود باگ دست بزنی.
- اگر curl جواب 200 داد ولی اپ 401/403 می‌گیرد → مشکل سمت اپ است (مرحله ۳ تا ۵).

### مرحله ۲ — درست‌کردن توکن در BrightData و Firebase (علت A)

1. وارد پنل BrightData شو → **Account settings → API tokens / API keys**.
2. بررسی کن:
   - توکن فعلی فعال است؟ تاریخ انقضا دارد؟
   - **Billing / balance** حساب مثبت است؟
   - توکن دسترسی به **Web Scraper API / Datasets** (و dataset `gd_ld7ll037kqy322v05`) دارد؟
   - **IP allowlist** خاموش باشد (گوشی‌های کاربران IP ثابت ندارند).
3. در صورت نیاز یک توکن جدید بساز.
4. در **Firebase Console → Remote Config**، مقدار پارامتر `bright_data_token` را با توکن جدید عوض کن
   و حتماً **Publish changes** را بزن (بدون Publish، به گوشی‌ها نمی‌رسد).
5. نام پارامتر دقیقاً باید `bright_data_token` باشد (همانی که در `RemoteConfigManager.kt` خوانده می‌شود).

### مرحله ۳ — وقتی توکن خالی است، درخواست را نفرست (علت C)

در `AppRepository.getAirbnbInfo` بعد از تلاش مجدد، اگر توکن هنوز خالی بود، همان‌جا خطا برگردان
به جای اینکه درخواست بی‌هدر بفرستی:

```kotlin
if (HiltNetworkModule.BRIGHT_DATA_TOKEN.isBlank()) {
    RemoteConfigManager.fetchAndActivate()
    HiltNetworkModule.loadKeysFromRemoteConfig()
}

// still no token → don't send an unauthenticated request
if (HiltNetworkModule.BRIGHT_DATA_TOKEN.isBlank()) {
    return@withContext Result.failure(IllegalStateException("BrightData token is not available"))
}
```

و در `RemoteConfigManager.fetchAndActivate()` خطا را حداقل لاگ کن تا بدانی fetch شکست خورده:

```kotlin
} catch (e: Exception) {
    android.util.Log.w("RemoteConfig", "fetchAndActivate failed", e)
}
```

### مرحله ۴ — اگر 401/403 آمد، توکن تازه بگیر و یک بار دوباره امتحان کن (علت B)

۱) در `RemoteConfigManager` یک تابع برای fetch اجباری (بدون کش) اضافه کن:

```kotlin
suspend fun forceFetchAndActivate() {
    try {
        remoteConfig.fetch(0).await()   // 0 = ignore the 1-hour cache
        remoteConfig.activate().await()
    } catch (e: Exception) {
        android.util.Log.w("RemoteConfig", "forceFetchAndActivate failed", e)
    }
}
```

۲) در `AppRepository.getAirbnbInfo` اگر جواب 401 یا 403 بود، یک بار توکن را تازه کن و دوباره بفرست:

```kotlin
return@withContext try {
    var response = requestApi.getAirbnbInfo(input = airBnbRequestModel)

    // token may have been rotated in Remote Config while this device still has the cached one
    if (response.code() == 401 || response.code() == 403) {
        val oldToken = HiltNetworkModule.BRIGHT_DATA_TOKEN
        RemoteConfigManager.forceFetchAndActivate()
        HiltNetworkModule.loadKeysFromRemoteConfig()
        if (HiltNetworkModule.BRIGHT_DATA_TOKEN != oldToken) {
            response = requestApi.getAirbnbInfo(input = airBnbRequestModel)
        }
    }
    Result.success(response)
} catch (e: Exception) {
    Result.failure(e)
}
```

> توجه: Firebase برای fetch با interval صفر محدودیت (throttling) دارد، ولی چون فقط هنگام 401/403
> صدا زده می‌شود مشکلی ایجاد نمی‌کند.

### مرحله ۵ — بدنه‌ی خطا را لاگ کن (علت D)

در `HomeRegisterViewModel.getAirbnbInfo` لاگ خطا را (فقط در debug) فعال کن تا دفعه‌ی بعد علت دقیق را ببینی:

```kotlin
val errorBody = response.errorBody()?.string() ?: "No error body"
if (BuildConfig.DEBUG) {
    Log.e("BRIGHTDATA_ERROR", "HTTP ${response.code()}\n$errorBody")
}
```

نکته: `errorBody()?.string()` فقط **یک بار** قابل خواندن است. الان هم در `AppRepository` و هم در
ViewModel خوانده می‌شود؛ در ViewModel همیشه خالی برمی‌گردد. فقط در یک جا بخوان (ترجیحاً ViewModel)
و خواندن آن را از `AppRepository` حذف کن.

### مرحله ۶ (اختیاری) — پیام بهتر برای کاربر
پیام فعلی می‌گوید «بعداً امتحان کن» ولی کاربر کاری از دستش برنمی‌آید. پیشنهاد:
- 401/403 → «Couldn't fetch the listing right now. You can upload a photo of the page instead.»
  (چون گزینه‌ی Take Photo / Choose Picture همان صفحه وجود دارد.)

---

## ۵. تست بعد از رفع

| سناریو | نتیجه‌ی مورد انتظار |
|---|---|
| توکن معتبر در Remote Config، لینک Airbnb درست | اطلاعات خانه لود می‌شود (HTTP 200) |
| توکن را در Firebase عوض کن و Publish کن، بدون پاک کردن دیتای اپ دوباره تست کن | بعد از اولین 401/403، توکن تازه گرفته می‌شود و درخواست دوم موفق است |
| پارامتر `bright_data_token` را موقتاً خالی کن | هیچ درخواست بی‌هدری در Chucker دیده نمی‌شود؛ پیام خطای مناسب نمایش داده می‌شود |
| حالت هواپیما | پیام خطای شبکه (نه authorization) |
| بیلد release (با R8) | همان رفتار بیلد debug |

---

## ۶. خلاصه

1. **علت مستقیم:** BrightData به درخواست جواب 401/403 داده → توکن `bright_data_token` نامعتبر، منقضی، بدون موجودی، یا اصلاً ارسال نشده.
2. **اول** با Chucker یا curl مشخص کن توکن ارسال شده یا نه و BrightData دقیقاً چه گفته.
3. **حل اصلی (سرور):** توکن/حساب BrightData را درست کن و در Firebase Remote Config مقدار جدید را Publish کن.
4. **مقاوم‌سازی (اپ):** درخواست بی‌توکن نفرست، هنگام 401/403 توکن را force-refresh کن و یک بار retry کن، و errorBody را در debug لاگ کن.
