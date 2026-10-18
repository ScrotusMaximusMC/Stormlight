#!/usr/bin/env bash
# Runs only the game tests affected by your changes.
#
#   scripts/test-affected.sh              changes on this branch vs main, plus uncommitted work
#   scripts/test-affected.sh --uncommitted  only uncommitted work
#   scripts/test-affected.sh --all        every test
#   scripts/test-affected.sh --dry-run    print what would run, don't run it
#
# Changes that touch no tested feature are compiled but not run.
set -euo pipefail
cd "$(dirname "$0")/.."

# Test class prefix -> regex matched against changed paths (lowercased, underscores removed).
# Add a line here when you add tests for a new feature.
RULES=(
  "Highstorm|highstorm|stormfront|cloudrenderermixin|soundenginelull"
  "Gemcutting|gemcut"
  "SphereForging|sphereforg|sphereitem|mould"
  "SprenSeek|sprenseek|sprenbond"
)
# Changes here can break anything, so they run every test.
SHARED='^(build\.gradle|settings\.gradle|gradle\.properties|gradle/|src/main/resources/fabric\.mod\.json|src/main/java/com/scrotey/stormlight/[^/]+\.java|src/main/java/com/scrotey/stormlight/(block/ModBlocks|block/entity/ModBlockEntities|item/ModItems|recipe/ModRecipes|screen/ModMenuTypes)\.java|src/client/java/com/scrotey/stormlight/client/StormlightClient\.java)$'

mode=branch dry=false
for arg in "$@"; do
  case $arg in
    --uncommitted) mode=uncommitted ;;
    --all) mode=all ;;
    --dry-run) dry=true ;;
    *) echo "unknown option: $arg" >&2; exit 2 ;;
  esac
done

run() {
  echo "> ./gradlew $*"
  $dry || sh gradlew "$@"
}

if [[ $mode == all ]]; then
  run runGameTest runClientGameTest
  exit
fi

changed=$( {
  [[ $mode == branch ]] && git diff --name-only "$(git merge-base main HEAD)" HEAD
  git diff --name-only HEAD
  git ls-files --others --exclude-standard
} | sort -u)

if [[ -z $changed ]]; then
  echo "No changes found; nothing to test."
  exit
fi

if grep -qE "$SHARED" <<<"$changed"; then
  echo "Shared code changed:"; grep -E "$SHARED" <<<"$changed" | sed 's/^/  /'
  run runGameTest runClientGameTest
  exit
fi

normalized=$(tr 'A-Z' 'a-z' <<<"$changed" | tr -d '_')
selected=()
for rule in "${RULES[@]}"; do
  prefix=${rule%%|*}
  grep -qE "${rule#*|}" <<<"$normalized" && selected+=("$prefix")
done

if (( ${#selected[@]} == 0 )); then
  echo "No tested features changed; compiling only."
  run compileGametestJava
  exit
fi

filter=$(IFS=,; echo "${selected[*]}")
echo "Affected tests: $filter"
run runGameTest runClientGameTest "-Ptests=$filter"
