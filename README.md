# PixelForge — Native Android Pixel Art Studio

PixelForge is a native Android pixel-art studio and sprite creation tool built with **Kotlin**, **Jetpack Compose**, **Android Canvas**, and local-first **Room Database** & app-private transactional file storage.

Designed with a custom retro-futuristic dark neon aesthetic (deep navy `#080B18`, vivid purple `#8B5CF6`, electric cyan `#22D3EE`, and emerald `#34D399`), it operates **100% offline with zero cloud accounts, zero tracking, and no external AI or internet dependencies**.

---

## Key Features

1. **Precision Pixel Art Engine**
   - Logical pixel grid separated from display resolution (16×16, 32×32, 48×48, 64×64, 96×96, 128×128, and custom).
   - High-performance touch-to-grid coordinate conversion.
   - Smooth stroke interpolation via **Bresenham's Line Algorithm** (no missed pixels during fast gestures).
   - Multi-touch pinch-to-zoom (up to 32x) and two-finger pan with fit-to-screen action.
   - Configurable grid-line overlay and checkerboard transparency background.
   - Crisp, razor-sharp nearest-neighbor pixel rendering with zero anti-aliasing.

2. **Complete Creative Tool Suite**
   - **Pencil**: Freehand drawing with configurable brush sizes (1px, 2px, 3px).
   - **Eraser**: Restores pixels to transparency.
   - **Bucket Fill**: Non-recursive, 4-way stack-based flood fill.
   - **Eyedropper**: Sample colors directly from composite canvas layers.
   - **Geometric Shapes**: Bresenham Line, Rectangle outline, Filled Rectangle, Midpoint Circle, and Filled Circle with real-time preview before committing.
   - **Pan & Zoom Tool**: Dedicated canvas navigation tool.

3. **Multi-Layer System**
   - Create, duplicate, rename, reorder (up/down), and delete layers.
   - Layer visibility toggle and opacity adjustment (0% to 100%).
   - Merge active layer down with Porter-Duff alpha blending.
   - Independent layer pixel buffers stored in project file.

4. **Palette & Color Studio**
   - Native Color Studio dialog with **HEX (AARRGGBB)** and **RGB + Alpha** sliders.
   - 9 curated built-in offline retro palettes:
     - *Cyberpunk Neon*
     - *Classic 8-Bit*
     - *Game Boy DMG*
     - *PICO-8*
     - *Fantasy RPG*
     - *Sunset Glow*
     - *Pastel Dreams*
     - *Earth Tones*
     - *Monochrome*
   - Transparent color quick-toggle and recently sampled color history.

5. **Curated Starter Templates**
   - 12 built-in pixel artworks across categories: *Blade of Legend, Retro Life Heart, Mana Elixir, Knight's Crest Shield, Super Mushroom, Dungeon Slime, Cosmic UFO, RPG Grass Tile, Pixel Cottage, Calico Kitty, Ancient Oak, and Electric Cyan Crystal*.
   - One-tap "Use Template" opens a new editable project without altering original template assets.

6. **Local-First Transactional Storage & Autosave**
   - **Room Database** for indexing project metadata and fast gallery queries.
   - App-private versioned JSON file storage (`filesDir/projects/{id}.json`) with atomic `.tmp` file swap to prevent corruption.
   - Debounced autosave (1.2s debounce) to avoid disk stalls while drawing.
   - Complete undo/redo history stack (up to 40 steps).

7. **Crisp PNG Export & Android Sharing**
   - Export artworks at integer scales (1x, 2x, 4x, 8x, 16x, 32x) via nearest-neighbor resampling.
   - Optional solid background color or transparent PNG alpha channel.
   - Native Android Sharesheet integration via `FileProvider`.
   - Android Storage Access Framework (`CreateDocument`) for direct file saving.

8. **Settings & Customization**
   - Theme options: Retro Dark (default), Light, and System Default.
   - Default canvas size and export scale configuration.
   - Default grid line visibility toggle.
   - Local storage disk usage monitor and data clear action.
   - Zero-cloud privacy guarantee statement.

---

## Architecture & Package Structure

```
com.example
├── MainActivity.kt                      # Edge-to-edge Activity hosting PixelForgeApp
├── data/
│   ├── BuiltInPalettes.kt              # 9 bundled retro color palettes
│   ├── BuiltInTemplates.kt             # 12 bundled pixel art templates
│   ├── LocalPreferences.kt             # DataStore settings repository
│   ├── local/
│   │   ├── PixelForgeDatabase.kt       # Room SQLite database
│   │   ├── ProjectDao.kt               # Project metadata queries
│   │   └── ProjectEntity.kt            # Room project table entity
│   ├── repository/
│   │   └── ProjectRepository.kt        # Coordination between Room and JSON storage
│   └── serialization/
│       └── ProjectSerializer.kt        # Versioned atomic project JSON serializer
├── drawing/
│   ├── BresenhamAlgorithms.kt          # Lines, rects, circles, flood fill
│   ├── PixelCanvasState.kt             # Central drawing and layer state machine
│   └── PixelHistory.kt                 # Snapshot-based Undo/Redo stack
├── export/
│   ├── ImageImporter.kt                # External image downsampler
│   └── PngExporter.kt                  # Multi-layer nearest-neighbor PNG exporter
├── model/
│   ├── DrawingTool.kt                  # Enum of tools and descriptions
│   ├── PixelLayer.kt                   # Single layer model and pixel buffer
│   ├── PixelPalette.kt                 # Palette model
│   ├── PixelProject.kt                 # Full project document model
│   └── TemplateItem.kt                 # Template specification
└── ui/
    ├── components/
    │   ├── ColorPickerDialog.kt        # HEX/RGB color picker
    │   ├── CreateProjectDialog.kt      # Canvas creation dialog
    │   ├── ExportDialog.kt             # Resolution scale & export dialog
    │   ├── LayersDialog.kt             # Multi-layer manager dialog
    │   ├── PixelCanvasView.kt          # Android Compose Canvas drawing view
    │   └── ResizeCanvasDialog.kt       # Canvas resize and crop dialog
    ├── navigation/
    │   └── PixelForgeApp.kt            # Root navigation host and bottom bar
    ├── screens/
    │   ├── EditorScreen.kt             # Primary pixel art studio
    │   ├── GalleryScreen.kt            # Artwork gallery with search and sort
    │   ├── HomeScreen.kt               # Dashboard with recent art and shortcuts
    │   ├── SettingsScreen.kt           # Studio preferences and storage
    │   └── TemplatesScreen.kt          # Categorized starter templates browser
    └── theme/
        ├── Color.kt                    # Retro neon palette
        ├── Theme.kt                    # M3 dark and light theme setup
        └── Type.kt                     # Typography configuration
```

---

## Build and Installation Instructions

### Minimum SDK & Compatibility
- **minSdk**: 24 (Android 7.0 Nougat) — ensures 95%+ Android phone compatibility while supporting Java 8+ streams and modern APIs.
- **targetSdk**: 36 (Android 16) — modern Android platform standards with edge-to-edge support and predictive back compatibility.
- **Compile Java Version**: Java 11.

### Step 1: Open in Android Studio
1. Launch Android Studio (Ladybug / Koala or newer recommended).
2. Choose **File > Open...** and select the project directory.
3. Allow Gradle to sync dependencies automatically.

### Step 2: Build Debug APK via Command Line
Run the following Gradle command in the root folder:
```bash
./gradlew assembleDebug
```
*(On Windows: `gradlew.bat assembleDebug`)*

### Step 3: APK Location
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Install on Device or Emulator
Connect an Android device with USB Debugging enabled, then execute:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or click the green **Run 'app'** button (Shift + F10) in Android Studio.

---

## Testing

Run local JVM unit and Robolectric tests via Gradle:
```bash
./gradlew testDebugUnitTest
```
Tests cover Bresenham line generation, rectangle geometry, 4-way flood fill, undo/redo state stack, project JSON serialization and corruption recovery, and layer mutations.
