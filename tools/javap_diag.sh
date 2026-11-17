#!/usr/bin/env bash
# Muestra los métodos públicos de clases de Minecraft (para saber qué cambió en una versión nueva).
# Uso: tools/javap_diag.sh <carpeta-del-proyecto> <clase> [<clase> ...]   (después de compilar con gradle)
dir="$1"; shift
jar=""
# con DIAG_MC=<versión> se prefieren los jar de esa versión (la caché de gradle puede traer los de otras)
cands=$(find "$HOME/.gradle" "$HOME/.m2" "$HOME/.mavenizer" "$dir" -name '*.jar' -size +8M 2>/dev/null)
[ -n "${DIAG_MC:-}" ] && cands="$(echo "$cands" | grep -F -- "${DIAG_MC}") $(echo "$cands" | grep -vF -- "${DIAG_MC}")"
for j in $cands; do
  if unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then jar="$j"; break; fi
done
[ -z "$jar" ] && { echo "no encontré el jar de Minecraft"; exit 0; }
echo "JAR: $jar"
for c in "$@"; do
  c="${c//%/\$}"   # en el flujo manual se escribe Clase%Interna (el $ lo come bash)
  echo "===== $c"
  javap -cp "$jar" -public -constants "$c" 2>&1 | head -${DIAG_LINEAS:-150}
done
# miembros privados que contengan un texto: DIAG_PRIVADOS="Clase:texto Clase:texto"
for ct in ${DIAG_PRIVADOS:-}; do
  c="${ct%%:*}"; c="${c//%/\$}"; t="${ct##*:}"
  echo "===== privados de $c con '$t'"
  javap -cp "$jar" -p "$c" 2>/dev/null | grep -i "$t" | head -20
done
# orden de llamadas dentro de un método: DIAG_METODO=Clase#metodo
if [ -n "${DIAG_METODO:-}" ]; then
  for cm in $DIAG_METODO; do
    c="${cm%%#*}"; m="${cm##*#}"
    echo "===== orden en $c.$m()"
    javap -cp "$jar" -c -p "$c" 2>/dev/null | awk -v m="$m" 'index($0, " " m "(") && index($0, ")") && !index($0, "invoke") {on=1; next} on && (/invoke/ || /putfield/ || /Hud|hide|Hidden/) {print} on && /^$/ {exit}' | sed 's/^ *//; s/.*\/\/ //' | cut -c1-160 | head -120
  done
fi
# código completo de un método: DIAG_CODIGO=Clase#metodo
if [ -n "${DIAG_CODIGO:-}" ]; then
  for cm in $DIAG_CODIGO; do
    c="${cm%%#*}"; c="${c//%/\$}"; m="${cm##*#}"
    echo "===== código de $c.$m()"
    javap -cp "$jar" -c -p "$c" 2>/dev/null | awk -v m="$m" 'index($0, " " m "(") && index($0, ")") && !index($0, "invoke") {on=1} on {print} on && /^$/ {exit}' | cut -c1-200 | head -90
  done
fi
exit 0
