#!/bin/bash
# Los cuatro videos de la cuenta regresiva (7, 5, 3 y 1 día): vertical en alta (TikTok, Reels, Shorts) y horizontal para Discord.
set -e
cd "$(dirname "$0")"
mkdir -p out/entrega
for n in 7 5 3 1; do
  for f in vertical horizontal; do
    CUENTA=$n python3 cuenta.py $f
    m=out/ElColeccionista_Faltan${n}_$f.mp4
    if [ $f = vertical ]; then
      ffmpeg -v error -y -i $m -c:v libx264 -crf 21 -preset slow -pix_fmt yuv420p -c:a aac -b:a 192k -movflags +faststart out/entrega/tmp.mp4
      mv out/entrega/tmp.mp4 out/entrega/Faltan${n}_vertical.mp4
    else
      ffmpeg -v error -y -i $m -vf "scale=1280:720:flags=lanczos,hqdn3d=1.5:1.5:4:4" -c:v libx264 -b:v 3500k -maxrate 5000k -bufsize 7000k -preset slow -pix_fmt yuv420p -c:a aac -b:a 128k -movflags +faststart out/entrega/tmp.mp4
      mv out/entrega/tmp.mp4 out/entrega/Faltan${n}_horizontal_discord.mp4
    fi
    rm -f $m out/caudio${n}_$f.wav
  done
done
ls -la out/entrega | grep Faltan
