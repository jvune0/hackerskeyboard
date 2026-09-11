# Hacker's Keyboard (Dead Keys)

A fork of [Hacker's Keyboard](https://github.com/klausw/hackerskeyboard) that adds **Alt dead keys for physical (Bluetooth or USB) keyboards**: Alt+U then a types ä, Alt+E then e types é, Alt+S types ß. The on-screen keyboard and everything else work as in the original. The original README follows [below](#overview).

## Why

On a Huawei MatePad with HarmonyOS 4.3, the physical keyboard layout list always contains a built-in **Auto** layout. With the system language set to English, Auto is the AOSP English (US) layout, which has no Alt characters: Alt+S types a plain `s`. Auto can't be removed from the Ctrl+Space cycle, and system layouts can't be changed without root.

A keyboard app sees hardware key events before the text field does, so this fork provides the Alt combinations itself. It only acts on keys from real physical keyboards, and only when the current layout gives Latin letters. Everything else passes through unchanged: Russian and other Cyrillic layouts (including right Alt + letter for Latin letters), and Ctrl or Meta shortcuts such as Ctrl+Space, Ctrl+C and Ctrl+V.

It asks for no new permissions (no internet access) and installs alongside the original Hacker's Keyboard.

## Key combinations

"Alt" means either Alt key.

| Combination | Result |
|---|---|
| Alt+U | dead diaeresis ¨ |
| Alt+E | dead acute ´ |
| Alt+I | dead circumflex ^ |
| Alt+N | dead tilde ~ |
| Alt+\` | dead grave \` |
| Alt+Shift+\` | dead tilde ~ |
| Alt+Shift+6 | dead circumflex ^ |
| Alt+C / Alt+Shift+C | ç / Ç |
| Alt+S | ß |

After a dead key:

- **A letter** gives the accented letter: ä, é, î, ñ, à. Shift or Caps Lock gives the capital: Ä. If there is no accented form, you get the accent followed by the character: ´x.
- **Space** gives the accent on its own: ¨.
- **Another dead key** gives the first accent on its own and waits for a letter for the new one.
- **Backspace or Esc** cancels the accent.
- **Enter, Tab, arrows, other non-character keys and Ctrl or Meta shortcuts** give the accent on its own, then do what they normally do.
- **Shift, Alt, Ctrl or Caps Lock** pressed alone keep the accent waiting.

Shift makes no difference for Alt+U, Alt+E, Alt+I and Alt+N. Alt+Shift+S and Alt+6 are left unchanged.

In terminal apps, the combinations above type accents, so they no longer work as Alt shortcuts there (such as readline's Alt+U and Alt+C). Other Alt combinations are unchanged.

## Installation

1. On the tablet, download the latest `hackerskeyboard-deadkeys-*.apk` from [Releases](https://github.com/jvune0/hackerskeyboard/releases) and open it. If HarmonyOS blocks installing apps from outside AppGallery, allow it when prompted.
2. Select **Hacker's Keyboard (Dead Keys)** in **Settings → System & updates → Language & input → Default keyboard**. Android shows a warning that the keyboard can collect what you type; it shows this for every keyboard app.
3. Set up the physical keyboard layouts as described below.

## Physical keyboard layouts (HarmonyOS 4.3)

Keep the system language English and use **Russian** as the only extra layout:

1. Connect the keyboard and open **Settings → Accessibility features → Keyboard**.
2. For each keyboard, remove the extra layouts (such as **ExKeyMo Layout**) and keep **Russian**.
3. Ctrl+Space now switches between **Auto** (English with dead keys from this app) and **Russian**.

Repeat this for every keyboard you pair.

## Testing

Open the [key event viewer](https://w3c.github.io/uievents/tools/key-event-viewer.html) in Chrome, tap its input field and type on the physical keyboard. It lists the key events and the text that reaches the field. Then check:

- Alt+U then a, o, u, Shift+A → ä, ö, ü, Ä; Alt+S → ß; Alt+Shift+C → Ç.
- Alt+E then x → ´x; Alt+U then Space → ¨.
- Russian layout: Cyrillic typing is unchanged, and right Alt + letter still types Latin.
- Ctrl+Space switches layouts; Ctrl+C, Ctrl+V and other shortcuts work.
- The on-screen keyboard works as before, including suggestions.
- It works in a plain text field, in Chrome and in a terminal app.

## Builds and releases

GitHub Actions runs the unit tests and builds a debug APK on every push and pull request; the APK is attached to the run as the `app-debug` artifact. Debug APKs are signed with a temporary key that can change between runs; if installing a newer one fails, uninstall the previous one first. Pushing a tag that starts with `v` builds a signed release APK and publishes it as a GitHub Release; releases install as updates of each other.

To build locally you need JDK 17 and the Android SDK:

```sh
./gradlew testDebugUnitTest assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
```

### Create the signing key (once)

`keytool` comes with any JDK (for example `sudo apt install openjdk-17-jdk-headless`):

```sh
keytool -genkeypair -v -keystore deadkeys-release.jks -alias deadkeys \
  -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 deadkeys-release.jks > deadkeys-release.jks.b64   # macOS: base64 -i deadkeys-release.jks -o deadkeys-release.jks.b64
```

keytool asks for a password and the certificate details. It creates a PKCS12 keystore, where the key password is the same as the keystore password.

**Back up `deadkeys-release.jks` and its password, and never commit them** (`.gitignore` excludes `*.jks` and `*.jks.b64`). Android only installs updates signed with the same key. If the key is lost, you have to uninstall the app and install a build signed with a new key, which loses the keyboard's settings and learned words.

### Add the secrets

In the GitHub repository, open **Settings → Secrets and variables → Actions** and add these repository secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | contents of `deadkeys-release.jks.b64` |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `deadkeys` |
| `KEY_PASSWORD` | the key password (the same as the keystore password) |

Then delete `deadkeys-release.jks.b64`.

### Publish a release

```sh
git tag v1.41.1-dk1
git push origin v1.41.1-dk1
```

The tag becomes the app's version name, and the version code grows with every release run, so each release installs as an update.

## License

Apache License 2.0, like the original. See [LICENSE](LICENSE), and [NOTICE](NOTICE) for what this fork changes.

---

## Overview ##

**WARNING:** *This is a rather ancient project that was originally developed back in 2011 based on the Android 2.3 (Gingerbread) AOSP keyboard. While it still works as-is for many users, it would need some major rewrites to work with newer APIs, and some features such as language switching or popup keys don't work right on modern Android systems. I'm not currently planning on significant updates, and it's possible that it will stop working on modern devices or will no longer be updateable via the Google Play store due to minimum API level requirements. Play Store requires targeting API level 29 (Android 10), while the code was written for API level 9 (Android 2.3) from 2011.*

Are you missing the key layout you're used to from your computer when using an Android device? This software keyboard has separate number keys, punctuation in the usual places, and arrow keys. It is based on the AOSP Gingerbread soft keyboard, so it supports multitouch for the modifier keys.

This keyboard is especially useful if you use ConnectBot for SSH access. It provides working Tab/Ctrl/Esc keys, and the arrow keys are essential for devices such as the Xoom tablet or Nexus S that don't have a trackball or D-Pad.

The supported keyboard layouts include Armenian (Հայերեն), Arabic (العربية),
British (en\_GB), Bulgarian (български език), Czech (Čeština), Danish (dansk),
Carpalx English (language "en-CX"), Dvorak English (language "en-DV"), English
(QWERTY), Finnish (Suomi), French (Français, AZERTY), German (Deutsch, QWERTZ),
German Neo2 (Deutsch, language "de-NE"),
Greek (ελληνικά), Hebrew (עברית), Hungarian (Magyar), Italian (Italiano), Lao
(ພາສາລາວ), Norwegian (Norsk bokmål), Persian (فارسی), Portuguese (Português),
Romanian (Română), Russian (Русский), Russian phonetic (Русский, ru-rPH),
Serbian (Српски), Slovak (Slovenčina), Slovenian
(Slovenščina)/Bosnian/Croatian/Latin Serbian, Spanish (Español, Español
Latinoamérica), Swedish (Svenska), Tamil (தமிழ்), Thai (ไทย), Turkish (Türkçe),
and Ukrainian (українська мова).

To install, get **[Hacker's
Keyboard](https://play.google.com/store/apps/details?id=org.pocketworkstation.pckeyboard)**
from the Play Store, plus optional [dictionary
packs](https://play.google.com/store/apps/developer?id=Klaus+Weidner).

## Additional resources ##

See the **[Release Notes](https://github.com/klausw/hackerskeyboard/wiki/ReleaseNotes)** for changes in the Play Store released versions.

Having problems? See the **[User's Guide](https://github.com/klausw/hackerskeyboard/wiki/UsersGuide)** and **[FAQ](https://github.com/klausw/hackerskeyboard/wiki/FrequentlyAskedQuestions)**, and check the [issue tracker](https://github.com/klausw/hackerskeyboard/issues) for known bugs or filing new ones.

Comments, requests, or contributions? Join the [discussion group](http://groups.google.com/group/hackerskeyboard/).

Application developers: see [the page about keyboard support in applications](https://github.com/klausw/hackerskeyboard/wiki/KeyboardSupportInApplications) if you want to enable the additional keys in your Android application, the same method also works for hardware USB or Bluetooth keyboards.

![hk-5row-en-s.png](hk-5row-en-s.png)
