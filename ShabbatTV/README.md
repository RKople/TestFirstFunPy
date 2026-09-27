# Shabbat TV 1.11 beta

## One operating mode

Configure Plex, select films/audio/subtitles, create sessions and press **Démarrer le mode Shabbat**.
The app stays in the foreground and plays an embedded, silent, black MP4 on repeat. Ten minutes
before the next session it overlays a countdown, while the video continues playing underneath.
At the scheduled time it hands off to the internal Plex player. After the film (including the
last one), or a Plex playback failure, it returns to the same black-video mode. Stop manually
with Back when finished. Normal TV standby can then be used again.

A single 20-minute end-to-end test uses the exact same scheduling path. The manual film preview
only checks Plex playback and the chosen audio/subtitles.

## Removed

No standby/wake cycle, Philips pairing/power API, Wake-on-LAN, Chromecast wake, boot receiver,
exact alarms, competing wake tests or automatic final-film shutdown. Fifteen abandoned Java
classes and the exact-alarm/boot permissions were removed. UpgradeCleanup only cancels alarms
left by previous versions; it never schedules an alarm. The existing preference namespace is
retained, so a compatible in-place update preserves the Plex connection and stored sessions.
Unrelated Python projects in the repository are not modified. The previous app source is kept
on branch `shabbat-tv-backup-v1-10`.

## Timing and error handling

Sessions are ordered by their stored timestamp. A film is never intentionally started early.
The trigger has a 500 ms polling interval; actual first-image time additionally depends on Plex
and decoding. A delay of at most 60 seconds can be recovered; older missed sessions are logged
and skipped instead of being replayed hours later. Known-duration schedule overlaps are rejected.
An ongoing film is not interrupted by another deadline. Keep enough margin between sessions,
especially when duration is unknown. Audio/subtitle choices are snapshots stored per session.

The waiting engine stops its callbacks and releases its video/session during movie playback.
A session is consumed when playback starts, or after a terminal failure. Plex buffering stalled
for 90 seconds returns to black so later sessions are not indefinitely blocked. Black-video
errors or 30-second progress stalls have three bounded recovery attempts; repeated failure
stops the mode visibly rather than pretending a static black screen is safe. The journal keeps
start/end/first-frame events, actual delay, interruptions, and a waiting heartbeat every 30 minutes.

## TV setup and limitations

Check the TV clock; disable its sleep timer and inactivity power-off, and turn Ambilight off for
a dark room. Do not press Power or Home during a run. Keep the app in the foreground. Normal
Android keep-screen-on protection and actual playback do not guarantee bypassing every
manufacturer timer or screen-protection mechanism. Do not disable panel-protection maintenance.
This is not a deep-standby wake solution and does not automatically resume after power loss.

Before unattended use, validate on the actual Philips: at least 8 hours of black video, a scheduled
Plex film, return to black after the last film, a second session, a Plex/network failure and manual
exit. This hardware validation has **not** been performed by the automated build.

## Build and checks

From `ShabbatTV`, with JDK 17, Android SDK 35 and Gradle 8.9:

```sh
python3 tools/verify_architecture.py  # requires ffmpeg
 gradle :app:testDebugUnitTest :app:assembleDebug
```

The verifier checks the minimal manifest, removal of wake code, loop/silent-playback wiring,
and decodes every embedded MP4 frame to verify RGB black and silent PCM audio. JVM tests cover
schedule boundaries, lateness, overlaps, midnight and overflow. These do not test the TV firmware
or the remote Plex server. GitHub Actions publishes `ShabbatTV-v1.11.apk` only after these checks.

APK signing follows the existing debug-build workflow. A debug key generated on a fresh runner
may differ from an already installed APK: in-place installation is not guaranteed. Do not uninstall
an existing app without first recording the planning and Plex setup; uninstalling erases app data.
Production distribution needs a securely persisted signing key (not a key committed to this repo).
