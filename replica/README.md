# Replica workstream — YORY KEYS

This folder contains the clean-room reconstruction plan for the YORY KEYS companion settings app and native keyboard.

## What Replica is doing
1. Recon: inspect user-supplied screenshots, public help pages and the current YORY source; map screens, flows, components, entities and feature parity.
2. Architecture: keep the current native Android and iOS stack, define navigation, platform boundaries, local state and optional cloud contracts.
3. Design: define YORY-owned tokens and components that reproduce the useful interaction patterns without copying Microsoft branding/assets.
4. Build: implement one screen/flow at a time and update features.csv.
5. Test: verify every flow and state on emulator/simulator and physical devices.

## Important design correction
The reference uses hierarchical settings navigation, not a four-item persistent bottom tab bar. YORY should use a root settings list, native detail navigation with a back button, a dedicated Themes gallery, and an optional keyboard quick-action anchored above the system safe area.

The Android navigation bar/gesture region and iOS home indicator are operating-system UI, never part of YORY's own navigation component.

## Files
- recon.md — evidence-based screen/flow inventory
- features.csv — priority and current parity matrix
- screens/README.md — private screenshot reference registry; screenshots are not shipped
- design/tokens.json — YORY-owned design tokens and contrast pairs
- design/components.md — reusable component and screen behavior specification
- design/keyboard-measurements.md — responsive dimensions for letter/symbol rows, toolbar, suggestions, More grid and added-language spacebar gestures
- architecture.md — native implementation plan and local/cloud boundaries
- test-plan.md — test cases and device QA requirements
- implementation-backlog.md — build order and acceptance criteria
- keyboard-reference-audit.md — detailed comparison of the three supplied keyboard screenshots against current Android/iOS source, platform gaps and prioritized parity plan

## Clean-room rule
Rebuild the function and UX pattern using original code, copy, colors, icon assets and theme art. Never copy Microsoft source, private APIs, branding, illustrations or proprietary theme assets. Do not claim a feature is finished until it works and passes QA.
