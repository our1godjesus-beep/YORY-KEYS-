# YORY KEYS — Obsidian Luxe contrast report

Computed with WCAG relative luminance for the exact token values in design/tokens.json.

| Foreground role | Background role | Ratio | Target | Result |
|---|---|---:|---:|---|
| text | bg | 17.13:1 | 4.5:1 | Pass |
| text | surface | 13.63:1 | 4.5:1 | Pass |
| text-muted | bg | 10.39:1 | 4.5:1 | Pass |
| text-muted | surface | 8.27:1 | 4.5:1 | Pass |
| on-accent | accent | 8.68:1 | 4.5:1 | Pass |
| accent | bg | 9.29:1 | 4.5:1 | Pass |
| accent | surface | 7.39:1 | 4.5:1 | Pass |
| danger | bg | 8.73:1 | 4.5:1 | Pass |
| success | bg | 11.21:1 | 4.5:1 | Pass |
| warning | bg | 13.08:1 | 4.5:1 | Pass |
| border-input | bg | 3.96:1 | 3:1 UI | Pass |
| border-input | surface | 3.15:1 | 3:1 UI | Pass |

Notes:
- These ratios apply to the exact HEX tokens, not to every translucent overlay after compositing.
- Runtime keycaps are mostly opaque, but pressed states, user-selected opacity, and custom themes still need rendered-device contrast checks.
- The contrast report does not certify every font size, display setting, outdoor condition, or custom theme.
- Re-run the calculation whenever the token values change.
