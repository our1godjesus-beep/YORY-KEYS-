# YORY KEYS

**Your keyboard. Your universe.**

YORY KEYS is a native keyboard product for Android and iOS, designed around the Tinted Glass aesthetic, deep personalization, on-device typing intelligence, and privacy-first offline operation.

> Repository status: active engineering foundation. The feature matrix is the authority for what is implemented, partial, planned, or not yet shipped. Do not treat the roadmap as a list of completed features.

## Product promise

- **Real system keyboard:** Android `InputMethodService`; iOS `UIInputViewController` custom keyboard extension.
- **YORY Studio:** a keyboard design studio with theme presets and granular controls.
- **Local-first typing:** no live keystrokes, passwords, prediction context, or clipboard payloads are uploaded.
- **Optional cloud:** Supabase for accounts, public themes, preference sync, encrypted dictionary backup, and signed language-pack delivery.
- **Honest language support:** 120 languages are listed as a target catalogue. Each model and layout must pass its own quality gates before being marked as supported.

## Current implementation slices

| Area | Current state |
|---|---|
| Android | Native IME source, basic English/Arabic layouts, key actions, local theme store, YORY Studio, starter prefix suggestions |
| iOS | SwiftUI host-app source, UIKit keyboard extension, XcodeGen project definition, App Group theme sharing, basic EN/AR layout |
| Themes | Shared versioned JSON schema, platform-local theme controls and presets |
| Languages | 120-entry prioritized target catalogue; only a starter local dictionary prototype exists |
| Backend | Supabase migrations, RLS policies, Storage bucket rules, theme publishing function source |
| QA | Android unit tests and Android/iOS CI workflows added; platform builds and device QA still required |

See [implementation status](docs/IMPLEMENTATION_STATUS.md), [feature catalogue](shared/specs/feature-catalog.json), and [roadmap](docs/ROADMAP.md).

## Android build

Requirements: Android Studio or JDK 17 with Android SDK platform 35 and Gradle 8.9.

```bash
cd android
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest
```

Install the resulting APK on a test phone, open YORY KEYS, enable it in system keyboard settings, then choose it from the input-method picker. Test first in a normal text field and separately verify a secure/password field.

## iOS project generation

Requirements: macOS, Xcode, XcodeGen, and an Apple Developer signing team for physical-device installation.

```bash
cd ios
brew install xcodegen
xcodegen generate
open YORYKeys.xcodeproj
```

Configure the same App Group `group.app.yory.keys` on the host app and keyboard extension, configure signing for both targets, then build and test on a real iPhone. The first extension configuration deliberately does not request Open Access.

## Supabase

Supabase is not required for typing. The connected Supabase project discovered during the initial setup was inactive and was intentionally not modified.

Apply migrations only to a confirmed YORY project, preferably after local testing. Follow [supabase/README.md](supabase/README.md). Never place service-role or secret keys in mobile apps or this public repository.

## Repository map

- `android/` — Kotlin app, InputMethodService, theme editor, local completion prototype.
- `ios/` — SwiftUI host app, UIKit keyboard extension, XcodeGen project.
- `shared/specs/` — theme schema, feature status, 120-language catalogue.
- `supabase/` — migrations and Edge Functions.
- `docs/` — product, privacy, architecture, QA, iOS limitations and roadmap.
- `.github/workflows/` — Android and iOS CI workflows.

## Non-negotiable privacy rules

Never upload:
- Live typed text or surrounding context.
- Passwords, OTPs, payment information, or secure-field content.
- Clipboard contents, screenshots, or personal dictionary plaintext.

Any optional sync must be explicit, authenticated, scoped, and protected by RLS. The keyboard must remain useful offline and with iOS Open Access disabled.
