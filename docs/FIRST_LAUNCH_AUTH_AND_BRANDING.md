# YORY KEYS: first-launch experience, account flow, and branding

## Separation of the two experiences

The **YORY Studio settings app** and the **YORY keyboard extension / Android IME** are intentionally separate.

- The settings app opens with a one-time animated Glass intro.
- After the intro, a new user sees Create Account first. Existing users can switch to Sign In.
- A saved Supabase session is restored on later launches. A signed-in user opens the settings home; Account contains Sign Out.
- The keyboard is not gated by this flow. After the user enables and selects YORY KEYS in operating-system keyboard settings, it is available in text fields and remains usable offline. It does not open the intro or require account sign-in.

## Logo asset required from product owner

The intro loader expects the supplied brand asset at:
- Android: `android/app/src/main/assets/LOGO7.jpeg`
- iOS: add the source image as `LOGO7.jpeg` to the YORYKeys app target resources (or add it to the asset catalog as `LOGO7`).

The file named `LOGO7.jpeg` was not present in the available conversation files when this pass was implemented. Until the real file is added, Android shows a temporary Y mark and iOS falls back to a keyboard symbol. The code already looks for the real asset.

## Supabase mobile configuration

The client only uses the Supabase **Project URL** and **publishable key**. Never put a Supabase secret/service-role key in a mobile app.

### Android
1. Copy `android/local.properties.example` to `android/local.properties`.
2. Replace `YOUR_PROJECT_REF` and `sb_publishable_YOUR_KEY` with the YORY project's Project URL and publishable key.
3. Sync/build Android. `local.properties` is ignored by Git.

### iOS
1. Copy `ios/YORYKeys/SupabaseConfig.plist.example` to `ios/YORYKeys/SupabaseConfig.plist`.
2. Replace the two placeholder values with the YORY project's Project URL and publishable key.
3. Add `SupabaseConfig.plist` to the **YORYKeys app target** as a bundle resource. The real plist is ignored by Git.

## Auth behavior

- Signup uses Supabase Auth email/password and sends the display name as user metadata.
- When Supabase requires email confirmation, the app explains that the user must verify their email and then sign in.
- Sign-in restores a session; Android protects the stored session with Android Keystore AES-GCM, and iOS stores it in Keychain.
- The app does not write typed text or keyboard input to Supabase. Auth requests contain only the account fields needed by Supabase Auth.
- The app currently displays a configuration error rather than pretending registration succeeded if URL/key configuration is missing.

## Backend binding still requiring owner confirmation

A Supabase project is visible on the connected account, but its name is generic and its public schema is empty. It has not been confirmed as the YORY production project. Do not deploy YORY tables or bind production mobile builds to it until the owner confirms the project reference. The mobile clients deliberately use local configuration placeholders rather than hard-coding an unconfirmed project.

## Keyboard reference layout

The keyboard's main letter rows and language-specific order remain separate from app launch/auth. English QWERTY, German QWERTZ and Arabic are selectable basic layouts. The keyboard's spacing, height, corner radius, font size, and font family are customizable through the local theme. Basic Android settings now expose number-row placement, cursor arrows, long-press duration, long-press symbols, accent options, key popups, large key text, sound volume, and haptic duration.

The settings UI also exposes several reference sections whose advanced implementation is not yet complete on both platforms (for example voice dictation, search, iOS clipboard history, and some advanced emoji catalog options). Their screens must not imply those features are active until implemented and tested.
