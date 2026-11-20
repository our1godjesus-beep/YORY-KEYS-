# YORY KEYS — Test Plan

## Test layers

### Contract tests
- Theme JSON validates against schema v1.
- Android and iOS encode/decode round-trips preserve supported tokens.
- Invalid, oversized, unknown-version and malicious file inputs are rejected.
- Language manifests verify signatures and checksums.
- Feature catalogue contains explicit, recognized status values.

### Android
- IME service is enabled and selectable from system settings.
- Insert/delete/space/enter/shift/symbol/language actions work in real apps.
- InputConnection handles cursor movement, selection and composing text safely.
- Password and secure numeric fields never trigger learning, clipboard capture or suggestions.
- Input traits produce suitable email, URL, number, phone and search layouts.
- Arabic RTL and mixed-script punctuation work.
- Clipboard history is opt-in, local-only, clearable and expiry-aware.
- Orientation, process recreation, low memory and accessibility.
- Theme changes reach the live IME, not just the host preview.

### iOS
- Host app and keyboard extension install and sign correctly.
- Keyboard is enabled and selected through iOS Settings.
- Works with Open Access disabled.
- Next-input-mode switch key works where system requests it.
- Secure, phone and name-phone input fields correctly fall back to Apple's keyboard.
- Apps that prohibit third-party keyboards are documented as unsupported contexts.
- Email, URL, number, decimal, search and return-key types are handled.
- UITextDocumentProxy edits do not corrupt text around cursor/selection.
- iPhone portrait/landscape and iPad docked/floating widths.
- App Group theme reads work; corrupted data falls back safely.
- Extension survives memory pressure and relaunch.
- VoiceOver, contrast, Reduce Motion and readable hit targets.

### Privacy and security
- No typed string appears in app/server logs.
- No keyboard-path HTTP/DNS call.
- No clipboard content reaches backend.
- Secure fields disable prediction learning and clipboard UI.
- App Group stores settings only, not text context.
- Supabase RLS denies cross-user theme/dictionary access.
- Storage policies restrict private sticker assets to owner.
- Account deletion and data export paths are verified.
- Static scans detect accidental secrets before merge.

## Release gates

A platform release is blocked until:
1. Native project builds in CI.
2. Unit/contract tests pass.
3. At least one supported physical device per platform is tested.
4. No unresolved blocker/high-severity input or privacy defect exists.
5. Store claims match feature catalogue status.
6. Privacy policy, support page, screenshots and onboarding are reviewed.
7. Crash/ANR/extension termination behavior is acceptable.
