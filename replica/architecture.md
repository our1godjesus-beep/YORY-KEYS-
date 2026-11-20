# YORY KEYS Replica architecture

Date: 2026-11-20
Status: proposed implementation plan based on current YORY source and clean-room recon.

## 1. Stack decision

Keep the existing native stack. A rewrite to a cross-platform web wrapper would make the system keyboard integration worse and is not justified.

| Layer | Existing / chosen | Why |
|---|---|---|
| Android Studio app | Kotlin + Jetpack Compose | Already in repository; native settings, system intents and WindowInsets |
| Android keyboard | InputMethodService + InputConnection | Already implemented; direct native input integration |
| iOS Studio app | SwiftUI | Already in repository; native NavigationStack and system settings guidance |
| iOS keyboard | UIKit UIInputViewController | Already implemented; required native custom keyboard extension |
| Theme contract | shared JSON Schema v1 / .yorytheme | Cross-platform preset/editor/renderer contract already exists |
| Android settings storage | current local preferences + YoryThemeStore | Preserve offline-first baseline; consolidate keys behind a SettingsRepository |
| iOS settings storage | App Group UserDefaults + theme document | Share only preferences/theme snapshots with the extension |
| Local data | platform-private app storage; encrypted personal dictionary | Typed content remains local; clipboard stays platform-specific |
| Optional account/cloud | Supabase in the containing app only | Optional sync/publishing, not required by live keyboard |
| Test | Kotlin unit/instrumentation + XCTest where possible + manual device matrix | Keyboard extensions and system bars require real OS/device validation |

No microservices. No Microsoft private API. No network request from the keystroke hot path.

## 2. Navigation architecture

Use a native hierarchical route stack.

| Route | Screen ID | Platform |
|---|---|---|
| /studio | S01 | Android, iOS |
| /studio/themes | S02 | Android, iOS |
| /studio/themes/:themeId | S03 | Android, iOS |
| /studio/themes/create | S04 | Android, iOS |
| /studio/languages | S05 | Android, iOS |
| /studio/languages/:tag | S06 | Android, iOS |
| /studio/typing | S07 | Android, iOS |
| /studio/emoji | S08 | Android, iOS |
| /studio/rich-input | S09 | Android, iOS |
| /studio/clipboard | S10 | Android only in v1 |
| /studio/layout-keys | S11 | Android, iOS |
| /studio/resize | S12 | Android only for free-position resize |
| /studio/sound-vibration | S13 | Android, iOS |
| /studio/privacy | S14 | Android, iOS |
| keyboard runtime | S15 | Android IME, iOS extension |
| keyboard toolbar | S16 | Android IME, iOS extension |
| toolbar customization | S17 | staged |
| onboarding/setup | S18 | Android, iOS |
| account | S19 | Android, iOS |
| help/about | S20–S21 | Android, iOS |

Navigation contract:
- A settings row pushes its detail; detail back returns to the same scroll position on S01.
- Theme detail returns to the selected category and scroll offset.
- A theme edit that has unsaved changes prompts Save / Discard / Continue editing.
- OS setup launches system settings only through documented intents/links, then re-checks the system keyboard-enabled/selected state when the app resumes.
- Do not implement a four-tab bottom bar. The keyboard quick action is not a navigation tab.

## 3. Core modules

### studio-ui
- Root Settings screen and route stack.
- Reusable rows, toggle/slider/chip/color controls and info cards.
- Platform responsive size classes and RTL layout.
- Searchable settings only when real search behavior exists; don't add a decorative search field.

### settings-domain
- Typed setting keys and validation ranges.
- Read/write API independent of UI.
- Change notifications to the keyboard renderer.
- Atomic updates for groups such as keyboard geometry.

### theme-domain
- Existing ThemeDocument schema is source of truth.
- ThemeRepository: list presets, load current, save custom, apply, duplicate, delete, import, export, reset.
- ThemePreview uses the same renderer rules as the live keyboard.
- Local presets always available offline.
- Optional published themes live in a separate cloud repository and cannot be required by local typing.

### language-domain
- Installed language/layout manifest, selected layout, direction and capability level.
- A language can have a keyboard layout without having a prediction dictionary. Expose this distinction.
- v1: English (US) and Arabic (Egypt) as supported core layouts; German QWERTZ remains basic until a quality-checked language pack is ready.

### keyboard-runtime
- Android InputMethodService and iOS UIInputViewController.
- Read a compact local settings/theme snapshot.
- No HTTP, Supabase, analytics, cloud sync or disk-heavy work per keypress.
- Sensitive editor traits suppress prediction, personal learning and clipboard suggestions.
- Platform-specific toolbar registry so unsupported controls are hidden, not greyed out as if working.

### clipboard-domain
- Android v1 only, local and opt-in.
- Store text clips, timestamps, optional pin/expiry state; no cloud upload.
- Suppress sensitive field ingestion and obvious one-time codes; heuristics are not a guarantee.
- iOS displays a truthful capability explanation until a user-driven pasteboard flow is actually implemented.

### account-cloud
- Optional Supabase Auth and cloud feature client in Studio.
- Preferences/themes sync only after explicit opt-in.
- Never sends typed text, predictions, password fields, or clipboard payloads.
- Sign-out does not delete local themes, language layouts or user settings.

## 4. Data contracts

### StudioPreference
- key: stable string enum
- value: type-safe scalar/object
- updatedAt: local timestamp
- source: user/system/default
- platform: android/ios/shared

### LanguageLayout
- tag, nativeName, displayName, layoutId, direction, isInstalled, capabilityLevel
- capabilityLevel: layout_only | suggestions | prediction_pack | certified
- activeLanguage tag persisted locally.

### ThemeDocument
- Continue using shared/specs/yory-theme.schema.json.
- Extend with migration rather than adding platform-specific unversioned fields.
- Preserve exact values: background, key, keyPressed, text, accent, glass opacity, radius, key spacing, key height, font family and size.
- Theme editor writes versioned document; keyboard reads a small resolved theme snapshot.

### KeyboardProfile (future extension)
- heightDp, widthMode, horizontalOffset, verticalOffset, oneHandedSide, floatingMode, splitMode.
- Only fields supported by a platform/device may be shown in UI.
- Current code supports a keyboard-height slider, not complete resize/reposition behavior.

### ClipboardEntry (Android v1)
- id, text, createdAt, expiresAt, pinned, classification.
- Local-only; classification is best-effort and not guaranteed.
- No image/screenshot harvesting and no cloud sync.

### AccountSession
- provider, userId, secureTokenRef, expiry, optionalSyncConsent.
- Stored via current secure session storage.
- A session does not gate offline Studio or keyboard use.

## 5. Routes / local operations

| Operation | Who | Input | Result |
|---|---|---|---|
| readStudioSettings | Studio + keyboard | setting keys | local typed values |
| updateStudioSettings | Studio | key/value + validation | local persisted setting |
| listThemes | Studio | platform/capability | local theme list |
| previewTheme | Studio | ThemeDocument | rendered preview |
| applyTheme | Studio | theme ID/document | active local theme snapshot |
| importTheme / exportTheme | Studio | .yorytheme file | validated local document |
| listLanguages | Studio | platform | installed layouts/capabilities |
| selectLanguage | Studio or keyboard | language tag | local active layout |
| updateKeyboardProfile | Studio | supported geometry values | local keyboard profile |
| listClipboard / insertClip / pinClip / deleteClip | Android keyboard | local entry ID | local clipboard result |
| openKeyboardSettings | Android Studio | none | Android system keyboard settings |
| openKeyboardPicker | Android Studio | none | InputMethodPicker |
| signIn / signOut | Studio | credentials/session | optional account state |
| syncPreferences | Studio background only | opt-in changed preferences | cloud sync status |

## 6. Platform insets and resizing

### Android
- Handle status bar, display cutout, navigation bar/gesture area and IME as WindowInsets.
- Android 15+ enforces edge-to-edge when targeting SDK 35; apply insets exactly once.
- Prefer Scaffold content padding or explicit WindowInsets, not both for the same edge.
- Keep the floating keyboard action above navigationBars insets.
- For the settings screen, scrolling content adjusts to IME insets when text/color fields are focused.
- Test 3-button navigation, gesture navigation, predictive Back, font scaling, cutouts and rotation.

Official references:
- https://developer.android.com/develop/ui/compose/system/insets
- https://developer.android.com/develop/ui/compose/system/setup-e2e
- https://developer.android.com/develop/ui/compose/system/material-insets

### iOS
- Use safeAreaInsets/safeAreaLayoutGuide for the host app.
- Keep the home-indicator region clear of interactive controls.
- Respect Dynamic Island/status bar and Dynamic Type.
- Keyboard extension must use its actual available height and system-provided host behavior.
- Test third-party keyboard disabled, secure fields, iPad docked/floating and next-keyboard switching.

Official reference:
- https://developer.apple.com/documentation/uikit/uiview/safeareainsets

## 7. Implementation order

1. Replace the current one-list Studio shell with a typed route stack while keeping the same settings categories.
2. Build S02 Themes gallery, S03 preview/detail and complete S04 editor with a shared live preview.
3. Complete S05/S06 language management and capability badges.
4. Complete S11/S12 Android resizing and geometry; keep iOS capabilities truthful.
5. Unify settings storage and immediate keyboard refresh across app/extension lifecycle.
6. Add S16 toolbar registry per platform and Android clipboard manager S10.
7. Complete setup, privacy, empty/error/offline states.
8. Run the test plan on emulator/simulator; then physical phones and tablets.

## 8. Definition of done

- No app bottom-tab bar that conflicts with the observed hierarchical navigation model.
- Every setting displayed is wired to a real local value or explicitly marked unavailable.
- Theme preview and actual keyboard renderer show the same values.
- Android resize handles work with pointer/touch and survive rotation/relaunch.
- iOS has no fake resize feature; extension height remains compatible with system constraints.
- Offline typing and local settings work in airplane mode.
- No keystroke, password, personal dictionary text or clipboard text is uploaded.
- UI remains readable and operable at 320, 360, 390 and 430 dp widths; tablet/foldable is adaptive.
- No bottom control overlaps Android 3-button navigation, gesture handle, iOS home indicator, Dynamic Island or keyboard IME transition.
- Device QA passes before any feature is called production-ready.
