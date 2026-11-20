# YORY Studio component and interaction specification

The source of truth for values is design/tokens.json. Use native components in Jetpack Compose and SwiftUI/UIKit; do not add a web/Tailwind layer to this native app.

## Navigation model

- Root route is a vertically scrolling Settings list.
- Selecting a row pushes a detail route. The detail route shows a native back affordance and a concise title.
- Themes is a dedicated gallery flow: category filters → owned/preset cards → preview/details → apply or customize.
- A keyboard quick-action may be anchored near the lower-right of S01/S18, but it must be above navigation-bar/home-indicator insets and cannot cover content.
- No persistent YORY bottom tab bar in v1. The screenshots and public SwiftKey settings flow show hierarchical navigation.
- OS back gesture/button and app's top back control must return to the prior route without losing unsaved edits unless the user confirms discard.

## Components

### SettingsRow
- Variants: standard, compact, with status, unavailable.
- Layout: 40–44dp icon zone; title; optional one/two-line subtitle; trailing chevron or state.
- Minimum interactive height: 64dp standard; never below 48dp.
- Full row is one accessible target. Avoid placing a second small clickable icon inside the row unless it has its own distinct hit target.

### DetailTopBar
- Back action + title + optional trailing action (Create, Edit, Add).
- Keep title truncation graceful on 320dp widths and large text.
- Mirror directional back affordance for RTL, not arbitrary decorative icons.

### ThemeCategoryChip
- Horizontal, scrollable category row; selected/unselected variants.
- Selection does not cause the page title to jump. Preserve selected category during card detail return.
- Categories are YORY-owned (e.g. All, Yours, Dark, Light, Colorful), not copied Microsoft category labels or copyrighted theme catalogs.

### ThemeCard
- Shows YORY-owned keyboard preview, name, ownership/status and selection state.
- Card preview keeps identical aspect ratio and never stretches a keyboard screenshot.
- Apply/download affordances are capability-aware: local preset apply works offline; cloud fetch is not represented as available until implemented.

### KeyboardPreview
- Reusable live renderer, not a flat screenshot. Supports English QWERTY, Arabic RTL, symbols and number row.
- In editor mode, render draft theme values. In applied mode, render current saved theme.
- Preview state must match actual keyboard renderer for key height, spacing, shape and text size.

### ToggleRow
- Leading title, supporting text, trailing platform-native switch.
- Entire row may be clickable, but avoid nested double-toggle behavior.
- Persist local change immediately; show a clear unavailable state if a platform cannot support the option.

### SliderRow
- Label + current value + slider.
- Value updates continuously in the editor; persist at controlled checkpoints to avoid disk writes for every pointer frame.
- Provide minimum/maximum labels when useful and never rely on color alone.

### ColorField
- Swatch + editable HEX + native color picker.
- Validate input before changing the applied theme. Show inline field error; do not silently reset to an arbitrary color.

### SegmentedControl
- Use for a small exclusive set such as Top/Bottom or Classic/Soft.
- On small screens it may wrap or stack; never clip a segment.

### KeyboardToolbar
- Runtime-only toolbar; separate from Studio app navigation.
- Every button has an accessibility label and platform capability state.
- Android v1: emoji, language, symbols, Studio/settings, local clipboard.
- iOS v1: emoji, language, symbols, next keyboard, more tools; do not show Android clipboard history or unsupported features.
- Expand/collapse transitions preserve focus and never insert text accidentally.

### SuggestionRow
- Three suggestion slots in the current code; support hidden/empty/sensitive-field states.
- Never render personal suggestions in password, numeric PIN, email or URL fields.
- Use local suggestions only. A loading state must not block key input.

### KeyboardKey
- Variants: letter, modifier, space, delete, return, emoji, numeric.
- States: normal, pressed, long-press popup, disabled.
- Touch target geometry is based on available width and row weights; key labels scale within a bounded range.
- Return key label reflects host editor traits. RTL changes reading order without reversing semantic actions incorrectly.

### KeyboardQuickAction
- Opens Android InputMethodPicker where supported.
- iOS uses a setup/help action; do not pretend iOS exposes the same Android picker API.
- Anchor above the OS bottom safe area; hide/relocate when the available viewport is too small.

### InfoCard / PrivacyCard
- Concise, plain-language message and optional action.
- Distinguish on-device features from online/account features.
- Never claim a permission prompt or system setting was opened unless the platform API actually did it.

### Dialog / Snackbar
- Confirm destructive operations (clear clipboard/dictionary, discard theme).
- Announce results to screen readers. Undo only where the data operation supports a safe undo.

## Screen behavior requirements

### S01 Settings home
- Keep the 13 categories grouped and scannable. Account, Languages, Themes, Typing, Emoji, Rich input, Layout & keys, Sound & vibration and Privacy are primary.
- YORY Rewards/Achievements remain hidden or clearly disabled until their data model and behavior exist.
- The quick-action must not look or behave like system navigation.

### S02 Themes
- Show category chips and a scrollable grid/list that reflows across widths.
- Keep previews large enough to identify key geometry and contrast.
- Provide owned/local state and clear empty/offline states.

### S04 Theme editor
- Keyboard preview first, controls below in scrollable groups.
- Compact mode uses single-column controls. Large width may use preview + controls in two panes.
- Show unsaved changes and a clear Apply/Save action.
- Use the exact same ThemeDocument contract used by the keyboard renderer.

### S05 Languages
- Show native language name and selected layout.
- Distinguish a basic keyboard layout from a complete language dictionary/model pack.
- Do not imply German or any other language has full predictions unless the pack is installed and quality-certified.

### S11 Layout & keys
- Group size, key rows, long-press behavior and editor geometry.
- Keyboard height is only part of resizing. Android sizing must include width/position mode where supported; iOS must use its actual extension sizing constraints and must not fake unsupported system controls.

### S15 Runtime keyboard
- Rebuild layout for secure, numeric, email, URL, phone and search text traits.
- On Arabic use RTL layout and correct punctuation/spacebar semantics.
- Preserve editor selection, cursor movement, delete repetition and return action.

## Responsive rules

- 320–359dp: compact; one-column screens, scrollable category chips, no two-column settings forms, controls stretch within safe margins.
- 360–389dp: baseline phone layout.
- 390–430dp: more breathing room; gallery can use two columns only if previews remain legible.
- 600dp+: tablet/foldable adaptive split view or bounded max-width content; do not stretch preference rows across the full canvas.
- Landscape: keyboard height and Studio content adapt independently; no fixed vertical assumptions.
- Arabic RTL: mirror directional layout and text alignment; do not mirror numeric values, sliders or non-directional glyphs.

## Safe-area and system navigation

### Android
- Treat status bar, display cutout, navigation bar/gesture handle and on-screen IME as WindowInsets, not as app widgets.
- Android 15+ edge-to-edge is enforced for apps targeting SDK 35. Apply insets exactly once; avoid a second manual bottom padding if Scaffold already consumes them.
- In 3-button mode, keep interactive content above the Back/Home/Recents navigation area.
- In gesture mode, preserve a transparent gesture area and keep the bottom action/tab area clear of the system gesture handle.
- On keyboard appearance, use IME insets/adjustment so focused fields and bottom actions remain visible.
- Test predictive Back and system back dispatch.

### iOS
- Respect UIView.safeAreaInsets/safeAreaLayoutGuide and SwiftUI safe-area behavior.
- Keep app controls above the home indicator and below the status bar/Dynamic Island.
- The keyboard extension is not the same safe area as the containing app; use its actual available height and host editor traits.
- Test next-keyboard switching, secure-field fallback, iPad docked/floating keyboard layouts and extension relaunch.

## Accessibility and quality gates

- Text/background AA contrast target: at least 4.5:1 for normal text; large text and important UI boundaries at least 3:1.
- Dynamic Type / font scaling without truncating key actions.
- VoiceOver/TalkBack labels for icon-only toolbar controls.
- 48dp Android touch targets and 44pt iOS targets as the product target.
- Reduce Motion and reduced-effects theme option.
- No text conveyed by color alone; visible focus/selected state; minimum 1-line title and 2-line subtitle support.
- Verify Arabic screen reader order and focus direction.
