#!/usr/bin/env bash
# Sube el .jar compilado a la rama jar-<mod>-<version> (la nube de Claude no puede bajar artefactos de Actions, pero sí ramas).
# Uso: tools/push_jar.sh <mod> <version> <carpeta_libs> <token> <owner/repo> <sha>
set -e
mod=$1; ver=$2; libs=$3; tok=$4; repo=$5; sha=$6
d=$(mktemp -d)
for j in "$libs"/*.jar; do case "$j" in *-sources.jar|*-slim.jar) ;; *) cp "$j" "$d/";; esac; done
cd "$d"
echo "$mod $ver $sha" > LEEME.txt
git init -q -b "jar-$mod-$ver"
git config user.name ci; git config user.email ci@example.com
git add -A; git commit -qm "$mod $ver ($sha)"
git push -f -q "https://x-access-token:$tok@github.com/$repo" "jar-$mod-$ver"
echo "subido a jar-$mod-$ver:"; ls -la
