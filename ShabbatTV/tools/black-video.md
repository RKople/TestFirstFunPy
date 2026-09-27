# Black-video asset provenance

The v1.10 embedded MP4 failed a real FFmpeg decode with a broken video header.
It is replaced with `app/src/main/assets/black_loop.mp4.gz`: a real, locally generated
2-second 640x360 / 24 fps H.264 baseline, limited-range BT.709 MP4 with silent AAC stereo.
Gzip is just packaging; the player receives a normal decompressed MP4 from its cache.
The Java helper verifies the SHA-256 and writes the cache atomically. It never downloads media.

Uncompressed size: 4885 bytes.
SHA-256: `0b1a30c8ae4375f7d1639099ef8bd9a011773b9d75e04662964913bf8375e1f2`.

Generation command (FFmpeg 7.1.5; exact output varies with encoder version):

```sh
ffmpeg -v error -f lavfi -i 'color=c=black:s=640x360:r=24:d=2' \
  -f lavfi -i 'anullsrc=r=48000:cl=stereo' -t 2 \
  -c:v libx264 -profile:v baseline -level:v 3.0 -pix_fmt yuv420p \
  -preset medium -crf 30 -color_range tv -colorspace bt709 \
  -color_primaries bt709 -color_trc bt709 -c:a aac -b:a 32k \
  -shortest -movflags +faststart black_loop.mp4
```

Local verification decoded all 48 frames: 33,177,600 RGB bytes, every byte zero.
Decoded audio: 385,024 PCM bytes, every byte zero. The same checks run again in CI.
A build checks the complete media bytes, not just whether a file exists or is named black.mp4.
A new asset must update the expected hash and length together and pass the decoder checks.
This proves the bundled clip is black and silent, not that a particular TV firmware stays awake.
