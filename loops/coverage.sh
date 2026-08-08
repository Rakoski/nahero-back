#!/usr/bin/env bash
# Iterate: build -> measure JaCoCo coverage -> ask an AI agent for more tests -> repeat.
# Stops on: target reached, max iterations, or no measurable progress.
#
# Run from this directory (loops/). All project paths are resolved relative to
# PROJECT_ROOT (defaults to the parent directory).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT=${PROJECT_ROOT:-"$(cd "$SCRIPT_DIR/.." && pwd)"}

TARGET=${TARGET:-100}                 # coverage % you want
METRIC=${METRIC:-INSTRUCTION}         # INSTRUCTION | BRANCH | LINE | METHOD
MAX_ITER=${MAX_ITER:-8}
STALL_LIMIT=${STALL_LIMIT:-2}         # give up after N iterations with no gain
MIN_GAIN=${MIN_GAIN:-0.1}             # percentage points that count as "progress"

BUILD_CMD=${BUILD_CMD:-"./mvnw -B -q clean verify"}   # must produce the XML report
REPORT=${REPORT:-target/site/jacoco/jacoco.xml}       # relative to PROJECT_ROOT
TEST_DIR=${TEST_DIR:-src/test/java}                   # relative to PROJECT_ROOT
WORK=${WORK:-"$SCRIPT_DIR/.coverage-loop"}
GAPS_SCRIPT=${GAPS_SCRIPT:-"$SCRIPT_DIR/gaps.py"}

mkdir -p "$WORK"

cd "$PROJECT_ROOT" || { echo "!! cannot cd into $PROJECT_ROOT"; exit 1; }

command -v git >/dev/null && git rev-parse --git-dir >/dev/null 2>&1 || {
  echo "!! Run this inside a git repo — the loop relies on commits/rollback."; exit 1; }
[[ -z "$(git status --porcelain)" ]] || {
  echo "!! Commit or stash your work first."; exit 1; }

measure() {  # -> echoes coverage %, writes $WORK/gaps.json
  python3 "$GAPS_SCRIPT" "$REPORT" --metric "$METRIC" --top 8 > "$WORK/gaps.json" || return 1
  python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["primary"])' "$WORK/gaps.json"
}

ask_ai() {   # $1 = prompt file
  claude -p "$(cat "$1")" \
    --allowedTools "Read,Edit,Write,Grep,Glob,Bash(${BUILD_CMD%% *}:*)" \
    --max-turns 40 \
    --max-budget-usd "${BUDGET_USD:-2}" \
    --output-format json > "$WORK/agent.json" 2> "$WORK/agent.err"
}

prev=-1; stall=0
for (( i=1; i<=MAX_ITER; i++ )); do
  echo "=== iteration $i/$MAX_ITER ==============================="

  if ! $BUILD_CMD > "$WORK/build.log" 2>&1; then
    echo "-- build/tests red; asking the agent to fix before adding more"
    tail -n 200 "$WORK/build.log" > "$WORK/failure.txt"
    cat > "$WORK/prompt.md" <<EOF
The build is failing. Fix ONLY the broken tests or compilation errors below.
Do not modify production code under src/main. Do not delete or @Disabled any test
to make it pass. Re-run \`$BUILD_CMD\` until it is green.

\`\`\`
$(cat "$WORK/failure.txt")
\`\`\`
EOF
    ask_ai "$WORK/prompt.md"
    if ! $BUILD_CMD > "$WORK/build.log" 2>&1; then
      echo "!! still red after repair attempt — rolling back and stopping"
      git reset --hard HEAD; exit 1
    fi
  fi

  cov=$(measure) || { echo "!! could not read $REPORT"; exit 1; }
  echo "-- $METRIC coverage: ${cov}%  (target ${TARGET}%)"

  if awk -v c="$cov" -v t="$TARGET" 'BEGIN{exit !(c+0 >= t+0)}'; then
    echo "✅ target reached in $i iteration(s)"; exit 0
  fi

  if awk -v c="$cov" -v p="$prev" -v m="$MIN_GAIN" 'BEGIN{exit !(c-p < m)}'; then
    (( stall++ ))
    echo "-- no real gain (${prev}% -> ${cov}%), stall ${stall}/${STALL_LIMIT}"
    if (( stall >= STALL_LIMIT )); then
      echo "⛔ stuck at ${cov}% — the rest probably needs a human. See $WORK/gaps.json"
      exit 2
    fi
  else
    stall=0
    git add -A && git commit -qm "test: coverage ${prev}% -> ${cov}% (loop iter $((i-1)))" || true
  fi
  prev=$cov

  cat > "$WORK/prompt.md" <<EOF
Raise JaCoCo $METRIC coverage from ${cov}% toward ${TARGET}%.

Rules:
- Add or extend tests under $TEST_DIR only. Never touch src/main.
- Never weaken assertions, and never add tests that only call a method without
  asserting on its result or observable side effects.
- Every test must express real behaviour: given/when/then, meaningful names.
- Cover the *branches* listed as partially covered, not just the lines.
- If a gap is genuinely untestable (generated code, private constructor of a
  utility class, defensive \`throw new AssertionError\`), do not contort the code:
  append the class and the reason to $WORK/unreachable.md instead.
- Run \`$BUILD_CMD\` yourself and make sure it is green before you finish.

Remaining gaps (JaCoCo, worst first):
\`\`\`json
$(cat "$WORK/gaps.json")
\`\`\`
EOF

  ask_ai "$WORK/prompt.md" || echo "-- agent exited non-zero, continuing to re-measure"
done

echo "⏹ hit MAX_ITER ($MAX_ITER) at ${prev}%"
exit 3
