# YORY KEYS — Competitive Strategy & Monetization Plan
Research date: 2026-11-21
Repository reviewed: current `main` branch
Audience: product owner, engineering and launch team
Status: strategic recommendation; not a claim that planned features or revenue systems are implemented.

## Executive decision

Do not position YORY as "another keyboard with more icons". Do not try to match Gboard's entire feature set or SwiftKey's language catalogue feature-for-feature. Those are mature products with enormous distribution, language data, model infrastructure and years of quality iteration.

**Recommended position: YORY KEYS is the premium-feeling, Arabic-first bilingual keyboard that gives users unusually good control over how they type and how their keyboard looks, while keeping core typing local and useful offline.**

The first defensible audience is Arabic/English bilingual users who care about Arabic layout quality, fast language switching, visual personalization and privacy. Egypt is a sensible first test market because the owner is building from an Arabic-speaking context, but the product should not assume every Arabic user has the same dialect, spelling or layout needs.

The order of priorities is:
1. Make everyday typing reliably good.
2. Make Arabic/English switching and customization noticeably better.
3. Prove privacy and platform behavior.
4. Earn revenue from optional personalization and genuinely valuable language tools.
5. Expand into creator packs, language packs and user-requested writing utilities only after quality and moderation are ready.

## 1. What the current product actually contains

The repository's feature matrix and implementation status are the authority. The following is a source-based snapshot, not marketing copy.

### Existing strengths
- Native Android `InputMethodService` and iOS custom keyboard extension rather than a fake keyboard overlay.
- Seven offline letter layouts: English US, Arabic, German, Spanish, French, Portuguese and Italian.
- Only English and Arabic currently have bundled starter prefix dictionaries. The other five are layout-only; the 120-language catalogue is a target, not 120 shipping language packs.
- A local prefix-completion baseline and optional personal learning. The current engine is not a trained next-word model and not production autocorrect.
- Android text clipboard manager with local AES-GCM encryption/Android Keystore, 50-item cap, 24-hour expiry for unpinned items and heuristic secret filtering. This is Android-only; do not promise clipboard-history parity on iOS.
- Theme Studio, locally saved themes, theme import/export, key geometry and sound/haptic controls. Gallery previews are still illustrative in places and are not yet proven to match the live keyboard renderer perfectly.
- Incognito controls and privacy-oriented local storage.
- Optional Supabase account/backend source, with RLS migrations already applied to the development project. Theme publishing is still source-only and the discovery/creator marketplace UI is not complete.

### Gaps that block a serious launch claim
- Context-aware autocorrect, reliable next-word prediction, swipe typing and robust mixed-language prediction are not shipped.
- Voice typing is not connected.
- Emoji text search and full Unicode/ZWJ coverage are incomplete.
- GIF discovery, sticker marketplace, screenshot/image insertion and writing/translation tools are not complete.
- Five of the seven installed layouts do not have bundled starter dictionaries.
- There is no production in-app billing/subscription implementation in the repository tree reviewed for this plan.
- No signed production release, store listing, full support/terms package or physical-device QA has been established by this strategy review.

**Decision:** market the current version as an early beta with strong customization and local-first basics. Do not describe it as an AI keyboard, a 120-language keyboard, a full autocorrect solution, a voice keyboard or a SwiftKey replacement yet.

## 2. Competitive landscape and what it means

### Gboard — distribution and breadth
The Google Play listing currently shows 10B+ downloads and 18.8M reviews. Its listed capabilities include Glide Typing, voice typing, handwriting, emoji search, GIFs, multilingual autocorrect/suggestions and Translate. This is not a feature list YORY should attempt to recreate immediately; it is evidence of the scale and breadth of the incumbent.

**YORY response:** do not compete on raw language count or broad utility first. Compete on a clearly better Arabic/English experience, customization workflow and transparent privacy.

### Microsoft SwiftKey — prediction, languages and integrated extras
The Google Play listing describes over 700 languages, support for up to five enabled languages, autocorrect and AI-powered predictions, swipe typing, toolbar shortcuts, AI rewriting, emoji/GIF/stickers, and more than 100 themes.

**YORY response:** language count is not a credible early differentiator. A smaller set of accurately tested languages is better than a large number of weak layouts. Build quality tiers and label them honestly.

### Grammarly — writing outcome, not only keyboard appearance
Grammarly markets real-time grammar/spelling/punctuation assistance and voice-to-text that cleans up filler words. Its differentiator is the quality of the message produced, not the color of the keycaps.

**YORY response:** a later YORY writing assistant may be valuable, but it should be a user-initiated tool in the companion app. The user should select or paste text deliberately; never silently upload the focused editor or keystroke stream.

### The gap YORY can own
A useful wedge is:
- Arabic/English bilingual typing with carefully tested RTL, punctuation and code-switching.
- A prominent, reliable left/right spacebar language gesture that cycles only through languages the user added.
- A genuine keyboard design studio: coherent themes, geometry, compactness, key feedback and saved personal styles.
- Local-first typing and clear, restrained permission requests.
- A small number of polished, fast language packs rather than exaggerated language-count claims.

This is a positioning hypothesis to validate with real users, not a claim that YORY already beats those products in typing accuracy.

## 3. The product promise

Proposed store-facing promise:

**"YORY KEYS — your Arabic and English keyboard, shaped around your style. Personalize the keys, switch languages with a swipe, and keep everyday typing on your device."**

Supporting proof points that can be used only after device QA:
- Native keyboard on Android and iOS.
- Arabic and English layouts available offline.
- Seven available letter layouts, with dictionaries currently bundled only for English and Arabic.
- Custom themes and keyboard geometry.
- Spacebar tap inserts a space; left/right swipe changes among the languages the user has added.
- Local learning can be turned off or cleared.
- Android clipboard history is optional and local; iOS does not provide the same automatic history.

Avoid claims such as "best", "fastest", "100% secure", "AI-powered autocorrect", "120 languages ready", "voice typing", "millions of themes", "works in every app" or fabricated ratings/download totals.

## 4. Monetization: freemium, not pay-to-type

A keyboard is a highly trusted, frequently used utility. Users should never feel that the keyboard is holding their words hostage or making typing unreliable until they pay.

### Free forever
Keep these free:
- Core typing and text insertion.
- Arabic and English basic layouts.
- Basic language switching, including the spacebar gesture.
- Essential number/symbol pages and editor-aware Enter/Backspace.
- Incognito and privacy controls.
- Basic useful themes and a working default design.
- Local operation without sign-in.
- Access to Android/iOS system keyboard switching and setup help.

Do not put the basic ability to type, delete, use Arabic or leave the keyboard behind a paywall.

### YORY Studio Plus — first paid offer
Only sell features that are implemented, stable and visibly valuable:
- Premium theme collections and seasonal design packs.
- Advanced theme editor options and larger personal theme libraries.
- Extra keyboard layouts/visual presets, where they are tested on the user's platform.
- Advanced sound and haptic packs, only if licensed and fully implemented.
- Additional customization workflows such as per-language theme profiles, once complete.

Prefer one-time purchases for discrete theme packs. A subscription should fund ongoing value, not merely unlock a static color palette.

### YORY Plus — subscription, later
Consider a low-friction monthly and annual plan only after it provides ongoing value such as:
- Regularly maintained, quality-tested downloadable dictionary/autocorrect packs.
- Opt-in encrypted settings/theme sync.
- Optional encrypted personal-dictionary backup, encrypted on-device before upload.
- Premium creator theme catalogue with new releases and support.
- Explicit, user-invoked writing utilities with clear limits and privacy disclosures.

Do not sell cloud sync, AI rewriting, signed language downloads or marketplace access before the complete backend, security, moderation, restore-purchase and support flows are deployed and tested.

### Creator marketplace — future second revenue stream
After the app has real active users:
- Let approved creators publish original YORY theme packs.
- Show previews, compatibility, version, creator identity and clear price.
- Provide report, refund/help and moderation paths.
- Offer creator revenue share only after store billing, tax, age/identity, licensing and payout arrangements are resolved by the responsible adult business owner and relevant platforms.
- Do not allow unlicensed brand logos, copied keyboards, copyrighted artwork or misleading theme thumbnails.

### Ads
Recommendation for the first release: **no ads inside the keyboard and no ads based on typed content.** Keyboard trust is more valuable than early ad inventory. If ads are ever tested in the companion Studio app, they must be clearly separate from typing, non-personalized where required, and never use keystrokes, clipboard content or editor context for targeting.

### Pricing research, not guesses
Do not copy Grammarly's price or SwiftKey's model. Start with willingness-to-pay interviews and a small A/B pricing test after users have experienced the free product. Test one-off theme packs separately from subscription benefits. Record conversion, refund and renewal behavior; do not choose a price from competitor screenshots alone.

## 5. Product roadmap in priority order

### Phase 0 — trust and release readiness
Release gate:
- Reconcile the iOS Full Access configuration and all privacy documentation. Current repository files conflict: the keyboard extension plist sets `RequestsOpenAccess=true`, while `ios/README.md` and `docs/PRIVACY_MODEL.md` describe Open Access as disabled. The user must never be misled about this. Test core typing with Full Access off and on, decide whether the app truly needs it, and only ask for it when the exact capability requires it.
- Validate first-run setup on real Android devices and keyboard enable/select status.
- Add accurate iOS keyboard activation instructions and a real test field.
- Test keyboard on real devices across Arabic, English, mixed RTL/LTR, email, URL, numeric and secure fields.
- Measure startup, per-key response, memory, battery and crash behavior on representative midrange phones.
- Complete TalkBack/VoiceOver, safe-area/navigation and compact-width testing.
- Complete privacy policy, support contact, terms, account deletion/export controls, Data safety disclosures and store metadata.
- Remove all placeholder marketing claims, fake ratings, fake download counts and unsupported feature labels.

### Phase 1 — win the Arabic/English daily user
- Improve Arabic typing corpus and layout quality with reviewers who actually type Egyptian Arabic and Modern Standard Arabic; keep dialects distinct in evaluation.
- Build a real, local autocorrect engine with undo/restore; measure false corrections before turning it on by default.
- Improve word completion for Arabic and English and evaluate suggestions using a human-reviewed corpus.
- Add robust mixed-language switching, punctuation, numerals, diacritics and cursor behavior.
- Validate the spacebar swipe gesture across screen sizes and common Android vendors.
- Make live keyboard preview match the actual keyboard renderer.
- Add searchable emoji locally.
- Publish language capability states accurately: layout, dictionary, completion, autocorrect, next-word prediction, personal learning and swipe are separate flags.

### Phase 2 — deepen personalization
- Saved per-language and per-user keyboard profiles.
- More professional theme packs, theme editor improvements and live preview.
- Theme marketplace and creator tooling with review/moderation.
- Better clipboard UX on Android with sensitive-data controls; user-initiated paste on iOS.
- Optional, user-initiated sticker creation in the companion app.
- Add voice only with a clear permission rationale, cancellation/error handling and platform-specific tests.

### Phase 3 — selective intelligent writing tools
- Build a YORY writing assistant as a separate, explicit workflow.
- User pastes or selects text, chooses an action (shorten, clarify, formalize, translate), reviews the result and chooses whether to insert it.
- Disclose when a network service is used and never send an entire editor field or background keystroke stream.
- Keep basic typing independent from the writing service and network.

## 6. Distribution and growth

### First audience
Recruit a small, diverse beta cohort of Arabic/English bilingual users:
- Heavy daily Arabic texters.
- People who regularly mix Arabic and English.
- Users who value keyboard appearance and theme control.
- Users with varied Android screen sizes and at least one iOS cohort once a signed iOS build exists.

Do not begin mass promotion before the release gates in Phase 0 pass.

### Demonstration-led content
Make short, honest clips showing one real benefit at a time:
1. Tap spacebar versus swipe left/right to change added languages.
2. Arabic and English typing on a real device.
3. Build a custom theme in Studio and apply it to the live keyboard.
4. Show offline typing after airplane mode is enabled.
5. Explain exactly what clipboard history saves and how to turn it off (Android only).

Never use AI-generated UI as proof of a feature that the app does not ship. Never show fabricated star ratings, download counts or user testimonials.

### Referral loop
Let a user share a theme card or a YORY setup link. A shared card should show the creator/theme, not the user's words, clipboard, contacts or editor context. Reward referrals with a theme pack only if a real referral and entitlement system has been implemented.

## 7. Metrics that protect the business

Collect only aggregate, opt-in diagnostics. Do not log text, keystrokes, typed tokens, clipboard contents or editor context.

Measure:
- Activation: install → enable keyboard → select keyboard → complete a first test entry.
- Setup drop-off at each step.
- D1, D7 and D30 retained keyboard users.
- Keyboard crash-free sessions and startup/per-key latency percentiles.
- Suggestion acceptance rate from non-content aggregate counters, if explicitly opted in.
- Spacebar gesture success/failure in controlled test sessions.
- Theme creation, apply and reuse.
- Free-to-paid conversion, subscription renewal, refunds and churn after monetization exists.
- Support tickets by category and platform.

A first launch should be judged by activation, retention, typing quality and trust—not downloads alone.

## 8. Privacy and store constraints

- Google Play requires accurate disclosure of personal/sensitive data handling, a privacy policy, consent where applicable, secure handling and account/data deletion mechanisms. See the User Data policy.
- Google Play-distributed paid digital features/subscriptions generally require Google Play Billing unless a listed exception applies.
- Apple's keyboard extension rules require real keyboard input, next-keyboard progression and functionality without full network access or a requirement for Full Access. The keyboard extension itself must not contain advertising, marketing or in-app purchases; keep the commercial purchase experience in YORY Studio, not inside the keyboard.
- Apple's in-app purchase rules apply when selling digital features or subscriptions in the app.
- Keep permission prompts feature-driven. Voice requires microphone permission only when the user invokes voice. Image selection should use a system picker. Basic typing should not demand microphone, contacts, camera or location permissions.
- Never imply that "local-first" means every app action is offline or that data is never used unless the code and privacy disclosures prove it.

## 9. Revenue architecture and launch readiness

Repository review did not find billing, purchase, subscription or paywall implementation files. Before accepting money:
1. Define products/entitlements in a platform-neutral catalogue.
2. Integrate Google Play Billing on Android and StoreKit/App Store In-App Purchase on iOS.
3. Validate receipt/transaction state server-side where appropriate.
4. Implement restore purchases, cancellation, expiration, grace periods, refunds and account/device transfer behavior.
5. Keep keyboard typing functional if payment validation or network access fails.
6. Explain the renewal, period, price and benefits before confirmation.
7. Add store-policy-compliant purchase disclosures and testing.
8. Only expose paid toggles once the feature is actually available on that platform.

## 10. Immediate decisions

1. **Own the Arabic/English niche first.** Do not advertise 120-language support.
2. **Quality beats feature count.** Build autocorrect and Arabic evaluation before adding more decorative tools.
3. **Keep typing free.** Monetize personalization and services that create ongoing value.
4. **No keyboard ads.** Protect trust and do not use typed content for targeting.
5. **Make Studio the business surface.** Keep the system keyboard fast, simple and free of paywalls.
6. **Treat privacy as product design.** Reconcile the iOS Full Access inconsistency before any public trust claim.
7. **Prove retention before scaling promotion.** A keyboard installed but not enabled is not an active user.

## Sources and evidence
### Repository
- `README.md`
- `docs/IMPLEMENTATION_STATUS.md`
- `docs/ARCHITECTURE.md`
- `docs/PRIVACY_MODEL.md`
- `docs/LANGUAGE_QUALITY_PROGRAM.md`
- `replica/features.csv`
- `ios/YORYKeyboard/Info.plist`
- `ios/README.md`
- `supabase/README.md`

### External sources reviewed on 2026-11-21
- Google Play, Gboard: https://play.google.com/store/apps/details?id=com.google.android.inputmethod.latin
- Google Play, Microsoft SwiftKey: https://play.google.com/store/apps/details?id=com.touchtype.swiftkey
- Grammarly Mobile: https://www.grammarly.com/mobile
- Apple, custom keyboard Open Access and trust: https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard
- Apple, App Store Review Guidelines — sections 3.1 and 4.4.1: https://developer.apple.com/app-store/review/guidelines/
- Google Play, Payments policy: https://support.google.com/googleplay/android-developer/answer/9858738
- Google Play, User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Android Developers, Create an input method: https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method

The competitor store listings and platform policies can change. Re-check each policy and listing at release time.
