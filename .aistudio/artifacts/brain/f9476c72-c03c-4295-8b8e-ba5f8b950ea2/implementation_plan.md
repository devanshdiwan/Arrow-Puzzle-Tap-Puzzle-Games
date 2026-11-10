# Arrow Puzzle: Tap Puzzle Games - Implementation Plan

A native Android arrow escape puzzle game built with Kotlin and Jetpack Compose, featuring reverse-construction solvable puzzle generation, 100 progressive levels with custom silhouettes, smooth canvas animations, and offline-first Room persistence.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product and technical decisions were confirmed through Phase 1 clarification:
> - **Arrow Mechanics**: Hybrid system supporting both single-direction straight arrows and multi-segment arrows with 90° elbow bends (as seen in the reference screenshots).
> - **Board Silhouettes**: Mix of rectangular grids and themed geometric silhouettes (heart shapes, diamonds, stars, frames, and mazes) across the 100 levels.
> - **Ad Monetization**: Dual-mode setup with an offline-safe interactive test ad simulation (rewarded video preview overlay, banner bars) along with AdMob SDK integration readiness using standard Google test ad IDs.
> - **App Launcher Icon**: Custom adaptive launcher icon generated directly from the uploaded reference icon asset (`/public/assets/aistudio/gamelogo_faa2c443.png`).

---

### 1. Overview & Core Concept

- **What It Does**: Players are presented with an intricate grid or shaped silhouette of directional and elbow-turning arrows. Tapping an arrow causes it to launch forward along its trajectory. If its forward path to the edge of the board is completely unobstructed, it escapes off-screen with satisfying haptic and visual feedback. If blocked by any other arrow, it shakes, flashes red, and deducts 1 of 3 player hearts. Players clear the entire board to win stars and progress.
- **Target Audience / Persona**: Casual puzzle gamers, brain-teaser enthusiasts, and fans of minimal logic games like "Tap Away" and "Unblock Me".
- **Key Value**: Crisp minimalist visuals, tactile spatial logic satisfaction, zero latency, guaranteed solvable levels, and rich meta-progression (100 levels, daily challenges, trophies, undo, and hints).

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Launch & Splash**: Clean splash transition displaying the custom Arrow Puzzle logo, transitioning to the Home Screen.
2. **Home Screen**:
   - Hero header with animated floating puzzle motif and current player level badge.
   - Large primary **Play / Continue** button with level progress.
   - Quick-access cards: **Daily Challenge** (with calendar streak), **Level Selection** (100 levels with 1-3 star ratings), **Trophies & Achievements**, and **Settings**.
3. **Level Selection Screen**:
   - 4 difficulty tiers: Tutorial (1–10), Easy (11–30), Medium (31–60), Hard (61–100).
   - Card grid showing locked/unlocked state, star badges (0–3 stars), and silhouette type previews.
4. **Gameplay Screen**:
   - Top Bar: Back button, Level title, Star rating progress, 3 Animated Heart Lives, Settings/Pause button.
   - Central Playfield: Responsive Jetpack Compose `Canvas` drawing smooth rounded arrow paths, arrowheads, and tap feedback. Supports interactive tap detection, a toggleable zoom/magnifier lens for dense puzzles, and path clearance raycasting.
   - Bottom Action Bar: Undo button (with badge), Hint bulb button (highlights a guaranteed safe escape arrow), and Reset button.
5. **Game Over Modal**: Heartbreak animation, Retry button, and "Watch Ad for +1 Life" rewarded ad flow.
6. **Victory Screen**: Star celebration (1-3 stars calculated by lives remaining and time), level stats, and Next Level button.
7. **Daily Challenge Screen**: Calendar grid showing daily puzzle completion, monthly trophy progress, and unique daily seed puzzle.
8. **Trophies Screen**: 12 unlockable achievement badges (e.g., First Escape, Perfect 3-Stars, Daily Streak, Untouchable, Master Tapper).
9. **Settings Screen**: Audio toggles (SFX, Music), Haptic Feedback toggle, Ad preferences, Reset Progress with confirmation, and Credits.

#### Visual Identity & Theme
- **Color Palette**:
  - Background: Soft modern off-white `#F8F9FE` with subtle ambient lavender tones.
  - Surface & Cards: Crisp pure white `#FFFFFF` with soft drop shadows and rounded corners (16–24dp).
  - Arrow Inactive / Default: Deep midnight navy `#1A1D2E` with smooth rounded caps and crisp chevron arrowheads.
  - Arrow Active / Escaping: Electric vibrant cobalt blue `#1E6BFF` (matching reference screenshots).
  - Arrow Blocked / Danger: Vivid coral ruby `#FF3B5C`.
  - Hearts & Lives: Radiant crimson red `#FF2D55`.
  - Stars & Hints: Warm amber gold `#FFB800`.
- **Typography**: Clean, geometric modern sans-serif hierarchy (Material 3 displayLarge, titleMedium, labelLarge).
- **Interactive Feedback**:
  - Tapping safe arrow: Springs forward and glides off the canvas edge with fade-out.
  - Tapping blocked arrow: 4-pixel lateral oscillation shake with red flash along the collision obstacle.
  - Custom synthetic Audio synthesizer / SoundPool for crisp tap, escape whoosh, error buzzer, and victory fanfare.

---

### 3. Key Product Decisions & Trade-Offs

#### Decision 1: Reverse-Construction Solvability Engine
- *Chosen Approach*: The puzzle engine generates guaranteed solvable puzzles by starting with an empty board and placing arrows in reverse order (from last escaped to first escaped), ensuring that every puzzle has at least one valid topological removal sequence.
- *Why*: Eliminates the risk of impossible deadlock puzzles that frustrate players and break level progression.
- *Alternatives Considered*: Pure random placement with forward solver validation (slow, high rejection rate on complex silhouettes like hearts).

#### Decision 2: Hybrid Straight & Waypoint Arrow Paths
- *Chosen Approach*: Arrows are represented as an ordered sequence of 2D grid coordinates: a tail, optional intermediate 90-degree corner elbows, and a head pointing in one of 4 cardinal directions (Up, Down, Left, Right).
- *Why*: Perfectly reproduces both standard direct grid levels and the intricate puzzle mazes shown in the user's reference screenshots.
- *Alternatives Considered*: Straight-only arrows (less visually varied and doesn't match the user's uploaded heart and bend images).

#### Decision 3: Offline-First Room Database + DataStore
- *Chosen Approach*: Local SQLite storage via Room Database (`LevelProgressEntity`, `DailyChallengeEntity`, `TrophyEntity`) and Jetpack DataStore for audio/haptic preferences.
- *Why*: Zero cloud latency, works 100% offline, guarantees instant state restoration on app relaunch, and fulfills prompt requirements cleanly.

#### Decision 4: Non-Intrusive Test Ad System + AdMob SDK Hooks
- *Chosen Approach*: An interactive test ad simulator for banners and rewarded video dialogs (offering hints/extra lives) that works smoothly inside the emulator without network timeouts or account requirements, alongside a clean `AdManager` abstraction ready for production AdMob unit IDs.
- *Why*: Never disrupts active gameplay while fully satisfying the monetization requirements.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                  │
│  MainActivity  ──►  NavHost (Compose Navigation)                       │
│  ├── SplashScreen                                                      │
│  ├── HomeScreen                                                        │
│  ├── LevelSelectScreen                                                 │
│  ├── GameplayScreen (Canvas Board + Zoom Lens + Controls)              │
│  ├── PauseModal / GameOverModal / VictoryModal                         │
│  ├── DailyChallengeScreen                                              │
│  ├── TrophyScreen                                                      │
│  └── SettingsScreen                                                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ observes StateFlow / dispatches Events
┌───────────────────────────────────▼────────────────────────────────────┐
│                           VIEWMODEL LAYER                              │
│  GameplayViewModel        HomeViewModel          SettingsViewModel     │
│  - Lives & Score          - Level unlocks        - Audio / Haptics     │
│  - Undo Stack             - Daily streak         - Ad state            │
│  - Hint Engine            - Trophies & Stars                           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ calls
┌───────────────────────────────────▼────────────────────────────────────┐
│                         GAME ENGINE LAYER                              │
│  ArrowPuzzleEngine                                                     │
│  ├── CollisionDetector (Raycasting from arrow head to boundary)        │
│  ├── SolvabilityValidator & HintFinder (Reverse topological scan)      │
│  ├── LevelGenerator & SilhouetteTemplates (Heart, Star, Box, Diamond) │
│  └── LevelCatalog (100 pre-generated & verified levels)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ persists
┌───────────────────────────────────▼────────────────────────────────────┐
│                         REPOSITORY & DATA                              │
│  GameRepository                                                        │
│  ├── Room AppDatabase                                                  │
│  │   ├── LevelProgressDao (levelNumber, stars, completed, bestTime)    │
│  │   ├── DailyChallengeDao (dateKey, completed, stars, seed)           │
│  │   └── TrophyDao (trophyId, unlocked, progress, timestamp)          │
│  └── PreferencesRepository (DataStore: sound, music, haptic, hints)    │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data Entities
- `Arrow(id, segments: List<Point>, direction, isEscaping, isBlockedShake)`
- `LevelData(levelNumber, rows, cols, arrows, difficulty, silhouetteShape, parMoves)`
- `LevelProgressEntity(@PrimaryKey levelId: Int, stars: Int, isUnlocked: Boolean, isCompleted: Boolean, hintsUsed: Int)`
- `DailyChallengeEntity(@PrimaryKey dateKey: String, completed: Boolean, stars: Int)`
- `TrophyEntity(@PrimaryKey id: String, title: String, description: String, isUnlocked: Boolean, progress: Int, maxProgress: Int)`

#### Step-by-Step Implementation Sequence
1. **Project & Package Setup**: Update `applicationId` to `com.arrowpuzzle.game`, update `strings.xml` and `metadata.json` to "Arrow Puzzle: Tap Puzzle Games".
2. **App Launcher Icon**: Configure adaptive launcher icon using the uploaded logo asset `/public/assets/aistudio/gamelogo_faa2c443.png` across all mipmap densities.
3. **Data Layer**: Room Database entities, DAOs, AppDatabase, and GameRepository. DataStore for settings preferences.
4. **Game Engine**: Arrow models, raycasting collision detection, hint solver, undo history, and 100 guaranteed solvable level definitions (Tutorial 1-10, Easy 11-30, Medium 31-60, Hard 61-100) including custom silhouettes.
5. **Audio & Haptics Engine**: Sound effect synthesizer / SoundPool for tap, escape, blocked shake, and victory chimes. Android vibrator haptics integration.
6. **Canvas Game Board Component**: Custom Compose Canvas drawing arrows with rounded strokes, directional heads, tap hit-testing, smooth escape projectile animations, and blocked shake effects.
7. **Screens & Navigation**:
   - Splash & Home Screen
   - Level Selection Screen with star ratings
   - Gameplay Screen with HUD, Hearts, Undo, Hint, and Pause
   - Modals: Pause, Game Over (+1 Life Ad), Level Completed
   - Daily Challenge & Trophy Screen
   - Settings Screen
8. **AdMob / Test Ad Subsystem**: Banner ad presentation and Rewarded Video ad simulation modal with timer and reward callbacks.
9. **Build & Compilation Verification**: Run `compile_applet` and verify zero errors.
