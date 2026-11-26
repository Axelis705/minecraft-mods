#!/bin/bash
# Arma cada versión y saca sus copias para compartir (alta calidad y para Discord, menos de 10 MB); se hace una por una para no llenar el disco.
set -e
cd "$(dirname "$0")"
mkdir -p out/entrega
for f in horizontal vertical; do
  python3 montar.py $f
  m=out/ElColeccionista_trailer_$f.mp4
  ffmpeg -v error -y -i $m -c:v libx264 -crf 21 -preset slow -pix_fmt yuv420p -c:a aac -b:a 192k -movflags +faststart out/entrega/tmp.mp4
  mv out/entrega/tmp.mp4 out/entrega/ElColeccionista_Trailer_${f}.mp4
  if [ $f = horizontal ]; then esc=1280:720; else esc=720:1280; fi
  ffmpeg -v error -y -i $m -vf "scale=$esc:flags=lanczos,hqdn3d=1.5:1.5:4:4" -c:v libx264 -b:v 1150k -preset slow -pass 1 -passlogfile out/entrega/p_$f -an -f mp4 /dev/null
  ffmpeg -v error -y -i $m -vf "scale=$esc:flags=lanczos,hqdn3d=1.5:1.5:4:4" -c:v libx264 -b:v 1150k -maxrate 2000k -bufsize 3000k -preset slow -pass 2 -passlogfile out/entrega/p_$f -pix_fmt yuv420p -c:a aac -b:a 96k -movflags +faststart out/entrega/tmp.mp4
  mv out/entrega/tmp.mp4 out/entrega/ElColeccionista_Trailer_${f}_discord.mp4
  rm -f $m out/audio_$f.wav out/entrega/p_$f*
done
ls -la out/entrega
