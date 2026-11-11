# Quran Ruku Ledger & Whole-App Refinement Plan

An all-inclusive upgrade to the prayer and Quran tracking ecosystem: delivering an interactive Quran Ruku ledger with dual progress rings, collapsible Juz/Surah checklists, Khatam target pacing, a monthly prayer history heatmap, geofence auto-switching alerts, and authentic Ahl al-Hadith fiqh reference cards.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed during interactive clarification and will guide this implementation:

- **Quran Reading Display**: Implement **both** dual circular progress rings (daily goal progress & Khatam total) AND an interactive Ruku checklist organized by Juz and Surah with collapsible accordions, plus ±1 quick stepper buttons for rapid logging.
- **Reading Goal Strategy**: Configurable daily Rukus with a dynamic Khatam target calculator (computes required Rukus/day to finish by a selected date, e.g., Ramadan, Eid, or end-of-year, handling backlogs and surplus).
- **Comprehensive App Refinements**:
  1. *Prayer History*: Interactive monthly calendar heatmap with streak metrics and punctuality indicators.
  2. *Mosque Automation*: Clear geofence auto-switching alerts and active place banner.
  3. *Ahl al-Hadith Fiqh*: Dedicated reference card modal explaining Ahl al-Hadith prayer methodology (Raf' al-Yadayn, Ameen bil-Jahr, Asr Mithl-1, combined prayer rules during travel/rain).

---

### 1. Overview & Core Concept

- **What It Does**:
  - **Quran Ruku Ledger**: Transforms the Quran screen into a daily reading companion. Users can see their progress toward daily goals and complete Quran recitation (*Khatam*), check off Rukus one-by-one by Surah/Juz or log in batches, and pace themselves with a target date calculator.
  - **Whole-App Refinements**: Enriches the Namaz app with an interactive monthly prayer heatmap, visible auto-switching place banners with proximity notifications, and authentic Sunnah fiqh documentation according to the Ahl al-Hadith school of thought.
- **Target Audience / Persona**:
  - Practicing Muslims who follow the Quran and Sunnah (Ahl al-Hadith), seeking precise prayer times for local mosques (Defence, Clifton, Hala New City) and a disciplined daily Quran recitation habit.
- **Key Value**:
  - Eliminates guesswork in Quran completion, provides transparent ledger tracking with rollover and backlog accountability, and makes mosque switching and prayer history immediately accessible.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Daily Quran Reading & Checkoff**:
   - User opens the Quran tab.
   - At the top, a **Dual Progress Hero** displays:
     - Outer ring: Khatam completion percentage (e.g., 240 / 558 Rukus, 43%).
     - Inner ring: Today's reading goal (e.g., 3 / 4 Rukus today, 75%).
     - Central countdown: "1 Ruku remaining today".
   - Below the hero, quick stepper buttons `[-1] [+1 Ruku] [+4 (1 Ruku/Salah)]` allow instantaneous 1-tap logging.
   - Below the steppers is an **Interactive Ruku Checklist**:
     - Grouped into 30 Juz tabs or collapsible cards.
     - Each Juz expands into its Surahs (e.g., *Al-Baqarah*, *Al-Imran*), showing individual Ruku checkboxes (`Ruku 1`, `Ruku 2`, `Ruku 3...`) with brief topic notes.
     - Tapping a checkbox immediately logs that Ruku, updates the daily total and ledger, and triggers a subtle haptic feedback tick.

2. **Khatam Target Calculator**:
   - User taps "Set Khatam Target" or "Configure Goal".
   - Selects target date (e.g., 30 days, 60 days, or custom date) or daily goal (e.g., 4 Rukus/day).
   - The app recalculates required daily pace, showing how many Rukus are required per day to finish on time.

3. **Monthly Prayer History Heatmap**:
   - In the History tab/sheet, a 30-day visual grid shows prayer punctuality (emerald green for all 5 prayed, gold for marked at mosque, muted tones for partial, red/amber for missed/qada).
   - Tapping any calendar day shows that specific day's prayer completion breakdown.

4. **Ahl al-Hadith Fiqh Reference Guide**:
   - Available via an "Ahl al-Hadith Prayer Guide" action card on the Settings or Today screen.
   - Displays clear, authentic citations for:
     - Asr time calculation (Mithl-1 / Shadow equals object length, as narrated in Sunan Abi Dawud & Tirmidhi).
     - Raf' al-Yadayn (raising hands before and after Ruku, Sahih al-Bukhari).
     - Ameen bil-Jahr (saying Ameen aloud behind the Imam, Sahih al-Bukhari).
     - Fasting on Mondays, Thursdays, and Ayyam al-Beed (White Days).

#### Visual Identity & Theme Tokens

- **Dominant Palette**:
  - Canvas / Background: Midnight Oasis (`#051811`) in dark mode, Pearlescent Alabaster (`#F7FAF8`) in light mode.
  - Primary Accent: Deep Islamic Emerald (`#083324` / `#135D43`).
  - Sacred Accent: Kaaba Kiswa Gold (`#D4AF37`) and Gold Glow (`#FFE082`).
  - Surface: Frosted glass containers with `0.72f` opacity and 1dp subtle gold/emerald borders.
- **Typography & Rhythm**:
  - Stat displays in bold numerals, Arabic calligraphy labels (`القرآن الكريم`, `الصلوات`), and legible labels.
  - Generous 16dp / 20dp edge padding, 48dp minimum touch targets on all checkboxes and buttons.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Quran Structure Model (558 Traditional Rukus)**
  - *Chosen Approach*: Model the standard 30 Juz, 114 Surahs, and 558 canonical Rukus as an in-memory structured dataset, tied directly to the existing Room `quran_sessions` and `QuranLedgerEngine`.
  - *Why*: Allows checking off specific Surah/Ruku positions while preserving the existing, tested mathematical ledger engine (backlog, surplus, streaks).
  - *Alternatives Considered*: Storing only an abstract integer counter without Surah names. Rejected because users requested a checkbox list for tangible reading continuity.

- **Decision 2: Seamless Integration with Existing Ledger Engine**
  - *Chosen Approach*: When a checkbox in the Ruku checklist is toggled, it appends or removes a `QuranSession` with `surah` and `rukuNumber` populated.
  - *Why*: Guarantees zero regression on existing ledger tests (`QuranLedgerEngineTest`), audit trails, and backup/restore capabilities.

- **Decision 3: Local Offline Performance**
  - *Chosen Approach*: Pre-index the Quran Surah and Ruku metadata locally in Kotlin. Fast, instant search and scrolling without any network requests.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                  │
│                                                                        │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │   TodayScreen.kt      │  │           QuranScreen.kt              │  │
│  │  - Next Prayer Hero   │  │  - DualProgressRing (Khatam + Today)  │  │
│  │  - Daily Hadith Card  │  │  - Quick Ruku Stepper (+1, -1, +4)    │  │
│  │  - Timeline & 1-Tap   │  │  - Khatam Target Calculator Sheet     │  │
│  │  - Geofence Status    │  │  - Collapsible Juz/Surah Checklists   │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│                                                 │                      │
│  ┌───────────────────────┐  ┌───────────────────┴───────────────────┐  │
│  │    HistoryScreen.kt   │  │         FiqhGuideDialog.kt            │  │
│  │  - Monthly Heatmap    │  │  - Ahl al-Hadith authentic proofs     │  │
│  │  - Streak Analytics   │  │  - Asr, Ameen, Raf' al-Yadayn guides  │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ State & Actions
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        VIEWMODEL & DOMAIN LAYER                        │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ AppViewModel.kt                                                  │  │
│  │  - recordRukuCheckoff(surah, ruku, isChecked)                    │  │
│  │  - setKhatamTargetDate(targetDate)                               │  │
│  │  - adjustDailyGoal(goal)                                         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                     │                                  │
│  ┌───────────────────────────┐  ┌───┴───────────────────────────────┐  │
│  │ QuranStructureData.kt     │  │ QuranLedgerEngine.kt              │  │
│  │  - 30 Juz metadata        │  │  - Daily target & balance math    │  │
│  │  - 114 Surahs with Rukus  │  │  - Backlog & surplus calculations │  │
│  │  - Ruku names & ranges    │  │  - Streak tracking                │  │
│  └───────────────────────────┘  └───────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Reactive Flow
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            DATA PERSISTENCE                            │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ NamazDatabase (Room)                                             │  │
│  │  - quran_sessions: append-only reading sessions with surah/ruku  │  │
│  │  - quran_goal_history: effective date goal history               │  │
│  │  - prayer_history: marked prayers with context and timestamps    │  │
│  │  - mosques, places, schedules: local timetable tables            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Planned Component Updates

1. **`QuranStructureData.kt`** (New Domain Model):
   - Defines canonical Quran structure: 30 Juz, 114 Surahs, and standard Ruku divisions (558 Rukus total).
   - Provides helpers: `rukuCountForSurah(surahNumber)`, `juzForRuku(globalRukuIndex)`, and `surahNameArabic(surahNumber)`.

2. **`DualProgressRing.kt`** (New Design Component):
   - Concentric dual Canvas sweep ring displaying Khatam completion and today's target with smooth animated sweep transitions.

3. **`QuranScreen.kt`** (Enhanced):
   - Integrates `DualProgressRing`.
   - Adds quick action buttons (`+1`, `+4`, `-1`).
   - Adds Khatam target calculation modal (calculates required daily pace based on user-chosen Khatam target date).
   - Embeds collapsible Juz/Surah Ruku checklist with checkbox state bound to logged sessions.

4. **`PrayerHistoryHeatmap.kt`** (New Component in History Screen):
   - Monthly calendar grid visualizing prayer completion consistency across all 5 prayers with day-by-day modal.

5. **`FiqhGuideDialog.kt`** (New UI Component):
   - Beautiful authentic Ahl al-Hadith reference guide modal with cited Ahadith.

---

### Verification & Testing Plan

1. **Build Verification**:
   - Run `compile_applet` to ensure clean compilation.
2. **Automated Unit Tests**:
   - Run existing unit test suite `gradle :app:testDebugUnitTest` to guarantee all 52 tests pass.
   - Add new unit tests for Khatam pace calculation (`KhatamCalculatorTest`) and Ruku metadata mapping.
3. **Manual Flow Testing**:
   - Test toggling Ruku checkboxes and verify ledger numbers update instantly.
   - Test setting a Khatam target date and verify the daily required goal recalculates accurately.
   - Verify dark and light theme contrast and touch target responsiveness.
