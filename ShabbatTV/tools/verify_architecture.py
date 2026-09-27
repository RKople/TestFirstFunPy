#!/usr/bin/env python3
"""Build-time checks only. No TV or private Plex access is performed."""
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/fr/shabbattv'
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
permissions = {e.attrib[ns + 'name'] for e in manifest.findall('uses-permission')}
assert permissions == {'android.permission.INTERNET', 'android.permission.WAKE_LOCK'}, permissions
assert not manifest.findall('.//receiver'), 'Legacy receiver remains registered'
removed = ['AlarmReceiver', 'AlarmTools', 'ArmedActivity', 'BootReceiver', 'PhilipsPairActivity',
           'PhilipsTvClient', 'PlaybackActivity', 'PreWakeReceiver', 'RobustWakeReceiver',
           'ScheduleReceiver', 'SleepHelper', 'SleepTestActivity', 'TestActivity', 'WaitingActivity', 'WakeReceiver']
for name in removed:
    assert not (src / (name + '.java')).exists(), name
for file in src.glob('*.java'):
    if file.name == 'UpgradeCleanup.java':
        continue  # One-time cancellation is the only permitted AlarmManager use.
    text = file.read_text()
    for forbidden in ('AlarmManager', 'setAlarmClock(', 'setExactAndAllowWhileIdle(',
                      'ACQUIRE_CAUSES_WAKEUP', 'setTurnScreenOn(', 'SCREEN_BRIGHT_WAKE_LOCK',
                      'SleepHelper.', 'PhilipsTvClient.'):
        assert forbidden not in text, (file.name, forbidden)
mode = (src / 'ShabbatModeActivity.java').read_text()
assert 'Player.REPEAT_MODE_ONE' in mode and 'keeper.setVolume(0f)' in mode
assert 'FLAG_KEEP_SCREEN_ON' in mode and 'MediaSession.Builder' in mode
assert 'onRenderedFirstFrame' in mode and 'lastProgressAt' in mode
player = (src / 'PlayerActivity.java').read_text()
assert 'putExtra("returning", true)' in player
assert 'hasFutureSchedule' not in player, 'Last film must also return to black'

# Decode the actual bundled media, not just a filename or source-code assertion.
data = gzip.decompress((root / 'app/src/main/assets/black_loop.mp4.gz').read_bytes())
sha = hashlib.sha256(data).hexdigest()
assert sha == '0b1a30c8ae4375f7d1639099ef8bd9a011773b9d75e04662964913bf8375e1f2'
assert sha in (src / 'BlackPlaybackAsset.java').read_text()
assert shutil.which('ffmpeg'), 'ffmpeg is required for the media verification'
with tempfile.TemporaryDirectory() as temp:
    mp4 = Path(temp) / 'black.mp4'
    mp4.write_bytes(data)
    frames = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(mp4), '-map', '0:v:0',
        '-pix_fmt', 'rgb24', '-f', 'rawvideo', '-'], timeout=30)
    assert frames and max(frames) == 0, 'The video is not decoded RGB black'
    audio = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(mp4), '-map', '0:a:0',
        '-acodec', 'pcm_s16le', '-f', 's16le', '-'], timeout=30)
    assert audio and max(audio) == 0, 'The audio is not digitally silent'
report = {'architecture': 'PASS', 'legacy_classes_removed': len(removed),
          'black_rgb': 'PASS', 'silent_audio': 'PASS', 'embedded_mp4_bytes': len(data),
          'black_video_sha256': sha,
          'tv_hardware_test': 'NOT RUN', 'remote_plex_playback_test': 'NOT RUN'}
(root / 'verification-report.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
