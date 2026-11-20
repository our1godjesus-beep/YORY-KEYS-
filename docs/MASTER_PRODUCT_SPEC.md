# YORY KEYS — Master Product Specification

**Product promise:** a native, personal keyboard that combines expressive design, excellent typing, and privacy-first intelligence.

This document defines the complete target product. It is not a claim that every listed feature is already implemented. The current truth is in docs/IMPLEMENTATION_STATUS.md and shared/specs/feature-catalog.json.

## 1. Product identity

- Product name: **YORY KEYS**
- Design language: **Tinted Glass** — translucent surfaces, controlled blur, subtle highlights, high-contrast keycaps, quiet motion.
- Product attitude: elegant, expressive, fast, dependable, personal.
- Platforms: native Android IME and native iOS Custom Keyboard Extension, each with a native companion app.
- Core modes: offline-first; optional online features.
- Primary differentiator: a real keyboard design studio with independent controls for visual and sensory details, backed by a genuinely usable keyboard.

## 2. Experience principles

1. Typing quality and response time always outrank decorative effects.
2. Every visible control must work, or be clearly labeled as unavailable/planned.
3. The keyboard must stay useful without account, network, or optional permissions.
4. Personalization must be granular but safe: defaults are attractive, accessible, and readable.
5. A theme preview and the live keyboard must render from the same theme document.
6. Android and iOS share product contracts, not identical platform implementations.
7. Every feature has a status, acceptance criteria, privacy boundary, and test owner.
8. Do not market a language or AI capability until its quality gates pass.

## 3. Complete feature system

### A. Core keyboard and text interaction
- Native text insertion and deletion.
- Letter, number, punctuation, symbol, shift, caps-lock, long-press alternates, space, enter, language switch, and next-keyboard controls.
- Input-aware layouts: general text, email, URL, search, numeric, decimal, phone, and editor action.
- RTL/LTR, bidirectional mixed-script text, diacritics, Arabic punctuation, combining marks, and cursor-safe editing.
- Autocapitalization, sentence capitalization, smart punctuation, and editor return-key behavior where supported.
- Portrait, landscape, compact-width, split/floating iPad layouts where supported.
- Accessibility labels, scalable text, touch target sizing, contrast and reduced-motion support.

### B. YORY Studio — theme and keyboard design
- Theme presets plus a complete editor.
- Per-token color controls for panel, key, pressed key, primary text, secondary text, symbols, toolbar, suggestion surface, border, accent, and dividers.
- Solid colors, gradients, alpha, HEX, RGB and HSV entry.
- Optional user-selected image backgrounds in the host app; safe local asset copying and file-size limits.
- Glass opacity, blur intensity (only where the platform can render it), saturation, edge highlight, reflection and glow.
- Key geometry: shape, radius, height, spacing, row inset, special-key widths.
- Typography: supported font family, size, weight, symbol size.
- Layout density: compact, standard, spacious; custom values bounded for usability.
- Motion: press feedback, ripple/scale/glow, animation speed, reduced-motion mode.
- Live preview, undo/reset, duplicate, rename, save, import, export, and theme migration.
- Theme file format: versioned .yorytheme, shared schema, bounded asset sizes, strict validation.
- Presets must include Dark Tinted Glass, Light Tinted Glass, Midnight, Arctic, Neon, Rose, and a low-effects accessibility preset.

### C. Sound and haptics
- Independent enable/disable switches.
- Global volume plus per-action sound selection for letter, space, delete, enter, modifier and emoji.
- Built-in sound packs with documented licenses.
- User-imported sounds only in the companion app, with type/size validation; the keyboard loads only approved local assets.
- Haptic intensity and action mapping where supported by platform APIs.
- Respect system settings, silent mode, user preference and extension limitations.
- Never use haptics/audio in a way that blocks input or makes the keyboard feel delayed.

### D. Typing intelligence
- Next-word prediction.
- Word completion.
- Autocorrect with undo/restore.
- Personal dictionary and learned frequency counts.
- Optional learning control: off, learn accepted words, or clear all learning.
- Mixed-language typing and language identification.
- Language-specific tokenization, casing, punctuation, morphology and keyboard layouts.
- Arabic dialect support and Arabizi support as distinct capabilities.
- Swipe typing after tap typing has passed quality gates.
- Contextual emoji and phrase suggestions.
- A local engine interface that can host a compact quantized model, n-gram model, dictionary, or rule engine.
- No cloud inference on live typing in the default product architecture.

### E. Emoji
- Search in supported interface languages.
- Categories, recent items, favorites, skin-tone variants where Unicode supports them.
- Contextual suggestions with a clear privacy boundary.
- Custom artwork packs inside YORY's own picker only when licensing permits.
- Explain that inserted Unicode emoji are rendered by the recipient OS/app; YORY cannot force another app to display Apple's, Google's, or another vendor's artwork.

### F. Clipboard
- User-controlled clipboard manager with explicit enable/disable.
- Local text snippets, links, pinned items, search, delete, expiry and clear-all.
- Sensitive-item suppression for passwords, one-time codes, financial patterns and secure fields.
- User-visible explanation of platform clipboard rules.
- User-initiated image selection/import in the host app.
- No silent screenshot harvesting; no promise that iOS permits background clipboard monitoring or image capture.
- Do not sync clipboard contents.

### G. Sticker Studio
- Select an image in the host app.
- Crop, resize, add text, outline, shadow and decorative layers.
- Background removal only via an on-device model or an explicitly opted-in service.
- Export transparent PNG/WebP with bounds and sanitization.
- Local sticker packs, favorites, search, import/export and user-initiated sharing.
- Keyboard insertion must use the target platform's actual supported attachment/paste mechanisms; do not claim image insertion in every app.

### H. Writing tools
- Local spelling/grammar utilities where a local language pack exists.
- Optional translation and rewrite tools via a clearly disclosed online service.
- Tone controls: concise, friendly, formal, clearer.
- Smart replies only from text the user explicitly selects or enters into the tool.
- No silent upload of the current document or surrounding field contents.
- Explicit copy/insert confirmation and a visible progress/error state.

### I. Accounts, marketplace and cloud
- Supabase Auth: Apple/Google/email as configured; anonymous mode only if its lifecycle is fully specified.
- Public theme discovery, detail, ratings, favorites, reporting, creator profile and moderation.
- Theme preview assets in a public read bucket with owner-scoped writes.
- Sticker pack metadata and private owner-scoped assets by default.
- User preferences sync, never required for local use.
- End-to-end encrypted personal dictionary backup; server stores ciphertext only.
- Signed language-pack manifests, checksums and signature verification before activation.
- Offline outbox, retry/backoff, idempotency, conflict policy and account deletion.
- Analytics only as opt-in aggregated events; no typed strings, clipboard content or personal dictionary data.

### J. Onboarding and settings
- Explain how to enable/select the system keyboard.
- Explain iOS Open Access separately, accurately and without pressure.
- Permission requests only at the moment a feature needs them.
- Default to offline functionality.
- Keyboard preview and typing test.
- Language downloads, storage usage, update controls and low-storage behavior.
- Privacy dashboard, export, delete account, clear learning and clipboard controls.
- Troubleshooting: keyboard not visible, restricted access, app-specific blocked extension, language missing, sync paused.

## 4. Platform architecture

### Android
- Kotlin native app and InputMethodService.
- Use InputConnection for editor communication.
- Inspect EditorInfo and handle secure fields before showing suggestions, learning or clipboard UI.
- Host app stores theme assets and local preferences; IME loads only the active theme and active language resources.
- Use Android lifecycle and background-work APIs for optional sync.
- Handle low-memory, process recreation, orientation and multiple display sizes.

### iOS
- SwiftUI companion app and UIKit UIInputViewController extension.
- Shared App Group for explicitly shared settings/assets.
- Extension works without Open Access.
- Use UITextDocumentProxy and only the context the system exposes.
- Build for varying keyboard widths and available height; test iPhone, landscape and iPad compact/floating presentations.
- Respect secure fields, phone/name-phone fields, and host apps that prohibit third-party keyboards.
- Network and write access to the shared App Group require Open Access; keep the base keyboard functional without it.

### Shared contracts
- Theme JSON Schema and version migrations.
- Language-pack manifest and capability metadata.
- Feature catalogue with truthful status.
- Privacy rules and allowed sync payloads.
- Test fixtures for text tokenization, RTL, theme round trips and language behavior.

## 5. Privacy and data classification

| Data | Local | Cloud |
|---|---:|---:|
| Live keystrokes and surrounding context | Yes, transient only | Never |
| Password/secure-field content | No learning/capture | Never |
| Personal dictionary | Yes | Ciphertext only, opt-in |
| Theme documents | Yes | Optional user-published theme only |
| Clipboard payloads | Optional local only | Never |
| Screenshot/image content | User-selected local workflow | Never by default |
| Account/profile metadata | Cache as needed | Yes, minimum required |
| Preference settings | Yes | Optional sync |
| Language-pack files | Installed locally | Static signed distribution |
| Aggregate product analytics | Optional | No raw text or unique content |

## 6. Release levels

### Foundation release
Real native keyboard, English and Arabic layouts, input traits, RTL, Tinted Glass presets, local theme editing, offline typing, onboarding and basic QA.

### Intelligence release
Reliable local word completion, autocorrect, local personal dictionary, bilingual switching and model-pack infrastructure.

### Studio release
Complete color/shape/glass/typography controls, sounds/haptics, theme files, preview parity and asset import.

### Creative release
Searchable emoji, clipboard manager, image picker and sticker creator.

### Connected release
Auth, preference sync, theme marketplace, encrypted dictionary backup, signed language packs and account lifecycle.

### Global release
Additional language packs only after each language passes its quality, layout, security and performance gates.

## 7. Definition of done

A feature is done only when:
- The flow works end-to-end on the target platform.
- It survives app/extension process recreation.
- Offline behavior is verified.
- Privacy and permission behavior is verified.
- Automated tests pass.
- Target device QA passes for supported screen sizes and OS versions.
- Accessibility and performance checks pass.
- The feature catalogue is updated with evidence and known limitations.
- Store-facing claims match the actual release behavior.
