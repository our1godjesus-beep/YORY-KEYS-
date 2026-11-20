# YORY KEYS Roadmap

## Milestone A — Real keyboard baseline
- Android IME install/build/device QA.
- iOS Xcode project generation, signing setup, extension install/device QA.
- Arabic/English input, editor actions, secure-field handling.
- Native preview and local theme persistence.

## Milestone B — Theme platform
- Shared theme parser, schema validation, version migration.
- Full color, gradient, key geometry, font and surface controls.
- Native tinted-glass rendering adapters.
- Export/import and theme presets.
- Sound and haptics wired to platform-supported APIs.

## Milestone C — Typing intelligence
- Local tokenization and language detection.
- Quality-tested word lists and autocorrect rules.
- N-gram baseline, then compact quantized neural models if product quality justifies them.
- Personal dictionary with opt-in learning and clear/reset controls.
- Swipe typing only after stable core input behavior.
- Per-language quality metrics and regression corpus.

## Milestone D — Productivity features
- Explicit clipboard history with expiry and sensitive-data filtering.
- Emoji search, recents, categories and contextual suggestions.
- Host-app sticker creation and supported insertion/share flows.
- Translation and writing assistance as optional tools.

## Milestone E — Cloud features
- Supabase authentication.
- Theme marketplace and ratings.
- User preferences sync.
- Client-encrypted dictionary backup.
- Signed language-pack manifests and downloads.
- Offline sync queue, retries and conflict resolution.

## Milestone F — Global release
- Expand beyond initial Arabic/English baseline.
- Verify each language's layout, tokenizer, dictionary, prediction, autocorrect and RTL/IME behavior.
- Publish only the supported feature level for each language.
- Performance, privacy, security and store review.
