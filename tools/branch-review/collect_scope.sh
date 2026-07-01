#!/usr/bin/env bash
set -euo pipefail

BASE_REF="${1:-origin/develop}"
OUT_DIR="${2:-.tmp/branch-review}"

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Not a git repository."
  exit 1
fi

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"

if ! git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  echo "Base ref '$BASE_REF' does not exist locally."
  echo "Try: git fetch origin"
  exit 1
fi

MERGE_BASE="$(git merge-base "$BASE_REF" HEAD)"
RANGE="${MERGE_BASE}..HEAD"

mkdir -p "$OUT_DIR"

git --no-pager diff --name-status "$RANGE" >"$OUT_DIR/files.txt"
git --no-pager diff --unified=3 "$RANGE" >"$OUT_DIR/diff.patch"
git --no-pager log --oneline "$RANGE" >"$OUT_DIR/commits.txt"

cat >"$OUT_DIR/summary.md" <<EOF
[SKILL ACTIVE] branch-review

Branch: ${CURRENT_BRANCH}
Base ref: ${BASE_REF}
Merge base: ${MERGE_BASE}
Range: ${RANGE}

Artifacts:
- ${OUT_DIR}/files.txt
- ${OUT_DIR}/commits.txt
- ${OUT_DIR}/diff.patch
EOF

echo "[SKILL ACTIVE] branch-review"
echo "Branch: ${CURRENT_BRANCH}"
echo "Range: ${RANGE}"
echo
echo "Changed files:"
cat "$OUT_DIR/files.txt"
echo
echo "Artifacts written to: $OUT_DIR"
