# YORY KEYS — SwiftKey-inspired product reconstruction

Scope: Android and iOS companion app + native keyboard, based on the user's supplied Microsoft SwiftKey screenshots and public Microsoft help documentation. Rebuild equivalent user jobs and interaction patterns with original YORY branding, copy, artwork and code.
For: YORY KEYS users who need a customizable bilingual keyboard and a reliable offline-first settings experience.
Date: 2026-11-20
Method: Replica clean-room reconnaissance. Screenshots are evidence of visible layout and states; public help pages supplement behavior. No Microsoft source code, private APIs, binaries, logos, artwork, licensed fonts or service endpoints are used.

## Executive conclusion

The supplied screenshots show a hierarchical settings app, not a four-tab application. The root is a vertically scrolling settings list. Each row opens a detail screen with a back control. The Themes section has its own gallery/category browsing pattern. A floating keyboard shortcut appears near the lower-right in several settings screenshots, but it is an app action and must sit above the operating-system safe area. The native Android navigation bar/gesture handle and the iOS home indicator are separate system-owned controls.

The most important corrections for YORY are:
1. Preserve hierarchical settings navigation; do not add a persistent four-tab bottom bar just because the app is mobile.
2. Build a real theme gallery, theme detail/preview and editor flow. YORY now has native Android/iOS theme galleries, local categories, Apply states, and named offline custom-theme saving. A dedicated theme-detail route, full theme-library management and live-renderer parity remain.
3. Upgrade the current height slider into platform-aware keyboard sizing. Microsoft documents drag-handle resizing and repositioning on Android; it explicitly says this resize feature is not offered on iOS.
4. Treat Rich Input as platform-specific. The current YORY source has an Android local clipboard slice, while iOS explicitly tells users that clipboard history is not available in the current preview.
5. Keep live typing offline. Account, cloud sync and any optional online tools are separate from the keyboard input path.
6. Validate safe areas, system bars, keyboard/IME resizing, Dynamic Type, RTL and small screens on real devices. Screenshots cannot prove those runtime behaviors.

## Sources

| # | Source | URL / reference | What it supports |
|---|---|---|---|
| 1 | User-supplied Microsoft SwiftKey screenshots | Private Library references listed in replica/screens/README.md | Visible screens, text hierarchy, settings grouping, theme cards, toolbar and keyboard appearance |
| 2 | Microsoft SwiftKey setup guide | https://support.microsoft.com/en-us/swiftkey-keyboard/how-to-set-up-microsoft-swiftkey-keyboard | Settings entry, language/layout management, typing, sound and vibration setup |
| 3 | Microsoft SwiftKey toolbar guide | https://support.microsoft.com/en-us/swiftkey-keyboard/how-do-i-access-toolbar-in-microsoft-swiftkey-keyboard | Toolbar groups: themes, clipboard, layouts, modes, translator, account, resize, emoji and related tools |
| 4 | Microsoft SwiftKey resize guide | https://support.microsoft.com/en-us/swiftkey-keyboard/how-to-resize-microsoft-swiftkey-keyboard | Android resize route, drag handles and repositioning; no equivalent resize option on iOS |
| 5 | Microsoft SwiftKey clipboard guide | https://support.microsoft.com/en-us/swiftkey-keyboard/how-does-the-clipboard-work-with-microsoft-swiftkey-keyboard | Clipboard save, paste, delete, undo and pinned clips |
| 6 | Microsoft SwiftKey voice typing guide | https://support.microsoft.com/en-us/swiftkey-keyboard/how-do-i-use-voice-to-text-with-microsoft-swiftkey-keyboard | Voice typing and multimodal voice typing behavior |
| 7 | YORY KEYS source repository | https://github.com/our1godjesus-beep/YORY-KEYS- | Current implementation comparison |
| 8 | Android Compose WindowInsets guidance | https://developer.android.com/develop/ui/compose/system/insets | Android system-bar, navigation and IME inset handling |
| 9 | Android edge-to-edge setup | https://developer.android.com/develop/ui/compose/system/setup-e2e | Android 15 / target SDK 35 edge-to-edge behavior |
| 10 | Apple safe-area guidance | https://developer.apple.com/documentation/uikit/uiview/safeareainsets | iOS safe areas around system and ancestor UI |

## Core loop

Open YORY Studio → find a setting or theme → make a change → save it locally → preview the actual keyboard → return to any text field and verify that the keyboard reflects the change. Account creation and cloud features are optional and must never block this loop.

## Screens

| ID | Screen | Route / how to reach | Purpose | Key components | States seen / required |
|---|---|---|---|---|---|
| S01 | Studio settings home | Launch YORY Studio | Main destination for configuration | Brand header, vertical settings rows, icon, title, subtitle, chevron, keyboard quick-action | Populated; long list scroll; selected keyboard; keyboard not enabled |
| S02 | Theme gallery | S01 → Themes | Browse and choose themes | Top bar with back/Create, category chips (All/Yours/Dark/Light/Colorful), theme cards, generated keyboard preview, applied marker, local Apply/Customize | Selected, built-in, user-saved, offline, empty category |
| S03 | Theme detail / preview | S02 → theme card | Inspect a theme before applying | Large keyboard preview, theme name, apply button, details | Preview, applying, applied, error |
| S04 | Theme editor | S02 → Create or S03 → Customize | Create and tune a theme | Keyboard preview, preset selector, color controls, opacity, radius, height, spacing, font size, key shape, import/export | Default, custom, unsaved, saved, invalid HEX, import/export success/error |
| S05 | Languages | S01 → Languages | Choose installed languages | Language/native name, layout label, active marker, edit/settings affordance, add language | English, Arabic, German basic-layout state, loading, unavailable |
| S06 | Language/layout detail | S05 → language | Choose a keyboard layout for a language | Layout variants, active selection, explanatory text | Selected, not selected, unsupported layout |
| S07 | Typing | S01 → Typing | Control suggestions and local learning | Suggestion toggle, capitalization, personal-learning toggle, privacy explanation | Enabled, disabled, saved, restricted input |
| S08 | Emoji | S01 → Emoji | Control emoji behavior | Emoji predictions, dedicated emoji key, recents, font/style option | On/off, unsupported option, empty recents |
| S09 | Rich Input | S01 → Rich input | Access secondary input tools | Clipboard, voice typing, optional online tool explanations, compact mode | Android/iOS capability variants, permission denied, offline, unavailable |
| S10 | Clipboard manager (Android v1) | S09 or keyboard toolbar | Paste/manage recent copied text | Clip rows, pin, insert, delete, undo, clear | Empty, filled, pinned, expired, cleared, sensitive item suppressed |
| S11 | Layout & keys | S01 → Layout & keys | Tune keyboard structure | Resize entry, number row, number position, long-press symbols, accents, arrow keys, key pop-ups, large key text, long-press duration | Toggle states, slider values, compact screen |
| S12 | Resize editor (Android) | S11 → Resize | Adjust keyboard key height | Keyboard preview, 36–60dp height slider, Compact/Standard/Tall presets, Reset, local persistence | Default, resized, preset selected, reset, keyboard reopened |
| S13 | Sound & vibration | S01 → Sound & vibration | Tune key feedback | Sound toggle/volume/profile, haptic toggle/system setting/duration | On/off, muted, slider minimum/maximum |
| S14 | Privacy | S01 → Privacy | Explain data handling and controls | Privacy-policy links, data-sharing explanation, local data controls, third-party service disclosures | Default, offline, link open, clear-data confirmation |
| S15 | Keyboard runtime | Any text field → YORY KEYS | Main typing surface | Toolbar, suggestions, language/layout control, letter rows, number/symbol mode, spacebar, enter, delete, emoji | English, Arabic RTL, numeric, symbols, secure field, email/URL/search/phone field |
| S16 | Expanded keyboard toolbar | Keyboard → toolbar expand | Reach keyboard tools without leaving the text field | Emoji, clipboard (Android), language/layout, settings, more tools, tool labels | Expanded/collapsed, selected tool, unavailable tool |
| S17 | Toolbar customization | S16 → Customize | Arrange available shortcuts | Available/active tool list, drag/reorder, reset | Default, reordered, unavailable tool |
| S18 | First-run / keyboard setup | First launch or S01 setup CTA | Enable and select YORY KEYS in system settings | Platform-specific instructions, open system settings action, choose keyboard action, test field | Not enabled, enabled/not selected, selected, system action cancelled |
| S19 | Account | S01 → Account | Optional registration/sign-in and online features | Sign-up/sign-in, connection state, optional cloud state, sign-out | Offline, connected, invalid credentials, loading, error |
| S20 | Help & Feedback | S01 → Help & Feedback | Troubleshooting and help | Setup steps, common problems, feedback entry | Offline/help available, feedback unavailable |
| S21 | About | S01 → About YORY KEYS | Product/version details | App name, version, legal links | Current build, update available |

The dedicated theme detail, setup and toolbar-customizer screens are inferred from visible entry points and public help documentation; the theme detail route remains unimplemented in YORY. The theme gallery/editor are now partially implemented natively. Manual reference tap counts and physical-device validation remain outstanding.

## Flows

```
F01 Enable and test YORY KEYS
    S18 -> Android system keyboard settings -> choose YORY KEYS -> S15
    iOS path: S18 -> iOS Settings > General > Keyboard > Keyboards -> add YORY KEYS -> S15
    Edge: permission denied, user exits Settings, keyboard disabled, host app blocks third-party keyboards.

F02 Browse and apply a theme
    S01 -> S02 -> Apply local preset/custom theme -> S15
    Edge: offline, empty category, preset already selected, apply while keyboard extension is not running.
    Remaining: a dedicated S03 theme detail/preview route is not yet built.

F03 Create a custom theme
    S01 -> S02 -> S04 -> preview English/Arabic -> save -> S15
    Edge: invalid HEX, reset, process recreation, theme schema migration, imported malformed file.

F04 Change language/layout
    S01 -> S05 -> S06 -> apply -> S15
    Edge: switch while text entry is active, unsupported language, RTL mixed with numbers, process restart.

F05 Configure keyboard structure
    S01 -> S11 -> change options -> S15
    Edge: long-press minimum/maximum, compact screen, conflicting number-row and numeric-field states.

F06 Resize keyboard (Android)
    S01 -> S11 -> Resize -> adjust 36–60dp slider or choose Compact/Standard/Tall -> reset/save -> S15
    Current implementation changes key height and writes the same value read by InputMethodService.
    Remaining: free drag handles, width adjustment and repositioning are not yet implemented.
    iOS: key height can be changed in Theme Studio; do not show a false free-reposition feature.

F07 Manage clipboard (Android)
    S15 -> S16 -> Clipboard -> insert/pin/delete/undo/clear -> return to text field
    Edge: sensitive field, one-time code, empty clipboard, expiry, listener disabled, app process restart.

F08 Tune sound/haptics
    S01 -> S13 -> change settings -> S15
    Edge: system vibration disabled, sound volume zero, unsupported haptic device.

F09 Offline-first use
    S19 -> Continue Offline -> S01 -> change local settings -> S15
    Edge: no network, expired account session, reconnect later. Typing must never wait for network.

F10 Optional cloud account
    S01 -> S19 -> register/sign in -> connected state
    Edge: no network, invalid credentials, cancellation, expired session, sign-out; local settings remain available.
```

The exact tap counts for theme creation, account screens and OS setup need a recorded manual pass. Do not claim a benchmark until counted on both platforms.

## Components

| Component | Variants | States | Used on |
|---|---|---|---|
| Settings row | icon + title/subtitle + chevron; compact row | default, pressed, disabled, unavailable, selected | S01 |
| Detail app bar | back + title + optional trailing action | default, RTL, long title | S02–S14, S19–S21 |
| Category chip | selected / unselected | default, pressed, focus, disabled | S02 |
| Theme card | owned, preset, downloadable, selected | preview, selected, offline, loading, unavailable | S02–S04 |
| Keyboard preview | EN QWERTY, AR RTL, numeric, symbol | current, draft, disabled | S03–S04 |
| Toggle row | title + helper + switch | on, off, disabled, saving | S07–S09, S11, S13 |
| Value slider | label, slider, live value | min, mid, max, disabled, editing | S04, S11–S13 |
| Segmented option | two/three option group | selected, unselected, disabled | S04, S11, S13 |
| Color input | swatch, HEX field, picker | valid, invalid, focused, disabled | S04 |
| Keyboard toolbar button | icon + accessibility label | default, pressed, active, unavailable | S15–S17 |
| Suggestion chip | word / emoji / tool action | available, selected, unavailable, hidden | S15 |
| Keyboard key | letter, modifier, space, return, delete | default, pressed, long-press, disabled | S15 |
| Floating keyboard action | test/open keyboard picker | default, pressed, unavailable | S01, S18 |
| Info/privacy card | title + concise explanation | default, link, warning | S07–S10, S14 |
| Confirmation dialog | confirm/cancel | open, confirming, complete | S04, S10, S14 |
| Snackbar/banner | success, error, offline | transient, dismissible | all mutation flows |

## Inferred data model

```
StudioPreference
  key, value, platform, updatedAt
  evidence: S07-S14 settings controls and current YORY local settings
  confidence: high

LanguageLayout
  tag, nativeName, displayName, layoutId, direction, isInstalled, isSelected, capabilityStatus
  evidence: S05-S06 language list, current YORY settings and keyboard source
  confidence: high

Theme
  id, name, ownerScope(local|cloud), schemaVersion, document, previewRef, isSelected,
  createdAt, updatedAt, capabilityFlags
  evidence: S02-S04 gallery/editor and shared YORY theme schema
  confidence: high

ToolbarConfiguration
  platform, orderedToolIds, hiddenToolIds, updatedAt
  evidence: S15-S17 toolbar in screenshot and official toolbar help
  confidence: medium

ClipboardEntry
  id, text, createdAt, expiresAt, isPinned, classification
  evidence: official clipboard guide; Android-only local capability for YORY v1
  confidence: medium

KeyboardProfile
  platform, height, widthMode, offset, oneHandedSide, floatingPosition, keySpacing, keyShape
  evidence: S11-S12 official Android resize behavior and YORY theme fields
  confidence: medium; current YORY code only has part of this

AccountSession
  userId, provider, accessTokenRef, expiresAt, syncEnabled
  evidence: S19 and optional account source
  confidence: medium
```

Relationships:
- StudioPreference belongs to one local installation/profile.
- LanguageLayout is referenced by the active KeyboardProfile.
- Theme belongs to local storage by default; optional cloud publishing is a separate feature and never required for typing.
- ClipboardEntry is local and Android-only in the initial YORY implementation.
- AccountSession is optional; it must not own or gate local preferences.

## Feature matrix

See features.csv. Priorities are set for the YORY product, not for Microsoft's business model. Existing YORY implementation states are recorded in clone; "partial" means a source slice exists but is incomplete or not device-verified.

## Out of scope / clean-room exclusions

- Microsoft SwiftKey source code, private APIs, reverse-engineered network endpoints, binaries or internal implementation.
- Microsoft, Copilot, Rewards or SwiftKey branding, logos, icon artwork, illustrations, theme images, proprietary copy, or licensed fonts.
- Microsoft Rewards, Microsoft account infrastructure, Copilot Search, Microsoft Translator service, Klipy licensing/partnerships, marketplace inventory and user community.
- The original app's user accounts, data, service backend or partner network.
- Feature claims that YORY does not yet implement. Similar behavior may be implemented only with YORY-owned UI, original copy and official/public APIs.

## Size

22 screens, 10 principal flows, 6 inferred local entities/models. Size: XL for full parity across Android and iOS. Hard parts: native keyboard lifecycle and input traits, live theme preview/renderer parity, keyboard resizing and mode geometry, per-platform toolbar/clipboard capabilities, and edge-to-edge/safe-area/device QA. Recommended next sequence: architecture → design tokens/components → app shell and gallery → keyboard toolbar/resize → platform-specific tools → device testing.
