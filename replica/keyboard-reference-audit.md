# Keyboard Reference Audit — YORY KEYS
Date: 2026-11-20
Reference set: three user-supplied Microsoft SwiftKey keyboard screenshots (Arabic letter keyboard, symbols/number keyboard with a contextual screenshot chip, and expanded More-tools panel).
Compared source: current `main` at `c578915913099f6a46bc9f2845c1dc770e1c8a10`; keyboard implementation inherited from source commit `7374e805ea6b7a1944dbe241cf68c62f96098624`.
Method: inspect visible reference states and compare them with Android `YoryInputMethodService` and iOS `KeyboardViewController`. Rebuild user jobs with YORY-owned visuals and behavior; do not copy Microsoft branding, artwork, private APIs or service integrations.

## Executive finding

YORY already has a working native typing foundation: seven offline letter layouts, local prefix suggestions, language-aware personal learning, emoji categories/favorites, Android encrypted text clipboard history, Incognito guards, configurable toolbar order/visibility, key feedback, long-press alternatives and platform-aware numeric input.

It is not yet at the reference's keyboard-runtime feature depth. The largest gaps are:
1. The current Arabic layout is materially narrower than the Arabic key set visible in the reference and lacks several standard Arabic letters in its base rows.
2. The symbol experience is a single basic page and differs between Android and iOS; it needs a deliberate two-page symbol design and complete punctuation coverage.
3. The reference toolbar has a real overflow-tools grid. Android currently has no equivalent grid; iOS `toolsMode` only places “Clear learned words” and “Done” in the suggestion row.
4. The reference's top suggestion area has a collapse affordance and contextual chips; YORY currently renders up to three local prefix suggestions, without that control/chip system.
5. The reference shows secondary number legends on Arabic letter keys and language arrows in the spacebar; these details are not present in the current YORY keyboard.
6. Voice input, keyboard search, GIF/sticker discovery and image/screenshot insertion are not connected features in YORY. They must not be shown as working until supported APIs, permissions, consent and platform behavior exist.

Automated CI passing is not proof of visual or input parity. All reference screenshot comparisons and physical-device tests remain open.

## 1. Reference anatomy

### A. Persistent top toolbar (screenshots 1–3)
The visible toolbar contains seven icon slots:
- A branded/assistant-like shortcut.
- A sticker/emoji-like shortcut.
- Clipboard.
- Language/translation.
- Microphone.
- Search.
- More (changes to a close control while expanded).

The third screenshot shows the More panel as a separate surface. It includes a “Reorder items” action and a 4-column grid of twelve entries: Sign in, Rewards, GIF, Tips, Stickers, Settings, Languages, Layouts, Incognito, Resize, Themes and Modes.

Interpretation: this is a shortcut surface, not a second navigation bar and not a reason to add app-level bottom tabs. The YORY implementation should have an original branded tool registry, a compact row for pinned tools and a real More grid for available tools. Tool visibility/order should remain persisted and the active editor should stay focused.

### B. Suggestion / contextual-action row (screenshots 1–2)
The Arabic screen shows a circular collapse affordance and three word suggestions. The symbols screen shows an additional contextual “Insert screenshot” chip.

YORY currently:
- Shows at most three local prefix suggestions (two in compact mode).
- Combines local personal-dictionary candidates with the shipped starter dictionary where available.
- Suppresses suggestions in Incognito, sensitive, numeric, email and URL contexts.
- Shows the language label when Android has no suggestions; iOS may leave the row empty.
- Has no collapse affordance or generic contextual-chip registry.
- Does not provide screenshot/image insertion from clipboard history.

Recommended direction: preserve privacy guards and add a compact/collapse state plus a typed suggestion/action-chip model. Contextual chips must be editor-aware, dismissible and accessible. Screenshot/image insertion must use an explicit user action and supported content-commit/paste APIs; never silently harvest image data.

### C. Arabic letter keyboard (screenshot 1)
Visible traits:
- Three alphabet rows with a wider key count than YORY's current Arabic profile.
- Small Latin/Arabic-Indic number hints at the top of the first-row letter keys.
- Shift and Backspace at the outer edges of the third letter row.
- A dedicated bottom row with number/symbol toggle, emoji, punctuation/voice affordance, wide language-labelled spacebar, punctuation and Enter.
- A small collapse/keyboard-system control area below the keys.

Current YORY Arabic row definition is shared across Android and iOS:
- `ضصثقفغعهخح`
- `شسيبلاتنم`
- `ئءؤرىةوز`

That is a 10/9/8 row profile. It does not include these standard Arabic alphabet letters in its base rows: `ج د ذ ك ط ظ`. It also does not offer the full set of hamza/alif variants as dedicated base keys. The current implementation therefore must not be described as a complete Arabic alphabet layout. These letters and variants need to be restored in an ergonomic, tested arrangement, with access to extended forms through long-press where appropriate.

YORY currently has Shift, Backspace, number/symbol toggle, emoji, language switch, comma, language-labelled space, period and Enter. It does not place directional language arrows inside the spacebar, and there is no connected speech action.

### D. Numbers and symbols (screenshot 2)
The reference shows:
- A full 0–9 top number row.
- Two additional symbol rows with operators, paired punctuation, quotation marks, braces/grouping, Arabic question mark and plus.
- Backspace at the right end of the third symbol row.
- Bottom row with a return-to-letters control, language/numeral controls, comma, wide space, period and Enter.
- A visible contextual screenshot insertion chip above the keys.

Current YORY Android symbol rows are:
- `1234567890`
- `@#$%&-*+=`
- `()[]{}` plus Backspace

Current YORY iOS symbol rows are:
- `@#$%&*()-`
- `_+=[]{}?!`
- `<>/\\|;:`

Both currently have one symbol mode and a regular bottom row. They are not visually or behaviorally identical to one another, and neither models the full reference punctuation grouping or a second symbol page. A single basic symbol page is not enough for parity.

Recommended direction: introduce a shared platform-neutral symbol-page specification with page 1/page 2, Arabic punctuation variants, quotes, braces, mathematical operators, currency signs and reliable ABC/123 transitions. The renderers can remain native, but the symbol inventory and insertion actions must come from the same tested specification.

### E. Expanded More-tools panel (screenshot 3)
The reference uses a 4-column grid with icon, label and consistent cell boundaries, plus Reorder items. Its twelve entries are evidence of the kind of tool discovery the user expects, not a requirement to clone each Microsoft service.

Current YORY:
- Android's runtime toolbar displays emoji, language, symbols, settings, clipboard and privacy shortcuts according to stored preferences. It has no More grid.
- iOS has emoji, language, symbols, next-keyboard, tools and privacy slots. Its tools action only swaps the suggestion row for “Clear learned words” and “Done”; it is not the pictured grid.
- A toolbar visibility/order settings flow already exists on both platforms.
- Android clipboard history is text-only in this build; iOS keyboard clipboard history is explicitly unavailable.
- Account setup exists in YORY Studio, but a real end-to-end authenticated session/cloud sync still needs verification.
- Themes and Android Resize screens exist in Studio; the keyboard extensions do not currently expose all of those routes as a unified tool grid.
- GIF search, sticker catalogue, Rewards and voice typing are not shipped functions.

Recommended direction: build a YORY More-tools grid driven by a typed capability registry. Show only tools that have a working action on that platform. Use explicit “Unavailable on this platform” explanations only where the user can reasonably expect the feature; do not create fake working tiles. Microsoft Rewards and Microsoft-specific sign-in/services are excluded. YORY can have its own optional account destination.

## 2. Platform parity matrix

| Capability | Android current | iOS current | Reference comparison / decision |
|---|---|---|---|
| Native letter typing | Implemented | Implemented | Keep; expand test coverage |
| Seven offline layouts | Implemented | Implemented | Keep; distinguish layout from dictionary support |
| Complete Arabic alphabet in base layout | Incomplete | Incomplete | Must fix before claiming Arabic parity |
| Arabic RTL direction | Basic support | Basic support | Validate mixed Arabic/Latin/numbers and cursor |
| Secondary number hints on Arabic keys | Missing | Missing | Add as optional legends, not inserted characters |
| Three local word suggestions | Up to 3 | Up to 3 | Present; improve quality and empty/loading behavior |
| Suggestion-row collapse | Missing | Missing | Add |
| Contextual action chips | Missing | Missing | Add a typed, editor-aware model |
| Number/symbol mode | One basic symbol page | One basic symbol page | Add two coordinated pages and restore punctuation coverage |
| Shift / Backspace / Space / Enter | Implemented | Implemented | Preserve every key while changing rows |
| Language switch | Implemented | Implemented | Keep; make current language state clear |
| Spacebar language arrows | Missing | Missing | Add directional cursor through installed language/layouts or cycle affordance; avoid accidental text insertion |
| Emoji categories / recents / favorites | Implemented | Implemented | Keep; add search and broaden Unicode coverage later |
| Text clipboard history | Encrypted local history | Not available as background history | Preserve platform boundary; iOS use explicit user-driven paste |
| Image/screenshot clipboard insertion | Not implemented | Not implemented | Future opt-in feature; supported API and editor compatibility required |
| Microphone / voice typing | Not connected | Not connected | Do not show as live until speech + permission flow is real |
| Keyboard search | Not implemented | Not implemented | Define exact scope first (emoji, settings, symbols or online content) |
| GIF / sticker discovery | Not implemented | Not implemented | Separate feature with data source, moderation, privacy and licensing plan |
| More-tools grid | Missing | Minimal two-action tools mode | Build platform capability-based grid |
| Toolbar reorder | Preference flow exists | Preference flow exists | Keep; use the same capability IDs and safe defaults |
| Incognito | Implemented | Implemented | Preserve and include in More grid |
| Themes shortcut from keyboard | Settings/Studio shortcut only | Host-app route not exposed as a complete keyboard tool | Add a clear Studio route where platform policy permits |
| Resize | Studio key-height control; no free drag/position | No SwiftKey-style free resize | Android: distinguish height vs free resize; iOS: do not fake unsupported free resize |
| Modes (one-handed/floating/split) | Not implemented | Not implemented | Optional future phase; capability-gated |
| System navigation / safe area | Source handling exists; device QA pending | Native safe-area behavior; device QA pending | Validate physical devices, not screenshots alone |

## 3. Recommended implementation order

### P0 — correctness and key completeness
1. Define one shared Arabic character inventory and row profile for Android/iOS. Include all 28 standard Arabic letters in the primary arrangement and provide hamza/alif/ta-marbuta/maqṣūra variants through explicit keys or long-press menus. Do not remove existing insertion actions.
2. Add tests asserting every required Arabic letter and extended form is reachable from the default keyboard or a documented long-press menu.
3. Unify Android/iOS symbol inventories and add two symbol pages. Verify page transitions, Backspace, ABC, comma, period, space and editor-specific Enter.
4. Add Arabic key secondary number legends without changing the inserted character or accessibility name.

### P1 — toolbar and suggestion UX
5. Add a real More-tools grid on Android and iOS using a common capability registry. Persist order and visibility; never list unavailable services as working actions.
6. Add a collapse/expand suggestion row and reusable contextual action chips. Preserve password/Incognito/numeric/email/URL privacy suppression.
7. Add spacebar language/cursor affordances only after touch-target and RTL behavior tests.
8. Replace generic toolbar text glyphs with original YORY vector icons and meaningful TalkBack/VoiceOver labels.

### P2 — optional rich input
9. Speech-to-text using platform-approved speech APIs and a permission explanation initiated from Studio; failure/cancellation must leave typing available.
10. Searchable local emoji/symbol catalogue. Do not silently turn this into web search.
11. Explicit image/screenshot paste support only after Android content-commit and iOS host-pasteboard capability tests; no background image harvesting.
12. GIF/stickers only after defining a lawful content source, moderation rules, offline/online states and privacy behavior.

### Platform truth
- Android may gain free keyboard width/position controls only after the native IME window behavior is proven on gesture and 3-button navigation.
- iOS must not present a fake free-resize or background clipboard-history control.
- Sign-in is a YORY optional account action, not a Microsoft sign-in clone.
- Microsoft Rewards, proprietary artwork, Copilot branding and service endpoints are out of scope.

## 4. Definition of done

- No standard Arabic letter is missing from the primary layout or the documented alternates.
- Android/iOS share the same tested symbol inventory and page transitions.
- All original keys remain available: letters, shift, delete, symbols/ABC, language, comma, space, period and editor action.
- Tool registry order/visibility survives keyboard recreation and is consistent with Studio settings.
- More grid only shows working features for the active platform.
- Suggestion collapse and contextual chips do not appear in sensitive or Incognito contexts.
- No button claims voice, image paste, search, GIF or sticker support until the complete flow works.
- Touch targets, screen-reader labels, 320/360/390/430dp widths, landscape, Android navigation modes, iOS home indicator and keyboard-extension height are validated.
- Screenshot diffs are run for Arabic letters, symbols page 1, symbols page 2, suggestion row, toolbar and More grid on both platforms.
- CI success and physical-device QA are recorded separately.

## 5. Open validation work

The screenshots establish appearance and visible controls, not exact tap results, hidden states or network behavior. A manual reference pass is still needed for the Reorder items behavior, exact symbol-page transition, microphone permission flow, screenshot insertion support, GIF/sticker source, search scope and per-platform resize controls. Mark these unknown until tested; do not infer implementation details from the screenshots alone.
