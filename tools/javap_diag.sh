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
exit 0
