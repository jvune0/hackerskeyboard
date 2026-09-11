# Task: Hardware-keyboard dead keys in a fork of Hacker's Keyboard

Fork [Hacker's Keyboard](https://github.com/klausw/hackerskeyboard) (Apache 2.0) and add Alt-based dead keys for **physical** keyboards, while keeping the on-screen keyboard and all existing behavior intact.

## Background

- Device: Huawei MatePad running HarmonyOS 4.3 (Android 12 / API 31 base) with several Bluetooth keyboards.
- Stock Android's base physical layout (`Generic.kcm`) maps Alt+U to a combining diaeresis, Alt+S to ß, etc. On this HarmonyOS version the physical-keyboard layout picker has a built-in **Auto** entry that, with an English system language, resolves to AOSP `keyboard_layout_english_us.kcm`, which has no Alt characters. **Auto** can't be removed from the Ctrl+Space cycle, and system layouts can't be patched without root.
- Verified with a key event viewer: the Alt modifier does reach apps (reported as right Alt), the layout just maps Alt+S to plain `s`.
- Hacker's Keyboard currently passes hardware key events through untouched, so the app receives characters from the system layout. That must stay true for everything except the combinations below.
- Target setup: system language English, physical layouts = **Russian** only, Ctrl+Space cycles **Auto** ↔ **Russian**, dead keys provided by this IME.

## Goals

- Alt dead keys work with every hardware keyboard, in any Latin system layout.
- Nothing changes for the on-screen keyboard, Cyrillic layouts, shortcuts, or any other key.
- No new permissions (the upstream manifest has VIBRATE and READ/WRITE_USER_DICTIONARY; keep it that way, never add INTERNET).
- The project builds with current tooling and CI produces an installable APK.

## Behavior

"Alt" means the right Alt key only. Left Alt is an ordinary shortcut modifier (like Ctrl and Meta) and is never used for dead keys.

| Combination | Result |
|---|---|
| Alt+U | dead diaeresis (U+0308) |
| Alt+E | dead acute (U+0301) |
| Alt+I | dead circumflex (U+0302) |
| Alt+N | dead tilde (U+0303) |
| Alt+` | dead grave (U+0300) |
| Alt+Shift+` | dead tilde (U+0303) |
| Alt+Shift+6 | dead circumflex (U+0302) |
| Alt+C / Alt+Shift+C | ç / Ç |
| Alt+S | ß |

Rules:

1. Only handle events from a physical keyboard: `(event.getSource() & InputDevice.SOURCE_KEYBOARD) != 0` and the device is not virtual (`InputDevice.getDevice(id)` is non-null and `!isVirtual()`). Everything else follows the existing code path.
2. Intercept a combination only if right Alt is pressed and left Alt, Ctrl and Meta are not, and the key's base character in the current layout is Latin (`event.getUnicodeChar(0)` in `a..z`, or `` ` `` / `6` for those keys). With a Cyrillic layout everything passes through, so Russian's right-Alt Latin letters keep working.
3. An intercepted press is consumed completely: keyDown (including auto-repeat) and the matching keyUp. Track consumed key codes.
4. After a dead key, the next printable press is consumed too:
   - get the character honoring Shift/Caps Lock: `event.getUnicodeChar(metaState with Alt bits cleared)`;
   - compose with `java.text.Normalizer` (NFC); if `char + combining mark` normalizes to a single code point, commit it;
   - otherwise commit the spacing accent (¨ U+00A8, ´ U+00B4, `^`, `~`, `` ` ``) followed by the character;
   - Space commits the spacing accent;
   - another dead key commits the previous spacing accent and becomes the pending one.
5. Non-printable keys while an accent is pending: Backspace and Escape cancel it (consumed); Enter, Tab and arrows commit the spacing accent and then pass through; lone modifiers (Shift, Alt, Ctrl, Caps Lock) keep the pending state.
6. Clear the pending accent in `onStartInput`/`onFinishInput`.
7. Never interfere with Ctrl+Space or other system shortcuts.

## Implementation notes

- Hook point: `onKeyDown` / `onKeyUp` in `app/src/main/java/org/pocketworkstation/pckeyboard/LatinIME.java`. Upstream only handles Back, volume and D-pad there; run the dead-key handler first and fall back to the existing code when it returns "pass".
- Before committing composed text, finish any in-progress word the way the IME already does (see `commitTyped(InputConnection, boolean)`, `mPredicting`, `finishComposingText()`), so suggestions don't duplicate or swallow text.
- Put the mapping and composition logic in a separate pure-Java class with no Android dependencies (input: key code, base char, char with modifiers, modifier flags; output: pass / consume / commit(text) / commit-then-pass). Keep the project in Java for consistency with upstream.
- JVM unit tests (JUnit) for that class: every combination, capitals (Ä, Ü, Ç), non-composable characters, Space, Backspace/Escape, repeated dead keys, Cyrillic pass-through, Ctrl/Meta pass-through, key-up consumption.

## Build modernization

Upstream uses Android Gradle Plugin 3.2.1, `compileSdkVersion 26`, `minSdkVersion 14`, `targetSdkVersion 26`, and native dictionary code built with CMake (`app/CMakeLists.txt`, `app/src/main/cpp`).

- Add the Gradle wrapper and upgrade to a current AGP/Gradle, add `namespace`, update `compileSdk` and the NDK/CMake configuration. Raise `minSdk` only as far as the toolchain requires.
- Keep `targetSdk` as low as the tooling allows; if it must go to 31+, handle `android:exported` and any other required manifest changes and note them.
- Keep changes minimal and separate them into their own commits (build modernization before the feature).

## Identity

- Change `applicationId` (e.g. `org.pocketworkstation.pckeyboard.deadkeys`) and the IME label (e.g. "Hacker's Keyboard (Dead Keys)") so it installs alongside the original, which is signed with a different key.
- Keep upstream attribution and license; add a short NOTICE of modifications as Apache 2.0 requires.

## CI

GitHub Actions:

- on every push and PR: unit tests and a debug build;
- on tags `v*`: a signed release APK attached to a GitHub Release, signing key from secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## README

Add a section at the top of the README covering:

- what the fork adds and why (short version of Background);
- how to create the signing key and add the secrets;
- installation, then selecting it in **Settings → System & updates → Language & input → Default keyboard**;
- physical layout setup on HarmonyOS 4.3 in **Settings → Accessibility features → Keyboard**: for each keyboard remove extra layouts (such as ExKeyMo Layout) and keep **Russian**;
- the combination table;
- manual testing with https://w3c.github.io/uievents/tools/key-event-viewer.html.

## Manual test checklist

- Alt+U then a/o/u/A → ä/ö/ü/Ä; Alt+S → ß; Alt+Shift+C → Ç.
- Alt+E then x → ´x; Alt+U then Space → ¨.
- Russian layout: Cyrillic typing unchanged, right Alt + letter still gives Latin.
- Ctrl+Space switches layouts; Ctrl+C/Ctrl+V and other shortcuts work.
- On-screen keyboard works as before, including suggestions.
- Works in a plain text field, Chrome, and a terminal app.

## Workflow

1. Fork and get upstream building unchanged with modern tooling; stop and report what changed.
2. Show the public interface of the logic class and wait for confirmation.
3. Logic + tests, then the `LatinIME` integration, then CI and README.

