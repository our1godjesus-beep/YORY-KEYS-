# Supabase Deployment Runbook

## Status
Migration source exists in this repository. It is not considered deployed until the correct YORY project is confirmed, a local reset succeeds, and the hosted migration history is verified.

A connected Supabase project was visible during this session with status ACTIVE_HEALTHY, but its generic name is not sufficient evidence that it belongs to YORY. No hosted database changes have been made.

## Safe local-first order
1. Install/check the current Supabase CLI using its official documentation; CLI v3 changes hosted/local defaults.
2. Start a local Supabase stack.
3. Apply repository migrations from 0001 through 0004 to the local database.
4. Run the pgTAP test suite under supabase/tests.
5. Run Supabase security and performance advisors against the local/staging project.
6. Verify Storage policies for public theme previews and owner-only sticker assets.
7. Confirm the target hosted project by project name and project reference inside the Supabase dashboard.
8. Only then link the repository and deploy migrations intentionally.
9. Re-run RLS tests, verify project settings, and confirm no typed text or clipboard payloads reach cloud services.

## Client configuration
- Use only the project URL and publishable key in Android/iOS.
- Keep secret/service-role keys in Edge Function secrets only.
- The keyboard must not need Supabase configuration to type offline.
- Do not place a production project reference or secret values into public source files.

## Data contract
Allowed cloud data: accounts, user-published themes, theme metadata, non-content preferences, encrypted dictionary ciphertext, signed language-pack manifests, and aggregate opt-in events.

Forbidden cloud data: live keystrokes, passwords, selected editor context, prediction prompts, clipboard contents, screenshots, or plaintext personal dictionaries.
