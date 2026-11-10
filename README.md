# Dorfikstrap

A small open-source Android launcher for Roblox with a friendly **FastFlag editor**, written from scratch
(Material 3). Inspired by Bloxstrap and Chevstrap. It does **not** modify or repackage the Roblox client.

> Not affiliated with, endorsed by, or sponsored by Roblox Corporation. "Roblox" is a trademark of
> Roblox Corporation. Use FastFlags at your own risk: Roblox may ignore or block some flags, and
> changing some of them may be considered a violation of its rules.

## How it works

Roblox for Android reads an optional `ClientAppSettings.json` from `/data/local/tmp/` when it starts.
Dorfikstrap writes your flags there through [Shizuku](https://shizuku.rikka.app/) (shell access, no root),
then restarts and launches Roblox.

## Features

- Launch Roblox with your FastFlags applied (also as a home-screen shortcut with your own name)
- FastFlag manager: on/off switch per flag, add by name and value or paste a JSON, delete all
- Material 3 dynamic colours, or an AMOLED black theme with a green accent; three icon variants
- Languages: English, Українська, Русский, Polski, Deutsch, Español, Português (Brasil), Türkçe
  (translations other than English and Ukrainian are machine-assisted; fixes are welcome)

## Requirements

- Android 8.0 (API 26) or newer
- [Shizuku](https://shizuku.rikka.app/) installed and running (Wireless debugging is enough)
- Roblox installed

## Usage

1. Start Shizuku, open Dorfikstrap and tap **Allow Shizuku**.
2. Settings → FastFlags → **Add FastFlag** (or paste a JSON).
3. **Apply and launch**.

Flags that the client does not know are ignored. If Roblox misbehaves, turn off **Use FastFlags**
or tap **Delete all FastFlags**.

## Not included (yet)

- Discord Rich Presence (needs the official Discord SDK and an application ID)
- Asset modding (emoji, fonts, sounds): Roblox's content is not reachable without modifying the client
- Roblox Studio: no Android version exists

## Building

`gradle :app:assembleDebug` (JDK 17, Android SDK). GitHub Actions builds every push; pushing a tag
`vX.Y.Z` builds a release APK and attaches it to a GitHub Release. Release signing uses the optional
secrets `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; without them the APK is signed
with a debug key (it will not update over a differently signed build).

## License

MIT.
