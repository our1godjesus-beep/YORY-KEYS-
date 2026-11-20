# iPhone-First Experience Standard

YORY KEYS must feel intentionally designed for iPhone while remaining an original product. Do not clone Apple's keyboard. Use iOS conventions for interaction quality while maintaining YORY's Tinted Glass identity.

## Visual identity

- Default style: Tinted Glass with a translucent panel, soft edge, calm shadows, carefully tuned keycaps and restrained accent color.
- Initial presets: Dark Tinted Glass, Light Tinted Glass, Midnight, Arctic, Rose, Neon and Reduced Effects.
- Keep letter contrast strong in every theme; don't allow a wallpaper or gradient to reduce glyph contrast.
- Use a stable key geometry system, not independent arbitrary sizes per key.
- Keep toolbar symbols recognizable and accessible; each action gets an accessibility label.
- Show the current language near the space key without making the space key too narrow.
- Use restrained motion; provide Reduced Motion and low-effects modes.
- Do not imitate system settings icons/branding or present YORY as an Apple product.

## iPhone interaction quality

- Keyboard appears quickly and does not perform network access during first render.
- First frame uses cached theme and layout; optional resources load lazily.
- Touch feedback is immediate and should not wait for a sound or haptic.
- Key repeat and long-press alternates must not accidentally commit a character twice.
- The keyboard height must be tuned per device/available size and tested in compact and landscape conditions.
- When an editor requests email, URL, numbers, decimal, search, or another special layout, adapt controls to that input trait.
- Always let users switch to the next enabled keyboard when the system requests the globe/next-input key.
- On iPadOS test docked, compact-width and floating presentations.

## Tinted Glass implementation rules

1. Draw the translucent panel and edge treatment with native views/layers.
2. Use system blur only where supported and measure extension memory/performance cost.
3. Never sample another app's screen or claim true live wallpaper extraction behind the keyboard.
4. When live backdrop blur is unavailable, use a theme tint plus subtle gradient/noise and edge highlight as a deliberate fallback.
5. Offer a solid-surface fallback for high contrast, low power and reduced effects.
6. Keep shadows and glow subtle to avoid visual clutter.

## App-to-extension settings

- Share only theme/configuration data via App Group.
- The host app writes versioned, validated preference snapshots atomically.
- The extension reads a small active-theme snapshot and never waits for cloud sync.
- Theme changes apply on the next extension appearance or a safe refresh point.
- Corrupt or unknown theme versions fall back to a known default without crashing.

## iOS platform rules to design around

- The custom keyboard is an isolated extension; it does not have direct unrestricted access to the host text view.
- Use UITextDocumentProxy for the insertion point and exposed text context.
- Secure text fields always show the system keyboard instead of a custom keyboard.
- Phone-pad and name-phone-pad fields show the system keyboard.
- Some apps can disallow third-party keyboard extensions.
- Open Access is required for network access and writing to the shared App Group from the extension. The core typing experience must work without it.
- Users must enable the keyboard in Settings; onboarding must not imply that an app can silently activate it.

## Required iOS QA

- Current supported iOS release and the minimum deployment target.
- iPhone small/large display sizes.
- Portrait and landscape.
- iPad docked and floating/compact mode.
- Light and dark appearance.
- All supported input traits.
- Next-keyboard switching.
- Secure fields and phone pad fallback.
- App with third-party keyboards disabled.
- Open Access disabled.
- Memory pressure and extension termination/relaunch.
- VoiceOver, Dynamic Type behavior where feasible, increased contrast and Reduce Motion.

## Official platform references

- https://developer.apple.com/documentation/uikit/creating-a-custom-keyboard
- https://developer.apple.com/documentation/uikit/configuring-a-custom-keyboard-interface
- https://developer.apple.com/documentation/uikit/handling-text-interactions-in-custom-keyboards
- https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard


## Optional Open Access

The extension declares `RequestsOpenAccess = true` so iOS can offer the user the system's **Allow Full Access** setting. This is a request, not an automatic grant. YORY must continue normal typing when `hasFullAccess == false`. The only current keyboard-side behavior gated by Full Access is the iOS input-click audio API. No typed content, surrounding text, passwords, or clipboard payloads are transmitted. Keep all account, marketplace and downloads in the containing app, not in the live typing path.
