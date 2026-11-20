# YORY Replica — contrast check

Computed with WCAG relative luminance on the current token values in design/tokens.json.

| Foreground role | Background role | Ratio | Target | Result |
|---|---|---:|---:|---|
| text | bg | 18.22:1 | 4.5:1 | Pass |
| text | surface | 14.92:1 | 4.5:1 | Pass |
| text-muted | bg | 9.55:1 | 4.5:1 | Pass |
| text-muted | surface | 7.82:1 | 4.5:1 | Pass |
| on-accent | accent | 10.46:1 | 4.5:1 | Pass |
| accent | bg | 11.33:1 | 4.5:1 | Pass |
| danger | bg | 8.61:1 | 4.5:1 | Pass |
| success | bg | 10.68:1 | 4.5:1 | Pass |
| warning | bg | 13.56:1 | 4.5:1 | Pass |
| border-input | bg | 7.57:1 | 3:1 UI | Pass |
| border-input | surface | 6.20:1 | 3:1 UI | Pass |

Notes:
- These results apply to the exact HEX values in the token file, not to every translucent overlay after compositing.
- Glass opacity, backgrounds and blur/tint layers must be tested in rendered screenshots; a transparent border can have a different effective contrast.
- Re-run the calculation whenever token values change.
- Contrast is not a substitute for testing actual font weight, dynamic type, outdoor brightness and focus visibility.
