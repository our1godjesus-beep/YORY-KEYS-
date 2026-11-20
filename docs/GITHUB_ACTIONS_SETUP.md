# GitHub Actions setup for YORY KEYS

## Add the two client configuration secrets

1. Open the YORY-KEYS- repository on GitHub.
2. Open Settings.
3. Under Security, open Secrets and variables, then Actions.
4. Select New repository secret.
5. Add SUPABASE_URL with the project URL:
   https://qyiipvwjwojrikokwjgz.supabase.co
6. Add SUPABASE_PUBLISHABLE_KEY with the project's publishable key (the sb_publishable_... value).

Do not add a service_role key, sb_secret key, database password, signing private key, or other privileged credential as a mobile build setting.

## What the workflows do

- Android CI writes android/supabase.local.properties during the runner job.
- iOS CI writes ios/YORYKeys/SupabaseConfig.plist before XcodeGen runs.
- Both generated files are ignored by Git and are not committed.
- The Android and iOS host apps receive only the project URL and publishable key.
- If the secrets are missing, CI can still compile the apps, but account authentication and cloud calls will show as unconfigured.

The publishable key is designed to be present in a client application. It is not an authorization boundary: enforce user access in Supabase RLS and Edge Functions. Never trust the client for privileged actions.

## Local development

Android:
    cp android/supabase.local.properties.example android/supabase.local.properties

iOS:
    cp ios/YORYKeys/SupabaseConfig.plist.example ios/YORYKeys/SupabaseConfig.plist
    cd ios && xcodegen generate

Replace example values locally as needed. Keep the generated files untracked.

## Encryption and typing performance

The keyboard typing path must remain offline and must not call Supabase, perform disk I/O, or decrypt a file per keypress.

- Android auth sessions use AES-256-GCM through Android Keystore; the raw key is non-exportable from the Keystore.
- iOS auth sessions are stored in Keychain with ThisDeviceOnly accessibility.
- Android personal dictionary persistence uses authenticated AES-GCM encryption and is cached in memory after unlock/load. Disk encryption and writes happen outside the input hot path.
- Any future cloud dictionary backup must be encrypted on-device before upload. The server stores ciphertext only; encryption keys must never be sent to Supabase.
- Use unique random nonces for every AES-GCM encryption operation. Never reuse a nonce with the same key.
- Treat corrupted ciphertext or authentication-tag failures as a reason to discard the local cached session/data safely, not as plaintext fallback.
- Keep crypto work to session restore, explicit sync, and background persistence. Do not add per-keystroke encryption or network work.

Security hardening must be benchmarked on physical low-, mid-, and high-tier devices. Measure keyboard open latency, key-to-render latency, memory, battery, and background persistence before and after each change. Do not claim that encryption has zero cost; the goal is to keep it outside the real-time typing path so normal typing is not slowed.

## Build artifacts

Android CI uploads a debug APK. iOS CI uploads an unsigned Simulator app bundle. The iOS artifact is not an IPA and cannot be installed on a physical iPhone. Physical iPhone builds require Apple signing credentials and a separate signed archive workflow.
