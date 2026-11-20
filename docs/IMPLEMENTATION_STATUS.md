# Implementation Status

This is the source of truth for repository state. UI presence is not proof that a feature is production-ready. The feature catalogue in \`shared/specs/feature-catalog.json\` records each feature's current status.

## Implemented source slices

### Shared product contracts
- Master product specification, iPhone-first experience standard, security threat model, language quality program, test plan, Supabase deployment runbook, and feature-by-feature implementation map are committed.
- The feature catalogue contains 52 individually tracked capabilities.
- The language target catalogue contains 120 unique language tags. This is a rollout catalogue, not proof that 120 production language packs exist.
- Theme JSON Schema v1 defines portable cross-platform themes. Eight-digit colors use CSS-style \`#RRGGBBAA\`.
- Android and iOS theme encoders/decoders share the same versioned field contract and import legacy fields with defaults.

### Android
- Native Kotlin host app opens YORY Studio.
- A real \`InputMethodService\` and \`InputConnection\` implementation supports basic English and Arabic layouts, numbers, symbols, shift, delete, space, return, language switching, an emoji strip, suggestions, and editor-aware layout traits.
- Android Studio supports preset themes, custom HEX/ARGB background/key/text/accent/pressed-key colors, glass opacity, corner radius, key height, key spacing, letter size, key shape, sound/haptic toggles, clipboard-history opt-in, and .yorytheme import/export.
- The active theme is stored locally and consumed by the real keyboard renderer, including per-key pressed feedback, shape, spacing and font-size settings.
- Android applies editor-aware automatic capitalization through the active \`InputConnection\`.
- Android offers a small offline prefix-completion baseline from bundled English/Arabic word lists. It is not a trained next-word model or production autocorrect engine.
- Optional Android clipboard text history stores up to 50 local entries and offers quick insert/clear. It does not ingest screenshot/image items. Filtering is heuristic and not guaranteed to identify every sensitive string.

### iOS
- A SwiftUI host app and UIKit custom keyboard extension are generated through XcodeGen.
- The iOS keyboard supports basic English/Arabic layouts, number/symbol modes, shift, delete, space, return, next-keyboard switching, an emoji strip, a small offline prefix-suggestion list, and input-trait-aware behavior.
- iOS Studio exposes presets, color pickers for background/key/text/accent/pressed key, glass opacity, corner radius, key spacing, letter size, key shape, sound/haptic toggles, language selection, and .yorytheme import/export.
- The iOS keyboard renderer reads and applies theme colors, pressed-key color, shape, spacing, font size, corner radius, opacity, sounds and haptic preferences.
- Automatic capitalization respects the host text object's \`autocapitalizationType\`.
- The extension declares \`RequestsOpenAccess = true\) so iOS can offer the system Allow Full Access option. This is user-controlled, not automatic. Basic typing remains available without Full Access; iOS keyboard click audio is only invoked when \`hasFullAccess\` is true.
- No code in the keyboard typing path sends keystrokes, passwords, surrounding text, or clipboard payloads to a server.

### Backend source
- Supabase migration source defines profiles, themes, ratings, encrypted dictionary-backup ciphertext storage, language-pack manifests, preferences, favorites, download events and sticker-pack metadata.
- RLS policies, explicit least-privilege client grants and pgTAP contract tests are committed.
- A JWT-authenticated theme publishing Edge Function is present in source.
- No hosted Supabase project has been modified. A connected project was visible with ACTIVE_HEALTHY status, but its generic project name did not prove it was the intended YORY project. Deployment is blocked until its identity is confirmed.

### Validation
- GitHub Actions repository-contract validation has passed on recent commits, checking JSON contracts, 120 unique language tags, feature status values and basic secret-pattern scanning.
- Android CI has passed for earlier commits containing the core theme model, codec and keyboard renderer changes.
- iOS CI has passed for earlier commits containing the iOS theme renderer and Open Access handling.
- The latest automatic-capitalization and Android preview refinements have new CI runs; check the Actions tab for their final status before treating those exact revisions as build-verified.
- Successful CI builds are not a substitute for installation and testing on physical phones.

## Not yet production-ready

- No signed Android APK/AAB has been released. Android CI builds are development artifacts only.
- No signed iOS archive, App Store submission, or physical-device installation has been completed.
- Neither platform has passed physical-device QA for touch accuracy, layout geometry, battery, memory, accessibility, app compatibility, audio/haptics or extension lifecycle.
- Current word suggestions are tiny bundled prefix lists. Neural next-word prediction, robust language-specific autocorrect, personal learning, multilingual language identification and swipe typing are not implemented.
- Only basic English and Arabic layouts exist. The other 118 language entries are catalogue-only; no production language packs are shipped.
- Theme Studio does not yet include a production gradient editor, imported image backgrounds, full supported-font browser, multi-theme library management, marketplace client, or a fully validated live wallpaper blur/refraction renderer.
- Glass currently uses tinted translucent surfaces and subtle borders. True live backdrop blur/refraction must be implemented and measured separately on each OS/device class.
- iOS clipboard history, automatic screenshot/image ingestion, sticker creation, translation tools and writing assistance are not implemented. iOS clipboard access is not equivalent to Android and must remain user-driven.
- Android clipboard filtering is heuristic and cannot guarantee detection of all OTP, payment or otherwise sensitive text. It is local-only and user-controlled.
- Custom imported sound packs, per-key sound mapping, advanced haptic tuning, emoji search/categories/skin-tone picker and alternate emoji artwork packs are not complete.
- Sticker creation, creator profiles, marketplace moderation, theme reporting and public theme discovery UI are not implemented.
- Supabase migrations and Edge Function are source-only. They have not been applied to a verified YORY project. Storage policy deployment, service configuration, secret provisioning and live RLS verification remain pending.
- Personal dictionary sync is not implemented. If added, the client must encrypt it before upload and the server must store ciphertext only.
- Store listing metadata, privacy disclosures, terms, support contact, crash reporting and release signing remain to be completed.

## Verification status policy

- \`implemented_unverified\`: source code exists but has not passed required platform/device QA.
- \`partial_implementation\`: a limited end-to-end slice exists.
- \`prototype\`: a narrow baseline exists and is not release quality.
- \`schema_scaffold\`: API/database contract only.
- \`ui_scaffold_only\`: visible UI without complete behavior.
- \`not_implemented\`: no implementation exists yet.
- \`production_ready\`: only after CI, device QA, security/privacy review and release validation all pass.

## Next engineering order

1. Finish current Android and iOS CI runs after the newest source changes.
2. Test the native keyboards on physical Android and iPhone devices.
3. Complete keyboard input behavior, accessibility and layout geometry.
4. Build the full theme editor and ensure preview/live rendering parity.
5. Add personal dictionary and quality-evaluated offline prediction.
6. Add emoji search, sticker creation, clipboard controls and writing tools within platform rules.
7. Confirm the correct Supabase project, test migrations locally, run RLS/Storage tests, then deploy deliberately.
8. Add signed language packs one language at a time after quality and licensing checks.
