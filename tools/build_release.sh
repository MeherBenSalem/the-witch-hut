#!/usr/bin/env bash
# Builds every workspace and copies the release jars into releases/.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$ROOT/releases"
for v in 26.1.2 26.2 26.3; do
  [ -d "$ROOT/$v" ] || continue
  (cd "$ROOT/$v" && ./gradlew clean build --no-daemon)
  for loader in fabric neoforge; do
    find "$ROOT/$v/$loader/build/libs" -maxdepth 1 -name "better_witch_huts-$loader-$v-*.jar" \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' -exec cp {} "$ROOT/releases/" \;
  done
done
ls -1 "$ROOT/releases"
