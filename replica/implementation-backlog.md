# Implementation backlog — YORY KEYS Replica

## Phase 0 — Evidence and correctness
- [x] Register user-provided SwiftKey screenshots without copying image assets into the product.
- [x] Inspect current Android/iOS settings navigation and keyboard toolbar source.
- [x] Verify Android resize route and iOS resize limitation against public help docs.
- [x] Correct the earlier inaccurate proposal for four persistent bottom tabs.
- [ ] Record exact per-screen tap counts through a manual pass of the reference app, if the user supplies or operates it.

## Phase 1 — Native Studio shell
- [ ] Build stable route IDs and preserve root scroll position.
- [ ] Group settings categories and add actual state indicators.
- [x] Add platform-aware keyboard quick actions above safe areas (Android input-method picker; iOS setup guidance).
- [x] Implement Android edge-to-edge and safe drawing insets for settings/keyboard UI; physical-device confirmation remains open.

## Phase 2 — Themes
- [x] Build native category filters and local theme gallery on Android and iOS.
- [x] Add local Apply state and active-theme indicator for presets/custom saved themes.
- [x] Add named local custom-theme saving into My Themes (Android SharedPreferences store; iOS App Group UserDefaults).
- [x] Build dedicated theme detail/preview screens on Android and iOS, separate from gallery cards.
- [ ] Upgrade theme editor with grouped controls and an unsaved-change/discard flow.
- [x] Add gallery detail controls for duplicate/favorite/delete/rename; protect built-in presets from destructive actions.
- [ ] Make the gallery preview use the same renderer/component as the live keyboard.

## Phase 3 — Languages and layout
- [ ] Distinguish keyboard layout availability from language-model availability.
- [ ] Maintain English (US) and Arabic (Egypt) as supported v1.
- [ ] Mark German QWERTZ as basic layout only until its language resources pass QA.
- [x] Add a dedicated Android keyboard-height Resize screen with live preview, size slider, Compact/Standard/Tall presets and Reset.
- [ ] Implement free drag handles, width changes and keyboard positioning on Android; the new screen currently adjusts key height only.
- [ ] Keep iOS sizing truthful: key height is available in Theme Studio, but do not expose SwiftKey-style floating/reposition controls that do not exist.

## Phase 4 — Tools and per-platform behavior
- [x] Define platform-specific toolbar registries and persist visibility/order locally.
- [x] Android: clipboard history UI with pin/delete/undo/clear, encrypted local storage and sensitivity filters.
- [x] Android/iOS: add a session-scoped Incognito mode that pauses local learning and predictions; suppress clipboard capture on Android.
- [x] iOS: show only supported host-pasteboard/keyboard actions; clipboard history is explicitly labelled as unavailable in the current iOS keyboard build.
- [x] Add compact suggestion mode across Android and iOS; it reduces the suggestion bar to two entries and a shorter row.
- [ ] Add voice input only after a real permission and system/API flow exists; voice typing is not connected in this build.
- [ ] Keep online tools optional and outside the live typing path.

## Phase 5 — QA and release gates
- [ ] Test 320/360/390/430dp widths and tablet/foldable.
- [ ] Test Arabic RTL, mixed numbers, long strings and Dynamic Type.
- [ ] Test Android 3-button and gesture navigation.
- [ ] Test iOS home indicator/Dynamic Island and keyboard extension available height.
- [ ] Test airplane mode, process recreation, settings persistence and theme renderer parity.
- [ ] Physical-device QA before production-ready labels.


## Milestone T2 — Theme library and toolbar management

Status: **implemented in source; Android and contract checks passed before the emoji-picker commit; iOS build for the current emoji-picker commit is pending**.

- [x] Android and iOS theme detail/preview screens.
- [x] Favorites, rename, duplicate, delete and protected built-in presets.
- [x] Android and iOS toolbar visibility/order screens and persisted settings.
- [x] Android keyboard toolbar consumes saved order/visibility; iOS keyboard extension reads the same App Group preferences on appearance.
- [x] Android clipboard history is encrypted with an AES-GCM key in Android Keystore, bounded to 50 items, rejects common OTP/PIN/payment-card patterns, and expires unpinned clips after 24 hours.
- [x] Android clipboard manager includes re-copy, pin/unpin, individual delete, undo for the last deletion, and clear-all confirmation.
- [x] Incognito mode disables local predictions/learning; Android also pauses clipboard history capture.
- [ ] iOS keyboard clipboard history remains unavailable in this build. The iOS keyboard can only use supported host-pasteboard actions; do not advertise background clipboard capture.
- [ ] Voice typing is not connected on either platform.
- [ ] Gallery preview rendering is still separate from the live keyboard renderer.
- [ ] Physical-device QA, accessibility/RTL QA, and release signing still remain.


## Phase 6 — Emoji picker
- [x] Replace the fixed emoji strip with category browsing on Android and iOS (Recent, Smile, People, Nature, Food, Travel, Symbols).
- [x] Store up to 24 recently used emoji locally and insert through the active input connection.
- [x] Add a starter skin-tone selector for supported basic people/hand emoji.
- [ ] Add searchable localized emoji catalogue and expand Unicode compound/ZWJ coverage (favorites are implemented locally).
- [ ] Verify the picker height and scroll behavior on small devices and iPad/landscape.


## Validation refresh — 2026-11-20 23:45 EET

Latest verified main commit: `7374e805ea6b7a1944dbe241cf68c62f96098624`.

- [x] Android CI completed successfully (run `44771343560`).
- [x] iOS CI completed successfully (run `44771343591`).
- [x] Repository contract validation completed successfully (run `44771343573`).
- [x] Latest Android test job includes the clipboard policy and emoji catalog JVM test suites.
- [ ] Physical-device QA and reference screenshot comparison remain release gates; CI success does not establish visual parity or real-device behavior.


## Phase 7 — Keyboard reference parity (2026-11-20)

Detailed evidence and per-platform findings: `keyboard-reference-audit.md`.

### P0 — key inventory and input correctness
- [x] Add the missing standard Arabic letters to the Android/iOS primary rows; retain extended hamza/alif/ta-marbuta/maqṣūra forms through discoverable alternatives. Device typing/RTL QA remains.
- [x] Add a layout unit test for all standard Arabic letters and the base extended forms; manual verification of long-press alternatives remains.
- [x] Add secondary digit legends to Arabic first-row keys on both platforms; screen-reader and physical tap QA remains.
- [x] Add two symbol pages with matched ordered inventories on Android/iOS and ABC/page transitions; move the inventory into a shared data contract and complete insertion QA next.
- [ ] Verify Backspace, Shift, language switch, comma, space, period and editor-specific Enter remain present and functional on every page.

### P1 — toolbar and suggestion experience
- [x] Add native More-tools grids on Android and iOS with platform-available actions. A shared capability registry and persistence/reorder UI still need consolidation.
- [ ] Use original YORY icons, labels, accessibility descriptions, stable tool IDs and persistent order/visibility. Never expose Microsoft Rewards, Microsoft account or proprietary branded assets as YORY features.
- [ ] Add editor-aware contextual chips; [x] suggestion-row collapse/expand is now implemented while preserving restricted-field/Incognito suppression.
- [x] Add spacebar swipe: left = next added language, right = previous added language. Gesture/RTL hit-target/focus tests remain.

### P2 — optional rich input (only with complete real integrations)
- [ ] Speech-to-text via approved platform APIs, with a user-initiated permission explanation and cancellation/error handling.
- [ ] Searchable local emoji/symbol catalogue with localization and accessibility.
- [ ] Explicit image/screenshot insertion using supported user-driven content commit/paste behavior; no background image harvesting.
- [ ] GIF/sticker browsing only after content source, licensing, moderation, privacy and offline/online behavior are defined.

### Platform-specific release gates
- [ ] Android: distinguish current key-height adjustment from future free resize/width/position controls; validate with gesture and 3-button navigation.
- [ ] iOS: do not expose fake free-resize or background clipboard history; rely only on supported extension sizing and explicit host-pasteboard interactions.
- [ ] Run Arabic, both symbol pages, More grid, suggestion collapse and contextual-chip screenshot comparisons at 320/360/390/430dp-equivalent widths on both platforms.
- [ ] Complete TalkBack/VoiceOver, RTL/mixed-bidi, process recreation, keyboard-extension lifecycle and physical-device QA.

No runtime keyboard source files were changed by this reference-audit pass; this phase records the verified gaps and acceptance criteria before the code changes.


## Implementation update — added-language swipe and runtime parity

- Android and iOS settings now distinguish an active language from languages added to the keyboard.
- Only added layouts participate in the spacebar swipe cycle. Selecting a language in Settings automatically adds it; the Add/Added control can manage the cycle membership.
- Spacebar tap remains a normal space insertion; a horizontal swipe changes language without inserting text.
- Android and iOS now have two symbol pages, expanded Arabic key rows, secondary Arabic digit legends, a collapsible suggestion row, and a platform-appropriate More grid.
- Exact dimensions and responsive targets are documented in `design/keyboard-measurements.md`.
- The first CI pass caught an Android callback-overload issue and an outdated unknown-language unit expectation; both were corrected in source. Re-run CI on the latest commit before marking this work validated.
- No physical-device QA has been performed by this change set; gesture accuracy, Arabic typing, system navigation and extension height remain release gates.
