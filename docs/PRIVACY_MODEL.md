# YORY KEYS Privacy Model

## Default promise

Typing is a local operation. YORY KEYS does not send keystrokes, surrounding text, passwords, clipboard payloads, or prediction context to Supabase or another server.

## Local-only data

- Active theme and theme drafts.
- Installed dictionaries and language models.
- Personal dictionary and learning profile.
- Keyboard layout and accessibility settings.
- Android clipboard history, only when the user enables it: encrypted locally with AES-GCM and an Android Keystore key, capped at 50 items, unpinned items expire after 24 hours, pinned items remain until removed. OTP/PIN/payment-number pattern filtering is heuristic and cannot guarantee detection of every secret.
- Sticker drafts and locally selected images.

## Optional cloud data

Only after the user signs in and enables the relevant feature:
- Public/shared theme documents and preview images.
- Theme ratings and favourites.
- Sticker pack metadata or public packs.
- Non-content appearance preferences.
- Encrypted personal dictionary backup (ciphertext only; encryption must happen on-device).
- Signed language-pack manifests and downloadable model files.
- Anonymous aggregate product diagnostics only when separately opted in.

## Explicit exclusions

Never upload:
- Live typed text or predictive context.
- Passwords, OTPs, payment data or secure-field content.
- Clipboard contents or screenshots.
- Personal dictionary plaintext.
- Contact lists or location history.

## Platform permission behavior

- Android: detect secure editor input types and disable suggestions/learning/clipboard features in those fields. Follow Android clipboard access rules. Incognito mode pauses local suggestions, learning and clipboard capture for the current input session.
- iOS: work with Open Access disabled. Do not imply that the iOS extension can read arbitrary text or inspect every app. Incognito mode pauses local suggestions and learning; automatic clipboard history/background clipboard monitoring is not implemented.
- Explain each requested permission in context; do not ask for all permissions during first launch.

## User controls

Provide controls to pause local learning, clear learned words, clear clipboard history, remove local files, export personal data, unlink cloud sync, and delete the cloud account. These controls must be implemented before claiming the related feature is complete.


## Local personal-dictionary deletion
- Android exposes a confirmation action to clear the encrypted local dictionary. Clearing removes the encrypted file and its Android Keystore key; the opt-in preference remains unchanged.
- iOS stores personal words in the app's local App Group container so the host app can execute an explicit user-requested clear action. This remains local device storage and is not a network sync.
