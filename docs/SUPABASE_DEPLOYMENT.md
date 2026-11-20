# Supabase Deployment Runbook

## Connected development project

The confirmed YORY KEYS development project is active:
- Project reference: `qyiipvwjwojrikokwjgz`
- Project URL: `https://qyiipvwjwojrikokwjgz.supabase.co`

The Android/iOS client config templates contain only the publishable key for this development project. The real local config files are ignored by Git. Replace these values when switching to another environment.

## Client configuration

- Android: copy `android/supabase.local.properties.example` to `android/supabase.local.properties`.
- iOS: copy `ios/YORYKeys/SupabaseConfig.plist.example` to `ios/YORYKeys/SupabaseConfig.plist`, then run `xcodegen generate` from `ios/`.
- Mobile clients may contain only the project URL and publishable key.
- Never place an `sb_secret_` key or legacy `service_role` key in Android, iOS, or a public repository.
- Keep Android SDK settings in `android/local.properties`; Supabase credentials use the separate file.
- The keyboard must not need Supabase configuration to type offline.

## Database rollout

The repository migrations define profile rows, public/private themes, preferences, theme favorites and ratings, encrypted dictionary backup ciphertext, language-pack manifests, sticker-pack metadata, Storage buckets, and RLS policies. Review and deploy these in order to the confirmed development project. Run the Supabase security and performance advisors after deployment.

## Data contract

Allowed cloud data: accounts, user-published themes, theme metadata, non-content preferences, encrypted dictionary ciphertext, signed language-pack manifests, and aggregate opt-in events.

Forbidden cloud data: live keystrokes, passwords, selected editor context, prediction prompts, clipboard contents, screenshots, or plaintext personal dictionaries.
