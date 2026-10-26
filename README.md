# AuroraTV

A cinematic Android TV launcher: layered mountain scenery, a large clock that the foreground ridge overlaps,
and a floating glass dock of your installed apps. Built for a TCL 65Q51K (Android TV 12) but should work on any Android TV 9+.

**Status: version 0.1.0 - source only, NOT yet compiled or tested on a TV by its author.**
See "Unverified" at the bottom. The first GitHub Actions build may need a small fix; send me the build log if it fails.

## How it takes over the TV
- AuroraTV installs as a normal app **and** registers as a Home-screen app. Android itself decides who is Home, so the first
  time you press the **Home** button on your remote, the TV asks which Home app to use. Choose **AuroraTV** and tap **Always**.
  From then on Home, and turning the TV on, open AuroraTV instead of the TCL home screen.
- If no prompt appears (some TV makers hide it), the TV does not allow replacing its launcher without special tools;
  AuroraTV still works when opened from the app list. I will not use root/ADB tricks to force it.
- Your original launcher stays installed (it can't be removed safely), but you won't see it unless you ask:
  AuroraTV Settings > **Go to original TV home screen** (one-off visit) or **Use AuroraTV as Home screen: Off** (goes back permanently).
- Dock has a **TV Settings** item (Wi-Fi, display, sound, inputs...) and an **AuroraTV** item (looks and behaviour),
  so you are never stuck inside AuroraTV.
- No permissions at all: no network, no microphone, no storage, no analytics, no accounts, no API keys.

**Important safety advice:** this first build is untested. Before pressing "Always", try AuroraTV from the app list (or choose
"Just once" at the Home prompt) for a day. If it ever misbehaves, uninstall it (Settings > Apps > AuroraTV, or
`adb uninstall com.auroratv.launcher`) and the original home returns.

## Build it with GitHub Actions (free, no Android Studio needed)
1. Unzip this project on your Windows PC.
2. On github.com open your repository > **Add file > Upload files**. Drag in the **contents** of the unzipped
   `AuroraTV` folder (the folders `app`, `gradle`, `.github`, and the loose files), not the .zip itself, then **Commit changes**.
   The `.github` folder is hidden-style; if it doesn't appear in your repo afterwards, use **Add file > Create new file**,
   type `.github/workflows/build-apk.yml` as the name, and paste in the contents of that file.
3. Open the repo's **Actions** tab. A run called **Build AuroraTV debug APK** starts by itself on every push.
   You can also start it by hand: Actions > Build AuroraTV debug APK > **Run workflow**.
4. When the run shows a green tick (about 5-10 minutes), open it and download **AuroraTV-debug-apk** from the
   **Artifacts** section at the bottom. Unzip it to get `app-debug.apk`
   (inside the build: `app/build/outputs/apk/debug/app-debug.apk`).
5. Red cross? Open the failed step, copy the error text and send it to me.

Notes: the workflow uses only free GitHub-hosted runners, needs no secrets, and deploys nothing.
Because the repo is public, anyone signed in to GitHub can download the artifact; the APK contains no private data.
Every build is signed with the same public debug key in the repo (`app/aurora-debug.keystore`, password `android`),
so new builds install over old ones. That key is not secret, which is fine for a personal sideloaded app.

## Install on the TCL TV
One-time: on the TV open Settings > Device Preferences > About, click **Build** 7 times; then Settings > Device Preferences >
Developer options > turn on **USB debugging** (and **Network debugging** if offered). Choose ONE method:

**A. USB stick (easiest, no PC tools):** copy `app-debug.apk` to a USB stick, plug it into the TV, open a file manager app on
the TV (install "File Commander" or "X-plore" from the Play Store if needed), allow "Install unknown apps" for it, and open the APK.

**B. ADB from Windows PC (free):** download Google's "platform-tools" for Windows, unzip, then in that folder in a terminal:
```
adb connect TV_IP_ADDRESS:5555
adb install -r path\to\app-debug.apk
```
(TV IP is under Settings > Network. Accept the debugging prompt shown on the TV.)

Then press **Home** on your remote and pick AuroraTV (or open it from the TV's app list).

## Using it
- D-pad Left/Right moves along the dock; OK opens the app. The last two items are **TV Settings** and **AuroraTV** settings (Menu button also opens AuroraTV settings). Back does nothing on the Home screen itself.
- No input for the ambient time: the dock fades out and the scenery drifts slowly. Any key brings the dock back
  (that first key press only wakes it up).
- Settings: scenery (auto/sunrise/sunset/twilight), clock format, **clock height** (adjusts how much of the ridge covers the
  numbers), date on/off, parallax on/off, ambient delay, ambient motion, TV apps only vs all apps.

## Update
Upload changed files to the same GitHub repo, wait for the Actions run to finish, install the new APK the same way (it installs over the old one).

## Uninstall / recovery
- In AuroraTV: Settings > **Use AuroraTV as Home screen: Off**, or **Go to original TV home screen**.
- Or Settings > Apps > See all apps > AuroraTV > Uninstall. Or `adb uninstall com.auroratv.launcher`.
- If AuroraTV ever crashes on Home and you can't reach settings, use ADB from your PC: `adb uninstall com.auroratv.launcher`.

## Project layout
- `app/src/main/java/com/auroratv/launcher/`
  - `HomeActivity.kt` - the screen: layer stack, clock, dock, parallax, ambient mode
  - `ScenicLayer.kt` - original procedural mountain art (sky / far / mid / foreground), no copyrighted images
  - `Palettes.kt` - sunrise, sunset, twilight colours
  - `DockAdapter.kt` - dock icons, focus animation
  - `SettingsActivity.kt`, `Prefs.kt` - settings
  - `assistant/AssistantProvider.kt` - empty hook for a future voice assistant (does nothing now)
- `.github/workflows/build-apk.yml` - cloud build recipe. `NOTICE.md` - licensing.

## Not in 0.1.0 (planned)
Looping video wallpaper, custom user photo wallpapers, true backdrop blur on the dock, voice assistant.

## Unverified (honest list)
Nothing here has been compiled or run yet. In particular unverified: the GitHub Actions build itself; remote navigation and focus
feel; app discovery on the TCL; Android 12 behaviour; how much of the clock the ridge covers (use the "Clock height" setting);
performance of the parallax and ambient drift; whether the TCL shows the Home-app prompt and whether turning AuroraTV off returns cleanly to the original home.
The glass dock is a translucent tint with a sheen - it does not blur what is behind it.
