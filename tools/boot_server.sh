#!/usr/bin/env bash
# Arranca un servidor Forge real con el mod y comprueba que cargue sin errores.
# Uso: tools/boot_server.sh <carpeta-del-mod>   (se ejecuta desde la raiz del repo)
set -u
mod="$1"; cd "$mod"
mkdir -p run; echo "eula=true" > run/eula.txt
# propiedades del servidor de prueba opcionales del mod (por ejemplo un mundo plano para probar estructuras)
[ -f ci_server.properties ] && cp ci_server.properties run/server.properties
# desde 1.21.2 el servidor se "pausa" a los 60 s sin jugadores (las entidades ya no se mueven): en las pruebas no
mkdir -p run; grep -q "^pause-when-empty-seconds" run/server.properties 2>/dev/null || echo "pause-when-empty-seconds=0" >> run/server.properties
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
    case "$line" in
      ''|'#expect '*) continue;;
      '#pausa '*)
        # "#pausa SEGUNDOS": dejar que el servidor termine algo (por ejemplo, cargar los chunks de un forceload)
        sleep "${line#\#pausa }"; continue;;
      '#wait '*)
        # "#wait TEXTO SEGUNDOS": esperar a que TEXTO salga en el registro (por ejemplo, el final de una construcción larga)
        w="${line#\#wait }"; txt="${w% *}"; secs="${w##* }"
        for i in $(seq 1 "$secs"); do
          if grep -qF "$txt" "$log" 2>/dev/null || grep -qF "$txt" run/logs/latest.log 2>/dev/null; then break; fi
          if grep -q '^exit=' "$log"; then break; fi
          sleep 1
        done
        continue;;
    esac
    echo "$line" >&3; sleep 1
  done < ci_commands.txt
  # esperar a que el servidor termine de procesar todo (en una máquina lenta las construcciones grandes tardan); máximo 3 minutos
  echo "say CI_FIN_COMANDOS" >&3
  for i in $(seq 1 90); do
    if grep -q 'CI_FIN_COMANDOS' "$log" 2>/dev/null || grep -q 'CI_FIN_COMANDOS' run/logs/latest.log 2>/dev/null; then break; fi
    if grep -q '^exit=' "$log"; then break; fi
    sleep 2
  done
  sleep 4
fi
echo "stop" >&3; sleep 8; exec 3>&-
pkill -f "runServer" 2>/dev/null || true
# el registro del archivo también cuenta (en 26.x la consola a veces deja de escribir)
[ -f run/logs/latest.log ] && { echo "----- run/logs/latest.log -----"; cat run/logs/latest.log; } >> "$log"
echo "===== resumen del arranque de $mod ====="
ann() { # manda un texto como anotacion visible en GitHub (varias lineas)
  local t; t=$(tail -n "$2" "$3" | cut -c1-300 | sed 's/%/%25/g' | awk '{printf "%s%%0A", $0}')
  echo "::error title=$1::${t}"
}
# lo que dijeron las pruebas (también si luego falla algo, para saber hasta dónde llegó)
if [ -f ci_commands.txt ]; then
  echo "----- fallos de las pruebas (primero) -----"
  grep -E "_FALLO|unexpected error|An unexpected|Exception caught|Exception while" "$log" | sed 's/^.*\] \[/[/' | cut -c1-600 | head -20
  echo "----- lineas de prueba del registro -----"
  grep -E "PRUEBA_|DIAG_|Unknown or incomplete command|unexpected error|Incorrect argument|Can't keep up" "$log" | sed 's/^.*\] \[/[/' | cut -c1-300 | sort -u | head -80
  echo "----- final del registro -----"
  grep -vE "^\s*at |^----- run/logs" "$log" | tail -25 | cut -c1-300
  echo "-----"
fi
if [ $ok -ne 1 ]; then echo "ERROR: el servidor no llego a 'Done'"; tail -80 "$log"; grep -nE "Caused by|Exception|Error|/ERROR\]|/FATAL\]|Invalid|Unable|Failed" "$log" | grep -vE "jarjar|PlatformDependent|setAccessible|server.properties|/DEBUG\]" | head -25 > "$log.why"; ann "Servidor no arranco ($mod)" 25 "$log.why"; exit 1; fi
bad=$(grep -E "/(ERROR|FATAL)\]|Missing registry|Couldn't parse|Parsing error|Failed to load (recipe|advancement|loot|function|tag)|Exception in thread|Mod .* failed|Failed to start the minecraft server|Crash" "$log" \
  | grep -vE "server.properties|PlatformDependent|jarjar|ForgeGradle|Reflective setAccessible|/DEBUG\]" | head -30)
if [ -n "$bad" ]; then echo "ERROR: hay errores en el registro del servidor:"; echo "$bad"; echo "$bad" > "$log.bad"; ann "Errores en el servidor ($mod)" 30 "$log.bad"; exit 1; fi
if [ -f ci_commands.txt ]; then
  # lo mas importante del registro del servidor, visible en el registro de la accion
  echo "----- registro de las pruebas de comandos -----"
  grep -E "construida|sin apoyo|DIAG_|has the following|PRUEBA_" "$log" | sed 's/^.*\] \[/[/' | cut -c1-400 | head -150
  echo "-----"
  miss=""
  while IFS= read -r line; do
    case "$line" in '#expect '*) want="${line#\#expect }"; grep -qF "$want" "$log" || miss="$miss $want";; esac
  done < ci_commands.txt
  if [ -n "$miss" ]; then
    echo "ERROR: faltan en el registro:$miss"
    { echo "Faltan:$miss"; grep -E "_FALLO" "$log" | sed 's/^.*\] \[/[/' | cut -c1-300 | head -10; grep -E "DIAG_|PRUEBA_|/ERROR\]|/WARN\].*casino|Exception|unexpected error|at com\.casino|not loaded" "$log" | sed 's/^.*\] \[/[/' | cut -c1-200 | head -60; } > "$log.bad"
    ann "Prueba de comandos fallo ($mod)" 62 "$log.bad"; exit 1
  fi
  echo "Comandos de prueba: bien"
fi
grep -E "Done \(" "$log"; echo "Arranque limpio"
