# Musta IPTV 0.3.0

Android TV IPTV player with a set-top-box style interface (title and clock on top, live preview on the left,
list on the right, colored-key hints at the bottom). Ships with no channels: add only sources you have the right to use.

## Features
- Xtream codes: up to 30 servers (change `Limits` in `data/Models.kt` to raise it)
- Stalker / MAC portals: up to 30 (MAC prefilled with 00:1A:79:, must be the one registered with your provider)
- M3U / M3U8 links sent from your phone (the TV shows an address; open it on your phone)
- Import servers from a TXT list: from the phone (browser file picker) or from the box (file picker, or a .txt in the app folder)
- Categories, channel list, favorites, live preview, full screen, zapping, aspect ratio, previous channel, restart stream

## Remote keys
| key | does |
| --- | --- |
| Red / 1 | add M3U (providers screen) - favorites (categories) - EPG (channels) |
| Green / 2 | add Stalker portal |
| Yellow / 3 | add Xtream (providers) - actions (other screens) |
| Blue / 4 | import a file (providers) - back to categories (channels, player) |
| OK | open / play (OK again on the playing channel = full screen) |
| Long OK, Menu or Info | actions on a server or channel |
| Left / Right | categories: Left = menu, Right = channel list |
| Up / Down or CH+ / CH- | next / previous channel in full screen |

## TXT list format (one server per line)
```
http://host/playlist.m3u
http://host:8080 username password
http://host/get.php?username=U&password=P&type=m3u_plus
http://portal-host/c/ 00:1A:79:AA:BB:CC
# lines starting with # are ignored
```

## Build the APK (GitHub Actions)
1. Create a new GitHub repository and upload the contents of this folder.
2. In the repo choose Add file > Create new file, name it `.github/workflows/build-apk.yml` and paste the file from this folder.
3. Actions > Build APK > Run workflow. Download `musta-iptv-debug-apk` from the run page and unzip it.
Install with ADB (`adb install -r app-debug.apk`) or from a USB stick.

## Status
Not compiled or run in the environment that wrote it. Stalker is untested against a real portal. EPG and settings are placeholders.
