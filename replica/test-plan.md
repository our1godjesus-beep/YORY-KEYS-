# YORY KEYS Replica — test plan

Build: baseline repository commit bd885c80f7d0b8cef13fc8b5162212f122da1b11
Date: 2026-11-20
Environment: Android emulator/device and iOS simulator/device. No test in this document is marked passed until executed.

| Case | Flow | Type | Steps | Expected | Automation / manual | Result |
|---|---|---|---|---|---|---|
| F01-H1 | Enable keyboard on Android | happy | Open setup; open system keyboard settings; enable YORY KEYS; open picker; select YORY; return and type | Keyboard becomes active; setup status updates; text is inserted correctly | Manual + instrumentation | Not run |
| F01-H2 | Enable keyboard on iOS | happy | Follow Settings > General > Keyboard > Keyboards; add YORY; select it in a text field | iOS setup is accurate; keyboard appears where third-party keyboards are allowed | Manual physical-device | Not run |
| F01-E1 | Cancel setup | edge | Open setup CTA, then back out of system settings | Studio remains usable; UI does not falsely report keyboard enabled | Manual | Not run |
| F01-N1 | System does not allow keyboard | negative | Disable keyboard or use an app that disallows third-party keyboards | Explain the limitation without crashing or claiming activation | Manual | Not run |
| F02-H1 | Browse and apply local theme | happy | Open Themes; select category; open a local theme; Apply | Active theme changes locally and appears on the next keyboard presentation | UI test + manual | Not run |
| F02-E1 | Browse themes offline | edge | Enable airplane mode; open Themes; browse built-in presets; apply one | Built-in themes remain available; no blocking network spinner | Instrumentation + manual | Not run |
| F02-E2 | Empty category | edge | Select a category with no matching items | Clear empty state and a route back to All | UI test | Not run |
| F03-H1 | Create custom theme | happy | Create; change colors and geometry; preview EN/AR; Save; reopen keyboard | Saved theme persists and preview/keyboard geometry matches | UI test + manual | Not run |
| F03-E1 | Invalid HEX input | edge | Enter invalid hex; attempt Apply | Validation error is inline; previous valid color is retained | Unit + UI test | Not run |
| F03-E2 | Discard unsaved theme | edge | Change settings; Back; choose Discard | Draft is discarded and saved theme is unchanged | UI test | Not run |
| F03-N1 | Malformed theme import | negative | Import invalid JSON or unsupported schema version | Safe error; no active theme corruption; fallback remains usable | Unit + UI test | Not run |
| F04-H1 | Select English layout | happy | Choose English (US) / QWERTY; open keyboard | Selection is persisted and runtime keyboard uses QWERTY | Unit + manual | Not run |
| F04-H2 | Select Arabic layout | happy | Choose Arabic (Egypt); open keyboard | Arabic letters, RTL semantic layout and appropriate punctuation are displayed | Manual | Not run |
| F04-E1 | Mixed Arabic and numbers | edge | Type Arabic text with Latin numbers, punctuation and parentheses | Bidi ordering remains legible; cursor movement and deletion behave correctly | Manual | Not run |
| F04-E2 | German basic layout | edge | Select Deutsch / QWERTZ | UI labels it as a basic layout only; no claim of full dictionary or prediction pack | UI test | Not run |
| F05-H1 | Toggle number row | happy | Open Layout & keys; enable number row; focus text field | Number row appears in the selected position | UI test + manual | Not run |
| F05-E1 | Numeric editor trait | edge | Focus numeric/phone field while number row is enabled | Native numeric layout is sensible; no duplicate number row | Manual | Not run |
| F05-E2 | Long press extremes | edge | Set duration to minimum and maximum; test alternate key press | No accidental duplicate commit; popup timing reflects setting | Manual | Not run |
| F06-H1 | Resize keyboard height on Android | happy | Open Layout & keys > Resize; adjust the 36–60dp slider; choose Compact, Standard and Tall; reopen a text field | Preview updates, selected value persists and live key height changes on next keyboard presentation; Reset returns to 46dp | Android CI + manual device | Not run |
| F06-N1 | iOS resize capability | negative | Inspect iOS Layout & keys | No fake unsupported resize screen; only supported extension sizing behavior is shown | UI test | Not run |
| F07-H1 | Clipboard insert on Android | happy | Copy normal text; open keyboard clipboard; tap clip | Text is inserted into current text field | Manual | Not run |
| F07-E1 | Pin, delete and undo clip | edge | Pin clip; delete another; use Undo if available | Pin/delete/undo state is consistent and persistent | UI test + manual | Not run |
| F07-N1 | Sensitive input | negative | Focus password, numeric PIN, email and URL fields; copy sensitive-looking strings | Suggestions/learning/clipboard capture are suppressed according to policy | Manual security QA | Not run |
| F07-N2 | iOS clipboard parity | negative | Open Rich input on iOS | Do not display Android clipboard history as if supported | UI test | Not run |
| RI-C1 | Compact suggestion mode | happy | Open Rich input; enable Compact mode; open a normal text field; compare suggestion bar; disable it and reopen | Compact mode shows a shorter row with at most two suggestions; normal mode restores the standard row and up to three suggestions; typed input remains unaffected | Unit/UI + manual on both platforms | Not run |
| F08-H1 | Sound/haptics settings | happy | Toggle sound and haptics; alter supported values; tap keys | Runtime respects local settings and system-level haptic controls | Manual physical-device | Not run |
| F08-E1 | System haptics disabled | edge | Disable system vibration and keep YORY haptics on | UI explains system override; no crash or false feedback state | Manual | Not run |
| F09-H1 | Offline launch and typing | happy | Enable airplane mode; launch Studio; open keyboard; type and change local theme | Studio and basic typing remain available without network | Instrumentation + manual | Not run |
| F09-E1 | Expired account session offline | edge | Start with expired optional session; turn off network | Local settings and typing stay available; app does not block on token refresh | Manual | Not run |
| F10-H1 | Optional account sign-in | happy | Open Account; sign in while online | Auth succeeds; cloud-only status appears only after server confirmation | Integration test | Not run |
| F10-N1 | Sign-in failure/offline | negative | Submit invalid credentials or remove network | Clear error; user can continue offline; no local data loss | Integration + manual | Not run |
| F10-E1 | Sign out | edge | Sign out from connected state; return to Studio | Cloud session ends; local themes/languages/preferences remain | Integration + manual | Not run |
| SYS-A1 | Android 3-button nav | platform | Test Back/Home/Recents with Studio list, details and dialogs | App content/actions stay above system navigation region | Manual physical-device | Not run |
| SYS-A2 | Android gesture nav | platform | Test bottom quick-action and scrolling with gesture handle | No overlap with gesture area; no accidental Home gesture capture | Manual physical-device | Not run |
| SYS-A3 | Android edge-to-edge / IME | platform | Target SDK 35; open focused color/HEX field and software keyboard | Insets applied exactly once; content moves with IME and remains reachable | Instrumentation + manual | Not run |
| SYS-I1 | iOS safe area | platform | Test notch/Dynamic Island and home-indicator devices | Top/bottom controls remain unobscured | Manual physical-device | Not run |
| SYS-I2 | iOS keyboard extension height | platform | Test portrait, landscape, iPad docked and floating where supported | Extension fits available height; no key/toolbar clipping | Manual physical-device | Not run |
| RSP-1 | 320dp compact width | responsive | Open every settings screen at 320dp equivalent | No clipped controls; content scrolls; labels wrap; touch targets remain usable | Screenshot test + manual | Not run |
| RSP-2 | 360/390/430dp widths | responsive | Repeat main screens at each width | Same hierarchy with fluid margins; no abrupt text shrink | Screenshot test | Not run |
| RSP-3 | 600dp+ adaptive | responsive | Open Studio in tablet/foldable window | Bounded content width or adaptive pane; never stretch forms edge-to-edge | Manual | Not run |
| A11Y-1 | TalkBack / VoiceOver | accessibility | Navigate settings rows and keyboard toolbar using screen reader | Every icon-only action has a meaningful label and logical focus order | Manual | Not run |
| A11Y-2 | Dynamic Type / font scale | accessibility | Increase system font size | Main controls remain visible and reachable; scroll instead of clipping | Manual | Not run |

| F11-H1 | Theme details and apply | happy | Open a theme's Details page; inspect larger preview and settings; Apply | The selected theme and active state update consistently in gallery and keyboard | UI test + manual on both platforms | Not run |
| F11-H2 | Duplicate, rename and favorite theme | happy | Duplicate a preset; rename the saved copy; favorite it; filter Favorites | Only the saved copy is renamed/deleted; favorites persist across app restart | UI test + persistence test | Not run |
| F11-N1 | Protect built-in presets | negative | Open a built-in theme's Details page | Rename/delete are disabled; Duplicate remains available | UI test | Not run |
| F12-H1 | Customize toolbar | happy | Reorder tools; hide optional tools; save; reopen keyboard | Both visibility and order persist and appear on the live keyboard; required switch/settings actions remain accessible | UI test + manual on both platforms | Not run |
| F12-E1 | Toolbar settings migration | edge | Open toolbar settings with no saved order, partial saved order or unknown tool IDs | Defaults are restored for missing items; invalid IDs are ignored; no crash | Unit/UI test | Not run |
| P01-H1 | Incognito typing | privacy | Enable Incognito; type words and accept suggestions; copy text on Android; switch editor | Predictions and personal learning pause; Android clipboard capture pauses; session resets at next keyboard input session | Manual privacy QA on both platforms | Not run |
| P01-E1 | Incognito with manual clipboard | privacy | Enable Incognito; open clipboard shortcut if visible; disable Incognito | Clipboard capture does not run in Incognito; existing saved clips remain user-controlled and are not silently deleted | Manual | Not run |
| C02-H1 | Encrypted clipboard lifecycle | happy | Copy regular text; pin one item; delete another; Undo; restart app; clear all | Entries persist encrypted, pin status is retained, Undo restores last deletion, Clear removes all entries | Instrumentation + manual Android | Not run |
| C02-E1 | Clipboard expiry | edge | Use a test clock or aged fixture for unpinned and pinned entries | Unpinned entries older than 24 hours are removed; pinned entries remain until removed | Unit/instrumentation Android | Not run |
| C02-N1 | Sensitive clipboard filtering | security | Copy common one-time-code, PIN and payment-card-like strings | Common patterns are rejected; product copy explains filtering is heuristic and not guaranteed | Security QA Android | Not run |

| E01-H1 | Browse emoji categories | happy | Open emoji picker; switch through Recent, Smile, People, Nature, Food, Travel and Symbols | Category changes update the picker and selecting an emoji inserts it at the current cursor | Manual UI + input test on both platforms | Not run |
| E01-E1 | Recent emoji persistence | edge | Insert several emoji; close and reopen keyboard; open Recent | Up to 24 most-recent emoji appear in most-recent-first order; data stays local | Manual persistence test on both platforms | Not run |
| E01-H2 | Emoji skin tones | happy | Open People category; choose each tone; insert supported hand/person emoji | The selected tone is applied to supported basic emoji; Default restores unmodified variants | Manual Unicode insertion test | Not run |
| E01-N1 | Emoji picker on restricted editor | negative | Open numeric, email and secure fields; inspect emoji action availability | No keyboard crash or accidental change to numeric layout; platform restrictions are respected | Manual platform test | Not run |

| E01-H3 | Favorite emoji | happy | Long-press an emoji; open Favorites; remove it from Favorites | Favorite state persists locally; removal updates Favorites immediately | UI + persistence test on both platforms | Not run |
| E01-E2 | Empty Favorites category | edge | Open Favorites without any saved emoji | Helpful empty state explains how to add an emoji; no blank or broken panel | UI test on both platforms | Not run |

| F07-N3 | Hide clipboard UI for restricted fields | privacy | Switch between normal text, password, numeric PIN, email and URL fields while the keyboard is open | Clipboard shortcut and saved clips are hidden for restricted fields; capture listener is detached and does not collect copied content | Android manual security QA | Not run |

| U01-H1 | Emoji data helpers | unit | Run YoryEmojiCatalogTest | Categories are unique; recent/favorites ordering and bounds are correct; storage parsing deduplicates; skin-tone modifier placement is stable | JVM unit test | Pending CI |
| U02-H1 | Clipboard privacy policy | unit | Run ClipboardHistoryPolicyTest | Common OTP/PIN/payment card patterns are filtered; unpinned expiry, pinning window and 50-entry cap are deterministic | JVM unit test | Pending CI |

| C02-H2 | Search clipboard history | happy | Type a word or phrase in the clipboard manager search field | Matching clips are filtered case-insensitively; clear search restores all entries; an informative empty result appears | JVM policy test + Android UI QA | Pending CI |
| C02-E2 | Legacy clipboard migration write failure | edge | Simulate encrypted write failure during legacy migration | The old preference copy is retained until an encrypted replacement is saved successfully | Unit/instrumentation test with injected storage failure | Not run |

| L01-H1 | Seven offline language layouts | happy | Select English US, Arabic, German, Spanish, French, Portuguese and Italian from Languages and cycle with the language key | Correct letter rows and language label appear on Android and iOS; Arabic uses RTL and others LTR | JVM layout unit tests + manual platform QA | Pending CI |
| L01-E1 | Unknown saved language tag | edge | Restore an unknown or unsupported language tag from app settings | Keyboard safely falls back to English US without crashing | JVM unit test + manual iOS QA | Pending CI |
| L01-H2 | Language-specific learning separation | happy | Enable personal learning and accept a word under each supported language tag | Learned words are stored under that language's base tag and never appear as English suggestions in another language | JVM unit test + manual iOS QA | Pending CI |
| L01-N1 | Layout-only language prediction | negative | Select German, Spanish, French, Portuguese or Italian with no local learned words | No English or Arabic bundled suggestions are shown; UI labels these layouts as layout-only | Manual UI/input test | Not run |


## Keyboard reference parity — new cases (2026-11-20)

| Case | Flow | Type | Steps | Expected | Automation / manual | Result |
|---|---|---|---|---|---|---|
| KBD-AR-01 | Complete Arabic base alphabet | functional | Open Arabic layout on Android and iOS; enumerate all 28 standard letters; insert each into a test field | Every standard Arabic letter is reachable from the base layout; no existing special action is lost | Shared layout unit test + manual device input | Not run |
| KBD-AR-02 | Arabic extended forms and digit legends | functional/accessibility | Long-press the relevant Arabic keys; inspect secondary digit hints with TalkBack/VoiceOver | Extended hamza/alif/ta-marbuta/maqṣūra forms are discoverable; number legends are visual hints only and do not alter inserted letter or screen-reader name | Unit + accessibility manual | Not run |
| KBD-SYM-01 | Symbol page one | functional | Switch from letters to symbols; insert each symbol; use Backspace, comma, period, space and Enter | First page has the approved shared symbol inventory; all keys commit the intended character | Shared inventory unit test + manual Android/iOS | Not run |
| KBD-SYM-02 | Symbol page two and return | edge | Move between symbol pages; return to ABC; switch language; reopen symbols | Second page and ABC/123 transitions work; no key is lost and selected language is preserved | UI test + manual device | Not run |
| KBD-TOOLS-01 | More-tools grid | functional | Open More; browse every tool; reorder/hide tools; restart keyboard; tap each available item | Only platform-supported connected actions appear; order/visibility persist; every tile has a real action and accessible label | UI + persistence tests on both platforms | Not run |
| KBD-SUG-01 | Suggestion collapse and contextual chips | functional/privacy | Collapse and expand suggestions; trigger contextual chips; repeat in Incognito, password, numeric, email and URL editors | Layout remains stable; chips are dismissible; restricted contexts suppress learning/suggestions and do not expose private content | UI + privacy manual | Not run |
| KBD-PASTE-01 | Explicit image/screenshot insertion | platform | Use a user-initiated supported image paste/content-commit action in compatible host apps; cancel and deny where possible | Image is inserted only after explicit user action and only in compatible editors; no clipboard image is captured in background | Android API/UI + iOS capability manual | Not run |
| KBD-A11Y-01 | Keyboard toolbar and key accessibility | accessibility | Navigate all letter rows, symbols, More grid, suggestion chips and spacebar controls with TalkBack/VoiceOver | Every icon/key has a meaningful label, logical focus order and adequate touch target; visual secondary hints are not duplicated in spoken output | Manual physical-device | Not run |
| KBD-RESIZE-01 | Platform resize capability truth | platform | Open resize controls on Android and iOS; inspect system navigation and keyboard extension behavior | Android key-height control is not mislabeled free resize; iOS does not advertise unsupported free repositioning | Manual Android/iOS device | Not run |


| KBD-LANG-01 | Spacebar swipe to next added language | functional | Add English, Arabic and French; focus a normal text field; swipe left across the spacebar | Active layout changes in order to the next added language; no space character is inserted; suggestion language and RTL state update | Android/iOS UI automation + physical-device gesture QA | Not run |
| KBD-LANG-02 | Spacebar swipe to previous added language | functional | With at least two added layouts active, swipe right across the spacebar | Previous added layout becomes active; no space is inserted; list wraps correctly | Android/iOS UI automation + physical-device gesture QA | Not run |
| KBD-LANG-03 | Single added language / remove active language | edge | Add only one language; attempt both swipes; then remove the active language when alternatives remain | A single-layout cycle remains unchanged; removing the active layout selects a remaining added layout safely | Unit + settings persistence + device QA | Not run |
| KBD-LANG-04 | Spacebar tap vs swipe boundary | edge/accessibility | Tap, short horizontal movement, vertical movement, and a deliberate horizontal swipe near touch-slop boundary | Tap inserts exactly one space; non-horizontal gestures do not switch language; deliberate swipe changes language without a space | Android/iOS gesture unit/UI test + physical QA | Not run |
| KBD-SYM-03 | Quotation mark insertion | functional | Tap the double-quote key on symbol page 1 on Android and iOS | Exactly one quotation mark is inserted | UI input test on both platforms | Not run |
