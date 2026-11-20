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
- Android personal word learning is opt-in and stored in an AES-GCM encrypted file under no-backup app-private storage, with its key in Android Keystore. Learning occurs only when a word is completed in ordinary text fields; password, numeric, email and URL fields are excluded. YORY Studio can disable learning or clear the dictionary. Persistence runs off the keyboard input thread, and the decrypted dictionary is cached in-process to keep suggestion lookup off the disk/decryption hot path.
- Optional Android clipboard text history stores up to 50 local entries and offers quick insert/clear. It does not ingest screenshot/image items. Filtering is heuristic and not guaranteed to identify every sensitive string.

### iOS
- A SwiftUI host app and UIKit custom keyboard extension are generated through XcodeGen.
- The iOS keyboard supports basic English/Arabic layouts, number/symbol modes, shift, delete, space, return, next-keyboard switching, an emoji strip, a small offline prefix-suggestion list, optional personal word learning, and input-trait-aware behavior.
- iOS Studio exposes presets, color pickers for background/key/text/accent/pressed key, glass opacity, corner radius, key spacing, letter size, key shape, sound/haptic toggles, language selection, and .yorytheme import/export.
- The iOS keyboard renderer reads and applies theme colors, pressed-key color, shape, spacing, font size, corner radius, opacity, sounds and haptic preferences.
- The iOS host app exposes a personal-learning opt-in through its App Group preferences; the keyboard extension stores learned terms in its own extension-private UserDefaults store. The keyboard's ••• tools row can clear the learned-word store. The extension does not upload these terms.
- Automatic capitalization respects the host text object's \`autocapitalizationType\`.
- The extension declares \`RequestsOpenAccess = true\) so iOS can offer the system Allow Full Access option. This is user-controlled, not automatic. Basic typing remains available without Full Access; iOS keyboard click audio is only invoked when \`hasFullAccess\` is true.
- No code in the keyboard typing path sends keystrokes, passwords, surrounding text, or clipboard payloads to a server.

### Backend source
- Supabase migration source defines profiles, themes, ratings, encrypted dictionary-backup ciphertext storage, language-pack manifests, preferences, favorites, download events and sticker-pack metadata.
- RLS policies, explicit least-privilege client grants and pgTAP contract tests are committed.
- A JWT-authenticated theme publishing Edge Function is present in source.
- The owner confirmed the active Supabase development project (`qyiipvwjwojrikokwjgz`). Repository migrations 0001–0006 have been applied successfully. Nine public tables are present with RLS enabled; theme-preview and private sticker Storage buckets/policies are configured. Production use still requires separate release review.

### Validation
- GitHub Actions repository-contract validation has passed on recent commits, checking JSON contracts, 120 unique language tags, feature status values and basic secret-pattern scanning.
- Android CI passed on 2026-11-20 for commit `8f61eb36518c9f4a6f5511268c9b860a927ef4dd`, including encrypted local personal learning, background persistence, cached dictionary lookup and Android debug APK artifact upload.
- iOS CI passed on 2026-11-20 for commit `bd068d6170ea27538fb21cbf248ba0170087e92f`, including the native host app, keyboard extension and iOS Simulator app artifact upload.
- Repository contract validation passed on commit `8f61eb36518c9f4a6f5511268c9b860a927ef4dd`, confirming the 120-language catalogue and feature status contracts.
- Successful CI builds are not a substitute for installation and testing on physical phones.

## Not yet production-ready

- A debug APK artifact is generated by Android CI and can be sideloaded for development testing; it is not a signed production/release build.
- An unsigned iOS Simulator .app bundle is generated by iOS CI; it cannot be installed on a physical iPhone. No signed iOS archive, App Store submission, or physical-device installation has been completed.
- Neither platform has passed physical-device QA for touch accuracy, layout geometry, battery, memory, accessibility, app compatibility, audio/haptics or extension lifecycle.
- Current word suggestions combine tiny bundled prefix lists with optional local personal learning. Neural next-word prediction, robust language-specific autocorrect, multilingual language identification and swipe typing are not implemented.
- Only basic English and Arabic layouts exist. The other 118 language entries are catalogue-only; no production language packs are shipped.
- Theme Studio does not yet include a production gradient editor, imported image backgrounds, full supported-font browser, multi-theme library management, marketplace client, or a fully validated live wallpaper blur/refraction renderer.
- Glass currently uses tinted translucent surfaces and subtle borders. True live backdrop blur/refraction must be implemented and measured separately on each OS/device class.
- iOS clipboard history, automatic screenshot/image ingestion, sticker creation, translation tools and writing assistance are not implemented. iOS clipboard access is not equivalent to Android and must remain user-driven.
- Android clipboard filtering is heuristic and cannot guarantee detection of all OTP, payment or otherwise sensitive text. It is local-only and user-controlled.
- Custom imported sound packs, per-key sound mapping, advanced haptic tuning, emoji search/categories/skin-tone picker and alternate emoji artwork packs are not complete.
- Sticker creation, creator profiles, marketplace moderation, theme reporting and public theme discovery UI are not implemented.
- The six Supabase schema migrations are deployed to the confirmed YORY development project. The 24 database/RLS contract conditions pass and the Security Advisor currently reports zero findings. Performance Advisor currently reports only unused-index INFO notices on this new, low-traffic project. The JWT-authenticated theme-publishing Edge Function remains source-only and has not been deployed or tested end-to-end.
- Personal dictionary cloud backup/sync is not implemented. Any future backup must be explicitly opt-in, encrypted on-device before upload, and stored as ciphertext only.
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

1. Test Create Account, email confirmation, Sign In, session refresh and Sign Out on physical Android and iPhone devices.
2. Test the native keyboards on physical Android and iPhone devices.
3. Complete keyboard input behavior, accessibility and layout geometry.
4. Build the full theme editor and ensure preview/live rendering parity.
5. Expand personal dictionary QA and add quality-evaluated offline prediction.
6. Add emoji search, sticker creation, clipboard controls and writing tools within platform rules.
7. Continue Supabase Auth/device testing, then implement and deploy the theme-publishing Edge Function only after endpoint-specific tests.
8. Add signed language packs one language at a time after quality and licensing checks.


## Offline-first and canonical branding update (2026-11-20)

- YORY KEYS is the product and keyboard display name; YORY Studio is the companion settings experience. The stable app identifiers remain app.yory.keys and app.yory.keys.keyboard.
- First launch includes an explicit Continue Offline path. Local keyboard use, bundled layouts, theme settings and on-device preferences are not gated on account creation.
- Account registration/sign-in and any cloud-only service require internet. Expired sessions are preserved locally; opening the app no longer attempts a refresh-token request.
- Sign-out clears the locally stored session and does not need a network connection.
- The canonical owner-provided logo is preserved byte-for-byte in branding/LOGO7.jpeg; Android and iOS app resources use the same source image. Platform launcher icons are generated from that image.
- A root .env.example and local config generator create ignored Android/iOS Supabase configuration files. No privileged Supabase credentials belong in client configuration.
- These changes require fresh CI and physical-device airplane-mode QA before being treated as validated.

## Premium Glass UI update (2026-11-20)

- Applied a unified dark premium-glass visual treatment to Android and iOS keyboard surfaces, including translucent key fills, subtle glass highlights/borders, and pressed-state emphasis.
- Extended the glass styling into Android YORY Studio controls/previews and the iOS YORY Studio screen, preview keys, presets, and segmented controls.
- Preserved the existing keyboard row order, key labels, language layout selection, and core input actions; this visual pass does not intentionally change the main typing layout.
- Build/device verification for this visual update is pending. Native blur rendering differs by platform; Android currently uses translucent gradient surfaces rather than a true real-time blur behind each key.


## SwiftKey-reference glass settings pass (2026-11-20)

### Interface implemented
- Android and iOS app settings now use a dedicated settings landing page with the same broad section order shown in the reference screenshots: Account, YORY Rewards, Languages, Themes, Typing, Emoji, Rich input, Layout & keys, Sound & vibration, Privacy, Achievements, Help & Feedback, and About.
- Settings sections open their own detail pages. The Themes destination retains the existing theme editor and preview.
- Dark premium-glass surfaces, borders, translucent panels and light gradients are applied across settings and previews. iOS uses native material blur; Android uses translucent gradient glass panels and highlights.
- The keyboard keeps its existing main key-row geometry and typing actions. Language switching now includes English QWERTY, basic German QWERTZ, and Arabic layout on both platforms.

### Preferences connected
- Android: selected language, number-row preference, emoji shortcut visibility, word suggestions, auto-capitalization, personal dictionary, sound, haptics and optional clipboard history are connected to the relevant local settings/keyboard code.
- iOS: selected language, number row, emoji shortcut visibility, word suggestions, auto-capitalization, personal dictionary, sound and haptics are connected through the shared App Group settings.
- Voice typing, external search, and iOS clipboard history are not yet implemented; the settings screens identify these as unavailable rather than implying they work.
- Options such as accented-character behavior, arrow keys, long-press symbol variants, recent-emoji behavior and key pop-up behavior still need full interaction wiring. Do not describe those as completed until separately implemented and tested.

### Verification
- Android build and repository validation are running for the latest preference-wiring commits; earlier commits for the new settings navigation and initial German layout passed Android CI.
- iOS CI is still running for the latest settings and keyboard preference commits. The earlier iOS settings view compile errors were corrected and a subsequent iOS simulator build passed at commit 43dd0df6ed2b90b75b4beef7eb6d85889109442b.
- Physical-device validation is still outstanding. The iOS build remains an unsigned simulator build, not an installable signed iPhone release.


## First-run app experience and detailed keyboard settings (2026-11-20)

### Settings app flow (host app only)
- Android main activity and iOS SwiftUI app root now start with a first-launch Glass intro screen. The intro is persisted as completed after the user continues.
- The intro leads to Create Account by default, with a Sign In switch for existing users. Successful sign-in moves into the settings home; the Account page exposes Sign Out.
- Android Supabase Auth REST calls support email/password signup, sign-in, email-verification-required state, refresh and sign-out. The session is encrypted using Android Keystore AES-GCM.
- iOS Supabase Auth REST calls support the same basic flow. Session tokens are stored in Keychain.
- The **keyboard extension/IME is not gated by this flow**. It remains separately enabled through operating-system keyboard settings and is designed to keep working offline without an app session.

### Setup dependency
- Mobile clients read only the Supabase Project URL and publishable key. Android reads `android/supabase.local.properties` (ignored by Git), leaving Android Studio's `android/local.properties` for SDK settings. iOS reads `ios/YORYKeys/SupabaseConfig.plist` from the host-app bundle. Development example files are provided and the real config files are ignored by Git.
- The owner confirmed `qyiipvwjwojrikokwjgz` as the YORY development project. Six schema migrations have been applied; the mobile config templates contain only its development Project URL and publishable key. This connects the app's existing Supabase Auth REST implementation to the selected development backend. A real account signup/sign-in on a physical device has not yet been performed, so end-to-end auth still needs device verification.
- The requested `LOGO7.jpeg` asset was not present in the attached/current file sources. The intro code loads it from Android assets / the iOS app bundle and uses a temporary brand mark fallback until the real image is supplied.

### Reference keyboard controls
- Android settings expose number-row placement, long-press duration, long-press symbol/accent choices, key pop-up preview, larger key labels, keyboard height, key height/spacing/radius, font family, sound profile/volume and haptic mode/duration.
- Android IME applies number-row placement, cursor arrow keys, large-key text, key preview, timed long-press option menu, sound profile/volume and haptic preferences.
- iOS app and keyboard share number-row placement, arrow keys, key height, spacing, key shape, font size/family, long-press symbol/accent menu, large-key text and haptic intensity. iOS uses native long-press menu timing; the custom duration slider is not yet used by UIKit's menu trigger.
- Existing main key rows remain in place for English QWERTY and Arabic. German QWERTZ is now an additional basic layout. The 120-language catalogue remains broader than the language layouts currently implemented.

### Remaining limitations and verification
- Voice typing, multi-modal voice, online search/translation, advanced emoji catalogue/recent prediction, iOS clipboard history, some marketplace/theme-download flows and several account/cloud profile features are not implemented end-to-end.
- Android compilation passed at the earlier verified commits including the app launch and keyboard settings; the latest sound-profile change is undergoing a fresh Android CI run.
- iOS simulator build and repository validation passed for the latest iOS code at commit `6afea2fd`. The build is an unsigned simulator build, not a signed installable iPhone release.
- No physical Android/iPhone interaction test has been performed in this pass.


## Supabase development binding (2026-11-20)

- Confirmed project: `qyiipvwjwojrikokwjgz` (`https://qyiipvwjwojrikokwjgz.supabase.co`), region eu-west-1, status ACTIVE_HEALTHY.
- Android Gradle reads Supabase values from `android/supabase.local.properties`; `android/local.properties` is left for SDK configuration. iOS host app reads `ios/YORYKeys/SupabaseConfig.plist`. Both real local files are ignored by Git.
- Development templates for both platforms contain only the publishable API key. Never put the secret/service-role key into either mobile client.
- Migrations 0001–0006 were applied successfully on 2026-11-20. Nine public tables now exist, all with RLS enabled; storage buckets and scoped policies were created for public theme previews and private sticker assets.
- 24 of 24 database/RLS access-contract checks passed. Supabase Security Advisor returned zero findings after hardening the pre-existing `rls_auto_enable()` event-trigger helper. The event trigger remains active, but client roles no longer have direct EXECUTE permission.
- Performance Advisor no longer reports auth-policy initialization or missing foreign-key indexes; it reports only unused-index INFO notices expected on a new project before normal traffic exists.
- Android CI passed on the Gradle config change (run 44748310187); iOS CI passed on the iOS config-template change (run 44748319738); repository contract validation passed on the 0006 migration commit (run 44748691380).
- Supabase Auth signup/sign-in code is configured to use this project, but no live test account was created and no physical-device end-to-end registration was performed. Do not call auth production-verified until that test passes.
- The keyboard remains independent from account login and must remain usable offline. Raw typing, passwords, surrounding editor context, clipboard content and plaintext dictionaries are not sent to Supabase.


## Current implementation addendum — 2026-11-20

The following code work supersedes older "not implemented" bullets above where noted. This is not a production-ready sign-off.

### Additional completed source slices
- Android and iOS theme library detail views are present, with larger previews, apply/customize, favorites, duplication, rename and delete for user-saved themes. Built-in presets cannot be renamed or deleted.
- Android and iOS keyboard toolbars have local visibility/order customization. Keyboard extensions read those preferences when the keyboard appears.
- Android clipboard history is opt-in and encrypted at rest using AES-GCM with a key held in Android Keystore. It is limited to 50 items; unpinned items expire after 24 hours; pin/unpin, copy again, individual delete, undo-last-delete and clear-all are implemented. OTP/PIN/payment-number detection remains heuristic, not guaranteed.
- Incognito mode is session-scoped on both platforms. It suppresses local suggestions and word learning; Android also stops clipboard capture. It resets when a new keyboard input session begins.
- Android edge-to-edge/safe-drawing insets are wired in Studio screens. Physical navigation-mode verification remains open.
- The iOS keyboard includes context-aware return-key labels and editor-aware keyboard type layouts.
- Voice typing remains disconnected. iOS automatic clipboard history is not implemented; no background clipboard reading is claimed.

### Latest validation evidence
- Android CI passed on commit `df746dd31bf38e73542141dbc09e2076d7b98102` (run 44764347406), including `:app:assembleDebug` and `:app:testDebugUnitTest`. This commit includes Android clipboard manager, toolbar controls, incognito handling and edge-to-edge source.
- iOS CI passed on commit `aceda3f1ce1cee93c35be218c89c11e22766e546` (run 44764745857), including the app and keyboard extension build plus iOS Simulator app artifact generation.
- Repository contract validation passed on commit `bf442dc8e7f4966dc1ca1a949856c7a8d6f2e10a` (run 44765880468).
- iOS CI for `bf442dc8e7f4966dc1ca1a949856c7a8d6f2e10a` was in progress when this addendum was prepared; verify its result before treating the latest commit as build-verified.

### Still open
- No physical Android phone or iPhone was available for this source-editing pass. Keyboard activation, Arabic mixed-bidi, privacy transitions, safe-area/IME insets and Dynamic Type require real-device QA.
- The gallery preview is illustrative and is not yet a single shared renderer with the live keyboard.
- Android resize is key-height adjustment, not drag-to-resize width or floating position.
- Voice typing, swipe typing, context-aware autocorrect, neural prediction, emoji search/skin tones, sticker creation and iOS clipboard history remain unimplemented.
- Supabase auth and marketplace code need separate integration/security/release verification; app-store signing and release artifacts are not complete.


### Emoji picker source addendum — 2026-11-20
- Android and iOS keyboard extensions now render category-based emoji panels for Recent, Smile, People, Nature, Food, Travel and Symbols.
- Up to 24 recent selections are stored locally on-device and inserted through the active input connection.
- The People category includes user-selectable skin-tone modifiers for its starter hand/person set.
- Searchable emoji, favorites, full Unicode/ZWJ variant coverage and localization remain unfinished; these are not marked as complete.
- The emoji panel resets to the normal keyboard view when a new input session begins.


### Emoji favorites addendum — 2026-11-20
- Both keyboard extensions now have a Favorites category.
- Long-press an emoji to add/remove it. Up to 80 favorite entries are stored locally on-device.
- Favorite actions use a native long-press menu on iOS and an Android long-press handler. Physical-device touch and accessibility verification remains open.


### Clipboard search addendum — 2026-11-20
- Android Clipboard manager now includes case-insensitive search across stored clip text, with an empty-results state and a clear-search action.
- Search normalization and filtering are covered by JVM unit tests; Android UI validation remains in CI/manual QA.
