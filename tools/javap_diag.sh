#!/usr/bin/env bash
# Muestra los métodos públicos de clases de Minecraft (para saber qué cambió en una versión nueva).
# Uso: tools/javap_diag.sh <carpeta-del-proyecto> <clase> [<clase> ...]   (después de compilar con gradle)
dir="$1"; shift
jar=""
# con DIAG_MC=<versión> se prefieren los jar de esa versión (la caché de gradle puede traer los de otras)
cands=$(find "$HOME/.gradle" "$HOME/.m2" "$HOME/.mavenizer" "$dir" -name '*.jar' -size +8M 2>/dev/null)
[ -n "${DIAG_MC:-}" ] && cands="$(echo "$cands" | grep -F -- "${DIAG_MC}") $(echo "$cands" | grep -vF -- "${DIAG_MC}")"
# con DIAG_JAR=<texto> se usa el primer jar cuyo nombre lo contenga (por ejemplo "eventbus" para las clases del bus de eventos)
if [ -n "${DIAG_JAR:-}" ]; then
  jar=$(find "$HOME/.gradle" "$HOME/.m2" "$HOME/.mavenizer" "$dir" -name "*${DIAG_JAR}*.jar" 2>/dev/null | grep -v sources | head -1)
  # también para las listas de clases (aunque sea un jar chico)
  [ -n "$jar" ] && cands="$jar $cands"
  # todos los jar que se parezcan (por ejemplo, el de Forge y el de Minecraft de una versión): se muestran para saber cuál usar
  echo "JARS con '${DIAG_JAR}':"; find "$HOME/.gradle" "$HOME/.m2" "$HOME/.mavenizer" "$dir" -name "*${DIAG_JAR}*.jar" 2>/dev/null | grep -v sources | head -12
fi
if [ -z "$jar" ]; then
for j in $cands; do
  if unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then jar="$j"; break; fi
done
fi
[ -z "$jar" ] && { echo "no encontré el jar de Minecraft"; # clases de un paquete en todos los jar (para buscar eventos nuevos): DIAG_LISTA="net/minecraftforge/client/event/ otro/paquete/"
for pre in ${DIAG_LISTA:-}; do
  echo "===== clases en $pre"
  for j in $cands; do
    # con / al final: solo las clases del paquete; sin /: también las internas (Clase$Interna) de lo que empiece así
    case "$pre" in
      */) unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep "^$pre[^/]*\.class$" | grep -v '\$' ;;
      *) unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep "^$pre[^/]*\.class$" ;;
    esac | sed "s|^|$(basename "$(dirname "$j")")/$(basename "$j"): |"
  done | sort -u | head -300
done
exit 0; }
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
# clases de un paquete en todos los jar (para buscar eventos nuevos): DIAG_LISTA="net/minecraftforge/client/event/ otro/paquete/"
for pre in ${DIAG_LISTA:-}; do
  echo "===== clases en $pre"
  for j in $cands; do
    # con / al final: solo las clases del paquete; sin /: también las internas (Clase$Interna) de lo que empiece así
    case "$pre" in
      */) unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep "^$pre[^/]*\.class$" | grep -v '\$' ;;
      *) unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep "^$pre[^/]*\.class$" ;;
    esac | sed "s|^|$(basename "$(dirname "$j")")/$(basename "$j"): |"
  done | sort -u | head -300
done
exit 0
