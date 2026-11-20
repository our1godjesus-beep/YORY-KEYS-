# Replica build log — YORY KEYS

Date: 2026-11-20
Repository: https://github.com/our1godjesus-beep/YORY-KEYS-
Current branch: main

## Milestone S02 — Theme gallery and local My Themes

Status: **partial implementation; automated Android build passed; iOS verification pending**.

### Delivered source
- Android native Compose theme gallery: `YoryThemeGalleryScreen.kt`.
- Android hierarchical route from Studio Settings → Themes → Gallery → existing editor.
- Android local custom-theme store: `YoryThemeLibraryStore.kt`, encoding named theme documents with the shared `.yorytheme` codec.
- Android editor adds a theme-name field and Save to My Themes action.
- iOS native SwiftUI `YoryThemeGalleryView` with category chips, generated keyboard previews, selected state, Apply, Create, Customize and empty states.
- iOS theme editor adds named local save using the shared App Group preferences and existing `.yorytheme` JSON representation.
- Gallery supports All, Yours, Dark, Light and Colorful categories. Local themes and presets are available offline.
- The theme editor remains the pre-existing editor; this milestone adds gallery/navigation and saved-theme flows rather than claiming a complete redesign of every setting.

### Automated validation evidence
- Android CI passed on commit `4e3c2e860b9de0eddc83f260f62a2fd8c6250cfd`: Assemble and test + debug APK artifact upload succeeded (run 44760049272).
- Repository contract validation passed on commit `4e3c2e860b9de0eddc83f260f62a2fd8c6250cfd` (run 44760049196).
- iOS had successive compiler failures during this iteration:
  1. Cross-file private background helper access; replaced with a local gallery gradient.
  2. Complex SwiftUI expression in saved-theme decoding; split into a dedicated decode helper.
  3. Complex SwiftUI category-chip expression; extracted into `categoryChip(_:) `.
- The latest iOS build after the category-chip fix is run 44760341584 on commit `982ca562b4787a0fcb29f87ebc84c4b5f7b91a47`. At the time of writing, that run has not yet been confirmed complete. Do not mark iOS build as passed until its result is success.
- The latest Android-only change after its passing commit preserves a previously entered custom theme name when reopening the editor (commit `4e3c2e860b9de0eddc83f260f62a2fd8c6250cfd`).

### Known gaps
- Gallery preview cards are generated from the theme settings but are not yet a shared live renderer component with the keyboard itself.
- Dedicated theme detail/large preview screen is not implemented; a card can apply or launch Customize.
- Duplicate, favorite, delete and reorder controls are not yet exposed in the gallery UI.
- No physical Android phone or iPhone QA was performed during this milestone.
- No visual screenshot-diff was run against the reference app.
- Android 3-button/gesture safe-area behavior, iOS home-indicator/Dynamic Island, rotation, tablet/foldable and Dynamic Type still require device testing.
- Full theme editor unsaved-change/discard flow and library round-trip tests remain.

### Next build order
1. Confirm iOS CI after the SwiftUI simplifications.
2. Test gallery apply/save/reopen flows on Android and iOS.
3. Add a theme detail page with a larger preview.
4. Add custom theme delete/duplicate/favorite UI.
5. Unify preview and runtime keyboard rendering.
6. Continue with S01 navigation polish and S11/S12 keyboard geometry/Android resize behavior.

## Milestone S12 — Android keyboard height resize

Status: **partial implementation; CI run pending at time of this update**.

### Delivered source
- New `YoryKeyboardResizeScreen.kt` with a keyboard-key preview based on current theme colors and geometry.
- The 36–60dp slider updates the preview as the value changes and persists `keyHeightDp` after the slider interaction completes.
- Compact (40dp), Standard (46dp), Tall (54dp) and Reset (46dp) shortcuts.
- Layout & keys now opens a dedicated Resize screen with its own back action.
- The InputMethodService already reads `keyHeightDp` for its actual key-row heights; the new screen writes to that same ThemeStore field.

### Known limitations
- This is key-height resizing, not free drag handles, keyboard width resizing or floating-position adjustment.
- The preview is illustrative and still needs comparison against the actual IME renderer on physical devices.
- iOS has a key-height value in Theme Studio; no SwiftKey-style free-reposition tool is advertised.

### Validation
- Repository contract validation passed on the preceding commit `0008ffdbb849338112fc4e190296bef455728866`.
- Android CI on commit `a04deacf1183775090666ba5bf178aac9fb51f71` was in progress at the time the S12/compact-mode work was recorded. The newer CI run for the current source must be used to close this gate.
- No physical-device resize QA has been performed.

## Milestone RI-C1 — Compact suggestion mode

Status: **source implemented on Android and iOS; automated builds pending**.

- Android Rich input setting writes `compact_mode` to local settings; IME reduces suggestion row height and shows at most two suggestions rather than three.
- iOS Rich input setting persists `yory.input.compactMode` in the App Group; keyboard extension shortens the suggestion row and limits suggestions to two.
- This changes suggestion display only. It does not change inserted text, editor selection or the keyboard input connection.
- UI/physical-device checks remain pending.


## Milestone T3 — Privacy, clipboard and build stabilization (2026-11-20)

- Android clipboard history is encrypted at rest with AES-GCM and Android Keystore; migration from the earlier local JSON preference is included.
- Android clipboard tools include a full Studio manager with pin/unpin, copy again, individual delete, undo-last-delete and clear-all confirmation. Unpinned clips expire after 24 hours; maximum 50 entries.
- Android and iOS both include session-scoped Incognito mode. It pauses local predictions and personal learning; Android also disables clipboard capture while active.
- Toolbar customization includes optional Incognito mode on both platforms and persists ordering/visibility.
- Android edge-to-edge and safe-drawing insets are applied to the Studio screens and keyboard quick action.
- iOS context-aware return-key label support was restored and the iOS build passed on commit aceda3f1ce1cee93c35be218c89c11e22766e546.
- Latest iOS privacy-default/incognito guard commit bf442dc8e7f4966dc1ca1a949856c7a8d6f2e10a is pending CI confirmation at the time of this log update.
- Android CI passed on df746dd31bf38e73542141dbc09e2076d7b98102; repository contracts passed on bf442dc8e7f4966dc1ca1a949856c7a8d6f2e10a.
- Device QA, visual screenshot-diff, iOS clipboard history, voice typing, swipe typing, neural prediction and autocorrect remain open.


## Milestone E1 — Categorized emoji picker (2026-11-20)

- Replaced the fixed eight-emoji strip on Android and iOS with category-based panels: Recent, Smile, People, Nature, Food, Travel and Symbols.
- The most recent 24 chosen emoji are stored in local preferences and inserted into the active text field.
- Added a skin-tone selector for the starter set of simple person/hand emoji.
- Emoji recent history remains local; it is not synced or backed up.
- Emoji text search, favorites and complete Unicode compound sequence coverage remain unimplemented.
- Automated build/contract results are being run on the new source commit; no manual device QA is claimed.
