#!/usr/bin/env python3
"""Validate YORY KEYS repository contracts without mobile SDKs."""
from __future__ import annotations
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ALLOWED_STATUSES = {
    "implemented_unverified", "source_implemented_unverified",
    "basic_scaffold", "partial_implementation", "prototype",
    "not_implemented", "planned", "catalog_added", "schema_scaffold",
    "not_connected", "ui_scaffold_only", "production_ready",
}

def load_json(relative: str):
    with (ROOT / relative).open("r", encoding="utf-8") as handle:
        return json.load(handle)

def main() -> int:
    errors: list[str] = []
    schema = load_json("shared/specs/yory-theme.schema.json")
    if not {"schemaVersion", "id", "name", "appearance"}.issubset(set(schema.get("required", []))):
        errors.append("Theme schema is missing required contract fields.")

    languages = load_json("shared/specs/language-catalog.json")
    language_rows = languages.get("languages", [])
    if languages.get("count") != 120 or len(language_rows) != 120:
        errors.append("Language catalogue must contain exactly 120 target entries.")
    tags = [row.get("tag") for row in language_rows if isinstance(row, dict)]
    if len(tags) != len(set(tags)):
        errors.append("Language catalogue contains duplicate language tags.")

    features = load_json("shared/specs/feature-catalog.json")
    rows = features.get("features", [])
    ids = [row.get("id") for row in rows if isinstance(row, dict)]
    if len(ids) != len(set(ids)):
        errors.append("Feature catalogue contains duplicate IDs.")
    for row in rows:
        if row.get("status") not in ALLOWED_STATUSES:
            errors.append(f"Unknown status for feature {row.get('id')}: {row.get('status')}")

    secret_patterns = [
        re.compile(r"""(?i)(service_role|secret_key|private_key)\s*[:=]\s*['"][^\s'"]{20,}"""),
        re.compile(r"-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    ]
    for path in ROOT.rglob("*"):
        if not path.is_file() or ".git" in path.parts or path.suffix.lower() not in {
            ".kt", ".swift", ".sql", ".md", ".json", ".toml", ".yml", ".yaml", ".ts", ".plist"
        }:
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for pattern in secret_patterns:
            if pattern.search(content):
                errors.append(f"Possible secret detected in {path.relative_to(ROOT)}")

    if errors:
        print("\n".join(f"ERROR: {item}" for item in errors))
        return 1
    print(f"YORY contracts OK: {len(rows)} features, {len(language_rows)} target languages.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
