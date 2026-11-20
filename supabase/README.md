# Supabase backend

Supabase is optional for core typing. The keyboard must keep working when the network is unavailable or account services are not configured.

## Confirmed development project

The YORY KEYS development project has been confirmed and is active:
- Project reference: `qyiipvwjwojrikokwjgz`
- Project URL: `https://qyiipvwjwojrikokwjgz.supabase.co`

The mobile app uses Supabase Auth with the project's publishable key. The publishable key may be present in mobile builds; it does not grant administrative access. All user-owned records must be protected with RLS.

## Local development

Use the currently installed Supabase CLI and consult `supabase --help` before running commands. Supabase CLI v3 changes local/hosted workflow defaults; inspect the current CLI docs before any hosted mutation.

Recommended workflow:
1. Start a local Supabase stack for future schema changes.
2. Apply repository migrations to the local database.
3. Run RLS/security tests and the Supabase advisors.
4. Test Edge Functions locally with an authenticated user.
5. Review and deploy any new migrations deliberately to the confirmed YORY development project.

Migrations 0001–0006 are already deployed to this development project. Nine public tables have RLS enabled, the 24 database/RLS access-contract checks pass, and the Security Advisor reports zero findings. The theme-publishing Edge Function is still source-only.

## Secrets

- Mobile clients may contain only the Supabase project URL and publishable key.
- Never place a service-role or secret key in Android, iOS, or a public repository.
- Store Edge Function secrets in the Supabase project environment, never in source control.

## Data boundary

Supabase stores user accounts, theme documents, public theme metadata, explicit non-content preferences, encrypted backup ciphertext, and language-pack manifests. It never receives raw keystrokes, live prediction context, clipboard contents, screenshot data, or plaintext personal dictionaries.
