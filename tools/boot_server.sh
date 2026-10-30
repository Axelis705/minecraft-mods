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
if [ $ok -ne 1 ]; then echo "ERROR: el servidor no llego a 'Done'"; tail -80 "$log"; exit 1; fi
bad=$(grep -E "\[(Server thread|main)/(ERROR|FATAL)\]|Exception|Failed to load|Missing registry|Couldn't parse|Parsing error" "$log" | grep -v "ForgeGradle" | head -30)
if [ -n "$bad" ]; then echo "ERROR: hay errores en el registro del servidor:"; echo "$bad"; exit 1; fi
grep -E "Done \(" "$log"; echo "Arranque limpio"
