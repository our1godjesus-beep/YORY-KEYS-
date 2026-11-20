# YORY KEYS

**Your words. Your flow.**

YORY KEYS is a native keyboard product for Android and iOS, designed around the Tinted Glass aesthetic, deep personalization, on-device typing intelligence, and privacy-first offline operation.

> Repository status: active engineering foundation. The feature matrix is the authority for what is implemented, partial, planned, or not yet shipped.

## Product promise

- Real system keyboard: Android InputMethodService; iOS UIInputViewController custom keyboard extension.
- YORY Studio: a keyboard design studio with theme presets and granular controls.
- Local-first typing: no live keystrokes, passwords, prediction context, or clipboard payloads are uploaded.
- Optional cloud: Supabase for accounts, public themes, preference sync, encrypted dictionary backup, and signed language-pack delivery.
- Honest language support: 120 languages are a target catalogue, not 120 completed production layouts.

## Build

Android: use JDK 17, Android SDK platform 35, Gradle 8.9:
    cd android
    gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest

iOS: use macOS, Xcode and XcodeGen:
    cd ios
    brew install xcodegen
    cp YORYKeys/SupabaseConfig.plist.example YORYKeys/SupabaseConfig.plist
    xcodegen generate
    open YORYKeys.xcodeproj

## Offline-first behavior

YORY KEYS is designed so that the keyboard, typing, bundled language layouts, local themes, personal dictionary and local preferences do not depend on a network connection. The first-run screen offers Continue offline; account creation and sign-in are optional and require internet. Cloud sync, account-only services and future online catalogues also require an explicit connection. Losing internet must never disable the keyboard or erase local settings.

The Android keyboard runs as its own system input method after the user enables and selects it in device settings. The iOS keyboard extension remains local; iOS system permissions and extension limits still apply. No typed text, passwords, OTPs, payment fields or clipboard contents are sent to YORY servers.

## Environment and Supabase

Copy .env.example to .env, fill in the Supabase project URL and publishable client key, then run:

    python3 tools/configure_local_env.py

The script writes ignored platform config files for local builds. With no .env, local keyboard and Studio features remain available; account/cloud actions show that internet/configuration is required.

The confirmed YORY KEYS development project is configured through GitHub Actions secrets. Supabase is optional for typing: the keyboard must remain useful offline.

For GitHub-hosted builds, add these two repository Actions secrets:
- SUPABASE_URL
- SUPABASE_PUBLISHABLE_KEY

In GitHub, open repository Settings → Secrets and variables → Actions → New repository secret. Enter the Supabase project URL and its publishable key. Android and iOS workflows generate their ignored local configuration files from these secrets during each CI build. If the secrets are absent, the app still compiles, but account sign-in and cloud features remain unconfigured. The publishable key is intended for client apps; never use an sb_secret_ or service_role key here.

Detailed steps and the encryption/performance rules are in docs/GITHUB_ACTIONS_SETUP.md.

Never put a service_role key, sb_secret key, database password, or other privileged credential in a mobile app or repository file. A privileged key, if ever needed by a server-only job, must be stored separately as a GitHub Actions secret and never shipped in an app.

## Repository map

- android/ — Kotlin YORY Studio, system InputMethodService, adaptive icon, theme editor and offline word completion.
- ios/ — SwiftUI YORY Studio, UIKit keyboard extension, generated App Store icon and XcodeGen project.
- branding/ — canonical owner-supplied LOGO7.jpeg source used by both platforms.
- shared/specs/ — theme schema, feature status, 120-language catalogue.
- supabase/ — migrations and Edge Functions.
- docs/ — product, privacy, architecture, QA, iOS limitations and roadmap.
- .github/workflows/ — Android and iOS CI workflows.

## Privacy rules

Never upload live typed text, editor context, passwords, OTPs, payment information, secure-field content, clipboard contents, screenshots, or plaintext personal dictionaries. Optional sync must be explicit, authenticated, scoped, and protected by RLS. Typing must remain offline-capable.

## Specifications

- docs/MASTER_PRODUCT_SPEC.md
- docs/IOS_EXPERIENCE_STANDARD.md
- docs/LANGUAGE_QUALITY_PROGRAM.md
- docs/FEATURE_IMPLEMENTATION_MAP.md
- docs/TEST_PLAN.md
- docs/SECURITY_THREAT_MODEL.md
- docs/IMPLEMENTATION_STATUS.md
- docs/SUPABASE_DEPLOYMENT.md

Run repository contract checks locally with:
    python tools/validate_repository.py
