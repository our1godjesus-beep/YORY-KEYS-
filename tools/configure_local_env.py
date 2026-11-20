#!/usr/bin/env python3
"""Generate ignored Android/iOS Supabase client config from the root .env file."""
from __future__ import annotations

import os
import plistlib
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"
ANDROID_FILE = ROOT / "android" / "supabase.local.properties"
IOS_FILE = ROOT / "ios" / "YORYKeys" / "SupabaseConfig.plist"


def parse_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].strip()
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        if key in {"SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY"}:
            values[key] = value
    return values


def main() -> int:
    if not ENV_FILE.exists():
        print("No root .env found. Cloud account features remain unconfigured; offline use is unaffected.")
        print("Copy .env.example to .env when you are ready to configure account features.")
        return 0

    values = parse_env(ENV_FILE)
    url = values.get("SUPABASE_URL", "").strip()
    key = values.get("SUPABASE_PUBLISHABLE_KEY", "").strip()

    if not url and not key:
        print("Supabase values are empty. No platform config was generated; offline use remains available.")
        return 0
    if not url.startswith("https://"):
        print("Invalid SUPABASE_URL: use the HTTPS project URL.", file=sys.stderr)
        return 2
    if not key:
        print("SUPABASE_PUBLISHABLE_KEY is required when SUPABASE_URL is set.", file=sys.stderr)
        return 2
    if key.startswith("sb_secret_") or "service_role" in key.lower():
        print("Refusing to write a privileged Supabase key into a mobile client config.", file=sys.stderr)
        return 2

    ANDROID_FILE.parent.mkdir(parents=True, exist_ok=True)
    IOS_FILE.parent.mkdir(parents=True, exist_ok=True)
    ANDROID_FILE.write_text(
        f"SUPABASE_URL={url}\nSUPABASE_PUBLISHABLE_KEY={key}\n",
        encoding="utf-8",
    )
    with IOS_FILE.open("wb") as stream:
        plistlib.dump(
            {"SUPABASE_URL": url, "SUPABASE_PUBLISHABLE_KEY": key},
            stream,
            fmt=plistlib.FMT_XML,
            sort_keys=True,
        )

    try:
        os.chmod(ANDROID_FILE, 0o600)
        os.chmod(IOS_FILE, 0o600)
    except OSError:
        pass
    print("Generated ignored Android and iOS client config files. Values were not printed.")
    print("The mobile app receives only the Supabase publishable key; privileged keys are rejected.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
