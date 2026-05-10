# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Rabbit Launcher** (formerly Focus Launcher) is a minimalist, dark, AI-aware Android launcher built with Jetpack Compose and Kotlin. It uses [Circuit](https://github.com/slackhq/circuit) (Slack's Compose-driven architecture) as its primary UI architecture pattern.

## Common Commands

```bash
# Build (use dev flavor for development)
./gradlew assembleDevDebug

# Run all unit tests
./gradlew testDebugUnitTest --tests "dev.mslalith*"

# Run tests for a specific module
./gradlew :screens:launcher:testDebugUnitTest --tests "dev.mslalith.focuslauncher.screens.launcher.*"

# Static analysis (also runs automatically on git push via pre-push hook)
./gradlew detekt

# Generate Kover HTML coverage report
./gradlew koverHtmlReportDevDebug

# Clean build
./gradlew clean

# Install on connected device (Samsung)
/home/lotus/Android/Sdk/platform-tools/adb -s adb-R5CY508WWYW-aiEwri._adb-tls-connect._tcp install -r "app/build/outputs/apk/dev/debug/Focus-Launcher-v0.9.0-dev-debug.apk"
```

CI runs `detekt` and `testDebugUnitTest` on every PR. The pre-push hook at `scripts/pre-push` also runs `detekt` automatically.

## Architecture

### Multi-Module Structure

The project uses a layered multi-module architecture:

- **`app/`** — Application entry point; wires all modules together via Hilt and Circuit
- **`core/`** — Shared infrastructure modules:
  - `model` — Data classes and domain models
  - `data` — Repositories, Room database, DataStore, Ktor network client
  - `domain` — Use cases that orchestrate data layer operations
  - `common` — Coroutine dispatchers, extensions, `NetworkMonitor`, `ClockProvider`
  - `ui` — Shared Compose components
  - `screens` — Circuit `Screen` object definitions (all navigation destinations live here)
  - `launcherapps` — Android `LauncherApps` system API wrappers, icon packs, icon cache
  - `circuitoverlay` — Custom Circuit overlay infrastructure
  - `testing`, `testing-compose`, `testing-circuit` — Shared test utilities
- **`feature/`** — Self-contained feature modules (homepage, settingspage, appdrawerpage, clock24, lunarcalendar, quoteforyou, favorites, theme)
- **`screens/`** — Full-screen Circuit screen implementations that compose features together (launcher, editfavorites, hideapps, currentplace, iconpack, about, developer)
  - **`screens/aiscreen/`** — AI chat screen with Pix (rabbit mascot), mock responses

### Pager Layout (3 pages)

`HorizontalPager` in `screens/launcher/Launcher.kt`:
- **Page 0** — Discovery (placeholder, Google feed placeholder)
- **Page 1** — Home (default page; `ON_RESUME` always animates back here)
- **Page 2** — App Drawer

Settings is **not** a pager page — it is pushed as a Circuit screen via `Navigator` from a gear icon on Home. AI screen is also pushed via `Navigator` from Home.

### Circuit Pattern

Every screen follows the Circuit contract pattern with three files:

1. **`*Contract.kt`** — Defines `*State : CircuitUiState` and `*UiEvent : CircuitUiEvent`
2. **`*Presenter.kt`** — Business logic; annotated with `@CircuitInject` for Hilt codegen
3. **`*.kt`** — Composable UI function

All `Screen` objects are defined centrally in `:core:screens`. Navigation happens by pushing/popping screens via `Navigator`.

Circuit uses KSP code generation (`circuit.codegen.mode = hilt`) to auto-generate Hilt presenter factories.

### Convention Plugins

Custom Gradle plugins in `build-logic/convention/` standardize module setup:

| Plugin | Use for |
|---|---|
| `focuslauncher.android.feature` | Feature modules — auto-adds core:model, core:ui, core:data, core:common, core:domain, core:resources |
| `focuslauncher.screen.new` | Screen modules — auto-adds Circuit codegen, circuit-overlay, core:screens |
| `focuslauncher.android.library` | Plain Android library modules |
| `focuslauncher.android.library.compose` | Compose-enabled library modules |
| `focuslauncher.android.hilt` | Adds Hilt DI to any module |
| `focuslauncher.android.room` | Adds Room with KSP |

### Product Flavors

- **`dev`** — Development builds (`applicationId = dev.mslalith.focuslauncher.dev`)
- **`store`** — Production/Play Store builds

### Testing Conventions

- Presenter tests extend `PresenterTest<P, S>` from `:core:testing-circuit`, which provides `runPresenterTest`, a `FakeNavigator`, and helpers like `assertFor`/`assertForTrue`
- `CoroutineTest` (from `:core:testing`) is the base for non-presenter unit tests using Turbine and `kotlinx-coroutines-test`
- Production dependencies (repos, use cases) have `Test*` fakes (e.g., `TestLauncherAppsManager`) used in unit tests instead of mocks where possible
- MockK is available for mocking when fakes don't exist
- `FakeWeatherApi` lives in `core/data/.../network/api/fakes/` and is bound via `TestNetworkModule` (`@TestInstallIn`) — never hardcode temperature in tests

### Dependency Injection

Hilt is used throughout. Each module has a `di/` package with `@Module` objects. Test modules in `di/test/` (e.g., `TestNetworkModule`) replace production bindings in tests via `@TestInstallIn`.

---

## Key Design Decisions (Rabbit Launcher)

### Theme
- Always **dark** — `#000000` background, `#FFFFFF` text, `#4ADE80` green accent
- `LauncherTheme.kt` ignores the `Theme` enum and always applies `darkColorScheme`
- The `Theme` enum is kept intact to avoid breaking DataStore serialization

### App Drawer
- **`AppDrawerPage.kt`**: `Column` → search field + `Box` (apps list + alphabet index overlay)
- **`AlphabetIndex.kt`**: two-layer approach:
  - `Canvas(fillMaxSize)` — drawing only, no pointer input (bubble can render outside strip)
  - `Box(48dp wide, right-aligned)` — pointer input only; never blocks the list
- **`AppDrawerListItem.kt`**: uses `detectTapGestures` (not `combinedClickable`) so the DOWN event is consumed immediately, preventing Samsung edge-gesture system from intercepting the first tap
- **`AppsList.kt`**: no `animateItemPlacement()` to avoid hit-test drift during recomposition
- **Alphabet index scroll**: `charIndexAt` maps Y position → character group; `LazyListState.scrollToItem` jumps to that group's item index (one item per group in the LazyColumn)
- **Search auto-launch**: if filtered results narrow to exactly 1 app, it launches automatically
- **Screen time**: `UsageStatsManager` reads today's foreground time per package (requires `PACKAGE_USAGE_STATS` permission granted by user); shown as `Xh Ym` or `Xm` next to each app name in the drawer

### Home Screen
- **`HomePagePresenter.kt`**: computes date string, day progress (minutes/1440), year progress (dayOfYear/daysInYear), battery level via `BatteryManager`, weather via `WeatherRepo`
- **Weather**: `WeatherApiImpl` calls open-meteo.com; location obtained via `LocationManager.getLastKnownLocation` / `requestLocationUpdates`; location permission requested at runtime from the `HomePage` composable (the embedded overload used by `Launcher.kt`)
- **`HomePage.kt`** has **two overloads**:
  1. `@CircuitInject` annotated — used by Circuit's standalone screen navigation
  2. Non-annotated with `onNavigateToAiScreen` / `onNavigateToSettings` params — used by `Launcher.kt`'s pager; this overload handles location permission logic
- **Favorites**: `FavoritesListUiComponent` reads `UsageStatsManager` on composition and on `ON_RESUME`; shows usage time (small text, `onSurfaceVariant` color) in the upper-left corner of each favorite app item

### App Loading & Caching
- `LoadAllAppsUseCase(forceLoad = false)` — skips the system `LauncherApps` scan when the Room DB already has data
- `LauncherPresenter` passes `forceLoad = false`; first launch (empty DB) still triggers a full scan
- `PackageActionUseCase` handles installs/uninstalls surgically (add/remove single app)

### Navigation (ON_RESUME)
- `LauncherInternal` uses `OnLifecycleEventChange`: on `ON_RESUME`, if `pagerState.currentPage != 1`, animates to page 1 (Home)
- This ensures returning from any launched app always lands on Home, not the drawer

### AI Screen (`screens/aiscreen/`)
- Circuit screen: `AiScreenContract` / `AiScreenPresenter` / `AiScreen.kt`
- Mock responses with 1.5s thinking delay; `PixState` enum drives mascot animation state
- `PixMascot.kt`: Canvas-drawn rabbit; idle bounce via `InfiniteTransition`; THINKING = pulsing opacity; RESPONDING = ear rotation
- Pushed via `Navigator` from Home's AI button; not a pager page
