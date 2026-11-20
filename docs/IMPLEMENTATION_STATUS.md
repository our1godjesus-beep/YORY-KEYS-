# Implementation Status

This file is the source of truth for what currently exists in the repository. A feature is not production-ready merely because it appears in a UI or schema.

## Implemented source slices

- Android host app opens YORY Studio.
- Android Studio includes five theme presets, free ARGB HEX inputs for background/key/text/accent, opacity/radius/height controls, sound/haptics toggles, and an explicit clipboard-history opt-in.
- Android theme settings persist locally and are consumed by the actual IME.
- Android IME uses InputMethodService/InputConnection and supports basic English/Arabic layouts, number row, shift, delete, space, return, symbol mode, language switching, emoji strip, key click sound and haptic feedback.
- Android has an offline prefix-completion prototype from small bundled English and Arabic lists. It is not a neural prediction or autocorrect model.
- Android has an optional local text clipboard history (up to 50 items), basic filtering for common OTP/card-like numeric strings, a quick insert row and clear action. It does not capture screenshots or image items.
- iOS host app includes a SwiftUI YORY Studio with presets, color pickers, opacity and corner-radius controls, sound/haptics toggles, and language preference.
- iOS XcodeGen project definition, extension Info.plist, and App Group entitlements are present.
- iOS keyboard extension uses UIKit, has basic English/Arabic layouts, number/symbol rows, shift, delete, space, return, next-keyboard switch, emoji strip, standard system keyboard click sound and light haptic feedback.
- iOS reads host-app theme preferences from the shared App Group while the extension remains in restricted mode; the extension does not request Open Access in this milestone.
- iOS has a small bundled local prefix-suggestion dictionary. It is not a trained prediction or autocorrect model.
- Shared theme JSON schema, feature status catalogue, and a 120-language target catalogue are present.
- Supabase migrations include user profiles, themes, ratings, encrypted-dictionary backup storage, language-pack manifest, preferences, favourites, sticker-pack metadata, storage buckets and RLS policies.
- A JWT-authenticated theme publishing Edge Function is present in source.
- Android unit test source and Android/iOS CI workflow definitions are present.

## Not yet production-ready

- No Android APK/AAB has been built or installed in this session.
- No iOS archive, signing, or physical-device installation has been performed.
- Android and iOS keyboards have not been device-tested for geometry, orientation, accessibility, secure fields, third-party app compatibility, memory, sound and haptic behavior.
- The current local word completion is a small prefix dictionary. Neural next-word prediction, robust autocorrect, personal learning, multilingual language detection and swipe typing are not implemented.
- Only basic English and Arabic keyboard layouts exist. The other 118 language entries are catalogue-only; no production language packs are shipped.
- Theme Studio does not yet implement gradients, user image backgrounds, full font management, theme import/export, or a marketplace client.
- The visual treatment is tinted translucent surfaces, not true live backdrop blur/refraction. Platform-level blur and background sampling still need design and performance validation.
- iOS automatic clipboard history, image/screenshot ingestion, sticker creation, translation tools and writing assistance are not implemented. iOS clipboard capabilities are not equivalent to Android and must be user-driven.
- Android clipboard history stores opted-in clipboard text locally. The filtering is heuristic and cannot guarantee detection of every sensitive string; the user can inspect and clear it.
- Custom user-imported sound packs and per-key sound mapping are not implemented. Current feedback uses platform-standard sound APIs.
- Sticker creation, emoji search/categories/skin-tone UI, alternate emoji artwork packs and Theme Marketplace UI are not implemented.
- Supabase schema and Edge Function source are not deployed to a YORY Supabase project. The only connected project discovered during setup was inactive and was not modified.
- The current execution environment does not include Gradle/Android SDK or Xcode/iOS SDK, so native builds could not be run here. CI workflows are committed but a successful Actions run has not been confirmed.

## Status vocabulary

- `implemented_unverified`: source code is present but has not passed target-device QA.
- `partial_implementation`: a limited end-to-end slice exists.
- `prototype`: narrow baseline, not release quality.
- `schema_scaffold`: database/API contract only.
- `ui_scaffold_only`: visible control, not connected to final platform behavior.
- `not_implemented`: no implementation yet.
- `production_ready`: only after build, tests, privacy review and real-device QA.
