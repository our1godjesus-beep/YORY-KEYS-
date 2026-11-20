# YORY KEYS: first-launch experience, account flow, branding, and Supabase setup

## Separation of the two experiences

The **YORY Studio settings app** and the **YORY keyboard extension / Android IME** are intentionally separate.

- The settings app opens with a one-time animated Glass intro.
- After the intro, a new user sees Create Account first. Existing users can switch to Sign In.
- A saved Supabase session is restored on later launches. A signed-in user opens the settings home; Account contains Sign Out.
- The keyboard is not gated by this flow. After the user enables and selects YORY KEYS in operating-system keyboard settings, it is available in text fields and remains usable offline. It does not open the intro or require account sign-in.

## Canonical logo asset (included in both app targets)

The original owner-supplied image is preserved byte-for-byte at branding/LOGO7.jpeg and copied to Android assets, Android launcher icon resources, and the iOS YORYKeys app target.
- Android: `android/app/src/main/assets/LOGO7.jpeg`
- iOS: add the source image as `LOGO7.jpeg` to the YORYKeys app target resources (or add it to the asset catalog as `LOGO7`).

The canonical logo is now included in both app packages, so the launch, account and launcher branding use the same image.

## Supabase development configuration

YORY KEYS is connected to the confirmed development Supabase project:
- Project reference: `qyiipvwjwojrikokwjgz`
- Project URL: `https://qyiipvwjwojrikokwjgz.supabase.co`

The example config files currently contain the project's **publishable** key for development. This is designed for mobile clients and is not an administrative secret. Replace it with the publishable key from the new project when switching environments. Never use an `sb_secret_` or legacy `service_role` key in the app.

### Android

1. Copy `android/supabase.local.properties.example` to `android/supabase.local.properties`.
2. The file contains `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`.
3. Android Gradle reads this separate file; keep Android Studio's `android/local.properties` (SDK path) untouched.
4. `android/supabase.local.properties` is ignored by Git.

### iOS

1. Copy `ios/YORYKeys/SupabaseConfig.plist.example` to `ios/YORYKeys/SupabaseConfig.plist`.
2. The local plist contains the same two values and is read by the YORYKeys host app.
3. Run `cd ios && xcodegen generate` after copying it so XcodeGen includes the plist in the generated app project.
4. `ios/YORYKeys/SupabaseConfig.plist` is ignored by Git.

### Auth and privacy

- Signup uses Supabase Auth email/password and sends the display name as user metadata.
- When Supabase requires email confirmation, the app explains that the user must verify their email and then sign in.
- Sign-in restores a session; Android protects the stored session with Android Keystore AES-GCM, and iOS stores it in Keychain.
- The app does not write typed text or keyboard input to Supabase. Auth requests contain only account fields needed by Supabase Auth.
- The app displays a configuration error rather than pretending registration succeeded if either URL or key is missing.
- A publishable key is intended to be visible in a mobile app. Access to future user data must still be protected by Row Level Security (RLS).

## Backend scope and rollout

The mobile sign-up/sign-in flow uses Supabase Auth. The repository also contains separate schema migrations for profiles, themes, preferences, storage policies, and encrypted dictionary backup metadata. Only deploy those migrations after verifying the target project and reviewing the security advisor results. Never upload raw keystrokes, prediction context, clipboard contents, or plaintext dictionaries.

## Keyboard reference layout

The keyboard's main letter rows and language-specific order remain separate from app launch/auth. English QWERTY, German QWERTZ and Arabic are selectable basic layouts. The keyboard's spacing, height, corner radius, font size, and font family are customizable through the local theme. Basic Android settings expose number-row placement, cursor arrows, long-press duration, long-press symbols, accent options, key popups, large key text, sound volume, and haptic duration.

The settings UI also exposes several reference sections whose advanced implementation is not yet complete on both platforms (for example voice dictation, search, iOS clipboard history, and some advanced emoji catalog options). Their screens must not imply those features are active until implemented and tested.
