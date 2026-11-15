#!/usr/bin/env bash
# Muestra los métodos públicos de clases de Minecraft (para saber qué cambió en una versión nueva).
# Uso: tools/javap_diag.sh <carpeta-del-proyecto> <clase> [<clase> ...]   (después de compilar con gradle)
dir="$1"; shift
jar=""
for j in $(find "$HOME/.gradle" "$HOME/.m2" "$HOME/.mavenizer" "$dir" -name '*.jar' -size +8M 2>/dev/null); do
  if unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then jar="$j"; break; fi
done
[ -z "$jar" ] && { echo "no encontré el jar de Minecraft"; exit 0; }
echo "JAR: $jar"
for c in "$@"; do
  echo "===== $c"
  javap -cp "$jar" -public "$c" 2>&1 | head -150
done
# orden de llamadas dentro de un método: DIAG_METODO=Clase#metodo
if [ -n "${DIAG_METODO:-}" ]; then
  for cm in $DIAG_METODO; do
    c="${cm%%#*}"; m="${cm##*#}"
    echo "===== orden en $c.$m()"
    javap -cp "$jar" -c -p "$c" 2>/dev/null | awk -v m="$m" '$0 ~ " "m"\(" && /\(/ {on=1} on && /invoke/ {print} on && /^$/ {if (on) exit}' | sed 's/^ *//' | cut -c1-200 | head -80
  done
fi
exit 0
