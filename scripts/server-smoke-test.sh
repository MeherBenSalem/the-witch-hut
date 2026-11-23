#!/usr/bin/env bash
# Starts a dev server for one Minecraft version and loader, places every Witchbound structure, hurts a few mobs
# (so the damage hooks and mixins run) and stops after 30 seconds. Fails if the server crashes or never gets there.
#   usage: scripts/server-smoke-test.sh <minecraft version folder> <fabric|forge|neoforge>
set -u
VERSION="$1"; LOADER="$2"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/$VERSION"
RUN="$LOADER/runs/server"
rm -rf "$RUN/world" "$RUN/crash-reports" "$RUN/logs"
PACK="$RUN/world/datapacks/witchbound_smoke"
mkdir -p "$PACK"
echo "eula=true" > "$RUN/eula.txt"
cat > "$RUN/server.properties" <<PROPS
online-mode=false
function-permission-level=4
spawn-protection=0
view-distance=4
simulation-distance=4
level-seed=20261123
initial-enabled-packs=vanilla,file/witchbound_smoke
PROPS
sed 's/"description": .*/"description": "Witchbound smoke test"/' common/src/main/resources/pack.mcmeta > "$PACK/pack.mcmeta"

STRUCTURES="$(ls common/src/main/resources/data/better_witch_huts/worldgen/structure/ | sed 's/\.json$//')"
for dir in function functions; do
  mkdir -p "$PACK/data/witchbound_smoke/$dir" "$PACK/data/minecraft/tags/$dir"
  echo '{"values":["witchbound_smoke:start"]}' > "$PACK/data/minecraft/tags/$dir/load.json"
  printf 'forceload add -48 -48 48 48\nschedule function witchbound_smoke:run 100t\n' > "$PACK/data/witchbound_smoke/$dir/start.mcfunction"
  {
    echo 'time set midnight'
    x=-200
    for s in $STRUCTURES; do echo "place structure better_witch_huts:$s $x 64 200"; x=$((x + 80)); done
    for m in zombie witch skeleton; do
      echo "execute positioned 4 0 4 positioned over world_surface run summon minecraft:$m ~ ~1 ~ {Tags:[\"wb_smoke\"],PersistenceRequired:1b}"
    done
    echo 'execute as @e[tag=wb_smoke] run damage @s 2 minecraft:generic'
    echo 'execute as @e[tag=wb_smoke] run damage @s 3 minecraft:fall'
    echo 'say WITCHBOUND_SMOKE_RAN'
    echo 'schedule function witchbound_smoke:finish 600t'
  } > "$PACK/data/witchbound_smoke/$dir/run.mcfunction"
  printf 'kill @e[tag=wb_smoke]\nsay WITCHBOUND_SMOKE_OK\nstop\n' > "$PACK/data/witchbound_smoke/$dir/finish.mcfunction"
done

LOG="$ROOT/server-smoke-$VERSION-$LOADER.log"
./gradlew ":$LOADER:runServer" --stacktrace < /dev/null > "$LOG" 2>&1 &
PID=$!
result=timeout
for _ in $(seq 1 180); do
  sleep 5
  if grep -q "WITCHBOUND_SMOKE_OK" "$LOG"; then result=ok; break; fi
  if grep -q -E "This crash report has been saved|Encountered an unexpected exception|Failed to start the minecraft server|Mixin apply.*failed|InvalidInjectionException" "$LOG"; then result=crash; break; fi
  if ! kill -0 $PID 2>/dev/null; then result=exited; break; fi
done
sleep 10
pkill -P $PID 2>/dev/null; kill $PID 2>/dev/null; wait $PID 2>/dev/null

grep -n -E "WITCHBOUND_SMOKE|better_witch_huts|Witchbound|Placed|Failed to place" "$LOG" | grep -v -E "Download|Task :" | head -40
for f in "$RUN"/crash-reports/*.txt; do [ -f "$f" ] && { echo "=== $f"; head -80 "$f"; }; done
grep -n -E "ERROR|Exception|Caused by|[Mm]ixin.*(fail|error)" "$LOG" | head -60
echo "Result: $result"
if [ "$result" != ok ] || grep -q -E "This crash report has been saved|InvalidInjectionException" "$LOG"; then exit 1; fi
grep -q "WITCHBOUND_SMOKE_RAN" "$LOG" || { echo "The smoke function never ran"; exit 1; }
