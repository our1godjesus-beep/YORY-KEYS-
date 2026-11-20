# iOS Custom Keyboard: Platform Constraints

YORY KEYS must be designed around the public Custom Keyboard Extension APIs, not assumptions from Android.

## Required behavior

- Implement the extension with `UIInputViewController` and insert/delete through `textDocumentProxy`.
- Respect the host field's input traits and the current keyboard context.
- Provide a useful keyboard when Full Access / Open Access is disabled.
- Do not attempt to replace the system keyboard in secure text fields or fields where iOS/app policy disallows third-party keyboards.
- Treat the app and keyboard extension as separate processes with a constrained shared-container contract.
- Avoid unbounded memory use and expensive synchronous initialization in the extension.

## Open Access decision

The first YORY iOS source sets `RequestsOpenAccess = false`. The host app owns local theme editing and writes theme preferences to the App Group. The extension reads what iOS makes available to it. Typing does not need network access.

If a later release genuinely needs write access to the shared group, remote dictionary downloads, or another protected capability, it must:
1. Explain the precise user benefit before asking.
2. Work with the permission denied.
3. Never transmit live keystrokes or surrounding text.
4. Separate optional cloud sync from normal keyboard operation.
5. Revisit the privacy disclosure and threat model.

## Known system restrictions

- A custom keyboard is not shown for some secure/password and phone-pad contexts.
- The containing app can be opened by the user to manage settings; the extension must not rely on being able to launch it arbitrarily.
- The host app and keyboard extension have separate lifecycles.
- Third-party apps can opt out of custom keyboards.
- Audio, microphone, pasteboard, network, and shared-container access are affected by the extension sandbox and permission state.
- Emoji artwork inserted as Unicode is rendered by the receiving app/OS; YORY can only control artwork inside its own picker or sticker feature.

## Official references

- [Creating a custom keyboard](https://developer.apple.com/documentation/uikit/creating-a-custom-keyboard)
- [Configuring open access for a custom keyboard](https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard)
- [Archived Custom Keyboard Programming Guide](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html)

These constraints are product requirements, not implementation bugs.
