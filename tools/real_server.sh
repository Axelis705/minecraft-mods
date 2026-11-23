#!/usr/bin/env bash
# Arranca un servidor Forge REAL (instalado con el instalador oficial, como el de un jugador) con los jars dados en mods/, le manda
# comandos y comprueba lo que sale en el registro. Sirve para probar lo que el entorno de desarrollo no prueba: que un mod que lleva
# Axelis Core dentro (Jar-in-Jar) lo cargue de verdad.
# Uso: tools/real_server.sh <mc> <forge> <carpeta> <comandos.txt> <jar>...
#   comandos.txt: un comando por línea; "#expect TEXTO" exige que TEXTO salga en el registro; "#wait N" espera N segundos.
set -u
mc=$1; forge=$2; dir=$3; cmds=$(realpath "$4"); shift 4
jars=(); for j in "$@"; do jars+=("$(realpath "$j")"); done
mkdir -p "$dir" && cd "$dir"
url="https://maven.minecraftforge.net/net/minecraftforge/forge/$mc-$forge/forge-$mc-$forge-installer.jar"
curl -sSfL --retry 4 -o installer.jar "$url" || { echo "::error title=Servidor real $mc::no se pudo bajar $url"; exit 1; }
java -jar installer.jar --installServer . > install.log 2>&1 || { tail -40 install.log; echo "::error title=Servidor real $mc::falló el instalador"; exit 1; }
echo "eula=true" > eula.txt
printf 'online-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\nmax-tick-time=-1\n' > server.properties
mkdir -p mods; for j in "${jars[@]}"; do cp "$j" mods/; done
echo "mods/:"; ls -la mods
if [ -f run.sh ]; then
  start=(bash run.sh nogui)
else
  shim=$(ls forge-*-shim.jar forge-*-server.jar forge-*.jar 2>/dev/null | grep -v installer | head -1)
  start=(java -jar "$shim" nogui)
fi
echo "arranque: ${start[*]}"
log="$(pwd)/server_out.log"; fifo="$(mktemp -u)"; mkfifo "$fifo"
("${start[@]}" < "$fifo" > "$log" 2>&1; echo "exit=$?" >> "$log") &
exec 3> "$fifo"
ok=0
for i in $(seq 1 300); do
  if grep -q 'Done (' "$log"; then ok=1; break; fi
  if grep -q '^exit=' "$log"; then break; fi
  sleep 2
done
if [ $ok -eq 1 ]; then
  while IFS= read -r line; do
    case "$line" in
      ''|'#expect '*) continue;;
      '#wait '*) sleep "${line#\#wait }"; continue;;
    esac
    echo "$line" >&3; sleep 1
  done < "$cmds"
  sleep 4
fi
echo "stop" >&3; sleep 10; exec 3>&-
pkill -f "nogui" 2>/dev/null || true
[ -f logs/latest.log ] && { echo "----- logs/latest.log -----"; cat logs/latest.log; } >> "$log"
echo "===== servidor real $mc ====="
grep -E "Axelis Core|AXCORE_|PRUEBA_|Loading [0-9]+ mods|- [a-z_]+ [0-9]|jarjar|JarJar|Exception|ERROR" "$log" | cut -c1-300 | sort -u | head -80
fail=0
if [ $ok -ne 1 ]; then echo "::error title=Servidor real $mc::no llegó a Done"; tail -60 "$log"; fail=1; fi
while IFS= read -r line; do
  case "$line" in
    '#expect '*)
      t="${line#\#expect }"
      if grep -qF "$t" "$log"; then echo "OK: $t"; else echo "::error title=Servidor real $mc::no salió '$t'"; fail=1; fi;;
  esac
done < "$cmds"
exit $fail
