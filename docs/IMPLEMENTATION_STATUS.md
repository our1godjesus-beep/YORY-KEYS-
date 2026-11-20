# Implementation Status

This file is the source of truth for what currently exists in the repository. A feature is not production-ready merely because it appears in a UI or schema.

## Implemented source slices

- Android host app opens YORY Studio.
- Android YORY Studio writes theme values to app-private preferences.
- Android IME is registered as an InputMethodService and commits characters through InputConnection.
- Android IME includes basic English and Arabic key rows, number row, shift, delete, space, return, language switch, a starter emoji strip, and a local prefix-completion prototype for non-sensitive fields.
- iOS host app and Custom Keyboard Extension source are present.
- iOS XcodeGen project definition, extension Info.plist, and App Group entitlements are present.
- iOS YORY Studio writes theme settings to the shared App Group; the keyboard reads settings without requesting Open Access.
- Shared theme JSON schema and a 120-language target catalogue are present.
- Supabase foundation migration exists; an additional marketplace/preferences migration and publish-theme function are being added.

## Not yet production-ready

- No Android APK/AAB has been built or installed from this repository in this session.
- No iOS archive, signing, or physical-device installation has been performed.
- Android and iOS keyboards have not been device-tested for geometry, orientation, accessibility, secure fields, third-party app compatibility, or memory behavior.
- The Android word completion uses a small hand-curated dictionary. It is not a neural language model, advanced prediction, or a reliable autocorrect system.
- iOS prediction, autocorrect, personal learning, swipe typing, clipboard history, screenshot import, sticker creation, translation, and the theme marketplace are not complete.
- Sound and haptics controls are scaffolds until they are wired to supported system APIs and tested.
- 120 language entries are a target catalogue, not 120 installed language packs.
- Supabase schema and Edge Function source are not deployed to a YORY Supabase project. The connected project discovered during setup is inactive and was not modified.

## Status vocabulary

- `implemented_unverified`: source code is present but has not passed target-device QA.
- `partial_implementation`: a limited end-to-end slice exists.
- `prototype`: narrow baseline, not release quality.
- `schema_scaffold`: database/API contract only.
- `ui_scaffold_only`: visible control, not connected to final platform behavior.
- `not_implemented`: no implementation yet.
- `production_ready`: only after build, tests, privacy review and real-device QA.
