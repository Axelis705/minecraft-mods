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
# comandos de prueba opcionales del mod (ci_commands.txt): una linea por comando; "#expect TEXTO" exige que TEXTO salga en el registro
if [ $ok -eq 1 ] && [ -f ci_commands.txt ]; then
  while IFS= read -r line; do
    case "$line" in ''|'#expect '*) continue;; esac
    echo "$line" >&3; sleep 1
  done < ci_commands.txt
  sleep 9
fi
echo "stop" >&3; sleep 8; exec 3>&-
pkill -f "runServer" 2>/dev/null || true
echo "===== resumen del arranque de $mod ====="
ann() { # manda un texto como anotacion visible en GitHub (varias lineas)
  local t; t=$(tail -n "$2" "$3" | cut -c1-300 | sed 's/%/%25/g' | awk '{printf "%s%%0A", $0}')
  echo "::error title=$1::${t}"
}
if [ $ok -ne 1 ]; then echo "ERROR: el servidor no llego a 'Done'"; tail -80 "$log"; grep -nE "Caused by|Exception|Error|/ERROR\]|/FATAL\]|Invalid|Unable|Failed" "$log" | grep -vE "jarjar|PlatformDependent|setAccessible|server.properties|/DEBUG\]" | head -25 > "$log.why"; ann "Servidor no arranco ($mod)" 25 "$log.why"; exit 1; fi
bad=$(grep -E "/(ERROR|FATAL)\]|Missing registry|Couldn't parse|Parsing error|Failed to load (recipe|advancement|loot|function|tag)|Exception in thread|Mod .* failed|Failed to start the minecraft server|Crash" "$log" \
  | grep -vE "server.properties|PlatformDependent|jarjar|ForgeGradle|Reflective setAccessible|/DEBUG\]" | head -30)
if [ -n "$bad" ]; then echo "ERROR: hay errores en el registro del servidor:"; echo "$bad"; echo "$bad" > "$log.bad"; ann "Errores en el servidor ($mod)" 30 "$log.bad"; exit 1; fi
if [ -f ci_commands.txt ]; then
  while IFS= read -r line; do
    case "$line" in '#expect '*) want="${line#\#expect }"; if ! grep -qF "$want" "$log"; then echo "ERROR: falta en el registro: $want"; { echo "No salio '$want'"; grep -nE "\[Server\]|Summoned|Unknown|Invalid|Incorrect|Could not|Expected|Failed|Unable|Found no|block data|Executed" "$log" | tail -14; } > "$log.bad"; ann "Prueba de comandos fallo ($mod)" 16 "$log.bad"; exit 1; fi;; esac
  done < ci_commands.txt
  echo "Comandos de prueba: bien"
fi
grep -E "Done \(" "$log"; echo "Arranque limpio"
