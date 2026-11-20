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

Status: **implemented in source; latest iOS CI still running at this update**.

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
