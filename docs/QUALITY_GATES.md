# Quality Gates

No platform release is accepted until the following gates are met.

## Build gates

- Android debug and release variants build from a clean checkout.
- Android unit tests pass in CI.
- iOS XcodeGen creates a deterministic project.
- iOS app and keyboard extension compile in CI without signing.
- Signed builds are tested on physical devices before release.

## Keyboard correctness

- Letters, numbers, symbols, shift, delete, space, and return work.
- Input actions adapt to editor type where supported.
- English and Arabic layouts work; mixed RTL/LTR text and punctuation are verified.
- Switching language does not lose or duplicate typed content.
- No unexpected keyboard dismissal or input duplication.

## Privacy and safety

- Password and secure fields disable local learning, suggestions and clipboard actions.
- No code path sends raw typed text to a server.
- Offline typing has no dependency on Supabase.
- No service-role/secret key is embedded in a mobile app.
- RLS is enabled and tested for every exposed user-data table.
- Full Access is optional and denied-state behavior is tested on iOS.

## Performance and accessibility

- Keyboard appears without visible jank on reference low/mid/high devices.
- No memory growth from repeatedly opening/closing the keyboard.
- 60 Hz and 120 Hz devices maintain smooth interaction where supported.
- VoiceOver/TalkBack labels exist for action keys.
- Large text, contrast, reduced motion, and landscape layouts are checked.
- Glass effects degrade to solid surfaces when unsupported or expensive.

## Release truth

A UI control is not a feature until it changes real behavior, persists where expected, handles errors, and passes the applicable tests.
