# Feature Implementation Map

Use the JSON feature catalogue for machine-readable state. This document defines what each workstream must deliver before it can be called complete.

| Workstream | Product output | Completion evidence |
|---|---|---|
| Native keyboard core | Fully working Android IME and iOS extension | Real-device typing tests, editor traits and keyboard switching |
| Keyboard layouts | Language/script-specific layouts and alternates | Layout test vectors, RTL and mixed-script QA |
| YORY Studio | Full theme editor with shared live renderer | Preview/live parity and round-trip file tests |
| Glass renderer | Tinted translucent surfaces with accessible fallback | Device performance, contrast and reduced-effects tests |
| Theme files | Import/export, validation, migration | Cross-platform schema fixtures and malicious file tests |
| Sound/haptics | User-controlled key feedback | Real-device settings and silent-mode tests |
| Prediction | Completion, correction and next-word ranking | Language-specific evaluation reports |
| Personal learning | Local, user-controlled adaptation | Secure-field exclusion and erase/reset tests |
| Emoji | Search, categories, recent, favorites and skin tones | Localized search corpus and insertion tests |
| Clipboard | User-controlled local snippets | Consent, expiry, sensitive filtering and no-cloud proof |
| Sticker Studio | Image editing and local pack management | Export/import and supported insertion tests |
| Writing tools | Translation and rewriting with explicit user action | Provider disclosure, no silent upload, offline fallback |
| Accounts | Sign-in/out and recovery | Auth lifecycle tests |
| Marketplace | Public themes, favorites, ratings and creator tools | RLS, storage ACL, moderation, report/takedown tests |
| Sync | Offline outbox, retries and conflict handling | Network interruption, duplicate and conflict tests |
| Language packs | Signed pack delivery and atomic activation | Signature/checksum tamper tests |
| Privacy dashboard | User can inspect and control all local/cloud data | Export, clear, revoke and delete-account tests |
| Accessibility | VoiceOver/TalkBack, contrast, motion, touch targets | Platform accessibility audit |
| Release operations | Signed app bundles and store metadata | Reproducible CI build and release checklist |

## Workstream order

1. Make the existing Android and iOS vertical slices build and stable.
2. Finish theme engine/renderer parity; it is the differentiator the user will see immediately.
3. Stabilize layout, input traits, cursor operations and Arabic/English.
4. Add testable local intelligence interfaces before integrating larger models.
5. Complete local creative features.
6. Add Supabase only after confirming the intended project and passing local RLS tests.
7. Expand languages in measured tiers.
