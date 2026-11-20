# Supabase backend

Supabase is optional for core typing. The keyboard must keep working when no Supabase project is configured or the network is unavailable.

## Local development

Use the currently installed Supabase CLI and consult `supabase --help` before running commands. Supabase CLI v3 changes local/hosted workflow defaults; inspect the current CLI docs before any hosted mutation.

Recommended workflow:
1. Start a local Supabase stack.
2. Apply repository migrations to the local database.
3. Run the RLS/security checks.
4. Test Edge Functions locally with an authenticated user.
5. Only then link a verified YORY hosted project and apply migrations.

## Hosted project safety

Do not link this repository to an unrelated project. One connected Supabase project was visible during this session and its status was ACTIVE_HEALTHY. Its generic project name does not prove it is intended for YORY, so no hosted schema or data was changed. Confirm the project identity before deployment.

## Secrets

- Mobile clients may contain only the Supabase project URL and publishable key.
- Never place a service-role key or secret key in Android, iOS, or a public repository.
- Store Edge Function secrets in the Supabase project environment, never in source control.

## Data boundary

Supabase stores user accounts, theme documents, public theme metadata, explicit non-content preferences, encrypted backup ciphertext, and language-pack manifests. It never receives raw keystrokes, live prediction context, clipboard contents, screenshot data, or plaintext personal dictionaries.
