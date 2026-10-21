#!/usr/bin/env python3
"""Rebrand the vendored Android string resources from OwnTV to BlankTV.

Why a script and not a one-off edit: the strings and the artwork under ``core/`` are vendored from
``ahXN00/OwnTV_Core``, and re-vendoring a newer upstream release would silently undo every rename.
Run this afterwards and the brand is back in place. ``--check`` is the guard: it exits non-zero if a
brand token is left anywhere it should have been renamed, so a re-vendor cannot slip through.

Scope and rules
---------------
* Only *values* are touched, never resource names. Every key (``content_exit_owntv`` and friends)
  stays as upstream wrote it, because Kotlin code, translations on Weblate and future upstream merges
  all reference the key, not the sentence.
* Only the displayed brand token changes: ``OwnTV`` -> ``BlankTV``. Lowercase identifiers
  (``owntv-backup.own``, package names, resource file names) are left alone — the first is a file
  name the app writes and parses, the others are Java/Kotlin identity.
* Keys in :data:`KEEP_UPSTREAM` describe services that really are upstream's — the shared metadata
  Worker, its fair-share policy, the OpenSubtitles proxy, and the translation project on Hosted
  Weblate. Renaming those would make the app claim a service it does not own and send users to a
  page that has nothing to do with BlankTV, so they keep the upstream name on purpose.

Usage
-----
    python3 tools/brand/rebrand_strings.py            # rewrite
    python3 tools/brand/rebrand_strings.py --check     # verify, change nothing
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# Resource directories whose string values are rebranded. Sorted for a stable report.
STRING_DIRS = [
    "core/src/main/res",
    "app/src/main/res",
]
STRING_FILES = ("strings*.xml", "donottranslate.xml")

BRAND_RE = re.compile(r"\bOwnTV\b")
NEW_BRAND = "BlankTV"

# Strings that name a service BlankTV does not operate. See the module docstring.
KEEP_UPSTREAM = {
    # Shared TMDB metadata Worker and its allowances.
    "settings_metadata_shared_worker_description",
    "settings_metadata_clear_advanced_message",
    "settings_metadata_fair_share",
    # OpenSubtitles proxy that runs on the same upstream Worker.
    "settings_open_subtitles_access_priority",
    # The translation project lives on Hosted Weblate under the upstream name.
    "settings_language_help_translate",
    "settings_language_contribution_description",
    "settings_language_contribution_qr_description",
}

ENTRY_RE = re.compile(
    r"<(string|item)\b([^>]*)>(.*?)</\1>",
    re.S,
)
COMMENT_RE = re.compile(r"<!--(.*?)-->", re.S)
NEXT_ENTRY_RE = re.compile(r"<(?:string|item)\b[^>]*name=\"([^\"]+)\"")
NAME_RE = re.compile(r'name="([^"]+)"')


def string_files() -> list[Path]:
    files: list[Path] = []
    for base in STRING_DIRS:
        root = ROOT / base
        if not root.is_dir():
            continue
        for values_dir in sorted(root.glob("values*")):
            for pattern in STRING_FILES:
                files.extend(sorted(values_dir.glob(pattern)))
    return files


def rebrand(path: Path, check: bool) -> tuple[int, int, list[str]]:
    """Rebrand one file. Returns (renamed values, kept values, checked keys still bearing brand)."""
    text = path.read_text(encoding="utf-8")
    renamed = kept = 0
    leftover: list[str] = []

    def replace(match: re.Match[str]) -> str:
        nonlocal renamed, kept
        tag, attrs, body = match.group(1), match.group(2), match.group(3)
        name_match = NAME_RE.search(attrs)
        name = name_match.group(1) if name_match else ""
        if not BRAND_RE.search(body):
            return match.group(0)
        if name in KEEP_UPSTREAM:
            kept += 1
            leftover.append(name)
            return match.group(0)
        renamed += 1
        return f"<{tag}{attrs}>{BRAND_RE.sub(NEW_BRAND, body)}</{tag}>"

    updated = ENTRY_RE.sub(replace, text)
    updated, commented = rebrand_comments(updated)
    if not check and updated != text:
        path.write_text(updated, encoding="utf-8")
    return renamed, kept, leftover


def rebrand_comments(text: str) -> tuple[str, int]:
    """Rebrand translator comments too, except the ones introducing a KEEP_UPSTREAM string.

    A comment is guidance for whoever translates the string that follows it, so it has to name the
    product that string is about. Everything else in the file stays byte-for-byte.
    """
    count = 0

    def replace_comment(match: re.Match[str]) -> str:
        nonlocal count
        body = match.group(1)
        if not BRAND_RE.search(body):
            return match.group(0)
        after = text[match.end():]
        following = NEXT_ENTRY_RE.search(after)
        if following and following.group(1) in KEEP_UPSTREAM:
            return match.group(0)
        count += 1
        return f"<!--{BRAND_RE.sub(NEW_BRAND, body)}-->"

    return COMMENT_RE.sub(replace_comment, text), count


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--check",
        action="store_true",
        help="report what would change (and fail if a rename is outstanding); write nothing",
    )
    args = parser.parse_args()

    total_renamed = total_kept = 0
    pending: list[tuple[str, int]] = []
    files = string_files()
    for path in files:
        renamed, kept, _ = rebrand(path, check=args.check)
        total_renamed += renamed
        total_kept += kept
        if renamed:
            pending.append((path.relative_to(ROOT).as_posix(), renamed))

    for rel, count in pending:
        print(f"  {'would rename' if args.check else 'renamed'} {count:>4} value(s) in {rel}")
    print(
        f"{len(files)} string files · {total_renamed} value(s) "
        f"{'to rename' if args.check else 'renamed'} · {total_kept} kept upstream on purpose"
    )
    if args.check and total_renamed:
        print("\nBrand tokens are still un-renamed — run without --check to apply.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
