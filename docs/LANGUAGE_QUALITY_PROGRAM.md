# Language Quality Program — 120-Language Target

The 120-language catalogue is a target, not a claim that 120 high-quality keyboards or prediction models are already shipped.

## Capability levels

Each language receives per-platform capability flags:
- layout: a tested keyboard layout exists.
- dictionary: a local lexicon is bundled or downloadable.
- completion: prefix/word completion is available.
- autocorrect: correction is enabled and validated.
- next_word: context-sensitive next-word ranking exists.
- personal_learning: local user dictionary/learning works safely.
- swipe: gesture typing has a validated decoder.
- emoji_search: localized emoji search terms are available.

A language must not inherit another language's capability simply because the UI can switch to its language tag.

## Tier policy

- Tier 1: Arabic, English, French, Spanish, Hindi, Simplified Chinese, German, Portuguese, Turkish, Urdu.
- Tier 2: languages with a mature, tested layout and dictionary but limited prediction.
- Tier 3: catalogue entries awaiting layout, test corpus, dictionary and model quality.

The exact language tag, script, direction, input layout, tokenization, data version and capability state are stored in shared/specs/language-catalog.json.

## Quality gates

Before enabling autocorrect for a language:
- Build a human-reviewed representative test set.
- Measure word error rate and false-correction rate.
- Include proper nouns, names, dialect terms, code-switching, numbers, URLs and mixed punctuation.
- Verify undo/revert behavior.
- Provide a per-language autocorrect off switch.

Before enabling next-word prediction:
- Evaluate top-1/top-3 usefulness and irrelevant suggestion rate.
- Ensure suggestions do not leak previous sessions or other users' text.
- Test offline cold start and warm latency.
- Enforce memory limits in the keyboard process.

Before enabling swipe:
- Evaluate word accuracy across short/long words, accents, Arabic morphology and adjacent-key paths.
- Keep tap typing reliable when swipe decoding fails.

## Performance targets (engineering targets, not current measurements)

- Keyboard first paint: p95 under 100 ms on a representative midrange device after process warm-up; p95 under 250 ms cold start.
- Suggestion refresh: p95 under 50 ms for dictionary lookup on device.
- No network call on the keystroke hot path.
- Model loads asynchronously and only for the active language.
- A memory-budget test is required for every platform/device tier.

These targets must be measured on actual release hardware; never report them as achieved until CI/device telemetry confirms them.

## Language pack format

Each signed pack manifest records:
- BCP-47 language tag and script.
- Pack schema version and model version.
- Layout identifiers and RTL/LTR direction.
- Tokenizer/normalizer version.
- Lexicon, n-gram and model asset paths.
- SHA-256 checksums and signature key ID.
- Supported capabilities.
- Minimum app version and release date.
- License and attribution metadata.

Clients verify manifest signature, checksum, file size, compatibility and free storage before atomically activating a new pack. If verification fails, keep the last known-good pack.

## Data and model governance

- Use only licensed/publicly permissible datasets or original data with documented provenance.
- Keep a dataset/model card: source, license, language variety, date, limitations, evaluation results and known bias.
- Never train or personalize from users' live typing in the cloud.
- Optional personal learning remains local and can be reviewed or erased.
- Do not scrape private conversations or upload clipboard data to build a corpus.
