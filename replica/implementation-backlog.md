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
- [ ] Add a platform-aware keyboard quick action above safe areas.
- [ ] Implement edge-to-edge and IME insets without double padding.

## Phase 2 — Themes
- [x] Build native category filters and local theme gallery on Android and iOS.
- [x] Add local Apply state and active-theme indicator for presets/custom saved themes.
- [x] Add named local custom-theme saving into My Themes (Android SharedPreferences store; iOS App Group UserDefaults).
- [ ] Build a dedicated theme detail/preview screen separate from the gallery card.
- [ ] Upgrade theme editor with grouped controls and an unsaved-change/discard flow.
- [ ] Add gallery UI for duplicate/favorite/delete/reset (store delete API alone is not a finished UI).
- [ ] Make the gallery preview use the same renderer/component as the live keyboard.

## Phase 3 — Languages and layout
- [ ] Distinguish keyboard layout availability from language-model availability.
- [ ] Maintain English (US) and Arabic (Egypt) as supported v1.
- [ ] Mark German QWERTZ as basic layout only until its language resources pass QA.
- [ ] Complete keyboard geometry editor.
- [ ] Android: implement size and position modes with reset.
- [ ] iOS: do not expose a false resize feature.

## Phase 4 — Tools and per-platform behavior
- [ ] Define toolbar registry per platform.
- [ ] Android: clipboard history UI, pin/delete/undo/clear, sensitivity filters.
- [ ] iOS: show only supported host-pasteboard/keyboard actions.
- [ ] Add voice input only after a real permission and system/API flow exists.
- [ ] Keep online tools optional and outside the live typing path.

## Phase 5 — QA and release gates
- [ ] Test 320/360/390/430dp widths and tablet/foldable.
- [ ] Test Arabic RTL, mixed numbers, long strings and Dynamic Type.
- [ ] Test Android 3-button and gesture navigation.
- [ ] Test iOS home indicator/Dynamic Island and keyboard extension available height.
- [ ] Test airplane mode, process recreation, settings persistence and theme renderer parity.
- [ ] Physical-device QA before production-ready labels.
