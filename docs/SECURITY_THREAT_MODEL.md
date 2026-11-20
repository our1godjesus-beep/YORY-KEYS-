# Security and Privacy Threat Model

## Protected assets

- User-entered text and surrounding editor context.
- Passwords, payment details, one-time codes and authentication data.
- Clipboard entries and selected images.
- Personal dictionary and learning history.
- Account tokens, encrypted backup keys, theme uploads and sticker assets.

## Threat boundaries

- The IME/keyboard extension is a sensitive input component.
- The host app is a separate process with wider UI responsibilities.
- Supabase is an optional cloud boundary.
- Public theme assets are untrusted input and may contain malformed images or oversized metadata.
- Imported theme/audio/image files are untrusted input.

## Mandatory controls

1. Never transmit keystrokes, editor context, secure-field content or clipboard payloads.
2. Do not learn from secure fields; clear current prediction context on field change.
3. Keep network operations out of the per-keystroke path.
4. Store sessions in platform secure storage (Keychain/Keystore).
5. Encrypt personal dictionary backups on-device before upload; server stores ciphertext only.
6. Use publishable keys in clients; never embed service-role/secret keys.
7. Enable RLS on every exposed table and configure least-privilege table grants.
8. Use owner-scoped Storage paths and policies.
9. Validate theme JSON schema, file sizes, MIME types and image decoding before use.
10. Verify checksums and signatures for downloadable language packs.
11. Make sync opt-in and show exactly which data types are synced.
12. Support export, account deletion, local data clearing and revocation of cloud sync.

## Abuse and marketplace safeguards

- Rate-limit publish, rating and upload endpoints.
- Reject invalid schema versions and oversized payloads.
- Restrict theme previews to safe image types and size limits.
- Keep user-uploaded packs private until published/moderated.
- Add report, moderation, takedown and attribution processes before public marketplace launch.
- Do not permit theme metadata to contain executable code or scripts.

## Incident response

- Provide a kill switch for optional cloud features and a way to revoke compromised signing keys.
- Preserve the last known-good offline language pack if an update fails.
- Roll back a bad theme schema migration via explicit compatibility handling.
- Publish clear security contact and vulnerability-reporting instructions before public launch.
