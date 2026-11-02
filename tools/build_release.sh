#!/usr/bin/env bash
# Builds every workspace (clean, with the unit tests) and copies the release jars into releases/.
# Java: 21 for 1.21.1, 25 for 26.x (Gradle itself runs on 25 there: Loom 1.18 needs it on 26.3).
# Override with JAVA21_HOME / JAVA25_HOME.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
J21=${JAVA21_HOME:-$(ls -d /usr/lib/jvm/java-21-openjdk-amd64 /opt/jdks/jdk-21* 2>/dev/null | head -1 || true)}
J25=${JAVA25_HOME:-$(ls -d /usr/lib/jvm/java-25-openjdk-amd64 /opt/jdks/jdk-25* "$HOME"/.gradle/jdks/*25*linux* 2>/dev/null | grep -v "\.tar\|lock" | head -1 || true)}
mkdir -p "$ROOT/releases"
for v in 1.21.1 26.1.2 26.2 26.3; do
  [ -d "$ROOT/$v" ] || continue
  if [ "$v" = 1.21.1 ]; then java_home=$J21; else java_home=$J25; fi
  echo "== $v (JAVA_HOME=$java_home)"
  (cd "$ROOT/$v" && JAVA_HOME="$java_home" ./gradlew clean build --no-daemon)
  for loader in fabric neoforge; do
    find "$ROOT/$v/$loader/build/libs" -maxdepth 1 -name "better_witch_huts-$loader-$v-*.jar" \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' -exec cp {} "$ROOT/releases/" \;
  done
done
ls -1 "$ROOT/releases"
