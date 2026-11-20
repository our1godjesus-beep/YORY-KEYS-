# YORY KEYS

**Your keyboard. Your universe.**

YORY KEYS is an original, privacy-first, highly customizable keyboard platform for Android and iOS. The product is designed as two real native keyboard implementations with a shared product specification and interoperable theme format.

## Product principles

1. **A real keyboard, not a mockup.** Android uses an Input Method Service (IME); iOS uses a Custom Keyboard Extension.
2. **Customization is a core feature.** Users can independently customize colors, gradients, key geometry, typography, glass appearance, sounds, haptics, and the keyboard's accessory UI.
3. **Offline-first.** Typing, local dictionaries, installed language packs, saved themes, emoji search, and local clipboard features must remain useful without connectivity.
4. **Privacy by design.** Raw typed text, passwords, clipboard contents, and prediction context are never uploaded. Optional cloud features are explicit and narrowly scoped.
5. **Native platform behavior.** Respect secure fields, RTL, input traits, return-key types, accessibility, and OS restrictions.
6. **Honest language support.** Never advertise 120 equally mature languages until each language has passed a documented quality test suite.

## Repository layout

- `android/` — native Android app and IME.
- `ios/` — native iOS host app and keyboard-extension source.
- `shared/specs/` — versioned product contracts and theme format.
- `supabase/migrations/` — backend schema and row-level security.
- `docs/` — architecture, privacy, quality gates, and roadmap.

## Current implementation status

This is the initial engineering foundation. It is **not yet a release-ready keyboard**. The Android and iOS source scaffolds need to be opened and built in their native toolchains, then tested on physical devices. Features are considered complete only after real-device verification.

## Build direction

- Android: Kotlin, Android SDK, InputMethodService, Jetpack Compose for the host/settings app.
- iOS: Swift, SwiftUI host app, UIKit-based Keyboard Extension.
- Backend: Supabase Auth, Postgres, Storage, and Edge Functions only for opt-in cloud features.
- Prediction: on-device engine behind a platform-independent interface; language packs are versioned, signed, and installed locally.

## Security promise

The keyboard must not transmit keystrokes or surrounding text. Network access belongs to the host app's opt-in sync/update flows, not the live typing path. Clipboard and screenshot access are constrained by platform APIs and user consent; automatic screenshot harvesting is not promised where the OS does not permit it.

## First milestone

Build and verify the smallest real vertical slice:
1. Install the Android app.
2. Enable YORY KEYS in system keyboard settings.
3. Select YORY KEYS and type in ordinary text fields.
4. Verify password fields do not trigger learning or clipboard features.
5. Switch between English and Arabic and verify RTL behavior.
6. Apply a saved theme and confirm the actual IME updates.
7. Establish the equivalent iOS host-app + keyboard-extension target in Xcode.
