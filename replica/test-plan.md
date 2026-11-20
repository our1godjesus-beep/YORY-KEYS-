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
| RI-C1 | Compact suggestion mode | happy | Open Rich input; enable Compact mode; open a normal text field; compare suggestion bar; disable it and reopen | Compact mode shows a shorter row with at most two suggestions; normal mode restores the standard row and up to three suggestions; typed input remains unaffected | Unit/UI + manual on both platforms | Not run |\n| F08-H1 | Sound/haptics settings | happy | Toggle sound and haptics; alter supported values; tap keys | Runtime respects local settings and system-level haptic controls | Manual physical-device | Not run |
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
