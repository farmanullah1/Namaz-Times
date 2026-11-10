# UI/UX Modernization & Interactive Feature Plan for Noor Salah

Comprehensive design and experience overhaul elevating Noor Salah into a premium Islamic application. This plan enhances the Today dashboard with an animated prayer progress ring, one-tap quick prayer logging, luxury Islamic aesthetics (deep emerald, Kaaba gold, frosted glass), an interactive audio Adhan preview sheet, daily Hadith reflections, Sunnah fasting badges, and an immersive Qibla compass.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed through the interactive design questionnaire:
> - **Visual Aesthetic**: Cohesive modern Islamic luxury—deep emerald tones, radiant Kaaba gold accents, and frosted glass cards, coupled with crisp Material 3 typography across both light (warm ivory/sand) and midnight oasis dark modes.
> - **Primary Workflow Focus**: The **Today Home Screen**, transforming the Next Prayer card into an animated hero with real-time countdown, radial progress ring, and active mosque status.
> - **Confirmed Interactive Features**:
>   1. Animated circular progress ring with one-tap prayer completion logging.
>   2. Audio Adhan preview player and sound selector sheet.
>   3. Daily authentic Hadith reflections and Sunnah fasting badges (Ayyam al-Beed, Mondays/Thursdays).
>   4. Immersive Qibla compass dial with haptic alignment and tilt calibration guidance.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms Noor Salah from a functional timetable utility into an inviting, daily spiritual companion. It provides glanceable countdowns, visual cues for current and upcoming prayer windows, immediate 1-tap prayer tracking, sound previews, authentic daily Hadith, and an animated Qibla dial.
- **Target Audience**: Practicing Muslims—specifically adhering to the Ahl al-Hadith fiqh standard—who travel between local mosques (e.g. Defence, Clifton, Hala) and value aesthetic elegance, astronomical accuracy, and spiritual reminders.
- **Key Value**: Reduces cognitive load with glanceable visual hierarchy, eliminates multiple-tap friction for daily prayer logging, and instills a serene spiritual ambiance through tailored colors and typography.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Glance & Quick Log (Today Screen)**:
   - User opens the app and immediately sees the **Next Prayer Hero Card** with a circular animated progress ring showing time elapsed in the current prayer window and countdown to the next prayer.
   - Next to the prayer countdown is a direct **Quick Mark** button. Tapping it instantly records the prayer with a haptic ripple and gold celebratory checkmark without opening complex dialogs.
   - Mosque selector pill at the top shows the active mosque (e.g., Defence Mosque), distance, and tap-to-switch affordance.

2. **Daily Hadith & Sunnah Fasting Bar**:
   - Right below the header, an elegant gilded banner shows today's Hijri date and Sunnah fasting reminder (e.g., "White Days Sunnah Fast (13th Jumada al-Awwal)").
   - A curated daily Hadith card with Arabic calligraphy accents presents an authentic narration on prayer, wudu, or remembrance.

3. **Audio Adhan & Sound Selector Sheet**:
   - Tapping the bell icon on any prayer row opens a bottom sheet with a live sound preview (Takbeer, full Adhan, gentle short tone, beep, or silent vibrate), volume slider, and per-prayer notification preferences.

4. **Immersive Qibla Compass**:
   - Clean full-screen dial with smooth needle interpolation, degree markers, Kaaba direction angle, distance in kilometers, real-time sensor accuracy badge, and figure-8 calibration guidance when needed.
   - Vibrates subtly and pulses emerald green upon achieving true Qibla alignment.

### Visual Identity & Theme

- **Palette Tokens**:
  - *Primary Emerald*: Deep Forest Emerald (`#093824` dark, `#0D5A3D` light).
  - *Accent Gold*: Radiant Kaaba Gold (`#D4AF37`, `#F2B835`).
  - *Surface / Glass*: Semi-translucent frosted cards with subtle 1dp gold/white borders (`0x1FFFFFFF` / `0x22D4AF37`) and tonal elevation.
  - *Light Mode Background*: Warm sand & ivory gradient (`#F9F9F4` to `#EDECE4`).
  - *Dark Mode Background*: Midnight Oasis (`#06130D` to `#0B1F17`).
- **Typography**:
  - Bold tabular numbers for the countdown and clock times to prevent jitter.
  - Arabic Nastaliq / Naskh script styling for Hijri dates and prayer names (`الفجر`, `الظهر`, `العصر`, `المغرب`, `العشاء`).
  - Clear M3 title and body hierarchies with generous line spacing.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct 1-Tap Quick Mark vs. Multi-Field Dialog**
  - *Chosen Approach*: Provide a 1-tap quick mark button on the Today timeline for immediate completion, with a long-press or detail tap to add custom notes, tags, or congregation context.
  - *Why*: Daily prayer tracking is repeated 5 times a day; zero-friction logging ensures high user retention.
  - *Alternatives Considered*: Always opening the mark dialog, which created unnecessary friction.

- **Decision 2: Radial Progress Ring vs. Horizontal Linear Bar**
  - *Chosen Approach*: Centerpiece circular progress ring encircling the countdown in the Hero Card.
  - *Why*: Circles mirror astronomical cycles (sun arc, compass dial, lunar phase) and provide balanced visual weight.

- **Decision 3: Embedded Curated Authentic Hadiths vs. Remote Network API**
  - *Chosen Approach*: Bundle 40 authentic Hadiths locally (Sahih al-Bukhari, Sahih Muslim) rotating daily by day of year.
  - *Why*: Zero network dependencies, instant zero-latency loading, and offline reliability.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI Layer (Jetpack Compose)                    │
│                                                                         │
│  ┌───────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │     TodayScreen       │  │     QiblaScreen     │  │ MosquesScreen │  │
│  │ ┌───────────────────┐ │  │ ┌─────────────────┐ │  │               │  │
│  │ │PrayerProgressRing │ │  │ │EnhancedCompass  │ │  │               │  │
│  │ └───────────────────┘ │  │ └─────────────────┘ │  │               │  │
│  │ ┌───────────────────┐ │  │ ┌─────────────────┐ │  │               │  │
│  │ │DailyHadithCard    │ │  │ │TiltGuidanceView │ │  │               │  │
│  │ └───────────────────┘ │  │ └─────────────────┘ │  │               │  │
│  │ ┌───────────────────┐ │  │                     │  │               │  │
│  │ │SoundSelectorSheet │ │  │                     │  │               │  │
│  │ └───────────────────┘ │  │                     │  │               │  │
│  └───────────┬───────────┘  └──────────┬──────────┘  └───────┬───────┘  │
└──────────────┼─────────────────────────┼─────────────────────┼──────────┘
               │                         │                     │
               ▼                         ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      State Layer (AppViewModel)                         │
│  - today: StateFlow<TodayContext?>                                      │
│  - soundPreview: AudioPlayer                                            │
│  - compassHeading: StateFlow<Float>                                     │
│  - dailyHadith: StateFlow<DailyHadith>                                  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Data & Repository Layer                            │
│  - MosqueRepository (Room DAO)       - PrayerHistoryRepository          │
│  - AdhanPrayerCalculator             - CompassSensorManager             │
│  - SettingsRepository (DataStore)    - ReminderAudioPlayer              │
└─────────────────────────────────────────────────────────────────────────┘
```

### Component & State Enhancements:
- `PrayerProgressRing.kt`: Canvas-based sweep gradient ring with smooth animated progress and centered digital countdown.
- `DailyHadithProvider.kt`: Offline collection of authentic Ahadith rotating with day-of-year.
- `SoundSelectorSheet.kt`: Modal bottom sheet with play/stop audio preview and sound mode picker.
- `EnhancedQiblaDial.kt`: Polished compass dial with Kaaba beacon, degree ticks, and tilt warning when sensor reports excessive inclination.
