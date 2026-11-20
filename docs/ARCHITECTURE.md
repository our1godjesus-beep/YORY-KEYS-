# Technical Architecture

## High-level components

```text
Android Native IME ─────┐
                        ├── Shared contracts: theme schema, language-pack manifest,
iOS Keyboard Extension ─┘   privacy rules, test vectors
          │
          ├── Local storage (platform-native, encrypted where appropriate)
          ├── On-device prediction adapter
          └── No live typing network requests

Host Apps (Android / iOS)
          │
          └── Explicit Sync & Download Coordinator
                    │
                    ├── Supabase Auth
                    ├── Postgres + RLS
                    ├── Storage (theme assets / signed language packs)
                    └── Edge Functions (validation, quotas, signed URLs)
```

## Platform boundaries

### Android
- Implement keyboard using `InputMethodService`; never fake keyboard input with an overlay.
- Use `InputConnection` for composing text and committing text.
- Respect `EditorInfo` input classes, variation flags, action IDs, and password variations.
- Host/settings UI may use Jetpack Compose; IME rendering should be optimized for startup and low allocation.
- Use DataStore/Room as appropriate; avoid loading large language models into memory until needed.

### iOS
- Implement a real Custom Keyboard Extension using `UIInputViewController`.
- Host app and extension share only the data explicitly placed in an App Group container.
- iOS may restrict network access, full access, and access to text before the insertion point. The keyboard must still work with restricted access.
- The host app owns account, download, and sync flows. Do not assume background execution is guaranteed.

## Theme contract

The versioned JSON theme schema in `shared/specs/yory-theme.schema.json` is the cross-platform source of truth. Each platform maps supported tokens to native rendering capabilities. Unsupported effects must degrade gracefully and must not break layout.

## Prediction engine boundary

Expose a stable adapter:
- `loadLanguagePack(languageTag)`
- `suggest(context, composingWord, limit)`
- `correct(token, languageTag)`
- `learnAcceptedToken(token, languageTag)`
- `clearPersonalLearning()`

The initial implementation may return no suggestions until a tested local model/dictionary is integrated. Do not ship fake or hard-coded "AI" claims. Keep model inference on-device.

## Sync protocol

1. User enables a specific sync feature and signs in.
2. Host app serializes only that feature's allowed data.
3. Sensitive dictionary backups are encrypted on-device before upload.
4. Sync jobs are queued locally and retried safely.
5. Server validates JWT, ownership, schema, size, and rate limits.
6. Client verifies downloaded pack hashes/signatures before activation.
7. Live keystrokes, clipboard contents, and prediction context are excluded.

## Quality gates

- IME can be selected and used in real text fields.
- No crashes during rapid typing, rotation, process recreation, or keyboard switching.
- Secure fields disable learning and clipboard features.
- Arabic RTL and mixed Arabic/Latin punctuation are tested.
- Theme changes apply to the real keyboard, not only preview.
- Offline mode has no hidden dependency on Supabase.
- Accessibility contrast and text scaling are checked.
