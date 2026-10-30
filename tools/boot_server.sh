#!/usr/bin/env bash
# Arranca un servidor Forge real con el mod y comprueba que cargue sin errores.
# Uso: tools/boot_server.sh <carpeta-del-mod>   (se ejecuta desde la raiz del repo)
set -u
mod="$1"; cd "$mod"
mkdir -p run; echo "eula=true" > run/eula.txt
log="$(mktemp)"; fifo="$(mktemp -u)"; mkfifo "$fifo"
chmod +x gradlew
# el fifo mantiene abierta la entrada del servidor; luego le mandamos "stop"
(./gradlew runServer --no-daemon < "$fifo" > "$log" 2>&1; echo "exit=$?" >> "$log") &
exec 3> "$fifo"
ok=0
for i in $(seq 1 240); do
  if grep -q 'Done (' "$log"; then ok=1; break; fi
  if grep -q '^exit=' "$log"; then break; fi
  sleep 2
done
echo "stop" >&3; sleep 8; exec 3>&-
pkill -f "runServer" 2>/dev/null || true
echo "===== resumen del arranque de $mod ====="
ann() { # manda un texto como anotacion visible en GitHub (varias lineas)
  local t; t=$(tail -n "$2" "$3" | cut -c1-300 | sed 's/%/%25/g' | awk '{printf "%s%%0A", $0}')
  echo "::error title=$1::${t}"
}
if [ $ok -ne 1 ]; then echo "ERROR: el servidor no llego a 'Done'"; tail -80 "$log"; ann "Servidor no arranco ($mod)" 35 "$log"; exit 1; fi
bad=$(grep -E "/(ERROR|FATAL)\]|Missing registry|Couldn't parse|Parsing error|Failed to load (recipe|advancement|loot|function|tag)|Exception in thread|Mod .* failed|Failed to start the minecraft server|Crash" "$log" \
  | grep -vE "server.properties|PlatformDependent|jarjar|ForgeGradle|Reflective setAccessible|/DEBUG\]" | head -30)
if [ -n "$bad" ]; then echo "ERROR: hay errores en el registro del servidor:"; echo "$bad"; echo "$bad" > "$log.bad"; ann "Errores en el servidor ($mod)" 30 "$log.bad"; exit 1; fi
grep -E "Done \(" "$log"; echo "Arranque limpio"
