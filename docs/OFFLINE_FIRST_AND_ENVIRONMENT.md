# YORY KEYS — offline-first contract and local environment

## Product identity
- Product and keyboard display name: YORY KEYS
- Companion settings experience: YORY Studio
- Tagline: Your words. Your flow.
- Stable application identifiers: Android/iOS host app app.yory.keys; iOS keyboard extension app.yory.keys.keyboard. Do not change these for a visual rebrand because they identify existing installs and local Keychain/App Group data.

## What must work offline
- Launch YORY Studio and choose Continue Offline without creating an account.
- Enable and select the keyboard in operating-system settings.
- Type, switch between bundled layouts, use local symbols/emoji controls, and apply locally stored themes.
- Local preferences and the on-device personal dictionary.
- Import/export theme files and perform local account sign-out.
- No network calls, telemetry, analytics, crash-upload SDKs, or remote requests are allowed from the keypress/input hot path.

The Android host app may restore a still-valid encrypted session from local storage without network access. An expired session is retained locally; the app does not attempt a refresh-token request during startup. iOS follows the same rule with Keychain. Account creation, explicit sign-in and cloud-only features require internet. If a session expires while offline, the user can continue in offline mode and sign in again after reconnecting.
When connectivity becomes available, YORY Studio may show a small, optional in-app invitation explaining that account registration unlocks upcoming cloud services. It must never block keyboard activation, typing, local settings or offline use. The invitation is dismissible, has a clear "Not now" choice, respects a locally saved dismissal/preference and uses a conservative frequency cap. Provide a separate way to revisit registration from YORY Studio.
Do not request operating-system notification permission just to display this in-app invitation. Any future push/service alerts require a separate explicit opt-in and OS permission where applicable. Do not claim planned services are already available, and never transmit typed text, editor context, clipboard content or plaintext personal dictionary data to promote registration. Registration remains optional; core keyboard functionality never depends on it.

## Local environment setup
1. Copy .env.example to .env in the repository root.
2. Set only SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY.
3. Run: python3 tools/configure_local_env.py
4. Build Android from android/, and build iOS from ios/ on macOS with Xcode and XcodeGen.

The script generates android/supabase.local.properties and ios/YORYKeys/SupabaseConfig.plist. Both generated files are ignored by Git. Keep Android SDK paths in android/local.properties; do not put Supabase values there. The root .env is ignored by Git. The tracked .env.example contains placeholders only.

Without .env, builds remain possible with empty cloud configuration. Local keyboard and Studio functionality must not be gated on configuration.

## Credential policy
Only a Supabase publishable client key may be embedded in a mobile app. It is not an administrative secret. Never put a Supabase secret key (sb_secret_), legacy service_role key, database password, signing key, or service credential in .env.example, mobile binaries, or the Git repository. Store privileged server-side credentials only in the server environment or protected GitHub Actions secrets, never in client builds.

RLS remains mandatory for every user-owned table. Client-key visibility is not authorization: every query and write must still be protected by database policies and authenticated user identity.

## Performance and privacy gates
- No per-keypress encryption, filesystem writes, or network traffic.
- Keep local preference and dictionary operations off the main/input thread where possible.
- Only complete-word local learning in allowed ordinary text fields; exclude password, numeric, email and URL fields.
- Do not transmit typed text, editor context, clipboard contents, OTPs, payment information, screenshots, or plaintext personal dictionaries.
- Reconnect and sync must be explicit and tested separately from offline typing.
- Validate airplane-mode startup, typing, theme changes, app restarts, low storage, expired sessions, and sign-out on physical devices before any production claim.

## App icon pipeline
branding/LOGO7.jpeg is the single source of truth. Android uses the original image as an app resource. The iOS 1024px App Store icon is generated on macOS by running:
swift tools/generate_ios_app_icon.swift branding/LOGO7.jpeg ios/YORYKeys/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png
The generated PNG is a build artifact and is not committed; CI creates it before XcodeGen and Xcode build.
