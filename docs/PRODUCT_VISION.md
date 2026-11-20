# YORY KEYS — Product Vision

## Positioning

YORY KEYS is a personal keyboard platform: a fast, intelligent input tool and a creative customization studio. It should feel expressive without sacrificing legibility, reliability, privacy, or typing speed.

## The problem we are solving

Users may find existing keyboards visually generic, restrictive in personalization, or difficult to make their own. YORY KEYS responds with meaningful control over the keyboard's visual and sensory system—not merely a gallery of fixed skins.

## Product pillars

### 1. YORY Studio
- Free color selection (HEX/RGB/HSV), palettes, gradients, and per-component color tokens.
- Independent controls for keyboard background, key surfaces, pressed state, labels, symbols, borders, highlights, and suggestion strip.
- Key shapes, corner radius, spacing, key height, typography, and symbol sizing.
- Glass controls: translucency, blur where supported, tint, edge highlight, and restrained glow.
- Live preview that uses the same theme renderer as the real keyboard.
- Save, duplicate, rename, import, export, reset, and share themes.
- Accessibility checks for contrast, text scaling, and reduced motion.

### 2. Sound and haptics
- Built-in sound packs with per-action overrides (letter, space, delete, enter, modifier).
- Volume and haptics controls, including a global silent mode.
- User-imported sounds only where the platform and extension sandbox allow it.
- Respect device silent mode, system settings, and platform policies.

### 3. Typing intelligence
- Next-word suggestions, autocorrect, personal dictionary, multilingual typing, and later swipe typing.
- Local learning is opt-in and can be paused, reviewed, reset, or excluded per app/field where possible.
- Never learn from secure/password fields.
- Separate language-quality tiers; publish per-language capability and test results.

### 4. Clipboard
- Explicitly user-controlled clipboard history with pinning, deletion, expiry, and sensitive-item filtering.
- Never silently collect OTPs, passwords, payment data, or sensitive content.
- Android and iOS have different clipboard and extension restrictions; implement platform-specific behavior rather than claiming identical parity.
- Do not promise automatic screenshot ingestion. Offer user-initiated image selection/paste where supported.

### 5. Emoji and stickers
- Emoji search, recents, categories, and contextual suggestions.
- Emoji appearance inside YORY's own picker may use licensed/compatible artwork packs. The recipient app and OS decide how inserted Unicode emoji are rendered.
- Sticker creation starts as a host-app feature: select an image, crop, add outline/text, save a local pack, and share through supported system insertion mechanisms.
- Respect image, font, and emoji artwork licenses.

### 6. Offline and online
Offline: keyboard input, installed dictionaries/models, local themes, local settings, and supported local clipboard features.
Online (optional): account sync, theme marketplace, encrypted dictionary backup, and signed language-pack metadata/downloads.
Cloud sync must be opt-in and transparent.

## Design direction

Premium glass is an aesthetic direction, not an excuse to reduce contrast or frame rate. The interface must remain readable in bright light, dark mode, large text, and low-power devices. Provide Solid, Glass, and Low-motion/Low-effects rendering modes.

## Non-goals for the first release

- Claiming parity with the prediction quality of large incumbent keyboards.
- Sending live typing context to a cloud model.
- Circumventing iOS or Android permission/sandbox restrictions.
- Shipping 120 languages before quality, layout, IME behavior, and test coverage are verified.
