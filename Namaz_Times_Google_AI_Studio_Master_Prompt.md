# NAMAZ TIMES — Complete Google AI Studio Master Prompt

> **Document type:** Single master build prompt + implementation specification + reference code contracts  
> **Target:** Native Android application built in Google AI Studio Build mode  
> **Working app name:** Namaz Times  
> **Primary user profile:** Muslim user following Ahl al-Hadith; mosque-entered local timings must take precedence over calculated fallback timings  
> **Primary language:** English, with Arabic prayer names and optional Urdu localization  
> **Default region for first-run examples:** Karachi, Sindh, Pakistan  
> **Data policy:** Local-first, offline-capable, no account, no advertising, no tracking

---

# PART 1 — MASTER BUILD INSTRUCTION FOR GOOGLE AI STUDIO

## 1.1 Your role and the expected result

Act as a senior Android engineer, product designer, accessibility specialist, location-services engineer, and QA engineer. Build **Namaz Times**, a complete, polished, production-minded native Android application for prayer schedules, mosque-specific Adhan and Iqamah reminders, Qibla direction, Hijri dates, prayer tracking, and a Quran Ruku reading target with an automatically calculated backlog.

This is an implementation request, not a request for a design concept, static prototype, screenshot, or clickable mockup. Generate a working Android project with real persistence, real scheduling, real permissions, real user interactions, validated input, and tests for the important business rules.

**Build mode:** Select **Android** in Google AI Studio's platform picker. Do not build a React website or a WebView app. Use Kotlin and Jetpack Compose. Follow the currently supported Google AI Studio Android project constraints: a single-activity, single-module Android project using Jetpack Compose. Organize the code into multiple Kotlin packages and files inside that module. Do not place the entire application in one giant `MainActivity.kt` file.

The Markdown document is deliberately one self-contained master prompt. The generated Android project should still use the correct separate Kotlin, Gradle, manifest, drawable, and resource files. Do not sacrifice maintainability by putting Android resources or every class into one source file.

## 1.2 Non-negotiable delivery rules

1. Generate the actual application, not merely a plan or an explanation.
2. Implement every feature described in this document. If a feature is restricted by Android, explain the restriction in the app and implement the best honest fallback.
3. Do not create fake prayer data, fake mosque records, sample users, placeholder notifications, simulated GPS, fake countdowns, or dummy Quran progress in the production experience.
4. Do not hardcode invented Adhan or Iqamah times for Defence, Clifton, Hala New City, or any mosque. These locations are examples only. The user must enter or import the real mosque timetable.
5. Do not claim that a calculated prayer time is an official mosque time. Label every time source clearly: **Mosque timetable**, **Calculated**, or **Manual override**.
6. Do not silently change the user's selected calculation method, prayer times, mosque, timezone, Hijri adjustment, or Quran history.
7. Do not silently request every permission on first launch. Explain a feature, request only its necessary permission at the moment it is enabled, and keep the app useful when permission is denied.
8. Do not use a server, Firebase, cloud database, analytics, advertising SDK, account system, telemetry, or remote location tracking. This is a local-only app.
9. Do not add an AI chatbot or use a generative AI model at runtime. This app is a deterministic utility. It must continue working without an internet connection.
10. Do not use `TODO`, `FIXME`, empty callbacks, placeholder functions, ellipses in place of implementation, fake success messages, or comments such as `// implement later` in delivered code.
11. Do not catch and ignore exceptions. Handle expected failures, log technical details locally without sensitive data, and show a useful recovery message.
12. All controls shown in the interface must work. Hide a feature until it is implemented rather than presenting a non-functional button.
13. Use real state from Room/DataStore and Android system APIs. Do not use hardcoded UI-only values for persisted features.
14. Include database migrations, backup/import validation, alarm rescheduling, and automated tests.
15. Before finishing, build/compile the app, resolve compilation errors, run available tests, and inspect the app on a narrow Android phone layout and a larger phone layout.
16. If Google AI Studio cannot complete all implementation in one generation pass, continue in focused passes while preserving the existing project. Never delete working code or replace implemented screens with mockups to make the build appear complete.
17. Do not remove a feature because it is difficult. Implement an honest supported fallback and clearly document any platform limitation.
18. Use current stable AndroidX/Jetpack dependencies compatible with the Android SDK and Google AI Studio environment. Pin dependencies to resolved versions; never invent artifact coordinates.
19. Use a documented, maintained prayer-calculation library for astronomy. Do not write an untested approximation of solar-position mathematics.
20. Do not include copyrighted Adhan recordings or proprietary fonts unless they are properly licensed. Allow the user to select/import their own audio.

## 1.3 Product definition

Namaz Times helps the user observe prayer times based on the **mosque and place they choose**, with automatic location switching as an optional convenience. It must support saved places such as:

- Defence, Karachi
- Clifton, Karachi
- Hala New City, Sindh
- Home
- Office
- Any other place the user adds

Each saved place can be associated with a mosque profile. A mosque profile stores its own exact Adhan and Iqamah timetable, Friday Khutbah and Jumuah schedule, location, timezone, reminder preferences, and active/disabled state.

The app must never assume that all mosques in one neighbourhood follow the same timetable. The user decides which mosque profile belongs to a saved place. The user can manually select a profile, manually drop a pin, or use automatic location detection.

The app includes five main areas:

1. **Today:** next prayer, countdown, selected mosque, daily prayer timeline, Hijri date, and quick actions.
2. **Places:** saved areas, mosques, manual selection, GPS matching, and travel mode.
3. **Qibla:** compass, bearing, accuracy/calibration status, and map/bearing fallback.
4. **Quran:** daily Ruku goal, reading progress, backlog, optional surplus credit, and reading history.
5. **More:** prayer history, Qada tracking, widgets, notification settings, calculation settings, backup/restore, and app preferences.

## 1.4 Religious accuracy and Ahl al-Hadith handling

The user follows Ahl al-Hadith. Respect that preference without pretending there is one universally authoritative worldwide Ahl al-Hadith calculation preset.

- Mosque-entered prayer times are authoritative for that selected mosque profile.
- The calculation engine is a fallback when no mosque timetable is available, when the user explicitly selects Travel/Calculated mode, or when the user chooses a calculated preview.
- Provide editable calculation settings: calculation preset, Fajr angle, Isha angle or interval, Asr shadow factor, high-latitude rule, and per-prayer minute adjustments.
- Provide a clearly named **Standard Asr (one shadow-length factor)** option and a **Hanafi Asr (two shadow-length factor)** option. The default suggested profile for an Ahl al-Hadith user may select Standard Asr, but the app must explain that the user should verify the exact practice and local timetable with the mosque or scholar they follow. Do not label a particular angle preset as a definitive Ahl al-Hadith ruling.
- Offer the **Karachi calculation preset** as a convenient regional fallback for Pakistan. In the referenced Adhan Kotlin library, this preset uses Fajr 18° and Isha 18°. It is a calculation setting, not a promise that it matches any mosque's timetable.
- Do not issue religious rulings, judge the user's worship, or mark prayers/Quran reading automatically without explicit user action.
- Show the source and configuration behind calculated times in a detail sheet.
- If two local mosques differ, show the selected mosque's times, not an average or a silent compromise.
- Preserve the user's original mosque timetable when a calculation setting changes.

Add an informational note in calculation settings:

> “Calculated prayer times are estimates based on the selected astronomical method and settings. Your selected mosque's published timetable may differ. Follow the mosque or trusted scholar you normally follow.”

## 1.5 First launch and onboarding

Create a short, skippable onboarding flow with these steps:

### Step A — Welcome

- App name: Namaz Times.
- Explain that the app stores mosque timings and prayer history locally on the device.
- State that no login is required.
- State that automatic location switching is optional.
- Provide **Set up my first place** and **Explore manually** actions.

### Step B — Choose the first place

Offer:

- Use current location (request foreground location only after the user taps it).
- Search/type a place name and enter coordinates manually.
- Drop a pin on a map if an available map provider is configured, or use a local coordinate-entry screen.
- Add a place without GPS.

For the first-run example, suggest a place named “Defence, Karachi” only as a blank user-editable profile. Do not insert an actual mosque or fake prayer times.

### Step C — Choose a mosque profile

Allow the user to:

- Create a new mosque profile.
- Add mosque name, address/area label, latitude/longitude (optional), timezone, and notes.
- Enter Adhan and Iqamah times for all five prayers.
- Enter Friday Khutbah start and Jumuah prayer start.
- Save a profile with incomplete timing fields, but clearly mark missing times as **Not entered** and do not schedule alarms for them.

### Step D — Choose timing mode

Options:

- Mosque timetable (recommended when the user has entered a mosque schedule).
- Calculated times.
- Manual selection / Travel mode.

Explain the difference before the user confirms.

### Step E — Notifications

Explain what Adhan reminders, Iqamah reminders, and pre-prayer notifications do. Ask for Android notification permission when the user enables notifications. Create notification channels before posting notifications.

### Step F — Optional automatic switching

Explain that automatic switching uses saved place boundaries and background location permission on supported Android versions. Do not request background location during initial welcome. Let the user finish setup without it.

### Step G — Optional prayer silence

Explain that automatic silence may require Android Do Not Disturb policy access. Do not enable it without explicit opt-in and the required Android permission/access.

At the end, show a setup checklist with statuses: mosque profile, prayer times, notifications, exact alarms, location automation, Qibla sensor, and backup. A missing permission must not block the rest of the application.

---

# PART 2 — PLATFORM, ARCHITECTURE, AND PROJECT STRUCTURE

## 2.1 Android technical target

- Native Android application.
- Kotlin.
- Jetpack Compose and Material 3.
- Single Activity, single Android module, multiple packages/files.
- Minimum SDK: API 26 or another minimum required by the selected compatible dependencies. Prefer API 26+ to simplify `java.time` use, or correctly enable core-library desugaring if a lower minimum is selected.
- Target and compile SDK: the current stable versions supported by Google AI Studio at build time.
- Edge-to-edge layout with correct system-bar and display-cutout insets.
- Portrait-first, responsive phone UI; support landscape without clipping critical controls.
- Offline-first storage. Do not require internet to display existing schedules, calculate prayer times, show Qibla bearing, or track Quran reading.

## 2.2 Architecture

Use a clean, testable structure inside the one supported Android module:

```text
app/
  src/main/
    AndroidManifest.xml
    java/com/namaztimes/app/
      NamazTimesApplication.kt
      MainActivity.kt
      navigation/
        AppNavHost.kt
        AppDestination.kt
      core/
        time/
          DateTimeProvider.kt
          PrayerDayResolver.kt
          TimeZoneResolver.kt
        permissions/
          PermissionStatus.kt
          PermissionCoordinator.kt
        notifications/
          NotificationChannels.kt
          PrayerNotificationFactory.kt
        design/
          AppTheme.kt
          AppColors.kt
          AppTypography.kt
          FrostedCard.kt
          AppIcons.kt
      data/
        local/
          NamazDatabase.kt
          dao/
          entity/
          migrations/
        repository/
          MosqueRepository.kt
          PrayerScheduleRepository.kt
          PrayerHistoryRepository.kt
          QuranRepository.kt
          SettingsRepository.kt
          BackupRepository.kt
      domain/
        model/
        usecase/
          ResolveActiveMosqueUseCase.kt
          CalculatePrayerTimesUseCase.kt
          RebuildPrayerScheduleUseCase.kt
          GetNextPrayerUseCase.kt
          RecordPrayerUseCase.kt
          RecalculateQuranLedgerUseCase.kt
      location/
        LocationRepository.kt
        GeofenceManager.kt
        GeofenceBroadcastReceiver.kt
        LocationConfidenceEvaluator.kt
      prayer/
        AdhanPrayerCalculator.kt
        PrayerOccurrenceFactory.kt
        PrayerScheduleResolver.kt
        PrayerAlarmScheduler.kt
        PrayerAlarmReceiver.kt
        ScheduleRebuildCoordinator.kt
      qibla/
        QiblaCalculator.kt
        CompassSensorManager.kt
        CompassAccuracy.kt
      hijri/
        HijriDateProvider.kt
        HijriAdjustment.kt
      quran/
        QuranLedgerEngine.kt
        QuranReadingRepository.kt
      widgets/
        TodayGlanceWidget.kt
        WidgetUpdateCoordinator.kt
      receivers/
        BootReceiver.kt
        TimeChangeReceiver.kt
        PackageReplaceReceiver.kt
      ui/
        today/
        places/
        mosque/
        qibla/
        quran/
        history/
        settings/
        backup/
    res/
      drawable/
      drawable-nodpi/
      mipmap-anydpi-v26/
      values/
        colors.xml
        strings.xml
        themes.xml
      xml/
        backup_rules.xml
        data_extraction_rules.xml
        shortcuts.xml
```

This is a logical organization guide. Adapt package/file names to Google AI Studio's generated project, but keep responsibilities separated and names clear.

## 2.3 Dependency rules

Use these implementation choices unless the current Android environment requires a compatible alternative:

- Jetpack Compose + Material 3 for all app screens.
- AndroidX Navigation Compose for navigation.
- Room for durable relational app data.
- DataStore Preferences for simple app settings and user preferences.
- Kotlin Coroutines and Flow for asynchronous data and observable database state.
- Android AlarmManager for exact user-requested prayer notification times, with permission checks and graceful fallback.
- Android NotificationManager for notification channels and actions.
- Fused Location Provider and Android Geofencing API when available and configured.
- Android SensorManager / rotation vector / accelerometer + magnetometer for compass heading.
- `java.time` for dates, instants, zones, and DST-safe scheduling.
- Jetpack Glance for app widgets if the current Android build environment supports the needed dependency.
- Storage Access Framework for local backup export/import.
- A maintained prayer time library such as `com.batoulapps.adhan:adhan2:0.0.7` for astronomical calculations. Verify the current artifact, API, and dependency compatibility during build. Do not invent a new version or API.

Do not add a network geocoding service as a requirement. Place names and mosque details can be entered manually. Optional reverse geocoding must fail gracefully and must never prevent saving coordinates.

## 2.4 Local-only data and privacy

- No account, email, phone number, or login.
- No analytics or advertising.
- No remote logging.
- No automatic upload of GPS coordinates, mosque data, prayer history, Quran activity, or backup files.
- Request only the Android permissions needed for features the user enables.
- Store location coordinates, prayer logs, Quran logs, and schedules in local Room tables.
- Store non-relational preferences in DataStore.
- Use Android's app-private storage by default.
- Use the Storage Access Framework for user-selected backup locations. Never scan unrelated files.
- Do not add broad storage permissions when SAF is sufficient.
- Do not add `INTERNET` permission unless an explicitly implemented optional feature truly requires it. Core app features must not require internet.
- If an optional external map is opened, use an explicit user action and Android map intent. Explain that the external map app/provider may receive the location the user chose to open.
- Support a password-protected export using authenticated encryption such as AES-GCM if implemented. Do not invent cryptography. Use platform cryptography, a random nonce, a derived key from a user-supplied passphrase using a recognized KDF, and versioned metadata. Never store the passphrase.
- Protect database migrations and backups from data loss. Never silently clear the database to fix a migration error.

---

# PART 3 — PRAYER AND MOSQUE DATA MODEL

## 3.1 Separate a place from a mosque

Do not merge a geographic area and a mosque into one ambiguous record. Use two entities:

### SavedPlace

Fields:

- `id`: stable UUID.
- `displayName`: e.g. Defence, Clifton, Hala New City, Home, Office.
- `latitude`, `longitude`: nullable when not known.
- `geofenceRadiusMeters`: user-configurable; recommended initial value 150–300 m for a local place pin, but explain that actual boundary size depends on the place.
- `timezoneId`: explicit IANA timezone where known; default Pakistan examples to `Asia/Karachi` only when the user selects Pakistan or confirms the timezone.
- `mosqueProfileId`: optional linked mosque.
- `priorityRank`: integer; a smaller number means higher user priority when multiple saved areas overlap.
- `isEnabled`: disabled places must not participate in automatic selection.
- `isPriorityLocation`: optional user-marked priority.
- `createdAt`, `updatedAt`.

### MosqueProfile

Fields:

- `id`: stable UUID.
- `name`: mosque name entered by user.
- `addressLabel`: free-text area/address.
- `latitude`, `longitude`: nullable.
- `timezoneId`: IANA timezone.
- `isEnabled`: user may disable temporarily without deleting.
- `isArchived`: archived profiles remain available for historical records but do not participate in matching.
- `userPriority`: integer.
- `notes`: optional.
- `defaultReminderProfileId`: optional.
- `createdAt`, `updatedAt`.

One SavedPlace may point to one preferred MosqueProfile. A MosqueProfile may be associated with more than one SavedPlace if the user chooses. Do not silently duplicate mosque records.

## 3.2 Mosque schedule model

A single fixed set of times is not sufficient for every mosque because mosque timetables may change by season, month, Ramadan, or a specific date. Support multiple schedule layers with deterministic precedence.

Each mosque must support:

1. **Recurring weekly schedule:** a per-weekday timetable when the mosque genuinely uses one.
2. **Fixed daily schedule:** one wall-clock time per prayer, repeated daily until changed.
3. **Seasonal/date-range schedule:** a timetable that is active between `effectiveFrom` and `effectiveUntil`.
4. **One-date override:** a special timetable for a specific local Gregorian date.
5. **Manual per-prayer override:** an explicit edit for an individual prayer/time.

Use this precedence for a date:

`one-date override > active date-range/seasonal schedule > weekday schedule > fixed daily schedule > calculated fallback`.

If two same-precedence entries conflict, do not choose silently. Reject the save with a clear conflict message or ask the user which entry should win.

For every prayer, store separate values:

- Adhan time (nullable until entered).
- Iqamah time (nullable until entered).
- Optional enable flags for Adhan notification and Iqamah notification.
- Optional per-prayer note.

Prayer set:

- Fajr
- Dhuhr
- Asr
- Maghrib
- Isha

Add `Sunrise` as an informational calculated event only. Never treat Sunrise as one of the five obligatory prayer reminders.

Add Friday fields separately:

- `khutbahStartTime` (nullable).
- `jumuahPrayerTime` (nullable).
- Optional second Jumuah time if a mosque has multiple congregations.
- Optional Friday-specific Adhan/Iqamah override for Dhuhr/Jumuah.
- Friday schedule notes.

Do not infer the Khutbah time from Dhuhr or Iqamah. It must be entered separately.

## 3.3 Schedule validation

When saving a schedule:

- Validate `HH:mm` wall-clock inputs.
- Warn if a prayer's Iqamah is earlier than its Adhan on the same local date, but allow a user-confirmed exception if the mosque timetable genuinely requires it.
- Allow a valid time to cross midnight only if the user explicitly configures a date-specific overnight event; do not silently roll a prayer time to the next day.
- Warn if two reminders for the same mosque/prayer/event would occur at the same instant.
- Detect missing prayer entries and show them as missing rather than deriving them invisibly.
- Preserve old timetable versions and the prayer-history snapshot that was used when a reminder was scheduled.
- Store local wall-clock time plus the date and timezone used to resolve its actual `Instant`.
- Recalculate future occurrences when timezone, daylight-saving rules, selected mosque, or the underlying schedule changes.

## 3.4 Mosque editor UX

Create a clean editor with a five-row table. Each row has:

- Prayer name and optional Arabic label.
- Adhan time field.
- Iqamah time field.
- “Remind me” switches for Adhan and Iqamah.
- Optional sound/vibration selectors.

The user can tap a time cell to open a native time picker. Add actions:

- Save timetable.
- Duplicate timetable to another season/date range.
- Set as current timetable.
- Preview today's prayer timeline.
- Export this mosque profile.
- Disable temporarily.

Do not prefill fake default prayer times. An empty field must remain empty.

---

# PART 4 — MOSQUE SELECTION, GPS, AND GEOFENCING

## 4.1 Exact selection priority

Implement this priority exactly. The app must never randomly pick a mosque when sources conflict.

1. **Active temporary override:** an explicit user-selected mosque/place with a chosen expiry time. This always wins until it expires or the user cancels it.
2. **Persistent manual selection / manual lock:** if the user has selected a mosque manually and has not switched back to Automatic mode, retain that selection. GPS must not override it.
3. **Active saved place boundary:** when the user is in Automatic mode, select a valid saved place whose geofence has been entered and whose dwell/accuracy checks passed.
4. **User-defined priority place:** if multiple eligible priority places overlap, choose the lowest numeric `priorityRank`; if tied, choose the closest place centre; if still tied, choose the stable UUID order to ensure deterministic behavior.
5. **Nearest enabled saved mosque/place:** choose the closest eligible profile only when it is within the user's configured maximum distance. Ties are resolved by user priority, then distance, then stable UUID.
6. **Calculated fallback:** if no saved mosque/place qualifies, calculate local times from current location and current calculation settings.

A disabled or archived profile must never be selected automatically. A missing mosque link on a SavedPlace must not create a fake mosque profile; the UI should ask the user to link one or use calculated times.

## 4.2 Automatic/manual modes

Provide three visible modes:

- **Automatic:** use the priority algorithm and saved areas.
- **Manual:** hold the user-selected mosque until the user changes it or returns to Automatic.
- **Temporary:** hold a selected mosque for a duration such as 1, 2, 4, 8, or custom hours, then return to the previously selected mode.

The dashboard must show the active mode and the selected mosque/place. Do not make automatic mode appear active when the user has locked a manual selection.

## 4.3 GPS quality and hysteresis

Never switch the mosque because of one noisy GPS point.

- Use the best available fused location fix; do not continuously poll GPS.
- Record the fix timestamp and reported accuracy.
- Reject stale location fixes older than the configured age (default 2 minutes for an active foreground decision; tune for background geofence processing).
- Reject a location if reported horizontal accuracy is worse than the configured threshold (default 100 m; editable).
- Require a new boundary to remain valid for a dwell period (default 2 minutes; editable) before switching.
- Use a configurable boundary margin/hysteresis (default 50 m) so a user standing near a boundary does not oscillate between two profiles.
- Do not leave the current area until the location is confidently outside the current boundary plus the exit margin.
- When confidence is low, retain the last valid profile and show “Location uncertain — keeping [mosque]” instead of switching repeatedly.
- Offer a confirmation dialog when two boundaries overlap and their priority is equal or when location confidence is low.
- Store the last accepted location decision's time, accuracy, active profile, and decision reason for on-device diagnostics.
- Do not persist a trail of the user's movement. Keep only the most recent fix and minimal state required for geofence decisions.

## 4.4 Geofence limits and behavior

- Store unlimited saved places locally.
- Android geofencing has a platform limit (currently 100 active geofences per app/user), so do not try to register unlimited boundaries simultaneously.
- Register the most relevant enabled places based on user priority and proximity, and maintain the rest as saved profiles for manual selection/nearest matching.
- Use geofence ENTER/EXIT events and optionally DWELL to reduce boundary flapping.
- A circular region uses latitude, longitude, and radius; allow a user to edit the radius.
- An initial radius of at least 100 m is a reasonable platform-oriented starting point, not a mandatory radius for every location.
- Background geofence monitoring requires background location permission on Android 10+ and must be explained before requesting it.
- Never use a permanently running GPS foreground service merely to detect mosque changes.
- Keep manual location selection fully functional when GPS or background location permission is denied.

## 4.5 Travel mode and outside-area behavior

When the user is outside all saved areas:

- Show “Outside saved places”.
- Offer “Use calculated times for this location”, “Choose a saved mosque”, and “Add this place”.
- In Travel mode, use the current device location only while the user allows it, calculate times using selected settings, and label them **Calculated — Travel mode**.
- If GPS is unavailable, allow manual coordinates or retain the last confirmed place with a visible stale-location warning.
- Never quietly switch to a random nearby mosque.

## 4.6 Manual map pin

- Provide a map/pin workflow when a map component is available.
- Allow coordinate entry without a map.
- Display latitude, longitude, accuracy, radius, and timezone in an editable details section.
- Moving a pin must require Save/Confirm before changing the mosque selection.
- Do not send the pin to a web service unless the user explicitly opens an external map or geocoder.

## 4.7 Reference selection logic — Kotlin

Implement the actual selection in a pure, unit-testable resolver. Adapt model names to the final Room/domain layer, but preserve the ordering and deterministic tie-breakers.

```kotlin
enum class LocationMode { AUTOMATIC, MANUAL, TEMPORARY }

data class CandidatePlace(
    val id: String,
    val mosqueProfileId: String?,
    val distanceMeters: Double,
    val priorityRank: Int,
    val isInsideBoundary: Boolean,
    val isPriorityLocation: Boolean,
    val isEnabled: Boolean,
    val isArchived: Boolean
)

data class SelectionState(
    val mode: LocationMode,
    val temporaryMosqueId: String?,
    val temporaryExpiresAt: java.time.Instant?,
    val manuallySelectedMosqueId: String?,
    val currentAcceptedMosqueId: String?,
    val maximumNearestDistanceMeters: Double
)

sealed interface MosqueSelection {
    data class Selected(val mosqueId: String, val reason: String) : MosqueSelection
    data object NeedsUserChoice : MosqueSelection
    data object UseCalculatedFallback : MosqueSelection
}

fun resolveMosqueSelection(
    state: SelectionState,
    candidates: List<CandidatePlace>,
    now: java.time.Instant
): MosqueSelection {
    val temporaryStillActive = state.temporaryMosqueId != null &&
        state.temporaryExpiresAt?.isAfter(now) == true

    if (temporaryStillActive) {
        return MosqueSelection.Selected(
            state.temporaryMosqueId!!,
            "Temporary user override"
        )
    }

    if (state.mode == LocationMode.MANUAL && state.manuallySelectedMosqueId != null) {
        return MosqueSelection.Selected(
            state.manuallySelectedMosqueId,
            "Manual selection"
        )
    }

    val valid = candidates.filter {
        it.isEnabled && !it.isArchived && it.mosqueProfileId != null
    }

    if (state.mode == LocationMode.AUTOMATIC) {
        val activeBoundary = valid.filter { it.isInsideBoundary }
        if (activeBoundary.isNotEmpty()) {
            val winner = activeBoundary.sortedWith(
                compareBy<CandidatePlace> { it.priorityRank }
                    .thenBy { it.distanceMeters }
                    .thenBy { it.id }
            ).first()
            return MosqueSelection.Selected(
                winner.mosqueProfileId!!,
                "Active saved place boundary"
            )
        }
    }

    val priority = valid.filter { it.isPriorityLocation }
    if (priority.isNotEmpty()) {
        val winner = priority.sortedWith(
            compareBy<CandidatePlace> { it.priorityRank }
                .thenBy { it.distanceMeters }
                .thenBy { it.id }
        ).first()
        return MosqueSelection.Selected(
            winner.mosqueProfileId!!,
            "User priority place"
        )
    }

    val nearest = valid
        .filter { it.distanceMeters <= state.maximumNearestDistanceMeters }
        .sortedWith(
            compareBy<CandidatePlace> { it.priorityRank }
                .thenBy { it.distanceMeters }
                .thenBy { it.id }
        )
        .firstOrNull()

    return nearest?.let {
        MosqueSelection.Selected(it.mosqueProfileId!!, "Nearest saved mosque")
    } ?: MosqueSelection.UseCalculatedFallback
}
```

Note: The temporary selection is an explicit override above the user's normal mode. Expiry must clear it transactionally, restore the prior mode, then run the resolver again. Do not allow an expired override to remain active after process death or device reboot.

---

# PART 5 — PRAYER CALCULATION AND PRAYER-DAY LOGIC

## 5.1 Calculation engine

Use the Adhan Kotlin library or another maintained, well-tested equivalent. Do not create approximate formulas manually.

The referenced Adhan Kotlin Multiplatform library exposes calculation presets, coordinates, date components, Fajr/Isha angles, Isha intervals, Asr madhab/shadow factor, high-latitude rules, per-prayer minute adjustments, and Qibla bearing. Its documented artifact is:

```kotlin
implementation("com.batoulapps.adhan:adhan2:0.0.7")
```

Check compatibility before finalizing dependency resolution. On Android, verify the library's `kotlinx.datetime`/`java.time` requirements and enable core-library desugaring if needed. Do not replace a compatible dependency with an unverified fork.

## 5.2 Calculation settings

Persist these settings per global profile and optionally override them per place:

- Calculation preset: Karachi, Muslim World League, Egyptian, Umm al-Qura, Moonsighting Committee, or Other/manual.
- Fajr angle in degrees.
- Isha angle in degrees, or Isha interval in minutes when the selected method uses an interval.
- Asr factor: Standard/earlier (one shadow-length factor) or Hanafi/later (two shadow-length factor).
- High-latitude rule: Middle of the Night, Seventh of the Night, or Twilight Angle.
- Per-prayer minute adjustment for Fajr, Dhuhr, Asr, Maghrib, and Isha.
- Rounding mode supported by the library.
- Coordinate source and timezone source.

Show a preview before applying a changed calculation profile. A calculation setting must never rewrite stored mosque-entered times.

## 5.3 Suggested Pakistan fallback profile

For first-run convenience, offer the following as an editable **suggested calculation fallback**, not as a religious ruling:

- Preset: Karachi.
- Fajr: library preset value (18° in the documented library).
- Isha: library preset value (18° in the documented library).
- Asr: Standard/earlier option as the suggested default for the user's stated preference; user can change it.
- High latitude: Middle of the Night unless the user chooses a different supported rule.
- Per-prayer adjustment: 0 minutes initially.
- Timezone: Asia/Karachi only when location/timezone confirms Pakistan.

The selected mosque's entered timetable always overrides this calculated fallback.

## 5.4 Prayer time conversion

The calculation library returns instants in UTC. Convert them to the profile's effective `ZoneId` before display and before deriving local wall-clock labels. Do not format a UTC instant as if it were local time.

Use `java.time.Instant`, `ZonedDateTime`, `LocalDate`, `LocalTime`, and `ZoneId` consistently. Do not add a hardcoded `+5 hours` offset. Pakistan uses `Asia/Karachi`, but travel and future timezone-rule changes require a named zone rather than a fixed offset.

## 5.5 Reference prayer calculation code — Kotlin

Adapt imports and exact library types to the resolved Adhan dependency, then compile and test this implementation. The library API names shown below follow its documented Kotlin API.

```kotlin
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.DateComponents
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CalculatedPrayerInstants(
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant
)

data class PrayerCalculationConfig(
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 18.0,
    val ishaIntervalMinutes: Int? = null,
    val useStandardAsr: Boolean = true,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
    val fajrAdjustmentMinutes: Int = 0,
    val dhuhrAdjustmentMinutes: Int = 0,
    val asrAdjustmentMinutes: Int = 0,
    val maghribAdjustmentMinutes: Int = 0,
    val ishaAdjustmentMinutes: Int = 0
)

fun calculatePrayerInstants(
    latitude: Double,
    longitude: Double,
    localDate: LocalDate,
    config: PrayerCalculationConfig
): CalculatedPrayerInstants {
    require(latitude in -90.0..90.0) { "Invalid latitude" }
    require(longitude in -180.0..180.0) { "Invalid longitude" }
    require(config.fajrAngle in 0.0..30.0) { "Invalid Fajr angle" }
    require(config.ishaAngle in 0.0..30.0) { "Invalid Isha angle" }

    val coordinates = Coordinates(latitude, longitude)
    val dateComponents = DateComponents(
        localDate.year,
        localDate.monthValue,
        localDate.dayOfMonth
    )

    val base = CalculationMethod.KARACHI.parameters
    val parameters = base.copy(
        fajrAngle = config.fajrAngle,
        ishaAngle = config.ishaAngle,
        ishaInterval = config.ishaIntervalMinutes,
        madhab = if (config.useStandardAsr) Madhab.SHAFI else Madhab.HANAFI,
        highLatitudeRule = config.highLatitudeRule,
        prayerAdjustments = PrayerAdjustments(
            fajr = config.fajrAdjustmentMinutes,
            dhuhr = config.dhuhrAdjustmentMinutes,
            asr = config.asrAdjustmentMinutes,
            maghrib = config.maghribAdjustmentMinutes,
            isha = config.ishaAdjustmentMinutes
        )
    )

    val result = PrayerTimes(coordinates, dateComponents, parameters)
    fun toJavaInstant(value: kotlinx.datetime.Instant): Instant =
        Instant.ofEpochMilli(value.toEpochMilliseconds())

    return CalculatedPrayerInstants(
        fajr = toJavaInstant(result.fajr),
        sunrise = toJavaInstant(result.sunrise),
        dhuhr = toJavaInstant(result.dhuhr),
        asr = toJavaInstant(result.asr),
        maghrib = toJavaInstant(result.maghrib),
        isha = toJavaInstant(result.isha)
    )
}
```

If the current library version changes a property or type, adapt to the verified API and retain the same inputs/outputs and test coverage. Do not leave a non-compiling sample in the actual project.

## 5.6 Source-aware schedule resolution

Build one domain function that resolves each prayer event from the correct source:

1. Resolve the current MosqueProfile and SavedPlace.
2. Resolve the effective mosque timetable for the target local date using the schedule precedence in Part 3.
3. For each prayer, use mosque Adhan/Iqamah values when present.
4. Use a manual override when the user explicitly configured it and it is more specific than the underlying schedule.
5. Use calculated values only when the relevant mosque value is absent or the user selected Calculated/Travel mode.
6. Return the selected time and its source metadata.

Each event result must include:

- Prayer name.
- Event type: ADHAN, IQAMAH, PRE_ADHAN, PRE_IQAMAH, KHUTBAH, JUMUAH, or CALCULATED_INFORMATION.
- Exact `Instant`.
- Local displayed date/time.
- Mosque/place profile ID.
- Source enum: MOSQUE_TIMETABLE, MANUAL_OVERRIDE, CALCULATED, TRAVEL_CALCULATED.
- Schedule revision ID.
- Whether an alarm should be scheduled.

A missing Iqamah does not mean “Iqamah equals Adhan”. Do not invent an Iqamah time.

## 5.7 Prayer-day and midnight correctness

Do not derive “next prayer” by comparing only `HH:mm` strings. That breaks around midnight.

- Construct actual occurrences as zoned date-time/instant values for the previous, current, and next local dates.
- Sort by `Instant`.
- Choose the first enabled occurrence whose `Instant` is after `now`.
- Isha on one date remains that date's Isha event.
- Fajr after midnight belongs to the upcoming local Gregorian morning's Fajr occurrence, not to the previous night's Isha.
- At 00:30, do not incorrectly label yesterday's Isha as upcoming merely because its time is “later” as a wall-clock string.
- Show the next prayer date when it is not today.
- Use local timezone and DST-safe conversions.
- When the current date changes, rebuild the next day's occurrence set and update the timeline/widget.

Maintain separate concepts:

1. **Civil date:** local Gregorian calendar date used for mosque schedule lookup.
2. **Prayer tracking day:** the group of five prayers the user tracks together. Default anchor: Fajr-to-Isha for the local Gregorian date; the next Fajr starts the next tracking group.
3. **Hijri date:** shown using the selected Hijri calendar convention and a sunset boundary when configured.
4. **Quran tracking day:** independent reset rule selected by the user in Quran settings.

Do not use one date boundary for every feature by accident.

---

# PART 6 — TODAY DASHBOARD AND PRAYER TIMELINE

## 6.1 Dashboard hierarchy

The top of the dashboard should be visually calm. Place only:

- Current date and Hijri date.
- Current saved place or travel label.
- Selected mosque name.
- Mode indicator: Automatic, Manual, Temporary, or Calculated.
- Optional location accuracy status.

The main visual card should show:

- Next prayer name in English and optional Arabic.
- Large countdown.
- Adhan time.
- Iqamah time when available.
- Source label.
- Mosque/place name.
- A clear “View timetable” action.

The lower section should show a five-prayer timeline:

- Fajr, Dhuhr, Asr, Maghrib, Isha.
- Adhan and Iqamah values.
- Completed state only when user marks it.
- Current/upcoming state.
- A subtle vertical or horizontal timeline connector.
- Optional prayer method/mode badge only where useful.

The bottom half should contain thumb-reachable controls:

- Mark as prayed.
- Switch place/mosque.
- Open Qibla.
- Open Quran tracker.
- Bottom navigation.

## 6.2 Timeline behavior

- Completed prayer state comes from PrayerHistory, not from the clock alone.
- The app may suggest that a prayer time has passed, but must not automatically mark the prayer as performed.
- Tapping a prayer opens a detail sheet with Adhan, Iqamah, reminder status, source, mosque, and mark-prayed action.
- If Adhan exists but Iqamah is missing, show “Iqamah not entered”.
- If calculated mode is active, show the calculation method details in a secondary disclosure.
- If manual mode is active, show the selected mosque and the manual-selection status.
- If there is no timetable and no usable location, show a clear setup state, not a fabricated schedule.

## 6.3 Prayer completion interaction

When the user taps **Mark as prayed**:

- Save prayer name, prayer tracking day, actual recorded timestamp, selected mosque/place, source schedule revision, and optional method/context.
- Offer context: Mosque, Congregation/Jama'ah, Alone, Travelling, Other.
- Offer optional notes and tags such as Home, Mosque, Travel, Work.
- Allow the user to edit or undo a record.
- Prevent accidental duplicate marks. If a record already exists, open it for editing rather than adding another.
- Do not infer “at mosque” from GPS.

## 6.4 Dashboard empty/error states

Provide useful states for:

- No mosque added.
- Mosque added but times not entered.
- Current location denied.
- Location accuracy too low.
- Outside saved areas.
- Calculation method not configured.
- Notification permission denied.
- Exact alarm permission unavailable.
- No upcoming prayer occurrence could be built because input data is invalid.

Each state must state what happened and offer a specific recovery action.

---

# PART 7 — ALARMS, ADHAN AUDIO, NOTIFICATIONS, AND RELIABILITY

## 7.1 Reminder types

Support independently configurable reminders for:

- Adhan time.
- Iqamah time.
- 5, 10, 15, 20, 30, or custom minutes before Adhan.
- 5, 10, 15, 20, 30, or custom minutes before Iqamah.
- Optional Friday Khutbah reminder.
- Optional Jumuah reminder.

Allow different preferences per prayer and per mosque. The default must be quiet until the user chooses sound/vibration options.

## 7.2 Sound and vibration

Offer:

- Notification sound.
- Full Adhan audio (user-selected/imported local audio).
- Short reminder tone.
- Silent notification.
- Vibration only.
- Sound + vibration.
- Per-prayer volume preference where Android audio APIs allow it without changing unrelated device volume.
- Test sound button.

Do not bundle a copyrighted Adhan recording. Provide a built-in short original neutral chime only if it is created/licensed for the app. Otherwise use the system notification sound by default.

Optional spoken announcement:

- Use Android TextToSpeech when available.
- Allow language and voice selection exposed by the installed TTS engine.
- Explain that an offline TTS voice may need to be installed by the user.
- Do not depend on network TTS.

If the selected audio file is moved/deleted or cannot be decoded, fall back to the system notification tone and display a non-blocking warning.

## 7.3 Notification channels

Create separate Android notification channels:

- Prayer Adhan.
- Iqamah.
- Pre-prayer reminders.
- Friday/Jumuah.
- App/system reliability alerts.

Allow channel-level sound/vibration behavior and explain that Android's channel settings can override in-app choices. Do not create dozens of channels for individual mosques.

For Android 13+, declare and request `POST_NOTIFICATIONS` at runtime before expecting standard notifications. If denied, keep schedules and prayer calculations working and show an in-app warning.

## 7.4 Notification actions

Where the OS supports them, notifications must include:

- **Mark as Prayed** — records the correct prayer/day and opens an undo/edit path.
- **Snooze** — offers a small set of durations such as 5, 10, or 15 minutes. Snooze must not modify the underlying Adhan/Iqamah time.
- **Open Qibla** — launches the Qibla screen.
- **Open Map** — explicit action to open the selected mosque/place in an external map app if coordinates exist.

Use explicit PendingIntents. Validate that the action refers to a real persisted occurrence, not a stale notification payload.

## 7.5 Exact alarms and honest reliability

Prayer notifications are time-sensitive user-facing reminders. Implement AlarmManager with a permission-aware strategy:

- Prefer an exact alarm only when required for the selected reminder and when Android grants the required access.
- Use `SCHEDULE_EXACT_ALARM` as the permission-aware approach unless a confirmed Google Play policy review shows the app qualifies for the restricted `USE_EXACT_ALARM` permission. Do not add `USE_EXACT_ALARM` casually.
- Before calling `setExact`, `setExactAndAllowWhileIdle`, or `setAlarmClock`, check exact-alarm availability on supported Android versions.
- When permission is denied, do not crash. Use a documented inexact fallback, show an in-app status warning, and explain that Android may delay delivery.
- Never claim guaranteed delivery. OEM battery-management, user-disabled notifications, Doze, permission revocation, and manufacturer restrictions can affect delivery.
- Ask the user to enable exact alarm permission only after explaining why it improves time-critical reminders.

The app must not use a permanently running foreground service just to keep a countdown alive. Use system scheduling, explicit receivers, and durable local data.

## 7.6 One-event-at-a-time alarm coordinator

Avoid scheduling hundreds of stale alarms. Maintain a single next-event coordinator where practical:

1. Calculate the next enabled event from the current persisted schedule.
2. Cancel the previous coordinator PendingIntent using the same explicit identity.
3. Schedule the next eligible occurrence.
4. When the receiver fires, re-read the persisted schedule revision and occurrence.
5. Ignore stale/duplicate events.
6. Post the appropriate notification and schedule the next event.
7. If a pre-alert and main prayer event are close together, preserve both events and process them in chronological order; do not lose one by overwriting another.
8. Keep a bounded horizon of generated occurrences in the database for history/debugging, not as an unbounded list.

If the implementation chooses several concurrently scheduled PendingIntents instead, each must have a stable unique event identity and be completely cancellable/rebuildable. Do not mix the two strategies in a way that produces duplicates.

## 7.7 Mandatory alarm rebuild scenario

This is an acceptance-critical requirement.

**Scenario:** The user changes Fajr Adhan from 5:30 AM to 5:20 AM.

The app must perform the following as one serialized, idempotent schedule update:

1. Validate the new time.
2. Save a new schedule revision.
3. Recompute every affected future Adhan, Iqamah, pre-reminder, and related event.
4. Cancel obsolete alarm PendingIntents belonging to the previous revision.
5. Schedule the new future events.
6. Prevent old and new Fajr notifications from both firing.
7. Update the Today timeline and next-prayer countdown.
8. Update the home-screen widget.
9. Update any persistent/status notification that displays prayer timing.
10. Preserve old prayer-history records and the schedule revision used by historical records.
11. Persist enough alarm state to rebuild after process death or reboot.
12. Show a clear success confirmation only after the database and scheduler operations succeed.

If scheduling fails after saving the new schedule, mark the schedule as saved but reminders as needing repair; show a warning and a Retry action. Do not revert to a fake old schedule or silently report full success.

## 7.8 Rebuild triggers

Rebuild future prayer events when any of these occur:

- Mosque Adhan/Iqamah schedule changes.
- Schedule effective date/season changes.
- User changes the active mosque or place.
- User changes calculation settings while in calculated mode.
- User changes reminder offsets, sound, or vibration settings.
- User changes the timezone or travel mode.
- System date changes.
- System time changes.
- Timezone changes.
- Device restarts.
- App package is replaced/updated.
- Exact alarm permission is granted or revoked.
- Notification permission changes.
- A geofence decision switches the active mosque.
- A temporary location override starts or expires.

Make rebuilding idempotent. Repeated calls with the same schedule revision must not create duplicate reminders.

## 7.9 Boot and system receivers

Implement explicit receivers for:

- `BOOT_COMPLETED`.
- `TIME_SET`.
- `TIMEZONE_CHANGED`.
- `MY_PACKAGE_REPLACED`.
- Exact alarm permission state changed, where applicable.

On receiving a trigger:

- Re-read Room/DataStore state.
- Recompute future local occurrences.
- Rebuild alarms and widget state.
- Do not start a long-running service unnecessarily.
- Do not try to display an Activity directly from a background receiver.
- Use WorkManager only for non-time-critical maintenance/retry operations, not as a replacement for exact prayer alarm scheduling.

Alarms are removed by Android on reboot, so the app must reschedule after boot. The user must have launched the app at least once and enabled the feature for boot restoration to be active.

## 7.10 Duplicate and stale event protection

Every event needs a deterministic key such as:

`{mosqueId}:{scheduleRevision}:{localDate}:{prayer}:{eventType}:{offsetMinutes}`.

Before posting a notification:

- Read the event row or verify its stable occurrence ID.
- Confirm that it is enabled and belongs to the current schedule revision.
- Confirm that its scheduled instant is still the intended instant.
- Check whether it has already been delivered.
- If already delivered, do not show it again.
- If cancelled or stale, ignore it and schedule the current next event.

Use a transaction or serialized coroutine/Mutex around schedule rebuilding and delivered-state updates.

## 7.11 Device-aware reliability screen

Create Settings → Reminder Reliability with live checks:

- Notifications allowed.
- Prayer notification channel enabled.
- Exact alarm access granted or not required.
- Battery optimization status.
- App background activity restrictions where detectable.
- Background location access for automatic geofencing, if enabled.
- Device reboot restoration registered.
- Last successful alarm rebuild timestamp.
- Last schedule revision.
- Current active mosque.

Offer Android system-settings shortcuts where supported. Do not claim to inspect proprietary manufacturer settings that Android does not expose.

Include manufacturer guidance that can be selected by device family, with wording such as:

- “Autostart may be available on some devices.”
- “Allow background activity if your manufacturer provides this setting.”
- “Remove this app from aggressive battery restrictions if you want stronger reminder reliability.”

For Infinix/XOS and other heavily customized Android devices, show a generic, version-dependent checklist and tell the user that menu names can differ by software version. Do not hardcode a guaranteed menu path that may not exist.

## 7.12 Automatic silence during prayer

This feature is OFF by default and requires explicit consent.

- Offer silent, vibrate, or custom sound profile actions.
- Explain the Android Do Not Disturb policy access requirement before opening Settings.
- Check `NotificationManager.isNotificationPolicyAccessGranted` before changing DND state.
- If policy access is absent, do not pretend the phone was silenced. Provide a button to open the correct system settings page.
- Save the device state immediately before the app changes it.
- Restore only the state that the app itself changed.
- If the user manually changes sound/DND while the prayer silence is active, do not overwrite the user's newer choice at restore time.
- Schedule a restoration event and also restore on the next app foreground if an expired session is detected.
- Handle force-stop, reboot, schedule edits, and timezone changes without leaving DND stuck.
- Do not bypass emergency calls, alarms, accessibility needs, or user-selected exceptions.
- Show a persistent, understandable status while app-managed silence is active and provide a manual Restore Now action.

---

# PART 8 — QIBLA COMPASS AND MAP FALLBACK

## 8.1 Qibla calculation

Use the geographic coordinates of the Kaaba:

- Latitude: `21.4224779`.
- Longitude: `39.8251832`.

Use a validated great-circle initial-bearing calculation or the Qibla bearing provided by the Adhan library. Keep the true-north bearing separate from the device's magnetic heading.

## 8.2 Compass sensor behavior

- Prefer the Android rotation-vector sensor when available.
- If it is unavailable, combine accelerometer and magnetometer with Android sensor APIs.
- Convert magnetic heading to true heading using geomagnetic declination when the device/location data supports it.
- Apply sensible smoothing to reduce needle jitter without making the compass feel delayed.
- Do not present a compass needle as accurate when sensor accuracy is low.
- Listen for sensor accuracy changes.
- Detect unavailable sensors and show the bearing/map fallback.
- Provide a calibration prompt only when relevant; do not repeatedly nag the user.
- Show device heading and calculated Qibla bearing in degrees.
- Use clear labels: “Qibla bearing from true north” and “Current device heading”.

## 8.3 Interference and calibration

Show an accuracy label:

- High confidence.
- Moderate confidence.
- Low confidence / calibrate device.
- Sensor unavailable.

If Android reports low accuracy, show a calibration instruction such as moving the device in a gentle figure-eight motion away from magnetic objects. Do not claim that this guarantees perfect accuracy.

The app must not use GPS accuracy as a substitute for compass accuracy; show them separately.

## 8.4 Map/bearing fallback

When sensors are unavailable:

- Show a large numeric Qibla bearing.
- Draw a directional arrow from the current location toward the Kaaba on a simple local schematic/map view.
- If coordinates or a map provider are unavailable, show a clear compass-style directional graphic with the bearing.
- An external map can be opened only by an explicit user action.
- Never require a network map tile to calculate or display the bearing.

## 8.5 Reference Qibla calculation code

```kotlin
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Qibla

fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
    require(latitude in -90.0..90.0)
    require(longitude in -180.0..180.0)
    val coordinates = Coordinates(latitude, longitude)
    return Qibla(coordinates).direction
}
```

Verify whether the returned bearing is degrees clockwise from true north in the resolved library version. Keep that convention explicit in the UI and tests.

---

# PART 9 — HIJRI DATE

## 9.1 Display requirements

Show:

- Hijri day, month name, and year.
- Gregorian date.
- The selected Hijri date source/method in settings.
- A small adjustment indicator when manual adjustment is active.

## 9.2 Calendar options

Offer:

- Umm al-Qura calendar where supported.
- A selectable calculated Hijri calendar source where available.
- Manual day adjustment, default 0, configurable from -2 to +2 days (expand only if a real user requirement exists).
- Manual regional/local moon-sighting override, where the user can set the displayed date.

Do not claim that an algorithmic date is the official date announced by a local moon-sighting authority. Explain that Hijri dates can differ by region and observation.

## 9.3 Sunset boundary

The Hijri calendar convention begins the Islamic date at sunset. Support a display setting that increments the displayed Hijri date after the local Maghrib/sunset boundary. Use the resolved Maghrib occurrence for the selected place when available; otherwise use a clearly labelled calculated sunset/Maghrib boundary.

Keep this Hijri display boundary separate from the prayer-history day and Quran reading reset rules.

## 9.4 Manual adjustment indicator

If the user applies `+1`, `-1`, or any manual date override:

- Show a small visible “Adjusted” label near the Hijri date.
- Provide a details sheet displaying source, original date, adjustment, and final displayed date.
- Never silently drop the adjustment after an app update or backup restore.

---

# PART 10 — QURAN RUKU READING AND BACKLOG SYSTEM

## 10.1 Purpose

The user normally reads one Ruku per day. If a daily target is missed, the unpaid amount becomes backlog and the next day's target increases. The app must calculate this reliably and allow corrections for forgotten entries.

Do not assume the user has read anything. All reading progress is entered by the user.

## 10.2 Default settings

- Daily goal: 1 Ruku.
- Surplus credit: OFF by default; user can enable it.
- Quran day reset: local midnight by default.
- Alternative reset choices: a user-selected time, Fajr, or Maghrib. Show the active rule clearly.
- Reading entry mode: quick count by default; optional Surah/Ruku detail.
- Missed day: treated as 0 completed only after the Quran day has closed.
- Goal changes: effective from the next Quran tracking day by default; do not retroactively rewrite earlier obligations unless the user explicitly chooses to recalculate historical periods.

## 10.3 Backlog rule — exact formula

Track a signed balance:

- Positive balance = Rukus owed (backlog).
- Zero balance = no backlog and no stored surplus.
- Negative balance = surplus credit available when the user enables surplus credit.

For each completed Quran tracking day:

`closingBalance = openingBalance + dailyGoal - rukusCompletedToday`

If surplus credit is OFF:

`closingBalance = max(0, closingBalance)`

If surplus credit is ON:

- Keep the signed balance.
- Backlog shown = `max(0, closingBalance)`.
- Surplus credit shown = `max(0, -closingBalance)`.
- Next day's target = `max(0, dailyGoal + openingBalance)`.

This gives the requested examples:

| Day | Opening backlog | Daily goal | Read today | Required target | Closing backlog |
|---|---:|---:|---:|---:|---:|
| Day 1 | 0 | 1 | 1 | 1 | 0 |
| Day 2 | 0 | 1 | 0 | 1 | 1 |
| Day 3 | 1 | 1 | 0 | 2 | 2 |
| Day 4 | 2 | 1 | 0 | 3 | 3 |

If a target is 3 and the user reads 5:

- With surplus credit enabled, the net balance becomes 2 Rukus of credit; that credit can reduce future daily targets until consumed.
- With surplus credit disabled, backlog is cleared at zero and the extra two Rukus are recorded in the reading history but do not create future credit.

Show the user this behavior in a short explanation before enabling surplus credit.

## 10.4 Reading-day boundary

The app must make the rule visible, not hidden.

Default:

- The Quran tracking day begins at 00:00 local time.
- A reading entered at 23:59 counts toward the current day.
- A reading entered at or after 00:00 counts toward the next day.

When the user chooses a custom reset time:

- If the reset is 04:00, a reading at 02:00 belongs to the previous Quran day, and a reading at 04:00 belongs to the new Quran day.
- For a Fajr reset, the boundary follows the selected mosque's Fajr time, or calculated Fajr when no mosque time exists. If the boundary changes due to a schedule change, do not silently move historical reading entries; store the effective reset instant for each date.
- For a Maghrib reset, use selected mosque Maghrib or clearly labelled calculated Maghrib.

The active screen must state for example:

> “Reading day resets at 12:00 AM (local time). Entries before midnight count for today.”

## 10.5 Daily target, progress, and backlog display

Show:

- Today's target.
- Rukus completed today.
- Remaining Rukus for today.
- Current backlog.
- Optional surplus credit.
- Current streak.
- Total Rukus completed.
- Overall completion percentage when a verified total/edition is selected.
- Recent reading dates.

Use a simple progress ring/bar and a clear numerical target. Do not use guilt-inducing red warnings or shame language.

## 10.6 Reading entry interactions

Allow:

- Quick add `+1 Ruku`.
- Quick add `+2`, `+3`, or custom count.
- Undo last entry.
- Edit today's total.
- Add a reading session with date/time and Ruku count.
- Optional Surah and Ruku number selection.
- Optional note.
- Manual marking for a date that was forgotten.

If the user records Surah/Ruku detail, validate it against the selected Quran edition's verified metadata. Do not invent Surah/Ruku boundaries.

The app must not require the user to enter the exact Ruku if they prefer a quick count.

## 10.7 Missed-day correction

Provide a calendar/history editor.

When the user changes a past day:

1. Validate the edited count.
2. Save the correction with an edit timestamp and optional reason.
3. Recalculate the ledger from the earliest changed date through today.
4. Recompute each day's opening balance, target, completion, closing balance, backlog, and surplus.
5. Refresh streak and progress statistics.
6. Keep an audit trail of the correction without retaining unnecessary private information.
7. Never double-count the original and corrected entries.

## 10.8 Streak definition

Define the streak clearly:

- A day is complete when the total user-entered reading meets or exceeds that day's computed target by the reset boundary.
- When surplus credit covers the entire target, show “Covered by surplus” and let the user choose whether such a day counts toward streaks. Default: count it as target-satisfied but show that no new Ruku was recorded that day.
- A missed day resets the streak.
- Editing a past day recalculates affected streaks.
- Do not use the phone's current wall-clock date without applying the configured Quran day boundary.

## 10.9 Total Ruku count / Quran edition

- Do not assume every Quran print edition uses identical Ruku metadata.
- Provide a configurable Quran edition/profile.
- Only show overall percentage if a verified local Ruku metadata dataset is bundled or the user specifies a total.
- A common South Asian Ruku division may be offered only with a clear edition label and verified metadata. Do not fabricate Surah/Ruku indices.
- If no verified total is available, show “Rukus completed” and daily/backlog statistics without a percentage.

## 10.10 Reference Quran ledger code — Kotlin

Use this as the core pure business rule and add unit tests. Persist daily reading entries separately and recalculate this ledger when entries or settings change.

```kotlin
data class QuranDayInput(
    val dateKey: java.time.LocalDate,
    val dailyGoal: Int,
    val completedRukus: Int
)

data class QuranDayResult(
    val dateKey: java.time.LocalDate,
    val openingBalance: Int,
    val requiredTarget: Int,
    val completedRukus: Int,
    val remainingForToday: Int,
    val closingBalance: Int,
    val backlogAfterClose: Int,
    val surplusCreditAfterClose: Int
)

fun calculateQuranLedger(
    days: List<QuranDayInput>,
    initialBalance: Int = 0,
    surplusCreditEnabled: Boolean
): List<QuranDayResult> {
    require(initialBalance >= 0 || surplusCreditEnabled) {
        "Negative initial balance requires surplus credit to be enabled"
    }

    var balance = initialBalance
    return days.sortedBy { it.dateKey }.map { day ->
        require(day.dailyGoal >= 0) { "Daily goal cannot be negative" }
        require(day.completedRukus >= 0) { "Completed Rukus cannot be negative" }

        val opening = balance
        val target = (day.dailyGoal + opening).coerceAtLeast(0)
        val remaining = (target - day.completedRukus).coerceAtLeast(0)

        balance = opening + day.dailyGoal - day.completedRukus
        if (!surplusCreditEnabled && balance < 0) balance = 0

        QuranDayResult(
            dateKey = day.dateKey,
            openingBalance = opening,
            requiredTarget = target,
            completedRukus = day.completedRukus,
            remainingForToday = remaining,
            closingBalance = balance,
            backlogAfterClose = balance.coerceAtLeast(0),
            surplusCreditAfterClose = (-balance).coerceAtLeast(0)
        )
    }
}
```

Implementation notes:

- For a closed missed day, use `completedRukus = 0`.
- Do not close the active day prematurely. The current day's target uses the opening balance; closing balance is finalized when the configured reset boundary passes.
- When building days from a first tracking date, fill missing closed dates with zero readings so the backlog accrues one missed daily goal per day.
- Do not add a duplicate day row when the app is opened more than once.
- Apply goal changes using an effective-from date.
- If a past entry is edited, recalculate later results from the edited day onward.

---

# PART 11 — PRAYER HISTORY AND QADA TRACKING

## 11.1 Prayer history

Store per-prayer records with:

- Prayer name.
- Tracking date.
- Scheduled Adhan and Iqamah instants used at the time.
- Actual user-marked completion timestamp.
- Mosque/place profile ID snapshot.
- Schedule revision snapshot.
- Completion context: Mosque, Congregation, Alone, Travelling, Other.
- Optional tags and note.
- Created/updated timestamps.

The UI should show a day/week/month history view with edit and undo actions.

## 11.2 Qada / make-up prayers

Qada tracking is optional and must be disabled until the user enables it.

- Keep current-day prayer completion separate from Qada records.
- Allow the user to add a make-up prayer record.
- Allow the user to track a backlog of missed prayers only if they intentionally choose that workflow.
- Do not automatically infer a religious obligation or determine how many prayers the user owes.
- Let the user enter a count or add individual prayer records.
- Provide clear edit/correction controls.
- Do not pressure the user or make judgments.

## 11.3 Day reset setting for prayer history

Default prayer tracking day is the local date's five-prayer group from Fajr through Isha. The next Fajr begins the next group. The user may choose a custom history reset time, but the app must explain how it affects history grouping.

Store the effective reset instant with a historical record so later settings changes do not silently move old records to another day.

---

# PART 12 — HOME-SCREEN WIDGETS

Provide Android home-screen widgets using Jetpack Glance where supported:

1. **Next Prayer widget:** selected mosque/place, next prayer name, Adhan time, Iqamah time when available, and a countdown label.
2. **Compact widget:** next prayer name and time.
3. **Qibla shortcut widget:** tap to open Qibla screen; show last calculated bearing only if location data exists and label it with its update status.
4. **Hijri widget:** Hijri date and adjustment marker.
5. **Combined widget:** next prayer, countdown label, Hijri date, and Qibla shortcut.

Widget requirements:

- Respect light/dark mode.
- Use a readable, compact layout and resize gracefully.
- Tapping the prayer area opens the dashboard; tapping the Qibla area opens Qibla.
- Update widgets after a mosque/time/location/timezone/settings change, alarm delivery, day change, Hijri adjustment, and boot restoration.
- Do not pretend a widget can update its visible countdown every second. Use supported periodic updates and event-triggered updates. Show a useful countdown label that may be refreshed periodically.
- If a widget has stale data, show a stale/last-updated indication instead of fabricated live data.
- A missing selected mosque should produce a setup prompt, not fake times.

---

# PART 13 — UI / UX DESIGN SYSTEM

## 13.1 Visual direction

Create a premium, ultra-minimalist, thumb-friendly interface inspired by the visual qualities of modern frosted-glass mobile interfaces, including the user's requested “iOS 27-style” direction. This is a visual reference, not a request to copy proprietary system artwork or claim the app is iOS.

Core visual qualities:

- Translucent frosted surfaces.
- Light rim borders.
- Controlled backdrop blur.
- Soft, atmospheric backgrounds.
- Clear typography and strong contrast.
- Calm transitions and subtle motion.
- Minimal visual clutter.
- One-handed interaction.

Use native Android interaction conventions, system back behavior, permission sheets, time/date pickers, accessibility semantics, and Material 3 behavior where appropriate.

## 13.2 Light theme

- Background: warm/soft off-white, such as `#F4F5F7` with a subtle tonal gradient.
- Primary frosted card: semi-transparent white with approximately 0.62–0.78 opacity depending on contrast needs.
- Rim: subtle white border and a very light neutral edge.
- Text: deep charcoal / near-black.
- Secondary text: medium neutral grey with adequate contrast.
- Accent: restrained deep teal, emerald, or the selected brand accent.
- Avoid neon gradients and excessive saturated colors.

## 13.3 Dark theme

- Background: deep black / near-black such as `#050607` or `#0A0A0C`.
- Frosted card: translucent black/dark grey surface.
- Rim: subtle light border with low opacity.
- Text: off-white.
- Secondary text: readable muted grey, not too dim.
- Accent: same hue family as light theme with contrast-adjusted brightness.
- Do not use pure black text on dark cards or grey-on-grey illegible labels.

## 13.4 Frosted glass implementation

Create a reusable `FrostedCard` composable:

- Rounded corners approximately 22–28 dp.
- Semi-transparent surface color.
- Thin border.
- Soft elevation/shadow.
- Backdrop blur where supported by the Android version and Compose APIs used.
- Fallback to a translucent surface without blur on devices where blur is unsupported or too expensive.
- Avoid applying heavy blur to every pixel of the screen; use it on important surfaces only.
- Ensure text contrast remains high over every background.
- Respect Reduce Motion and low-power/device performance considerations.

Do not put essential text over a busy blurred background without a sufficiently opaque surface layer.

## 13.5 One-handed layout

- Place bottom navigation within comfortable thumb reach.
- Put primary actions in the lower half of the screen.
- Put large titles and non-interactive status in the top area.
- Use bottom sheets for frequent selection/edit actions when practical.
- Keep destructive actions away from primary tap zones.
- Do not require the user to reach the top-right corner for common actions.
- Support large font/display scaling without clipped prayer times.
- Use minimum touch targets of 48 dp or the current Android accessibility recommendation.

## 13.6 Typography and spacing

Suggested visual tokens:

- Screen horizontal padding: 20 dp on normal phones, adaptive on narrow phones.
- Card spacing: 12–16 dp.
- Card internal padding: 16–20 dp.
- Small radius: 12–16 dp.
- Main card radius: 24–28 dp.
- Page title: 28–34 sp depending on width.
- Next prayer name: 24–30 sp.
- Countdown: 34–44 sp, tabular numerals.
- Prayer row name: 15–17 sp.
- Time labels: 14–16 sp.
- Secondary labels: 12–14 sp, subject to contrast/accessibility.

Use system typography or properly licensed fonts. Support Arabic shaping and right-to-left layout.

## 13.7 Motion

- Gentle fade/scale for changing next prayer card.
- Smooth countdown text updates without layout jumping.
- Short navigation transitions.
- Subtle haptic feedback for completed prayer or Ruku entry where enabled.
- Avoid constant, distracting particle effects.
- Respect Android animator duration scale and accessibility settings.

## 13.8 Bottom navigation

Five main destinations:

- Today.
- Places.
- Qibla.
- Quran.
- More.

Use clear icons plus labels. The active state should be obvious in both themes. Do not use icons alone without an accessible label.

---

# PART 14 — SCREEN-BY-SCREEN REQUIREMENTS

## 14.1 Today screen

- Active place/mosque and mode.
- Hijri and Gregorian date.
- Next prayer card and countdown.
- Adhan/Iqamah times.
- Source indicator.
- Five-prayer timeline.
- Mark-as-prayed action.
- Quick Qibla shortcut.
- Quick Quran target shortcut.
- Notification/permission warning if reminders are not fully configured.

## 14.2 Places screen

- Saved places list.
- Search/filter.
- Current location marker only when permission is granted.
- Add place.
- Edit place.
- Disable/enable place.
- Priority order management.
- Place boundary radius.
- Linked mosque profile.
- Current mode and active selection.
- Travel mode.
- Temporary override.

## 14.3 Mosque profiles screen

- Mosque profile list.
- Add/edit/delete with confirmation.
- Disable temporarily.
- Assign to a SavedPlace.
- Edit weekday/date-range/fixed schedule.
- Enter Adhan and Iqamah separately.
- Friday Khutbah/Jumuah fields.
- Reminder profile.
- Export individual profile.
- Duplicate schedule for a season/date range.

## 14.4 Qibla screen

- Large compass/arrow.
- Bearing from true north.
- Current device heading.
- Sensor accuracy/calibration status.
- Current location and location accuracy.
- Manual coordinate fallback.
- Map/bearing fallback.
- Open external map action where coordinates exist.

## 14.5 Quran screen

- Today's target.
- Completed Rukus.
- Remaining Rukus.
- Backlog.
- Surplus credit (when enabled).
- Streak.
- Total progress where verified metadata exists.
- Quick add controls.
- Reading history.
- Reset-time rule displayed.
- Goal and backlog settings.
- Missed-day correction.

## 14.6 History screen

- Prayer history by day/week/month.
- Completion context and notes.
- Edit/undo entries.
- Optional Qada tab.
- No fabricated completion states.

## 14.7 Settings screen

Sections:

- Prayer calculation.
- Mosque selection/automatic location.
- Notifications and Adhan audio.
- Iqamah reminders.
- Phone silence/DND.
- Qibla calibration.
- Hijri calendar and adjustment.
- Prayer history reset.
- Quran goal/reset/backlog.
- Widgets.
- Backup and restore.
- Privacy and app data.
- Reminder reliability checklist.
- About/version.

## 14.8 Backup and restore

- Export all data to a versioned JSON backup (or an encrypted archive containing JSON and optional audio references).
- Export one mosque profile separately.
- Import from a user-selected file through Storage Access Framework.
- Validate schema version and all IDs before merging.
- Show a preview of imported places, mosques, prayer schedules, preferences, and history counts.
- Offer Merge or Replace with clear consequences.
- Do not delete existing data until a replace import has been validated and the user confirms.
- Make a safety backup before Replace.
- Preserve UUIDs unless the imported ID conflicts; resolve conflicts with a deterministic, user-visible rule.
- Handle malformed, partial, unsupported-version, and corrupted files gracefully.
- Never execute code contained in a backup file.
- Do not automatically import audio binaries from untrusted archives; let the user select audio separately or validate file types and size.

---

# PART 15 — APP ICON, LAUNCHER ART, AND ANDROID RESOURCES

Android applications do not use a browser favicon as their primary icon. Create the correct **Android launcher icon**, adaptive icon, monochrome icon for themed icons, notification small icon, and splash-screen brand asset. If an optional web companion is ever created, then create a favicon separately; it is not needed for this native Android app.

## 15.1 Icon concept

Create a restrained, recognizable mark:

- Minimal mosque arch/dome silhouette.
- A small crescent or directional Qibla motif.
- Clean geometry that remains identifiable at small sizes.
- No tiny text.
- Strong silhouette on both light and dark launchers.
- Use the same accent palette as the app.

Do not copy an existing commercial prayer app icon.

## 15.2 Required icon resources

Generate:

- `mipmap-anydpi-v26/ic_launcher.xml` adaptive icon.
- `mipmap-anydpi-v26/ic_launcher_round.xml` if needed by the project.
- Foreground vector drawable.
- Background color/drawable.
- Monochrome adaptive icon layer for themed icons where supported.
- `drawable/ic_stat_prayer.xml` as a simple monochrome small notification icon.
- Splash screen icon using the same brand mark.
- Compose vector icons for internal navigation/actions where appropriate.

Example adaptive icon resource (adapt to the actual generated resource names):

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

Example background color resource:

```xml
<resources>
    <color name="launcher_background">#0D6B62</color>
</resources>
```

Create real vector paths for the foreground and monochrome resources. Do not leave missing drawable references or an emoji as the launcher icon.

---

# PART 16 — REFERENCE ANDROID MANIFEST CONTRACT

Use the minimum permissions required for the features actually implemented. The following is a reference contract, not a reason to request every permission automatically.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Notifications on Android 13+ -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Alarm restoration after reboot -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- User-facing exact prayer reminders; request special access at runtime/settings -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <!-- Foreground location for user-initiated location features -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <!-- Include/request only when automatic background geofence monitoring is implemented and enabled -->
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Vibration for prayer reminders -->
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".NamazTimesApplication"
        android:allowBackup="false"
        android:label="Namaz Times"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".prayer.PrayerAlarmReceiver"
            android:enabled="true"
            android:exported="false" />

        <receiver
            android:name=".receivers.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".location.GeofenceBroadcastReceiver"
            android:enabled="true"
            android:exported="false" />

        <!-- Add the real Glance receiver/provider declaration generated by the selected widget API. -->
    </application>
</manifest>
```

Manifest instructions:

- Verify receiver requirements and exported rules against the resolved Android API levels.
- Do not add `ACCESS_BACKGROUND_LOCATION` to the user-facing flow unless automatic geofence monitoring is enabled and justified.
- On Android 11+, background location permission requires a separate settings flow; it is not granted by the foreground permission dialog.
- Do not add `USE_EXACT_ALARM` unless the app has been checked against the current Google Play restricted-permission policy and truly qualifies.
- Do not add full-screen intent permission. Prayer reminders do not need to launch an intrusive full-screen activity.
- Do not add accessibility service permissions, call-log, contacts, microphone, camera, SMS, or unrelated permissions.
- If optional location-based features can work with approximate location, allow approximate location where sufficient. Explain when precise location is needed.

---

# PART 17 — REFERENCE ALARM SCHEDULER CONTRACT

The final app must implement a real alarm scheduler with explicit PendingIntent identity, permission checks, exception handling, stale-event validation, and an idempotent rebuild path.

```kotlin
class PrayerAlarmScheduler(
    private val context: android.content.Context,
    private val alarmManager: android.app.AlarmManager
) {
    fun canUseExactAlarms(): Boolean {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
    }

    fun schedule(
        eventId: String,
        triggerAtMillis: Long,
        allowWhileIdle: Boolean = true
    ): Result<Unit> {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return Result.failure(IllegalArgumentException("Alarm time must be in the future"))
        }

        val intent = android.content.Intent(
            context,
            PrayerAlarmReceiver::class.java
        ).apply {
            action = "com.namaztimes.app.PRAYER_ALARM"
            putExtra("event_id", eventId)
        }

        val requestCode = eventId.hashCode()
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canUseExactAlarms()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && allowWhileIdle) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Result.success(Unit)
        } catch (security: SecurityException) {
            Result.failure(security)
        } catch (error: RuntimeException) {
            Result.failure(error)
        }
    }

    fun cancel(eventId: String) {
        val intent = android.content.Intent(
            context,
            PrayerAlarmReceiver::class.java
        ).apply {
            action = "com.namaztimes.app.PRAYER_ALARM"
            putExtra("event_id", eventId)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_NO_CREATE or
                android.app.PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
```

Important implementation notes:

- The sample is an API contract, not permission to omit the database coordinator.
- PendingIntent identity must include a collision-safe stable identifier. Do not rely on `String.hashCode()` alone for correctness; store a generated integer request code mapped to the stable event ID or use a unique data URI/action identity.
- Do not schedule an alarm with an old schedule revision.
- Verify exact-alarm permission immediately before scheduling; Android can revoke it and cancel previously scheduled exact alarms.
- If `setExactAndAllowWhileIdle` is throttled or unavailable, handle the platform result and do not claim perfect delivery.
- Persist the alarm event ID and next trigger in Room.
- After one event fires, schedule the next eligible event.
- Receivers must use `goAsync()` only when necessary and finish promptly. Do not perform long work on the main thread.

---

# PART 18 — REFERENCE NOTIFICATION CONTRACT

Create notification channels in `Application.onCreate()` and request Android 13+ notification permission only when the user enables reminders.

Notification content example:

- Title: `Fajr — Adhan`
- Body: `5:20 AM · Defence Mosque · Mosque timetable`
- Actions: Mark as Prayed, Snooze, Open Qibla

Iqamah example:

- Title: `Fajr — Iqamah`
- Body: `5:40 AM · Defence Mosque`

Pre-reminder example:

- Title: `Fajr in 10 minutes`
- Body: `Adhan at 5:20 AM · Defence Mosque`

Do not hardcode these sample times in the actual application. They illustrate notification formatting only.

The receiver must resolve the event from Room before creating the notification. It must not trust only the text/extras in an old PendingIntent.

---

# PART 19 — ROOM DATABASE CONTRACT

Implement normalized Room entities/relations for at least:

- SavedPlaceEntity.
- MosqueProfileEntity.
- MosqueScheduleEntity.
- PrayerScheduleEntryEntity or equivalent date/prayer/event-specific schedule rows.
- ReminderPreferenceEntity.
- PrayerOccurrenceEntity.
- ScheduledAlarmEntity.
- PrayerHistoryEntity.
- QuranSettingsEntity.
- QuranReadingSessionEntity.
- QuranDailyLedgerEntity or recalculable projection.
- HijriSettingsEntity.
- AppSettingsEntity.
- BackupMetadataEntity if needed.

Database requirements:

- Use stable UUIDs.
- Add foreign keys where appropriate.
- Add indexes for profile IDs, local dates, occurrence IDs, and active schedule selection.
- Use transactions for schedule revisions, imports, and historical corrections.
- Provide explicit migrations from each released schema version.
- Never call destructive migration in production.
- Use database flows to update Compose UI.
- Keep the calculation engine pure and independent of Room.
- Store instants and timezone identifiers in unambiguous forms; do not store only formatted strings for real event time.
- Store original user-entered wall-clock time for recurring schedules.
- Store source/revision snapshots in prayer occurrences and prayer-history records.

---

# PART 20 — ERROR HANDLING AND RECOVERY

Implement clear handling for these scenarios:

## 20.1 Location errors

- Permission denied.
- Approximate location only.
- Precise location unavailable.
- Location services disabled.
- GPS fix stale.
- Accuracy outside threshold.
- Geofence permission denied.
- Geofence registration limit reached.
- Multiple overlapping places.
- Place has no coordinates.

Recovery: keep manual selection available and show which location source is being used.

## 20.2 Prayer schedule errors

- Missing Adhan.
- Missing Iqamah.
- Invalid wall-clock time.
- Overlapping/conflicting timetable revisions.
- Timezone mismatch.
- Invalid coordinates for calculation.
- Library calculation failure.
- A calculated event falls outside a valid date/time range.

Recovery: show the missing field/source; do not create fake values.

## 20.3 Alarm/notification errors

- Notification permission denied.
- Channel blocked by user.
- Exact alarm access denied/revoked.
- OEM battery restriction.
- Reboot before first app launch.
- Alarm receiver gets stale event.
- Alarm fires twice.
- Device time/timezone changed.
- App update interrupted a schedule rebuild.

Recovery: use reliable state, idempotent rebuild, inexact fallback where appropriate, and a setup checklist.

## 20.4 Qibla errors

- No compass sensor.
- Low sensor accuracy.
- Magnetic interference.
- No location.
- Manual coordinates invalid.

Recovery: show numeric bearing and map/directional fallback; never present a false “accurate” state.

## 20.5 Quran errors

- Negative Ruku entry.
- Duplicate reading session.
- Editing a closed day.
- Change in daily goal.
- Change in reset time.
- Missing historical days.
- Unknown total Ruku metadata.

Recovery: validate inputs, recalculate deterministically, preserve the audit trail, and explain how the target changed.

## 20.6 Backup errors

- Unsupported schema.
- Invalid JSON.
- Corrupt or incomplete file.
- Existing ID conflicts.
- Insufficient storage.
- User cancels the picker.
- Encrypted backup passphrase incorrect.

Recovery: do not partially overwrite live data. Show an import preview and a rollback/safety-backup option.

---

# PART 21 — TEST PLAN AND ACCEPTANCE CRITERIA

Write unit tests for pure business logic, database integration tests where feasible, and instrumentation/UI tests for important flows.

## 21.1 Mosque selection tests

- Temporary override wins over everything until expiry.
- Expired temporary override no longer wins after process restart.
- Manual mode wins over geofence.
- Manual mode remains selected after noisy GPS movement.
- Automatic mode chooses an eligible active boundary.
- Overlapping active boundaries use priority rank, then distance, then stable ID.
- Disabled profiles are excluded.
- Priority locations beat nearest-mosque fallback.
- Nearest mosque is used only inside configured maximum distance.
- No eligible saved place uses calculated fallback.

## 21.2 Prayer calculation tests

- Karachi preset values are loaded from the library correctly.
- Standard and Hanafi Asr options produce the expected ordering for validated coordinates/dates.
- Per-prayer adjustments are applied.
- Fajr/Isha angle changes update only calculated schedules.
- Mosque-entered times are not overwritten by calculation changes.
- Conversion to Asia/Karachi displays the expected local wall-clock time for fixed test instants.
- Different timezones are tested.

## 21.3 Midnight/date tests

- At 23:50 before Isha, next prayer is Isha.
- At 00:30 after Isha, next prayer is the next Fajr.
- Fajr occurrence belongs to its correct local Gregorian date.
- Isha is not duplicated after midnight.
- Timezone changes rebuild future events without firing old-zone alarms.
- DST transitions do not create invalid or duplicated local occurrences in zones that use DST.

## 21.4 Mandatory Fajr edit test

Given a scheduled Fajr Adhan at 05:30:

1. Change it to 05:20.
2. Verify old event ID is cancelled/stale.
3. Verify new event instant is scheduled.
4. Verify no duplicate 05:30 reminder remains.
5. Verify widget and dashboard show 05:20.
6. Verify prayer history is unchanged.
7. Re-run rebuild twice and verify no duplicate events.

## 21.5 Alarm reliability tests

- Notification permission granted/denied.
- Exact alarm access granted/denied/revoked.
- Device reboot causes future events to be rebuilt.
- Time/timezone change causes rebuild.
- App update causes rebuild.
- Duplicate receiver invocation does not duplicate notification.
- Stale schedule revision is ignored.
- Inexact fallback is visibly labelled as less precise.

## 21.6 Quran backlog tests

Minimum required test cases:

- Day 1 goal 1, read 1 → backlog 0.
- Day 2 goal 1, read 0 → backlog 1.
- Day 3 goal 1, read 0 → target 2, backlog 2.
- Day 4 goal 1, read 0 → target 3, backlog 3.
- Backlog 2, goal 1, read 3 → backlog 0.
- Backlog 2, goal 1, read 5, surplus enabled → surplus credit 2.
- Backlog 2, goal 1, read 5, surplus disabled → backlog 0, no credit.
- A missed day accrues only once even if app opens repeatedly.
- A past day's count correction recalculates all later balances.
- Reset at 04:00 assigns 02:00 reading to the previous Quran day.
- Goal change effective tomorrow does not retroactively change previous ledger rows.

## 21.7 Qibla tests

- Known test coordinates return a stable bearing.
- Invalid coordinates are rejected.
- Low sensor accuracy produces a warning.
- Sensor-unavailable state shows a bearing fallback.
- True/magnetic heading conversion is applied consistently.

## 21.8 UI tests

- Light and dark themes.
- Narrow phone width.
- Large display/text scale.
- Arabic text and RTL layout.
- Empty mosque list.
- Missing prayer times.
- Denied notification/location permissions.
- Bottom navigation and all primary buttons work.
- No clipped countdown or overlapping bottom navigation.

---

# PART 22 — REQUIRED IMPLEMENTATION PASSES

Build in controlled passes while preserving prior work:

### Pass 1 — Foundation

- Native Android project, Compose, theme, navigation, Room, DataStore.
- Icon, splash, app shell, bottom navigation.
- Local-only/privacy baseline.

### Pass 2 — Mosque and schedule data

- SavedPlace and MosqueProfile CRUD.
- Timetable editor.
- Fixed/weekly/date-range/date-specific schedules.
- Friday Khutbah/Jumuah fields.
- Import/export one mosque profile.

### Pass 3 — Prayer engine and dashboard

- Adhan calculation integration.
- Source-aware mosque-vs-calculated resolver.
- Next prayer across midnight.
- Today dashboard and prayer timeline.

### Pass 4 — Reminders

- Notification channels and permission flow.
- Alarm scheduler and receiver.
- Event identity, duplicate protection, schedule rebuild.
- Boot/time/timezone/package-replaced receivers.
- Reliability checklist.

### Pass 5 — Location automation

- Foreground location.
- Saved-place geofences.
- Location confidence/hysteresis.
- Automatic/manual/temporary modes.
- Background permission education.

### Pass 6 — Qibla and Hijri

- Qibla bearing and sensor compass.
- Calibration/accuracy indicators.
- Numeric/map fallback.
- Hijri source, manual adjustment, sunset boundary.

### Pass 7 — Quran and history

- Ruku reading entries.
- Backlog ledger and surplus rules.
- Reset boundary.
- Past-day correction.
- Prayer history and optional Qada tracking.

### Pass 8 — Widgets, backup, polish

- Glance widgets.
- Full backup/restore.
- Manufacturer reliability help.
- Accessibility and visual polish.
- Regression tests and final build validation.

Do not skip tests because a later pass is visually more interesting.

---

# PART 23 — FINAL ACCEPTANCE CHECKLIST

Do not consider Namaz Times complete until all applicable items below are true.

## Prayer and mosque

- [ ] User can add unlimited SavedPlace records locally.
- [ ] User can add and edit MosqueProfile records.
- [ ] Each mosque stores Adhan and Iqamah separately for all five prayers.
- [ ] Missing times remain missing; no fake times are generated.
- [ ] Friday Khutbah and Jumuah are separate fields.
- [ ] Seasonal, weekly, fixed, and date-specific timetable selection is deterministic.
- [ ] Mosque-entered times override calculation fallback.
- [ ] Standard and Hanafi Asr settings are available and editable.
- [ ] Calculation method and adjustments are visible.
- [ ] Travel mode is available.

## Selection and location

- [ ] Manual selection, automatic boundary, priority place, nearest mosque, and calculated fallback resolve in the exact documented order.
- [ ] Temporary override expires correctly.
- [ ] Disabled mosque/place is not selected.
- [ ] Hysteresis and accuracy threshold prevent boundary flapping.
- [ ] Permission denial does not block manual use.
- [ ] No continuous GPS service is required.

## Reminders

- [ ] Adhan and Iqamah reminders are independent.
- [ ] Pre-reminder offsets work.
- [ ] Notification actions work.
- [ ] Fajr 05:30 → 05:20 edit cancels old event and updates dashboard/widget/notifications.
- [ ] Rebuilding twice does not duplicate alarms.
- [ ] Device reboot, time change, timezone change, app update, and permission change rebuild future reminders.
- [ ] Exact-alarm and notification permissions are checked and explained.
- [ ] Automatic silence is opt-in and restores the prior state safely.

## Qibla and Hijri

- [ ] Qibla bearing is calculated from current/manual coordinates.
- [ ] Sensor accuracy is visible.
- [ ] Sensor failure has a numeric/map fallback.
- [ ] Hijri date source and adjustment are visible.
- [ ] Sunset boundary is independent from prayer/Quran tracking boundaries.

## Quran and history

- [ ] Default daily goal is one Ruku.
- [ ] Backlog examples produce 0, 1, 2, then 3 exactly as specified.
- [ ] Surplus credit works only when enabled.
- [ ] Reset-time rules are visible and tested.
- [ ] Past corrections recalculate downstream ledger/streak.
- [ ] User can mark reading manually and optionally identify Surah/Ruku.
- [ ] Prayer completion is never inferred automatically.
- [ ] Qada tracking is separate and opt-in.

## Design and privacy

- [ ] Light frosted-glass theme works.
- [ ] Deep-black frosted-glass theme works.
- [ ] UI is one-handed and responsive.
- [ ] Android launcher/adaptive/monochrome/notification icons are included.
- [ ] No dead buttons or placeholder screens.
- [ ] No cloud account or tracking.
- [ ] Backup/restore works and is versioned.
- [ ] All visible settings persist after app restart.
- [ ] The app works offline for its core features.

---

# PART 24 — FINAL INSTRUCTION TO GOOGLE AI STUDIO

Now implement the complete Namaz Times Android application from this specification.

Before writing code, inspect the current generated project and preserve any valid code that already exists. Create the correct native Android project structure, implement the data model and core business logic first, and then connect every Compose screen to real persisted data.

Do not stop after the visual shell. The most important parts are correctness of mosque schedule precedence, the location selection hierarchy, date/time handling across midnight, reminder cancellation/rescheduling, and the Quran backlog ledger. These features must work even when the app is closed or the phone restarts, subject to Android permissions and manufacturer power restrictions.

Use real Kotlin implementations, not pseudocode, for:

- Room entities/DAOs/database and migrations.
- DataStore settings.
- Prayer time calculation and timezone conversion.
- Mosque schedule precedence.
- Active mosque resolver and hysteresis.
- AlarmManager scheduling/cancellation and receivers.
- Notification channels and notification actions.
- Boot/time/timezone/package update rebuild.
- Qibla calculation and sensor status.
- Hijri date calculation and manual adjustment.
- Quran ledger, history, corrections, and streak.
- Backup/export/import.
- Compose screens and all user actions.

If a code sample in this prompt needs an API adjustment for the resolved dependency version, update it to the verified current API and compile it. Do not leave invalid code just because it appears in this prompt.

After implementation:

1. Build/compile the Android app.
2. Run all unit tests.
3. Fix compilation, lint, and test failures that are under your control.
4. Verify app launch, navigation, theme switching, form validation, and empty states.
5. Verify the mandatory Fajr time-change scenario end-to-end.
6. Verify Quran backlog scenarios end-to-end.
7. Verify notification/location permission denial paths.
8. Verify the app can display previously saved prayer schedules and Quran history offline.
9. Verify no fake mosque timings or dummy records remain.
10. Provide a final implementation report listing completed features, any genuine Android/Google AI Studio limitations, required permissions, and tests that actually passed. Never claim a test passed if it was not run.

The finished result must feel like a polished real Android prayer companion, not a generic dashboard template.

---

# PART 25 — TECHNICAL REFERENCES FOR IMPLEMENTATION

Use these primary references while implementing. Check them again if Android APIs or dependency versions change.

1. **Google AI Studio — Build Android apps:** native Android generation using Kotlin and Jetpack Compose, project structure, build mode, and current platform limitations.  
   https://ai.google.dev/gemini-api/docs/aistudio-android

2. **Google AI Studio — Build mode:** platform selection and project generation workflow.  
   https://ai.google.dev/gemini-api/docs/aistudio-build-mode

3. **Android Developers — Schedule alarms:** AlarmManager, exact/inexact alarms, Doze considerations, and boot restoration.  
   https://developer.android.com/develop/background-work/services/alarms

4. **Android Developers — Android 14 exact alarm changes:** exact-alarm permission behavior and permission checks.  
   https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

5. **Android Developers — Geofencing:** geofence regions, transition/dwell behavior, active-geofence limits, and permission requirements.  
   https://developer.android.com/develop/sensors-and-location/location/geofencing

6. **Android Developers — Location permissions:** foreground/background location rules and permission minimization.  
   https://developer.android.com/develop/sensors-and-location/location/permissions

7. **Android Developers — Background location:** Android 11+ user flow for granting background location.  
   https://developer.android.com/develop/sensors-and-location/location/permissions/background

8. **Android Developers — Time-sensitive notifications:** Android 13+ notification permission and notification channels.  
   https://developer.android.com/develop/ui/views/notifications/time-sensitive

9. **Android Developers — Jetpack Glance:** home-screen widget development using Kotlin APIs.  
   https://developer.android.com/develop/ui/compose/glance

10. **Adhan Kotlin — calculation library:** supported presets, Fajr/Isha angles/intervals, Asr settings, high-latitude rules, prayer adjustments, UTC instants, and Qibla API.  
    https://github.com/batoulapps/adhan-kotlin

11. **Google Qibla Finder — Qibla explanation:** Kaaba coordinates and great-circle direction/calibration concepts.  
    https://support.google.com/faqs/answer/7364753

---

## End of master prompt

**Instruction for the user:** In Google AI Studio, choose **Build → Android**, then paste or attach this entire Markdown document as the master specification. Keep this document as the single source of truth for the app requirements. The Android project itself should contain separate, correctly named Kotlin and Android resource files generated from it.

## 15.3 Concrete vector drawable reference

Use a real Android vector asset as the foreground. The final design can be refined, but it must compile and remain readable at adaptive-icon sizes.

`res/drawable/ic_launcher_foreground.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Left minaret -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M17,42 L17,25 L21,19 L25,25 L25,42 L28,46 L28,86 L14,86 L14,46 Z" />

    <!-- Right minaret -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M83,42 L83,25 L87,19 L91,25 L91,42 L94,46 L94,86 L80,86 L80,46 Z" />

    <!-- Main mosque silhouette -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M28,55 L28,48 C28,29 39,17 54,17 C69,17 80,29 80,48 L80,55 L86,55 L86,86 L22,86 L22,55 Z" />

    <!-- Crescent accent -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M68,12 C61,12 55,18 55,26 C55,34 61,40 69,40 C64,36 62,31 62,26 C62,20 64,15 68,12 Z" />
</vector>
```

`res/drawable/ic_launcher_monochrome.xml` may reuse the same silhouette but must remain a single-color vector with no gradients or raster image dependencies. `res/drawable/ic_stat_prayer.xml` must be a separate, simplified, white-only notification icon with transparent background; Android notification small icons must not reuse a multicolor launcher bitmap.

The generated icon must be previewed on light, dark, and themed-icon launcher backgrounds. Keep all important shapes inside the adaptive icon safe area.
