# YORY KEYS — Keyboard layout measurements
Date: 2026-11-20
Scope: Android IME and iOS keyboard extension; native implementation only.

This is the measurement contract for a responsive keyboard inspired by the interaction patterns in the user's screenshots. Use YORY's own visual identity, glass surface, icons and labels. Dimensions are starting targets, not pixel-perfect constants: the renderer must adapt to actual safe width, key count, font scale, theme and host-provided keyboard height.

## 1. Layout breakpoints

| Available width | Profile | Rules |
|---|---|---|
| 320–359 dp/pt | Compact | 4–6 dp side padding; compact key typography; no extra optional row unless enabled; keep utility controls visible |
| 360–389 dp/pt | Baseline | Default phone geometry; standard key spacing and suggestion row |
| 390–430 dp/pt | Wide phone | Preserve the same row structure; distribute extra width to keys and spacebar; do not enlarge toolbar glyphs excessively |
| 431–599 dp/pt | Large phone / small tablet | Bound content width where appropriate; preserve comfortable key widths |
| 600+ dp/pt | Tablet / foldable | Use adaptive width and actual extension/window limits; never stretch every key across the full screen |

Android values are dp; iOS values are points. Keep the visual ratios consistent, but use the platform's native measurement unit and extension limits.

## 2. Runtime keyboard geometry

| Element | Baseline target | Compact target | Notes |
|---|---:|---:|---|
| Keyboard horizontal inset | 6 dp/pt | 4–5 dp/pt | Must include theme inset and avoid display cutouts |
| Top toolbar height | 38–42 dp/pt | 36–38 dp/pt | Seven slots only if available width permits; icon visual size 20–23 |
| Suggestion row | 30–34 dp/pt | 22–25 dp/pt | Three suggestions normally, two in compact mode, one collapse affordance |
| Collapsed suggestion strip | 18–20 dp/pt | 18 dp/pt | Must not keep hidden rows reserving old height |
| Standard letter key height | 44–48 dp/pt | 40–44 dp/pt | Theme may vary within existing supported height bounds |
| Row vertical gap | 4–6 dp/pt | 3–4 dp/pt | Recalculate total IME height from visible rows |
| Standard horizontal key gap | 3–5 dp/pt | 2–3 dp/pt | Avoid reducing spacing below safe visual separation |
| Dense Arabic key gap | 1.5–2.5 dp/pt | 1–2 dp/pt | Required for the 12/11/10 key rows; compensate with clear key outlines |
| Arabic secondary digit legend | 8–9 sp/pt | 7.5–8 sp/pt | Visual hint only; accessibility label must announce the primary character, not the hint |
| Primary key label | 17–20 sp/pt | 14–16 sp/pt | Adapt down gently for dense Arabic rows |
| Special modifier width | 1.15–1.3 key units | 1.15 key units | Shift and Backspace stay at opposite ends of the final letter row |
| Spacebar width | 36–40% of bottom row | 34–38% | Keep a stable hit target; swipe gesture must not intercept a tap |
| Spacebar height | 44–48 dp/pt | 40–44 dp/pt | Label uses current active language |
| Bottom action keys | 34–44 dp/pt | 32–40 dp/pt | Number/symbol, globe, comma, period, Enter remain discoverable |
| Utility touch target | 44–48 dp/pt minimum | 44 dp/pt target | Toolbar/tools targets must not shrink with dense letter rows |

Dense rows are a keyboard-specific exception to the normal utility touch-target target: on a 320–360dp viewport the 12-key Arabic first row necessarily has narrower visual caps. Keep the key centers separated, preserve clear borders and test adjacent-key accuracy on real devices.

## 3. Arabic layout profile

- Primary rows: 12 / 11 / 10 letter slots before modifiers.
- First row includes a secondary digit hint aligned consistently near the top edge of each key.
- The second and third rows use the same compact height and baseline; Shift and Backspace widen the outer ends of the third row.
- The spacebar and toolbar follow RTL semantics; punctuation and numeric rows remain LTR where appropriate.
- All 28 standard Arabic letters must be inserted directly from the primary layout.
- Extended alif/hamza forms remain reachable through a visible/discoverable long-press alternative on their related base key. Do not treat a visual digit hint as an inserted character.

## 4. Symbol pages

- Page 1: digits and frequently used punctuation/operators/brackets.
- Page 2: currency, mathematical, quotation and Arabic punctuation alternatives.
- ABC/!#1 control returns to letters; page control changes only the symbol page.
- Backspace, comma, period, space and editor-specific action remain available on the bottom row.
- Both platforms use the same ordered symbol inventory, even though the renderers remain native.

## 5. More-tools grid

- Four columns at phone widths; tile width is calculated from available width after outer insets and 6–8 dp gaps.
- Target tile height: 64–72 dp/pt; icon 20–24; label 10–12.
- The panel should fit two to three rows without overlapping the host editor or system navigation area.
- Render only capabilities that actually work on the active platform and in the current editor type.
- Required navigation/switch actions must remain available even if optional toolbar entries are hidden.

## 6. Spacebar language swipe

- The visible spacebar label shows subtle left/right chevrons around the active language name to signal the gesture.
- Tap inserts exactly one space and retains normal learning/suggestion behavior.
- Swipe left advances to the next added language.
- Swipe right selects the previous added language.
- Only languages explicitly marked Added in Studio participate; inactive installed layouts are not silently inserted into the cycle.
- One added language means swipes leave the active language unchanged.
- An unknown stored language tag falls back to the first supported/added layout.
- A successful swipe refreshes key rows, RTL direction, spacebar label, suggestion language and active toolbar state without inserting a character.
- Gesture recognition must respect the platform touch slop and allow a normal tap to reach the space action.
- Screen reader copy must describe the directional action and provide another non-gesture language-switch route.

## 7. Height and navigation

- Compute visible keyboard height from the actual toolbar, suggestion, tool/emoji panel and key rows; do not reserve hidden-row height.
- Keep the keyboard extension within the platform-provided height budget.
- Android content must be tested in both 3-button and gesture navigation; its IME is system-managed and must not draw an app-owned fake bottom bar.
- iOS must respect extension height and next-keyboard switching behavior; do not present unsupported floating/reposition controls.
- Test portrait, landscape, 320/360/390/430 widths, font scaling, Arabic RTL, mixed text and iPad/large-window states.

## 8. Visual quality gates

- Key outlines and backgrounds remain distinct in dark themes; no text/legend collides with the primary glyph.
- Text contrast target at least 4.5:1 for normal labels; essential UI boundaries at least 3:1.
- Toolbar and More actions have meaningful TalkBack/VoiceOver labels.
- Key touch feedback does not cause cumulative geometry shifts.
- Reference screenshot diffs should be made at matching device width, density, language, editor type, theme and keyboard height.
- Automated builds do not replace adjacent-key, spacebar swipe, gesture-nav or screen-reader testing on physical hardware.


### Spacebar label typography
- Treat the language-labelled spacebar as a utility key rather than a letter key. Use the compact utility font scale (roughly 10–13sp/pt depending on display width) so left/right chevrons and longer names such as Português stay inside the 36–40% spacebar.
- Keep a minimum visual gap between chevrons and text; the spoken label remains “Space, [active language]” with a separate hint describing swipe-left/right behavior.
